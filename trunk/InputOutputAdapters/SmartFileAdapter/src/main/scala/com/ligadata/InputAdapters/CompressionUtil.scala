package com.ligadata.InputAdapters

/**
  * Created by Yasser on 3/10/2016.
  */
import java.io.{InputStream, IOException}
import java.util.zip.GZIPInputStream
import com.ligadata.Exceptions.KamanjaException
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.anarres.lzo.LzopInputStream
import org.apache.logging.log4j.LogManager
import net.sf.jmimemagic._
import org.apache.tika.Tika
import org.apache.tika.detect.DefaultDetector
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io._
import java.nio.file._



object FileType{
  val GZIP =  "application/gzip"
  val BZIP2 = "application/x-bzip2"
  val LZO = "application/x-lzop"
  val PLAIN = "text/plain"
  val UNKNOWN = "unknown"
}
import FileType._

object CompressionUtil {

  def BZIP2_MAGIC = 0x685A42
  def LZO_MAGIC   = 0x4f5a4c
  def GZIP_MAGIC = GZIPInputStream.GZIP_MAGIC


  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  /**
    * gets what type of compression used to compress the file
    * @param genericFileHandler
    * @param detectionType : how to detect the compression type. for now only ByMagicNumbers is supported, it is also default if no value is provided
    * @return
    */
  def getFileType(genericFileHandler: SmartFileHandler, filePath : String, detectionType : String) : String = {

    if(detectionType == null || detectionType.length == 0 || detectionType.equalsIgnoreCase("ByMagicNumbers")){
      val startTm = System.nanoTime

      val typ = detectFileType(genericFileHandler, filePath)

      val endTm = System.nanoTime
      val elapsedTm = endTm - startTm
      logger.info("CompressionUtil - finished gettting file type for file %s. Operation took %fms. StartTime:%d, EndTime:%d.".
        format(filePath, elapsedTm/1000000.0,elapsedTm, endTm))
      typ
    }
    else
      throw new KamanjaException("Unsupported type for detecting files compression: " + detectionType, null)
  }

  /**
    * checking the compression type by comparing magic numbers which is the head of the file
    * @param is : input stream object
    * @return CompressionType
    */
  def detectCompressionTypeByMagicNumbers(is : InputStream) : String = {
    //some types magic number is only two bytes and some are 3 or 4 bytes
    //if length is less than 2 bytes then the type is known (and probably the file is corrupt)

    val minlen = 2
    val maxlen = 4
    val buffer = new Array[Byte](maxlen)
    val readlen = is.read(buffer, 0, maxlen)
    if (readlen < minlen)
      return UNKNOWN

    //check for magic numbers with two bytes
    //for now only gzip
    val testGzipHead = (buffer(0) & 0xff) | ((buffer(1) << 8) & 0xff00)
    if(testGzipHead == GZIP_MAGIC)
      return GZIP

    if(readlen < 3)
      return UNKNOWN

    //check for magic numbers with three bytes
    //for now only bzip2
    val testBzip2Head = (buffer(0)& 0xff) | ((buffer(1) << 8) & 0xff00) | ((buffer(2) << 16) & 0xff0000)
    if(testBzip2Head == BZIP2_MAGIC)
      return BZIP2

    if(readlen < 4)
      return UNKNOWN

    //lzo magic number is 3 bytes but starting from the second byte
    val testLzoHead = (buffer(1) & 0xff) | ((buffer(2) << 8) & 0xff00) | ((buffer(3) << 16) & 0xff0000)
    if (testLzoHead == LZO_MAGIC)
      return LZO


    UNKNOWN
  }

  /**
    * checking the compression type using tika and jmimemagic libraries
    * @param genericFileHandler
    * @return CompressionType
    */
  def detectFileType(genericFileHandler: SmartFileHandler, filePath : String) : String ={
    val loggerName = this.getClass.getName
    val logger = LogManager.getLogger(loggerName)


    val detector = new DefaultDetector()
    val tika = new Tika(detector)
    var is : InputStream = null
    var contentType :String = null

    try {
      is = genericFileHandler.getDefaultInputStream(filePath)
      contentType = tika.detect(is)
    }catch{
      case e:IOException =>{
        logger.warn("SmartFileConsumer - Tika unable to read from InputStream - "+e.getMessage)
        throw e
      }
      case e:Exception =>{
        logger.warn("SmartFileConsumer - Tika processing generic exception - "+e.getMessage)
        throw e
      }
      case e:Throwable =>{
        logger.warn("SmartFileConsumer - Tika processing runtime exception - "+e.getMessage)
        throw e
      }
    }
    try{
      if (is != null) is.close()
    }
    catch{
      case e : Throwable => logger.warn("SmartFileConsumer - error while closing file" + filePath, e)
    }

    var checkMagicMatchManually = false
    if(contentType!= null && !contentType.isEmpty() && contentType.equalsIgnoreCase("application/octet-stream")){
      var magicMatcher : MagicMatch =  null;

      try{

        //read some bytes to pass to getMagicMatch
        is = genericFileHandler.getDefaultInputStream(filePath)
        val bufferSize = 10
        val bytes = new Array[Byte](bufferSize)
        is.read(bytes, 0, bufferSize)

        magicMatcher = Magic.getMagicMatch(bytes, false)
        if(magicMatcher != null)
          contentType = magicMatcher.getMimeType
      }catch{
        case e:MagicParseException =>{
          logger.warn("SmartFileConsumer - MimeMagic caught a parsing exception - "+e.getMessage)
          checkMagicMatchManually = true
        }
        case e:MagicMatchNotFoundException =>{
          logger.warn("SmartFileConsumer -MimeMagic Mime Not Found -"+e.getMessage)
          checkMagicMatchManually = true
        }
        case e:MagicException =>{
          logger.warn("SmartFileConsumer - MimeMagic generic exception - "+e.getMessage)
          checkMagicMatchManually = true
        }
        case e:Exception =>{
          logger.warn("SmartFileConsumer - MimeMagic processing generic exception - "+e.getMessage)
          checkMagicMatchManually = true
        }
        case e:Throwable =>{
          logger.warn("SmartFileConsumer - MimeMagic processing runtime exception - "+e.getMessage)
          checkMagicMatchManually = true
        }

      }
      try{
        if (is != null) is.close()
      }
      catch{
        case e : Throwable => logger.warn("SmartFileConsumer - error while closing file" + filePath, e)
      }
    }

    /*if(contentType == PLAIN)
      checkMagicMatchManually = true*/

    if(checkMagicMatchManually){
      //in case jmimemagic lib failed to detect, try manually - this happened when testing some lzop files
      try{
        logger.debug("SmartFileConsumer - checking magic numbers directly")
        is = genericFileHandler.getDefaultInputStream(filePath)
        val manuallyDetectedType = detectCompressionTypeByMagicNumbers(is)
        is.close()

        //if manual detected got unknown, keep the value plain. else get manually detected value
        if(manuallyDetectedType != UNKNOWN)
          contentType = manuallyDetectedType
      }
      catch{
        case e : Exception => {
          logger.warn("SmartFileConsumer - MimeMagic processing runtime exception - "+e.getMessage)
        }
        case e : Throwable => {
          logger.warn("SmartFileConsumer - MimeMagic processing Throwable - "+e.getMessage)
        }
      }
    }
    contentType
  }

  def detectCompressionTypeByExtension(filePath : String) : String = {

    val fileNameParts = filePath.split("\\.")
    if(fileNameParts.length < 2)
      return UNKNOWN

    val extension = fileNameParts(fileNameParts.length - 1).toLowerCase

    if(extension.equalsIgnoreCase("gzip") || extension.equalsIgnoreCase("gz"))
      return GZIP

    if(extension.equalsIgnoreCase("bz2"))
      return BZIP2

    if(extension.equalsIgnoreCase("lzo") || extension.equalsIgnoreCase("lzop"))
      return LZO

    UNKNOWN
  }

  /**
    * based on the compression type build a stream using the original stream.
    * this way the returned steam object can be treated in an abstract way
    *
    * @param originalInStream any input stream
    * @param fileType GZIP, BZIP2, LZO, PLAIN, UNKNOWN
    * @return input stream suitable for the file based on its compression type
    */
  def getProperInputStream(originalInStream : InputStream, fileType : String,
                           considerUnknownFileTypesAsIs : Boolean) : InputStream = {
    val loggerName = this.getClass.getName
    val logger = LogManager.getLogger(loggerName)
    try {
      fileType match {
        case GZIP => new GZIPInputStream(originalInStream)
        case BZIP2 => new BZip2CompressorInputStream(originalInStream)
        case LZO => new LzopInputStream(originalInStream)
        case PLAIN => originalInStream
        case UNKNOWN =>  if(considerUnknownFileTypesAsIs) originalInStream else throw new Exception("Unsupported file type " + fileType)
        case _ => if(considerUnknownFileTypesAsIs) originalInStream else throw new Exception("Unsupported file type " + fileType)

      }
    }
    catch{
      case e : Exception => {
        logger.error("SmartFileConsumer - error while getting Proper Input Stream - ", e)
        originalInStream
      }
      case e : Throwable => {
        logger.error("SmartFileConsumer - error while getting Proper Input Stream - ", e)
        originalInStream
      }
    }

  }

  /**
    *
    * @param srcCompression
    * @param destCompression
    * @return true if both src and dest have same compression
    */
  def compareSrcDistCompression(srcCompression : String, destCompression : String) :  Boolean = {
    (srcCompression.equalsIgnoreCase(FileType.GZIP) && destCompression.equalsIgnoreCase(CompressorStreamFactory.GZIP)) ||
      (srcCompression.equalsIgnoreCase(FileType.BZIP2) && destCompression.equalsIgnoreCase(CompressorStreamFactory.BZIP2)) ||
      ((srcCompression.equalsIgnoreCase(FileType.PLAIN) || srcCompression.equalsIgnoreCase(FileType.UNKNOWN)) && (destCompression == null || destCompression.length == 0 ))
  }

}


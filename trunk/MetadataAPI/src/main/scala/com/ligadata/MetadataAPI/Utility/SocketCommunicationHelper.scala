package com.ligadata.MetadataAPI.Utility


import java.io.{OutputStream, InputStream}
import scala.collection.mutable.ArrayBuffer
import java.nio.{ByteBuffer, ByteOrder}
import java.util.Arrays
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

/**
  * Created by Yasser on 6/8/2016.
  */
object SocketCommunicationHelper{

  val START_MARKER : Array[Byte] = Array(1.toByte,1.toByte,2.toByte,2.toByte)
  val END_MARKER : Array[Byte] = Array(2.toByte,2.toByte,1.toByte,1.toByte)

  def wrapCommandInJson(cmdTokens : Array[String]): String ={
    "{\"cmd\":[" + cmdTokens.map(token => "\"" + token + "\"").mkString(", ") + "]}"
  }

  def getCommandParts(cmdJson : String) : Array[String] = {
    val cmd = parse(cmdJson)

    if (cmd == null || cmd.values == null) {
      val err = "Invalid cmd Json"
      throw new Exception(err, null)
    }
    val cmdValues = cmd.values.asInstanceOf[Map[String, Any]]
    if(cmdValues.getOrElse("cmd", null) == null) {
      val err = "Invalid cmd Json"
      throw new Exception(err, null)
    }

    println("getCommandParts - cmd parts: " + cmdValues.get("cmd").get)
    val partsJson = cmdValues.get("cmd").get.asInstanceOf[List[String]]
    partsJson.toArray
  }

  /**
    *
    * @param in input stream
    * @return (msg, flag=true when stream is closed
    */
  def readMsg(in : InputStream) : (String, Boolean) = {
    var isClosed = false

    var msg = ""
    var buffer = new Array[Byte](START_MARKER.length)
    var readLen : Int = in.read(buffer, 0, START_MARKER.length)

    if(readLen < 0){
      isClosed = true
      return (null, isClosed)
    }

    if(readLen < START_MARKER.length){
      throw new Exception("Corrupt message: start marker not found")
    }
    if (!Arrays.equals(buffer, START_MARKER)){
      throw new Exception("Corrupt message: start marker not found")
    }

    buffer = new Array[Byte](4)//reading int representing msg size
    readLen = in.read(buffer, 0, 4)//read msg size
    if(readLen <= 0){
      throw new Exception("Corrupt message: msg size not found")
    }

    val msgSize = ByteBuffer.wrap(buffer.slice(0, 4)).order(ByteOrder.BIG_ENDIAN).getInt()
    if(msgSize > 0){

      buffer = new Array[Byte](msgSize)
      readLen = in.read(buffer, 0, msgSize)
      if(readLen < msgSize){
        throw new Exception("Corrupt message: could not read full message")
      }

      msg = new String(buffer)
    }

    buffer = new Array[Byte](END_MARKER.length)
    readLen = in.read(buffer, 0, END_MARKER.length)
    if(readLen < END_MARKER.length){
      throw new Exception("Corrupt message: end marker not found")
    }
    if (!Arrays.equals(buffer, END_MARKER)){
      throw new Exception("Corrupt message: end marker not found")
    }

    (msg, isClosed)
  }

  def writeMsg(msg : String, out : OutputStream): Unit ={
    out.write(START_MARKER)

    val msgBytes = msg.getBytes()
    val msgSize = msgBytes.length
    val msgSizeAsBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(msgSize).array()
    out.write(msgSizeAsBytes)
    out.write(msgBytes)

    out.write(END_MARKER)
  }

  /*def getMsgBytes(msg : String) : Array[Byte] = {
    val msgBytes = msg.getBytes
    val msgLen = msgBytes.size
    println("getMsgBytes="+msgLen)
    val buffer =  ArrayBuffer[Byte]()
    buffer ++= START_MARKER
    buffer ++= ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(msgLen).array()
    buffer ++= msgBytes
    buffer ++= END_MARKER
    buffer.toArray
  }*/

  /*def extractMsg(bytes : Array[Byte]) : String = {
    var msg = ""
    val start = bytes.slice(0, START_MARKER.length)
    if(!Arrays.equals(start, START_MARKER)){
      throw new Exception("Message is corrupt: start marker not found")
    }
    else{
      val sizeBytes  = bytes.slice(START_MARKER.length, START_MARKER.length + 4)
      val msgSize = ByteBuffer.wrap(sizeBytes).order(ByteOrder.BIG_ENDIAN).getInt()
      println("extractMsg="+msgSize)
      val msgBytes = bytes.slice(START_MARKER.length + 4, START_MARKER.length + 4 + msgSize)
      msg = new String(msgBytes)
      println("msg="+msg)
      val end = bytes.slice(START_MARKER.length + 4 + msgSize, START_MARKER.length + 4 + msgSize + END_MARKER.length)
      if(!Arrays.equals(end, END_MARKER)){
        throw new Exception("Message is corrupt: end marker not found")
      }
    }
    msg
  }*/
}

package com.ligadata.MetadataAPI


import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.InputSource
import org.xml.sax.XMLReader
import scala.collection.mutable._
import java.io.BufferedWriter
import java.io.FileWriter
import sys.process._
import java.io.PrintWriter
import org.apache.log4j._
import com.ligadata.olep.metadata._
import com.ligadata._
import com.ligadata.messagedef._
import com.ligadata.Compiler._

case class MsgCompilationFailedException(e: String) extends Throwable(e)
case class ModelCompilationFailedException(e: String) extends Throwable(e)

// CompilerProxy has utility functions to:
// Call MessageDefinitionCompiler, 
// Call PmmlCompiler, 
// Generate jar files out of output of above compilers
// Persist model definitions and corresponding jar files in Metadata Mgr
// Persist message definitions, and corresponding jar files in Metadata Mgr
class CompilerProxy{

  val loggerName = this.getClass.getName
  lazy val logger = Logger.getLogger(loggerName)

  def setLoggerLevel(level: Level){
    logger.setLevel(level);
  }

  def dumpStrTextToFile(strText : String, filePath : String) {
    val file = new File(filePath);
    val bufferedWriter = new BufferedWriter(new FileWriter(file))
    bufferedWriter.write(strText)
    bufferedWriter.close
  }

  def writeSrcFile(scalaGeneratedCode : String, scalaSrcTargetPath : String) {
    val file = new File(scalaSrcTargetPath);
    val bufferedWriter = new BufferedWriter(new FileWriter(file))
    bufferedWriter.write(scalaGeneratedCode)
    bufferedWriter.close
  }

  def createScalaFile(targPath : String, moduleSrcName : String, scalaGeneratedCode : String) {
    val scalaTargetPath = s"$targPath/$moduleSrcName"
    writeSrcFile(scalaGeneratedCode, scalaTargetPath)
  }

  /* 
   * Compile the supplied generated code and jar it, the originating pmml model, and the class output from the 
   * compile.  Add a registration module as well.  Note the classpath dependencies in the manifest.mf file
   * that is also included in the jar.
   */

  def compile (jarBuildDir : String
	     , scalahome : String
	     , moduleName : String
	     , classpath : String
	     , scalaGeneratedCode : String
	     , clientName : String
	     ) : Int = 
  {  
    val scalaSrcFileName : String = s"$moduleName.scala"
    createScalaFile(s"$jarBuildDir", scalaSrcFileName, scalaGeneratedCode)
		
    val scalacCmd = Seq("sh", "-c", s"$scalahome/bin/scalac -cp $classpath $jarBuildDir/$scalaSrcFileName")
    logger.debug(s"scalac cmd used: $scalacCmd")
    val scalaCompileRc = Process(scalacCmd).!
    if (scalaCompileRc != 0) {
      logger.error(s"Compile for $scalaSrcFileName has failed...rc = $scalaCompileRc")
      logger.error(s"Command used: $scalacCmd")
    }
		
    /** The compiled class files are found in com/$client/pmml of the current folder.. mv them to $jarBuildDir*/
    val mvCmd : String = s"mv com /tmp/$moduleName/"
    val mvCmdRc : Int = Process(mvCmd).!
    if (mvCmdRc != 0) {
      logger.error(s"unable to classes to build directory, $jarBuildDir ... rc = $mvCmdRc")
      logger.error(s"cmd used : $mvCmd")
    }		
    (scalaCompileRc | mvCmdRc)
  }

  def jarCode ( moduleName: String
		, scalaGeneratedCode : String
		, classpath : String
		, jarTargetDir : String
		, clientName : String
		, pmmlFilePath : String
		, scalahome : String
		, javahome : String) : (Int, String) =
  {
    //val (prop,envVars) = MetadataAPIImpl.readProperties
    /** prep the workspace and go there*/
    val killDir = s"rm -Rf /tmp/$moduleName"
    val killDirRc = Process(killDir).! /** remove any work space that may be present from prior failed run  */
    if (killDirRc != 0) {
      logger.error(s"Unable to rm /tmp/$moduleName ... rc = $killDirRc")
      return (killDirRc, "")
    }
    val buildDir = s"mkdir /tmp/$moduleName"
    val tmpdirRc = Process(buildDir).! /** create a clean space to work in */
    if (tmpdirRc != 0) {
      logger.error(s"The compilation of the generated source has failed because $buildDir could not be created ... rc = $tmpdirRc")
      return (tmpdirRc, "")
    }
    /** create a copy of the pmml source in the work directory */
    val cpRc = Process(s"cp $pmmlFilePath /tmp/$moduleName/").!
    if (cpRc != 0) {
      logger.error(s"Unable to create a copy of the pmml source xml for inclusion in jar ... rc = $cpRc")
      return (cpRc, "")
    }
    /** compile the generated code */
    val rc : Int = compile(s"/tmp/$moduleName", scalahome, moduleName, classpath, scalaGeneratedCode, clientName)
    if (rc != 0) {
      return (rc, "")
    }

    /** create the jar */
    val moduleNameJar : String = s"$moduleName.jar"
    val jarCmd : String = s"$javahome/bin/jar cvf $moduleNameJar -C /tmp/$moduleName/ ."
    logger.debug(s"jar cmd used: $jarCmd")
    logger.info(s"Jar $moduleNameJar produced.  Its contents:")
    val jarRc : Int = Process(jarCmd).!
    if (jarRc != 0) {
      logger.error(s"unable to create jar $moduleNameJar ... rc = $jarRc")
      return (jarRc, "")
    }
		
    /** move the new jar to the target dir where it is to live */
    val mvCmd : String = s"mv $moduleNameJar $jarTargetDir/"
    val mvCmdRc : Int = Process(mvCmd).!
    if (mvCmdRc != 0) {
      logger.error(s"unable to move new jar $moduleNameJar to target directory, $jarTargetDir ... rc = $mvCmdRc")
      logger.error(s"cmd used : $mvCmd")
    }
		
    (0, s"$moduleNameJar")
  }


  def compilePmml(pmmlStr: String) : (String,ModelDef) = {

    /** Ramana, if you set this to true, you will cause the generation of logger.info (...) stmts in generated model */
    val injectLoggingStmts : Boolean = false 
    val compiler  = new PmmlCompiler(MdMgr.GetMdMgr, "ligadata", logger, injectLoggingStmts)
    val (classStr,model) = compiler.compile(pmmlStr)

    var pmmlScalaFile = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR") + "/" + model.name + ".pmml"    
    val (jarFile,depJars) = 
      compiler.createJar(classStr,
			 MetadataAPIImpl.GetMetadataAPIConfig.getProperty("CLASSPATH"),
			 pmmlScalaFile,
			 MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR"),
			 MetadataAPIImpl.GetMetadataAPIConfig.getProperty("MANIFEST_PATH"),
			 MetadataAPIImpl.GetMetadataAPIConfig.getProperty("SCALA_HOME"),
			 MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAVA_HOME"),
			 false)
    model.jarName = jarFile
    if( model.ver == 0 ){
      model.ver     = 1
    }
    if( model.modelType == null){
      model.modelType = "RuleSet"
    }
    (classStr,model)
  }


  @throws(classOf[MsgCompilationFailedException])
  def compileMessageDef(msgDefStr: String) : (String,ContainerDef) = {
    try{
      val mgr = MdMgr.GetMdMgr
      val msg = new MessageDefImpl()
      logger.trace("Call Message Compiler ....")
      val(classStr, msgDef) = msg.processMsgDef(msgDefStr, "JSON",mgr)
      logger.trace("Message Compilation done ....")
      val msgDefFilePath = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR") + "/" + msgDef.name + ".txt"
      dumpStrTextToFile(msgDefStr,msgDefFilePath)
      val msgDefClassFilePath = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR") + "/" + msgDef.name + ".scala"
      dumpStrTextToFile(classStr,msgDefClassFilePath)
      var(status,jarFile) = jarCode(msgDef.name,
	    classStr,
	    MetadataAPIImpl.GetMetadataAPIConfig.getProperty("CLASSPATH"),
	    MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR"),
	    "Test Client",
	    msgDefFilePath,
	    MetadataAPIImpl.GetMetadataAPIConfig.getProperty("SCALA_HOME"),
	    MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAVA_HOME"))

      logger.trace("Status => " + status + ",Jar File => " + jarFile)

      if ( msgDef.nameSpace == null ){
	msgDef.nameSpace = MetadataAPIImpl.sysNS
      }

      msgDef.jarName = jarFile
      (classStr,msgDef)
    }
    catch{
      case e:Exception =>{
	logger.trace("Failed to compile the message definition " + e.toString)
	e.printStackTrace
	throw new MsgCompilationFailedException(msgDefStr)
      }
    }
  }
}


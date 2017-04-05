package com.ligadata.dataaccessapi

import java.io.File

import com.ligadata.Exceptions.KamanjaException
import com.ligadata.KamanjaBase.{ContainerFactoryInterface, _}
import com.ligadata.MetadataAPI.MetadataAPIImpl
import com.ligadata.Utils.{KamanjaClassLoader, KamanjaLoaderInfo, Utils}
import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadata.MdMgr._
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Formats}
import org.json4s._
import org.json4s.JsonDSL._

import scala.collection.mutable.TreeSet

/**
  * Created by Yousef on 3/31/2017.
  */

class SearchUtil(messageName: String) extends ObjectResolver {

  val searchUtilLoder = new KamanjaLoaderInfo
  var objFullName: String = _
  var typeNameCorrType: BaseTypeDef = _
  var messageObj: MessageFactoryInterface = _
  var containerObj: ContainerFactoryInterface = _
  typeNameCorrType = mdMgr.ActiveType(messageName.toLowerCase)
  var nodeInfo: NodeInfo = _
  var isOk: Boolean = true

  if (typeNameCorrType == null || typeNameCorrType == None) {
    logger.error("Not found valid type for " + messageName.toLowerCase)
    isOk = false
  } else {
    objFullName = typeNameCorrType.FullName.toLowerCase
  }

  if (isOk) {
    val jarPaths = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS").split(",")
    SearchUtilConfiguration.jarPaths = if (jarPaths == null) Array[String]().toSet else  jarPaths.map(str => str.replace("\"", "").trim).filter(str => str.size > 0).toSet
    if (SearchUtilConfiguration.jarPaths.size == 0) {
      logger.error("Not found valid JarPaths.")
      isOk = false
    }
  }

  if (isOk) {
    isOk = LoadJarIfNeeded(typeNameCorrType, searchUtilLoder.loadedJars, searchUtilLoder.loader)
  }

  var isMsg = false
  var isContainer = false

  if (isOk) {
    var clsName = typeNameCorrType.PhysicalName.trim
    if (clsName.size > 0 && clsName.charAt(clsName.size - 1) != '$') // if no $ at the end we are taking $
      clsName = clsName + "$"

    if (isMsg == false) {
      // Checking for Message
      try {
        // Convert class name into a class
        var curClz = Class.forName(clsName, true, searchUtilLoder.loader)

        while (curClz != null && isContainer == false) {
          isContainer = Utils.isDerivedFrom(curClz, "com.ligadata.KamanjaBase.ContainerFactoryInterface")
          if (isContainer == false)
            curClz = curClz.getSuperclass()
        }
      } catch {
        case e: Exception => {
          logger.error("Failed to load message class %s".format(clsName), e)
        }
      }
    }

    if (isContainer == false) {
      // Checking for container
      try {
        // If required we need to enable this test
        // Convert class name into a class
        var curClz = Class.forName(clsName, true, searchUtilLoder.loader)

        while (curClz != null && isMsg == false) {
          isMsg = Utils.isDerivedFrom(curClz, "com.ligadata.KamanjaBase.MessageFactoryInterface")
          if (isMsg == false)
            curClz = curClz.getSuperclass()
        }
      } catch {
        case e: Exception => {
          logger.error("Failed to load container class %s".format(clsName), e)
        }
      }
    }

    if (isMsg || isContainer) {
      try {
        val module = searchUtilLoder.mirror.staticModule(clsName)
        val obj = searchUtilLoder.mirror.reflectModule(module)
        val objinst = obj.instance
        if (objinst.isInstanceOf[MessageFactoryInterface]) {
          messageObj = objinst.asInstanceOf[MessageFactoryInterface]
          logger.debug("Created Message Object")
        } else if (objinst.isInstanceOf[ContainerFactoryInterface]) {
          containerObj = objinst.asInstanceOf[ContainerFactoryInterface]
          logger.debug("Created Container Object")
        } else {
          logger.error("Failed to instantiate message or conatiner object :" + clsName)
        }
      } catch {
        case e: Exception => {
          logger.error("Failed to instantiate message or conatiner object:" + clsName, e)
        }
      }
    } else {
      logger.error("Failed to instantiate message or conatiner object :" + clsName)
    }
  }

  override def getInstance(MsgContainerType: String): ContainerInterface = {
    if (MsgContainerType.compareToIgnoreCase(objFullName) != 0)
      return null
    // Simply creating new object and returning. Not checking for MsgContainerType. This is issue if the child level messages ask for the type
    if (isMsg)
      return messageObj.createInstance.asInstanceOf[ContainerInterface]
    if (isContainer)
      return containerObj.createInstance.asInstanceOf[ContainerInterface]
    return null
  }

  override def getInstance(schemaId: Long): ContainerInterface = {
    //BUGBUG:: For now we are getting latest class. But we need to get the old one too.
    if (mdMgr == null)
      throw new KamanjaException("Metadata Not found", null)

    val contOpt = mdMgr.ContainerForSchemaId(schemaId.toInt)

    if (contOpt == None)
      throw new KamanjaException("Container Not found for schemaid:" + schemaId, null)

    getInstance(contOpt.get.FullName)
  }

  override def getMdMgr: MdMgr = mdMgr

  private def LoadJarIfNeeded(elem: BaseElem, loadedJars: TreeSet[String], loader: KamanjaClassLoader): Boolean = {
    if (SearchUtilConfiguration.jarPaths == null) return false

    var retVal: Boolean = true
    var allJars: Array[String] = null

    val jarname = if (elem.JarName == null) "" else elem.JarName.trim

    if (elem.DependencyJarNames != null && elem.DependencyJarNames.size > 0 && jarname.size > 0) {
      allJars = elem.DependencyJarNames :+ jarname
    } else if (elem.DependencyJarNames != null && elem.DependencyJarNames.size > 0) {
      allJars = elem.DependencyJarNames
    } else if (jarname.size > 0) {
      allJars = Array(jarname)
    } else {
      return retVal
    }

    val jars = allJars.map(j => Utils.GetValidJarFile(SearchUtilConfiguration.jarPaths, j))

    // Loading all jars
    for (j <- jars) {
      logger.debug("Processing Jar " + j.trim)
      val fl = new File(j.trim)
      if (fl.exists) {
        try {
          if (loadedJars(fl.getPath())) {
            logger.debug("Jar " + j.trim + " already loaded to class path.")
          } else {
            loader.addURL(fl.toURI().toURL())
            logger.debug("Jar " + j.trim + " added to class path.")
            loadedJars += fl.getPath()
          }
        } catch {
          case e: Exception => {
            logger.error("Jar " + j.trim + " failed added to class path.", e)
            return false
          }
        }
      } else {
        logger.error("Jar " + j.trim + " not found")
        return false
      }
    }

    true
  }

  /**
    * This method used to check if message exists in our metadata
    *
    * @param messageName message name t find
    * @return flag for message if exist
    */
  def checkMessageExists(messageName: String): Boolean = {
    var flag = false
    val msgDefs: Option[scala.collection.immutable.Set[MessageDef]] = MdMgr.mdMgr.Messages(true, true)
    if (msgDefs.isEmpty) {
      flag = false
    } else {
      for (message <- msgDefs.get) {
        // check if case sensitive
        if (messageName.equalsIgnoreCase(message.FullName)) {
          flag = true
        }
      }
    }
    flag
  }

  /**
    * deserialize message data
    *
    * @param messageName  message full name
    * @param deserializer deserializer type
    * @param optionsjson  extra option for deserializer
    * @return message binding info
    */
  def ResolveDeserializer(messageName: String, deserializer: String, optionsjson: String): MsgBindingInfo = {
    val serInfo = MdMgr.mdMgr.GetSerializer(deserializer)
    if (serInfo == null) {
      throw new KamanjaException(s"Not found Serializer/Deserializer for ${deserializer}", null)
    }
    val phyName = serInfo.PhysicalName
    if (phyName == null) {
      throw new KamanjaException(s"Not found Physical name for Serializer/Deserializer for ${deserializer}", null)
    }
    try {
      val aclass = Class.forName(phyName).newInstance
      val ser = aclass.asInstanceOf[SerializeDeserialize]

      val map = new java.util.HashMap[String, String] //BUGBUG:: we should not convert the 2nd param to String. But still need to see how can we convert scala map to java map
      var options: collection.immutable.Map[String, Any] = null
      if (optionsjson != null) {
        implicit val jsonFormats: Formats = DefaultFormats
        val validJson = parse(optionsjson)

        options = validJson.values.asInstanceOf[collection.immutable.Map[String, Any]]
        if (options != null) {
          options.foreach(o => {
            map.put(o._1, o._2.toString)
          })
        }
      }
      ser.configure(this, map)
      ser.setObjectResolver(this)
      return MsgBindingInfo(deserializer, options, optionsjson, ser)
    } catch {
      case e: Throwable => {
        throw new KamanjaException(s"Failed to resolve Physical name ${phyName} in Serializer/Deserializer for ${deserializer}", e)
      }
    }
  }

  /**
    * get partition key from message data
    *
    * @param messageFullName    message full name
    * @param data               data that used to extract partition key from it
    * @param messageBindingInfo message binding info includes desrializer type
    * @return patition key
    */
  def getMessageKey(messageFullName: String, data: String, messageBindingInfo: MsgBindingInfo): Array[String] = {
    if (messageBindingInfo == null || messageBindingInfo.serInstance == null) {
      throw new KamanjaException("Unable to resolve deserializer", null)
    }
    val message = messageBindingInfo.serInstance.deserialize(data.getBytes, messageFullName)
    val partitionKey: Array[String] = message.getPartitionKey
    partitionKey
  }

  /**
    * used to check deserializer type
    *
    * @param format json or delimited
    * @return deserializer type
    */
  def getDeserializerType(format: String): String = {
    if (format.equalsIgnoreCase("json")) {
      "com.ligadata.kamanja.serializer.jsonserdeser"
    } else {
      "com.ligadata.kamanja.serializer.csvserdeser"
    }
  }

  /**
    * check the value delimiter
    * @param valueDel valueDelimiter
    * @return valueDelimiter
    */
  def getValueDelimiter(valueDel: String): String ={
    if(valueDel.length == 0)
      "~"
    else
      valueDel
  }

  /**
    * check the field delimiter
    * @param fieldDel  fieldDelimiter
    * @return fieldDelimiter
    */
  def getFieldDelimiter(fieldDel: String): String ={
    if(fieldDel.length == 0)
      ","
    else
      fieldDel
  }

  /**
    * check if always quote fields
    * @param quoteFields alwaysQuoteFields
    * @return alwaysQuoteFields
    */
  def getAlwaysQuoteFields(quoteFields: String): String ={
    if(quoteFields.equalsIgnoreCase("false") || quoteFields.equalsIgnoreCase("true"))
      quoteFields
    else
      "false"
  }

  /**
    * get deserializer option
    * @param quoteFields alwaysQuoteFields
    * @param fieldDel fieldDelimiter
    * @param valueDel valueDelimiter
    * @param options deseializer option
    * @return deserializer option
    */
  def getDeserializeOption(quoteFields: String, fieldDel: String, valueDel: String, options: String): String ={
    if (options.length !=0)
      options
    else
      "{\"alwaysQuoteFields\":" + getAlwaysQuoteFields(quoteFields) +",\"fieldDelimiter\":\""+ getFieldDelimiter(fieldDel) + "\",\"valueDelimiter\":\""+ getValueDelimiter(valueDel) + "\"}"
  }

  /**
    * create message data
    *
    * @param Messagename message full name
    * @param quoteFields
    * @param fieldDel
    * @param valueDel
    * @param formatType
    * @param payLoad data to insert
    * @return message data as json format
    */
  def makeMessage(Messagename: String, payLoad: String, quoteFields: String, fieldDel: String, valueDel: String, formatType: String): String ={ //push to kafka
  val json = (
      ("MsgType" -> Messagename)~
        ("FormatOption" -> ("formatType" -> getDeserializerType(formatType))~
          ("alwaysQuoteFields" -> getAlwaysQuoteFields(quoteFields))~
          ("fieldDelimiter" -> getFieldDelimiter(fieldDel))~
          ("valueDelimiter" -> getValueDelimiter(valueDel))
          )~
        ("PayLoad" -> payLoad)
      )
    compact(render(json))
  }

  def makeMessage1(Messagename: String, payLoad: String,  options: String): String ={ //push to kafka
  val json = (
      ("MsgType" -> Messagename)~
        ("FormatOption" -> options)~
        ("PayLoad" -> payLoad)
      )
    compact(render(json))
  }

  def getFormatOption(formatOption: String): String ={
    if(formatOption.equalsIgnoreCase("json"))
      "json"
    else
      "delimited"
  }

  def getDeserializeOptionWithFormatType(quoteFields: String, fieldDel: String, valueDel: String, formatType: String): String ={
    """ {"formatType": "%s", "alwaysQuoteFields": "%s", "fieldDelimiter": "%s", "valueDelimiter":"%s"} """.format(getDeserializerType(formatType), getAlwaysQuoteFields(quoteFields), getFieldDelimiter(fieldDel), getValueDelimiter(valueDel))
  }
}

object SearchUtilConfiguration {
  var nodeId: Int = _
  var configFile: String = _
  var jarPaths: collection.immutable.Set[String] = _
}
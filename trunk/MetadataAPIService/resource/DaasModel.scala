package com.ligadata.kamanja.model
import com.ligadata.KamanjaBase._
import com.ligadata.kamanja.daasinputmessage
import com.ligadata.kamanja.metadata.ModelDef
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats
import com.ligadata.KamanjaManager.KamanjaMetadata
import com.ligadata.kamanja.metadata._
import org.apache.logging.log4j._


class DaasModelFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  override def createModelInstance(): ModelInstance = return new DaasModel(this)
  override def getModelName: String = "DaasModel"
  override def getVersion: String = "0.0.1"
  override def createResultObject(): ModelResultBase = new MappedModelResults()
  override def isModelInstanceReusable(): Boolean = true
}

  case class daasFormatOption(formatType: Option[String], alwaysQuoteFields: Option[String], fieldDelimiter: Option[String], valueDelimiter: Option[String])
  
class DaasModel(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  lazy val log = LogManager.getLogger(this.getClass.getName)

  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
    implicit val formats = DefaultFormats
    println("take input msg")
    val msgIn = execMsgsSet(0).asInstanceOf[daasinputmessage]
    println("create an object from searchutil")
    println("parse formatoption")
    println(msgIn.msgtype)
    println(msgIn.payload)
    println(msgIn.formatoption)
    val obj = parse(msgIn.formatoption).extract[daasFormatOption]
    println("ResolveDeserializer")
    val msgBindingInfo = ResolveDeserializer(msgIn.msgtype, obj.formatType.get, msgIn.formatoption)
    println("prepare output message")
    val message = msgBindingInfo.serInstance.deserialize(msgIn.payload.getBytes, msgIn.msgtype)
    println("return message")
    return Array(message)
  }

  def ResolveDeserializer(messageName: String, deserializer: String, optionsjson: String): MsgBindingInfo = {
    val serInfo = MdMgr.mdMgr.GetSerializer(deserializer)
    if (serInfo == null) {

      log.error(s"Not found Serializer/Deserializer for ${deserializer}")
    }
    val phyName = serInfo.PhysicalName
    if (phyName == null) {
      log.error(s"Not found Physical name for Serializer/Deserializer for ${deserializer}")
    }
    try {
      val aclass = Class.forName(phyName).newInstance
      val ser = aclass.asInstanceOf[SerializeDeserialize]

      val map = new java.util.HashMap[String, String] //BUGBUG:: we should not convert the 2nd param to String. But still need to see how can we convert scala map to java map
      var options: collection.immutable.Map[String, Any] = null
      if (optionsjson != null) {
        implicit val jsonFormats = DefaultFormats
        val validJson = parse(optionsjson)

        options = validJson.values.asInstanceOf[collection.immutable.Map[String, Any]]
        if (options != null) {
          options.foreach(o => {
            map.put(o._1, o._2.toString)
          })
        }
      }
      ser.configure(KamanjaMetadata, map)
      ser.setObjectResolver(KamanjaMetadata)
      return MsgBindingInfo(deserializer, options, optionsjson, ser)
    } catch {
      case e: Throwable => {
        log.error(s"Failed to resolve Physical name ${phyName} in Serializer/Deserializer for ${deserializer}")
        sys.exit(1)
      }
    }
  }
}

/*
 * Copyright 2015 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.KamanjaBase

import java.net.URL
import java.net.URLClassLoader
import java.io.{ByteArrayInputStream, DataInputStream, DataOutputStream, ByteArrayOutputStream}
import com.ligadata.Exceptions.KamanjaException
import com.ligadata.Utils.KamanjaClassLoader
import com.ligadata.kamanja.metadata.MdMgr
import org.apache.logging.log4j._
import java.util.Date
import java.util.Calendar
import java.text.SimpleDateFormat

import scala.collection.mutable.ArrayBuffer
import java.util.concurrent.locks.ReentrantReadWriteLock;
import scala.collection.JavaConversions._

//import org.json4s._
//import org.json4s.JsonDSL._
//import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization

/*trait MessageContainerBase {
  // System Columns
  var transactionId: Long
  var timePartitionData: Long = 0 // By default we are taking Java date with 0 milliseconds.
  var rowNumber: Int = 0 // This is unique value with in transactionId

  // System Attributes Functions
  final def TransactionId(transId: Long): Unit = { transactionId = transId }
  final def TransactionId(): Long = transactionId

  final def TimePartitionData(timeInMillisecs: Long): Unit = { timePartitionData = timeInMillisecs }
  final def TimePartitionData(): Long = timePartitionData

  final def RowNumber(rno: Int): Unit = { rowNumber = rno }
  final def RowNumber(): Int = rowNumber

  def isMessage: Boolean
  def isContainer: Boolean
  def IsFixed: Boolean
  def IsKv: Boolean
  def CanPersist: Boolean
  def populate(inputdata: InputData): Unit
  def set(key: String, value: Any): Unit
  def get(key: String): Any
  def getOrElse(key: String, default: Any): Any
  def AddMessage(childPath: Array[(String, String)], msg: BaseMsg): Unit
  def GetMessage(childPath: Array[(String, String)], primaryKey: Array[String]): BaseMsg
  def Version: String // Message or Container Version
  def PartitionKeyData: Array[String] // Partition key data
  def PrimaryKeyData: Array[String] // Primary key data
  def FullName: String // Message or Container Full Name
  def NameSpace: String // Message or Container NameSpace
  def Name: String // Message or Container Name
  def Deserialize(dis: DataInputStream, mdResolver: MdBaseResolveInfo, loader: java.lang.ClassLoader, savedDataVersion: String): Unit
  def Serialize(dos: DataOutputStream): Unit
  def Save(): Unit
  def Clone(): MessageContainerBase
  def hasPrimaryKey: Boolean
  def hasPartitionKey: Boolean
  def hasTimeParitionInfo: Boolean
  def getNativeKeyValues: scala.collection.immutable.Map[String, (String, Any)]
}

trait MessageContainerObjBase {
  def isMessage: Boolean
  def isContainer: Boolean
  def IsFixed: Boolean
  def IsKv: Boolean
  def CanPersist: Boolean
  def FullName: String // Message or Container FullName
  def NameSpace: String // Message or Container NameSpace
  def Name: String // Message or Container Name
  def Version: String // Message or Container Version
  def PartitionKeyData(inputdata: InputData): Array[String] // Partition key data
  def PrimaryKeyData(inputdata: InputData): Array[String] // Primary key data
  def getTimePartitionInfo: (String, String, String) // FieldName, Format & Time Partition Types(Daily/Monthly/Yearly)
  def TimePartitionData(inputdata: InputData): Long
  def hasPrimaryKey: Boolean
  def hasPartitionKey: Boolean
  def hasTimeParitionInfo: Boolean

  private def extractTime(fieldData: String, timeFormat: String): Long = {
    if (fieldData == null || fieldData.trim() == "") return 0

    if (timeFormat == null || timeFormat.trim() == "") return 0

    if (timeFormat.compareToIgnoreCase("epochtimeInMillis") == 0)
      return fieldData.toLong

    if (timeFormat.compareToIgnoreCase("epochtimeInSeconds") == 0 || timeFormat.compareToIgnoreCase("epochtime") == 0)
      return fieldData.toLong * 1000

    // Now assuming Date partition format exists.
    val dtFormat = new SimpleDateFormat(timeFormat);
    val tm =
      if (fieldData.size == 0) {
        new Date(0)
      } else {
        dtFormat.parse(fieldData)
      }
    tm.getTime()
  }

  def ComputeTimePartitionData(fieldData: String, timeFormat: String, timePartitionType: String): Long = {
    val fldTimeDataInMs = extractTime(fieldData, timeFormat)

    // Align to Partition
    var cal: Calendar = Calendar.getInstance();
    cal.setTime(new Date(fldTimeDataInMs));

    if (timePartitionType == null || timePartitionType.trim() == "") return 0

    timePartitionType.toLowerCase match {
      case "yearly" => {
        var newcal: Calendar = Calendar.getInstance();
        newcal.setTimeInMillis(0)
        newcal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        return newcal.getTime().getTime()
      }
      case "monthly" => {
        var newcal: Calendar = Calendar.getInstance();
        newcal.setTimeInMillis(0)
        newcal.set(Calendar.YEAR, cal.get(Calendar.YEAR))
        newcal.set(Calendar.MONTH, cal.get(Calendar.MONTH))
        return newcal.getTime().getTime()
      }
      case "daily" => {
        var newcal: Calendar = Calendar.getInstance();
        newcal.setTimeInMillis(0)
        newcal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        return newcal.getTime().getTime()
      }
    }
    return 0
  }

}
*/

trait MdBaseResolveInfo {
  def getMessgeOrContainerInstance(typName: String): ContainerInterface

  // Get Latest SchemaId For Type
  // getMessgeOrContainerInstance for SchemaId
  // Convert to LatestVersion (Take any object and tries to covert to new version)
}


/*
object SerializeDeserialize {

  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)
  def Serialize(inst: MessageContainerBase): Array[Byte] = {
    val bos: ByteArrayOutputStream = new ByteArrayOutputStream(1024 * 1024)
    val dos = new DataOutputStream(bos)

    try {
      dos.writeUTF(inst.FullName)
      dos.writeUTF(inst.Version)
      dos.writeUTF(inst.getClass.getName)
      inst.Serialize(dos)
      val arr = bos.toByteArray
      dos.close
      bos.close
      return arr

    } catch {
      case e: Exception => {
        //LOG.error("Failed to get classname :" + clsName, e)
        logger.debug("Failed to Serialize", e)
        dos.close
        bos.close
        throw e
      }
    }
    null
  }

  def Serialize(inst: MessageContainerBase, dos: DataOutputStream): Unit = {
    try {
      dos.writeUTF(inst.FullName)
      dos.writeUTF(inst.Version)
      dos.writeUTF(inst.getClass.getName)
      inst.Serialize(dos)
    } catch {
      case e: Exception => {
        //LOG.error("Failed to get classname :" + clsName, e)
        logger.debug("Failed to Serialize", e)
        throw e
      }
    }
  }

  def Deserialize(bytearray: Array[Byte], mdResolver: MdBaseResolveInfo, loader: java.lang.ClassLoader, isTopObject: Boolean, desClassName: String): MessageContainerBase = {
    var dis = new DataInputStream(new ByteArrayInputStream(bytearray));

    val typName = dis.readUTF
    val version = dis.readUTF
    val classname = dis.readUTF
    try {
      // Expecting type name
      // get class instance for this type
      val typ =
        if (isTopObject) {
          mdResolver.getMessgeOrContainerInstance(typName)
        } else {
          try {
            Class.forName(desClassName, true, loader)
          } catch {
            case e: Exception => {
              logger.error("Failed to load Message/Container class %s".format(desClassName), e)
              throw e // Rethrow
            }
          }
          var curClz = Class.forName(desClassName, true, loader)
          curClz.newInstance().asInstanceOf[MessageContainerBase]
        }
      if (typ == null) {
        throw new Exception("Message/Container %s not found to deserialize".format(typName))
      }
      typ.Deserialize(dis, mdResolver, loader, version.toString)
      dis.close
      return typ
    } catch {
      case e: Exception => {
        // LOG.error("Failed to get classname :" + clsName, e)
        logger.debug("Failed to Deserialize", e)
        dis.close
        throw e
      }
    }
    null
  }
}
*/

/*trait BaseContainer extends MessageContainerBase {
  override def isMessage: Boolean = false
  override def isContainer: Boolean = true
}

trait BaseContainerObj extends MessageContainerObjBase {
  override def isMessage: Boolean = false
  override def isContainer: Boolean = true
  def CreateNewContainer: BaseContainer
}

trait BaseMsg extends MessageContainerBase {
  override def isMessage: Boolean = true
  override def isContainer: Boolean = false
}

trait BaseMsgObj extends MessageContainerObjBase {
  override def isMessage: Boolean = true
  override def isContainer: Boolean = false
  def NeedToTransformData: Boolean // Filter & Rearrange input attributes if needed
  def CreateNewMessage: BaseMsg
}
*/

case class MsgBindingInfo(serName: String, options: Map[String, Any], optionsJsonStr: String, serInstance: SerializeDeserialize)

trait AdaptersSerializeDeserializers {
  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)

  private var objectResolver: ObjectResolver = _
  private var msgBindings = scala.collection.mutable.Map[String, MsgBindingInfo]()
  // For now we are mapping to one message to one serializer. Later may be we need to handle multiple serializers
  private val reent_lock = new ReentrantReadWriteLock(true)

  def getAdapterName: String

  private def ReadLock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.readLock().lock()
  }

  private def ReadUnlock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.readLock().unlock()
  }

  private def WriteLock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.writeLock().lock()
  }

  private def WriteUnlock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.writeLock().unlock()
  }

  final def setObjectResolver(objectResolver: ObjectResolver): Unit = {
    this.objectResolver = objectResolver
    val allBinds = getAllMessageBindings
    if (allBinds.size > 0) {
      val bindings = resolveBindings(allBinds.map(b => (b._1.toLowerCase(), (b._2.serName, b._2.options))))
      WriteLock(reent_lock)
      try {
        msgBindings ++= bindings // If we support multiple, we must need to have proper comparison function.
      } catch {
        case e: Throwable => {
          throw e
        }
      }
      finally {
        WriteUnlock(reent_lock)
      }
    }
  }

  final def getObjectResolver() = objectResolver

  private def SerializeMapToJsonString(map: Map[String, Any]): String = {
    implicit val formats = org.json4s.DefaultFormats
    return Serialization.write(map)
  }

  protected def resolveBinding(serName: String, options: Map[String, Any]): MsgBindingInfo = {
    if (objectResolver == null)
      throw new KamanjaException("Metadata/ObjectResolver manager is not yet set", null)

    val serInfo = objectResolver.getMdMgr.GetSerializer(serName)
    if (serInfo == null) {
      throw new KamanjaException(s"Not found Serializer/Deserializer for ${serName}", null)
    }

    var phyName = serInfo.PhysicalName
    if (phyName == null) {
      throw new KamanjaException(s"Not found Physical name for Serializer/Deserializer for ${serName}", null)
    }

    try {
      val aclass = Class.forName(phyName).newInstance
      val ser = aclass.asInstanceOf[SerializeDeserialize]
      val map = new java.util.HashMap[String, String] //BUGBUG:: we should not convert the 2nd param to String. But still need to see how can we convert scala map to java map
      if (options != null) {
        options.foreach(o => {
          map.put(o._1, o._2.toString)
        })
      }
      ser.configure(objectResolver, map)
      ser.setObjectResolver(objectResolver)
      val optionsJsonStr = if (options != null) SerializeMapToJsonString(options) else "{}"
      return MsgBindingInfo(serName, options, optionsJsonStr, ser)
    } catch {
      case e: Throwable => {
        throw new KamanjaException(s"Failed to resolve Physical name ${phyName} in Serializer/Deserializer for ${serName}", e)
      }
    }

    return null // Should not come here
  }

  private def resolveBindings(allBinds: Map[String, (String, Map[String, Any])]): Map[String, MsgBindingInfo] = {
    if (allBinds.size > 0)
      allBinds.map(b => (b._1.toLowerCase(), resolveBinding(b._2._1, b._2._2)))
    else
      Map[String, MsgBindingInfo]()
  }

  final def getAllMessageBindings: Map[String, MsgBindingInfo] = {
    var retVal: Map[String, MsgBindingInfo] = null
    ReadLock(reent_lock)
    try {
      retVal = msgBindings.toMap
    } catch {
      case e: Throwable => {
        throw e
      }
    }
    finally {
      ReadUnlock(reent_lock)
    }
    return retVal
  }

  final def getMessageBinding(msgName: String): MsgBindingInfo = {
    if (msgName == null) return null

    var retVal: MsgBindingInfo = null
    ReadLock(reent_lock)
    try {
      retVal = msgBindings.getOrElse(msgName.toLowerCase(), null)
    } catch {
      case e: Throwable => {
        throw e
      }
    }
    finally {
      ReadUnlock(reent_lock)
    }
    return retVal
  }

  // No need to implement by default
  def getDefaultSerializerDeserializer: MsgBindingInfo = null

  // No need to implement by default
  def setDefaultSerializerDeserializer(defaultSerDeser: String, serDeserOptions: Map[String, Any]): Unit = {}

  // This tuple has Message Name, Serializer Name & options.
  final def addMessageBinding(msgName: String, serName: String, options: Map[String, Any]): Unit = {
    if (msgName != null && serName != null)
      addMessageBinding(Map(msgName ->(serName, options)))
  }

  // This tuple has Message Name, Serializer Name & options.
  final def addMessageBinding(binding: (String, String, Map[String, Any])): Unit = {
    if (binding != null)
      addMessageBinding(Map(binding._1 ->(binding._2, binding._3)))
  }

  // This tuple has Message Name, Serializer Name & options.
  final def addMessageBinding(bindings: Array[(String, String, Map[String, Any])]): Unit = {
    if (bindings == null || bindings.size == 0) return
    addMessageBinding(bindings.map(b => (b._1, (b._2, b._3))).toMap)
  }

  // This tuple has Message Name, Serializer Name & options.
  final def addMessageBinding(bindings: Map[String, (String, Map[String, Any])]): Unit = {
    if (bindings == null || bindings.size == 0) return

    val resolvedBindings = resolveBindings(bindings)

    if (resolvedBindings.size > 0) {
      WriteLock(reent_lock)
      try {
        msgBindings ++= resolvedBindings
      } catch {
        case e: Throwable => {
          throw e
        }
      }
      finally {
        WriteUnlock(reent_lock)
      }
      if (logger.isInfoEnabled && bindings.size > 0) {
        logger.info("For adapter %s adding new adapter bindings:%s. Total bindings:%s".format(getAdapterName, bindings.map(b => (b._1, b._2._1)).mkString("~"), msgBindings.map(b => (b._1, b._2.serName)).mkString("~")))
      }
    }
  }

  /*
    // This tuple has Message Name, Serializer Name.
    final def removeMessageBinding(msgName: String, serName: String): Unit = {
      if (msgName != null && serName != null)
        removeMessageBinding(Array((msgName, serName)))
    }

    // This tuple has Message Name, Serializer Name.
    final def removeMessageBinding(binding: (String, String)): Unit = {
      if (binding != null)
        removeMessageBinding(Array(binding))
    }

    // This tuple has Message Name, Serializer Name.
    final def removeMessageBinding(bindings: Array[(String, String)]): Unit = {
      if (bindings == null) return

      WriteLock(reent_lock)
      try {
        msgBindings --= bindings.map(b => b._1)
      } catch {
        case e: Throwable => {
          throw e
        }
      }
      finally {
        WriteUnlock(reent_lock)
      }
    }
  */

  // This tuple has Message Name, Serializer Name.
  final def removeMessageBinding(msgName: String): Unit = {
    if (msgName != null)
      removeMessageBinding(Array(msgName))
  }

  // This tuple has Message Name, Serializer Name.
  final def removeMessageBinding(msgNames: Array[String]): Unit = {
    if (msgNames == null) return

    if (msgNames.size > 0) {
      val lcMsgNames = msgNames.map(m => m.toLowerCase())
      WriteLock(reent_lock)
      try {
        msgBindings --= lcMsgNames
      } catch {
        case e: Throwable => {
          throw e
        }
      }
      finally {
        WriteUnlock(reent_lock)
      }
      if (logger.isInfoEnabled) {
        logger.info("For adapter %s removing adapter bindings:%s. Remaining bindings:%s".format(getAdapterName, msgNames.mkString(","), msgBindings.map(b => (b._1, b._2.serName)).mkString("~")))
      }
    }
  }

  // Returns serialized msgs, serialized msgs data & serializers names applied on these messages.
  def serialize(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): (Array[ContainerInterface], Array[Array[Byte]], Array[String]) = {
    if (outputContainers == null || outputContainers.size == 0) return (Array[ContainerInterface](), Array[Array[Byte]](), Array[String]())

    val serOutputContainers = ArrayBuffer[ContainerInterface]()
    val serializedContainerData = ArrayBuffer[Array[Byte]]()
    val usedSerializersNames = ArrayBuffer[String]()

    // We are going thru getAllMessageBindings and get from it ourself?. So one read lock for every serialize fintion
    val allMsgBindings = getAllMessageBindings

    outputContainers.map(c => {
      val tmpSer = allMsgBindings.getOrElse(c.getFullTypeName.toLowerCase(), null)
      val ser = if (tmpSer != null) tmpSer else if (getDefaultSerializerDeserializer != null) getDefaultSerializerDeserializer else null
      if (ser != null && ser.serInstance != null) {
        try {
          val serData = ser.serInstance.serialize(c)
          serOutputContainers += c
          serializedContainerData += serData
          usedSerializersNames += ser.serName
        } catch {
          case e: Throwable => {
            throw new KamanjaException("Failed to Serialize " + c.getFullTypeName, e)
          }
        }
      } else {
        val adapName = getAdapterName
        logger.error(s"Did not find/load Serializer for container/message:${c.getFullTypeName} @Adapter:${adapName}")
      }
    })

    (serOutputContainers.toArray, serializedContainerData.toArray, usedSerializersNames.toArray)
  }

  //    // Returns serialized msgs, serialized msgs data & serializers names applied on these messages.
  //    def serialize(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface], serializersNames: Array[String]): (Array[ContainerInterface], Array[Array[Byte]], Array[String]) = {
  //      if ((outputContainers == null || outputContainers.size == 0) && (serializersNames == null || serializersNames.size == 0)) return (Array[ContainerInterface](), Array[Array[Byte]](), Array[String]())
  //
  //      if (((outputContainers == null || outputContainers.size == 0) && !(serializersNames == null || serializersNames.size == 0)) ||
  //        (!(outputContainers == null || outputContainers.size == 0) && (serializersNames == null || serializersNames.size == 0)))
  //        throw new KamanjaException("Invalid input sizes", null)
  //
  //      val serOutputContainers = ArrayBuffer[ContainerInterface]()
  //      val serializedContainerData = ArrayBuffer[Array[Byte]]()
  //      val usedSerializersNames = ArrayBuffer[String]()
  //
  //      //FIXME:- yet to fix it
  //
  //      (serOutputContainers.toArray, serializedContainerData.toArray, usedSerializersNames.toArray)
  //    }

  // Returns deserialized msg, deserialized msg data & deserializer name, message name applied.
  def deserialize(data: Array[Byte]): (ContainerInterface, String, String) = {
    logger.debug("called deserialize")
    // We are going thru getAllMessageBindings and get from it ourself?. So one read lock for every serialize fintion
    val allMsgBindings = getAllMessageBindings
    if (allMsgBindings.size == 0 && getDefaultSerializerDeserializer == null) {
      val msg = "Not found any bindings to deserialize (including default serializer)"
      logger.debug(msg)
      throw new KamanjaException(msg, null)
    }

    if (allMsgBindings.size != 1) {
      throw new KamanjaException("We can not deserialize more than one message from input adapter. Found:" + allMsgBindings.map(b => b._1), null)
    }

    val tmpSer = allMsgBindings.values.head
    val ser = if (tmpSer != null) tmpSer else if (getDefaultSerializerDeserializer != null) getDefaultSerializerDeserializer else null
    val msgName = allMsgBindings.keys.head
    if (ser != null && ser.serInstance != null) {
      try {
        val container = ser.serInstance.deserialize(data, msgName)
        if (container == null)
          logger.error("Deserialize returned null container.")
        return (container, ser.serName, msgName)
      } catch {
        case e: Throwable => {
          throw new KamanjaException("Failed to Deserialize " + msgName, e)
        }
      }
    } else {
      val adapName = getAdapterName
      val msg = s"Did not find/load Serializer for container/message:${msgName} @Adapter:${adapName}"
      logger.error(msg)
      throw new KamanjaException(msg, null)
    }
  }

  private def getTypeForSchemaId(schemaId: Int): String = {
    val contOpt = objectResolver.getMdMgr.ContainerForSchemaId(schemaId.toInt)
    if (contOpt == None)
      return null
    contOpt.get.FullName
  }

  // Returns deserialized msg, deserialized msg data & deserializer name applied.
  def deserialize(data: Array[Byte], deserializerName: String, schemaId: Int, defaultMsgName: String): (ContainerInterface, String) = {
    var msgName = getTypeForSchemaId(schemaId)
    if (msgName == null) {
      if (defaultMsgName == null) {
        val msg = s"Did not find container/message for schemaid:${schemaId} and also does not have default container/message name"
        logger.error(msg)
        throw new KamanjaException(msg, null)
      } else {
        val msg = s"Did not find container/message for schemaid:${schemaId}. But found default container/message name as ${defaultMsgName}. There may be issues while deserializing into default name."
        logger.error(msg)
        msgName = defaultMsgName
      }
    }

    val tmpSer = getMessageBinding(msgName)
    val ser = if (tmpSer != null) tmpSer else if (getDefaultSerializerDeserializer != null) getDefaultSerializerDeserializer else null
    if (ser != null && ser.serInstance != null) {
      try {
        val container = ser.serInstance.deserialize(data, msgName)
        return (container, ser.serName)
      } catch {
        case e: Throwable => {
          throw e
        }
      }
    } else {
      val adapName = getAdapterName
      val msg = s"Did not find/load Serializer for container/message:${msgName} of schemaid:${schemaId} (including default serializer) @Adapter:${adapName}"
      logger.error(msg)
      throw new KamanjaException(msg, null)
    }
  }
}


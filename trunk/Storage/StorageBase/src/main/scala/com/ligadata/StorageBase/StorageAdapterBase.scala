/*
 * This interface is primarily used for implementing storage adapters where
 * row data is partitioned by a time stamp column and also contains one or more key
 * columns
 *
 */
package com.ligadata.StorageBase

import com.ligadata.Exceptions.{StackTrace, KamanjaException, NotImplementedFunctionException, InvalidArgumentException}
import com.ligadata.HeartBeat.{MonitorComponentInfo, Monitorable}
import com.ligadata.KamanjaBase._
import com.ligadata.KvBase.{ Key, Value, TimeRange }
import com.ligadata.Utils.{ KamanjaLoaderInfo }
import com.ligadata.kamanja.metadata.AdapterInfo

import scala.collection.mutable.ArrayBuffer

trait DataStoreOperations extends AdaptersSerializeDeserializers {
  // update operations, add & update semantics are different for relational databases

  override def getAdapterName: String = ""

  def putContainers(tnxCtxt: TransactionContext, container: ContainerInterface): Unit = {
    if (container == null)
      throw new InvalidArgumentException("container should not be null", null)
    putContainers(tnxCtxt, Array(container))
  }

  def putContainers(tnxCtxt: TransactionContext, containers: Array[ContainerInterface]): Unit = {
    if (containers == null)
      throw new InvalidArgumentException("containers should not be null", null)
    if (containers.size == 0) return

    val data = ArrayBuffer[(Key, String, Any)]()

    val data_list = containers.groupBy(_.getFullTypeName.toLowerCase).map(oneContainerData => {
      (oneContainerData._1, oneContainerData._2.map(container => {
        (Key(container.TimePartitionData(), container.PartitionKeyData(), container.TransactionId(), container.RowNumber()), "", container.asInstanceOf[Any])
      }))
    }).toArray

    put(tnxCtxt, data_list)
  }

  // sending multiple container at the same time
  def putContainers(tnxCtxt: TransactionContext, containers: Array[(String, Array[ContainerInterface])]): Unit = {
    if (containers == null)
      throw new InvalidArgumentException("containers should not be null", null)
    if (containers.size == 0) return

    val data = ArrayBuffer[(Key, String, Any)]()

    val data_list = containers.map(oneContainerData => {
      (oneContainerData._1, oneContainerData._2.map(container => {
        (Key(container.TimePartitionData(), container.PartitionKeyData(), container.TransactionId(), container.RowNumber()), "", container.asInstanceOf[Any])
      }))
    })

    put(tnxCtxt, data_list)
  }

  // value could be ContainerInterface or Array[Byte]
  def put(tnxCtxt: TransactionContext, containerName: String, key: Key, serializerTyp: String, value: Any): Unit = {
    if (containerName == null || key == null || serializerTyp == null || value == null)
      throw new InvalidArgumentException("ContainerName, Keys, SerializerTyps and Values should not be null", null)
    put(tnxCtxt, Array((containerName, Array((key, serializerTyp, value)))))
  }

  // value could be ContainerInterface or Array[Byte]
  def put(tnxCtxt: TransactionContext, containerName: String, keys: Array[Key], serializerTyps: Array[String], values: Array[Any]): Unit = {
    if (containerName == null || keys == null || serializerTyps == null || values == null)
      throw new InvalidArgumentException("Keys, SerializerTyps and Values should not be null", null)

    if (keys.size != serializerTyps.size || serializerTyps.size != values.size)
      throw new InvalidArgumentException("Keys:%d, SerializerTyps:%d and Values:%d should have same size".format(keys.size, serializerTyps.size, values.size), null)

    val data = ArrayBuffer[(Key, String, Any)]()

    for (i <- 0 until keys.size) {
      data += ((keys(i), serializerTyps(i), values(i)))
    }
    put(tnxCtxt, Array((containerName, data.toArray)))
  }

  // data_list has List of container names, and each container has list of key & value as ContainerInterface or Array[Byte]
  def put(tnxCtxt: TransactionContext, data_list: Array[(String, Array[(Key, String, Any)])]): Unit = {
    if (data_list == null)
      throw new InvalidArgumentException("Data should not be null", null)

    val putData = data_list.map(oneContainerData => {
      val containerData: Array[(com.ligadata.KvBase.Key, com.ligadata.KvBase.Value)] = oneContainerData._2.map(row => {
        if (row._3.isInstanceOf[ContainerInterface]) {
          val cont = row._3.asInstanceOf[ContainerInterface]
          val (containers, serData, serializers) = serialize(tnxCtxt, Array(cont))
          if (containers == null || containers.size == 0) {
            // throw new KamanjaException("Failed to serialize container/message:" + cont.getFullTypeName, null)
            // Ignoring these rows later
            (null, Value(0, null, null))
          } else {
            (row._1, Value(cont.getSchemaId, serializers(0), serData(0)))
          }
        } else {
          (row._1, Value(0, row._2, row._3.asInstanceOf[Array[Byte]]))
        }
      }).filter(row => row._1 != null || row._2.serializedInfo != null || row._2.serializerType != null || row._2.schemaId != 0)
      (oneContainerData._1, containerData)
    }).filter(oneContainerData => oneContainerData._2.size > 0)

    if (putData.size > 0)
      put(putData)
  }

  // update operations, add & update semantics are different for relational databases
  /* protected */ def put(containerName: String, key: Key, value: Value): Unit
  // def put(containerName: String, data_list: Array[(Key, Value)]): Unit
  /* protected */ def put(data_list: Array[(String, Array[(Key, Value)])]): Unit // data_list has List of container names, and each container has list of key & value

  // delete operations
  def del(containerName: String, keys: Array[Key]): Unit // For the given keys, delete the values
  def del(containerName: String, time: TimeRange, keys: Array[Array[String]]): Unit // For the given multiple bucket key strings, delete the values with in given date range
  def del(containerName: String, time: TimeRange): Unit // For the given date range, delete the values /*Added by Yousef Abu Elbeh at 2016-03-13*/
  // get operations
  // Returns Key, Either[ContainerInterface, Array[Byte]], serializerTyp, TypeName, Version
  def get(containerName: String, callbackFunction: (Key, Any, String, String, Int) => Unit): Unit = {
    val getCallbackFn = (k: Key, v: Value) => {
      if (callbackFunction != null) {
        if (v.schemaId > 0 /* && v.serializerType != null && v.serializerType.size > 0 */) {
          val typFromSchemaId = "" // schemaId
          val (cont, deserializerName) = deserialize(v.serializedInfo, v.serializerType, v.schemaId)
          callbackFunction(k, cont, v.serializerType, deserializerName, 0)
        } else {
          callbackFunction(k, v.serializedInfo, v.serializerType, null, 0)
        }
      }
    }
    get(containerName, if (callbackFunction != null) getCallbackFn else null)
  }

  // Returns Key, Either[ContainerInterface, Array[Byte]], serializerTyp, TypeName, Version
  def get(containerName: String, keys: Array[Key], callbackFunction: (Key, Any, String, String, Int) => Unit): Unit = {
    val getCallbackFn = (k: Key, v: Value) => {
      if (callbackFunction != null) {
        if (v.schemaId > 0 /* && v.serializerType != null && v.serializerType.size > 0 */) {
          val (cont, deserializerName) = deserialize(v.serializedInfo, v.serializerType, v.schemaId)
          callbackFunction(k, cont, v.serializerType, deserializerName, 0)
        } else {
          callbackFunction(k, v.serializedInfo, v.serializerType, null, 0)
        }
      }
    }
    get(containerName, keys, getCallbackFn)
  }

  // Returns Key, Either[ContainerInterface, Array[Byte]], serializerTyp, TypeName, Version
  def get(containerName: String, timeRanges: Array[TimeRange], callbackFunction: (Key, Any, String, String, Int) => Unit): Unit  = {
    val getCallbackFn = (k: Key, v: Value) => {
      if (callbackFunction != null) {
        if (v.schemaId > 0 /* && v.serializerType != null && v.serializerType.size > 0 */) {
          val (cont, deserializerName) = deserialize(v.serializedInfo, v.serializerType, v.schemaId)
          callbackFunction(k, cont, v.serializerType, deserializerName, 0)
        } else {
          callbackFunction(k, v.serializedInfo, v.serializerType, null, 0)
        }
      }
    }
    get(containerName, timeRanges, getCallbackFn)
  }

  // Returns Key, Either[ContainerInterface, Array[Byte]], serializerTyp, TypeName, Version
  def get(containerName: String, timeRanges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key, Any, String, String, Int) => Unit): Unit  = {
    val getCallbackFn = (k: Key, v: Value) => {
      if (callbackFunction != null) {
        if (v.schemaId > 0 /* && v.serializerType != null && v.serializerType.size > 0 */) {
          val (cont, deserializerName) = deserialize(v.serializedInfo, v.serializerType, v.schemaId)
          callbackFunction(k, cont, v.serializerType, deserializerName, 0)
        } else {
          callbackFunction(k, v.serializedInfo, v.serializerType, null, 0)
        }
      }
    }
    get(containerName, timeRanges, bucketKeys, getCallbackFn)
  }

  // Returns Key, Either[ContainerInterface, Array[Byte]], serializerTyp, TypeName, Version
  def get(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key, Any, String, String, Int) => Unit): Unit  = {
    val getCallbackFn = (k: Key, v: Value) => {
      if (callbackFunction != null) {
        if (v.schemaId > 0 /* && v.serializerType != null && v.serializerType.size > 0 */) {
          val (cont, deserializerName) = deserialize(v.serializedInfo, v.serializerType, v.schemaId)
          callbackFunction(k, cont, v.serializerType, deserializerName, 0)
        } else {
          callbackFunction(k, v.serializedInfo, v.serializerType, null, 0)
        }
      }
    }
    get(containerName, bucketKeys, getCallbackFn)
  }

  /* protected */ def get(containerName: String, callbackFunction: (Key, Value) => Unit): Unit
  /* protected */ def get(containerName: String, keys: Array[Key], callbackFunction: (Key, Value) => Unit): Unit
  /* protected */ def get(containerName: String, timeRanges: Array[TimeRange], callbackFunction: (Key, Value) => Unit): Unit // Range of dates
  /* protected */ def get(containerName: String, timeRanges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key, Value) => Unit): Unit
  /* protected */ def get(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key, Value) => Unit): Unit

  /*
  // Passing filter to storage
  def get(containerName: String, filterFunction: (Key, Value) => Boolean, callbackFunction: (Key, Value) => Unit): Unit
  def get(containerName: String, timeRanges: Array[TimeRange], filterFunction: (Key, Value) => Boolean, callbackFunction: (Key, Value) => Unit): Unit // Range of dates
  def get(containerName: String, timeRanges: Array[TimeRange], bucketKeys: Array[Array[String]], filterFunction: (Key, Value) => Boolean, callbackFunction: (Key, Value) => Unit): Unit
  def get(containerName: String, bucketKeys: Array[Array[String]], filterFunction: (Key, Value) => Boolean, callbackFunction: (Key, Value) => Unit): Unit
*/

  // getKeys operations similar to get, but only key values
  def getKeys(containerName: String, callbackFunction: (Key) => Unit): Unit
  def getKeys(containerName: String, keys: Array[Key], callbackFunction: (Key) => Unit): Unit
  def getKeys(containerName: String, timeRanges: Array[TimeRange], callbackFunction: (Key) => Unit): Unit // Range of dates
  def getKeys(containerName: String, timeRanges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key) => Unit): Unit
  def getKeys(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key) => Unit): Unit

  def copyContainer(srcContainerName: String, destContainerName: String, forceCopy: Boolean): Unit
  def isContainerExists(containerName: String): Boolean

  def backupContainer(containerName: String): Unit
  def restoreContainer(containerName: String): Unit

  def getAllTables: Array[String] //namespace or schemaName is assumed to be attached to adapter object
  def dropTables(tbls: Array[String]) // here tbls are full qualified name (including namespace)
  def dropTables(tbls: Array[(String, String)])
  def copyTable(srcTableName: String, destTableName: String, forceCopy: Boolean): Unit // here srcTableName & destTableName are full qualified name (including namespace)
  def copyTable(namespace: String, srcTableName: String, destTableName: String, forceCopy: Boolean): Unit
  def isTableExists(tableName: String): Boolean // here tableName is full qualified name (including namespace)
  def isTableExists(tableNamespace: String, tableName: String): Boolean
  def getTableName(containerName: String): String
}

trait DataStore extends DataStoreOperations with AdaptersSerializeDeserializers with Monitorable  {
  val nodeCtxt: NodeContext
  val adapterInfo: AdapterInfo

  // NodeContext
  val _TYPE_STORAGE = "Storage_Adapter"
  val _startTime: String = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))

  override final def getAdapterName = if (adapterInfo != null) adapterInfo.Name else ""

  var _defaultSerDeserName: String = null
  var _defaultSerDeser: MsgBindingInfo = null
  var _serDeserOptions: Map[String, Any] = null
  var _getOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()

  final override def getDefaultSerializerDeserializer: MsgBindingInfo = _defaultSerDeser

  final override def setDefaultSerializerDeserializer(defaultSerDeser: String, serDeserOptions: Map[String, Any]): Unit = {
    _defaultSerDeser = null
    _defaultSerDeserName = null
    _serDeserOptions = null
    _defaultSerDeser = resolveBinding(defaultSerDeser, serDeserOptions)
    if (_defaultSerDeser != null) {
      _defaultSerDeserName = defaultSerDeser
      _serDeserOptions = serDeserOptions
    }
  }

  def Category = "Storage"

  def externalizeExceptionEvent (cause: Throwable): Unit = {
    try {
      val exceptionEvent = nodeCtxt.getEnvCtxt.getContainerInstance("com.ligadata.KamanjaBase.KamanjaExceptionEvent").asInstanceOf[com.ligadata.KamanjaBase.KamanjaExceptionEvent]
      exceptionEvent.timeoferrorepochms = System.currentTimeMillis()
      exceptionEvent.componentname = adapterInfo.Name
      exceptionEvent.errortype = "exception"
      exceptionEvent.errorstring = StackTrace.ThrowableTraceString(cause)
      nodeCtxt.getEnvCtxt.postMessages(Array[ContainerInterface](exceptionEvent))
    } catch {
      case t: Throwable => {
        return
      }
    }
  }


  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    val lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
    logger.debug("component stats => " + getComponentSimpleStats)
    MonitorComponentInfo(_TYPE_STORAGE, if (adapterInfo != null) adapterInfo.Name else "", if (adapterInfo != null) adapterInfo.Name else "", _startTime, lastSeen, "{" + getComponentSimpleStats + "}")
  }

  override def getComponentSimpleStats: String = {
    var s:String = ""
    _getOps.keys.foreach( k => { 
      s = s + "Storage/"+getAdapterName+"/getOps" + "->" + "("+ k + ":" + _getOps(k) +")"
    })
    _putOps.keys.foreach( k => { 
      s = s + ",Storage/"+getAdapterName+"/putOps" + "->" + "("+ k + ":" + _putOps(k) +")"
    })
    _getObjs.keys.foreach( k => { 
      s = s + "Storage/"+getAdapterName+"/getObjs" + "->" + "("+ k + ":" + _getObjs(k) +")"
    })
    _putObjs.keys.foreach( k => { 
      s = s + ",Storage/"+getAdapterName+"/putObjs" + "->" + "("+ k + ":" + _putObjs(k) +")"
    })
    _getBytes.keys.foreach( k => { 
      s =s + ",Storage/"+getAdapterName+"/getBytes" + "->" + "("+ k + ":" + _getBytes(k) +")"
    })
    _putBytes.keys.foreach( k => { 
      s= s + ",Storage/"+getAdapterName+"/putBytes" + "->" + "("+ k + ":" + _putBytes(k) +")"
    })
    s
  }

  def beginTx(): Transaction
  def endTx(tx: Transaction): Unit // Same as commit
  def commitTx(tx: Transaction): Unit
  def rollbackTx(tx: Transaction): Unit

  // clean up operations
  def Shutdown(): Unit
  def TruncateContainer(containerNames: Array[String]): Unit
  def DropContainer(containerNames: Array[String]): Unit
  def CreateContainer(containerNames: Array[String]): Unit
  def CreateMetadataContainer(containerNames: Array[String]): Unit
}

trait Transaction extends DataStoreOperations {
  val parent: DataStore // Parent Data Store
}

// Storage Adapter Object to create storage adapter
trait StorageAdapterFactory {
  def CreateStorageAdapter(kvManagerLoader: KamanjaLoaderInfo, datastoreConfig: String, nodeCtxt: NodeContext, adapterInfo: AdapterInfo): DataStore
}


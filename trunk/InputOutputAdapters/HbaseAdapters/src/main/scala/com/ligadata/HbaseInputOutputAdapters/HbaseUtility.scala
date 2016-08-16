package com.ligadata.HbaseInputOutputAdapters

import com.ligadata.Exceptions.{ConnectionFailedException, FatalAdapterException, KamanjaException}
import com.ligadata.KamanjaBase.{ContainerInterface, SerializeDeserialize}
import com.ligadata.KvBase.{Key, TimeRange, Value}
import com.ligadata.adapterconfiguration.HbaseAdapterConfiguration
import com.ligadata.kamanja.metadata.MdMgr
import com.ligadata.kamanja.metadata.MdMgr._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.logging.log4j.LogManager
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Formats}

import scala.collection.mutable.ArrayBuffer
/**
  * Created by Yousef on 8/13/2016.
  */
trait LogTrait {
  val loggerName = this.getClass.getName()
  val logger = LogManager.getLogger(loggerName)
}

class HbaseUtility /*extends LogTrait*/{
  val hbaseConfig = new Configuration()
  var ugi = new UserGroupInformation()
  var conn: Connection=_
  var _getOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  private val siStrBytes = "serializedInfo".getBytes()
  private val baseStrBytes = "base".getBytes()
  private val siStrBytesLen = siStrBytes.length
  private val stStrBytes = "serializerType".getBytes()
  private val stStrBytesLen = stStrBytes.length
  private val schemaIdStrBytes = "schemaId".getBytes()
  private val schemaIdStrBytesLen = schemaIdStrBytes.length
  private[this] val lock = new Object
  var tableName: String=_
  var namespace: String=_
  var fulltableName: String=_
  var serName: String=_

  def createConnection(adapterConfig: HbaseAdapterConfiguration): Configuration = {
    hbaseConfig.setInt("zookeeper.session.timeout", 10000)
    hbaseConfig.setInt("zookeeper.recovery.retry", 1)
    hbaseConfig.setInt("hbase.client.retries.number", 1)
    hbaseConfig.setInt("hbase.client.pause", 10000)
    hbaseConfig.set("hbase.zookeeper.quorum", adapterConfig.host)
    if (adapterConfig.kerberos != null) {
      hbaseConfig.set("hadoop.security.authorization", "true")
      hbaseConfig.set("hadoop.proxyuser.hdfs.groups", "*")
      hbaseConfig.set("hadoop.security.authentication", "kerberos")
      hbaseConfig.set("hbase.security.authentication", "kerberos")
      //hbaseConfig.set("hbase.master.kerberos.principal", masterPrincipal)
      //hbaseConfig.set("hbase.regionserver.kerberos.principal", regionServer)
      org.apache.hadoop.security.UserGroupInformation.setConfiguration(hbaseConfig)
      UserGroupInformation.loginUserFromKeytab(adapterConfig.kerberos.principal, adapterConfig.kerberos.keytab)
      ugi = UserGroupInformation.getLoginUser()
    }
    hbaseConfig
  }

  def relogin(): Unit ={// not tested
    try {
      if (ugi != null) {
        ugi.checkTGTAndReloginFromKeytab()
      }
    } catch  {
      case e: Exception => throw FatalAdapterException ("Failed to relogin for Hbase using Hbase Producer: ", new Exception("Relogin Faild"))
    }
  }

  def getConnection() : Connection={
    val conn = ConnectionFactory.createConnection(hbaseConfig)
    conn
  }

  def setConnection(connect: Connection) : Unit={
    conn = connect
  }

  def closeConnection(): Unit={
    conn.close()
  }
  def createNamespace(): Unit={
    relogin()
    try {
      val desc = conn.getAdmin.getNamespaceDescriptor(namespace)
    } catch{
      case e: Exception => return
    }
    try{
      conn.getAdmin.createNamespace(NamespaceDescriptor.create(namespace).build())
    } catch {
      case e: Exception => throw FatalAdapterException("Failed to create namespace: " + namespace, new Exception("Fiald on create"))
    }
  }

  def getData(): Result ={
    try {
      relogin()
      var hTable = new HTable(hbaseConfig, tableName)
      val get = new Get(Bytes.toBytes(""))//check this bug
      val result = hTable.get(get)
      result
    } catch{
      case e: Exception => throw FatalAdapterException("Failed to get data into table: "+ tableName, new Exception("Fiald on read"))
    }
  }

  def toTableName(): String = {
    // we need to check for other restrictions as well
    // such as length of the table, special characters etc
    namespace + ':' + tableName.toLowerCase.replace('.', '_').replace('-', '_').replace(' ', '_')
  }

  private def getTableFromConnection(): Table = {
    try {
      relogin
      return conn.getTable(TableName.valueOf(fulltableName))
    } catch {
      case e: Exception => {
        throw ConnectionFailedException("Failed to get table " + fulltableName, e)
      }
    }

    return null
  }

  private def updateOpStats(operation: String,  tableName: String, opCount: Int) : Unit = lock.synchronized{
    operation match {
      case "get" => {
        if( _getOps.get(tableName) != None ){
          _getOps(tableName) = _getOps(tableName) + opCount
        }
        else{
          _getOps(tableName) = + opCount
        }
      }
      case "put" => {
        if( _putOps.get(tableName) != None ){
          _putOps(tableName) = _putOps(tableName) + opCount
        }
        else{
          _putOps(tableName) = opCount
        }
      }
      case _ => {
        throw FatalAdapterException("Internal Error: Failed to Update Op-stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def getRowSize(r: Result) : Int = {
    var keySize     = r.getRow().length
    var valSize     = 0
    if( r.containsNonEmptyColumn(siStrBytes, baseStrBytes) ){
      valSize = r.getValue(siStrBytes, baseStrBytes).length
    }
    keySize + valSize
  }

  private def updateObjStats(operation: String, tableName: String, objCount: Int) : Unit = lock.synchronized{
    operation match {
      case "get" => {
        if( _getObjs.get(tableName) != None ){
          _getObjs(tableName) = _getObjs(tableName) + objCount
        }
        else{
          _getObjs(tableName) = + objCount
        }
      }
      case "put" => {
        if( _putObjs.get(tableName) != None ){
          _putObjs(tableName) = _putObjs(tableName) + objCount
        }
        else{
          _putObjs(tableName) = objCount
        }
      }
      case _ => {
        throw FatalAdapterException("Internal Error: Failed to Update Obj-stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def updateByteStats(operation: String, tableName: String, byteCount: Int) : Unit = lock.synchronized{
    operation match {
      case "get" => {
        if( _getBytes.get(tableName) != None ){
          _getBytes(tableName) = _getBytes(tableName) + byteCount
        }
        else{
          _getBytes(tableName) = byteCount
        }
      }
      case "put" => {
        if( _putBytes.get(tableName) != None ){
          _putBytes(tableName) = _putBytes(tableName) + byteCount
        }
        else{
          _putBytes(tableName) = byteCount
        }
      }
      case _ => {
        throw FatalAdapterException("Internal Error: Failed to Update Byte Stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def MakeBucketKeyFromByteArr(keyBytes: Array[Byte], startIdx: Int): (Array[String], Int) = {
    if (keyBytes.size > startIdx) {
      var cntr = startIdx
      val cnt = (0xff & keyBytes(cntr).toInt)
      cntr += 1

      val bucketKey = new Array[String](cnt)
      for (i <- 0 until cnt) {
        val b1 = keyBytes(cntr)
        cntr += 1
        val b2 = keyBytes(cntr)
        cntr += 1

        val sz = ((0xff & b1.asInstanceOf[Int]) << 8) + ((0xff & b2.asInstanceOf[Int]) << 0)
        bucketKey(i) = new String(keyBytes, cntr, sz)
        cntr += sz
      }

      (bucketKey, (cntr - startIdx))
    } else {
      (Array[String](), 0)
    }
  }

  private def MakeBucketKeyFromByteArr(keyBytes: Array[Byte]): Array[String] = {
    val (bucketKey, consumedBytes) = MakeBucketKeyFromByteArr(keyBytes, 0)
    bucketKey
  }

  private def GetKeyFromCompositeKey(compKey: Array[Byte], isMetadataContainer: Boolean): Key = {
    if( isMetadataContainer ){
      var cntr = 0
      val k = new String(compKey)
      val bucketKey = new Array[String](1)
      bucketKey(0) = k
      new Key(0, bucketKey, 0, 0)
    }
    else{
      var cntr = 0
      val tp_b1 = compKey(cntr)
      cntr += 1
      val tp_b2 = compKey(cntr)
      cntr += 1
      val tp_b3 = compKey(cntr)
      cntr += 1
      val tp_b4 = compKey(cntr)
      cntr += 1
      val tp_b5 = compKey(cntr)
      cntr += 1
      val tp_b6 = compKey(cntr)
      cntr += 1
      val tp_b7 = compKey(cntr)
      cntr += 1
      val tp_b8 = compKey(cntr)
      cntr += 1

      val timePartition =
        (((0xff & tp_b1.asInstanceOf[Long]) << 56) + ((0xff & tp_b2.asInstanceOf[Long]) << 48) +
          ((0xff & tp_b3.asInstanceOf[Long]) << 40) + ((0xff & tp_b4.asInstanceOf[Long]) << 32) +
          ((0xff & tp_b5.asInstanceOf[Long]) << 24) + ((0xff & tp_b6.asInstanceOf[Long]) << 16) +
          ((0xff & tp_b7.asInstanceOf[Long]) << 8) + ((0xff & tp_b8.asInstanceOf[Long]) << 0))

      val (bucketKey, consumedBytes) = MakeBucketKeyFromByteArr(compKey, cntr)
      cntr += consumedBytes

      val tx_b1 = compKey(cntr)
      cntr += 1
      val tx_b2 = compKey(cntr)
      cntr += 1
      val tx_b3 = compKey(cntr)
      cntr += 1
      val tx_b4 = compKey(cntr)
      cntr += 1
      val tx_b5 = compKey(cntr)
      cntr += 1
      val tx_b6 = compKey(cntr)
      cntr += 1
      val tx_b7 = compKey(cntr)
      cntr += 1
      val tx_b8 = compKey(cntr)
      cntr += 1

      val transactionId =
        (((0xff & tx_b1.asInstanceOf[Long]) << 56) + ((0xff & tx_b2.asInstanceOf[Long]) << 48) +
          ((0xff & tx_b3.asInstanceOf[Long]) << 40) + ((0xff & tx_b4.asInstanceOf[Long]) << 32) +
          ((0xff & tx_b5.asInstanceOf[Long]) << 24) + ((0xff & tx_b6.asInstanceOf[Long]) << 16) +
          ((0xff & tx_b7.asInstanceOf[Long]) << 8) + ((0xff & tx_b8.asInstanceOf[Long]) << 0))

      val rowid_b1 = compKey(cntr)
      cntr += 1
      val rowid_b2 = compKey(cntr)
      cntr += 1
      val rowid_b3 = compKey(cntr)
      cntr += 1
      val rowid_b4 = compKey(cntr)
      cntr += 1

      val rowId =
        (((0xff & rowid_b1.asInstanceOf[Int]) << 24) + ((0xff & rowid_b2.asInstanceOf[Int]) << 16) +
          ((0xff & rowid_b3.asInstanceOf[Int]) << 8) + ((0xff & rowid_b4.asInstanceOf[Int]) << 0))

      new Key(timePartition, bucketKey, transactionId, rowId)
    }
  }

  private def processRow(k: Array[Byte], isMetadata: Boolean, schemaId: Int, st: String, si: Array[Byte], callbackFunction: (Key, Value) => Unit) {
    try {
      var key = GetKeyFromCompositeKey(k,isMetadata)
      // format the data to create Key/Value
      var value = new Value(schemaId, st, si)
      if (callbackFunction != null)
        (callbackFunction)(key, value)
    } catch {
      case e: Exception => {
     //   externalizeExceptionEvent(e)
        throw e
      }
    }
  }

  def get(callbackFunction: (Key, Value) => Unit): Unit = {
   // var FulltableName = toTableName(tableName, namespace)
    var tableHBase: Table = null
    try {
      relogin
     // val isMetadata = CheckTableExists(containerName)
      tableHBase = getTableFromConnection
      var scan = new Scan();
      var rs = tableHBase.getScanner(scan);
      updateOpStats("get",fulltableName,1)
      val it = rs.iterator()
      var byteCount = 0
      var recCount = 0
      while (it.hasNext()) {
        val r = it.next()
        byteCount = byteCount + getRowSize(r)
        recCount = recCount + 1
        val st = Bytes.toString(r.getValue(stStrBytes, baseStrBytes))
        val si = r.getValue(siStrBytes, baseStrBytes)
        val schemaId = Bytes.toInt(r.getValue(schemaIdStrBytes, baseStrBytes))
        processRow(r.getRow(), true /*isMetaData*/, schemaId, st, si, callbackFunction)
      }
      updateByteStats("get",fulltableName,byteCount)
      updateObjStats("get",fulltableName,recCount)
    } catch {
      case e: Exception => {
      //  externalizeExceptionEvent(e)
        throw FatalAdapterException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (tableHBase != null) {
        tableHBase.close()
      }
    }
  }

  private def MakeLongSerializedVal(l: Long): Array[Byte] = {
    val ab = new ArrayBuffer[Byte](16)
    ab += (((l >>> 56) & 0xFF).toByte)
    ab += (((l >>> 48) & 0xFF).toByte)
    ab += (((l >>> 40) & 0xFF).toByte)
    ab += (((l >>> 32) & 0xFF).toByte)
    ab += (((l >>> 24) & 0xFF).toByte)
    ab += (((l >>> 16) & 0xFF).toByte)
    ab += (((l >>> 8) & 0xFF).toByte)
    ab += (((l >>> 0) & 0xFF).toByte)

    ab.toArray
  }

  def get(time_range: Long, callbackFunction: (Key, Value) => Unit): Unit = {
    //var tableName = toTableName(tableName, namespace)
    var tableHBase: Table = null
    try {
      relogin
      //val isMetadata = CheckTableExists(containerName)
      tableHBase = getTableFromConnection

   //   val tmRanges = getUnsignedTimeRanges(time_ranges)
      var byteCount = 0
      var recCount = 0
      var opCount = 0
//      tmRanges.foreach(time_range => {
        // try scan with beginRow and endRow
        var scan = new Scan()
//        scan.setStartRow(MakeLongSerializedVal(time_range.beginTime))
//        scan.setStopRow(MakeLongSerializedVal(time_range.endTime + 1))
      scan.setStartRow(MakeLongSerializedVal(time_range))
        val rs = tableHBase.getScanner(scan);
        opCount = opCount + 1
        val it = rs.iterator()
        while (it.hasNext()) {
          val r = it.next()
          byteCount = byteCount + getRowSize(r)
          recCount = recCount + 1
          val st = Bytes.toString(r.getValue(stStrBytes, baseStrBytes))
          val si = r.getValue(siStrBytes, baseStrBytes)
          val schemaId = Bytes.toInt(r.getValue(schemaIdStrBytes, baseStrBytes))
          processRow(r.getRow(), true/*isMetadata*/, schemaId, st, si, callbackFunction)
        }
//      })
      updateByteStats("get",fulltableName,byteCount)
      updateObjStats("get",fulltableName,recCount)
      updateOpStats("get",fulltableName,opCount)
    } catch {
      case e: Exception => {
        //externalizeExceptionEvent(e)
        throw FatalAdapterException("Failed to fetch data from the table " + fulltableName, e)
      }
    } finally {
      if (tableHBase != null) {
        tableHBase.close()
      }
    }
  }

  def getKeys(time_range: TimeRange, callbackFunction: (Key) => Unit): Unit = {
    //var tableName = toTableName(tableName, namespace)
    var tableHBase: Table = null
    try {
      relogin
   //   val isMetadata = CheckTableExists(containerName)
      tableHBase = getTableFromConnection

 //     val tmRanges = getUnsignedTimeRanges(time_ranges)
      var byteCount = 0
      var recCount = 0
      var opCount = 0
//      tmRanges.foreach(time_range => {
        // try scan with beginRow and endRow
        var scan = new Scan()
        scan.setStartRow(MakeLongSerializedVal(time_range.beginTime))
        scan.setStopRow(MakeLongSerializedVal(time_range.endTime + 1))
        val rs = tableHBase.getScanner(scan);
        opCount = opCount + 1
        val it = rs.iterator()
        while (it.hasNext()) {
          val r = it.next()
          byteCount = byteCount + getRowSize(r)
          recCount = recCount + 1
          processKey(r.getRow(),true /*isMetadata*/, callbackFunction)
        }
//      })
      updateByteStats("get",fulltableName,byteCount)
      updateObjStats("get",fulltableName,recCount)
      updateOpStats("get",fulltableName,opCount)
    } catch {
      case e: Exception => {
     //   externalizeExceptionEvent(e)
        throw FatalAdapterException("Failed to fetch data from the table " + fulltableName, e)
      }
    } finally {
      if (tableHBase != null) {
        tableHBase.close()
      }
    }
  }

  private def processKey(k: Array[Byte], isMetadata: Boolean, callbackFunction: (Key) => Unit) {
    try {
      var key = GetKeyFromCompositeKey(k,isMetadata)
      if (callbackFunction != null)
        (callbackFunction)(key)
    } catch {
      case e: Exception => {
   //     externalizeExceptionEvent(e)
        throw e
      }
    }
  }

  private def processKey(key: Key, callbackFunction: (Key) => Unit) {
    try {
      if (callbackFunction != null)
        (callbackFunction)(key)
    } catch {
      case e: Exception => {
   //     externalizeExceptionEvent(e)
        throw e
      }
    }
  }

  def initilizeVariable(adapterConfig: HbaseAdapterConfiguration): Unit={
    tableName = adapterConfig.TableName
    namespace = adapterConfig.scehmaName
    fulltableName = toTableName
  }

  def getMdMgr: MdMgr = mdMgr

  private def AddBucketKeyToArrayBuffer(bucketKey: Array[String], ab: ArrayBuffer[Byte]): Unit = {
    // First one is Number of Array Elements
    // Next follows Each Element size & Element Data
    ab += ((bucketKey.size).toByte)
    bucketKey.foreach(k => {
      val kBytes = k.getBytes
      val sz = kBytes.size
      ab += (((sz >>> 8) & 0xFF).toByte)
      ab += (((sz >>> 0) & 0xFF).toByte)
      ab ++= kBytes
    })
  }

  private def MakeCompositeKey(key: Key,isMetadataContainer: Boolean): Array[Byte] = {
    if( isMetadataContainer ){
      key.bucketKey(0).getBytes
    }
    else{
      val ab = new ArrayBuffer[Byte](256)
      ab += (((key.timePartition >>> 56) & 0xFF).toByte)
      ab += (((key.timePartition >>> 48) & 0xFF).toByte)
      ab += (((key.timePartition >>> 40) & 0xFF).toByte)
      ab += (((key.timePartition >>> 32) & 0xFF).toByte)
      ab += (((key.timePartition >>> 24) & 0xFF).toByte)
      ab += (((key.timePartition >>> 16) & 0xFF).toByte)
      ab += (((key.timePartition >>> 8) & 0xFF).toByte)
      ab += (((key.timePartition >>> 0) & 0xFF).toByte)

      AddBucketKeyToArrayBuffer(key.bucketKey, ab)

      ab += (((key.transactionId >>> 56) & 0xFF).toByte)
      ab += (((key.transactionId >>> 48) & 0xFF).toByte)
      ab += (((key.transactionId >>> 40) & 0xFF).toByte)
      ab += (((key.transactionId >>> 32) & 0xFF).toByte)
      ab += (((key.transactionId >>> 24) & 0xFF).toByte)
      ab += (((key.transactionId >>> 16) & 0xFF).toByte)
      ab += (((key.transactionId >>> 8) & 0xFF).toByte)
      ab += (((key.transactionId >>> 0) & 0xFF).toByte)

      ab += (((key.rowId >>> 24) & 0xFF).toByte)
      ab += (((key.rowId >>> 16) & 0xFF).toByte)
      ab += (((key.rowId >>> 8) & 0xFF).toByte)
      ab += (((key.rowId >>> 0) & 0xFF).toByte)

      ab.toArray
    }
  }

  private def getKeySize(k: Key): Int = {
    var bucketKeySize = 0
    k.bucketKey.foreach(bk => { bucketKeySize = bucketKeySize + bk.length })
    8 + bucketKeySize + 8 + 4
  }

  private def getValueSize(v: Value): Int = {
    v.serializedInfo.length
  }

   def put(tableName: String, key: Key, value: Value): Unit = {
  //  var fulltableName = toFullTableName(containerName)
    var tableHBase: Table = null
    try {
      relogin
      //val isMetadata = CheckTableExists(containerName)
      tableHBase = getTableFromConnection();
      var kba = MakeCompositeKey(key,true/*isMetadata*/)
      var p = new Put(kba)
      p.addColumn(stStrBytes, baseStrBytes, Bytes.toBytes(value.serializerType))
      p.addColumn(siStrBytes, baseStrBytes, value.serializedInfo)
      p.addColumn(schemaIdStrBytes, baseStrBytes,Bytes.toBytes(value.schemaId))
      tableHBase.put(p)
      updateOpStats("put",fulltableName,1)
      updateObjStats("put",fulltableName,1)
      updateByteStats("put",fulltableName,getKeySize(key)+getValueSize(value))
    } catch {
      case e: Exception => {
        //externalizeExceptionEvent(e)
        throw new KamanjaException("Failed to save an object in table " + fulltableName, e)
      }
    } finally {
      if (tableHBase != null) {
        tableHBase.close()
      }
    }
  }

   def getManager(request: String, callbackFunction: (Array[Byte]) => Unit): Unit={
    var serDeser: SerializeDeserialize = null
    val serInfo = getMdMgr.GetSerializer(serName)
    if (serInfo == null) {
      throw new KamanjaException(s"Not found Serializer/Deserializer for ${serName}", null)
    }

    val phyName = serInfo.PhysicalName
    if (phyName == null) {
      throw new KamanjaException(s"Not found Physical name for Serializer/Deserializer for ${serName}", null)
    }

      val aclass = Class.forName(phyName).newInstance
      val ser = aclass.asInstanceOf[SerializeDeserialize]
//      val map = new java.util.HashMap[String, String] //BUGBUG:: we should not convert the 2nd param to String. But still need to see how can we convert scala map to java map
//      ser.configure(this, map)
//      ser.setObjectResolver(this)
      serDeser = ser

    val retriveData = (k: Key, v: Any, serializerTyp: String, tableName: String, ver: Int)=>{
      val value = v.asInstanceOf[ContainerInterface]
      if(!value.equals(null)) {
        try {
          val serData = serDeser.serialize(value)
          callbackFunction(serData)
        } catch {
          case e: Throwable => {
            throw e
          }
        }
      }
    }

     if(request.equalsIgnoreCase("getdata")){
       //get(retriveData)
     }
  }
}

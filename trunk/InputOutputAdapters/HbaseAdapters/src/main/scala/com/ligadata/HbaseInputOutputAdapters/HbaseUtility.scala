package com.ligadata.HbaseInputOutputAdapters

import com.ligadata.Exceptions.FatalAdapterException
import com.ligadata.adapterconfiguration.HbaseAdapterConfiguration
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.util.Bytes
/**
  * Created by Yousef on 8/13/2016.
  */
object HbaseUtility {
  val hbaseConfig = new Configuration()
  var ugi = new UserGroupInformation()

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

  def getConnection(): Connection={
    val conn = ConnectionFactory.createConnection(hbaseConfig)
    conn
  }

  def createNamespace(conn: Connection, namespace: String): Unit={
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

  def getData(tableName: String): Result ={
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
}

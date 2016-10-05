package com.ligadata.InputAdapters.hdfs

import com.ligadata.AdaptersConfiguration.FileAdapterConnectionConfig
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation

object HdfsUtility{
  def createConfig(connectionConf : FileAdapterConnectionConfig) : Configuration = {
    val hdfsConfig = new Configuration()
    hdfsConfig.set("fs.default.name", connectionConf.hostsList.mkString(","))
    hdfsConfig.set("fs.hdfs.impl", classOf[org.apache.hadoop.hdfs.DistributedFileSystem].getName)
    hdfsConfig.set("fs.file.impl", classOf[org.apache.hadoop.fs.LocalFileSystem].getName)

    if (connectionConf.hadoopConfig != null && !connectionConf.hadoopConfig.isEmpty) {
      connectionConf.hadoopConfig.foreach(conf => {
        hdfsConfig.set(conf._1, conf._2)
      })
    }


    //hdfsConfig.set("hadoop.job.ugi", "hadoop");//user ???
    if(connectionConf.authentication.equalsIgnoreCase("kerberos")){
      hdfsConfig.set("hadoop.security.authentication", "Kerberos")
      UserGroupInformation.setConfiguration(hdfsConfig)
      UserGroupInformation.loginUserFromKeytab(connectionConf.principal, connectionConf.keytab)
    }
    hdfsConfig
  }

  def getFilePathNoProtocol(path : String) : String = {
    if(path.toLowerCase().startsWith("hdfs://")){
      val part1 = path.substring(7)
      val idx = part1.indexOf("/")
      part1.substring(idx)
    }
    else{
      path
    }
  }
}

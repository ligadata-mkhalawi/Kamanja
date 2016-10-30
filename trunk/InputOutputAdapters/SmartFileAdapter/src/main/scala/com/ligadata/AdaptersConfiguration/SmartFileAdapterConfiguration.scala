package com.ligadata.AdaptersConfiguration

import com.ligadata.Exceptions.KamanjaException
import com.ligadata.InputAdapters.MonitorUtils
import com.ligadata.InputOutputAdapterInfo._
import org.apache.logging.log4j.LogManager
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.collection.mutable.ArrayBuffer


class ArchiveConfig {
  var archiveSleepTimeInMs: Int = 10
  var archiveParallelism: Int = 1
  var outputConfig: SmartFileProducerConfiguration = null
}

/**
  * Created by Yasser on 3/10/2016.
  */
class SmartFileAdapterConfiguration extends AdapterConfiguration {
  var _type: String = _ // FileSystem, hdfs, sftp

  var connectionConfig: FileAdapterConnectionConfig = null
  var monitoringConfig: FileAdapterMonitoringConfig = null
  var archiveConfig: ArchiveConfig = null
}

class FileAdapterConnectionConfig {
  var hostsList: Array[String] = Array.empty[String]
  var userId: String = _
  var password: String = _
  var authentication: String = _
  var principal: String = _
  var keytab: String = _
  var passphrase: String = _
  var keyFile: String = _

  var hadoopConfig: List[(String, String)] = null
}

class FileAdapterMonitoringConfig {
  var waitingTimeMS: Int = _
  var dirCheckThreshold: Int = 0 //when > 0 listing watched folders should stop when count of files waiting to be processed is above the threshold

  //var locations : Array[String] = Array.empty[String] //folders to monitor, simple comma separated list
  //var targetMoveDir : String = "" //generic target dir to move files to. used when src dir has no target

  var detailedLocations: Array[LocationInfo] = Array.empty[LocationInfo] //folders to monitor, with other info


  var fileBufferingTimeout = 300
  // in seconds
  //folders to move consumed files to. either one directory for all input (locations) or same number as (locations)
  //var targetMoveDirs : Array[String] = Array.empty[String]
  var consumersCount: Int = _
  //number of threads to read files
  var monitoringThreadsCount: Int = 1
  //number of threads to monitor src dirs
  var workerBufferSize: Int = 4 //buffer size in MB to read messages from files


  var msgTags: Array[String] = Array.empty[String] //public

  var tagDelimiter: String = "," //public

  var messageSeparator: Char = 10
  var orderBy: Array[String] = Array.empty[String]
  var entireFileAsOneMessage = false
  var enableEmailAndAttachmentMode = false
  var checkFileTypes = true
  var checkFileSize = true

  var enableMoving: String = "on"

  //on, off - public
  def isMovingEnabled: Boolean = enableMoving == null || enableMoving.length == 0 || enableMoving.equalsIgnoreCase("on")

  val createInputStructureInTargetDirs = true

}

class Padding {
  //var componentName : String = ""

  var padPos: String = "left"
  //left, right
  var padSize: Int = 0
  var padStr: String = ""
}

class FileComponents {
  var components: Array[String] = Array.empty[String]
  var regex: String = ""
  var paddings: Map[String, Padding] = Map[String, Padding]()
}

class LocationInfo {

  var srcDir = ""
  var targetDir = ""

  var fileComponents: FileComponents = null
  //array of keywords, each one has a meaning to the adapter, which will add corresponding data to msg before sending to engine
  var msgTags: Array[String] = Array.empty[String]
  var tagDelimiter: String = "" //if empty get public one


  var messageSeparator: Char = 0
  //0 this means separator is not set, so get it from public attributes
  var orderBy: Array[String] = Array.empty[String]

  var enableMoving: String = ""

  //on, off, empty means get it from public attribute
  def isMovingEnabled: Boolean = enableMoving == null || enableMoving.length == 0 || enableMoving.equalsIgnoreCase("on")
}

object SmartFileAdapterConfiguration {

  val defaultWaitingTimeMS = 1000
  val defaultConsumerCount = 2
  val defaultOrderBy = Array("$FILE_MOD_TIME") //last modification time

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  val validMsgTags = Set("MsgType", "FileName", "FileFullPath")

  def isValidMsgTag(tag: String) = tag != null && (validMsgTags contains tag)

  def getAdapterConfig(inputConfig: AdapterConfiguration): SmartFileAdapterConfiguration = {

    if (inputConfig.adapterSpecificCfg == null || inputConfig.adapterSpecificCfg.size == 0) {
      val err = "Not found Type and Connection info for Smart File Adapter Config:" + inputConfig.Name
      throw new KamanjaException(err, null)
    }

    val adapterConfig = new SmartFileAdapterConfiguration()
    adapterConfig.Name = inputConfig.Name
    //    adapterConfig.formatName = inputConfig.formatName
    //    adapterConfig.validateAdapterName = inputConfig.validateAdapterName
    //    adapterConfig.failedEventsAdapterName = inputConfig.failedEventsAdapterName
    adapterConfig.className = inputConfig.className
    adapterConfig.jarName = inputConfig.jarName
    adapterConfig.dependencyJars = inputConfig.dependencyJars
    //    adapterConfig.associatedMsg = if (inputConfig.associatedMsg == null) null else inputConfig.associatedMsg.trim
    //    adapterConfig.keyAndValueDelimiter = if (inputConfig.keyAndValueDelimiter == null) null else inputConfig.keyAndValueDelimiter.trim
    //    adapterConfig.fieldDelimiter = if (inputConfig.fieldDelimiter == null) null else inputConfig.fieldDelimiter.trim
    //    adapterConfig.valueDelimiter = if (inputConfig.valueDelimiter == null) null else inputConfig.valueDelimiter.trim

    adapterConfig.adapterSpecificCfg = inputConfig.adapterSpecificCfg

    logger.debug("SmartFileAdapterConfiguration (getAdapterConfig)- inputConfig.adapterSpecificCfg==null is " +
      (inputConfig.adapterSpecificCfg == null))
    val (_type, connectionConfig, monitoringConfig, archiveConfig) = parseSmartFileAdapterSpecificConfig(inputConfig.Name, inputConfig.adapterSpecificCfg)
    adapterConfig._type = _type
    adapterConfig.connectionConfig = connectionConfig
    adapterConfig.monitoringConfig = monitoringConfig
    adapterConfig.archiveConfig = archiveConfig

    adapterConfig
  }

  def parseSmartFileAdapterSpecificConfig(adapterName: String, adapterSpecificCfgJson: String): (String, FileAdapterConnectionConfig, FileAdapterMonitoringConfig, ArchiveConfig) = {

    val adapCfg = parse(adapterSpecificCfgJson)

    if (adapCfg == null || adapCfg.values == null) {
      val err = "Not found Type and Connection info for Smart File Adapter Config:" + adapterName
      throw new KamanjaException(err, null)
    }

    val adapCfgValues = adapCfg.values.asInstanceOf[Map[String, Any]]

    if (adapCfgValues.getOrElse("Type", null) == null) {
      val err = "Not found Type for Smart File Adapter Config:" + adapterName
      throw new KamanjaException(err, null)
    }
    val _type = adapCfgValues.get("Type").get.toString

    val connectionConfig = new FileAdapterConnectionConfig()
    val monitoringConfig = new FileAdapterMonitoringConfig()

    if (adapCfgValues.getOrElse("ConnectionConfig", null) == null) {
      val err = "Not found ConnectionConfig for Smart File Adapter Config:" + adapterName
      throw new KamanjaException(err, null)
    }

    var simpleLocations: Array[String] = Array.empty[String] //folders to monitor, simple comma separated list
    var publicTargetMoveDir: String = "" //generic target dir to move files to. used when src dir has no target
    var publicFileComponents: FileComponents = null

    val connConf = adapCfgValues.get("ConnectionConfig").get.asInstanceOf[Map[String, Any]]
    //val connConfValues = connConf.values.asInstanceOf[Map[String, String]]
    connConf.foreach(kv => {
      if (kv._1.compareToIgnoreCase("HostLists") == 0) {
        val hostLists = kv._2.asInstanceOf[String]
        connectionConfig.hostsList = hostLists.split(",").map(str => str.trim).filter(str => str.size > 0)
      } else if (kv._1.compareToIgnoreCase("UserId") == 0) {
        val userID = kv._2.asInstanceOf[String]
        connectionConfig.userId = userID.trim
      } else if (kv._1.compareToIgnoreCase("Password") == 0) {
        val password = kv._2.asInstanceOf[String]
        connectionConfig.password = password.trim
      } else if (kv._1.compareToIgnoreCase("Authentication") == 0) {
        val authentication = kv._2.asInstanceOf[String]
        connectionConfig.authentication = authentication.trim
      } else if (kv._1.compareToIgnoreCase("Principal") == 0) {
        //kerberos
        val principal = kv._2.asInstanceOf[String]
        connectionConfig.principal = principal.trim
      } else if (kv._1.compareToIgnoreCase("Keytab") == 0) {
        //kerberos
        val keyTab = kv._2.asInstanceOf[String]
        connectionConfig.keytab = keyTab.trim
      } else if (kv._1.compareToIgnoreCase("Passphrase") == 0) {
        //ssh
        val passPhrase = kv._2.asInstanceOf[String]
        connectionConfig.passphrase = passPhrase.trim
      } else if (kv._1.compareToIgnoreCase("KeyFile") == 0) {
        //ssh
        val keyFile = kv._2.asInstanceOf[String]
        connectionConfig.keyFile = keyFile.trim
      } else if (kv._1.compareToIgnoreCase("hadoopConfig") == 0) {
        val hadoopConfig = kv._2.asInstanceOf[Map[String, String]]
        connectionConfig.hadoopConfig = List[(String, String)]()
        hadoopConfig.foreach(hconf => {
          connectionConfig.hadoopConfig ::= (hconf._1, hconf._2)
        })
      }
    })
    if (connectionConfig.authentication == null || connectionConfig.authentication == "")
      connectionConfig.authentication = "simple" //default

    if (adapCfgValues.getOrElse("MonitoringConfig", null) == null) {
      val err = "Not found MonitoringConfig for Smart File Adapter Config:" + adapterName
      throw new KamanjaException(err, null)
    }

    val monConf = adapCfgValues.get("MonitoringConfig").get.asInstanceOf[Map[String, Any]]
    //val monConfValues = monConf.values.asInstanceOf[Map[String, String]]
    monConf.foreach(kv => {
      if (kv._1.compareToIgnoreCase("MaxTimeWait") == 0) {
        monitoringConfig.waitingTimeMS = kv._2.asInstanceOf[String].trim.toInt
        if (monitoringConfig.waitingTimeMS < 0)
          monitoringConfig.waitingTimeMS = defaultWaitingTimeMS
      } else if (kv._1.compareToIgnoreCase("ConsumersCount") == 0) {
        monitoringConfig.consumersCount = kv._2.asInstanceOf[String].trim.toInt
        if (monitoringConfig.consumersCount < 0)
          monitoringConfig.consumersCount = defaultConsumerCount
      }
      else if (kv._1.compareToIgnoreCase("MonitoringThreadsCount") == 0) {
        monitoringConfig.monitoringThreadsCount = kv._2.asInstanceOf[String].trim.toInt
      }
      /*else  if (kv._1.compareToIgnoreCase("TargetMoveDirs") == 0) {
        monitoringConfig.targetMoveDirs = kv._2.asInstanceOf[String].split(",").map(str => str.trim).filter(str => str.size > 0)
      }*/
      else if (kv._1.compareToIgnoreCase("WorkerBufferSize") == 0) {
        monitoringConfig.workerBufferSize = kv._2.asInstanceOf[String].trim.toInt
      }
      else if (kv._1.compareToIgnoreCase("MessageSeparator") == 0) {
        monitoringConfig.messageSeparator = kv._2.asInstanceOf[String].trim.toInt.toChar
      }
      else if (kv._1.compareToIgnoreCase("EntireFileAsOneMessage") == 0) {
        monitoringConfig.entireFileAsOneMessage = kv._2.asInstanceOf[String].trim.toBoolean
      }
      else if (kv._1.compareToIgnoreCase("CheckFileTypes") == 0) {
        monitoringConfig.checkFileTypes = kv._2.asInstanceOf[String].trim.toBoolean
      }
      else if (kv._1.compareToIgnoreCase("CheckFileSize") == 0) {
        monitoringConfig.checkFileSize = kv._2.asInstanceOf[String].trim.toBoolean
      }
      else if (kv._1.compareToIgnoreCase("EnableEmailAndAttachmentMode") == 0) {
        monitoringConfig.enableEmailAndAttachmentMode = kv._2.asInstanceOf[String].trim.toBoolean
      }
      else if (kv._1.compareToIgnoreCase("OrderBy") == 0) {
        monitoringConfig.orderBy = kv._2.asInstanceOf[List[String]].toArray
      }
      else if (kv._1.compareToIgnoreCase("TagDelimiter") == 0) {
        monitoringConfig.tagDelimiter = kv._2.asInstanceOf[String]
      }
      else if (kv._1.compareToIgnoreCase("MsgTags") == 0) {
        monitoringConfig.msgTags = kv._2.asInstanceOf[List[String]].toArray
      }
      else if (kv._1.compareToIgnoreCase("DirCheckThreshold") == 0) {
        monitoringConfig.dirCheckThreshold = kv._2.asInstanceOf[String].trim.toInt
      }
      else if (kv._1.compareToIgnoreCase("Locations") == 0) {
        //old fashioned-simple locations
        simpleLocations = kv._2.asInstanceOf[String].split(",").map(str => MonitorUtils.simpleDirPath(str.trim)).filter(str => str.size > 0)
      }
      else if (kv._1.compareToIgnoreCase("TargetMoveDir") == 0) {
        //public targetMoveDir
        publicTargetMoveDir = MonitorUtils.simpleDirPath(kv._2.asInstanceOf[String])
        //println("publicTargetMoveDir============="+publicTargetMoveDir)
      }
      else if (kv._1.compareToIgnoreCase("EnableMoving") == 0) {
        monitoringConfig.enableMoving = kv._2.asInstanceOf[String]
      }
      else if (kv._1.compareToIgnoreCase("FileComponents") == 0) {
        //public FileComponents
        val componentsMap = kv._2.asInstanceOf[Map[String, Any]]
        publicFileComponents = extractFileComponents(componentsMap)
      }

      else if (kv._1.compareToIgnoreCase("DetailedLocations") == 0) {
        val locationsInfoBuffer = ArrayBuffer[LocationInfo]()

        val locations = kv._2.asInstanceOf[List[Map[String, Any]]]
        locations.foreach(loc => {
          val locationInfo = new LocationInfo
          loc.foreach(kv => {
            if (kv._1.compareToIgnoreCase("srcDir") == 0) {
              locationInfo.srcDir = MonitorUtils.simpleDirPath(kv._2.asInstanceOf[String].trim)
            }
            else if (kv._1.compareToIgnoreCase("targetDir") == 0) {
              locationInfo.targetDir = MonitorUtils.simpleDirPath(kv._2.asInstanceOf[String].trim)
            }
            else if (kv._1.compareToIgnoreCase("EnableMoving") == 0) {
              locationInfo.enableMoving = kv._2.asInstanceOf[String]
            }
            else if (kv._1.compareToIgnoreCase("MsgTags") == 0) {
              locationInfo.msgTags = kv._2.asInstanceOf[List[String]].toArray
            }
            else if (kv._1.compareToIgnoreCase("TagDelimiter") == 0) {
              locationInfo.tagDelimiter = kv._2.asInstanceOf[String].trim
            }
            else if (kv._1.compareToIgnoreCase("MessageSeparator") == 0) {
              locationInfo.messageSeparator = kv._2.asInstanceOf[String].trim.toInt.toChar
            }
            else if (kv._1.compareToIgnoreCase("OrderBy") == 0) {
              locationInfo.orderBy = kv._2.asInstanceOf[List[String]].toArray
            }
            else if (kv._1.compareToIgnoreCase("FileComponents") == 0) {
              val componentsMap = kv._2.asInstanceOf[Map[String, Any]]
              val fileComponents = extractFileComponents(componentsMap)
              locationInfo.fileComponents = fileComponents
            }

          })

          locationsInfoBuffer.append(locationInfo)
        })

        monitoringConfig.detailedLocations = locationsInfoBuffer.toArray
      }

    })


    //if no detailedLocations defined, use locations, define them as detailedLocations
    if (monitoringConfig.detailedLocations.length == 0) {
      if (simpleLocations.length > 0) {
        if ((!monitoringConfig.isMovingEnabled) && (publicTargetMoveDir == null || publicTargetMoveDir.length == 0)) {
          val err = "Not found TargetMoveDir corresponding to locations for Smart File Adapter Config:" + adapterName
          throw new KamanjaException(err, null)
        }

        simpleLocations.foreach(loc => {
          val locationInfo = new LocationInfo
          locationInfo.srcDir = loc
          locationInfo.targetDir = publicTargetMoveDir
          locationInfo.orderBy = //get public order by or default
            if (monitoringConfig.orderBy == null || monitoringConfig.orderBy.length == 0) defaultOrderBy
            else monitoringConfig.orderBy

          locationInfo.enableMoving =
            if (monitoringConfig.enableMoving == null || monitoringConfig.enableMoving.length == 0) "on"
            else monitoringConfig.enableMoving

          locationInfo.messageSeparator = monitoringConfig.messageSeparator
          locationInfo.fileComponents = publicFileComponents

          monitoringConfig.detailedLocations = monitoringConfig.detailedLocations :+ locationInfo
        })
      }

    }
    else {
      //for each location, if local attributes have no values get them from public corresponding attributes
      monitoringConfig.detailedLocations.foreach(locationInfo => {
        if (locationInfo.targetDir == null || locationInfo.targetDir.trim.length == 0)
          locationInfo.targetDir = publicTargetMoveDir

        if (locationInfo.orderBy.length == 0) {
          locationInfo.orderBy = //get public order by or default
            if (monitoringConfig.orderBy == null || monitoringConfig.orderBy.length == 0) defaultOrderBy
            else monitoringConfig.orderBy
        }

        if (locationInfo.enableMoving.length == 0) {
          locationInfo.enableMoving = //get public order by or default
            if (monitoringConfig.enableMoving == null || monitoringConfig.enableMoving.length == 0) "on"
            else monitoringConfig.enableMoving
        }

        if (locationInfo.messageSeparator == 0) {
          //this location has no separator, get the public value
          locationInfo.messageSeparator = monitoringConfig.messageSeparator
        }
        if (locationInfo.fileComponents == null)
          locationInfo.fileComponents = publicFileComponents

        if (locationInfo.msgTags == null || locationInfo.msgTags.length == 0)
          locationInfo.msgTags = monitoringConfig.msgTags

        if (locationInfo.tagDelimiter == null || locationInfo.tagDelimiter.length == 0)
          locationInfo.tagDelimiter = monitoringConfig.tagDelimiter
      })
    }


    //TODO : fix validations

    /*if(monitoringConfig.locations == null || monitoringConfig.locations.length == 0) {
      val err = "Not found Locations for Smart File Adapter Config:" + adapterName
      throw new KamanjaException(err, null)
    }*/

    /* if(monitoringConfig.targetMoveDirs == null || monitoringConfig.targetMoveDirs.length == 0) {
       val err = "Not found targetMoveDirs for Smart File Adapter Config:" + adapterName
       throw new KamanjaException(err, null)
     }*/

    /*if(monitoringConfig.targetMoveDirs.length > 1 && monitoringConfig.targetMoveDirs.length < monitoringConfig.locations.length) {
      val err = "targetMoveDir should either have one dir or same number as (location) for Smart File Adapter Config:" + adapterName
      throw new KamanjaException(err, null)
    }*/

    /*if(monitoringConfig.msgTags != null && monitoringConfig.msgTags.length > 0){
      monitoringConfig.msgTags.foreach(tag => if(!isValidMsgTag(tag)) throw new Exception(s"Invalid msg tag ($tag) for file input adatper ($adapterName)"))
    }*/

    var archiveConfig: ArchiveConfig = null
    if (adapCfgValues.contains("ArchiveConfig")) {
      try {
        val connConf = adapCfgValues.get("ArchiveConfig").get.asInstanceOf[Map[String, Any]]
        val aConfig = SmartFileProducerConfiguration.getAdapterConfigFromMap(null, connConf)
        aConfig.Name = ""
        aConfig.className = ""
        aConfig.jarName = ""
        aConfig.dependencyJars = Set[String]()

        archiveConfig = new ArchiveConfig()

        archiveConfig.outputConfig = aConfig
        archiveConfig.archiveParallelism = if (connConf.contains("ArchiveParallelism")) connConf.getOrElse("ArchiveParallelism", "1").toString.trim.toInt else 1
        if (archiveConfig.archiveParallelism <= 0) archiveConfig.archiveParallelism = 1

        archiveConfig.archiveSleepTimeInMs = if (connConf.contains("ArchiveSleepTimeInMs")) connConf.getOrElse("ArchiveSleepTimeInMs", "10").toString.trim.toInt else 10
        if (archiveConfig.archiveSleepTimeInMs < 0) archiveConfig.archiveSleepTimeInMs = 10
      } catch {
        case e: Throwable => {
          logger.error("Failed to load ArchiveConfig", e)
        }
      }
    }

    logger.debug("archiveConfig:" + archiveConfig + ", has ArchiveConfig:" + adapCfgValues.contains("ArchiveConfig"))

    //TODO : validation for FilesOrdering

    (_type, connectionConfig, monitoringConfig, archiveConfig)
  }


  def extractFileComponents(componentsMap: Map[String, Any]): FileComponents = {
    val fileComponents = new FileComponents
    componentsMap.foreach(componentTuple => {
      if (componentTuple._1.equalsIgnoreCase("Components"))
        fileComponents.components = componentTuple._2.asInstanceOf[List[String]].toArray
      else if (componentTuple._1.equalsIgnoreCase("Regex"))
        fileComponents.regex = componentTuple._2.asInstanceOf[String]
      else if (componentTuple._1.compareToIgnoreCase("Paddings") == 0) {
        val paddingsMap = componentTuple._2.asInstanceOf[Map[String, Any]]

        paddingsMap.foreach(paddingTuple => {
          val paddingInfo = new Padding
          val paddingJsonInfoList = paddingTuple._2.asInstanceOf[List[Any]]
          if (paddingJsonInfoList != null && paddingJsonInfoList.length == 3) {
            paddingInfo.padPos = paddingJsonInfoList(0).asInstanceOf[String]
            paddingInfo.padSize = //accept number whether as num or string
              try {
                paddingJsonInfoList(1).asInstanceOf[scala.math.BigInt].toInt
              }
              catch {
                case ex: Exception => paddingJsonInfoList(1).asInstanceOf[String].toInt
              }

            paddingInfo.padStr = paddingJsonInfoList(2).asInstanceOf[String]
          }
          fileComponents.paddings += paddingTuple._1 -> paddingInfo

        })

      }
    })

    fileComponents
  }
}

case class SmartFileKeyData(Version: Int, Type: String, Name: String, PartitionId: Int)

class SmartFilePartitionUniqueRecordKey extends PartitionUniqueRecordKey {
  val Version: Int = 1

  var Name: String = _
  // Name
  val Type: String = "SmartFile"
  var PartitionId: Int = _ // Partition Id

  override def Serialize: String = {
    // Making String from key
    val json =
    ("Version" -> Version) ~
      ("Type" -> Type) ~
      ("Name" -> Name) ~
      ("PartitionId" -> PartitionId)
    compact(render(json))
  }

  override def Deserialize(key: String): Unit = {
    // Making Key from Serialized String
    implicit val jsonFormats: Formats = DefaultFormats
    val keyData = parse(key).extract[SmartFileKeyData]
    if (keyData.Version == Version && keyData.Type.compareTo(Type) == 0) {
      Name = keyData.Name
      PartitionId = keyData.PartitionId
    }
    // else { } // Not yet handling other versions
  }
}

case class SmartFileRecData(Version: Int, FileName: String, Offset: Option[Long])

class SmartFilePartitionUniqueRecordValue extends PartitionUniqueRecordValue {
  val Version: Int = 1
  var FileName: String = _
  var Offset: Long = -1 // Offset of next message in the file

  override def Serialize: String = {
    // Making String from Value
    val json =
    ("Version" -> Version) ~
      ("Offset" -> Offset) ~
      ("FileName" -> FileName)
    compact(render(json))
  }

  override def Deserialize(key: String): Unit = {
    // Making Value from Serialized String
    implicit val jsonFormats: Formats = DefaultFormats
    val recData = parse(key).extract[SmartFileRecData]
    if (recData.Version == Version) {
      Offset = recData.Offset.get
      FileName = recData.FileName
    }
    // else { } // Not yet handling other versions
  }
}

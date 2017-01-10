package com.ligadata.GenericContainerDataTool

import com.ligadata.KamanjaBase.{ContainerInterface, RDD, RDDObject}
import org.apache.logging.log4j.LogManager

import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}


class ContainerDataLoader[T <: ContainerInterface](getRddFunc: Array[String] => RDD[T]) {


  var mapData = mutable.Map[Array[String], T]()

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  def getFullContainerData(keyValues: Array[Array[String]]): mutable.Map[Array[String], T] = {

    //val rdd = R.getRDD.map(x => x.asInstanceOf[T]).toArray

    keyValues.foreach(keyValue => {
      //      mapData = getRddFunc(keyValues).map(rdd => rdd.getPartitionKey()(0) -> rdd).toArray.toMap
      logger.info("ContainerDataLoader.getFullContainerData :inside Outer foreach ")

      var temp = getRddFunc(keyValue)
      temp.foreach(rdd => {
        logger.info("ContainerDataLoader.getFullContainerData :inside Inner foreach ")
        val key = rdd.getPartitionKey()
        mapData.put(key, rdd)
      })
    })
    logger.info("ContainerDataLoader.getFullContainerData : returning values of size " + mapData.size)
    return mapData
  }


  def getContainerData(containerName: String, partitionValue: Array[String]): mutable.HashMap[String, Any] = {
    logger.info("ContainerDataLoader.getContainerData : containerName = " + containerName)
    logger.info("ContainerDataLoader.getContainerData : partitionValue = " + partitionValue.mkString("||"))

    var retValues: mutable.HashMap[String, Any] = new mutable.HashMap[String, Any]()

    try {
      val classLoader = getClass.getClassLoader
      val className = containerName
      // This throws an exception if we don't find className in classLoader. This is just to check whether this class loader has this class name or not
      val clz = Class.forName(className, true, classLoader)

      // We can get an exceptions if any of the following fails. We need to add try ... catch ... for this
      val mirror = ru.runtimeMirror(classLoader)
      val module = mirror.staticModule(className)
      val obj = mirror.reflectModule(module)

      val objInst = obj.instance
      if (objInst.isInstanceOf[RDDObject[ContainerInterface]]) {
        val container: RDDObject[ContainerInterface] = objInst.asInstanceOf[RDDObject[ContainerInterface]]
        logger.info("ContainerDataLoader.getContainerData : calling container.getRDD(partitionValue) ")
        val containerData = container.getRDD(partitionValue)

        var iterator = containerData.iterator
        logger.info("ContainerDataLoader.getContainerData : gathering data in retValues map")
        while (iterator.hasNext) {
          var next = iterator.next()
          next.getAllAttributeValues.foreach(attr => {
            retValues.put(attr.getValueType.getName, attr.getValue)
          })
          //                    next.getAllAttributeValues.filter(attr => !ignoreFields.contains(attr.getValueType().getName())).map(attr => attr.getValue)
          //          locationnum_id.put(next.locationnum, next.locationid)
        }
      }
      else {
        println("not instance")
        logger.error("ContainerDataLoader.getContainerData : objInst is not an instance of RDDObject[ContainerInterface]")
      }
    }
    catch {
      case ex: Throwable => logger.error(ex)
    }
    return retValues
  }


  //  def main(containerName: String, partitionFieldName: String, partitionValue: Array[String]): mutable.HashMap[String, Any] = {
  //
  //    //    val containerName: String = args(1)
  //    //    val partitionFieldName: String = args(2)
  //    //    val partitionValue: String = args(3)
  //
  //    val values: mutable.HashMap[String, Any] = getContainerData(containerName, partitionFieldName, partitionValue)
  //
  //    return values
  //  }
}

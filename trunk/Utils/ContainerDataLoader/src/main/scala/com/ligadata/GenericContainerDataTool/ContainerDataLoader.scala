package com.ligadata.GenericContainerDataTool

import com.ligadata.KamanjaBase.{ContainerInterface, RDDObject}

import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}


class ContainerDataLoader {

  def getContainerData(containerName: String, partitionFieldName: String, partitionValue: Array[String]): mutable.HashMap[String, Any] = {

    var retValues: mutable.HashMap[String, Any] = new mutable.HashMap[String, Any]()

    try {
      val classLoader = getClass.getClassLoader
      //      val className = "tests.DT_LOCATONS$"
      val className = containerName
      // This throws an exception if we don't find className in classLoader. This is just to check whether this class loader has this class name or not
      val clz = Class.forName(className, true, classLoader)

      // We can get an exceptions if any of the following fails. We need to add try ... catch ... for this
      val mirror = ru.runtimeMirror(classLoader)
      val module = mirror.staticModule(className)
      val obj = mirror.reflectModule(module)

      val objinst = obj.instance
      if (objinst.isInstanceOf[RDDObject[ContainerInterface]]) {
        val container: RDDObject[ContainerInterface] = objinst.asInstanceOf[RDDObject[ContainerInterface]]
        val containerData = container.getRDD(partitionValue)

        var iterator = containerData.iterator
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
      }
    }
    catch {
      case ex: Throwable => ex.printStackTrace()
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

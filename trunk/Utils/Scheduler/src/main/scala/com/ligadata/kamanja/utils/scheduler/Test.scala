package com.ligadata.kamanja.utils.scheduler

import scala.collection.JavaConversions._

/**
  * Created by Saleh on 8/22/2016.
  */
object Test {
  ///tes the code
  def main(args: Array[String]) {
    val sch = KamanjaSchedulerImp.createScheduler

    sch.add(
      """{
              "name":"test1",
              "startTime":"2016-08-22 6:00:00",
              "endTime":"2017-08-23 6:30:00",
              "cronJobPattern":"0/5 * * * * ?",
              "payload": ["a", "b", "c"],
              "jobname": "com.ligadata.adapter1job"
              }""", new CallBack)
    sch.add(
      """{
              "name":"test2",
              "startTime":"2016-08-22 6:00:00",
              "endTime":"2017-08-23 6:30:00",
              "cronJobPattern":"0/5 * * * * ?",
              "payload": ["e", "f", "g"],
              "jobname": "com.ligadata.adapter1job"
              }""", new CallBack)

    sch.getAll().foreach(println(_))

    sch.add(
      """{
              "name":"test3",
              "startTime":"2016-08-22 6:00:00",
              "endTime":"2017-08-23 6:30:00",
              "cronJobPattern":"0/5 * * * * ?",
              "payload": ["q", "w", "e"],
              "jobname": "com.ligadata.adapter1job"
              }""", new CallBack)


    sch.add(
      """{
              "name":"test4",
              "startTime":"2016-08-22 6:00:00",
              "endTime":"2017-08-23 6:30:00",
              "cronJobPattern":"0/5 * * * * ?",
              "payload": ["q", "w", "e"]
              }""", new CallBack)


    sch.remove("test1", "com.ligadata.adapter1job")
    sch.update(
      """{
              "name":"test2",
              "startTime":"2016-08-22 6:00:00",
              "endTime":"2017-08-23 6:30:00",
              "cronJobPattern":"0/1 * * * * ?",
              "jobname": "com.ligadata.adapter1job"
              }""")

    sch.getAll().foreach(println(_))
    //    sch.shutdown()
  }

}

class CallBack extends SchedulerCallback {
  override def call(SchedulerName: String, TriggerTime: String, Payload: Array[String]): Unit = {
    println("<%s> file at time [%s] payload data {%s}".format(SchedulerName, TriggerTime, Payload.mkString(",")))
  }
}

package com.ligadata.kamanja.utils.scheduler

import java.util.Date

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.json4s.DefaultFormats
import org.quartz.impl.matchers.GroupMatcher
import org.quartz.impl.triggers.CronTriggerImpl
import org.quartz.{JobExecutionContext, Job}
import org.quartz.impl.{StdSchedulerFactory, JobDetailImpl}
import scala.collection.JavaConversions._
import java.util.{Map => JMap}
import org.json4s.native.JsonMethods._

/**
  * Created by Saleh on 8/21/2016.
  */

object KamanjaSchedulerImp {
  val DEFAULT_GROUP = "DEFAULT"
  val CALLBACK = "callback"
  val PAYLOAD = "payload"

  def createScheduler = new KamanjaSchedulerImp
}

class KamanjaSchedulerImp extends KamanjaScheduler {
  private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

  val scheduler = new StdSchedulerFactory().getScheduler()
  scheduler.start()


  override def add(json: String, callback: CacheCallback) = {

    val jsonjob = parseJson(json)

    val job = new JobDetailImpl
    job.setName(jsonjob.name)
    job.setJobClass(classOf[SchedulerJob])
    job.getJobDataMap.put(KamanjaSchedulerImp.CALLBACK, callback)
    job.getJobDataMap.put(KamanjaSchedulerImp.PAYLOAD, jsonjob.payload)

    val trigger = createTrigger(jsonjob)

    scheduler.scheduleJob(job, trigger)
  }

  override def update(json: String) = {

    val jsonjob = parseJson(json)

    val trigger = createTrigger(jsonjob)

    scheduler.getJobKeys(GroupMatcher.jobGroupEquals(KamanjaSchedulerImp.DEFAULT_GROUP)).toList
      .filter(jobKey => jobKey.getName.equals(jsonjob.name))
      .foreach(jobKey => {
        scheduler.rescheduleJob(scheduler.getTriggersOfJob(jobKey).get(0).getKey, trigger)
      }
      )
  }

  override def remove(jobName: String) = {
    scheduler.getJobKeys(GroupMatcher.jobGroupEquals(KamanjaSchedulerImp.DEFAULT_GROUP)).toList
      .filter(jobKey => jobKey.getName.equals(jobName))
      .foreach(scheduler.deleteJob(_))
  }


  override def getAll(): JMap[String, String] = {
    val map = (for {
      jobKey <- scheduler.getJobKeys(GroupMatcher.jobGroupEquals(KamanjaSchedulerImp.DEFAULT_GROUP))
      jobName = jobKey.getName()
      jobGroup = jobKey.getGroup()
      //get job's trigger
      triggers = scheduler.getTriggersOfJob(jobKey)
      nextFireTime = triggers.get(0).getNextFireTime()

    } yield (jobName -> nextFireTime.toString)).toMap

    map
  }

  override def shutdown() {
    scheduler.shutdown()
  }

  case class JsonJob(name: String, startTime: String, endTime: String, cronJobPattern: String, payload: Array[String])

  private def parseJson(json: String): JsonJob = {
    implicit val formats = DefaultFormats

    parse(json).extract[JsonJob]
  }


  private def createTrigger(job: JsonJob): CronTriggerImpl = {
    val trigger = new CronTriggerImpl()
    trigger.setName(job.name)
    trigger.setStartTime(DateTime.parse(job.startTime, formatter).toDate)
    trigger.setEndTime(DateTime.parse(job.endTime, formatter).toDate)
    trigger.setCronExpression(job.cronJobPattern)

    trigger
  }

}

class SchedulerJob extends Job {

  override def execute(context: JobExecutionContext): Unit = {
    val dataMap = context.getJobDetail().getJobDataMap
    val callback = dataMap.get(KamanjaSchedulerImp.CALLBACK).asInstanceOf[CacheCallback]
    val payload = dataMap.get(KamanjaSchedulerImp.PAYLOAD).asInstanceOf[Array[String]]
    callback.call(context.getJobDetail.getKey.getName, context.getFireTime.toString, payload)
  }
}

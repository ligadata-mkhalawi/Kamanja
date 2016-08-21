import java.util.Date

import org.quartz._
import org.quartz.impl.matchers.GroupMatcher
import org.quartz.impl.{JobDetailImpl, StdSchedulerFactory}
import org.quartz.impl.triggers.{SimpleTriggerImpl, CronTriggerImpl}
import scala.collection.JavaConversions._

/**
  * Created by Saleh on 8/21/2016.
  */
object Mian extends App {


  class JobTest {
    def printval: Unit = {
      println("this is test")
    }
  }

  val job = new JobDetailImpl
  job.setName("dummyJobName")
  job.setJobClass(classOf[HelloJob])
  job.getJobDataMap.put("callback", new JobTest)

  val trigger = new CronTriggerImpl()
  trigger.setName("hello")
  trigger.setStartTime(new Date(System.currentTimeMillis() + 1000))
  trigger.setEndTime(new Date(System.currentTimeMillis() + 60000))
  trigger.setCronExpression("0/30 * * * * ?")
  //  val trigger = new SimpleTriggerImpl
  //  trigger.setName("test1")
  //  trigger.setRepeatCount(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT);
  //  trigger.setRepeatInterval(30000);

  val scheduler = new StdSchedulerFactory().getScheduler();
  scheduler.start();
  scheduler.scheduleJob(job, trigger)
  //  scheduler.shutdown()

  class HelloJob extends Job {

    override def execute(context: JobExecutionContext): Unit = {
      val dataMap = context.getJobDetail().getJobDataMap
      val main = dataMap.get("callback").asInstanceOf[JobTest]
      main.printval
    }
  }


  scheduler.getJobGroupNames().toList.foreach(groupName => {
    for (jobKey: JobKey <- scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

      val jobName = jobKey.getName();
      val jobGroup = jobKey.getGroup();

      //get job's trigger
      val triggers = scheduler.getTriggersOfJob(jobKey);
      val nextFireTime = triggers.get(0).getNextFireTime();

      System.out.println("[jobName] : " + jobName + " [groupName] : "
        + jobGroup + " - " + nextFireTime);

    }

  })

}

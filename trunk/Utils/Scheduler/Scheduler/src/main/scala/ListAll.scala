import org.quartz.JobKey
import org.quartz.impl.StdSchedulerFactory
import org.quartz.impl.matchers.GroupMatcher
import scala.collection.JavaConversions._

/**
  * Created by Saleh on 8/21/2016.
  */
object ListAll extends App {
  val scheduler = new StdSchedulerFactory().getScheduler();

  scheduler.getJobGroupNames().toList.foreach(groupName => {
    println(groupName)
    //      for (jobKey: JobKey <- scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
    //
    //        val jobName = jobKey.getName();
    //        val jobGroup = jobKey.getGroup();
    //
    //        //get job's trigger
    //        val triggers = scheduler.getTriggersOfJob(jobKey);
    //        val nextFireTime = triggers.get(0).getNextFireTime();
    //
    //        System.out.println("[jobName] : " + jobName + " [groupName] : "
    //          + jobGroup + " - " + nextFireTime);
    //
    //      }

  })
}

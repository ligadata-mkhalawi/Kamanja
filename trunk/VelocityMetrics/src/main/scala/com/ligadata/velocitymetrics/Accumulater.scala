package com.ligadata.velocitymetrics

import org.apache.logging.log4j.{ Logger, LogManager }
import com.ligadata.Exceptions._;
import com.ligadata.Exceptions.StackTrace;
import org.json4s.jackson.JsonMethods._;
import org.json4s.DefaultFormats;
import org.json4s.Formats;
import scala.collection.mutable.ListBuffer;

case class AccumulaterData(var aggKeys: Map[String, String], var dataTime: Long, var systemTime: Long)

case class AccmulatorConfig(var aggregatorConfig: List[AggregatorConfig])
case class AggregatorConfig(var aggregateKeys: List[String], intervalKey: String, intervals: List[(Int, String)])

class Aggregate(var bucketVal: Long, var intervalVal: Int, var aggregateKeyVal: AggregateKeyVal)
class AggregateKeyVal(var aggrKeyVal: String, var Counter: Int, var modified: Boolean)

class Accumulater(componentName: String, nodeiId: String, config: String) {

  private val accumulators: String = "accumulators"
  private val aggregatekeys = "aggregatekeys"
  private val intervalkey = "intervalkey"
  private val intervals = "intervals"
  private val LOG = LogManager.getLogger(getClass)
  private var aggregatedData = scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, scala.collection.mutable.Map[String, (Int, Boolean)]]]()

  var intervalTypes = List("sec", "secs", "second", "seconds", "min", "mins", "minute", "minutes", "hr", "hrs", "hour", "hours", "day", "days")
  var secs = List("sec", "secs", "second", "seconds")
  var mins = List("min", "mins", "minute", "minutes")
  var hour = List("hr", "hrs", "hour", "hours")
  var day = List("day", "days")

  val accConfig = parseAggregatorConfig(config: String)

  def compute(data: Array[AccumulaterData]): Unit = {
    //compute bucket value based on intervals(5sec, 1 min, 2 min, 1 hr)

    LOG.info("accConfig " + accConfig)
    // if (validateAggKeys(accConfig, data)) 

    computeBucketValue(data, accConfig)
  }

  private def parseAggregatorConfig(aggConfigJson: String): AccmulatorConfig = {
    var aggConfig = new ListBuffer[AggregatorConfig]
    val mapOriginal = parse(aggConfigJson).values.asInstanceOf[scala.collection.immutable.Map[String, Any]]
    type elist = List[String]
    if (mapOriginal == null)
      throw new Exception("Invalid json data")

    val map: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()
    mapOriginal.foreach(kv => { map(kv._1.trim().toLowerCase()) = kv._2 })

    if (map.contains(accumulators)) {
      val acclist = map.get(accumulators).getOrElse(null)
      if (acclist != null && acclist.isInstanceOf[List[Map[String, Any]]]) {
        val aList = acclist.asInstanceOf[List[Map[String, Any]]]
        aList.foreach(accmltr => {
          val aggmap = scala.collection.mutable.Map[String, Any]()
          var aggregateKeys = List[String]()
          var intrvlKey: String = ""
          var intrvls = ListBuffer[(Int, String)]()
          LOG.info("accmltr  " + accmltr)
          accmltr.foreach(kv => { aggmap(kv._1.trim().toLowerCase()) = kv._2 })

          if (aggmap.contains(aggregatekeys)) {
            val aggkeys = aggmap.get(aggregatekeys).getOrElse(null)
            if (aggkeys != null && aggkeys.isInstanceOf[elist]) {
              aggregateKeys = aggkeys.asInstanceOf[List[String]]
            }
          }
          if (aggmap.contains(intervalkey)) {
            val ikey = aggmap.get(intervalkey).getOrElse(null)
            if (ikey != null && ikey.isInstanceOf[String]) {
              intrvlKey = ikey.asInstanceOf[String]
            }
          }
          if (aggmap.contains(intervals)) {
            val ivls = aggmap.get(intervals).getOrElse(null)
            if (ivls != null && ivls.isInstanceOf[elist]) { //parse intervals and store
              val ivlList = ivls.asInstanceOf[List[String]]
              ivlList.foreach { interval =>
                {
                  val i = interval.split("(?<=\\d)(?=\\D)")
                  LOG.info(i(0) + "  " + i(1))
                  if (i.size < 2) throw new Exception("Please provide intervals with number and unit")
                  val intrvlVal = (i(0).trim().toInt, i(1).trim())
                  if (!intervalTypes.contains(i(1).trim())) throw new Exception("Please check the interval type in the config")
                  intrvls += intrvlVal
                }
              }
            }
          }
          LOG.info("aggmap  " + aggmap)

          var aggr = new AggregatorConfig(aggregateKeys, intrvlKey, intrvls.toList)
          aggConfig += aggr
        })
      }
    }
    new AccmulatorConfig(aggConfig.toList)
  }

  private def validateAggKeys(acmConfig: AccmulatorConfig, data: Array[AccumulaterData]): Boolean = {

    if (data == null || data.size == 0) throw new Exception("AccumulaterData is null")

    if (acmConfig == null) throw new Exception("Accumulator Config is null")
    val aggConfig = acmConfig.aggregatorConfig

    if (aggConfig == null || aggConfig.size == 0) throw new Exception("Aggregator Config is null or Aggregator Config does not exists ")
    var aggKeySet = Set[String]()
    aggConfig.foreach(aggKeys => {
      aggKeySet = aggKeySet ++ aggKeys.aggregateKeys.toSet
    })

    for (i <- 0 until data.length) {
      val aggKeys = data(i).aggKeys
      if (aggKeys == null) throw new Exception("Agg Keys is null")
      aggKeys.foreach(aggKey => {
        val aggKeyName = aggKey._1.trim()
        if (!aggKeySet.contains(aggKeyName)) throw new Exception(s"Aggregator Key $aggKeyName is not valid, does not present in the config ")
      })
    }
    true
  }

  private def getIntervalSecs(aggKeyDSet: Set[String], accConfig: AccmulatorConfig): Set[Long] = {

    var intervalSetInSec = Set[Long]()
    if (aggKeyDSet == null || aggKeyDSet.size == 0) throw new Exception("Agg Keys is null")
    if (accConfig == null) throw new Exception("accumulator Config is null")
    val aggConfig = accConfig.aggregatorConfig
    if (aggConfig == null || aggConfig.size == 0) throw new Exception(" Agg Config is null")
    aggConfig.foreach(agg => {
      val aggKeys = agg.aggregateKeys
      if (aggKeys == null || aggKeys.size == 0) throw new Exception("  ")
      aggKeyDSet.foreach(aggKey =>
        if (aggKeys.contains(aggKey)) intervalSetInSec = intervalSetInSec ++ getIntervalInsecs(agg.intervals))
    })
    intervalSetInSec
  }

  private def getIntervalInsecs(intervals: List[(Int, String)]): Set[Long] = {

    var intervalSet = Set[Long]()
    if (intervals == null || intervals.size == 0) throw new Exception("Intervals does not exists")

    intervals.foreach(interval => {
      if (interval._2 == null || interval._2.trim().size == 0)
        throw new Exception("Interval unit doe not exists")
      if (secs.contains(interval._2.trim())) intervalSet = intervalSet + interval._1
      else if (mins.contains(interval._2.trim())) intervalSet = intervalSet + interval._1 * 60
      else if (hour.contains(interval._2.trim())) intervalSet = intervalSet + interval._1 * 3600
      else if (hour.contains(interval._2.trim())) intervalSet = intervalSet + interval._1 * 86400
      else if (day.contains(interval._2.trim())) intervalSet = intervalSet + interval._1 * 86400 * 24
    })

    intervalSet
  }
  private def computeBucketValue(accData: Array[AccumulaterData], accConfig: AccmulatorConfig) = {

    try {

      var modified: Boolean = false
      var counter: Int = 1
      accData.foreach { aData =>
        {
          //get Interval from Config Keys baed on  the aData key
          //check the interval in config exists in the above list
          val intervalset = getIntervalSecs(aData.aggKeys.keySet, accConfig)
          if (intervalset == null || intervalset.size == 0) throw new Exception(s"Interval Set for Key  does not exists")
          intervalset.foreach(interval => {
            var configInterval: Long = interval * 1000
            var bucketVal = aData.dataTime / configInterval
            LOG.info("interval bucketVal: " + interval + " , " + bucketVal)
            // store interval as secs in map
            var k = Set[String]()
            LOG.info("aData.aggKeys " + aData.aggKeys)
            aData.aggKeys map (aggKeys => accConfig.aggregatorConfig map (keys => if (keys.aggregateKeys.contains(aggKeys._1)) k += aggKeys._2)) //. accConfig.aggregatorConfig map (keys => keys.aggregateKeys.map(key => aData.aggKeys.getOrElse(key, "")).mkString(",")) // => (key => println("=============="+key))

            val mapkey = k.mkString(",")

            if (aggregatedData.contains(configInterval)) {
              //get the bucketVal, if matches, add counter else add another record in bucketval map
              if (aggregatedData(configInterval).contains(bucketVal)) {

                if (aggregatedData(configInterval)(bucketVal).contains(mapkey)) {
                  var ctr: Int = aggregatedData(configInterval)(bucketVal)(mapkey)._1 + 1
                  var modify: Boolean = true
                  val counterVal: (Int, Boolean) = (ctr, modify)

                  val aggrData = updataCounterAggMap(ctr, modify, mapkey, aggregatedData(configInterval)(bucketVal).toMap)
                  aggregatedData(configInterval)(bucketVal) = aggrData
                  //    println("aggregatedData(configInterval)(bucketVal) " + aggregatedData(configInterval)(bucketVal))
                  //    println("aggregatedData(configInterval) " + aggregatedData(configInterval))

                } else {

                  LOG.info("bucketValMap - bucketVal  " + aggregatedData(configInterval)(bucketVal))
                  // need to check this scenario
                  var bucketValMap = scala.collection.mutable.Map[Long, Map[String, (Int, Boolean)]]()
                  var aggKeyMap = scala.collection.mutable.Map[String, (Int, Boolean)]()
                  aggregatedData(configInterval)(bucketVal).foreach(key => {
                    aggKeyMap(key._1) = key._2
                  })
                  aggKeyMap(mapkey) = (counter, modified)
                  bucketValMap(bucketVal) = aggKeyMap.toMap

                  val bktMap = aggregatedData(configInterval).asInstanceOf[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[String, (Int, Boolean)]]]
                  aggregatedData(configInterval) = bktMap ++ bucketValMap.asInstanceOf[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[String, (Int, Boolean)]]]
                }

              } else {

                var bucketValMap = scala.collection.mutable.Map[Long, Map[String, (Int, Boolean)]]()
                aggregatedData(configInterval).foreach(key => {
                  bucketValMap(key._1) = key._2.toMap
                })

                bucketValMap(bucketVal) = Map(mapkey -> (counter, modified))
                val bktMap = aggregatedData(configInterval).asInstanceOf[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[String, (Int, Boolean)]]]
                aggregatedData(configInterval) = bktMap ++ bucketValMap.asInstanceOf[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[String, (Int, Boolean)]]]
              }
            } else {

              var bucketValMap = scala.collection.mutable.Map[Long, Map[String, (Int, Boolean)]]()
              bucketValMap(bucketVal) = Map(mapkey -> (counter, modified))
              LOG.info("bucketValMap(bucketVal) " + bucketValMap(bucketVal))
              // bucketValMap(bucketVal) = aData.aggKeys map (aggKey => aggKey._2 -> (counter, modified))
              aggregatedData(configInterval) = bucketValMap.asInstanceOf[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[String, (Int, Boolean)]]]
            }
          })
        }
      }
      LOG.info("aggregatedData " + aggregatedData)
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        LOG.debug("StackTrace:" + stackTrace)
        throw e
      }
    }
  }

  private def updataCounterAggMap(ctr: Int, modify: Boolean, aggKey: String, keyData: scala.collection.immutable.Map[String, (Int, Boolean)]): scala.collection.mutable.Map[String, (Int, Boolean)] = {
    var keyVal = scala.collection.mutable.Map[String, (Int, Boolean)]()
    keyData.foreach(key => {
      if (aggKey.equals(key._1))
        keyVal(key._1) = (ctr, modify)
      else
        keyVal(key._1) = key._2
    })
    keyVal
  }
  private def genCounteResult() = {

  }

}


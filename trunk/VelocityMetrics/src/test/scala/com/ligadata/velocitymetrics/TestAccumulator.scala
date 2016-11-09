

package com.ligadata.velocitymetrics

object TestAccumulator {
  def main(args: Array[String]): Unit = {

    //assuming interval is secs 
    var aggKeys1: Map[String, String] = Map(("Key1", "Value1"), ("Key2", "Value2"))
    var aggKeys2: Map[String, String] = Map(("Key4", "Value4"), ("Key3", "Value3"))

    var aggKeys3: Map[String, String] = Map(("Key1", "Value1"), ("Key2", "Value2"))

    var aggConfigJson = "{\"Accumulators\":[{\"AggregateKeys\":[\"Key1\",\"Key2\"],\"IntervalKey\":\"IKey\",\"Intervals\":[\"3 sec\",\"1 mins\",\"2 hours\"]},{\"AggregateKeys\":[\"Key3\",\"Key4\"],\"IntervalKey\":\"IKey2\",\"Intervals\":[\"1 sec\",\"10 secs\",\"10 mins\",\"1 day\"]},{\"AggregateKeys\":[\"Key1\",\"Key3\"],\"IntervalKey\":\"IKey\",\"Intervals\":[\"1 sec\",\"1 mins\",\"2 hours\"]}]}"

    var accmltr: Accumulater = new Accumulater("FlieProcessor ", "Node1", aggConfigJson)

    var accData1 = new AccumulaterData(aggKeys1, System.currentTimeMillis(), 0)
    var accData2 = new AccumulaterData(aggKeys2, System.currentTimeMillis(), 0)
    Thread.sleep(5000)
    var accData3 = new AccumulaterData(aggKeys3, System.currentTimeMillis(), 0)
    var accData: Array[AccumulaterData] = Array(accData1, accData2, accData3)
    accmltr.compute(accData)

  }
}
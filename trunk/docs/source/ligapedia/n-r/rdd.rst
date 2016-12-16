
.. _rdd-term:

RDD
---

[Brief definition of RDD and how it is used in Kamanja]

These are the basic methods to use from Java or Scala programs
to interface with the Kamanja history.

::

  Scala
  def build: T = rddObj.build
  def build(from: T): T = rddObj.build(from)

  // First group of functions retrieve one object (either recent or for a given key &amp; filter)
  // Get recent entry for the Current Key
  def getRecent: Optional[T] = Utils.optionToOptional(rddObj.getRecent)
  def getRecentOrNew: T = rddObj.getRecentOrNew

  // Get recent entry for the given key
  def getRecent(key: Array[String]): Optional[T] = Utils.optionToOptional(rddObj.getRecent(key))
  def getRecentOrNew(key: Array[String]): T = rddObj.getRecentOrNew(key)

  def getOne(tmRange: TimeRange, f: Function1[T, java.lang.Boolean]): Optional[T] = null
  def getOne(key: Array[String], tmRange: TimeRange, f: Function1[T, java.lang.Boolean]): Optional[T] = null
  def getOneOrNew(key: Array[String], tmRange: TimeRange, f: Function1[T, java.lang.Boolean]): T = rddObj.build
  def getOneOrNew(tmRange: TimeRange, f: Function1[T, java.lang.Boolean]): T = rddObj.build

  // This group of functions retrieve collection of objects
  def getRDDForCurrKey(f: Function1[T, java.lang.Boolean]): JavaRDD[T] = null
  def getRDDForCurrKey(tmRange: TimeRange, f: Function1[T, java.lang.Boolean]): JavaRDD[T] = null

  // With too many messages, these may fail - mostly useful for message types where number of messages are relatively small
  def getRDD(): JavaRDD[T] = null
  def getRDD(tmRange: TimeRange, f: Function1[T, java.lang.Boolean]): JavaRDD[T] = null
  def getRDD(tmRange: TimeRange): JavaRDD[T] = rddObj.getRDD(tmRange)
  def getRDD(f: Function1[T, java.lang.Boolean]): JavaRDD[T] = null

  def getRDD(key: Array[String], tmRange: TimeRange, f: Function1[T, java.lang.Boolean]): JavaRDD[T] = null
  def getRDD(key: Array[String], f: Function1[T, java.lang.Boolean]): JavaRDD[T] = null
  def getRDD(key: Array[String], tmRange: TimeRange): JavaRDD[T] = rddObj.getRDD(key, tmRange)

  // Saving data
  def saveOne(inst: T): Unit = rddObj.saveOne(inst)
  def saveOne(key: Array[String], inst: T): Unit = rddObj.saveOne(key, inst)
  def saveRDD(data: RDD[T]): Unit = rddObj.saveRDD(data)
	

See:

- `Spark Quick Start
  <http://spark.apache.org/docs/latest/quick-start.html>`
  for a quick tutorial about RDD.

- `Spark API Documentation
  <http://spark.apache.org/docs/latest/api.html>`_




.. _java-scala-guide-history:

Accessing the History of Messages and Container Values
======================================================

Now, take a look at TransactionMsg.scala:

::

  package system.v1000000;
  ...
  object TransactionMsg extends RDDObject[TransactionMsg] with   BaseMsgObj {
  ...
  class TransactionMsg(var transactionId: Long, other: TransactionMsg) extends BaseMsg {
	

and TransactionMsgFactory.java:

::

  ...
  public static Optional getRecent() {
      return Utils.optionToOptional(TransactionMsg$.MODULE$.getRecent())
  ...
  }


Models use the :ref:`RDD<rdd-term>` interface
to access the history of messages and container values.
What is history? History is the data stored in a database,
such as Cassandra or HBase.

How to access the history of message and container values in the models created?
In the case of a Scala model,
call on RDD methods on a message Scala object.
In the case of a Java model,
use the additionally-generated Factory static class,
which is really a wrapper around the singleton Scala classes.

So, remember that Kamanja provides the RDD and JavaRDD interfaces
to access historical data.
Create, Read, and Update methods on history are supported.

Note: For now, a rule model must be encapsulated in a single file,
either .java or .scala.

.. _java-scala-guide-retrieve-history:

Retrieving Message Objects from History
=======================================

See how a model interacts with history.
See that a container is loaded by calling the getRecentOrNew(Array[String])
static class on the container object that implements the RDD trait.

::

  val gPref = GlobalPreferences.getRecentOrNew(Array("Type1"))
  /* This returns the most recent RDDObject that has a "Type1" partition key.
     Remember that the partition keys are arrays of values. In this case,
     it's an array of a single value. Or a new RDDObject if there is no history
     for this partition key. */
	
Likewise, see that a CustPreferences instance and CustAlertHistory
are instantiated.

::

  val pref = CustPreferences.getRecentOrNew
  ...
  val alertHistory = CustAlertHistory.getRecentOrNew

See :ref:`RDD<rdd-term>` for an overview
of the Kamanja RDD and RDDObject methods that are supported
and can be used to interface with history:

.. _java-scala-guide-create-history:

Creating and Saving New Message/Container Objects in History
============================================================

The previous section described retrieving message objects from history.
This section deals with putting new messages into history.

Later, if the logic allows for it,
a new CustAlertHistory RDDObject is inserted into the history
and will be thus available for future use.
Note that, to create a new message object within a model,
use the build() and withXXX() methods.

::

  CustAlertHistory.build.withalertdttminms(curTmInMs).

  withalerttype("lowbalancealert").Save
	



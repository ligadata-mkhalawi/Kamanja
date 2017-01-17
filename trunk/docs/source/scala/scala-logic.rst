
.. _scala-logic:

Logic of a Scala model
======================

This section covers the actual logic of creating a Kamanja model,
which can be very complicated.
To keep track, here are some important steps to follow:

#. Make sure the model has the objects and classes
   that are required for Scala models.
#. To access the history of messages and container values
   or add the messages and containers to the data store, use RDD methods.
#. Encapsulate the model in a single file (.scala).

Look at the model provided for the purposes of the example.

See the entire source code of a model here:

::

  LowBalanceAlert_Finance.scala

Every Scala model must have the following:

#. An object that extends a BaseModelObj trait.
   This is a singleton class used by the engine
   in the lifecycle of the instantiated model objects.
#. A class that extends a ModelBase trait.
   This is a class that executes actual logic.
#. A class that extends a ModelResultBase trait.
   MappedModelResult helper class implements a ModelResultBase
   to quickly output key/value results,
   but a developer may want to implement his/her own.

See the actual Scala code:
::

  object LowBalanceAlert extends ModelBaseObj {
    override def IsValidMessage(msg: MessageContainerBase): Boolean = return msg.isInstanceOf[TransactionMsg]
    override def CreateNewModel(mdlCtxt: ModelContext): ModelBase = return new LowBalanceAlert(mdlCtxt)
    override def ModelName(): String = "System.LowBalanceAlert" // Model Name
    override def Version(): String = "0.0.1" // Model Version
    override def CreateResultObject(): ModelResultBase = new LowBalanceAlertResult()
  }
	

When an engine gets a message from an adapter,
it checks all the registered models to see
if that model wants to process that specific message.
The engine calls the IsValidMessage method and if TRUE is returned,
it instantiates the actual class by calling the CreateNewModel method.

The class that has the executable logic looks like this:

::

  Executable Logic
  class LowBalanceAlert(mdlCtxt: ModelContext) extends ModelBase(mdlCtxt, LowBalanceAlert){
    private[this]valLOG=Logger.getLogger(getClass);
    override def execute(emitAllResults: Boolean):ModelResultBase={
      //First check the preferences and decide whether to continue or
      //not
      valgPref=GlobalPreferences.getRecentOrNew(Array("Type1"))valpref=CustPreferences.getRecentOrNewif(pref.minbalancealertoptout==true){
        returnnull
      }//Check if at least min number of hours elapsed since last alert
        valcurDtTmInMs=RddDate.currentGmtDateTimevalalertHistory=CustAlertHistory.getRecentOrNewif(
        curDtTmInMs.timeDiffInHrs(RddDate(alertHistory.alertdttminms))&lt;gPref.minalertdurationinhrs){
      returnnull
      }//continue with alert generation only if balance from current
      //transaction is less than threshold
      valrcntTxn=TransactionMsg.getRecentif(rcntTxn.isEmpty){
        returnnull
      }if(rcntTxn.isEmpty||rcntTxn.get.balance&gt;=gPref.minalertbalance){
        returnnull
      }valcurTmInMs=urDtTmInMs.getDateTimeInMs
      //create new alert history record and persist
      //(if policy is to keep only one, this will replace existing one)
      //CustAlertHistory.build.withalertdttminms(
      curTmInMs).withalerttype("lowbalancealert").Save
      //results
      newLowBalanceAlertResult().withCustId(rcntTxn.get.custid).withBranchId(rcntTxn.get.branchid).withAccNo(
            rcntTxn.get.accno).withCurBalance(rcntTxn.get.balance).withAlertType("lowBalanceAlert").withTriggerTime(curTmInMs)
    }
  }
	

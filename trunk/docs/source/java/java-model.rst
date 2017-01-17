
.. _java-scala-guide-java:

Creating a Java Model
=====================

A model written in Java has some structural differences
because of the inherent differences between
the Scala and Java programming languages,
and the fact that the engine is implemented in Scala.
To keep track, here are some important steps to follow:

#. Make sure the model has the objects and classes that are required for Java models.
#. To access the history of messages and container values or add the messages and containers to the data store, use JavaRDD methods.
#. Encapsulate the model in a single file (.java).

Sample Java model
-----------------

An example of a Java model can be seen here:

::

  package com.ligadata.kamanja.financial;
 
  import scala.Option;
 
  import com.ligadata.KamanjaBase.*;
  import System.*;
  public class LowBalanceAlertModel extends ModelBase {
    static LowBalanceAlertModelObj objSingleton = new LowBalanceAlertModelObj();
    ModelContext mdlCntxt;
 
    public LowBalanceAlertModel(ModelContext mdlContext) {
      super(mdlContext, objSingleton);
      mdlCntxt = mdlContext;
    }
 
    @Override
    public ModelBaseObj factory() {
      // TODO Auto-generated method stub
        return objSingleton;
    }
 
    @Override
    public ModelContext modelContext() {
      // TODO Auto-generated method stub
      return mdlCntxt;
    }
 
    public ModelResultBase execute(boolean emitAllResults) {
      GlobalPreferences gPref = (GlobalPreferences) GlobalPreferences.getRecentOrNew(new String[]{"Type1"});  //(new String[]{"Type1"});
      CustPreferences cPref = (CustPreferences) CustPreferences.getRecentOrNew();
 
      if(cPref.minbalancealertoptout()) {
        return null;
      }
 
      RddDate curDtTmInMs = RddDate.currentGmtDateTime();
      CustAlertHistory alertHistory = (CustAlertHistory) CustAlertHistory.getRecentOrNew();
      if(curDtTmInMs.timeDiffInHrs(new RddDate(alertHistory.alertdttminms())) < gPref.minalertdurationinhrs()) {
        return null;
      }
 
      TransactionMsg rcntTxn = (TransactionMsg) this.modelContext().msg();
 
      if (rcntTxn.balance() >= gPref.minalertbalance())
        return null;
 
      long curTmInMs = curDtTmInMs.getDateTimeInMs();
      // create new alert history record and persist (if policy is to keep only one, this will replace existing one)
      CustAlertHistory.build().withalertdttminms(curTmInMs).withalerttype("lowbalancealert").Save();
 
      Result[] actualResult = {new Result("Customer ID",rcntTxn.custid()),
        new Result("Branch ID",rcntTxn.branchid()),new Result("Account No.",rcntTxn.accno()),
        new Result("Current Balance",rcntTxn.balance()),new Result("Alert Type","lowbalancealert"),
        new Result("Trigger Time",new RddDate(curTmInMs).toString())
    };
    return new MappedModelResults().withResults(actualResult);
  }
 
  /**
  * @param inTxnContext
  */
 
  public boolean minBalanceAlertOptout(JavaRDD<custPreferences> pref) {
    return false;
  }
 
  public static class LowBalanceAlertModelObj implements ModelBaseObj {
    public boolean IsValidMessage(MessageContainerBase msg) {
      return (msg instanceof TransactionMsg);
    }
 
    public ModelBase CreateNewModel(ModelContext mdlContext) {
      return new LowBalanceAlertModel(mdlContext);
    }
 
    public String ModelName() {
      return "LowBalanceAlert";
    }
 
    public String Version() {
      return "0.0.1";
    }
 
    public ModelResultBase CreateResultObject() {
      return new MappedModelResults();
    }
  }
  }


A Java custom model has the following:

- A class that extends a ModelBase trait –
  a class that executes actual logic.
  The entry point into the logic is the execute method.
- A class that extends a ModelResultBase trait –
  a MappedModelResult helper class implements a ModelResultBase
  to quickly output key/value results,
  but a developer may want to implement his/her own.
- An object that extends a BaseModelObj trait –
  a singleton class used by the engine
  in the lifecycle of the instantiated model objects;
  in Java, this is an inner static class within the main model class.

This model uses a MappedModelResult class to simplify the code.

Accessing RDD Factories from Java Custom Models
-----------------------------------------------

Because all the code is generated in Scala,
a message factory Java class was generated
to assist Java programmers in accessing its history.
For example, for the TransactionMsg message in the example,
a TransactionMsgFactory.java was created
that wraps around the Scala RDD factories
in the TransactionMsg.scala Scala class.
Here is the example.
To get a JavaRDD, call the following method in the TransactionMsgFactory.java.

 
::

  public static JavaRDD getRDD(TimeRange tmRange) {
    return TransactionMsg$.MODULE$.getRDD(tmRange).toJavaRDD();
  }
	

Passing Explicit Functions to RDD Methods
-----------------------------------------

This is not currently supported in Java 7
(will be available in Java 8 once it is certified for the product).
So, a number of interfaces have been provided
that can be implemented by an inner Java class (contained within the model),
and then an instantiated object of that class is passed to the RDD method.

The interfaces that are provided are listed here
and the developer must choose the appropriate interface to implement
based on the RDD method that he/she wishes to use.

All the interfaces are defined in the
com.ligadata.KamanjaBase.api.java.function package:

- Function1.java
- Function2.java
- Function3.java
- FlatMapFunction1.java
- FlatMapFunction2.java
- FlatMapFunction3.java
- PairFunction.java

For example, a FlatMapFunction1.java interface is used
to pass in a function to an RDD method
that evaluates an input of type and returns a collection of type.

Here is the definition:

::

  public interface FlatMapFunction1&lt;T, R&gt; {
    public Iterable call(T t) throws Exception;
  }

ModelInstance/Factory
---------------------

Kamanja has enhancements to the way model instantiation is handled
when processing messages (new in v1.2).
This includes the ability to define a Java/Scala model as reusable.

Take a look at COPDRiskAssessment.java for these enhancements.

Once-per-node Initialization
----------------------------

Kamanja uses a factory, called ModelInstanceFactory,
in order to maintain the creation of model instances
(called ModelInstance).
The init function of the ModelInstanceFactory
is called only one time per Kamanja node when the model is first invoked,
regardless of the number of partitions being used by Kamanja
or the number of ModelInstances being created.

Leveraging this once-only initialization is best used
when large amounts of static (unchanging) data
is being referenced from the database
or expensive operations that need to only be performed once are being executed.
This initialization happens only once
when the engine loads the ModelInstanceFactory
(COPDRiskAssessmentFactory.init(txnCtxt)
is invoked only one time (per node in the cluster)).

In this particular example,
a collection of medical codes exists belonging to five different categories:
smoking, chronic cough, dyspnea, chronic sputum, and environmental exposure.
The init function calls getRDD to retrieve the codes
followed by several iterators going through each RDD object
and collecting the medical codes from the RDD objects.

Below is a modified example of the medical sample application in Java:

::

  public static class COPDRiskAssessmentFactory extends ModelInstanceFactory {
    // Lookup Arrays
    ArrayList<string> coughCodes = new ArrayList<>();
    ArrayList<string> dyspnoeaCodes = new ArrayList<>();
    ArrayList<string> envCodes = new ArrayList<>();
    ArrayList<string> smokeCodes = new ArrayList<>();
    ArrayList<string> sputumCodes = new ArrayList<>();

    public COPDRiskAssessmentFactory(ModelDef modelDef, NodeContext nodeContext) {
      super(modelDef, nodeContext);
    }

    public void init(TransactionContext txnContext) {
      System.out.println("==============================>Factory is initializing");

      // Getting container RDD objects
      JavaRDD<coughCodes> coughCodesRDD = CoughCodesFactory.rddObject.getRDD();
      JavaRDD<dyspnoeaCodes> dyspnoeaCodesRDD = DyspnoeaCodesFactory.rddObject.getRDD();
      JavaRDD<envCodes> envCodesRDD = EnvCodesFactory.rddObject.getRDD();
      JavaRDD<smokeCodes> smokeCodesRDD = SmokeCodesFactory.rddObject.getRDD();
      JavaRDD<sputumCodes> sputumCodesRDD = SputumCodesFactory.rddObject.getRDD();

      // Taking all icd9 codes from the containers and placing them in an ArrayList
      for (Iterator<coughCodes> coughCodeIt = coughCodesRDD.iterator(); coughCodeIt.hasNext(); ) {
        coughCodes.add(coughCodeIt.next().icd9code());
      }

      for (Iterator<dyspnoeaCodes> dyspCodeIt = dyspnoeaCodesRDD.iterator(); dyspCodeIt.hasNext(); ) {
        dyspnoeaCodes.add(dyspCodeIt.next().icd9code());
      }

      for (Iterator<envCodes> envCodeIt = envCodesRDD.iterator(); envCodeIt.hasNext(); ) {
        envCodes.add(envCodeIt.next().icd9code());
      }

      for (Iterator<smokeCodes> smokeCodeIt = smokeCodesRDD.iterator(); smokeCodeIt.hasNext(); ) {
        smokeCodes.add(smokeCodeIt.next().icd9code());
      }

      for (Iterator<sputumCodes> sputumCodeIt = sputumCodesRDD.iterator(); sputumCodeIt.hasNext(); ) {
        sputumCodes.add(sputumCodeIt.next().icd9code());
      }
    }
		...

ModelInstances are created by the engine through ModelInstanceFactory
when the engine requires ModelInstances.
ModelInstances are created and initialized
when the engine gets the first message for each partition.
If ModelInstanceFactory.isModelInstanceReusable() returns true,
the engine reuses the same ModelInstances partition
for all incomining messages for that partition.
If ModelInstanceFactory.isModelInstanceReusable() returns false,
the engine creates and initializes the new ModelInstances
for each incomining message.

The following block of code shows an example
of retrieving the codes that were initialized by the Factory:

::

  public class COPDRiskAssessment extends ModelInstance {
    public COPDRiskAssessment(ModelInstanceFactory factory) {
      super(factory);
    }

    public void init(String instanceMetadata) {
      System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~>Model instance initializing" + instanceMetadata);
      COPDRiskAssessmentFactory fact = (COPDRiskAssessmentFactory) getModelInstanceFactory();
      coughCodes = fact.coughCodes;
      dyspnoeaCodes = fact.dyspnoeaCodes;
      envCodes = fact.envCodes;
      smokeCodes = fact.smokeCodes;
      sputumCodes = fact.sputumCodes;
    }
  ...
	

In the init function shown,
COPDRiskAssessmentFactory fact = (COPDRiskAssessmentFactory)
getModelInstanceFactory();, is being used to retrieve the factory class.
Local collections are set that are defined elsewhere
in COPDRiskAssessment to hold the codes that were initialized by the factory.
Now, whenever those codes are needed by future ModelInstances,
they can simply refer to the factory
that was previously created and access the codes that were already initialized.



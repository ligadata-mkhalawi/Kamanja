
.. _java-scala-guide-message-container:

Generating Message/Container Sources and the Corresponding JARs
===============================================================

The first thing to do is to create the
:ref:`messages<messages-term>` and :ref:`containers<container-term>`
needed for model execution.
Then, when the JSON messages are submitted to the metadata API,
the API creates the JAR files.

Creating JSON Files
-------------------

The developer needs to create the messages and containers
that are added to the engine.
This section has some JSON files that are examples of the kinds
of messages and containers that need to be created.

Look at the
*/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/container* directory.
Notice the following files in this directory:

::

  AccountAggregatedUsage_Telecom.json
  AccountInfo_Telecom.json
  CoughCodes_Medical.json
  CustAlertHistory_Finance.json
  CustPreferences_Finance.json
  CustomerInfo_Finance.json
  DyspnoeaCodes_Medical.json
  EnvCodes_Medical.json
  GlobalPreferences_Finance.json
  SmokeCodes_Medical.json
  SputumCodes_Medical.json
  SubscriberAggregatedUsage_Telecom.json
  SubscriberGlobalPreferences_Telecom.json
  SubscriberInfo_Telecom.json
  SubscriberPlans_Telecom.json

To implement the above rule, take a look at the following JSON objects:

::

  CustAlertHistory_Finance.json
  CustPreferences_Finance.json
  CustomerInfo_Finance.json
  GlobalPreferences_Finance.json

To learn more about how to create messages and containers,
see Messages and Containers.

Now take a look at the /Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/message directory.
The following files are in this directory:

::

  Message_Definition_HelloWorld.json	inpatientclaim_Medical.json
  SubscriberUsage_Telecom.json		log-cleaner.log
  TransactionMsg_Finance.json		outpatientclaim_Medical.json
  beneficiary_Medical.json		zookeeper.out
  hl7_Medical.json

To further implement the above rule, take a look at the following JSON object:

::

    TransactionMsg_Finance.json

Again, the developer does not need to implement the source code
for these objects.
He/she just has to provide JSON-formatted message/containers
and the Kamanja compiler generates the source code
and creates the correct JAR files for him/her.

The following are definitions for the JSON properties
in TransactionMsg_Finance.json:

::

  Message – means it’s the message (it can be either Message or Container).
  NameSpace – namespace of the message/container.
  Name – name of the message/container.
  Version – version of the message/container.
  Description – description about the message/container.
  Fixed – message type (if Fixed is TRUE then it is a fixed message.
    If Fixed is FALSE then it is a mapped message).

Compiling Message/Container Definitions into JARs
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Five message/container documents need to be compiled
by adding them to the engine.
The metadata API then generates the proper code/JAR files.
This can be done using either
the :ref:`kamanja-command-ref` command or a REST service.

The Kafka topics have already been created.
The next step is to add the containers.

For example, run the **kamanja add container** command,
specifying the :ref:`metadataapiconfig-config-ref` file to use,
the JSON file that defines your :ref:`container-def-config-ref`,
and the :ref:`tenant<tenancy-term>` ID:

Type:

::

  > bash $KAMANJA_HOME/bin/kamanja
    $KAMANJA_HOME/config/MetadataAPIConfig.properties add container
    $KAMANJA_HOME/input/SampleApplications/metadata/container/CustAlertHistory_Finance.json
    TENANTID tenant1


Expected output:

::

  {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddContainerDef",
      "Result Data" : null,
      "Result Description" : "Container Added Successfully:system.custalerthistory.000000000001000000"
    }
  }

Now run:

::

  > bash $KAMANJA_HOME/bin/kamanja
    $KAMANJA_HOME/config/MetadataAPIConfig.properties add container
    $KAMANJA_HOME/input/SampleApplications/metadata/container/CustomerInfo_Finance.json
    TENANTID tenant1

Expected output:

::

  {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddContainerDef",
      "Result Data" : null,
      "Result Description" : "Container Added Successfully:system.customerinfo.000000000001000000"
    }
  }

Now run:

::

  > bash $KAMANJA_HOME/bin/kamanja \
    $KAMANJA_HOME/config/MetadataAPIConfig.properties add container \
    $KAMANJA_HOME/input/SampleApplications/metadata/container/CustPreferences_Finance.json \
    TENANTID tenant1

Expected output:

::

  {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddContainerDef",
      "Result Data" : null,
      "Result Description" : "Container Added Successfully:system.custpreferences.000000000001000000"
    }
  }

Now run:

::

  > bash $KAMANJA_HOME/bin/kamanja \
    $KAMANJA_HOME/config/MetadataAPIConfig.properties add container \
    $KAMANJA_HOME/input/SampleApplications/metadata/container/GlobalPreferences_Finance.json \
    TENANTID tenant1

Expected output:

::

  {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddContainerDef",
      "Result Data" : null,
      "Result Description" : "Container Added Successfully:system.globalpreferences.000000000001000000"
    }
  }

Ignore the warnings for now.
The results from the kamanja add container command
are that all four containers were successfully added.

The next step is to add the message.

For example, run the kamanja add message command.

Type:

::

  > bash $KAMANJA_HOME/bin/kamanja
    $KAMANJA_HOME/config/MetadataAPIConfig.properties add message
    $KAMANJA_HOME/input/SampleApplications/metadata/message/TransactionMsg_Finance.json
    TENANTID tenant1

Expected output:

::

  Result: {
    "APIResults" : {
      "Status Code" : 0,
      "Function Name" : "AddMessageDef",
      "Result Data" : null,
      "Result Description" : "Message Added Successfully:system.transactionmsg.000000000001000000"
    }
  }

The process (code generation, code compilation, JAR generation, JAR upload)
occurred successfully. Here are the results you should expect to see.

- The three generated source codes are in workingdir
  (as defined in the configuration file for the metadata API).
- The two generated JARs are in the JAR_TARGET directory
  (as defined in the configuration file for the metadata API).
- The JAR files have been uploaded to the metadata storage.

The configuration file (MetadataAPIConfig.properties)
is located in /Users/userid/Downloads/installKamanja/config.

MetadataAPIConfig.properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  #MetadataStore information
  MetadataDataStore={"StoreType": "hbase","SchemaName": "metadata","Location": "localhost"}
  ROOT_DIR=/Users/userid/Downloads/installKamanja
  GIT_ROOT=/Users/userid/Downloads/installKamanja
  JAR_TARGET_DIR=/Users/userid/Downloads/installKamanja/lib/application
  SCALA_HOME=/usr/local/Cellar/scala/2.11.7
  JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home
  MANIFEST_PATH=/Users/userid/Downloads/installKamanja/config/manifest.mf
  ...
  TYPE_FILES_DIR=/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/type/
  FUNCTION_FILES_DIR=/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/function/
  CONCEPT_FILES_DIR=/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/concept/
  MESSAGE_FILES_DIR=/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/message/
  CONTAINER_FILES_DIR=/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/container/
  CONFIG_FILES_DIR=/Users/userid/Downloads/installKamanja/config/
  MODEL_EXEC_LOG=false
  JarPaths=/Users/userid/Downloads/installKamanja/lib/system,/Users/userid/Downloads/installKamanja/lib/application
  SECURITY_IMPL_JAR=/Users/userid/Downloads/installKamanja/lib/system/simpleapacheshiroadapter_2.10-1.0.jar
  SECURITY_IMPL_CLASS=com.ligadata.Security.SimpleApacheShiroAdapter
  AUDIT_IMPL_JAR=/Users/userid/Downloads/installKamanja/lib/system/auditadapters_2.10-1.0.jar
  AUDIT_IMPL_CLASS=com.ligadata.audit.adapters.AuditCassandraAdapter
  DO_AUDIT=NO
  DO_AUTH=NO
  SSL_CERTIFICATE=/Users/userid/Downloads/installKamanja/config/keystore.jks

Go to /Users/userid/Downloads/installKamanja/workingdir
and see if there are three generated source code files there:

::

  > cd /Users/userid/Downloads/installKamanja/workingdir

Run ls in the workingdir directory.
The expected output is:

::

  CustAlertHistory		CustomerInfo_local
  CustAlertHistory.scala		CustomerInfo_local.scala
  CustAlertHistoryFactory.java	GlobalPreferences
  CustAlertHistory_local		GlobalPreferences.scala
  CustAlertHistory_local.scala	GlobalPreferencesFactory.java
  CustPreferences			GlobalPreferences_local
  CustPreferences.scala		GlobalPreferences_local.scala
  CustPreferencesFactory.java	TransactionMsg
  CustPreferences_local		TransactionMsg.scala
  CustPreferences_local.scala	TransactionMsgFactory.java
  CustomerInfo			TransactionMsg_local
  CustomerInfo.scala		TransactionMsg_local.scala
  CustomerInfoFactory.java

Each of the five JSON files mentioned earlier generated three source code files.

The three generated source code files are:

- <msgName>.scala – actual source code that is compiled into a deployable JAR.
  The package name has a version in it.
- <msgName>_local.scala – source code that can be downloaded
  and used as part of later model development.
  No version is inserted into the _local.scala file,
  so the model can import packages that are shown in here for model testing.
- <msgName>Factory.java – provides methods for models written in Java
  to create JavaRDD and JavaRDDObject objects
  (Refer here for a quick tutorial of RDD usage.
  Full API documentation can be found here).

For example, see the fully-generated code
of the TransactionMsg.json message here:

- TransactionMsg.scala
- TransactionMsg_local.scala
- TransactionMsgFactory.java

Go to /Users/userid/Downloads/installKamanja/lib/application
(this is the JAR_TARGET_DIR directory) and see the two generated JAR files.

Run the **ls** command on the *JAR_TARGET_DIR* directory.
The expected output is:

::

  system_CustAlertHistory.jar
  system_CustAlertHistory_1000000_1448927644430.jar
  system_CustPreferences.jar
  system_CustPreferences_1000000_1448927675635.jar
  system_CustomerInfo.jar
  system_CustomerInfo_1000000_1448927661143.jar
  system_GlobalPreferences.jar
  system_GlobalPreferences_1000000_1448927695756.jar
  system_TransactionMsg.jar
  system_TransactionMsg_1000000_1448927750168.jar

Each of the five JSON files mentioned earlier generated two JAR files.





.. _message-def-config-ref:

Message definition
==================

The :ref:`message<messages-term>` definition structure provides
design-time definitions of incoming data and interfaces
that makes it easy for Kamanja to process input messages.
Custom message definitions can be created
to control which input messages are sent to Kamanja.

Each input, output, or storage message used in the cluster
has its own "Message" definition,
identified by a unique "Name".

Message definitions include some core parameters
that are used for all message types
plus some parameters that are specific to each message type.

File structure
--------------

Input message file structure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  {
      "Message": {
      	"NameSpace": "<namespace>",
      	"Name": "<name>",
        "Type": input | output | storage
      	"Version": "<version>",
      	"Description": "<description of product",
      	"Persist": "true" | "false",
      	"Fixed": "true" | "false",
        "CaseSensitive" : "true" | "false",
      	"Fields": [{
      		"Name": "<attribute-name>",
      		"Type": "<type>"
      	}, {
                ...
      	}],
      	"PartitionKey": ["Id"],
      	"PrimaryKey": ["Id ", " Name"],
      	"TimePartitionInfo": {
      		"Key": "Id ",
      		"Format": "epochtime" | "epochtimeInMillis" | "epochtimeInSeconds" | SimpleDateFormat 
      		"Type": "Daily" | "Monthly" | "Yearly"
      	}
      }
  }

Output message file structure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  {
    "OutputMessage": {
          "NameSpace": "System",
          "Name": "BeneficiaryOutput",
          "Version": "00.01.00",
          "Description": "Beneficiary Output",
          "Queue": "testout_1",
          "PartitionKey": [
              "${System.Beneficiary.desynpuf_id}"
          ],
          "Defaults": [
              {
                  "Name": "System.COPDRiskAssessment.COPDSeverity ",
                  "Default": "true"
              }
          ],
          "DataDeclaration": [
              {
                  "Name": "Delim",
                  "Default": ","
              }
          ],
          "OutputFormat": "{ "Beneficiary.Desynpuf_Id":
      "${System.Beneficiary.Desynpuf_Id}",
      "Beneficiary.Bene_Birth_Dt":
      "${System.Beneficiary.Bene_Birth_Dt}",
      "COPDRiskAssessment.COPDSymptoms":
      "${System.COPDRiskAssessment.COPDSymptoms}",
      "COPDRiskAssessment.COPDSeverity":
      "${System.COPDRiskAssessment.COPDSeverity}",
      "COPDRiskAssessment.ChronicSputum":
      "${System.COPDRiskAssessment.ChronicSputum}",
      "COPDRiskAssessment.AYearAgo":
      "${System.COPDRiskAssessment.AYearAgo}",
      "COPDRiskAssessment.Age":
      "${System.COPDRiskAssessment.Age}",
      "COPDRiskAssessment.inPatientClaimCostsByDate":
      "${System.COPDRiskAssessment.inPatientClaimCostsByDate}",
      "COPDRiskAssessment.Dyspnoea":
      "${System.COPDRiskAssessment.Dyspnoea}",
      "COPDRiskAssessment.AATDeficiency":
      "${System.COPDRiskAssessment.AATDeficiency}"}"
      }
  }

Storage message file structure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Parameters
----------

Input message parameters
~~~~~~~~~~~~~~~~~~~~~~~~

- **NameSpace** – namespace of the message.
- **Name** – name of the message.
- **Version** – version of the message.
- **Description** – (optional) description of the message.
- **Persist** – (optional) If set to TRUE,
  data processed as this message type
  is saved to the data store.  See :ref:`persist-term`.
- **Fixed** – if set to TRUE, this is a fixed message;
  if set to FALSE, it is a mapped messages.

  Use fixed messages when all fields are available
  in the incoming data stream and they are presented to the model.
  They are represented as a Scala class instance at runtime.
- **CaseSensitive** -- if set to TRUE, fields in the message definition
  are case-sensitive.
  The variables in the generated message are the same case
  as given in the message definition.
  If set to FALSE, the fields in the message definition
  are considered lower case
  and the field variables in the message are generated as lower case.
  Default value is FALSE.
- **Fields/elements** – schema definition for the data included
  in this message.  This is a list of attribute names
  and the :ref:`type<types-term>` of each attribute.

  The message definition schema attribute value type
  can be a primitive data type such as int, long, float, double,
  Boolean, or string; or a complex data type such as an array,
  map, or container.

- **PartitionKey** – (optional) partition keys for the message.
  Choosing a good and relevant partition key is important
  and has implications on model complexity and performance.

  - All the messages with same partition key value
    are routed and processed on the same node in the cluster.
    This makes any aggregate computations
    on the partition key easy and efficient.
  - It is also important to have a good distribution
    on the values of the key
    so that all the resources in the cluster are used.

- **PrimaryKey** – (optional) primary keys for the message.
- **TimePartitionInfo** – (optional) time partition information.
  The attributes are:

  - **Key** – should be one of the fields from the message definition.
  - **Format** – The format of the data in the input message.
    The value of the format in the message definition
    can be one of the following: epochTime, epochtimeInMillis,
    epochtimeInSeconds, or java SimpleDateFormat pattern.
  - **Type** – can be Yearly or Monthly or Daily.



Output message parameters
~~~~~~~~~~~~~~~~~~~~~~~~~


- **NameSpace** – namespace of the output adapter.
- **Name** – name of the output adapter.
- **Version** – version of the output adapter.
- **Description** – description of the output adapter.
- **Queue** – logical name of the output adapter.
  The name can either be a file adapter, Kafka queue or MQ queue.
  The name is wherever the output message is pushed.
- **PartitionKey** – partition key information
  that is sent to the output adapter.
- **Defaults** – if the key is not present in the model results,
  then this default data is placed in the Defaults field
  mentioned in the output message definition.
- **DataDeclaration** – local variable declaration
  where the variables can be declared in the output message definition
  and the value of that variable is used in the output format.
- **OutputFormat** – format of the output message
  that is generated with the data and pushed to the output adapter.


Storage message parameters
~~~~~~~~~~~~~~~~~~~~~~~~~~


Usage
-----

How the engine handles input messages
that do not match the message definition:

- If a message input is defined as fixed,
  the message fails entirely.
  CSV must be fixed in length and order.

- If a message input is defined as mapped,
  as long as the message type is declared correctly,
  the correct fields are selected and unknown fields are ignored.

Output messages
~~~~~~~~~~~~~~~

Kamanja supports a particular format for the output message definition.
Use the following instructions to add an output message definition
to the metadata API.

#. Verify that the output message definition exists in the correct folder:

   ::

    $KAMANJA_HOME/input/SampleApplications/metadata/outputmsg/

#. Use the following command to add the output message definition
   to the metadata API:

   ::

     $KAMANJA_HOME/bin/kamanja \
     $KAMANJA_HOME/input/SampleApplications/metadata/config/MetadataAPIConfig_Medical.properties add outputmessage \
     $KAMANJA_HOME/input/SampleApplications/metadata/outputmsg/sampleOutputMsg.json

After running the engine, the output should exist in the output queue
specified in the output message definition.

Examples
--------

Output message -- Sample 1
~~~~~~~~~~~~~~~~~~~~~~~~~~

This is the JSON definition of the output message:

::

  {
      "OutputMessage": {
          "NameSpace": "System",
          "Name": "OutputMsgName",
          "Version": "00.01.00",
          "Description": "Output Msg Name",
          "Queue": "outputQueueName",
          "PartitionKey": [
              "${Namespace.MessageName.partionKeyattribute}"
          ],
          "OutputFormat": "{ "MessageAttribute1":
                             "${Namespace.MessageName.attribute1}",
                             "ModelAttribute1":
                             "${Namespace.ModelName.attribute1}",
                             "MessageAttribute2":
                             "${Namespace.MessageName.attribute2}",
                             "MessageAttribute3":
                             "${Namespace.MessageName.attribute3}",
                             "<wbr />MessageAttribute4":
                             "${Namespace.MessageName.attribute4}",  
                             "ModelAttribute2":
                             "${Namespace.ModelName.attribute2}",
                             "<wbr />ModelAttribute3":
                             "${Namespace.ModelName.attribute3}"}"
      }
  }

This is the output message that exists in the queue:

::

  "ExecutionTime":"2015-01-26T16: 53: 42.656-08: 00",
  "EventDate":1422259242656,
  "TxnId":100000000000021,
  "ModelName":"com.ligadata.pmml.System_COPDRiskAssessment_100",
  "uniqKey":"{
      "Version": 1,
      "Type": "Kafka",
      "Name": "testin_1",
      "TopicName": "testin_1",
      "PartitionId": 0
  }",
  "uniqVal":"{
      "Version": 1,
      "Offset": 1393
  }",
  "ModelVersion":"100",
  "DataReadTime":"2015-01-2616: 53: 42.620",
  "xformCntr":1,
  "ElapsedTimeFromDataRead":35994
  }

This is the meaning of the parameters in the output message:

- **ExecutionTime and EventDate** – time the output message was emitted.
- **TxnId** – transaction identifier associated with
  the model instance that processed the incoming message.
- **ModelName** – model name itself.
- **uniqKey** – incoming queue from which the input to the model originated.
- **uniqVal** – bookkeeping offset information
  from where in the queue that incoming message was found.
- **ModelVersion** – model version.
- **DataReadTime** – time the incoming message was read.
- **xformCntr** – transformation counter.
  The input message can transform into multiple messages
  in the engine to process. xformCntr tells which message
  (transformed internal message) this output belongs to.
- **xformCntr, uniqKey and uniqVal** – used exactly once
  to detect whether output is pushed to this adapter or not.

  For example: If the input message transforms into three internal messages
  and, after processing two of them, the engine crashes,
  it is necessary to track how many messages have been processed
  and how many messages have been output.
  Only the third transformed message is output
  when the engine restarts or the workload is distributed.
- **ElapsedTimeFromDataRead** – ElapsedTime from DataRead until
  the message emitted is presumably in micro-seconds.


See also
--------

- :ref:`GenerateMessage.sh<generatemessage-command-ref>`



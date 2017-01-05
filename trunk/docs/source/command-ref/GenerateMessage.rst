

.. _generatemessage-command-ref:

GenerateMessage.sh
==================

Generate a :ref:`message definition<message-def-config-ref>`
from a file header or from :ref:`PMML<pmml-term>`.
The shell script 
The tool is written in Scala and uses a shell script to run it. An input file and a JSON configuration file are provided. The configuration file is modified to specify where to put the output file and whether to generate the message definition from a file header or from PMML. The tool has exception handling capabilities.

Syntax
------

::

  bash $KAMANJA_HOME/bin/GenerateMessage.sh \
    --inputfile $KAMANJA_HOME/input/SampleApplication/data/file.csv \
    --config $KAMANJA_HOME/config/file.json

Options and arguments
---------------------

- **--inputfile** - file from which to generate an input message definition.
  if the --inputfile contains headers with quotation marks (")
  around each column name, fields are not generated.
  Quotation marks should not be a part of the headers.
- **--config** - JSON file that includes configuration parameters
  to generate an input message definition.

Configuration file structure
----------------------------

The configuration file structure is similar to the following:

::

  {
  "delimiter": ",",
  "outputPath": "/usr",
  "saveMessage": "false",
  "nameSpace": "com.ligadata.kamanja",
  "partitionKey": "id",
  "primaryKey": "id,name",
  "timePartition": "creationdate",
  "messageType": "input",
  "messageName": "testmessage",
  "createMessageFrom": "header",
  "messageStructure": "fixed",
  "detectDatatypeFrom": "3"
  }

Configuration file parameters
-----------------------------

- **delimiter** - delimiter between fields in the input file
  (mandatory for header and not needed for PMML).
- **outputPath** - directory to which the output file is saved
  (mandatory for header and PMML).
- **saveMessage** - equal to persist in the message definition;
  default value is FALSE.
  (optional for header and PMML).
- **nameSpace** - namespace for the message definition;
  default value is com.message
  (optional for header and PMML).
- **partitionKey** - partition key for the message definition;
  default value is an empty string,
  which means that partitionKey is not added
  to the message definition structure.
  (optional for header and PMML).
- **primaryKey** - primary key for the message definition;
  default value is an empty string
  which means primarynKey is not populated in the message definition structure.
  (optional for header and PMML).
- **timePartition** - time partition for the message definition;
  default value is an empty string
  which means timePartition is not populated
  in the message definition structure.
  (optional for header and PMML).
- **messageType** - type of message that is produced from the tool
  (either input or output message);
  default value is input.
  (optional for header and PMML).
- **messageName** - name of the message definition;
  default value is testmessage.
  (optional for header and PMML).
- **createMessageFrom** - option to create a message
  (supports header and PMML) (mandatory field).
- **messageStructure** - structure of the message
  (supports fixed or mapped); default value is fixed.
  (optional for header and PMML).
- **detectDatatypeFrom** - number of rows that are used
  to detect the data type of the field;
  default value is 4.
  (optional for header and not needed for PMML).

Usage
-----

Return values
-------------

If successful, GenerateMessage.sh returns a message
similar to the following:

::

  [RESULT] - message created successfully[RESULT] -
  you can find the file in this path /tmp/message_20160605095817.json

It also creates a *message_<yyyymmddmmss>.json file
that contains the generated message definition.

If the command is not successful,
an error message is generated:

::

  ERROR [main] - This file does not exist WARN [main] - Usage:  bash $KAMANJA_HOME/bin/GenerateMessage.sh --inputfile $KAMANJA_HOME/input/SampleApplication/data/file.csv --config $KAMANJA_HOME/config/file.json


- The specified input file does not exist.


::

  ERROR [main] - This file /opt/Kamanja/input/SampleApplications/data/test.csv does not include data. Check your file please. WARN [main] - Usage:  bash $KAMANJA_HOME/bin/GenerateMessage.sh --inputfile $KAMANJA_HOME/input/SampleApplication/data/file.csv --config $KAMANJA_HOME/config/file.json

- The specified input file does not include data.

::

  ROR [main] - This file does not exist WARN [main] - Usage:  bash $KAMANJA_HOME/bin/GenerateMessage.sh --inputfile $KAMANJA_HOME/input/SampleApplication/data/file.csv --config $KAMANJA_HOME/config/file.json


- The specified config file does not exist.

::

  ERROR [main] - This file /opt/KamanjaDoubleVersionsTest/Kamanja-1.5.0_2.10/config/ConfigFile_GeneratMessagetest.properties does not include data. Check your file please. WARN [main] - Usage:  bash $KAMANJA_HOME/bin/GenerateMessage.sh --inputfile $KAMANJA_HOME/input/SampleApplication/data/file.csv --config $KAMANJA_HOME/config/file.json

- The specified configuration file does not include data.

::

  ERROR [main] - The output path does not exist

- The specified config file does not define outputPath.


::

  ERROR [main] - The value for saveMessage should be true or false

- The specified config file defines saveMessage as something other than
  TRUE or FALSE; for example, "saveMessage": "test".

::

  ERROR [main] - The value of messageStructure should be fixed or mapped

- The specified config file defines messageStructure as something other than
  fixed or mapped; for example, "messageStructure": "test".

::

  ERROR [main] - The value for createMessageFrom should be header or PMML

- The specified config file defines createMessageFrom as something other than
  header or PMML (for example, "createMessageFrom": "test"):

::

  ERROR [main] - The value of massegeType should be input or output

- The specified config file defines messageType as something other than
  input or output; for example, "messageType": "test".

::

  ERROR [main] - you pass 10 in detectdatatypeFrom and the file size equal to 8 records, please pass a number greater than 1 and less than the file size

- The specified config file assigns a value to detectDatatypeFrom
  that is greater than the file size or less than 1.

::

  ERROR [main] - test key from partitioKey/PrimaryKey/TimePartitionInfo does not exist in message fields. choose another key please

- The specified config file assigns a value to partitionKey,
  primaryKey, or timePartition that does not exist in the header of the file;
  for example, "primaryKey": "test".

Examples
--------

These are end-to-end examples.

To Generate a Message from a File Header
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Go to $KAMANJA_HOME and run this command:

::

    ./GenerateMessage.sh --inputfile /opt/Kamanja/input/SampleApplications/data/SubscriberInfo_Telecom.dat --config /opt/KamanjaDoubleVersionsTest/Kamanja-1.5.0_2.10/config/ConfigFile_GeneratMessage.properties

Suppose that the ConfigFile_GeneratMessage.properties file
includes the following information:

::

  ConfigFile_GeneratMessage.properties

  {
  "delimiter": ",",
  "outputPath": "/tmp",
  "saveMessage": "false",
  "nameSpace": "com.ligadata.kamanja",
  "partitionKey": "msisdn",
  "primaryKey": "msisdn",
  "timePartition": "activationDate",
  "messageType": "input",
  "messageStructure": "fixed",
  "createMessageFrom": "header",
  "detectDatatypeFrom": 4
  }

After running the tool, the following messages is output:

::

  [RESULT] - message created successfully[RESULT] - you can find the file in this path /tmp/message_20160605095817.json

The message_20160605095817.json file includes:

::

  message_20160605095817.json

  {
  "Message": {
  "NameSpace": "com.ligadata.kamanja",
  "Name": "testmessage",
  "Verion": "00.01.00",
  "Description": "",
  "Fixed": "true",
  "Persist": "false",
  "Feilds": [{
  "Name": "msisdn",
  "Type": "System.Long"
  }, {
  "Name": "actNo",
  "Type": "System.Int"
  }, {
  "Name": "planName",
  "Type": "System.String"
  }, {
  "Name": "activationDate",
  "Type": "System.Int"
  }, {
  "Name": "thresholdAlertOptout",
  "Type": "System.Boolean"
  }],
  "PartitionKey": ["msisdn"],
  "PrimaryKey": ["msisdn"],
  "TimePartitionInfo": {
  "Key": "activationDate",
  "Format": "epochtime",
  "Type": "Daily"
  }
  }
  }

Suppose that the ConfigFile_GeneratMessage.properties file
includes the following information:

::

  ConfigFile_GeneratMessage.properties

  {
  "delimiter": ",",
  "outputPath": "/tmp",
  "saveMessage": "false",
  "nameSpace": "com.ligadata.kamanja",
  "partitionKey": "msisdn",
  "messageType": "input",
  "messageStructure": "mapped",
  "createMessageFrom": "header",
  "detectDatatypeFrom": 4
  }

After running the tool, the following messages are outputted:

::

  [RESULT] - message created successfully[RESULT] - you can find the file in this path /tmp/message_20160605102042.json

The message_20160605102042.json file includes:
::

  message_20160605102042.json

  {
  "Message": {
  "NameSpace": "com.ligadata.kamanja",
  "Name": "testmessage",
  "Verion": "00.01.00",
  "Description": "",
  "Fixed": "false",
  "Persist": "false",
  "Feilds": [{
  "Name": "msisdn",
  "Type": "System.Long"
  }, {
  "Name": "actNo",
  "Type": "System.Int"
  }, {
  "Name": "planName",
  "Type": "System.String"
  }, {
  "Name": "activationDate",
  "Type": "System.Int"
  }, {
  "Name": "thresholdAlertOptout",
  "Type": "System.Boolean"
  }],
  "PartitionKey": ["msisdn"]
  }
  }

To Generate a Message from PMML
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Go to $KAMANJA_HOME and run this command:


::

  GenerateMessage.sh --inputfile /opt/KamanjaGitTest/Kamanja/trunk/Utils/GenerateMessage/src/test/resources/DecisionTreeEnsembleIris.pmml --config /opt/KamanjaDoubleVersionsTest/Kamanja-1.5.0_2.10/config/ConfigFile_GeneratMessage.properties

Suppose the input file is DecisionTreeEnsembleIris.pmml
and the ConfigFile_GeneratMessage.properties file
includes the following information:

::

  ConfigFile_GeneratMessage.properties

  {
  "delimiter": ",",
  "outputPath": "/tmp",
  "saveMessage": "false",
  "nameSpace": "com.ligadata.kamanja",
  "partitionKey": "",
  "primaryKey": "",
  "timePartition": "",
  "messageType": "input",
  "messageStructure": "fixed",
  "createMessageFrom": "pmml"
  }

After running the tool, the following messages are outputted:

::

  [RESULT] - message created successfully[RESULT] - you can find the file in this path /tmp/message_20160615021006.json

The /tmp/message_20160615021006.json file includes:

::

  message_20160615021006.json

  {
  "Message": {
  "NameSpace": "com.ligadata.kamanja",
  "Name": "testmessage",
  "Version": "00.01.00",
  "Description": "",
  "Fixed": "true",
  "Persist": "false",
  "Fields": [{
  "Name": "Sepal_Length",
  "Type": "System.Double"
  }, {
  "Name": "Sepal_Width",
  "Type": "System.Double"
  }, {
  "Name": "Petal_Length",
  "Type": "System.Double"
  }, {
  "Name": "Petal_Width",
  "Type": "System.Double"
  }]
  }
  }

Suppose no input file is provided
and the ConfigFile_GeneratMessage.properties file includes
the following information:

::

  ConfigFile_GeneratMessage.properties

  {
  "delimiter": ",",
  "outputPath": "/tmp",
  "saveMessage": "false",
  "nameSpace": "com.ligadata.kamanja",
  "partitionKey": "",
  "primaryKey": "",
  "timePartition": "",
  "messageType": "output",
  "messageStructure": "fixed",
  "createMessageFrom": "pmml"
  }

After running the tool, the following messages are output:

::

  [RESULT] - no output message produced from file

This message was output because no output and/or target fields
are defined in the model.

Suppose the input file is DecisionTreeIris.pmml
and the ConfigFile_GeneratMessage.properties file
includes the following information:

::

  ConfigFile_GeneratMessage.properties

  {
  "delimiter": ",",
  "outputPath": "/tmp",
  "saveMessage": "false",
  "nameSpace": "com.ligadata.kamanja",
  "partitionKey": "",
  "primaryKey": "",
  "timePartition": "",
  "messageType": "output",
  "messageStructure": "fixed",
  "createMessageFrom": "pmml"
  }

After running the tool, the following messages are outputted:

::

  [RESULT] - The message changed to mapped because there are some ignored fields (P (Species=setosa),P (Species=versicolor),P (Species=virginica))[RESULT] - message created successfully[RESULT] - you can find the file in this path /tmp/message_20160615021451.json

The /tmp/message_20160615021451.json file includes:

::

  message_20160615021451.json

  {
  "Message": {
  "NameSpace": "com.ligadata.kamanja",
  "Name": "testmessage",
  "Version": "00.01.00",
  "Description": "",
  "Fixed": "false",
  "Persist": "false",
  "Fields": []
  }
  }

This message does not include any data in Fields
because all the fields in the PMML were invalid
and so the tool ignored the invalid characters.
Characters that are invalid are any special character
(such as @ or whitespace) except $ and _.

Suppose the input file is KMeansIris.pmml
and the ConfigFile_GeneratMessage.properties file
includes the following information:

::

  ConfigFile_GeneratMessage.properties

  {
  "delimiter": ",",
  "outputPath": "/tmp",
  "saveMessage": "false",
  "nameSpace": "com.ligadata.kamanja",
  "messageType": "output",
  "messageStructure": "fixed",
  "createMessageFrom": "pmml"
  }

After running the tool, the following messages are outputted:

::

  [RESULT] - message created successfully[RESULT] - you can find the file in this path /tmp/message_20160615022125.json

The /tmp/message_20160615022125.json file includes:

::

  message_20160615022125.json

  {
  "Message": {
  "NameSpace": "com.ligadata.kamanja",
  "Name": "testmessage",
  "Version": "00.01.00",
  "Description": "",
  "Fixed": "true",
  "Persist": "false",
  "Fields": [{
  "Name": "Cluster",
  "Type": "System.String"
  }]
  }
  }

See also
--------

- :ref:`message definition<message-def-config-ref>`

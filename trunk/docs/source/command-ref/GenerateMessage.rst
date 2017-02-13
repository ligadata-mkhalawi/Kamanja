

.. _generatemessage-command-ref:

GenerateMessage.sh
==================

Generate a :ref:`message definition<message-def-config-ref>`
from a file header or from :ref:`PMML<pmml-term>` DataDictionary.
The script calls a tool that is written in Scala
to extract the field names and types into the "Fields" section
of the message definition.
which saves you from having to type them all in manually.

To run the command, you populate a configuration file
that provides additional information about how to populate
the message definition.
To run the command, you supply the name of this configuration file
and the name of the file that contains the data source.

Syntax
------

::

  bash $KAMANJA_HOME/bin/GenerateMessage.sh \
    --inputfile $KAMANJA_HOME/input/SampleApplication/data/file.csv \
    --config $KAMANJA_HOME/config/file.json

Options and arguments
---------------------

- **--inputfile** - file from which to generate an input message definition.
  Each header in the specified file must be in a format
  that can be created as a Scala variable
  or that field is not generated.
  This means that field names cannot contain any special characters
  (such as @ or whitespace) except $ and _
  and cannot begin with a number.
  For example, "Cat" (enclosed in double quotes), C+at, and 1Cat
  are invalid;
  Cat, C_at, and Cat123 are valid.
- **--config** - JSON file that includes configuration parameters
  to generate an input message definition.

Configuration file structure
----------------------------

The structure of the configuration file
that is passed to **GenerateMessage.sh**
is similar to the following:

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
  This value must be at least 1
  and no larger than the number of rows in the data
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

It also creates a *message_<yyyymmddmmss>.json* file
that contains the generated message definition.

**GenerateMessage.sh** validates the syntax of the command
and the parameters in the configuration file and
generates an error message if it finds a problem.

Error messages returned for the syntax of the command
include the following Usage statement:

::

   WARN [main] - Usage:  bash $KAMANJA_HOME/bin/GenerateMessage.sh
   --inputfile $KAMANJA_HOME/input/SampleApplication/data/file.csv
   --config $KAMANJA_HOME/config/file.json

The error messages returned for the syntax of the command are:

- **ERROR [main] - This file does not exist.**

  An incorrect name or path was specified for the input file.

- **ERROR [main] - This file /opt/Kamanja/input/SampleApplications/data/test.csv
  does not include data. Check your file please.**

- **ERROR [main] - This file does not exist**

  An incorrect name or path was specified for the configuration file.

- **ERROR [main] - This file /opt/KamanjaDoubleVersionsTest/Kamanja-1.5.0_2.10/config/ConfigFile_GeneratMessagetest.properties
  does not include data. Check your file please.**

The following error messages identify errors in the values
supplied to the configuration file:

- **ERROR [main] - The value for saveMessage should be true or false**

- **ERROR [main] - The value of messageStructure should be fixed or mapped**

- **ERROR [main] - The value for createMessageFrom should be header or PMML**

- **ERROR [main] - The value of messageType should be input or output**

- **ERROR [main] - you pass 10 in detectdatatypeFrom
  and the file size equal to 8 records,
  please pass a number greater than 1 and less than the file size**

  This parameter should specify the number of rows to use;
  it cannot be set to a value greater than the number of rows
  in the data source.

- **ERROR [main] - test key from partitionKey/PrimaryKey/TimePartitionInfo
  does not exist in message fields. Choose another key please**


Examples
--------

Generate a Message from a File Header
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The first example generates a message definition
from a :ref:`CSV<csv-term>` file,
using the headers of that file for the "Name" of each field.
This is the *SubscriberInfo_Telecom.dat* file
that you can find in *SampleApplication/Telecom/data* directory
of your Kamanja installation:

::

  msisdn,actNo,planName,activationDate,thresholdAlertOptout
  4251114567,190345676,shared3G,20150720,false
  4251114568,190345677,shared3G,20150720,false
  4251114569,190345678,shared3G,20150718,false
  4251114570,190345679,individual1G,20150718,false

The *ConfigFile_GeneratMessage.properties* file
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


To generate the message definition, run this command:

::

    cd $KAMANJA_HOME
    ./GenerateMessage.sh
    --inputfile /opt/Kamanja/input/SampleApplications/data/SubscriberInfo_Telecom.dat
    --config /opt/KamanjaDoubleVersionsTest/Kamanja-1.5.0_2.10/config/ConfigFile_GeneratMessage.properties


Running the tool returns the following:

::

  [RESULT] - message created successfully
  [RESULT] - you can find the file in this path /tmp/message_20160605095817.json

The *message_20160605095817.json* file
contains the generated message definition:

::

  message_20160605095817.json

  {
  "Message": {
  "NameSpace": "com.ligadata.kamanja",
  "Name": "testmessage",
  "Version": "00.01.00", "Description": "",
  "Fixed": "true",
  "Persist": "false",
  "Fields": [{
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

The generated message contains five fields,
corresponding to the five headers in the CSV file,
and it guesses the "Type" for each field
based on the content of the first four rows in the file
because the **detectDatatypeFrom** parameter
in the configuration file is set to 4.
You can then manually edit the resulting message definition
to fine tune the types.
For example, the "actNo" field is defined as an integer
but you may want to treat this as "System.String" type.

Suppose that the *ConfigFile_GeneratMessage.properties* file
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

This configuration file does not contain the **partitionKey**
and the **timePartition** parameters.

After running the tool, the following messages are output:

::

  [RESULT] - message created successfully
  [RESULT] - you can find the file in this path /tmp/message_20160605102042.json

The *message_20160605102042.json* file
contains the generated message definition:

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
  "Fields": [{
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

This example generates a message definition from a PMML file.
The DataDictionary contains the following:

::

  <DataDictionary numberOfFields="5">
      <DataField dataType="double" name="Petal_Width" optype="continuous">
        <Interval closure="closedClosed" leftMargin="0.1" rightMargin="2.5"/>
      </DataField>
      <DataField dataType="double" name="Petal_Length" optype="continuous">
        <Interval closure="closedClosed" leftMargin="1.0" rightMargin="6.9"/>
      </DataField>
      <DataField dataType="double" name="Sepal_Length" optype="continuous">
        <Interval closure="closedClosed" leftMargin="4.3" rightMargin="7.9"/>
      </DataField>
      <DataField dataType="string" name="Species" optype="categorical">
        <Value value="setosa"/>
        <Value value="versicolor"/>
        <Value value="virginica"/>
      </DataField>
      <DataField dataType="double" name="Sepal_Width" optype="continuous">
        <Interval closure="closedClosed" leftMargin="2.0" rightMargin="4.4"/>
      </DataField>
    </DataDictionary>
    <MiningModel functionName="classification">
      <MiningSchema>
        <MiningField invalidValueTreatment="asIs" name="Sepal_Length"/>
        <MiningField invalidValueTreatment="asIs" name="Sepal_Width"/>
        <MiningField invalidValueTreatment="asIs" name="Petal_Length"/>
        <MiningField invalidValueTreatment="asIs" name="Petal_Width"/>
        <MiningField invalidValueTreatment="asIs" name="Species" usageType="target"/>
      </MiningSchema>


This is a sample *.properties* file
that contains configuration information
to generate a message from PMML.

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

The command to generate a message definition
based on this information is:


::

  cd $KAMANJA_HOME \
  GenerateMessage.sh
    --inputfile /opt/KamanjaGitTest/Kamanja/trunk/Utils/GenerateMessage/src/test/resources/DecisionTreeEnsembleIris.pmml \
    --config /opt/KamanjaDoubleVersionsTest/Kamanja-1.5.0_2.10/config/ConfigFile_GeneratMessage.properties


After running the tool, the following messages are output:

::

  [RESULT] - message created successfully
  [RESULT] - you can find the file in this path /tmp/message_20160615021006.json

The */tmp/message_20160615021006.json* file includes:

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

If you run the tool specifying this same
*ConfigFile_GeneratMessage.properties* file
but do not specify an input file,


After running the tool, the following messages are output:

::

  [RESULT] - no output message produced from file

This message was output because no output and/or target fields
are defined in the model.

Suppose the input file is **DecisionTreeIris.pmml**
and the **ConfigFile_GeneratMessage.properties** file
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

After running the tool, the following messages are output:

::

  [RESULT] - The message changed to mapped because there are some ignored fields
  (P (Species=setosa),P (Species=versicolor),P (Species=virginica))
  [RESULT] - message created successfully
  [RESULT] - you can find the file in this path /tmp/message_20160615021451.json

The */tmp/message_20160615021451.json* file includes:

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

Suppose the input file is **KMeansIris.pmml**,
which defines the following data:

::

    <DataDictionary numberOfFields="5">
        <DataField name="Sepal_Length" optype="continuous" dataType="double">
          <Interval closure="closedClosed" leftMargin="4.3" rightMargin="7.9"/>
        </DataField>
        <DataField name="Sepal_Width" optype="continuous" dataType="double">
          <Interval closure="closedClosed" leftMargin="2.0" rightMargin="4.4"/>
        </DataField>
        <DataField name="Petal_Length" optype="continuous" dataType="double">
          <Interval closure="closedClosed" leftMargin="1.0" rightMargin="6.9"/>
        </DataField>
        <DataField name="Petal_Width" optype="continuous" dataType="double">
          <Interval closure="closedClosed" leftMargin="0.1" rightMargin="2.5"/>
        </DataField>
        <DataField name="Species" optype="categorical" dataType="string">
          <Value value="setosa"/>
          <Value value="versicolor"/>
          <Value value="virginica"/>
        </DataField>
      </DataDictionary>
      <ClusteringModel modelName="k-means" functionName="clustering" modelClass="centerBased" numberOfClusters="3">
        <MiningSchema>
          <MiningField name="Sepal_Length" invalidValueTreatment="asIs"/>
          <MiningField name="Sepal_Width" invalidValueTreatment="asIs"/>
          <MiningField name="Petal_Length" invalidValueTreatment="asIs"/>
          <MiningField name="Petal_Width" invalidValueTreatment="asIs"/>
        </MiningSchema>


The **ConfigFile_GeneratMessage.properties** file
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

  [RESULT] - message created successfully
  [RESULT] - you can find the file in this path /tmp/message_20160615022125.json

The */tmp/message_20160615022125.json* file includes:

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



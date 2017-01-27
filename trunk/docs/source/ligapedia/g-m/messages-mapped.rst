
.. _messages-mapped-term:

Messages, mapped
----------------

:ref:`Message definitions<message-def-config-ref>`
can be :ref:`fixed<messages-fixed-term>` or mapped.
A mapped message represents the messages as a Scala map
[String, <some data type>] at runtime).

To use mapped messages:

1. In the :ref:`message definitions<message-def-config-ref>`,
   set Fixed:false to indicate a mapped message.

2. In the :ref:`adapter definition.json<adapter-def-config-ref>`,
   under the Adapters section of the configuration,
   change the DataFormat to JSON if using JSON data messages.
   Change the DataFormat to KV if using KV data messages.
   (This configures the input adapter to process data properly).

So, for example, in the message definition,
the following fields may exist if the data format is in KV:

::

  "DataFormat": "KV"
  "AssociatedMessage": "System.TestMsg"
  "KeyAndValueDelimiter" : "x01"
  "FieldDelimiter" : "x01"
  "ValueDelimiter" : ";"
	
  "DataFormat": "KV"
  "AssociatedMessage": "System.TestMsg"
  "KeyAndValueDelimiter" : "x01"
  "FieldDelimiter" : "x01"
  "ValueDelimiter" : ";"

3. Message data must be changed from CSV to either JSON or KV.

Look at an example mapped message definition in JSON.

::

  {
      "Message": {
          "NameSpace": "System",
          "Name": "Customer",
          "PhysicalName": "Customer",
          "Version": "00.01.00",
          "Description": "Basic Customer information",
          "Fixed": "false",
          "Elements": [
              {
                  "Field": {
                      "NameSpace": "System",
                      "Name": "CustomerID",
                      "Type": "System.string"
                  }
              },
              {
                  "Field": {
                      "NameSpace": "System",
                      "Name": "first_name",
                      "Type": "System.string"
                  }
              },
              {
                  "Field": {
                      "NameSpace": "System",
                      "Name": "last_name",
                      "Type": "System.string"
                  }
              }
          ],
          "PartitionKey": [
              "CustomerID"
          ],
          "PrimaryKey": [
              "CustomerID"
          ]
      }
    }
	
  {
      "Message": {
          "NameSpace": "System",
          "Name": "Customer",
          "PhysicalName": "Customer",
          "Version": "00.01.00",
          "Description": "Basic Customer information",
          "Fixed": "false",
          "Elements": [
              {
                  "Field": {
                      "NameSpace": "System",
                      "Name": "CustomerID",
                      "Type": "System.string"
                  }
              },
              {
                  "Field": {
                      "NameSpace": "System",
                      "Name": "first_name",
                      "Type": "System.string"
                  }
              },
              {
                  "Field": {
                      "NameSpace": "System",
                      "Name": "last_name",
                      "Type": "System.string"
                  }
              }
          ],
          "PartitionKey": [
              "CustomerID"
          ],
          "PrimaryKey": [
              "CustomerID"
            ]
      }
  }

The JSON message data is similar to:

::


  {
  "System.Customer": {
    "CustomerID":"a654ab",
    "first_name":"John",
    "last_name":"Doe"
    }
  }
	
  {
  "System.Customer": {
    "CustomerID":"a654ab",
    "first_name":"John",
    "last_name":"Doe"
    }
  }

The KV message data is similar to:

::

  field1data1field2data2field3data3field4data4field5data5field6data6

4. The command for SimpleKafkaProducer needs to be changed slightly
   to make JSON input of JSON work.
   In the following example, assume the CustomerInfo.json file
   contains some basic customer data:

::

  java -jar SimpleKafkaProducer-0.1.0
  --gz false
  --format json
  --topics “testin_1”
  --threads 1 --topicpartitions 8
  --brokerlist “localhost:9092”
  --files /location/of/CustomerInfo.json
  --partitionkeyidxs “System.Customer:CustomerID”

Where:

— **format** indicates the format of the data
  (either CSV or JSON).
— **partitionkeyidxs <System.MessageName>:<PartitionKey>**
  indicates the message name and the partition key
  by which to aggregate other messages in the message history.

Note: —partitionkeyidxs can be comma-delimited
if the file contains multiple message types.

For example:

::

  -partitionkeyidxs “System.Customer:CustomerID,
  System.CustomerAddress:CustomerID,
  System.CustomerPreferences:CustomerID”.



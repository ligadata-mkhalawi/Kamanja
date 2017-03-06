

.. _message-def-config-ref:

Message definition
==================

The :ref:`message<messages-term>` definition structure provides
design-time definitions of data and interfaces
that makes it easy for Kamanja to process messages.

Each message used in the cluster
has its own "Message" definition,
identified by a unique "Name".

A single defined message can serve as input to a model,
output from a model,
or output to the cluster's data warehouse;
its specific roll is determined by the
:ref:`adapter bindings<adapter-binding-config-ref>`

File structure
--------------

::

  {
      "Message": {
      	"NameSpace": "<namespace>",
      	"Name": "<name>",
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


Parameters
----------

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



Usage
-----

The easiest way to generate a message definition
is to use the :ref:`generatemessage-command-ref` command
which automatically generates a message definition
from the specified data source.
You can edit the message definition it generates if necessary,
usually to correct the "Type" assigned to some fields.

Input messages that do not match the message definition
are handled as follows:

- If a message input is defined as fixed,
  the message fails entirely.
  The CSV used as an input must be fixed in length and order.

- If a message input is defined as mapped,
  the correct fields are selected and unknown fields are ignored,
  as long as the message type is declared correctly.


Examples
--------

This is a sample message definition with time partitioning:

::

  {
    "Message": {
      "NameSpace": "com.ligadata.messages",
      "Name": "TimePartitionMsgDef",
      "Version": "00.01.00",
      "Description": "Time Partition Message Definition",
      "Fixed": "true",
      "Persist": "false",
      "Fields": [
        {
          "Name": "field0",
          "Type": "System.String"
        },
        {
          "Name": "field2",
          "Type": "System.String"
        },
        {
          "Name": "field3",
          "Type": "System.String"
        },
        {
          "Name": "field4",
          "Type": "System.String"
        },
        {
          "Name": "field5",
          "Type": "System.String"
        },
        {
          "Name": "DateTime",
          "Type": "System.String"
        }
      ],
      "TimePartitionInfo": {
        "Key": "DateTime",
        "Format": "epochTime",
        "Type": "Daily"
      }
    }
  }


See also
--------

- :ref:`GenerateMessage.sh<generatemessage-command-ref>`
- :ref:`message-ref-top`



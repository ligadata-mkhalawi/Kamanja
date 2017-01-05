
.. _message-bindings-guide:

Message bindings
================

Adapter message bindings are independently cataloged
:ref:`metadata<metadata-term>` objects
that associate three other metadata objects together:

- an adapter (input, output, or storage).
- a message that this adapter either deserializes from its source
  or serializes to send to its sink.
- the serializer to use that understands
  how to serialize and deserialize the message.

These bindings provide a flexible way to describe
the input, output, and storage streams
flowing to/from the adapter’s sink/source.
Being able to dial in the sort of serialization expected
or provided to others gives the Kamanja system administrator
a lot of flexibility to satisfy his/her objectives.

There are currently three builtin serializers provided
in the Kamanja distribution that can be used:

- a JSON serializer
- a CSV serializer
- :ref:`KBinary<kbinary-term>` serializer.

The Kamanja platform mostly uses KBinary
to manage data in its stores and caches.

The adapter message bindings can be ingested one at a time.
If there are multiple messages that are managed by an adapter
that all share the same serializer,
a compact representation is possible.
It is also possible to organize
one or more adapter message binding specifications
in a file and have the Kamanja ingestion tools consume that.

Note that adapters, messages, and serializers
must already be cataloged in the metadata
before a user can add a binding for them.
A rather comprehensive list of errors is returned if that is not the case.

Adapter Message Binding Examples
--------------------------------

What follows is a number of annotated examples
that illustrate the bindings
and how a user might use them to configure a cluster.
The local :ref:`kamanja<kamanja-command-ref>` command is used here.

First, the pretty-printed JSON is presented
that is submitted followed by the kamanja command
that ingests a flattened version of the same JSON
presented as the value to the FROMSTRING named parameter.
Where appropriate, commentary is presented
that further explains details about the JSON and/or the ingestion command.

For the commands,
a number of symbolic references are used to make the locations abstract.
Build ENV variables for them in the console session.
Briefly, their meanings are:

- KAMANJA_HOME – root directory of the Kamanja installation.
- APICONFIGPROPERTIES – file path that describes
  the MetadataAPI configuration file used in the command.
- METADATAROOT – directory that contains
  the organized set of configuration, model, and message source
  to deploy on the Kamanja cluster.

Input Adapter Message Binding
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To create the following binding:

::

  {
    "AdapterName": "kafkaAdapterInput1",
    "MessageName": "com.botanical.json.ordermsg",
    "Serializer": " com.ligadata.kamanja.serializer.JsonSerDeser"
  }

Run this command:

::

  $KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES add adaptermessagebinding \
    FROMSTRING ‘{“AdapterName”: “kafkaAdapterInput1”, “MessageName”: “com.botanical.json.ordermsg”, \
    “Serializer”: ” com.ligadata.kamanja.serializer.JsonSerDeser”}’

The keys in the JSON specification are case-sensitive.
For example, use AdapterName, not ADAPTERname or adapter name.

Note that the adapter message binding sent to the Kamanja script
is just a flattened version of the JSON map structure presented.

The serializer used in this case
is the name of the builtin CSV deserializer.
Note too that it has an options map that is used to configure it.

Notice that the MessageName key is used in the example.
It allows a single message to be specified.
It is also possible to specify multiple messages.
See the examples below.

Output Adapter Message Binding
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To create the following binding:

::

  {
    "AdapterName": "kafkaAdapterOutput2",
    "MessageNames": ["com.botanical.csv.emailmsg"],
    "Serializer": "com.ligadata.kamanja.serializer.csvserdeser",
    "Options": {
      "lineDelimiter": "rn",
      "fieldDelimiter": ",",
      "produceHeader": true,
      "alwaysQuoteFields": false
    }
  }

Run this command:

::

  $KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES add adaptermessagebinding \
     FROMSTRING ‘{“AdapterName”: “kafkaAdapterOutput2”, \
     “MessageNames”: [“com.botanical.csv.emailmsg”],
     “Serializer”: “com.ligadata.kamanja.serializer.csvserdeser”, \
     “Options”: {“lineDelimiter”: “rn”, “fieldDelimiter”: “,”,
     “produceHeader”: true, “alwaysQuoteFields”: false } }’

This output adapter uses the delimitedserdeser
(CSV for short) builtin adapter,
but notice too that there is one message mentioned
in the MessageNames key value.
What actually happens is that binding metadata is built
for each adapter/message/serializer combination.
Think of this as shorthand.

If the key for the message is MessageName,
then only the message name is given.
If MessageNames (plural), an array of names is expected.

Storage Adapter Message Binding
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To create the following binding:

::

  {
    "AdapterName": "hBaseStore1",
    "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"],
    "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"
  }

Run this command:

::

  $KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES add adaptermessagebinding FROMSTRING ‘{“AdapterName”: “hBaseStore1”, “MessageNames”: [“com.botanical.json.audit.ordermsg”, “com.botanical.json.audit.shippingmsg”], “Serializer”: “com.ligadata.kamanja.serializer.JsonSerDeser”}’

In this storage adapter binding,
the use of the builtin JSON adapter is illustrated.
Again, there are multiple messages specified.
The JSON serializer doesn’t currently have any options, so it can be omitted.

Storage Adapter Message Binding Specifications in a File
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

File ingestion of the adapter message bindings is also possible.
One file can include multiple binding specifications.
For example,

::

  [
	  {
	    "AdapterName": "kafkaAdapterInput1",
	    "MessageNames": ["com.botanical.json.ordermsg", "com.botanical.json.shippingmsg"],
	    "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"
	  },
	  {
	    "AdapterName": "kafkaAdapterOutput2",
	    "MessageNames": ["com.botanical.csv.emailmsg"],
	    "Serializer": "com.ligadata.kamanja.serializer.csvserdeser",
	    "Options": {
		  "lineDelimiter": "rn",
		  "fieldDelimiter": ",",
		  "produceHeader": "true",
		  "alwaysQuoteFields": "false"
	    }
	  },
	  {
	    "AdapterName": "hBaseStore1",
	    "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"],
	    "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"
	  }
  ]

There are a total of five bindings specified here.
This is a practical way to establish a new cluster.

A binding is prepared for the adapter/message/serializers in each map.
As can be seen in the kafkaAdapterOutput2 and hBaseStore1 adapters,
the multiple message shorthand is used
that causes a binding for each unique triple.

If the above file was called
**$METADATAROOT/config/AdapterMessageBindingsForClusterConfig1.5.0.json**,
the following Kamanja command can ingest it:

::

  $KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES add adaptermessagebinding FROMFILE $METADATAROOT/config/AdapterMessageBindingsForClusterConfig1.5.0.json

Like the other examples,
this can be pushed directly on the command-line
as a FROMSTRING parameter value.
It is helpful to have an editor that can pretty-print/flatten
the JSON text to do these more complicated structures for the command-line:

::

  $KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES add adaptermessagebinding FROMSTRING ‘[{“AdapterName”: “kafkaAdapterInput1”, “MessageNames”: [“com.botanical.json.ordermsg”, “com.botanical.json.shippingmsg”], “Serializer”: “com.ligadata.kamanja.serializer.JsonSerDeser”}, {“AdapterName”: “kafkaAdapterOutput2”, “MessageNames”: [“com.botanical.csv.emailmsg”], “Serializer”: “com.ligadata.kamanja.serializer.csvserdeser”, “Options”: {“lineDelimiter”: “rn”, “fieldDelimiter”: “,”, “produceHeader”: “true”, “alwaysQuoteFields”: “false”} }, {“AdapterName”: “hBaseStore1”, “MessageNames”: [“com.botanical.json.audit.ordermsg”, “com.botanical.json.audit.shippingmsg”], “Serializer”: “com.ligadata.kamanja.serializer.JsonSerDeser”} ]’

List Adapter Message Bindings
-----------------------------

The following list of commands are supported:

List them all:

::

  $KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES list adaptermessagebindings

List bindings for the supplied adapter:

::

  $KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES list adaptermessagebindings ADAPTERFILTER hBaseStore1

List bindings for a given message:

::

  $KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES list adaptermessagebindings MESSAGEFILTER com.botanical.csv.emailmsg

List bindings for a given serializer:

::

  $KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES list adaptermessagebindings SERIALIZERFILTER com.ligadata.kamanja.serializer.JsonSerDeser

Remove Adapter Message Binding
------------------------------

Removal of a binding is accomplished with this command.

Remove the supplied binding key:

The binding key, the parameter KEY’s value,
consists of the names of the three components of the binding,
namely the adapter name, the fully-qualified message name,
and the fully-qualified serializer name – comma separated:

::

  $KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES remove adaptermessagebinding KEY ‘hBaseStore1,com.botanical.json.audit.ordermsg,com.ligadata.kamanja.serializer.JsonSerDeser’



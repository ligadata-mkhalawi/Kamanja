****************************
**Adapter Message Bindings**

Adapter message bindings are independently cataloged metadata objects that associate three other metadata objects together:

- An adapter (input, output, or storage)
- A message that this adapter either deserializes from its source or serializes to send to its sink.
- The serializer to use that understands how to serialize and deserialize the message.

These bindings provide a flexible way to describe the input, output and storage streams flowing to/from the adapter's sink/source.  Being able to dial in the sort of serialization expected or provided to others gives the Kamanja system configurator lots of flexibility to satisfy their objectives.

There are currently three builtin serializers provided in the Kamanja distribution that can be used: a JSON serializer, a CSV serializer and a KBinary serializer.  The KBinary is used principally by the Kamanja platform to manage data in its stores and caches.

The adapter message bindings can be ingested one at a time (see the [input adapter example below]).  If there are multiple messages that are managed by an adapter that all share the same serializer, a compact representation is possible (see the [output adapter example below]).  It is also possible to organize one or more adapter message binding specifications in a file and have the Kamanja ingestion tools consume that.

Note that _all_ adapters, messages, and serializers _must already be cataloged in the metadata_ before one can add a binding for them.  A rather comprehensive list of errors will be returned if that is not the case.


************************************
**Adapter Message Binding Examples**

What follows is a number of annotated examples that illustrate the bindings and how one might use them to configure a cluster.  The local kamanja command is used here.  See [The wiki page for the MetadataAPI service] to see the service rendition of commands like these.

First the pretty-printed JSON is presented that will be submitted followed by the _kamanja_ command that ingests a _flattened_ version of the same JSON presented as the value to the _FROMSTRING_ named parameter.  Where appropriate commentary is presented that further explain details about the JSON and/or the ingestion command.

For the commands, a number of symbolic references are used to abstract away the locations. You would build ENV variables for them in your console session.  Briefly their meanings are:

	KAMANJA_HOME - the root directory of your Kamanja installation.
	
	APICONFIGPROPERTIES a file path that describes the MetadataAPI configuratoin file used on the command.
	
	METADATAROOT - a directory that contains the organized set of configuration, model and message source one plans to deploy on the Kamanja cluster.


**Input Adapter Message Binding**

{
  "AdapterName": "kafkaAdapterInput1",
  "MessageName": "com.botanical.json.ordermsg",
  "Serializer": " com.ligadata.kamanja.serializer.JsonSerDeser"
}

	$KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES add adaptermessagebinding FROMSTRING '{"AdapterName": "kafkaAdapterInput1", "MessageName": "com.botanical.json.ordermsg", "Serializer": " com.ligadata.kamanja.serializer.JsonSerDeser"}'


_The keys in the JSON specification are case sensitive.  For example, use "AdapterName", not "ADAPTERname" or "adaptername"._

Note that the adapter message binding sent to the kamanja script is just a flattened version of the json map structure presented.    

The serializer used in this case is the name of the builtin CSV deserializer.  Note too that it has an options map that is used to configure it.  See the [description of CSV serializer] for what the options mean.

Notice that the _MessageName_ key is used in the example.  It allows a single message to be specified.  It is also possible to specify multiple messages.  See the examples below.


**Output Adapter Message Binding**
{
  "AdapterName": "kafkaAdapterOutput2",
  "MessageNames": ["com.botanical.csv.emailmsg"],
  "Serializer": "com.ligadata.kamanja.serializer.delimitedserdeser",
  "Options": {
    "lineDelimiter": "\r\n",
    "fieldDelimiter": ",",
    "produceHeader": true,
    "alwaysQuoteFields": false
  }
}


	$KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES add adaptermessagebinding FROMSTRING '{"AdapterName": "kafkaAdapterOutput2", "MessageNames": ["com.botanical.csv.emailmsg"], "Serializer": "com.ligadata.kamanja.serializer.delimitedserdeser", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

This output adapter uses the delimitedserdeser (CSV for short) builtin adapter, but notice too that there are two messages mentioned in the MessageNames key value.  What actually happens is that binding metadata is built for each adapter/message/serializer combination.  Think of this as a short hand.

If the key for the message is "MessageName", then only message name is given.  If "MessageNames" (plural), an array of names is expected.

**Storage Adapter Message Binding**
{
  "AdapterName": "hBaseStore1",
  "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"],
  "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"
}

	$KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES add adaptermessagebinding FROMSTRING '{"AdapterName": "hBaseStore1", "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"}'

In this storage adapter binding, the use of the builtin JSON adapter is illustrated.  Again there are multiple messages specified.  The JSON serializer doesn't currently have any options, so it can be omitted.

**Storage Adapter Message Binding Specifications in a File**

File ingestion of the adapter message bindings is also possible.  With a file  multiple binding specifications can be specified in it.  For example,

[
	{
	  "AdapterName": "kafkaAdapterInput1",
	  "MessageNames": ["com.botanical.json.ordermsg", "com.botanical.json.shippingmsg"],
	  "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"
	},
	{
	  "AdapterName": "kafkaAdapterOutput2",
	  "MessageNames": ["com.botanical.csv.emailmsg"],
	  "Serializer": "com.ligadata.kamanja.serializer.delimitedserdeser",
	  "Options": {
		"lineDelimiter": "\r\n",
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

There are a total of five bindings specified here.  This is a practical way to establish a new cluster.

A binding is prepared for the adapter/message/serializers in each map.  As can be seen in the kafkaAdapterOutput2 and hBaseStore1 adapters, the multiple message shorthand is used that will cause a binding for each unique triple.

If the above file was called $METADATAROOT/config/AdapterMessageBindingsForClusterConfig1.4.0.json, the following Kamanja command can ingest it:

	$KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES add adaptermessagebinding FROMFILE $METADATAROOT/config/AdapterMessageBindingsForClusterConfig1.4.0.json

Like the other examples, you can also push this directly on the command line as a FROMSTRING parameter value.  It is helpful to have an editor that can pretty print/flatten the JSON text to do these more complicated structures for the command line:

	$KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES add adaptermessagebinding FROMSTRING '[{"AdapterName": "kafkaAdapterInput1", "MessageNames": ["com.botanical.json.ordermsg", "com.botanical.json.shippingmsg"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"}, {"AdapterName": "kafkaAdapterOutput2", "MessageNames": ["com.botanical.csv.emailmsg"], "Serializer": "com.ligadata.kamanja.serializer.delimitedserdeser", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": "true", "alwaysQuoteFields": "false"} }, {"AdapterName": "hBaseStore1", "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"} ]'



**List Adapter Message Bindings**

The following list commands are supported:

_List them all_
	$KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES list adaptermessagebindings 
_List bindings for the supplied adapter_
	$KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES list adaptermessagebindings ADAPTERFILTER hBaseStore1
_List bindings for a given message_
	$KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES list adaptermessagebindings MESSAGEFILTER com.botanical.csv.emailmsg
_List bindings for a given serializer_
	$KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES list adaptermessagebindings SERIALIZERFILTER com.ligadata.kamanja.serializer.JsonSerDeser

**Remove Adapter Message Binding**

Removal of a binding is accomplished with this command.

_Remove the supplied binding key_

The binding key, the parameter KEY's value, consists of the names of the three components of the binding, namely the adapter name, the fully qualified message name, and the fully qualified serializer name - comma separated:

	$KAMANJA_HOME/bin/kamanja $APICONFIGPROPERTIES remove adaptermessagebinding KEY 'hBaseStore1,com.botanical.json.audit.ordermsg,com.ligadata.kamanja.serializer.JsonSerDeser'




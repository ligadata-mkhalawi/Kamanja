****************************
**Adapter Message Bindings**

Adapter message bindings are independently cataloged metadata objects that associate three other metadata objects together:

- An adapter (input, output, or storage)
- A message that this adapter either deserializes from its source or serializes to send to its sink.
- The serializer to use that understands how to serialize and deserialize the message.

These bindings provide a flexible way to describe the input, output and storage streams flowing to/from the adapter's sink/source.  Being able to dial in the sort of serialization expected or provided to others gives the Kamanja system configurator lots of flexibility to satisfy their objectives.

There are currently three builtin serializers provided in the Kamanja distribution that can be used: a JSON serializer, a CSV serializer and a KBinary serializer.  The KBinary is used principally by the Kamanja platform to manage data in its stores and caches.

The adapter message bindings can be ingested one at a time (see the [input adapter example below]).  If there are multiple messages that are managed by an adapter that all share the same serializer, a compact representation is possible (see the [output adapter example below]).  It is also possible to organize one or more adapter message binding specifications in a file and have the Kamanja ingestion tools consume that.

************************************
**Adapter Message Binding Examples**

**_NOTE_: All of the adapters are defined in trunk/MetadataAPI/src/test/resources/Metadata/config/ClusterConfig.1.4.0.json.  The com.botanical.* messages mentioned are defined in trunk/MetadataAPI/src/test/resources/Metadata/message/. See the _Test Setup_ below.**

What follows is a number of annotated examples that illustrate the bindings and how one might use them to configure a cluster.  The local kamanja command is used here.  See [The wiki page for the MetadataAPI service] to see the service rendition of commands like these.

First the pretty-printed JSON is presented that will be submitted followed by the _kamanja_ command that ingests a _flattened_ version of the same JSON presented as the value to the _FROMSTRING_ named parameter.  Where appropriate commentary is presented that further explain details about the JSON and/or the ingestion command.


**Input Adapter Message Binding**

{
  "AdapterName": "kafkaAdapterInput1",
  "MessageName": "com.botanical.json.ordermsg",
  "Serializer": " com.ligadata.kamanja.serializer.JsonSerDeser"
}

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "kafkaAdapterInput1", "MessageName": "com.botanical.json.ordermsg", "Serializer": " com.ligadata.kamanja.serializer.JsonSerDeser"}'


_The keys in the JSON specification are case sensitive.  For example, use "AdapterName", not "ADAPTERname" or "adaptername"._

Note that the adapter message binding sent to the kamanja script is just a flattened version of the json map structure presented.    

The serializer used in this case is the name of the builtin CSV deserializer.  Note too that it has an options map that is used to configure it.  See the [description of CSV serializer] for what the options mean.

Notice that the _MessageName_ key is used in the example.  It allows a single message to be specified.  It is also possible to specify multiple messages.  See the examples below.


**Output Adapter Message Binding**
{
  "AdapterName": "kafkaAdapterOutput2",
  "MessageNames": ["com.botanical.csv.emailmsg"],
  "Serializer": "com.ligadata.kamanja.serializer.csvserdeser",
  "Options": {
    "lineDelimiter": "\r\n",
    "fieldDelimiter": ",",
    "produceHeader": true,
    "alwaysQuoteFields": false
  }
}


	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "kafkaAdapterOutput2", "MessageNames": ["com.botanical.csv.emailmsg"], "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

This output adapter uses the csvserdeser (CSV for short) builtin adapter, but notice too that there are two messages mentioned in the MessageNames key value.  What actually happens is that binding metadata is built for each adapter/message/serializer combination.  Think of this as a short hand.

If the key for the message is "MessageName", then only message name is given.  If "MessageNames" (plural), an array of names is expected.

**Storage Adapter Message Binding**
{
  "AdapterName": "hBaseStore1",
  "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"],
  "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"
}

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "hBaseStore1", "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"}'

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
	  "Serializer": "com.ligadata.kamanja.serializer.csvserdeser",
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

There are a total of five bindings specified here.  This is a practical way to describe the adapter bindings when establishing a new cluster.

A binding is prepared for the adapter/message/serializers in each map.  As can be seen in the kafkaAdapterOutput2 and hBaseStore1 adapters, the multiple message shorthand is used that will cause a binding for each unique triple.

If the above file was called $MetadataDir/config/AdapterMessageBindingsForClusterConfig1.4.0.json, the following Kamanja command can ingest it:

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $MetadataDir/config/AdapterMessageBindingsForClusterConfig1.4.0.json

Like the other examples, you can also push this directly on the command line too.  It is helpful to have an editor that can pretty print/flatten the JSON text to do these more complicated structures for the command line:

	export DBG="debug"
	export DBG=""

	$KAMANJA_HOME/bin/kamanja $DBG $apiConfigProperties add adaptermessagebinding FROMSTRING '[{"AdapterName": "kafkaAdapterInput1", "MessageNames": ["com.botanical.json.ordermsg", "com.botanical.json.shippingmsg"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"}, {"AdapterName": "kafkaAdapterOutput2", "MessageNames": ["com.botanical.csv.emailmsg"], "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": "true", "alwaysQuoteFields": "false"} }, {"AdapterName": "hBaseStore1", "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"} ]'


**List Adapter Message Bindings**

The following list commands are supported:

_List them all_
	$KAMANJA_HOME/bin/kamanja $apiConfigProperties list adaptermessagebindings 
_List bindings for the supplied adapter_
	$KAMANJA_HOME/bin/kamanja $apiConfigProperties list adaptermessagebindings ADAPTERFILTER hBaseStore1
_List bindings for a given message_
	$KAMANJA_HOME/bin/kamanja $apiConfigProperties list adaptermessagebindings MESSAGEFILTER com.botanical.csv.emailmsg
_List bindings for a given serializer_
	$KAMANJA_HOME/bin/kamanja $apiConfigProperties list adaptermessagebindings SERIALIZERFILTER com.ligadata.kamanja.serializer.JsonSerDeser

**Remove Adapter Message Binding**

Removal of a binding is accomplished with this command.

_Remove the supplied binding key_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding key 'hBaseStore1,com.botanical.json.audit.ordermsg,com.ligadata.kamanja.serializer.JsonSerDeser'


[
	"kafkaadapterinput1,com.botanical.json.ordermsg,com.ligadata.kamanja.serializer.jsonserdeser",
	"kafkaadapterinput1,com.botanical.json.shippingmsg,com.ligadata.kamanja.serializer.jsonserdeser",
	"kafkaadapteroutput2,com.botanical.csv.emailmsg,com.ligadata.kamanja.serializer.csvserdeser",
	"hbasestore1,com.botanical.json.audit.ordermsg,com.ligadata.kamanja.serializer.jsonserdeser",
	"hbasestore1,com.botanical.json.audit.shippingmsg,com.ligadata.kamanja.serializer.jsonserdeser"
]

_From String_
$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding FROMSTRING '["kafkaadapterinput1,com.botanical.json.ordermsg,com.ligadata.kamanja.serializer.jsonserdeser", "kafkaadapterinput1,com.botanical.json.shippingmsg,com.ligadata.kamanja.serializer.jsonserdeser", "kafkaadapteroutput2,com.botanical.csv.emailmsg,com.ligadata.kamanja.serializer.csvserdeser", "hbasestore1,com.botanical.json.audit.ordermsg,com.ligadata.kamanja.serializer.jsonserdeser", "hbasestore1,com.botanical.json.audit.shippingmsg,com.ligadata.kamanja.serializer.jsonserdeser"]'

_From File_
$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding FROMFILE $MetadataDir/config/AdapterMessageBindingsDeleteForClusterConfig1.4.0.json

**Failure test cases**

Each test case has a one message and multiple message version.

_mapped message used in following test case_

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $MetadataDir/message/com.botanical.csv.mapped.emailmsg.json tenantid "botanical"

$KAMANJA_HOME/bin/kamanja $DBG $apiConfigProperties remove adaptermessagebinding key 'kafkaAdapterOutput2,com.botanical.csv.mappedemailmsg,com.ligadata.kamanja.serializer.csvserdeser'

_csv serializer with mapped message_

	$KAMANJA_HOME/bin/kamanja $DBG $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "kafkaAdapterOutput2", "MessageNames": ["com.botanical.csv.mappedemailmsg"], "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

_csv serializer with container field_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "kafkaAdapterOutput2", "MessageNames": ["com.botanical.json.shippingmsg2"], "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'


_bad adapter name_
	
	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "badAdapterName", "MessageNames": ["com.botanical.json.audit.ordermsg"], "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "badAdapterName", "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"}'

_bad message name_
	
	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "kafkaAdapterOutput2", "MessageNames": ["com.botanical.csv.badmsg"], "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "hBaseStore1", "MessageNames": ["com.botanical.json.audit.badmsg", "com.botanical.json.audit.badmsgtoo"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"}'

_bad serializer name_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "kafkaAdapterOutput2", "MessageNames": ["com.botanical.csv.emailmsg"], "Serializer": "com.ligadata.kamanja.serializer.badSerializer", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "hBaseStore1", "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"], "Serializer": "com.ligadata.kamanja.serializer.badserializer"}'

_bad adapter name AND bad message name_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "badAdapterName", "MessageNames": ["com.botanical.json.audit.badmsg"], "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "badAdaptur", "MessageNames": ["com.botanical.json.audit.badMess", "com.botanical.json.audit.badMessToo"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"}'

_bad adapter name AND bad serializer name_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "badAdapterName", "MessageNames": ["com.botanical.csv.emailmsg"], "Serializer": "com.ligadata.kamanja.serializer.badserializer", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "badAdapterName", "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"], "Serializer": "com.ligadata.kamanja.serializer.badserializer"}'

_bad message name AND bad serializer name_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "kafkaAdapterOutput2", "MessageNames": ["com.botanical.csv.badmsg"], "Serializer": "com.ligadata.kamanja.serializer.badserializer", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "hBaseStore1", "MessageNames": ["com.botanical.json.audit.badmsg", "com.botanical.json.audit.badmsgtoo"], "Serializer": "com.ligadata.kamanja.serializer.badserializer"}'

_bad adapter name, bad message name, and bad serializer name_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "badAdapture", "MessageNames": ["com.botanical.csv.badmsg"], "Serializer": "com.ligadata.kamanja.serializer.badserializer", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "unreasonableadaptername", "MessageNames": ["com.botanical.json.audit.ordermsg", "com.botanical.json.audit.shippingmsg"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"}'

_message with container type fields and csvserdeser serializer_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "kafkaAdapterOutput2", "MessageNames": ["com.botanical.json.audit.ordermsg"], "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"lineDelimiter": "\r\n", "fieldDelimiter": ",", "produceHeader": true, "alwaysQuoteFields": false } }'

_command with invalid json_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{ ["com.botanical.zer : "com.ligadata.kamanja.serializer.JsonSerDeser"}'

**Test Setup**

To run the examples above (and expect them to succeed), the cluster configuration with the adapters defined in it as well as the com.botanical* messages need to be defined.  

_Define the path locations (adjust these)_

export KAMANJA_HOME=/tmp/drdigital/Kamanja-1.4.0_2.11
export KAMANJA_SRCDIR=~/github/dev/1.4.0.Base/kamanja/trunk
export MetadataDir=$KAMANJA_SRCDIR/MetadataAPI/src/test/resources/Metadata
export apiConfigProperties=$KAMANJA_HOME/config/Cassandra_MetadataAPIConfig.properties
_Install the parameterized version of SetPaths.sh called setPathsFor.scala (optional)_

This step uses a script called setPathsFor.scala that will do the same thing that the SampleApplication/EasyInstall/SetPaths.sh script does for some of the fixed examples we have in the distribution.  This does it for arbitrary files... one at a time.  The script is somewhat useful for setting up testing configurations.  Install it if you like on your PATH.  It can be found at trunk/Utils/Script/setPathsFor.scala 

Alternatively the template may be edited by hand if you're really old school.

_Configure the cluster configuration json file with cassandra storage values (make location substitutions... assumes that the JAVA_HOME and SCALA_HOME env variables are set in your system)_

setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/ClusterConfig.1.4.0_Template.json --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName testdata --schemaLocation localhost > $KAMANJA_HOME/config/adapterMessageBindingClusterConfig.json

_Configure the metadata api config file to use.  In this case, the cassandra values are used (make location substitutions... assumes that the JAVA_HOME and SCALA_HOME env variables are set in your system)_

setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/Cassandra_MetadataAPIConfig_Template.properties --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName metadata --schemaLocation localhost > $KAMANJA_HOME/config/Cassandra_MetadataAPIConfig.properties

_Configure the engine config file to use.  In this case, the cassandra values are used. NOTE: THE CURRENT Engine1Config.properties is overwritten_

setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/Engine1Config_Template.properties --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName metadata --schemaLocation localhost > $KAMANJA_HOME/config/Engine1Config.properties

_Define the cluster info_

$KAMANJA_HOME/bin/kamanja $apiConfigProperties upload cluster config $KAMANJA_HOME/config/adapterMessageBindingClusterConfig.json

_Define the system bindings_

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/SystemMsgs_Adapter_Binding.json

_Define the containers_

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add container $MetadataDir/container/packageDescr.json TENANTID "botanical"

_Define the messages_

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $MetadataDir/message/com.botanical.csv.emailmsg.json tenantid "botanical"
$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $MetadataDir/message/com.botanical.json.shippingmsg.json tenantid "botanical"
$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $MetadataDir/message/com.botanical.json.shippingmsg.withcontainerfield.json tenantid "botanical"
$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $MetadataDir/message/com.botanical.json.audit.shippingmsg.json tenantid "botanical"
$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $MetadataDir/message/com.botanical.json.audit.ordermsg.json tenantid "botanical"
$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $MetadataDir/message/com.botanical.json.ordermsg.json tenantid "botanical"


$KAMANJA_HOME/bin/kamanja $apiConfigProperties get all messages

Once complete return to the **Adapter Message Binding Examples** section and try out the adapter messasge binding add / remove commands.




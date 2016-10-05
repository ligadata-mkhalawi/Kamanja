export KAMANJA_HOME=/tmp/drdigital/Kamanja-1.5.0_2.11
export KAMANJA_SRCDIR=~/github/dev/1.5.0.Test/kamanja/trunk
export TestBin=$KAMANJA_SRCDIR/MetadataAPI/src/test/resources/bin
export MetadataDir=$KAMANJA_SRCDIR/MetadataAPI/src/test/resources/Metadata
export apiConfigProperties=$KAMANJA_HOME/config/MetadataAPIConfig.properties
_Install the parameterized version of SetPaths.sh called setPathsFor.scala (optional)_

This step uses a script called setPathsFor.scala that will do the same thing that the SampleApplication/EasyInstall/SetPaths.sh script does for some of the fixed examples we have in the distribution.  This does it for arbitrary files... one at a time.  The script is somewhat useful for setting up testing configurations.  Install it if you like on your PATH.  It can be found at trunk/Utils/Script/setPathsFor.scala 

Alternatively the template may be edited by hand if you're really old school.

_Configure the cluster configuration json file with cassandra storage values (make location substitutions... assumes that the JAVA_HOME and SCALA_HOME env variables are set in your system).  NOTE: THE CURRENT ClusterConfig.json is overwritten_

	setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/ClusterConfig_Template.json --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName testdata --schemaLocation localhost > $KAMANJA_HOME/config/ClusterConfig.json

_Configure the metadata api config file to use.  In this case, the cassandra values are used (make location substitutions... assumes that the JAVA_HOME and SCALA_HOME env variables are set in your system).  NOTE: THE CURRENT MetadataAPIConfig.properties is overwritten_

	setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/Cassandra_MetadataAPIConfig_Template.properties --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName metadata --schemaLocation localhost > $KAMANJA_HOME/config/MetadataAPIConfig.properties

_Configure the engine config file to use.  In this case, the cassandra values are used. NOTE: THE CURRENT Engine1Config.properties is overwritten_

	setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/Engine1Config_Template.properties --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName metadata --schemaLocation localhost > $KAMANJA_HOME/config/Engine1Config.properties

_Define the cluster info_
	$KAMANJA_HOME/bin/kamanja $apiConfigProperties upload cluster config $KAMANJA_HOME/config/ClusterConfig.json

_Start the engine_

bin/StartEngine.sh 

bin/StartEngine.sh debug	

_Load the hello world messages_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/Message_Definition_HelloWorld.json TENANTID tenant1

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/OutMessage_Definition_HelloWorld.json TENANTID tenant1

_Load the model_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties upload compile config $KAMANJA_HOME/config/Model_Config_HelloWorld.json

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add model java $KAMANJA_HOME/input/SampleApplications/metadata/model/HelloWorld.java DEPENDSON helloworldmodel TENANTID tenant1 

_Add bindings for system messages_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/SystemMsgs_Adapter_Binding.json


_Add the input adapter (CSV) binding_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "HelloWorldInput", "MessageName": "com.ligadata.kamanja.samples.messages.msg1", "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"alwaysQuoteFields": false, "fieldDelimiter": ","} }'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding key 'helloworldinput,com.ligadata.kamanja.samples.messages.msg1,com.ligadata.kamanja.serializer.csvserdeser'

_Add the output adapter (CSV) binding_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "TestOut_1", "MessageNames": ["com.ligadata.kamanja.samples.messages.outmsg1"], "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"alwaysQuoteFields": false, "fieldDelimiter": ","} }'

_Add the output adapter (JSON) binding_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "TestOut_1", "MessageNames": ["com.ligadata.kamanja.samples.messages.outmsg1"], "Serializer": " com.ligadata.kamanja.serializer.JsonSerDeser"}'

_Push data_

$TestBin/PushDataToKafka.sh "helloworldinput" 1 "$KAMANJA_HOME/input/SampleApplications/data/Input_Data_HelloWorld.csv.gz"
  
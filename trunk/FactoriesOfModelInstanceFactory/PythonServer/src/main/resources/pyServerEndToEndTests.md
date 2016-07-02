


**Script Setup**
export ipport=8998
export CLASSPATH=/tmp/drdigital/Kamanja-1.5.0_2.11/lib/system/ExtDependencyLibs_2.11-1.5.0.jar:/tmp/drdigital/Kamanja-1.5.0_2.11/lib/system/KamanjaInternalDeps_2.11-1.5.0.jar:/tmp/drdigital/Kamanja-1.5.0_2.11/lib/system/ExtDependencyLibs2_2.11-1.5.0.jar
export PY_METADATA=~/github1/dev/1.5.0.Test/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonServer/src/main/resources/metadata
export KAMANJAPYPATH=~/github1/dev/1.5.0.Test/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonServer/src/main/python
export DATA=~/github1/dev/1.5.0.Test/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonServer/src/main/resources/data
export KAMANJA_HOME=/tmp/drdigital/Kamanja-1.5.0_2.11
export KAMANJA_SRCDIR=~/github1/dev/1.5.0.Test/kamanja/trunk
export TestBin=$KAMANJA_SRCDIR/MetadataAPI/src/test/resources/bin
export MetadataDir=$KAMANJA_SRCDIR/MetadataAPI/src/test/resources/Metadata
export apiConfigProperties=$KAMANJA_HOME/config/MetadataAPIConfig.properties

**Use setPathsFor.scala to configure the templates for mdapi, engine and cluster  config.**
_This setPathsFor.scala will do the same thing that the SampleApplication/EasyInstall/SetPaths.sh script does for some of the fixed examples we have in the distribution.  This does it for arbitrary files... one at a time.  The script is somewhat useful for setting up testing configurations.  Install it if you like on your PATH.  It can be found at trunk/Utils/Script/setPathsFor.scala_

_Configure the cluster configuration json file with cassandra storage values (make location substitutions... assumes that the JAVA_HOME and SCALA_HOME env variables are set in your system).  NOTE: THE CURRENT ClusterConfig.json is overwritten_

	setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/ClusterConfig_Template.json --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName testdata --schemaLocation localhost > $KAMANJA_HOME/config/ClusterConfig.json

_Configure the metadata api config file to use.  In this case, the cassandra values are used (make location substitutions... assumes that the JAVA_HOME and SCALA_HOME env variables are set in your system).  NOTE: THE CURRENT MetadataAPIConfig.properties is overwritten_

	setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/Cassandra_MetadataAPIConfig_Template.properties --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName metadata --schemaLocation localhost > $KAMANJA_HOME/config/MetadataAPIConfig.properties

_Configure the engine config file to use.  In this case, the cassandra values are used. NOTE: THE CURRENT Engine1Config.properties is overwritten_

	setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/Engine1Config_Template.properties --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName metadata --schemaLocation localhost > $KAMANJA_HOME/config/Engine1Config.properties

**add cluster cfg**
	$KAMANJA_HOME/bin/kamanja $apiConfigProperties upload cluster config $KAMANJA_HOME/config/ClusterConfig.json

**Start the engine**

bin/StartEngine.sh 

bin/StartEngine.sh debug	

**add messages**
$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $PY_METADATA/message/arithmeticMsg.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $PY_METADATA/message/arithmeticMsg.json TENANTID tenant1

**add models**

$KAMANJA_HOME/bin/kamanja add model python $PY_METADATA/model/add.py MODELNAME AddTuple MESSAGENAME org.kamanja.arithmetic.arithmeticMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"a": "Int", "b": "Int"}' TENANTID tenant1

$KAMANJA_HOME/bin/kamanja add model python $PY_METADATA/model/add.py MODELNAME AddTuple MESSAGENAME org.kamanja.arithmetic.arithmeticOutMsg OUTMESSAGE <output message> MODELOPTIONS '{"a": "Int", "b": "Int"}'

$KAMANJA_HOME/bin/kamanja add model python $PY_METADATA/model/add.py MODELNAME AddTuple MESSAGENAME org.kamanja.arithmetic.arithmeticOutMsg OUTMESSAGE <output message> MODELOPTIONS '{"a": "Int", "b": "Int"}'

$KAMANJA_HOME/bin/kamanja add model python $PY_METADATA/model/add.py MODELNAME AddTuple MESSAGENAME org.kamanja.arithmetic.arithmeticOutMsg OUTMESSAGE <output message> MODELOPTIONS '{"a": "Int", "b": "Int"}'


**Add bindings for system messages**

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/SystemMsgs_Adapter_Binding.json


**Add the input adapter (CSV) binding**

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "TestIn_1", "MessageName": "org.kamanja.arithmetic.arithmeticMsg", "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"alwaysQuoteFields": false, "fieldDelimiter": ","} }'

$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding key 'TestIn_1,org.kamanja.arithmetic.arithmeticMsg,com.ligadata.kamanja.serializer.csvserdeser'

**Add the output adapter (CSV) binding**

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "TestOut_1", "MessageNames": ["org.kamanja.arithmetic.arithmeticOutMsg"], "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"alwaysQuoteFields": false, "fieldDelimiter": ","} }'

$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding key 'TestIn_1,org.kamanja.arithmetic.arithmeticOutMsg,com.ligadata.kamanja.serializer.csvserdeser'

**Add the output adapter (JSON) binding**

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "TestOut_1", "MessageNames": ["org.kamanja.arithmetic.arithmeticOutMsg"], "Serializer": " com.ligadata.kamanja.serializer.JsonSerDeser"}'

$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding key 'TestIn_1,org.kamanja.arithmetic.arithmeticOutMsg,com.ligadata.kamanja.serializer.csvserdeser'

**Push data**

$TestBin/PushDataToKafka.sh "helloworldinput" 0 "$DATA/arithmeticData.txt"
  


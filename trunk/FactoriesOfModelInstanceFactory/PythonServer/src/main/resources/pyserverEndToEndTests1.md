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

	setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/ClusterConfig_Template.json --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName testdata --schemaLocation localhost > $KAMANJA_HOME/config/ClusterConfig.json
	setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/Cassandra_MetadataAPIConfig_Template.properties --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName metadata --schemaLocation localhost > $KAMANJA_HOME/config/MetadataAPIConfig.properties
	setPathsFor.scala --installDir $KAMANJA_HOME --templateFile $MetadataDir/templates/Engine1Config_Template.properties --scalaHome $SCALA_HOME --javaHome $JAVA_HOME --storeType cassandra --schemaName metadata --schemaLocation localhost > $KAMANJA_HOME/config/Engine1Config.properties
	$KAMANJA_HOME/bin/kamanja $apiConfigProperties upload cluster config $KAMANJA_HOME/config/ClusterConfig.json

**Start the engine**

bin/StartEngine.sh 

bin/StartEngine.sh debug	

#**add messages**
$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $PY_METADATA/message/arithmeticMsg.json TENANTID tenant1
$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $PY_METADATA/message/arithmeticOutMsg.json TENANTID tenant1

#**add models**

$KAMANJA_HOME/bin/kamanja add model python $PY_METADATA/model/add.py MODELNAME AddTuple MESSAGENAME org.kamanja.arithmetic.arithmeticMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"InputTypeInfo": {"a": "Int", "b": "Int"}, "OutputTypeInfo": {"a": "Int", "b": "Int", "result": "Int"} }' TENANTID tenant1

$KAMANJA_HOME/bin/kamanja add model python $PY_METADATA/model/multiply.py MODELNAME MultiplyTuple MESSAGENAME org.kamanja.arithmetic.arithmeticOutMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"InputTypeInfo": {"a": "Int", "b": "Int"}, "OutputTypeInfo": {"a": "Int", "b": "Int", "result": "Int"} }' TENANTID tenant1

$KAMANJA_HOME/bin/kamanja add model python $PY_METADATA/model/subtract.py MODELNAME SubtractTuple MESSAGENAME org.kamanja.arithmetic.arithmeticOutMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"InputTypeInfo": {"a": "Int", "b": "Int"}, "OutputTypeInfo": {"a": "Int", "b": "Int", "result": "Int"} }' TENANTID tenant1

$KAMANJA_HOME/bin/kamanja add model python $PY_METADATA/model/divide.py MODELNAME DivideTuple MESSAGENAME org.kamanja.arithmetic.arithmeticOutMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"InputTypeInfo": {"a": "Int", "b": "Int"}, "OutputTypeInfo": {"a": "Int", "b": "Int", "result": "Int"} }' TENANTID tenant1

#**bindings**
$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/SystemMsgs_Adapter_Binding.json

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "TestIn_1", "MessageName": "org.kamanja.arithmetic.arithmeticMsg", "Serializer": "com.ligadata.kamanja.serializer.csvserdeser", "Options": {"alwaysQuoteFields": false, "fieldDelimiter": ","} }'

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMSTRING '{"AdapterName": "TestOut_1", "MessageNames": ["org.kamanja.arithmetic.arithmeticOutMsg"], "Serializer": "com.ligadata.kamanja.serializer.JsonSerDeser"}'


#**Push data**

$TestBin/PushDataToKafka.sh "testin_1" 0 "$DATA/arithmeticData.txt"
  
$KAMANJA_HOME/bin/kamanja debug add model python $PY_METADATA/model/add.py MODELNAME AddTuple MESSAGENAME org.kamanja.arithmetic.arithmeticMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"InputTypeInfo": {"a": "Int", "b": "Int"}, "OutputTypeInfo": {"a": "Int", "b": "Int", "result": "Int"} }' TENANTID tenant1

$KAMANJA_HOME/bin/kamanja debug add model python $PY_METADATA/model/multiply.py MODELNAME MultiplyTuple MESSAGENAME org.kamanja.arithmetic.arithmeticOutMsg OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg MODELOPTIONS '{"InputTypeInfo": {"a": "Int", "b": "Int"}, "OutputTypeInfo": {"a": "Int", "b": "Int", "result": "Int"} }' TENANTID tenant1

ERROR [pool-17-thread-1] - Failed to execute model:add.addtuple
com.ligadata.Exceptions.NotImplementedFunctionException: execute method is not implemented for model:add.addtuple
InputMessages (1) are:org.kamanja.arithmetic.arithmeticMsg and triggerdSetIndex:0

ERROR [pool-17-thread-1] - Failed to execute model:add.addtuple
com.ligadata.Exceptions.NotImplementedFunctionException: execute method is not implemented for model:add.addtuple
InputMessages (1) are:org.kamanja.arithmetic.arithmeticMsg and triggerdSetIndex:0
        at com.ligadata.KamanjaBase.ModelInstance.execute(ModelBase.scala:619) ~[KamanjaInternalDeps_2.11-1.5.0.jar:0.1-SNAPSHOT]





**Script Setup**
export ipport=8998
export CLASSPATH=/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/KamanjaInternalDeps_2.11-1.4.1.jar:/tmp/drdigital/Kamanja-1.4.1_2.11/lib/system/ExtDependencyLibs2_2.11-1.4.1.jar
export K_METADATA=~/github1/dev/1.5.0.Test/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonServer/src/main/resources/metadata
export KAMANJAPYPATH=~/github1/dev/1.5.0.Test/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonServer/src/main/python
export DATA=~/github1/dev/1.5.0.Test/kamanja/trunk/FactoriesOfModelInstanceFactory/PythonServer/src/main/resources/data


export KAMANJA_HOME=/tmp/drdigital/Kamanja-1.5.0_2.11
export KAMANJA_SRCDIR=~/github1/dev/1.5.0.Test/kamanja/trunk
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






port 9999_
SockClient.scala --cmd addModel --filePath $METADATA/model/add.py --modelName AddTuple --user kamanaja --modelOptions '{"a": "Int", "b": "Int"}'  --host localhost --port 9999 --pyPath $KAMANJAPYPATH


**add cluster cfg**

**add messages**

**add models**

kamanja add model python <some/path/to/model.py> MODELNAME <model class name> MESSAGENAME <input message> OUTMESSAGE <output message> MODELOPTIONS '{"a": "Int", "b": "Int"}'





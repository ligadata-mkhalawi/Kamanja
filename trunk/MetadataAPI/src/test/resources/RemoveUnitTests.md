**!/usr/bin/env bash**

**Setup**

	export KAMANJA_HOME=/tmp/drdigital/Kamanja-1.4.0_2.11
	export KAMANJA_SRCDIR=~/github/dev/1.4.0.Base/kamanja/trunk
	export MetadataDir=$KAMANJA_SRCDIR/MetadataAPI/src/test/resources/Metadata
	export apiConfigProperties=$apiConfigProperties


**1) Load the HellowWorldApp.sh content**


	$KAMANJA_HOME/bin/kamanja $apiConfigProperties upload cluster config $KAMANJA_HOME/config/ClusterConfig.json

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/Message_Definition_HelloWorld.json TENANTID tenant1

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/OutMessage_Definition_HelloWorld.json TENANTID tenant1

	# $KAMANJA_HOME/bin/kamanja $apiConfigProperties add model kpmml $KAMANJA_HOME/input/SampleApplications/metadata/model/KPMML_Model_HelloWorld.xml TENANTID tenant1

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties upload compile config $KAMANJA_HOME/config/Model_Config_HelloWorld.json

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add model java $KAMANJA_HOME/input/SampleApplications/metadata/model/HelloWorld.java DEPENDSON helloworldmodel TENANTID tenant1 

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/HelloWorld_Adapter_Binding.json

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/SystemMsgs_Adapter_Binding.json

**2) Remove things**

_adapter message bindings_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding key 'TestStatus_1,com.ligadata.KamanjaBase.KamanjaStatusEvent,com.ligadata.kamanja.serializer.csvserdeser'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding key 'testout_1,com.ligadata.kamanja.samples.messages.outmsg1,com.ligadata.kamanja.serializer.csvserdeser'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding key 'HelloWorldInput,com.ligadata.kamanja.samples.messages.msg1,com.ligadata.kamanja.serializer.csvserdeser'

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties list adaptermessagebindings 

_models_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove model com.ligadata.kamanja.samples.models.helloworldmodel.000000000000000001 TENANTID tenant1

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties get all models


_engine config_

**_NOTE_**

The remove config command uses the original input with the config to be removed specified in the json file.  This needs to be fixed to simply take the keys.  For example, 

	kamanja <api config> remove adapter key 'clusterId.adapterName'

instead of 

	kamanja <api config> remove engine config <some json file.json>

that has something like this in it:

	{
	  "Clusters": [
	    {
	      "ClusterId": "ligadata1",
	      "SystemCatalog": {
	        "StoreType": "hbase",
	        "SchemaName": "metadata",
	        "Location": "localhost"
	      },
	      "Adapters": [
	        {
	          "Name": "TestIn_1",
	          "TypeString": "Input",
	          "TenantId": "tenant1",
	          "ClassName": "com.ligadata.InputAdapters.KafkaSimpleConsumer$",
	          "JarName": "KamanjaInternalDeps_2.11-1.4.0.jar",
	          "DependencyJars": [
	            "ExtDependencyLibs_2.11-1.4.0.jar",
	            "ExtDependencyLibs2_2.11-1.4.0.jar"
	          ],
	          "AdapterSpecificCfg": {
	            "HostList": "localhost:9092",
	            "TopicName": "testin_1"
	          }
	        }
	      ]
	    }
	  ]
	}

this would remove the TestIn_1 adapter.

_messages_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove message com.ligadata.kamanja.samples.messages.outmsg1.000000000001000000 TENANTID tenant1

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove message com.ligadata.kamanja.samples.messages.msg1.000000000001000000 TENANTID tenant1

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties get message com.ligadata.kamanja.samples.messages.outmsg1.000000000001000000 TENANTID tenant1

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties get message com.ligadata.kamanja.samples.messages.msg1.000000000001000000 TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties get all messages

_cluster config_

This is broken principally in terms of the design.  You should be able to mention the name clusterId and that's it.  This what you do at the moment (don't know if it works)

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove engine config <some file with all of the config described in it.json>


**3) Load the MedicalApp.sh content**

$KAMANJA_HOME/bin/kamanja $apiConfigProperties upload cluster config $KAMANJA_HOME/config/ClusterConfig.json

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add container $KAMANJA_HOME/input/SampleApplications/metadata/container/CoughCodes_Medical.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add container $KAMANJA_HOME/input/SampleApplications/metadata/container/DyspnoeaCodes_Medical.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add container $KAMANJA_HOME/input/SampleApplications/metadata/container/EnvCodes_Medical.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add container $KAMANJA_HOME/input/SampleApplications/metadata/container/SmokeCodes_Medical.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add container $KAMANJA_HOME/input/SampleApplications/metadata/container/SputumCodes_Medical.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/COPDInputMessage.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/COPDOutputMessage.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/beneficiary_Medical.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/hl7_Medical.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/inpatientclaim_Medical.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $KAMANJA_HOME/input/SampleApplications/metadata/message/outpatientclaim_Medical.json TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties upload compile config $KAMANJA_HOME/config/COPDRiskAssessmentCompileCfg.json

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add model java $KAMANJA_HOME/input/SampleApplications/metadata/model/COPDRiskAssessment.java DEPENDSON copdriskassessment TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add model jtm $KAMANJA_HOME/input/SampleApplications/metadata/model/COPDDataIngest.jtm DEPENDSON COPDDataIngest TENANTID tenant1 

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/COPD_Adapter_Binding.json

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add adaptermessagebinding FROMFILE $KAMANJA_HOME/config/SystemMsgs_Adapter_Binding.json


**4) Remove things**

_containers_

$KAMANJA_HOME/bin/kamanja $apiConfigProperties get all containers

    "Result Data" : 
    com.ligadata.kamanja.samples.containers.envcodes.000000000001000000, 
    com.ligadata.kamanja.samples.containers.sputumcodes.000000000001000000, com.ligadata.kamanja.samples.containers.coughcodes.000000000001000000, system.messageinterface.000000000000000001
    system.envcontext.000000000000000001
    system.containerinterface.000000000000000001
    com.ligadata.kamanjabase.kamanjamodelevent.000000000000000001
	system.context.000000000000000001
	com.ligadata.kamanja.samples.containers.dyspnoeacodes.000000000001000000
	com.ligadata.kamanja.samples.containers.smokecodes.000000000001000000

$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove container com.ligadata.kamanja.samples.containers.smokecodes.000000000001000000 TENANTID tenant1

$KAMANJA_HOME/bin/kamanja debug $apiConfigProperties remove container com.ligadata.kamanja.samples.containers.dyspnoeacodes.000000000001000000 TENANTID tenant1

_messages_


$KAMANJA_HOME/bin/kamanja $apiConfigProperties get all messages

$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove message com.ligadata.kamanja.samples.messages.inpatientclaim.000000000001000000 TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove message com.ligadata.kamanja.samples.messages.outpatientclaim.000000000001000000 TENANTID tenant1

$KAMANJA_HOME/bin/kamanja debug $apiConfigProperties remove message com.ligadata.kamanja.samples.messages.hl7.000000000001000000 TENANTID tenant1

_get message_

com.ligadata.kamanjabase.kamanjastatisticsevent.000000000000000001
com.ligadata.kamanjabase.kamanjamessageevent.000000000000000001
com.ligadata.kamanjabase.kamanjaexceptionevent.000000000000000001
com.ligadata.kamanja.samples.messages.beneficiary.000000000001000000
com.ligadata.kamanjabase.kamanjaexecutionfailureevent.000000000000000001
com.ligadata.kamanjabase.kamanjastatusevent.000000000000000001
com.ligadata.kamanja.samples.messages.copdinputmessage.000000000001000000
com.ligadata.kamanja.samples.messages.copdoutputmessage.000000000001000000
com.ligadata.kamanja.samples.models.copdriskassessment_outputmsg.000000000000000001

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties get message com.ligadata.kamanjabase.kamanjastatusevent.000000000000000001 TENANTID tenant1

	(produces invalid json)

_jtm_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties get all models

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove model com.ligadata.kamanja.samples.models.copddataingest.000000000000000001 TENANTID tenant1


_adapter message binding_

	$KAMANJA_HOME/bin/kamanja $apiConfigProperties list adaptermessagebindings 

    "testout_1,com.ligadata.kamanja.samples.messages.copdoutputmessage,com.ligadata.kamanja.serializer.csvserdeser" 
    "teststatus_1,com.ligadata.kamanjabase.kamanjastatusevent,com.ligadata.kamanja.serializer.csvserdeser"
    "medicalinput,com.ligadata.kamanja.samples.messages.copdinputmessage,com.ligadata.kamanja.serializer.csvserdeser"

 
$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove adaptermessagebinding key 'medicalinput,com.ligadata.kamanja.samples.messages.copdinputmessage,com.ligadata.kamanja.serializer.csvserdeser'


_type_

$KAMANJA_HOME/bin/kamanja $apiConfigProperties get type system.float.1000000


_function_

$KAMANJA_HOME/bin/kamanja $apiConfigProperties get function pmml.AnyBetween.1

_pmml_

$KAMANJA_HOME/bin/kamanja $apiConfigProperties get all messages

$KAMANJA_HOME/bin/kamanja $apiConfigProperties remove message system.irismsg.000000000001000001 TENANTID tenant1

$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $MetadataDir/message/IrisMsg.json TENANTID tenant1
$KAMANJA_HOME/bin/kamanja $apiConfigProperties add message $MetadataDir/message/IrisMsg1.json TENANTID tenant1


$KAMANJA_HOME/bin/kamanja $apiConfigProperties add model pmml  MODELNAME com.botanical.jpmml.IrisDecisionTree MODELVERSION 000000.000001.000001 MESSAGENAME System.IrisMsg OUTMESSAGE System.IrisMsg1 TENANTID tenant1 $MetadataDir/model/Rattle/DecisionTreeIris.pmml 





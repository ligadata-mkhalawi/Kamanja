
.. _example-commands-api:

Example API commands
====================

This section shows
some example cURL REST API commands to see some cURL commands.

add message

::

  curl -X POST --data-binary  @"/tmp/myMessage.json"  -k  https://localhost:8081/api/message
     --header "TENANTID:tenant1"

update message

::

  curl -X PUT --data-binary @"COPDInputMessage.json"
     k https://localhost:8081/api/message --header "TENANTID:tenant1"

get message

::

  curl -k https://localhost:8081/api/message/
     com.ligadata.kamanja.samples.messages.copdinputmessage.000000000006000000
     --header "TENANTID:tenant1"

get all messages

::

  curl -k https://localhost:8081/api/keys/message --header "TENANTID:tenant1"

remove message

::

  curl -X DELETE -k https://localhost:8081/api/message/
     com.ligadata.kamanja.samples.messages.copdinputmessage.000000000007000000

add java model

::

  curl -X POST --data-binary  @"/myJavaModel.java"  -k  https://localhost:8081/api/model/java
     --header "modelconfig:MyModelConfig" --header "TENANTID:tenant1"

add scala model

::

  curl -X POST --data-binary  @"/tmp/myScalaModel.scala"
     -k  https://localhost:8081/api/model/scala
     --header "modelconfig:MyModelConfig" --header "TENANTID:tenant1"

add pmml model

::

  curl -X POST --data-binary  @"/tmp/myModel.pmml"
     -k  https://localhost:8081/api/model/pmml
     --header "modelconfig:system.DecisionTreeIris,0.1.0,system.irismsg"
     -header "TENANTID:tenant1"

  modelconfig : (full model name, model version, full message name)

  modelconfig : (full model name, model version)

add kpmml model

::

  curl -X POST --data-binary  @"/tmp/myKPMML.xml"  -k  https://localhost:8081/api/model/kpmml
     --header "TENANTID:tenant1"

get model

::

  curl -k https://localhost:8081/api/model/com.ligadata.samples.models.
     helloworldmodel.000000000000000001

get all models

::

  curl -k https://localhost:8081/api/keys/model

delete model

::

  curl -X DELETE -k https://localhost:8081/api/model/
     myModel.000000000001000000

activate model

::

  curl -X PUT -k https://localhost:8081/api/deactivate/model/system.
     helloworld.000000000001000000

deactivate model

::

  curl -X PUT -k https://localhost:8081/api/deactivate/model/system.
     helloworld.000000000001000000

upload model config

::

  curl -X PUT --data-binary  @"/tmp/MyModelConfig.json"
     -k  https://localhost:8081/api/UploadModelConfig  --header "TENANTID:tenant1"

update java model

::

  curl -X PUT --data-binary  @"/myJavaModel.java"
     -k  https://localhost:8081/api/model/java
     --header "modelconfig:MyModelConfig" --header "TENANTID:tenant1"

update scala model

::

  curl -X PUT --data-binary @"HelloWorld.scala"
     k https://localhost:8081/api/model/scala
     -header "modelconfig:HelloWorldModel" --header "TENANTID:tenant1"

update pmml model

::

  curl -X PUT --data-binary  @"/tmp/DecisionTreeIris.pmml"
     -k  https://localhost:8081/api/model/pmml
     --header "modelconfig:system.DecisionTreeIris,0.2.0"
     --header "TENANTID:tenant1"

update kpmml model

::

  curl -X PUT --data-binary @"KPPML_Model_HelloWorld.xml"
     -k https://localhost:8081/api/model/kpmml --header "TENANTID:tenant1"

add container

::

  curl -X POST --data-binary  @"/tmp/myContainer.json"
     -k  https://localhost:8081/api/container  --header "TENANTID:tenant1"

update container

::

  curl -X PUT --data-binary @"SputumCodes_Medical.json"
     -k https://localhost:8081/api/container --header "TENANTID:tenant1"

get container

::

  curl -k https://localhost:8081/api/container/com.ligadata.kamanja.samples.containers.
     sputumcodes.000000000001000000

get all containers

::

  curl -k https://localhost:8081/api/keys/container

delete container

::

  curl -X DELETE -k https://localhost:8081/api/container/myContainer.000000000001000000

add function

::

  curl -X POST --data-binary @"udfFcns.json" -k https://localhost:8081/api/function
     --header "TENANTID:tenant1"

update function

::

  curl -X PUT --data-binary @"coreUdfFcnDef_Medical.json" -k https://localhost:8081/api/function --header "TENANTID:tenant1"

get function

::

  curl -k https://localhost:8081/api/function/pmml.ceil.000000000000000001

get all functions

::

  curl -k https://localhost:8081/api/keys/function

remove function

::

  curl -X DELETE -k https://localhost:8081/api/function/pmml.min.000000000000000001

upload cluster config

::

  curl -X PUT --data-binary @"ClusterConfig.json" -k https://localhost:8081/api/UploadConfig

upload jar

::

  curl -X PUT --data-binary @A"../lib/system/kvinit_2.11-1.5.0.jar"
     -k https://localhost:8081/api/uploadjars?name=kvinit --header "TENANTID:tenant1"

get all bindings

::

  curl -k https://localhost:8081/api/keys/adaptermessagebinding

get all bindings per adapter name

::

  curl -k https://localhost:8081/api/adaptermessagebinding/adapter/testout_1

get all bindings per message name

::

  curl -k https://localhost:8081/api/adaptermessagebinding/message/
     com.ligadata.kamanja.samples.messages.copdinputmessage

get all bindings per serializer name

::

  curl -k https://localhost:8081/api/adaptermessagebinding/serializer/com.ligadata.kamanja.serializer.csvserdeser

add a bunch of bindings from a file (JSON only)

::

  curl -X POST --data-binary "@/Users/user_id/kamanja13/Kamanja-1.4.0_2.11/
     config/COPD_Adapter_Binding.json"
     -k https://localhost:8081/api/adaptermessagebinding

delete adapter

::

  curl -X DELETE -k https://localhost:8081/api/adaptermessagebinding/
     teststatus_1,com.ligadata.kamanjabase.kamanjastatusevent,
     com.ligadata.kamanja.serializer.csvserdeser

get whatever object in the metadata has SCHEMAID 2000001

::

  curl -k https://localhost:8081/api/typebyschemaid/2000001

get whatever object in the metadata has ELEMENTID 2000002

::

  curl -k https://localhost:8081/api/typebyelementid/2000002

Note: ELEMENTID applies to messages, containers and models,
but SCHEMAID only applies to messages and containers.



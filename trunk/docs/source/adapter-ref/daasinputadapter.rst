
.. _daas-input-adapter-ref: 

Daas Input Adapter
==================

The Daas Input Adapter enables you to ingest data
using the REST API.


Daas Input Adapter structure

::

  {
    "Name": "daasinputadapter",
    "TypeString": "Input",
    "TenantId": "tenant1",
    "ClassName": "com.ligadata.kafkaInputOutputAdapters_v9.KamanjaKafkaConsumer$",
    "JarName": "kamanjakafkaadapters_0_9_2.11-1.5.3.jar",
    "DependencyJars": [
      "kafka-clients-0.9.0.1.jar"
    ],
    "AdapterSpecificCfg": {
      "HostList": "localhost:9092",
      "TopicName": "inputtopic"
    }
  }

MetadataAPIConfig.properties properties
---------------------------------------

Add these lines to the :ref:`metadataapiconfig-config-ref` file
to use the DAAS Input Adapter:

::

  DaasConfig={
    "requestTopic": {"kafkaHost": "localhost:9092",
    "topicName": "testin_1"},
    "responseTopic": {"kafkaHost": "localhost:9092",
    "topicName": "testout_1", "numberOfThreads": "1"
  },
  "inputTopic": {"kafkaHost": "localhost:9092", "topicName": "inputtopic"}}


Parameters
----------


Usage
-----

#. Add the adapter definition shown in the
   "DAAS Input Adapter Structure" section above
   to the "Adapters" section in the :ref:`clusterconfig-config-ref` file
   and add the full path (kamanjamanager_2.11-1.5.3.jar)
   to the **classPath**.
   Then upload the *ClusterConfig.json* file.

#. Add the Daas input message to Kamanja
   using the :ref:`kamanja-command-ref` command:

   ::

     kamanja add message DaasInputMessage.json tenantid tenant1

#. Add Daas adapter binding using this command:

   ::

     kamanja add adaptermessagebinding FROMFILE DaasAdapterBinding.json

#. Add model config using this command:

   ::

     kamanja upload compile config DaasModelConfig.json

#. Add scala model using this command:

   ::

     kamanja add model scala DaasModel.scala DEPENDSON daasmodelconfig tenantid tenant1

#. Add the lines shown in the
   "MetadataAPIConfig.properties properties" section above
   to the :ref:`metadataapiconfig-config-ref` file.

#. Now start the web service:

   ::

     kamanja start webservice

#. Insert a record using **curl**:

   ::

     curl -XPOST -d '1,world,1'
          -k 'https://localhost:8081/data/com.ligadata.kamanja.samples.messages.
          msg1?format=delimited&valueDelimiter=~&alwaysQuoteFields=false&fieldDelimiter=,'

     1,world,1                                  = the data you want to send
     com.ligadata.kamanja.samples.messages.msg1 = message name
     format=delimited                           = parameter (json|delimited)
                                                  default value is delimited
     valueDelimiter=~                           = parameter default value is ~
     alwaysQuoteFields=false                    = parameter (true|false)
                                                  default value is false
     fieldDelimiter=,                           = parameter default value is
 

#. Check input topic using this command:

   ::

     $KAFKA_HOME/bin/kafka-console-consumer.sh --zookeeper localhost:2181  \
         --topic inputtopic --from-beginning

#. You should see this result:

   ::

     {"MsgType":"com.ligadata.kamanja.samples.messages.msg1",
         "FormatOption":"{\"formatType\": \"com.ligadata.kamanja.serializer.csvserdeser\",
         \"alwaysQuoteFields\": \"false\", \"fieldDelimiter\": \",\",
         \"valueDelimiter\":\"~\"} ","PayLoad":"1,world,1"}

#. Now start the Kamanja engine:

   ::

     kamanja start -v

#. You should see the alert raised in the "hello world" output topic.


Differences between versions
----------------------------

This adapter is supported in Kamanja 1.6.3 and later releases.

See also
--------

- :ref:`adapter-binding-config-ref`
- :ref:`adapter-def-config-ref`


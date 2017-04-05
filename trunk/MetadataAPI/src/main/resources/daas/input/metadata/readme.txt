use rest api to input data step by step:

1- in clusterconfig_kafka_v9.json add this part then upload cluster config:

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

and add to classPath the full path for this jar kamanjamanager_2.11-1.5.3.jar

2- add Daas input message to kamanja using this command:

kamanja add message DaasInputMessage.json tenantid tenant1

3- add Daas adapter binding using this command:

kamanja add adaptermessagebinding FROMFILE DaasAdapterBinding.json

4- add model confing using this command :

kamanja upload compile config DaasModelConfig.json

5- add scala model using this command :

kamanja add model scala DaasModel.scala DEPENDSON daasmodelconfig tenantid tenant1

6- in metadataAPIConfing.properties add this part :

DaasConfig={"requestTopic": {"kafkaHost": "localhost:9092", "topicName": "testin_1"}, "responseTopic": {"kafkaHost": "localhost:9092", "topicName": "testout_1", "numberOfThreads": "1"}, "inputTopic": {"kafkaHost": "localhost:9092", "topicName": "inputtopic"}}

7- now start web service :

kamanja start webservice

8- insert record using curl :

curl -XPOST -d '1,world,1' -k 'https://localhost:8081/data/com.ligadata.kamanja.samples.messages.msg1?format=delimited&valueDelimiter=~&alwaysQuoteFields=false&fieldDelimiter=,'

1,world,1                                  = the data you want to send
com.ligadata.kamanja.samples.messages.msg1 = message name
format=delimited                           = parameter (json|delimited) default value is delimited
valueDelimiter=~                           = parameter default value is ~
alwaysQuoteFields=false                    = parameter (true|false) default value is false
fieldDelimiter=,                           = parameter default value is ,

9- check inputtpoic topic using this command:

$KAFKA_HOME/bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic inputtopic --from-beginning

10- you shoud see this result:

{"MsgType":"com.ligadata.kamanja.samples.messages.msg1","FormatOption":"{\"formatType\": \"com.ligadata.kamanja.serializer.csvserdeser\", \"alwaysQuoteFields\": \"false\", \"fieldDelimiter\": \",\", \"valueDelimiter\":\"~\"} ","PayLoad":"1,world,1"}

11- now start kamanja engine:

kamanja start -v

12- now you should see the alert raised in hello world output topic.
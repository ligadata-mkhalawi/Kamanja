bin/kamanja get all models
 
bin/kamanja remove model com.ligadata.samples.models.helloworldjythonmodel.000000000000000008
  
bin/kamanja add model scala `pwd`/input/SampleApplications/metadata/model/HelloWorldJython2.scala DEPENDSON helloworldjythonmodel TENANTID tenant1 

input/SampleApplications/bin/PushSampleDataToKafka_HelloWorld.sh

vi config/log4j2.xml 

bin/StartEngine.sh debug

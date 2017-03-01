
.. simp_scala_adapter-msg-binding:

Create the data to present to Kamanja
=====================================

Kamanja leverages the capabilities of Kafka to read and write messages.
So, now it is time to create a single message and push it into Kafka.
Place the following (very simple) line into a file:

::

  NumberMessageData.csv

  27,54

That’s right, place two numbers, 27 and 54, separated by a comma,
into a text file and place it into the SimpleMathDAG directory.
Because the adapter message binding indicates
NumberMessage should be consumed from that adapter,
and NumberMessage is defined to have two integers,
the numbers 27 and 54 are taken in the same order
as the fields defined and are used to create a NumberMessage
for Kamanja to use and pass into the first model, DAGAddition.


Presenting NumberMessageData to Kamanja
---------------------------------------

To push the message into Kafka,
simply navigate to the Kafka installation directory:

::

  cd <KamanjaInstallationDirectory>

  java -cp <KamanjaInstallationDirectory>/lib/system/ExtDependencyLibs2_2.11-1.4.0.jar:<KamanjaInstallationDirectory>/lib/system/ExtDependencyLibs_2.11-1.4.0.jar:<KamanjaInstallationDirectory>/lib/system/KamanjaInternalDeps_2.11-1.4.0.jar:<KamanjaInstallationDirectory>/lib/system/simplekafkaproducer_2.11-1.4.0.jar \ 
     com.ligadata.tools.SimpleKafkaProducer –gz false –topics “testin_1” \ 
     –threads 1 –topicpartitions 1 –brokerlist “localhost:9092” \ 
     –files /path/to/SimpleMathDAG/NumberMessageData.csv \ 
     –partitionkeyidxs “1” –format CSV

Now that the data has been pushed into Kafka,
Kamanja picks up the data from Kafka, and treats it as a NumberMessage.
Kamanja takes NumberMessage, passes it in to DAGAddition
and all subsequent messages are generated and passed on down the line.
To make sure it has all worked, look at Kafka’s output queue,
where the result of the work of the ensemble can be seen.
Run the following commands:

::

  cd <KamanjaInstallationDirectory>

  bash bin/kamanja watch output queue


Expected Output

::

  {
      "@@SchemaId": 2000005,
          "integer1": 27,
          "integer2": 54,
          "integer3": 81,
      "integer4": 118098,
      "double": 5.226878953219224E-8
  }

@@SchemaId corresponds to the unique ID of the message,
in this case, the output, DividedMessage.
See that the original 27 and 54 have been preserved –
integer3 is the sum of 1 and 2 integers,
integer4 is the product of 1, 2 and 3 integers,
and double is the dividend of 1, 2, 3 and 4 integers.



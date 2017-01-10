
<<<<<<< HEAD
.. _adapters-input-guide:

Creating custom input adapters
==============================
=======
.. _adapters-input:

Input Adapters
==============
>>>>>>> b17dbbe01a24d89797d5e4e9c25f4497a200559b

An input adapter is a way of getting streams of data/events
into the Kamanja server.
Examples are Kafka messages and files.

This section describes how to write custom input adapters,
referred to here as consumers, that Kamanja can use to process messages.
Kafka adapters are provided with the codebase,
and therefore, most references in the following sections
are made to Apache Kafka,
including concepts such as partitions and offsets.
However, the origin of messages themselves
is not limited to any specific source,
as long as the trait/interface described here is implemented correctly.

In order to understand input adapters,
be familiar with concepts such as Kafka topics, partitions, and offsets.
Read http://kafka.apache.org/documentation.html carefully.

For example, the two methods,
GetAllPartitionBeginValues and GetAllPartitionEndValues,
make reference to Kafka partitions/offsets,
but if simple files are being used,
provide the values required by the specific adapter implementation.
For instance, for simple files,
it could be the FileName/Location and the byte offsets within the file.

Implementing Custom Consumers
-----------------------------

To implement a custom consumer for Kamanja, follow these rules.

Implementing a SimpleConsumer Wrapper
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Custom input adapters (consumers)
should be able to process messages
via their offsets in their respective data sources.
For example, if the Kamanja engine asks for a message at offset X,
then that message is returned back to the engine.

Custom adapters should implement the wrapper
for a Kafka SimpleConsumer as described by
the Kafka documentation found here: http://kafka.apache.org.
The reason for this is that Kamanja relies on the Kafka adapters
to be able to handle specific Kafka partition offsets.

An example of a basic Kafka SimpleConsumer Java implementation can be found
at `Kafka SimpleConsumer
<https://cwiki.apache.org/confluence/display/KAFKA/0.8.0+SimpleConsumer+Example>`_


Implementing the LigaData Input Adapter
---------------------------------------

To be able to use a custom adapter with the Kamanja engine,
it should have the following Scala trait:

::

  trait com.ligadata.KamanjaBase.InputAdapter

This enforces the presence of
the following required methods in the implementation class.

Method Signatures
~~~~~~~~~~~~~~~~~

The method signatures are as follows.

::

  // Shutdown the adapter now.

  def Shutdown: Unit

  // Finish processing current work and stop.

  def StopProcessing: Unit

  // Start reading the queue with the following parameters.

  def StartProcessing(
    maxParts: Int,
    partitionInfo: Array[
    (PartitionUniqueRecordKey,
    PartitionUniqueRecordValue,
    Long,(PartitionUniqueRecordValue, Int, Int))
    ],
    ignoreFirstMsg: Boolean): Unit

  // Gets an array of PartitionUniqueRecordKey, each describing a
  //partition of a Kafka topic being read.

  def GetAllPartitionUniqueRecordKey: Array[PartitionUniqueRecordKey]

  // Convert the KEY in a string format into the KafkaPartitionUniqueRecordKey

  def DeserializeKey(k: String): PartitionUniqueRecordValue

  // Convert the Value in a string format into the

  // KafkaPartitionUniqueRecordValue

  def DeserializeValue(v: String): PartitionUniqueRecordValue
  def getAllPartitionBeginValues: Array[
    (PartitionUniqueRecordKey,  PartitionUniqueRecordValue)]
  def getAllPartitionEndValues: Array[
    (PartitionUniqueRecordKey,  PartitionUniqueRecordValue)]

Method Definitions
~~~~~~~~~~~~~~~~~~

Here are the definitions of each method signature
in the com.ligadata.KamanjaBase.InputAdapter trait.

- **Shutdown/StopProcessing** – stops the execution of all threads
  that are involved in reading from a Kafka topic.
- **GetAllPartitionUniqueRecordKey** – called by the Kamanja engine
  on an instance of an InputAdapter.
  The output of this method is an array
  of KafkaPartitionUniqueRecordKey objects. Each object has a topic name.
  Each object also has a PartitionId that the message source has stored
  for this given topic.
  This information is used to call
  the GetAllPartitionBeginValues/GetAllPartitionEndValues methods
  to find the offsets within a message source partition
  to determine where to start processing.
- **getAllPartitionBeginValues/getAllPartitionEndValues** –
  return an array of KafkaPartitionUniqueRecordKey,
  KafkaPartitionUniqueRecordValue pairs.
  The KafkaPartitionUniqueRecordKey has information
  about a specific partition ID,
  while the KafkaPartitionUniqueRecordValue has information
  about the offset for the partition in question.
  After the Kamanja engine gets this array,
  it issues the StartProcessing method.
- **StartProcessing** – starts processing the specified partitions.

<<<<<<< HEAD
  - **maxParts** – maximum partitions that the Kamanja engine wants to monitor.
  - **partitionInfo** – information about the sources of the messages,
=======
  - ** maxParts** – maximum partitions that the Kamanja engine wants to monitor.
  - ** partitionInfo** – information about the sources of the messages,
>>>>>>> b17dbbe01a24d89797d5e4e9c25f4497a200559b
    with each array element representing a message source,
    for example a Kafka partition or a file.

    - The first element in the structure
      represents the partition identifier.
      For Kafka, this is a partition ID
      as represented by the PartitionUniqueRecordKey object.
    - The second element in the structure
      represents the position in the source at which reading starts.
      For Kafka, this is the partition offset,
      as represented by the PartitionUniqueRecordValue object.
    - The third element in the structure represents
      the beginning transaction ID (Long).
      Increment this value by one for each message processed
      (after sending that message to the Kamanja engine).
    - The fourth element in the structure is a triplet
      (PartitionUniqueRecordValue, Int, Int).
      This is used by the Kamanja server to ensure
      only-once processing in case of a failure.
      The first element in the triplet is a marker,
      which tells the adapter that for any message
      that is read from the source
      with the offset lower or equal to this marker,
      pass back the second and third parameters to the server.
      Otherwise, they pass back zeroes.
      In the following example,
      these values are processingXformMsg and totalXformMsg.
            

      ::

        (PartitionUniqueRecordKey,
                PartitionUniqueRecordValue,
                Long,(PartitionUniqueRecordValue, Int, Int))

- **ignoreFirst** – if the value is TRUE,
  then the adapter should not call back the Kamanja engine
  with the first message that it retrieves from each of the partition.



The custom adapter should have the following constructor signature.

::

  (inputConfig: com.ligadata.KamanjaBase.AdapterConfiguration,
  output: Array[com.ligadata.KamanjaBase.OutputAdapter],
  envCtxt: com.ligadata.KamanjaBase.EnvContext,
  mkExecCtxt: com.ligadata.KamanjaBase.MakeExecContext,
  cntrAdapter: com.ligadata.KamanjaBase.CountersAdapter)

- **inputConfig** – used to create and maintain the connection
  to a specific message broker for a specific topic.
  The custom adapter must create a connection
  to a message source/topic using this info.
- **output** – an array of OutputAdapters –'
  This array of adapters emits the output,
  passing it from input adapter to engine,
  which evaluates the input data
  and sends the output to these output adapters.
- **envCtxt** – the input adapter passes the environment context,
  envCtxt, to the engine as one of the arguments in MakeExecContext,
  and it is eventually used by the engine.
- **MakeExecContext** – used to create an execution context in the adapter,
  which is used to call back to the Kamanja engine
  as messages are processed.
- **cntrAdapter** – used by the adapter to keep track of relevant statistics.


Calling Back to the Server from a Custom Adapter
------------------------------------------------

To call back to the server, create an object by calling
the CreateExecContext method on
the com.ligadata.KamanjaBase.MakeExecContext passed as
the fourth parameter in the constructor.

::

   execThread = mkExecCtxt.CreateExecContext(input, partitionId, output, envCtxt)

Then, call the execute method with these parameters:

::


   execThread.execute(
     transactionId - Described above.
     message - UTF8 string
     format - CSV/JSON etc
     uniqueKey - This is the PartitionUniqueRecordKey representation of the partition: (partitionID, topic name).
     uniqueVal - Offset of the message within the source.
     readTmNs - System.nanoTime when the message was retrieved for the source.
     readTmMs - System.currentTimeMillis when the message was retrieved for the source
     dontSendOutputToOutputAdap - A boolean flag: TRUE if the 1st Element of the (PartitionUniqueRecordValue, Int, Int) structure is equal to or greater than the offset of this message.
     processingXformMsg - Described above
     totalXformMsg - Described above.
   )

Smart File Input Adapter
------------------------

Another type of input adapter is the smart file input adapter.


File Data Consumer
------------------

See :ref:`filedataconsumer-command-ref`



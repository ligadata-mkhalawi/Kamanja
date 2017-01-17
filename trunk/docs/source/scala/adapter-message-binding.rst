
.. simp_scala_adapter-msg-binding:

Write adapter message bindings
==============================

Add something referred to as an adapter message binding.
Adapter message bindings are required to tell the input adapter
(see more about adapters in the adapters section)
what message type the messages are treated as when read from the input adapter.

When a message is read by Kamanja, it treats all messages,
regardless of type, as the message defined in the adapter message binding.
If multiple message types are required to be read from the same input adapter,
see documentation about JSON Transformation Models (JTMs).

Adapter message bindings are also required
to indicate what message types should be sent to the output adapter.
Unlike the input adapter, this can be as many messages as preferred.

There is only have one message type that is read in from the input adapter,
NumberMessage, so there is no need to complicate the simple ensemble with a JTM.
In later walkthroughs, take a look at JTMs
and how they interact with messages and adapters.
For now, take a look at the adapter message binding:

Here are the contents of DAGMathAdapterBindings.json

::

  DAGMathAdapterBindings.json
  [{
  "AdapterName": "testin_1",
  "MessageName": "com.ligadata.test.dag.NumberMessage",
  "Serializer": "com.ligadata.kamanja.serializer.csvserdeser",
  "Options": {
  "alwaysQuoteFields": false,
  "fieldDelimiter": ","
  }
  }, {
  "AdapterName": "testout_1",
  "MessageNames": [
  "com.ligadata.test.dag.DividedMessage"
  ],
  "Serializer": "com.ligadata.kamanja.serializer.jsonserdeser",
  "Options": {}
  }]
	

The above shows an adapter message binding
for both the input adapter and the output adapter.
Here are the elements of the adapters.

- AdapterName is the name of the adapter that is defined in
  Kamanja’s cluster configuration
  (find an example in /config/ClusterConfig.json).
  For more info about configuration, click the message bindings section.
  This is essentially binding some message(s) to that adapter.

- MessageName is the full namespace and name of the message
  to bind to the aforementioned AdapterName.
  For input adapters (as defined in ClusterConfig.json),
  this means Kamanja treats every message, regardless of type,
  as what is listed in MessageName.
  In this case, make it NumberMessage,
  because that is the only message that is presented to Kamanja.

- Serializer defines the format that the message is received in or produced as.
  csvserdeser, as shown in the input adapter message binding,
  indicates Kamanja should expect all messages from the testin_1 adapter
  to be in a CSV – or some form of delimited – data.
  jsonserdeser, as shown in the testout_1 adapter,
  indicates that messages should be in JSON format.
  In this case, it means the contents of the DividedMessage message type
  is outputted as JSON into the testout_1 adapter.

- Options are simply additional options
  applied to any particular adapter’s Serializer.
  For the testin_1 adapter,
  the quotes are not accounted for in the field
  (and no quotes written in the case of an output adapter),
  and the fieldDelimiter tells Kamanja what delimiter to expect,
  in this case, the standard comma.

Adding Adapter Message Bindings
-------------------------------

Hopefully, there is a decent understanding of the adapter message bindings now.
For more information, click the message bindings section.
Now, add the final piece of metadata with the following command:

::

  cd <KamanjaInstallDirectory>

  bash bin/kamanja add adaptermessagebinding \ 
     FROMFILE /path/to/SimpleMathDAG/DAGMathAdapterBindings.json



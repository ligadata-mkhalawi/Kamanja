
.. _messages-term:

Messages
--------

Messages contain the data that flows into and out of the
:sh:`models<model-term>	in a pipeline that executes on Kamanja.
Three types of model are supported:

- input messages -- data flowing into a model
- output messages -- data flowing out of a model;
  the output message of one model can be the input message of another model.
- storage messages -- 

The message (message data) can be formatted as
:ref:`CSV<csv-term>`, :ref:`JSON<json-ref>`,
:ref:`XML<xml-term>`, or :ref:`KV<kv-term>`.
It is processed as a :ref:`Kafka<kafka-term>` message.

The Kamanja engine processes messages as follows:

- input messages:

- output messages: the Kamanja engine presents an output message definition
  to the output :ref:`adapter<adapter-term>`
  which can be a file adapter, Kafka queue, or MQ.
  The output message definition is defined with fields
  such as name, namespace, version, description, queue,
  partition key, and output format. 

- storage messages:

A :ref:`message definition<message-def-config-ref>` is a JSON file
that specifies the content and format of the information.

Use the :ref:`kamanja<kamanja-command-ref>` command
to add, remove, update, etc. the message definition
to the metadata for the cluster.

See also:

- :ref:`message definition<message-def-config-ref>` gives details
  about the format of the JSON file that defines a message.

- :ref:`kamanja<kamanja-command-ref>` reference page
  gives details about the commands used to manage message definitions. 

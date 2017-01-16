
.. _dag-term:

DAG
---

Kamanja implements a DAG (execution-directed acrycic graph)
to schedule work in the Kamanja :ref:`engine<engine-term>`.
This is a very efficient model
that reduces the time it takes for a packet of data
to get from one point to another.


Here is a description of a simple DAG.
The models change to take/consume messages and produce messages.
Here are two models.
Model one reads a message from some topic and generates some message.
Model two depends on this generated message.
It does not depend on the first message.
In the metadata registration,
the first model depends on the input message
and the output is the generated message.
Model two consumes the output of model one
and produces a different message,
which binds to some output queue.
Model two is now registered. This is a simple DAG.
The DAG is that model one has to be processed before model two
and model two depends on model one output.

The engine orchestrates the message flow based on these definitions.
It orchestrates which model executes first and based on the output,
invokes the appropriate models.

Consider the following diagram that consists of four models
(m1, m2, m3, and m4) that consume/produce messages.

.. image:: /_images/dag-pipeline.png


- Model m1 consumes message A that has been received from input adapter ia1,
  producing message B.
- Model m2 consumes message B producing message C.
- Message C is consumed by model m4 that produces message E,
  which is dispatched to the output adapter oa1.

- Message B is also consumed by model m3 that produces message D.
- Message D is dispatched to output adapter oa2 and oa3.

Note that the association of model-produced messages
that are not solely consumed by other models
are associated with adapters via the adapter message binding
(see :ref:`message bindings<message-bindings-term>`
for more information about them),
declared for each adapter/message/serialization method combination.

In the case of message D, the message is sent to two different adapters.
The :ref:`serialization<serial-deserial-term>` method
chosen for each adapter can be different.
Perhaps one is sent in JSON-serialized form to oa2
whereas oa3 expects a CSV-type serialized encoding.
For more about message serialization, see serialization.

Overall, the relationships of models and messages are codified in a DAG
that the engine uses to execute the models at the appropriate moment
(for example, message B has been produced for models m2 and m3 to consume).
Notice that the multi-core and multi-CPU commodity hardware
allows the model execution to be done in parallel,
as is evident with models m2 and m3.
Overall, the DAGs maintained by the Kamanja engine capture
the flow from the input adapters through the models to the consuming adapters.



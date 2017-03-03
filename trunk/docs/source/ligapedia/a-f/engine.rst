
.. _engine-term:

Engine
------

The Kamanja engine handles continuous decisioning.
It manages the receipt of event data
from some specified source or possibly sources,
constructs messages from this input,
and presents these messages to the models
in its execution working set that express an interest in such messages.
Each model interprets the incoming message,
possibly juxtaposing/filtering it with ancillary content
according to the rules in that model,
including a history of prior message events and their results.
If appropriate, Kamanja prepares and forwards output
to consuming applications or even other models in the working set.
The following graphic gives a high-level view of how Kamanja works:

.. image:: /_images/engine-basics.png

Kamanja is cluster-based.
In other words, Kamanja partitions work into distributable units
that other nodes in the cluster can execute and solve.
The system then can scale linearly
with the number of nodes participating in the cluster.
Furthermore, each node in the cluster
can simultaneously execute multiple models in parallel,
and manage the dependencies between the models executing at that moment.

The working set includes all of the models
that are executing on a given Kamanja cluster.
The model produces the output message.
This model can consume other models.
This means that other models that are in the working set
can consume the output of a previous model
or even the same input plus the output message
that a prior model generates.
This is a particularly useful mechanism to control model complexity,
effectively building up a group of models
that work together to analyze the input message content.

Nothing special needs to be done to make this happen
except to design the input and output data messages properly.
The engine uses metadata that the model generates at compile-time
to detect the input and output relationships
of the related models automatically.
This permits the engine to build a partial ordering of the models
so that all their inputs are available when they execute.


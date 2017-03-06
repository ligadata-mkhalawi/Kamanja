
.. _partition-term:

Partition
---------

partitions and threads
~~~~~~~~~~~~~~~~~~~~~~

On a multi-node cluster,
each partition has its own thread.

For example, consider a Kamanja cluster with 5 nodes;
storage has Model M.
The normal flow is that the input adapter reads the input data
and creates a proper :ref:`message<message-def-config-ref`>`
and :ref:`adapter binding<adapter-binding-config-ref>`
for model M to consume that message.
This flow happens on ONE thread per file.





.. _partition-term:

Partition
---------

partitions and threads
~~~~~~~~~~~~~~~~~~~~~~

On a multi-node cluster,
each partition has its own thread.

assume we have a cluster configuration, say 5 nodes of kamanja. storage has model M.
the normal flow is that input adapter reads the input (say a file) and creates proper message for model M to consume that message. this flow happens on ONE thread per file, right?
Question: in cluster config, each file (say F1, F2) will have their own threads, which will probably let model M consume the message of F1 or F2 on any of the nodes, correct? in other words, the model might right on any of the nodes for any given thread.

Can we just say two partitions (one file per partition,
if we set proper number of consumers)
are executing now in the cluster and both will run parallel.
And the model execution can happen on any node.
-- Pokuri says this is incorrect



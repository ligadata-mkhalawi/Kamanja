
.. _cluster-term:

Cluster (Kamanja)
-----------------

In a Kamanja environment,
a cluster is a group of one or more Kamanja :ref:`nodes<node-term>`
that work together on the same task;
typically, clusters do not overlap.
Kamanja scales horizontally;
to increase the capacity of the environment,
you can add additional nodes to the cluster
or add additional clusters.

For more information:

- The :ref:`clusterconfig-config-ref` file
  defines the ClusterId for the cluster,
  all the nodes, :ref:`adapters<adapter-term>`,
  :ref:`messages<messages-term>`, and :ref:`tenants<tenancy-term>`
  that are configured for the cluster.
  It also includes configuration details for services used by the cluster
  such as :ref:`ZooKeeper<zookeeper-term>` and :ref:`Kafka<kafka-term>`.
- The :ref:`metadataapiconfig-config-ref` file
  defines the :ref:`metadata<metadata-term>`
  for the cluster.
- Use the :ref:`clusterinstallerdriver-command-ref` command
  to install a cluster.
  For full instructions, see :ref:`kamanja-install-top`



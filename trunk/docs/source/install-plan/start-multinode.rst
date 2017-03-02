
.. _start-multi-cluster:

Start the multi-node cluster
============================

To start the cluster,
**ssh** to any of the nodes then navigate to the *KAMANJA_HOME/bin* folder
and run the following command:

::

  ./StartKamanjaCluster.sh --ClusterId ligadata1
    --MetadataAPIConfig ../config/MetadataAPIConfig.properties


The cluster should start on all nodes
that are defined in the *ClusterConfig.json* file.
In this case, four nodes are configured.


- The structure of the installed software is described in
  :ref:`dir-struct-install`.
- If you installed this cluster to study and explore Kamanja,
  a good next step is to run and study the
  :ref:`sample applications<run-samples-install>`
  that are provided.
- You can also create your own application
  by creating and configuring your own messages, models, and so forth.
  See :ref:`models-top`.




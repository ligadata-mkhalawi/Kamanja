

.. _statuskamanjacluster-command-ref:

StatusKamanjaCluster.sh
=======================

Reports the health of each node in the cluster.

The script reads the cluster metadata,
contacts each of the nodes described by it,
retrieves the respective PIDs,
and issues a ps command to see the status
of the Kamanja engine that is running on each node.

Syntax
------

::

  StatusKamanjaCluster.sh--ClusterId <cluster-name-identifer>
     --MetadataAPIConfig <metadataAPICfgPath>

Options and arguments
---------------------

- **ClusterId** -- string identifier for the cluster.
  This must match the value of the "ClusterID" field in the
  :ref:`clusterconfig-config-ref` file for the cluster.

- **MetadataAPIConfig** -- :ref:`metadataapiconfig-config-ref`
  for this cluster.
  It should refer to legitimate cluster metadata in the metadata cache
  found in the MetadataAPIConfig file given.

Usage
-----

See also
--------



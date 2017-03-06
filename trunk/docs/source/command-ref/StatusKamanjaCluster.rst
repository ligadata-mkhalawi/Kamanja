

.. _statuskamanjacluster-command-ref:

StatusKamanjaCluster.sh
=======================

Reports the health of each node in the cluster.


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

The script reads the cluster metadata,
contacts each of the nodes described by it,
retrieves the respective PIDs
that were created by the :ref:`startkamanjacluster-command-ref` command,
and issues a **ps** command to see the status
of the Kamanja engine that is running on each node.
The script reports the health of each node (whether it is up or down).

See also
--------


- :ref:`startkamanjacluster-command-ref`
- :ref:`stopkamanjacluster-command-ref`



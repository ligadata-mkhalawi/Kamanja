

.. _startkamanjacluster-command-ref:

StartKamanjaCluster.sh
======================

Start the specified cluster using the specified 
Metadata API file.
To issue this command,
**ssh** to any node in the cluster then issue the command.
The cluster should start on all nodes
that are defined in the *ClusterConfig.json* file for that cluster.


Syntax
------


::

  cd $KAMANJA_HOME/bin
  ./StartKamanjaCluster.sh --ClusterId <clusterid>
    --MetadataAPIConfig <path-of-MetadataAPIConfig.properties-file>



Options and arguments
---------------------

- **ClusterId** -- ID of the cluster as defined in the
  :ref:`clusterconfig-config-ref` file
- **MetadataAPIConfig** -- full path of the
  :ref:`metadataapiconfig-config-ref` file to use.
  Note that this file is usually located in the */config* directory
  so a relative pathname such as *../<file>* form is often used.
  

Usage
-----

This script reads the cluster metadata,
contacts each of the nodes that are defined for that cluster,
and starts the Kamanja engine at that location.
A Process Identifier (PID) is recorded
and written to the installation directory's *run* directory
for each of the nodes started.
This PID file is used by the
:ref:`statuskamanjacluster-command-ref` script
to verify that a process is alive on each respective cluster node.
The :ref:`stopkamanjacluster-command-ref` script
uses the PID to stop the cluster nodes that are running.

Use the :ref:`kamanja-command-ref` **start** command
to start individual nodes in the cluster.

Example
-------

::

  ./StartKamanjaCluster.sh --ClusterId ligadata1
    --MetadataAPIConfig ../config/MetadataAPIConfig.properties


See also
--------

- :ref:`clusterconfig-config-ref`
- :ref:`metadataapiconfig-config-ref`
- :ref:`kamanja-command-ref`

- :ref:`stopkamanjacluster-command-ref` 
- :ref:`statuskamanjacluster-command-ref` 



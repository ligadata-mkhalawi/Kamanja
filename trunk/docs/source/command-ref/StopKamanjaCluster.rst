

.. _stopkamanjacluster-command-ref:

StopKamanjaCluster.sh
======================

Stop the specified cluster using the specified 
Metadata API file.

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
contacts each of the nodes described by it,
and stops the process with the Process Identifier (PID)
that was written by the :ref:`startkamanjacluster-command-ref` command.

Use the :ref:`kamanja-command-ref` **stop** command
to stop individual nodes in the cluster.

Example
-------

::

  ./StopKamanjaCluster.sh --ClusterId ligadata1
    --MetadataAPIConfig ../config/MetadataAPIConfig.properties


See also
--------

- :ref:`clusterconfig-config-ref`
- :ref:`metadataapiconfig-config-ref`
- :ref:`kamanja-command-ref`

- :ref:`startkamanjacluster-command-ref` 
- :ref:`statuskamanjacluster-command-ref` 



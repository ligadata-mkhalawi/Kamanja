

.. _startkamanjacluster-command-ref:

StartKamanjaCluster.sh
======================

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

Example
-------

::

  ./StartKamanjaCluster.sh --ClusterId ligadata1
    --MetadataAPIConfig ../config/MetadataAPIConfig.properties


See also
--------



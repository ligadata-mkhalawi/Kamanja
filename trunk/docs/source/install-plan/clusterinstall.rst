
.. _clusterinstallerdriver-install:

Run ClusterInstallerDriver and start the multi-node cluster
===========================================================

After modifying the :ref:`clusterconfig-config-ref`
and :ref:`clustercfgmetadataapiconfig-config-ref` files,
navigate to the */CLusterInstall* directory
and run :ref:`ClusterInstallerDriver-1.6.2<clusterinstallerdriver-command-ref>`
specifying:

- The
  :ref:`ClusterCfgMetadataAPIConfig.properties<metadataapiconfig-config-ref>`
  and :ref:`ClusterConfig.json<clusterconfig-config-ref>` files
  that you edited as described in :ref:`config-edit-install`
- The tar file downloaded as described in :ref:`kamanja-download`.

.. note:  Before running this command, be sure that :ref:`ssh-term`
   is set up from each node to each node in the cluster,
   including from each node to itself.

A sample script:


::

  java -jar ClusterInstallerDriver-1.5.3 --install
    --apiConfig ClusterCfgMetadataAPIConfig.properties
    --clusterConfig ../config/ClusterConfig.json --toScala 2.11
    --tarballPath ../../Kamanja-1.5.3_2.11.tar.gz --tenantId tenant1
    --adapterMessageBindings ../config/SystemMsgs_Adapter_Binding.json
    --skipPrerequisites all




Note: ClusterInstall attempts to create a directory
defined in $KAMANJA_HOME.
If a directory with the same name is already present on the system,
delete it and try again.

When this has run successfully,
your cluster is installed and ready to use.
You should see the deployed instance of Kamanja
on all nodes on the target destination you defined
in *ClusterConfig.json* and *ClusterCfgMetadataAPIConfig.properties*;
under the $KAMANJA_HOME directory.

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




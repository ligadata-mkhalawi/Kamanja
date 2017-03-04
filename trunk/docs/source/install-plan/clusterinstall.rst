
.. _clusterinstallerdriver-install:

Run ClusterInstallerDriver
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

You can now start your cluster
following the instructions in :ref:`start-stop-cluster`.



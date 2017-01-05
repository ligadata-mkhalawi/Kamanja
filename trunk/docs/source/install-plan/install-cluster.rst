
.. _cluster-install:

Install a cluster
=================

Set up passwordless SSH
-----------------------

Before performing a cluster install you must set up passwordless SSH.

Follow the instructions in RRavi Saive's excellent article,
`SSH Passwordless Login Using SSH Keygen in 5 Easy Steps
<http://www.tecmint.com/ssh-passwordless-login-using-ssh-keygen-in-5-easy-steps/>`_.

Edit the configuration file
---------------------------

{InstallDirectory}, found throughout the
:ref:`MetadataAPIConfig.properties<metadataapiconfig-config-ref>`
and :ref:ClusterConfig.json<cluster-config-ref>` configuration files,
must be modified to show the directory where Kamanja will be installed.
The location must be the same on all nodes in the custer.

Download the Kamanja software
-----------------------------

Follow the instructions in :ref:`kamanja-install-top`
to install the prerequisite packages
and then download and extract the Kamanja software.

Update MetadataAPIConfig.properties and ClusterConfig.json files
----------------------------------------------------------------

Run the following command to update the
:ref:`metadataapiconfig-config-ref` and
:ref:`clusterconfig-config-ref` files:

::

  cd /home/my_install/Kamanja160/Kamanja-1.5.0_2.11/ClusterInstall

Need to revisit these steps; this is a fresh install
so why are they needing to migrate?
How much do we need to retain information about pre-1.4 releases?

- Update the MetadataAPI file according to the following matrix.
- Update the cluster configuration file according to the following matrix.

Stop the cluster
----------------


Run ClusterInstallerDriver-1.6.0
--------------------------------

Run :ref:`ClusterInstallerDriver-1.6.0<clusterinstallerdriver-command-ref>`
using the adapters binding file,
:ref:`ClusterCfgMetadataAPIConfig.properties<metadataapiconfig-config-ref>` file
and :ref:`ClusterConfig.json<clusterconfig-config-ref>`
with the 1.6.0 release package.

A sample script:

[Note that this specifies upgrade, not install]

::

  export KAMANJA_ROOT=/media/home2/installKamanja160
  export KAMANJA_INSTALL_HOME=$KAMANJA_ROOT/Kamanja-1.6.0_2.11/ClusterInstall

  java -Dlog4j.configurationFile=file:$KAMANJA_INSTALL_HOME/log4j2.xml \
      -jar $KAMANJA_INSTALL_HOME/ClusterInstallerDriver-1.6.0 \
      --clusterId "kamanjacluster160" \
      --apiConfig "$KAMANJA_INSTALL_HOME/ClusterCfgMetadataAPIConfig.properties" \
      --clusterConfig "$KAMANJA_INSTALL_HOME/ClusterConfig.json" \
      --tarballPath "$KAMANJA_ROOT/Kamanja-1.6.0_2.11.tar.gz" \
      --fromKamanja "1.3" --fromScala "2.11" --toScala "2.11" \
      --upgrade --externalJarsDir /media/home2/external_libs --tenantId kamanja
  
If upgrading from 1.4 or 1.4.1, don’t use the adapters binding file.

Note: ClusterInstall attempts to create a directory
defined in {InstallDirectory}.
If a directory with the same name is already present on the system,
delete it and try again.

For a description of ClusterInstallerDriver-1.6.0 parameters,
see ClusterInstallerDriver-1.6.0 parameters.

Make sure that $KAMANJA_HOME points to the newly installed cluster
------------------------------------------------------------------

Add new messages and/or JTMs as appropriate
-------------------------------------------

If the input adapter definition contains an AssociatedMessage,
it is called tagged.
So if the input adapters contain tagged messages,
add new messages and/or JTMs as appropriate.
Refer to the JTMs for more information.

Start the cluster
-----------------



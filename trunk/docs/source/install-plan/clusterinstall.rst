
.. _clusterinstallerdriver-install:

Run ClusterInstallerDriver
==========================

Run :ref:`ClusterInstallerDriver-1.6.2<clusterinstallerdriver-command-ref>`
using the adapters binding file,
:ref:`ClusterCfgMetadataAPIConfig.properties<metadataapiconfig-config-ref>` file
and :ref:`ClusterConfig.json<clusterconfig-config-ref>`
with the 1.6.2 release package.

A sample script:

[Note that this specifies upgrade, not install]

::

  export KAMANJA_ROOT=/media/home2/installKamanja162
  export KAMANJA_INSTALL_HOME=$KAMANJA_ROOT/Kamanja-1.6.2_2.11/ClusterInstall

  java -Dlog4j.configurationFile=file:$KAMANJA_INSTALL_HOME/log4j2.xml \
      -jar $KAMANJA_INSTALL_HOME/ClusterInstallerDriver-1.6.2 \
      --clusterId "kamanjacluster162" \
      --apiConfig "$KAMANJA_INSTALL_HOME/ClusterCfgMetadataAPIConfig.properties" \
      --clusterConfig "$KAMANJA_INSTALL_HOME/my-ClusterConfig.json" \
      --tarballPath "$KAMANJA_ROOT/Kamanja-1.6.2_2.11.tar.gz" \
      --fromKamanja "1.3" --fromScala "2.11" --toScala "2.11" \
      --upgrade --externalJarsDir /media/home2/external_libs --tenantId kamanja
  
If upgrading from 1.4 or 1.4.1, don’t use the adapters binding file.

Note: ClusterInstall attempts to create a directory
defined in {InstallDirectory}.
If a directory with the same name is already present on the system,
delete it and try again.

For a description of ClusterInstallerDriver-1.6.0 parameters,
see ClusterInstallerDriver-1.6.0 parameters.


When this has run successfully,
your cluster is installed and ready to use.

- The structure of the installed software is described in
  :ref:`dir-struct-install`.
- If you installed this cluster to study and explore Kamanja,
  a good next step is to run and study the
  :ref:`sample applications<run-samples-install>`
  that are provided.
- You can also create your own application
  by creating and configuring your own messages, models, and so forth.
  See :ref:`models-top`.




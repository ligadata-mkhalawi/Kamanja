
.. _upgrade-install-top:

Upgrade to 1.6.2
================

You can upgrade to Release 1.6.2 from an earlier Kamanja release,
The steps are:

- Verify that you have the correct versions of
  the Kamanja prerequises packages installed on your system;
  see :ref:`pkgs-prereqs-install`.
- Verify that all other software on the system
  is a version that is compatible with Kamanja 1.6.2;
  check the list at :ref:`component-versions`.
- Edit your *.bashrc* file (Linux) or *.bash_profile* (Mac)
  file to ensure that you have the correct path defined
  for all software components;
  see :ref:`env-variables-install`.
- Download and unzip the 1.6.2 package to $KAMANJA_INSTALL;
  follow the instructions in :ref:`kamanja-download`
- Copy the :ref:`clusterconfig-config-ref` file
  from your current Kamanja installation
  to the *$KAMANJA_INSTALL/config* directory
  and edit it to be appropriate for the new release
  by referencing the *ClusterConfig.json* file that is located there..
  See :ref:`upgrade-cluster-config`.
- Copy the :ref:`metadataapiconfig-config-ref` file
  from your current Kamanja installation
  to the *$KAMANJA_INSTALL/config* directory
  and edit it to be appropriate for the new release.
- Stop the cluster.
- Run the :ref:`clusterinstallerdriver-command-ref` command
  with the **-update** flag,
  specifying the edited configuration files you created above;
  see :ref:`run-clusterinstaller`
- Make sure that the $KAMANJA_HOME environment variable
  points to the newly installed cluster.
- Check and possibly modify your applications
  for the new release;  see :ref:`check-apps-upgrade`.
- Start the cluster,
  following the instructions in :ref:`start-node-install-guide`.
- If you need to rollback the cluster to the previous release,
  follow the instructions in :ref:`rollback-guide`.




.. _upgrade-cluster-config:

Edit the ClusterConfig.json file
--------------------------------

Copy the :ref:`clusterconfig-config-ref` file
from your current Kamanja installation
to the *$KAMANJA_INSTALL/config* directory,
giving it a name like *myClusterConfig.json*.
Edit this file to be appropriate for the new release
by referencing the *ClusterConfig.json* file that is located there.

See the reference pages for details about the contents of this file.
Specifically, set the following:

- Be sure that each node in the cluster is defined;
  you can add nodes to the cluster at this time if you like.
- Be sure that the **NodeId**, **NodePort**, and **NodeIpAddr** parameters
  are populated appropriately for each node.
- Define all :ref:`tenants<tenancy-term>` to be used.
- Set the **Scala_home** and **Java_home** parameters;
  be sure that the values match what is assigned
  to the **SCALA_HOME** and **JAVA_HOME** parameters
  in the *ClusterCfgMetadataAPIConfig.properties* file.
  The easiest and safest approach is to use environment variables
  such as $JAVA_HOME and $SCALA_HOME to populate these parameters.
- If you want to change the data store used for the cluster,
  follow the instructions in :ref:`change-data-store-cluster`.
- If you want to implement new 1.6.2 features
  such as Velocity Metrics in your cluster,
  copy and populate those sections from the
  *$KAMANJA_HOME/config/ClusterConfig.json* file.

.. note:: You can instead make a copy of the *ClusterConfig.json* file
   that is in the *$KAMANJA_INSTALL/config* directory
   and modify that with information about your cluster.


.. _upgrade-metadata-config:

Edit the ClusterCfgMetadataAPIConfig.properties file
----------------------------------------------------

Copy the :ref:`metadataapiconfig-config-ref` file
from your current Kamanja installation
to the *$KAMANJA_INSTALL/config* directory,
giving it a name like *myMetadataAPIConfig.properties*,
and edit it to be appropriate for the new release.

.. note:: You can instead make a copy of the
   :ref:`clustercfgmetadataapiconfig-config-ref` file
   that is in *$KAMANJA_INSTALL/config* and edit that
   with information about your environment.

Specifically, set the following:

- Check the **SCALA_HOME**, **JAVA_HOME**, **JAR_TARGET_DIR**
  parameters.
  Be sure that they contain the same values as the
  comparable parameters in the *ClusterConfig.json* file.
  The easiest and safest approach is to use environment variables
  such as $JAVA_HOME and $SCALA_HOME to populate these parameters.
- Set the **ROOT_DIR** parameter to indicate the parent
  directory (such as */opt/kamanja*)
  being used for your current Kamanja software.
- Set the **SERVICE_HOST** and **SERVICE_PORT** parameters
  with the IP address and port number used for the
  Kamanja metadata API REST service,
  if you are using it.
  For example, the SERVICE_HOST={HostName} line is replaced
  with SERVICE_HOST=180.34.23.1 where 180.34.23.1
  is the IP address of the cluster node
  where the Kamanja metadata API service runs.


.. _run-clusterinstaller:

Run the ClusterInstallerDriver.sh command
-----------------------------------------

Run the :ref:`clusterinstallerdriver-command-ref` command
with the **-update** flag.
See the reference page for the specific syntax.

**ClusterInstallerDriver.sh** uses the information
in the configuration files you edited
plus information specified on the command line
to poulate the :ref:`migrateconfig-template-config-ref` file.
It then creates a new directory tree
under the same parent directory used for the old release
and installs the new release in that new directory structure.

.. _check-apps-upgrade:

Check applications
------------------

**ClusterInstallerDriver** upgrades Kamanja software
but you may need to make some additional changes
to your application code before restarting the cluster.
Some examples of things to check:

- Verify that all jars associated with your application
  include the correct strings for the Kamanja release
  and Scala version.
  All jars are in /lib/system -- look there for the jar file to use;
  in most cases, you just need to change,
  for example, the "1.5.3.jar" string to "1.6.2.jar"
- Add new messages and/or JTMs as appropriate.
  If the input adapter definition contains an AssociatedMessage,
  it is called tagged.
  So if the input :ref:`adapters<adapter-term>` contain tagged messages,
  add new messages and/or JTMs as appropriate.
  Refer to the :ref:`jtm-guide-top` for more information.

.. _rollback-guide:

Rollback to previous version
----------------------------



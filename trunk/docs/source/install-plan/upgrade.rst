
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
- Edit the :ref:`clusterconfig-config-ref` file
  with information about your environment.
  Much of this information can be gleaned from the
  existing *ClusterConfig.json* file.
  See :ref:`upgrade-cluster-config`.
- Edit the :ref:`clustercfgmetadataapiconfig-config-ref` file
  with information about your installation
  and the metadata objects used by your application.
  see :ref:`upgrade-metadata-config`
- Run the :ref:`clusterinstallerdriver-command-ref` command
  to upgrade the system to the new Kamanja release.
  See :ref:`run-clusterinstaller`.
- Make sure that the $KAMANJA_HOME environment variable
  points to the newly installed cluster.
- Stop the cluster.
- Run the :ref:`clusterinstallerdriver-command-ref` command
  with the **-update** flag; see :ref:`run-clusterinstaller`
- Check and possibly modify your applications
  for the new release;  see :ref:`check-apps-upgrade`.
- Start the cluster,
  following the instructions in :ref:`start-node-install-guide`.
- To rollback the cluster to the previous release,
  redefine $KAMANJA_HOME to point to the old release directory structure.




.. _upgrade-cluster-config:

Edit the ClusterConfig.json file
--------------------------------

Update the :ref:`clusterconfig-config-ref` cluster configuration file
to define your environment;
see the reference pages for details about the contents of this file.
Specifically, set the following:

- Define each node in the cluster,
  populating the **NodeId**, **NodePort**, and **NodeIpAddr** parameters
  for each node.
- Define all :ref:`tenants<tenancy-term>` to be used.
- Set the **Scala_home** and **Java_home** parameters;
  be sure that the values match what is assigned
  to the **SCALA_HOME** and **JAVA_HOME** parameters
  in the *ClusterCfgMetadataAPIConfig.properties* file.

- Replace the **SystemCatalog** section
  with the datastore information for the current Kamanja deployment,
  if needed.
  For example, if using sqlserver as the data source, replace:

::

    Before

    "SystemCatalog": {
     "StoreType": "hbase",
     "SchemaName": "syscatalog",
     "Location": "localhost",
     "authentication": "kerberos",
     "regionserver_principal": "hbase/_HOST@INTRANET.LIGADATA.COM",
     "master_principal": "hbase/_HOST@INTRANET.LIGADATA.COM",
     "principal": "ligadata@INTRANET.LIGADATA.COM",
     "keytab": "/home/ligadata/keytab/ligadata.keytab"
    },

with

::

    After

    "SystemCatalog": {
     "StoreType": "sqlserver",
     "hostname": "192.168.56.1",
     "instancename": "KAMANJA",
     "portnumber": "1433",
     "database": "syscatalog",
     "user": "catalog_user",
     "SchemaName": "catalog_user",
     "password": "catalog_user",
     "jarpaths": "/media/home2/jdbc",
     "jdbcJar": "sqljdbc4-2.0.jar",
     "clusteredIndex": "YES",
     "autoCreateTables": "YES"
    },


- Replace the **PrimaryDataStore** section in the *ClusterConfig.json* file
  with the datastore information for the current Kamanja deployment, if needed.
  For example, if using sqlserver as the data source, replace:

::

    Before

    "PrimaryDataStore": {
     "StoreType": "hbase",
     "SchemaName": "tenant1_default",
     "Location": "localhost",
     "authentication": "kerberos",
     "regionserver_principal": "hbase/_HOST@INTRANET.LIGADATA.COM",
     "master_principal": "hbase/_HOST@INTRANET.LIGADATA.COM",
     "principal": "ligadata@INTRANET.LIGADATA.COM",
     "keytab": "/home/ligadata/keytab/ligadata.keytab"
    },

with

::

    After

    "PrimaryDataStore": {
     "StoreType": "sqlserver",
     "hostname": "192.168.56.1",
     "instancename": "KAMANJA",
     "portnumber": "1433",
     "database": "kamanja_tenant",
     "user": "all_tenants",
     "SchemaName": "all_tenants",
     "password": "all_tenants",
     "jarpaths": "/media/home2/jdbc",
     "jdbcJar": "sqljdbc4-2.0.jar",
     "clusteredIndex": "YES",
     "autoCreateTables": "YES"
    },

.. _upgrade-metadata-config:

Edit the ClusterCfgMetadataAPIConfig.properties file
----------------------------------------------------

Update the :ref:`clustercfgmetadataapiconfig-config-ref` file
to values appropriate for your environment.
Specifically, set the following:

- Set the **SCALA_HOME**, **JAVA_HOME**, **JAR_TARGET_DIR**
  parameters.
  Be sure that they contain the same values as the
  comparable parameters in the *ClusterConfig.json* file.
- Set the **ROOT_DIR** parameter

- Set the **SERVICE_HOST** and **SERVICE_PORT** parameters
  with the IP address and port number used for the
  Kamanja metadata API REST service,
  if you are using it..
  is updated with one of the Kamanja cluster node IP addresses
  For example, the SERVICE_HOST={HostName} line is replaced
  with SERVICE_HOST=180.34.23.1 where 180.34.23.1
  is the IP address of the cluster node
  where running the Kamanja metadata API service.


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




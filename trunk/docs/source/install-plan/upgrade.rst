
.. _upgrade-install-top:

Basic upgrade to 1.6.2
======================

To upgrade to Release 1.6.2 from an earlier Kamanja release,

Download the 1.6.2 package to */tmp* or */Downloads*
or some other location that is not where you want
to install the Kamanja software.

Upgrade -- to 1.6.2 from 1.5.x to 1.6.2


- download tarball; unzip in /tmp
- two options:

  - copy existing MetadataAPI and ClusterConfig to /tmp and
    make mods in old docs
  - modify these files in /tmp and copy info about application
    objects in old files into the new files

- may need to change jars from versions with 1.5.3 in the name.
  All jars are in /lib/system -- look there for the jar file
  to use -- probably just change "1.5.3.jar" string to "1.6.2.jar"

Step 4: Update ClusterCfgMetadataAPIConfig.properties and ClusterConfig.json

Run the following command:
Go to the directory where the 1.6.2 tar file was extracted:

::

  cd ......../Kamanja-1.6.2_2.11/ClusterInstall

Update the ClusterCfgMetadataAPIConfig.properties properties file
according to the following matrix.
Update the ClusterConfig.json cluster configuration file
according to the following matrix.
The first "matrix" link includes the following items:

- {ScalaInstallDirectory} is replaced with the Scala installation directory,
  such as /opt/apps/scala-2.11.7.

- {JavaInstallDirectory} is replaced with the JDK installation directory,
  such as /opt/apps/jdk1.8.0_05 (depending on the Java version used).

- {InstallDirectory} is replaced with the current Kamanja home,
  such as /opt/apps/kamanja12 (assuming the current version is 12).
  Note: When the upgrade completes,
  the current Kamanja home is saved with a different name.
  But the same path contains the 1.6.0 binaries.

- {HostName} is updated with one of the Kamanja cluster node IP addresses
  if using the Kamanja metadata API REST service.
  For example, the SERVICE_HOST={HostName} line is replaced
  with SERVICE_HOST=180.34.23.1 where 180.34.23.1
  is the IP address of the cluster node
  where running the Kamanja metadata API service.

The second "matrix" link includes the following items:

- {ScalaInstallDirectory} is replaced with the Scala installation directory,
  such as /opt/apps/scala-2.11.7.

- {JavaInstallDirectory} is replaced with the JDK installation directory, such as /opt/apps/jdk1.8.0_05 (depending on the Java version used).

- {InstallDirectory} is replaced with the current Kamanja home, such as /opt/apps/kamanja12 (assuming the current version is 12).Note: When the upgrade completes, the current Kamanja home is saved with a different name, but the same path contains the 1.5.0 binaries.

- Replace the SystemCatalog section in ClusterConfig.json
  with the datastore information for the current Kamanja deployment,
  if needed.For example, if using sqlserver as the data source, replace:

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


- Replace the PrimaryDataStore section in ClusterConfig.json
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


Step 5: Stop the cluster

Step 6: Run ClusterInstallerDriver-1.6.0

Run ClusterInstallerDriver-1.6.0 using the adapters binding file,
ClusterCfgMetadataAPIConfig.properties,
and ClusterConfig.json with the 1.6.0 release package.

A sample shell script:

::

  export KAMANJA_ROOT=/media/home2/installKamanja150
  export KAMANJA_INSTALL_HOME=$KAMANJA_ROOT/Kamanja-1.6.0_2.11/ClusterInstall

  java -Dlog4j.configurationFile=file:$KAMANJA_INSTALL_HOME/log4j2.xml -jar $KAMANJA_INSTALL_HOME/ClusterInstallerDriver-1.5.0 --clusterId “kamanjacluster150” --apiConfig “$KAMANJA_INSTALL_HOME/ClusterCfgMetadataAPIConfig.properties” --clusterConfig “$KAMANJA_INSTALL_HOME/ClusterConfig.json” --tarballPath “$KAMANJA_ROOT/Kamanja-1.6.0_2.11.tar.gz” --fromKamanja “1.3” --fromScala “2.10” --toScala “2.11” --upgrade --externalJarsDir /media/home2/external_libs --tenantId kamanja --adapterMessageBindings /tmp/AdapterMessageBindings.json

where /tmp/AdapterMessageBindings.json is the file generated in step two.



For a description of ClusterInstallerDriver-1.6.0 parameters,
see ClusterInstallerDriver-1.6.0 parameters.

Step 7: Make sure that $KAMANJA_HOME points to the newly installed cluster

Step 8: Add new messages and/or JTMs as appropriate

If the input adapter definition contains an AssociatedMessage, it is called tagged. So if the input adapters contain tagged messages, add new messages and/or JTMs as appropriate. Refer to the JTMs for more information.

Step 9: Start the cluster



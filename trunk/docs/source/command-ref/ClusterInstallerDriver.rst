
.. _clusterinstallerdriver-command-ref:

ClusterInstallerDriver.sh
=========================

Install a fresh Kamanja installation on a multi-node cluster
or upgrade from an earlier release.

Before running this command,
you must:

- :ref:`Download the Kamanja package<kamanja-download>`
- Unzip that package
- Edit the :ref:`clustercfgmetadataapiconfig-config-ref`
  and :ref:`clusterconfig-config-ref` files;
  **ClusterInstallerDriver.sh** uses these files
  to get information about how to set up the cluster.


For upgrade, they need to move info about application objects
from their old MetadataAPIConfig.properties file into the
file that is in /tmp before they upgrade

Copy over then make necessary updates to 1.6.2 files -- maybe change
database store -- copy old files from existing KAMANJA_HOME to new
/tmp where tarball is -- into CLusterInstall directory -- don't
overwrite the new file -- creating unique links -- 

Syntax
------

$KAMANJA_INSTALL is the directory to which
the Kamanja package is downloaded;
default location is */tmp*.

::

  export KAMANJA_ROOT=/media/home2/installKamanja162
  export KAMANJA_INSTALL=$KAMANJA_ROOT/Kamanja-1.6.2_2.11/ClusterInstall


  $KAMANJA_INSTALL/ClusterInstall/ClusterInstallDriver.sh
      -install|upgrade
      -apiConfig <path-to-MetadataAPIConfig.properties>
      -clusterConfig <path-to-ClusterConfig.json>
      -tarballPath <tarball-path>
      -toScala "2.11" | "2.10"
      -tenantID <tenantID>
      [-adapterMessageBindings <path-to-adapterMessageBindings-file>]
      [-workingDir <workingdirectory>]
      [-clusterId <id>]
      [-logDir <logDir>]
      [-migrationTemplate <MigrationTemplate>]
      [-skipPrerequisites "scala,java,hbase,kafka,zookeeper,all"]
      [-preRequisitesCheckOnly]


Options and arguments
---------------------

($KAMANJA_HOME is the 1.6.2 KAMANJA_HOME)

- **–upgrade | –install** -
  Specify whether this is a fresh install or an upgrade.
  The installation fails if any of the following are true:

  - Specify -install on a system where Kamanja is already installed
  - Specify -upgrade on a system with no existing installation of Kamanja
  - Specify both -upgrade and -install
   
- **–apiConfig <MetadataAPIConfig.properties file>** -
  Full pathname of the :ref:`metadataapiconfig-config-ref` file.
  Note that the same location is used for all nodes in the cluster.
   
- **–clusterConfig <ClusterConfig.json file>** -
  Full pathname of the :ref:`clusterconfig-config-ref` file.
  This file defines the IP address of each node and other information
  required to configure the cluster.
   
- **–tarballPath <tarball path>** -
  Full pathname of the tarball from which to install the Kamanja software.
  The tarball is copied to each of the cluster nodes
  then extracted and installed there.
   
- **–toScala “2.11” | “2.10”** -
  Specifies the Scala version to install.
  This controls the compiler version used by
  the Kamanja message compiler, Kamanja PMML compiler,
  and other components use when building their respective objects.
  The requested Scala version must already be installed on each node
  or the insallation fails.
 
- **–tenantId <ID>** -
  Specify the tenantID to apply to all metadata objects.
  See :ref:`tenancy-term`.
   
- **–adapterMessageBindings <JSON file>** -
  Gives the JSON file that contains the adapter message bindings.
   
- **–fromKamanja “N.N”** - (Upgrade only)
  Specifies the release from which you are upgrading.
  Valid values are “1.2”, “1.3”, 1.4, 1.4.1, or 1.5.0.
   
- **–fromScala “2.10”** - (Upgrade only)
  Specifies the Scala release used for the installation being upgraded;
  "2.10” is the only possible value for this release.
  This parameter merely documents the version of Scala installed
  with the previous release.
   
- **–workingDir <workingdirectory>** - (optional)
  Specifies the CRUD directory path that should be addressable
  on each node specified in the cluster configuration file
  as well as on the machine that this program is executing.
  It is used by this program and files it invokes
  to create intermediate files used during the installation process.
  Default value is */tmp/work*.
   
- **–clusterId <id>** - (Optional)
  Describes the key used to extract the cluster metadata
  from the node configuration.
  Default value is kamanjacluster_1_6_0_2_11.
   
- **–logDir <logDir>** - (Optional)
  Directory where :ref:`log files<clusterinstallerdriver-command-ref-log>`
  are written; default location is */tmp*.
  It is highly recommended that you specify a more permanent path
  so that the installations can be documented in a location
  that is not prone to being deleted.
   
- **–migrationTemplate <MigrationTemplate>** -
  Specify the location of the :ref:`migrateconfig-template-config-ref` file.

- -**-skipPrerequisites "scala,java,hbase,kafka,zookeeper,all"** - (Optional)
  Software components listed here are not checked before
  running the installation/upgrade.
   
- **–preRequisitesCheckOnly** - (Optional)
  Verify that all prerequisite software components are installed
  but do not execute the installation or update.
  If both **–skipPrerequisites** and **–preRequisitesCheckOnly**
  are specified, the command only checks the components
  that are not listed in the skip list.

Usage
-----

The script is installs a new version of the Kamanja release binaries
in the new installation directory on each node.
If the **--upgrade** flag is specified,
it invokes a migration JSON file
to upgrade an existing cluster installation
A migration json file is prepared with the essential
This command does the following:

- Verify that a valid environment is available on the cluster nodes
  specified in the *ClusterConfig.json* file.
  This checks that each :ref:`node<node-term>`
  mentioned in the *ClusterConfig.json* file exists
  and has the following:

  - is configured with the apporopriate Scala and Java versions
  - can access a valid :ref:`ZooKeeper<zookeeper-term>` cluster
  - can access a valid :ref:`Kafka<kafka-term>` cluster
  - can use the :ref:`HBase<hbase-term>` KV store
    that is configured for this cluster.

Notes:

- The prior installation directory, if upgrading,
  and the new installation directory are determined
  by the value of the ROOT_DIR parameter
  in the ref:`clustercfgmetadataapiconfig-config-ref` file

  - To ensure that the old and new installations are both located
    under the same parent directory,
    the name of the parent directory is supplied as an argument.

- The migration script requires a JSON file
  that contains information about the upgrade.
  There are substitution symbols ("macros") in the file
  that are substituted with the appropriate values,
  either supplied in the script parameters
  or from one of the configuation files whose path was supplied.
  The substitution symbols have the form:

  ::

    """({[A-Za-z0-9_.-]+})""" ...

  In other words, you can use 1 or more Alphameric characters
  plus the specified punctuation characters,
  enclosed in {} braces.

This JAR file lives in the *$KAMANJA_HOME/bin* directory generated by
the :ref:`easyInstallKamanja.sh<easyinstallkamanja-command-ref>`
command.
All the installation files required to install Kamanja
are located in that directory.

*$KAMANJA_HOME/config* contains
the various configuration files that are used during the installation,
including the default migrationTemplate. Sample cluster configuration,
and metadata API templates that are used to configure a custom Kamanja cluster.


.. _clusterinstallerdriver-command-ref-log:

Log file
--------

A log file is created each time this command is run,
located in */tmp* or in the directory specified for the **-logDir** option.
The file is named *InstallDriver.yyyyMMdd_HHmmss.log*;
for example, *InstallDriver.20160201_231101.log*.

Use this log to identify issues be encountered
during the install/upgrade operation.
Common problems include missing components and connectivity issues.

It can also provide help if you need to back off or abandoning an upgrade.

See also
--------



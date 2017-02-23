

.. _clustercfgmetadataapiconfig-config-ref:

ClusterCfgMetadataAPIConfig.properties
======================================

The *ClusterCfgMetadataAPIConfig.properties* file
is edited to reflect the values that will be populated
in the :ref:`metadataapiconfig-config-ref` file
after :ref:`clusterinstallerdriver-command-ref` is run
to install a multi-node cluster.

File structure
--------------

::


  NODEID={NodeId}
  MetadataDataStore={ "StoreType": "hbase","SchemaName":
     "kamanja","Location": "ip.of.hbase.master"}
  ROOT_DIR=$KAMANJA_HOME
  GIT_ROOT=$KAMANJA_HOME
  JAR_TARGET_DIR=$KAMANJA_HOME/application
  SCALA_HOME=/usr
  JAVA_HOME=/usr
  MANIFEST_PATH=$KAMANJA_HOME/config/manifest.mf
  CLASSPATH=$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-1.5.3.jar:
      $KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-1.5.3.jar:
      $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-1.5.3.jar
  NOTIFY_ENGINE=YES
  ZNODE_PATH=/kamanja
  API_LEADER_SELECTION_ZK_NODE=/kamanja
  ZOOKEEPER_CONNECT_STRING=172.18.127.71:2181
  COMPILER_WORK_DIR=$KAMANJA_HOME/workingdir
  SERVICE_HOST=localhost
  SERVICE_PORT=8081
  MODEL_FILES_DIR=$KAMANJA_HOME/config
  TYPE_FILES_DIR=$KAMANJA_HOME/config
  FUNCTION_FILES_DIR=$KAMANJA_HOME/config
  CONCEPT_FILES_DIR=$KAMANJA_HOME/config
  MESSAGE_FILES_DIR=$KAMANJA_HOME/config
  CONTAINER_FILES_DIR=$KAMANJA_HOME/config
  CONFIG_FILES_DIR=$KAMANJA_HOME/config
  MODEL_EXEC_LOG=false
  JarPaths=$KAMANJA_HOME/lib/system,$KAMANJA_HOME/lib/application
  SECURITY_IMPL_JAR=$KAMANJA_HOME/lib/system/simpleapacheshiroadapter_2.11-1.0.jar
  SECURITY_IMPL_CLASS=com.ligadata.Security.SimpleApacheShiroAdapter
  AUDIT_IMPL_JAR=$KAMANJA_HOME/lib/system/auditadapters_2.11-1.0.jar
  AUDIT_IMPL_CLASS=com.ligadata.audit.adapters.AuditCassandraAdapter
  DO_AUDIT=NO
  DO_AUTH=NO
  SSL_CERTIFICATE={InstallDirectory}/config/keystore.jks
  SSL_PASSWD=keystore



Parameters
----------

Usage
-----




.. _clustercfgmetadataapiproperties-ex1:

Example 1
---------

This example is for the same installation represented by
the :ref:`ClusterConfig.json Example 1<clusterconfig-ex1>`.
This file is for a bare-metal installation on a 4-node cluster
running CentOS 7 and :ref:`Hortonworks<hortonworks-term>` 2.5.3.0-37.
Other characteristics of this configuration include:

- Kafka version 0.10.0 is assumed; 
  you must use different jar files if you are using a different Kafka version;
  find the appropriate jar file names in the */lib/system* directory.
- Scala version 2.11.7 is used
- Kerberos is not enabled
- Failover is not enabled
- Velocity matrics is not enabled
- The file defines all the :ref:`adapters<adapter-term>`
  that are required to run the :ref:`Sample applications<run-samples-install>`.
  You can add additional adapters required for your applications
  after Kamanja is installed.
  
See :ref:`config-edit-install` for details about
creating and editing this file before running
the :ref:`clusterinstallerdriver-command-ref` command
to install your multi-node Kamanja cluster.

::


  NODEID={NodeId}
  MetadataDataStore={ "StoreType": "hbase","SchemaName":
     "kamanja","Location": "ip.of.hbase.master"}
  ROOT_DIR=$KAMANJA_HOME
  GIT_ROOT=$KAMANJA_HOME
  JAR_TARGET_DIR=$KAMANJA_HOME/application
  SCALA_HOME=/usr
  JAVA_HOME=/usr
  MANIFEST_PATH=$KAMANJA_HOME/config/manifest.mf
  CLASSPATH=$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-1.5.3.jar:
      $KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-1.5.3.jar:
      $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-1.5.3.jar
  NOTIFY_ENGINE=YES
  ZNODE_PATH=/kamanja
  API_LEADER_SELECTION_ZK_NODE=/kamanja
  ZOOKEEPER_CONNECT_STRING=172.18.127.71:2181
  COMPILER_WORK_DIR=$KAMANJA_HOME/workingdir
  SERVICE_HOST=localhost
  SERVICE_PORT=8081
  MODEL_FILES_DIR=$KAMANJA_HOME/config
  TYPE_FILES_DIR=$KAMANJA_HOME/config
  FUNCTION_FILES_DIR=$KAMANJA_HOME/config
  CONCEPT_FILES_DIR=$KAMANJA_HOME/config
  MESSAGE_FILES_DIR=$KAMANJA_HOME/config
  CONTAINER_FILES_DIR=$KAMANJA_HOME/config
  CONFIG_FILES_DIR=$KAMANJA_HOME/config
  MODEL_EXEC_LOG=false
  JarPaths=$KAMANJA_HOME/lib/system,$KAMANJA_HOME/lib/application
  SECURITY_IMPL_JAR=$KAMANJA_HOME/lib/system/simpleapacheshiroadapter_2.11-1.0.jar
  SECURITY_IMPL_CLASS=com.ligadata.Security.SimpleApacheShiroAdapter
  AUDIT_IMPL_JAR=$KAMANJA_HOME/lib/system/auditadapters_2.11-1.0.jar
  AUDIT_IMPL_CLASS=com.ligadata.audit.adapters.AuditCassandraAdapter
  DO_AUDIT=NO
  DO_AUTH=NO
  SSL_CERTIFICATE={InstallDirectory}/config/keystore.jks
  SSL_PASSWD=keystore

See also
--------

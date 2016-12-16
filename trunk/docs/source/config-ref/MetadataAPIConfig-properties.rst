
.. _metadataapiconfig-config-ref:

MetadataAPIConfig.properties
=============================

The MetadataAPIConfig.properties file
specifies the location of the installation on each node
defined in the :ref:`clusterconfig-config-ref` file.
Note that the same location is used for all nodes in the cluster.

File structure
--------------

::

  #MetadataStore information
  MetadataDataStore={"StoreType": "hbase","SchemaName": "metadata","Location": "localhost"}
  ROOT_DIR=/Users/userid/Downloads/installKamanja
  GIT_ROOT=/Users/userid/Downloads/installKamanja
  JAR_TARGET_DIR=/Users/userid/Downloads/installKamanja/lib/application
  SCALA_HOME=/usr/local/Cellar/scala/2.11.7
  JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home
  MANIFEST_PATH=/Users/userid/Downloads/installKamanja/config/manifest.mf
  ...
  TYPE_FILES_DIR=/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/type/
  FUNCTION_FILES_DIR=/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/function/
  CONCEPT_FILES_DIR=/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/concept/
  MESSAGE_FILES_DIR=/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/message/
  CONTAINER_FILES_DIR=/Users/userid/Downloads/installKamanja/input/SampleApplications/metadata/container/
  CONFIG_FILES_DIR=/Users/userid/Downloads/installKamanja/config/
  MODEL_EXEC_LOG=false
  JarPaths=/Users/userid/Downloads/installKamanja/lib/system,/Users/userid/Downloads/installKamanja/lib/application
  SECURITY_IMPL_JAR=/Users/userid/Downloads/installKamanja/lib/system/simpleapacheshiroadapter_2.10-1.0.jar
  SECURITY_IMPL_CLASS=com.ligadata.Security.SimpleApacheShiroAdapter
  AUDIT_IMPL_JAR=/Users/userid/Downloads/installKamanja/lib/system/auditadapters_2.10-1.0.jar
  AUDIT_IMPL_CLASS=com.ligadata.audit.adapters.AuditCassandraAdapter
  DO_AUDIT=NO
  DO_AUTH=NO
  SSL_CERTIFICATE=/Users/userid/Downloads/installKamanja/config/keystore.jks



Parameters
----------

Usage
-----

See also
--------

- :ref:`kamanja<kamanja-command-ref>` command reference page



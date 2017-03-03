
Fixed issues
============

- An issue that prevented the :ref:`kamanja-command-ref` command
  with the **start webservice** option
  or the **java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml
  -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-1.6.2.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-1.6.2.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-1.6.2.jar:
  $KAMANJA_HOME/lib/system/metadataapiservice_2.11-1.6.2.jar
  com.ligadata.metadataapiservice.APIService** command
  from running has been fixed.
  (`1519 <https://github.com/LigaData/Kamanja/issues/1519>`_)

- A problem that caused the SmartFileConsumer, when shutting down,
  to throw exceptions for each directory monitored over SFTP
  has been fixed.
  (`1517 <https://github.com/LigaData/Kamanja/issues/1517>`_)

- The condition that caused the
  **No net.sf.ehcache.EhcacheInit services found** error
  to be output when running the engine has been resolved.
  (`1470 <https://github.com/LigaData/Kamanja/issues/1470>`_)

- The *Engine1Config.properties* file no longer requires
  the ROOT_DIR property; this information is obtained
  from the :ref:`metadataapiconfig-config-ref` file.
  As part of the effort to simplify the *MetaDataAPIConfig.properties* file,
  this requirement was not removed,
  leading to situations where the Kamanja Engine could not start
  after upgrading the cluster.
  (`1465 <https://github.com/LigaData/Kamanja/issues/1465>`_)





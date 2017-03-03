
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

- :ref:`Velocity Metrics<velocity-metrics-term>`
  now properly includes the the name of the file
  to which output messages were written.
  (`1516 <https://github.com/LigaData/Kamanja/issues/1516>`_)

- A problem that caused the KamanjaManager process to hang
  when configured to use the :ref:`elastic-output-adapter-ref`
  if the ctrl-c, kill, or kill -9 commands were issued
  has been resolved.
  Note that kill -9 should not be used
  because it can cause data to be lost.
  (`1510 <https://github.com/LigaData/Kamanja/issues/1510>`_)

- :ref:`elastic-output-adapter-ref` can now connect to clusters
  that have names other than "elasticsearch".
  (`1478 <https://github.com/LigaData/Kamanja/issues/1478>`_)

- The condition that caused the
  **No net.sf.ehcache.EhcacheInit services found** error
  to be output when running the engine has been resolved.
  (`1470 <https://github.com/LigaData/Kamanja/issues/1470>`_)

- (`1467 <https://github.com/LigaData/Kamanja/issues/1467>`_)

- The *Engine1Config.properties* file no longer requires
  the ROOT_DIR property; this information is obtained
  from the :ref:`metadataapiconfig-config-ref` file.
  As part of the effort to simplify the *MetaDataAPIConfig.properties* file,
  this requirement was not removed,
  leading to situations where the Kamanja Engine could not start
  after upgrading the cluster.
  (`1465 <https://github.com/LigaData/Kamanja/issues/1465>`_)





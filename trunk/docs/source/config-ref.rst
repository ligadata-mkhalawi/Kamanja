
.. _config-ref-top:

==================================
Configuration File Reference Pages
==================================

.. list-table::
   :class: ld-wrap-fixed-table
   :widths: 30 50
   :header-rows: 1

   * - File
     - Description
   * - :ref:`adapter-def-config-ref`
     - Define adapters for the cluster
   * - :ref:`adapter-binding-config-ref`
     - Bind a message to an adapter and a serializer
   * - :ref:`clusterconfig-config-ref`
     - Configure components in a cluster
   * - :ref:`clustercfgmetadataapiconfig-config-ref`
     - File to edit to define the configuration that will be in
       :ref:`metadataapiconfig-config-ref`
       after running :ref:`clusterinstallerdriver-command-ref`
       to install or upgrade a multi-node cluster
   * - :ref:`container-def-config-ref`
     - Define a container
   * - :ref:`engineconfigproperties-config-ref`
     - Configure a node
   * - :ref:`log4j2-config-ref`
     - Control logging on the cluster
   * - :ref:`message-def-config-ref`
     - Define a message
   * - :ref:`metadataapiconfig-config-ref`
     - Configure :ref:`metadata-term` objects
   * - :ref:`migrateconfig-template-config-ref`
     - Generated file used to upgrade from an earlier release
   * - :ref:`modelcfg-config-ref`
     - Control compilation of a Java or Scala model
   * - :ref:`tenant-def-config-ref`
     - Define :ref:`tenancy<tenancy-term>` for the cluster

.. toctree::
   :titlesonly:

   config-ref/adapter-def
   config-ref/adapter-binding
   config-ref/container-def
   config-ref/ClusterCfgMetadataAPIConfig.properties
   config-ref/ClusterConfig
   config-ref/EngineConfig-properties
   config-ref/log4j2
   config-ref/message-def
   config-ref/MetadataAPIConfig-properties
   config-ref/MigrateConfig_template
   config-ref/modelCfg
   config-ref/tenant-def




.. _config-edit-install:

Edit the configuration files
============================

Two main configuration files control the Kamanja environment:

- :ref:`metadataapiconfig-config-ref` -- configures the
  :ref:`metadata<metadata-term>` objects
  (:ref`messages<messages-term>`, :ref`containers<containers-term>`
  :ref`models<model-term>`, :ref`functions<functions-term>` (UDFs),
  :ref`types<types-term>` - datatypes, and :ref`concepts<concepts-term>`)

- :ref:`clusterconfig-config-ref` files -- assigns a name to the cluster
  and configures all :ref:`tenants<tenancy-term>`,
  :ref:`ZooKeeper<zookeeper-term>`, Python, environment context,
  cache, all nodes in the cluster, and all adapters in the cluster.

Note that these files include all the configurations required
to run the :ref:`sample applications<run-samples-install>`
that are provided with the software.

Most configuration changes can be implemented
after you install Kamanja but you need to do the following
before proceding with the installation.
For more information about these parameters,
see the :ref:`clusterconfig-config-ref` reference page.

- Select the *ClusterConfig.json* file to use;
  different versions are provided
  to support different versions of :ref:`Kafka<kafka-term>`.
  For example, if you are using Kafka Version 10,
  you want to base your installation on the
  **ClusterConfig_kafka_v10.json** file.
- Copy the *ClusterConfig.json* file you want to use
  to a name such as *my-ClusterConfig.json*;
  you will specify this file when you install the cluster.
- Edit your copy of *ClusterConfig.json>* file:

  - [Old docs say: "{InstallDirectory}, found throughout the
    :ref:`MetadataAPIConfig.properties<metadataapiconfig-config-ref>`
    and :ref:`ClusterConfig.json<clusterconfig-config-ref>` configuration files,
    must be modified to show the directory where Kamanja will be installed.
    The location must be the same on all nodes in the custer."  But I
    don't find this string in the file]
  - Modify the value of the **ClusterID** parameter
    to the name you want to use for your cluster.
  - Set the **SystemCatalog/StoreType** attribute
    to the database type you want to use.
    You may need to modify the **SchemaName, **Location**, and **portnumber**
    attributes in **SystemCatalog** to support the selected database type.
  - Set a different **user** and **password** for the **StoreType**.
  - Define all the nodes that are to be configured in the cluster.
    See :ref:`failover-install` for notes about how to configure nodes
    to support the node :ref:`failover<failover-nodes-term>` feature.

[What about modifying the **Threads** parameters for the cluster?]

.. _failover-install:

Configuring failover
--------------------

If you are configuring :ref:`failover<failover-nodes-term>` for the cluster
make the following configuration changes before installing
the Kamanja software:

- Define all nodes -- what is the "Roles" value for replicas?
- et cetera

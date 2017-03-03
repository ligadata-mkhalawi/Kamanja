
.. _config-edit-install:

Edit the configuration files
============================

Two configuration files must be edited before you install
the multi-node cluster.
These are:

- :ref:`clustercfgmetadataapiconfig-config-ref`,
  (located in the *ClusterInstall* folder)
  which defines the values that are populated in the
  :ref:`metadataapiconfig-config-ref` file
  to define the :ref:`metadata<metadata-term>` objects
  (:ref:`messages<messages-term>`, :ref:`containers<container-term>`
  :ref:`models<model-term>`, :ref:`functions<functions-term>` (UDFs),
  :ref:`types<types-term>`, and :ref:`concepts<concepts-term>`)

- :ref:`clusterconfig-config-ref` files
  (located in the */config* directory)
  which assigns a name to the cluster
  and configures all :ref:`nodes<node-term>`,
  :ref:`adapters<adapter-term>` in the cluster
  and :ref:`tenants<tenancy-term>` to be created,
  as well as configuring :ref:`ZooKeeper<zookeeper-term>`,
  Python, environment context, and the cache.

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
- Edit these files to reflect the cluster to be installed:

  - Edit the paths found throughout both files
    to show the directory where each component will be installed.
    The location must be the same on all nodes in the custer.
  - Modify the value of the **ClusterID** parameter
    to the name you want to use for your cluster.
  - Set the **SystemCatalog/StoreType** attribute
    to the database type you want to use.
    You may need to modify the **SchemaName, **Location**, and **portnumber**
    attributes in **SystemCatalog** to support the selected database type.
  - Set a different **user** and **password** for the **StoreType**.
  - Define all the nodes that are to be configured in the cluster.

For samples of these files, see:

- :ref:`ClusterConfig.json, Example 1<clusterconfig-ex1>`
- :ref:`ClusterCfgMetadataAPIConfig.properties, Example 1<clustercfgmetadataapiproperties-ex1>`

.. _failover-install:

Configuring failover
--------------------

If you are configuring :ref:`failover<failover-nodes-term>` for the cluster
make the following configuration changes before installing
the Kamanja software:

- Define all nodes -- what is the "Roles" value for replicas?
- et cetera

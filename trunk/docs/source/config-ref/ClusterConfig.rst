
.. _clusterconfig-config-ref:

ClusterConfig.json
==================

The */config/ClusterConfig.json* file configures
all objects in a :ref:`cluster<cluster-term>`,
including :ref:`nodes<node-term>`,
input and output :ref:`adapters<adapter-term>`.
:ref:`tenants<tenancy-term>`,
and :ref:`Zookeeper<zookeeper-term>`.
To increase the compute power of the Kamanja environment,
you can add new nodes to a cluster
or you can add additional clusters.

Because of the complexity of the file,
some sections are documented on separate reference pages:

- :ref:`tenant-def-config-ref`
- :ref:`adapter-def-config-ref`
- :ref:`message-def-config-ref`
- :ref:`container-def-config-ref`

File structure
--------------

::

    "Clusters": [
      {
        "ClusterId": "cluster1",
        "GlobalReaderThreads":2,
        "GlobalProcessThreads":8,
        "LogicalPartitions":8192,
        "GlobalLogicalPartitionCachePort":7700,
        "SystemCatalog": {
           "StoreType": "h2db",
           "connectionMode": "embedded",
           "SchemaName": "kamanja",
           "Location": "/home/flare/Binaries/Kamanja911/Kamanja-1.5.3_2.11/storage/syscatalog",
           "portnumber": "9100",
           "user": "test",
           "password": "test"
        "Tenants": [
            ... {some config}
        ]
        ZooKeeperInfo": {
           "ZooKeeperNodeBasePath": "/kamanja",
           "ZooKeeperConnectString": "localhost:2181",
           "ZooKeeperSessionTimeoutMs": "30000",
           "ZooKeeperConnectionTimeoutMs": "30000"
        },
        "EnvironmentContext": {
           "classname": "com.ligadata.SimpleEnvContextImpl.SimpleEnvContextImpl$",
           "jarname": "KamanjaInternalDeps_2.11-1.5.3.jar",
           "dependencyjars": [
             "ExtDependencyLibs_2.11-1.5.3.jar",
             "ExtDependencyLibs2_2.11-1.5.3.jar"
        ]
        },
        "Cache": {
           "CacheStartPort": 7800,
           "CacheSizePerNodeInMB": 256,
           "ReplicateFactor": 1,
           "TimeToIdleSeconds": 31622400,
           "EvictionPolicy": "LFU"
        },
        "Nodes": [
        {
           "NodeId": "1",
           "ReaderThreads":2,
           "ProcessThreads":8,
           "LogicalPartitionCachePort":7700,
           "NodePort": 6541,
           "NodeIpAddr": "localhost",
           "JarPaths": [
             "/home/flare/Binaries/Kamanja911/Kamanja-1.5.3_2.11/lib/system",
             "/home/flare/Binaries/Kamanja911/Kamanja-1.5.3_2.11/lib/application"
           ],
           "Scala_home": "/usr",
           "Java_home": "/opt/jdk1.8.0_91",
           "Roles": [
             "RestAPI",
             "ProcessingEngine"
           ],
           "Classpath": ".:/home/flare/Binaries/Kamanja911/Kamanja-1.5.3_2.11/lib/system/ExtDependencyLibs_2.11-1.5.3.jar:/home/flare/Binaries/Kamanja911/Kamanja-1.5.3_2.11/lib/system/KamanjaInternalDeps_2.11-1.5.3.jar:/home/flare/Binaries/Kamanja911/Kamanja-1.5.3_2.11/lib/system/ExtDependencyLibs2_2.11-1.5.3.jar"
         }
       ],
        "Adapters": [
            ... see :ref:`adapter-def-config-ref`
        ],
        ... {some config}
      }
    ],
        "VelocityStatsInfo": {
			"RotationTimeInSecs": 120,
			"EmitTimeInSecs": 30
		}
      }
    ]
  }

Parameters
----------

- **ClusterID**: "<string>" -- unique identifier of this cluster.
- **GlobalReaderThreads** -
- **GlobalProcessThreads** -
- **LogicalPartitions** -
- **GlobalLogicalPartitionCachePort** -
- **SystemCatalog** - The System catalog is used during start-up and runtime
  to track information such as transaction IDs and Kafka offsets.
  The following fields must be configured.

  - **StoreType** - type of database to use for the System Catalog.
    Valid values are h2db, hbase, and cassandra.
    HBase is used for noSQL and is recommended for clustered configurations.
  - **SchemaName** - schema under which all tables concerning
    the metadata are held.
    In the case of Cassandra, it looks for a keyspace for the tables.
    In the case of HBase, it looks for a namespace.
  - **Location** - location of the database.
    For Cassandra and HBase, this should be the name of the server(s)
    where these databases exist.
  - **portnumber** - port used to access the database.
  - **user** - user ID for access to this cluster
  - **password** - password used to access this cluster

ZooKeeper parameters
~~~~~~~~~~~~~~~~~~~~

These parameters define how :ref:`ZooKeeper<zookeeper-term>`
is configured for the cluster.

- **ZooKeeperNodeBasePath** - node path (`zpath
  <http://zookeeper.apache.org/doc/r3.1.2/zookeeperProgrammers.html#sc_zkDataModel_znodes>`_)
  on which all runtime notification
  and configuration information is kept for use by Kamanja and MetadataAPI.
- **ZooKeeperConnectString** - server:port on which to connect to ZooKeeper.
  2181 is ZooKeeper’s default port.
- **ZooKeeperSessionTimeoutMs** - session timeout
- **ZooKeeperConnectionTimeoutMS** - time in milliseconds a Kamanja node
  should spend attempting to connect to ZooKeeper

Environment Context parameters
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Environment Context controls the flow of messages processed,
metadata allocated, and communication between models
in the execution environment and external components such as databases.

- **classname** - class that should be instantiated;
  this contains the logic required to access the database
  (or whatever storage used).
  This should be the full package.className$.
- **jarname** - name of the JAR in which the aforementioned ClassName exists.
  This JAR should exist in the JarPaths configured in the Nodes configuration.
- **dependencyjars** - list of JARs which
  the JAR mentioned in JarName requires to run properly.

Cache parameters
~~~~~~~~~~~~~~~~

- **CacheStartPort** - port used by JGroups
  to replicate and distribute Encached data over TCP.
- **CacheSizePerNodeInMB** -
- **ReplicateFactor** -
- **TimeToIdleSeconds** -
- **EvictionPolicy** -

Node parameters
~~~~~~~~~~~~~~~

The Node section of the file
controls the configuration of individual Kamanja nodes.
For each Kamanja node to run, an additional Node definitionis needed here.
This configuration allows for two or more Kamanja nodes
to be running in this cluster.
Any Kamanja nodes that are not defined here will not start,
giving the following error message:
“NodeId <#> not found” or “NodeID <#> is already running”.

- **NodeId** - unique ID used to reference this node.
  When a Kamanja node runs, it uses the Node set in its
  :ref:`engineconfigproperties-config-ref` file
  to determine what its configuration is.
- **ReaderThreads** - 
- **ProcessThreads** -
- **LogicalPartitionCachePort** -
- **NodePort** - port on which the node listens.
- **NodeIpAddr** - IP address or hostname to which this node binds.
- **JarPaths** - directories the node searches for dependency jars.
  When a Kamanja node starts up or is notified of a change in metadata,
  any JARs that are currently missing
  are downloaded into one of these two folders (usually the first one listed).
- **Scala_home** - home directory of Scala
- **Java_home** - home directory of Java
- **Roles** - Not currently used

  - **RESTAPI** -
  - **ProcessingEngine** -

- **Classpath** - default class path used by this node.

.. _velmetr-clustconfig:

Velocity Metrics parameters
~~~~~~~~~~~~~~~~~~~~~~~~~~~

To enable the :ref:`Velocity Metrics<velocity-metrics-term>` feature,
you must add the **VelocityStatsInfo** attribute with
these parameters to your *ClusterConfig.json* file:

- **RotationTimeInSecs** - Resets the accumulated values
- **EmitTimeInSecs** - Emit the accumulated metrics 

See:

- The :ref:`kamanjavelocitymetrics-msg-ref` page
  for an example *ClusterConfig.json* file that implements Velocity Matrics.
- :ref:`velocity-metrics-term` for a full list of the steps
  required to implement the Velocity Matrics feature.

Usage
-----


See also
--------

:ref:`kamanja<kamanja-command-ref>`



.. _clusterconfig-config-ref:

ClusterConfig.json
==================

The */config/ClusterConfig.json* file configures all objects in a cluster,
including input and output adapters.

File structure -- Clusters section
----------------------------------

::

    "Clusters": [
      {
        "ClusterId": "cluster1",
        ... {some config}
        "Adapters": [
          ... {some config}
          {
            "Name": "NEW_ADAPTER_NAME",
            "TypeString": "Input",
            "TenantId": "tenant1",
            "ClassName": "com.ligadata.InputAdapters.KafkaSimpleConsumer$",
            "JarName": "KamanjaInternalDeps_2.11-1.4.0.jar",
            "DependencyJars": [
              "ExtDependencyLibs_2.11-1.4.0.jar",
              "ExtDependencyLibs2_2.11-1.4.0.jar"
            ],
            "AdapterSpecificCfg": {
              "HostList": "localhost:9092",
              "TopicName": "testin_1"
            }
          },
          .... {some config}
        ],
        ... {some config}
      }
    ]
  }

Parameters
----------

- **ClusterId** -

- **Adapters**

  - **Name** - (Required)  

  - **TypeString** - (Required) Type of this adapter.
    Valid values are **input**, **Output**, and **Storage**.

  - **TenantId** -

  - **ClassName** -

  - **DependencyJars**

  - **AdapterSPecificCfg**

    - **HostList** - IP address and port number

    - **TopicName** -

Usage
-----

To add a new adapter to the cluster, create or modify a JSON file.
To add an object, add a new ADAPTER object to the configuration file
and submit it to the metadata using
the :ref:`kamanja<kamanja-command-ref>` upload cluster config command.

To update an existing object, update an existing property;
if the adapter object already exists in the system,
then uploading a cluster configuration results in an update operation.

An example command to add or update the cluster config:

::

    kamanja upload cluster config /tmp/kamanjaInstall/cong/ClusterConfig.json

To remove an object (in this case an input adapter),
upload the file with a desired object using
the :ref:`kamanja<kamanja-command-ref>` remove engine config command.
Any objects present in the JSON dcoument are removed.

Example for removing configuration objects:

::

    kamanja remove engine config /tmp/kamanjaInstall/cong/objectsToRemove.json



File structure -- Tenants
-------------------------

::

  "Tenants":[
  {
  "TenantId":"tenant1",
  "Description":"tenant1",
  "PrimaryDataStore":{
  "StoreType":"hbase",
  "SchemaName":"tenant1_default",
  "Location":"localhost",
  "authentication":"kerberos",
  "regionserver_principal":"hbase/_HOST@INTRANET.LIGADATA.COM",
  "master_principal":"hbase/_HOST@INTRANET.LIGADATA.COM",
  "principal":"ligadata@INTRANET.LIGADATA.COM",
  "keytab":"/home/ligadata/keytab/ligadata.keytab"
  },
  "CacheConfig":{
  "MaxSizeInMB":256
  }
  }
  ],

Usage -- Tenants
----------------

This means that, for each tenant, a TenantId must be specified,
along with its PrimaryDataStore, using the StoreType, SchemaName,
and Location described below.

- StoreType – indicates the type of database to use.
  Valid options are Cassandra and HBase for noSQL options
  (recommended for cluster configurations).
- SchemaName – indicates the schema under which all tables
  concerning the metadata is held.
  In the case of Cassandra, it searches for a keyspace for the tables.
  In the case of HBase, it searches for a namespace.
- Location – indicates the location of the database.
  For Cassandra and HBase, Location should be the name
  of the server(s) where these databases exist.

Each adapter specifies a TenantID.

See also
--------

:ref:`kamanja<kamanja-command-ref>`


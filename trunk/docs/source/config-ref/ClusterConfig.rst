
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


Usage
-----


See also
--------

:ref:`kamanja<kamanja-command-ref>`


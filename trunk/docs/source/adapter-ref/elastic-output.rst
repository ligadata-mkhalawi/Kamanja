
.. _elastic-output-adapter-ref:

Elasticsearch Output Adapter
============================

The ElasticSearch Output Adapter is used
to export the JSON data from a Kamanja output adapter
to the :ref:`ElasticSearch<elasticsearch-term>` engine.



ElasticSearch Output Adapter structure
--------------------------------------

::

  {
      "Name": "TestOut_1",
      "TypeString": "Output",
      "TenantId": "tenant1",
      "ClassName": "com.ligadata.ElasticsearchInputOutputAdapters.ElasticsearchProducer$",
      "JarName": "elasticsearchinputoutputadapters_2.11-1.6.1.jar",
      "DependencyJars": [
        "guava-19.0.jar", "elasticsearch-2.3.5.jar", "shield-2.3.5.jar"
      ],
      "AdapterSpecificCfg": {
        "HostList": "localhost:9300",
        "StoreType": "elasticsearch",
        "SchemaName": "testdata",
        "Location": "localhost",
        "clusterName":"elasticsearch",
        "portnumber": "9300",
        "tableName": "pokuritest",
        "PrependJarsBeforeSystemJars": "true",
        "DelayedPackagesToResolve": ["com.google.common.", "com.google.thirdparty."]
      }
    }

Parameters
----------

The - :ref:`adapter-def-config-ref` page defines the parameters
used in all adapter definitions.


Usage
-----


See also
--------

- :ref:`adapter-binding-config-ref`
- :ref:`adapter-def-config-ref`


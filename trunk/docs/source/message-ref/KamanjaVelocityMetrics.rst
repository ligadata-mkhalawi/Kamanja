
.. _kamanjavelocitymetrics-msg-ref:

KamanjaVelocityMetrics
======================

Message and container definitions for implementing
:ref:`velocity metrics<velocity-metrics-term>`.

Message definition
------------------

::

  {
    "Message": {
      "NameSpace": "com.ligadata.KamanjaBase",
      "Name": "KamanjaVelocityMetrics",
      "Version": "00.00.01",
      "Description": "Kamanja Velocity Metrics Message",
      "Fixed": "true",
      "Fields": [
        {
          "Name": "UUID",
          "Type": "System.String"
        },
        {
          "Name": "ComponentKey",
          "Type": "System.String"
        },
        {
          "Name": "NodeId",
          "Type": "System.String"
        },
        {
          "Name": "ComponentKeyMetrics",
          "Type": "com.ligadata.KamanjaBase.ArrayOfComponentKeyMetrics"
        }
      ]
    }
  }


For a description of the fields used,
see :ref:`message-def-config-ref`.
Note that the **ComponentKeyMetrics** container (shown below)
is nested in the **Fields** attribute.


ComponentKeyMetrics container definition
----------------------------------------

::

  {
    "Container": {
      "NameSpace": "com.ligadata.KamanjaBase",
      "Name": "ComponentKeyMetrics",
      "Version": "00.00.01",
      "Description": "Kamanja Velocity Metrics Component Key Metrics",
      "Fixed": "true",
      "Fields": [
       {
          "Name": "Key",
          "Type": "System.String"
        },
        {
          "Name": "MetricsTime",
          "Type": "System.Long"
        },
	  {
          "Name": "RoundIntervalTimeInSec",
          "Type": "System.Int"
        },
        {
          "Name": "FirstOccured",
          "Type": "System.Long"
        },
        {
          "Name": "LastOccured",
          "Type": "System.Long"
        },      
        {
          "Name": "MetricsValue",
          "Type": "com.ligadata.KamanjaBase.ArrayOfMetricsValue"
        }
      ]
    }
  }


Note that the **MetricsValue** container (shown below)
is nested in the **Fields** attribute.

MetricsValue container definition
---------------------------------

::

  {
    "Container": {
      "NameSpace": "com.ligadata.KamanjaBase",
      "Name": "MetricsValue",
      "Version": "00.00.01",
      "Description": "Kamanja Velocity Metrics Component Key Metrics",
      "Fixed": "true",
      "Fields": [
        {
          "Name": "MetricKey",
          "Type": "System.String"
        },
        {
          "Name": "MetricsKeyValue",
          "Type": "System.Long"
        }
      ]
    }
  }



For a description of the fields used,
see :ref:`container-def-config-ref`.

Usage
-----

To implement Velocity Metrics for your application,
you must create an :ref:adapter binding<adapter-binding-config-ref>`
that binds this message to the adapter and serializer.
See example below.

You must also implement Velocity Metrics in your
:ref:`clusterconfig-config-ref` file
and in each input and output :ref:`adapter<adapter-def-config-ref>`
you are using;
see :ref:`velocity-metrics-term` for a list of all steps.

Example
-------

ClusterConfig.json file with VelocityMetrics enabled
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This is a sample :ref:`clusterconfig-config-ref` file
with Velocity Metrics enabled.

::

  ClusterConfig- Adapter example:
	  {
	    "Name": "HelloWorldInput",
	    "TypeString": "Input",
	    "TenantId": "tenant1",
	    "ClassName": "com.ligadata.kafkaInputOutputAdapters_v10.KamanjaKafkaConsumer$",
	    "JarName": "kamanjakafkaadapters_0_10_2.11-1.6.1.jar",
	    "DependencyJars": [
		  "kafka-clients-0.10.0.1.jar",
		  "KamanjaInternalDeps_2.11-1.6.1.jar",
		  "ExtDependencyLibs_2.11-1.6.1.jar",
		  "ExtDependencyLibs2_2.11-1.6.1.jar"
	    ],
	    "AdapterSpecificCfg": {
		  "HostList": "localhost:9092",
		  "TopicName": "helloworldinput"
	    },
	    "VelocityMetrics": [
		  {
		    "MetricsByFileName": {
			  "TimeIntervalInSecs": 5
		    }
		  },
		  {
		    "MetricsByFileName": {
			  "TimeIntervalInSecs": 30,
			  "MetricsTime": {
			    "MetricsTimeType": "LocalTime"
			  }
		    }
		  },
		  {
		    "MetricsByMsgType": {
			  "ValidMsgTypes": [
			    "com.ligadata.kamanja.samples.messages.msg1"
			  ],
			  "TimeIntervalInSecs": 1,
			  "MetricsTime": {
			     "MetricsTimeType": "LocalTime"
			  }
		    }
		  },
		  {
		  "MetricsByMsgKeys": {
			  "ValidMsgTypes":[
			    "com.ligadata.kamanja.samples.messages.msg1"
			  ],
			  "Keys": [
				  "id"				
			  ],
			  "TimeIntervalInSecs": 1
		    }
		  }
	    ]
	  }


Adapter binding for Velocity Metrics
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Here is a sample :ref:`adapter binding<adapter-binding-config-ref>`
with Velocity Metrics enabled:

::

  {
      "AdapterName": "VelocityMetrics",
      "MessageNames": [
        "com.ligadata.KamanjaBase.kamanjavelocitymetrics"
      ],
      "Serializer": "com.ligadata.kamanja.serializer.jsonserdeser",
      "Options": {
       }
    }

Sample Velocity Metrics json message from Kafka topic
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  {
    "uuid": "d5eb7b1b-919a-46b2-bd41-cf33eb968f99",
    "componentkey": "kafkaproduceroa",
    "nodeid": "1",
    "componentkeymetrics": [
      {
        "key": "metricsbymsgkeys_lowbalancealertoutputmsg_3",
        "metricstime": 1485981810000,
        "roundintervaltimeinsec": 30,
        "firstoccured": 1485981810407,
        "lastoccured": 1485981810523,
        "metricsvalue": [
          {
            "metrickey": "processed",
            "metricskeyvalue": 5
          },
          {
            "metrickey": "exception",
            "metricskeyvalue": 0
          }
        ]
      },
      {
        "key": "metricsbymsgtype_lowbalancealertoutputmsg",
        "metricstime": 1485981810000,
        "roundintervaltimeinsec": 30,
        "firstoccured": 1485981810376,
        "lastoccured": 1485981810586,
        "metricsvalue": [
          {
            "metrickey": "processed",
            "metricskeyvalue": 45
          },
          {
            "metrickey": "exception",
            "metricskeyvalue": 0
          }
        ]
      },
      {
        "key": "metricsbymsgkeys_lowbalancealertoutputmsg_9",
        "metricstime": 1485981810000,
        "roundintervaltimeinsec": 30,
        "firstoccured": 1485981810547,
        "lastoccured": 1485981810586,
        "metricsvalue": [
          {
            "metrickey": "processed",
            "metricskeyvalue": 5
          },
          {
            "metrickey": "exception",
            "metricskeyvalue": 0
          }
        ]
      }
    ]
  }


See also
--------

- :ref:`velocity-metrics-term` describes all the steps
  required to implement Velocity Metrics for an application.





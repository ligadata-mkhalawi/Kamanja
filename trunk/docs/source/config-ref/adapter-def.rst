

.. _adapter-def-config-ref:

Adapter definition
==================

All :ref:`adapter<adapter-term>` objects
used in the cluster are defined in the
:ref:`ClusterConfig.json<clusterconfig-config-ref>` JSON file.
Each input, output, or storage adapter used in the cluster
has its own "Adapter" section,
identified by a unique "Name".

To implement a custom adapter:

- Code the adapter
- Create a definition for the adapter
- Submit the adapter to the metadata using the
  :ref:`kamanja<kamanja-command-ref>` command.

Adapter definitions include some core parameters
that are used for all adapter types
plus some parameters that are specific to each adapter type.

File structure
--------------

::

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




Output sdapter file structure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


Storage adapter file structure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Parameters
----------

Input adapter parameters
~~~~~~~~~~~~~~~~~~~~~~~~

- **Name** – name of the adapter.

- **TypeString** - (Required) Type of this adapter.
   Valid values are **input**, **Output**, and **Storage**.

- **TenantId** - ID of the :ref:`tenant<tenancy-term>` used for this adapter;
  see :ref:`tenant-def-config-ref`

- **ClassName** -

- **DependencyJars**

- **AdapterSPecificCfg**

  - **HostList** - IP address and port number

  - **TopicName** -



Output adapter parameters
~~~~~~~~~~~~~~~~~~~~~~~~~


- **NameSpace** – namespace of the output adapter.
- **Name** – name of the output adapter.


Storage adapter parameters
~~~~~~~~~~~~~~~~~~~~~~~~~~


Usage
-----

To add a new adapter object to the cluster:

-
- add a new ADAPTER object to the ClusterConfig.json configuration file
- submit it to the metadata using
  the :ref:`kamanja<kamanja-command-ref>` upload cluster config command.
  For example:

  ::

      kamanja upload cluster config /tmp/kamanjaInstall/cong/ClusterConfig.json

To update an existing object, update an existing property;
if the adapter object already exists in the system,
then uploading a cluster configuration results in an update operation.

To remove an object (in this case an input adapter),
upload the file with the desired object using
the :ref:`kamanja<kamanja-command-ref>` remove engine config command.
For example:

::

    kamanja remove engine config /tmp/kamanjaInstall/cong/objectsToRemove.json


Any objects present in the JSON dcoument are removed.

If the input adapter definition contains an AssociatedMessage, 
it is called tagged. 
So if the input adapters contain tagged messages, 
add new messages and/or JTMs as appropriate. 
Refer to the JTMs for more information.


Input adapters
~~~~~~~~~~~~~~

Output adapters
~~~~~~~~~~~~~~~

Storage adapters
~~~~~~~~~~~~~~~~


Examples
--------



See also
--------

- :ref:`adapters-input-guide`
- :ref:`adapters-output-guide`
- :ref:`adapters-storage-guide`





.. _metadata-term:

Metadata
--------

Kamanja metadata includes all the information that the
:ref:`engine<engine-term>` needs
to process incoming :ref:`messages<messages-term>`.
All metadata objects in a :ref:`cluster<cluster-term>`
run on all :ref:`nodes<node-term>` in the cluster.

The metadata objects are

- :ref:`messages<messages-term>`

  - input messages  - data flowing into Kamanja engine
  - output messages - data flowing out of models

- :ref:`containers<container-term>`
- :ref:`models<model-term>`
- :ref:`functions<functions-term>`
- :ref:`types<types-term>`
- :ref:`concepts<concepts-term>`

The :ref:`MetadataAPIConfig.properties<metadataapiconfig-config-ref>` file
is a JSON file that defines all metadata for the cluster.
The JSON representations are used to generate Scala code
that is stored as the metadata datastore
and is used to load the metadata into memory
when then engine is initialized.


For more information:

- :ref:`MetadataAPIConfig.properties<metadataapiconfig-config-ref>`

- The :ref:`kamanja-command-ref` is used to add, delete, update, and query
  metadata objects in the cluster.

- :ref:`metadata-mgr-term`



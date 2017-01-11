
.. _adapter-term:

Adapter
-------

Adapters are plugins that allow Kamanja to communicate data with external,
third-party systems.
There are two general types of adapters:

- input/output adapters for reading and writing messages
  (that is, record structures) with systems such as Kafka or MQ.
- storage adapters for saving data.
  Kamanja is packaged with several storage adapters 
  including Cassandra, Hbase, and Microsoft SQL server.

For more information:

- :ref:`adapters-input-guide` provides details about how to implement
  input adapters.
- :ref:`adapters-output-guide` provides details about how to implement
  output adapters.
- :ref:`adapters-storage-guide` provides details about how to implement
  storage adapters.

- Adapters are defined in
  the :ref:`ClusterConfig<clusterconfig-config-ref>` file.

- For information about specifying adapter-specific initialization,
  see :ref:`audit-adapter-term`.



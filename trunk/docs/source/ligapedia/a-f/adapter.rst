
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
  including :ref:`Cassandra<cassandra-term>`,
  :ref:`Hbase<hbase-term>`, and Microsoft SQL server.

An adapters is associated with a :ref:`message<messages-term>`
and a :ref:`serializer/deserializer<serial-deserial-term>`
by an adapter binding that is defined in
an :ref:`adapter message binding definition<adapter-binding-config-ref>`.

For more information:

- :ref:`adapters-input-guide` provides details about how to implement
  input adapters.
- :ref:`adapters-output-guide` provides details about how to implement
  output adapters.
- :ref:`adapters-storage-guide` provides details about how to implement
  storage adapters.
- :ref:`adapter-ref-top` lists adapters that are included in Kamanja.

- Adapters are defined in
  the :ref:`adapter definitions<adapter-def-config-ref>` section
  of the :ref:`ClusterConfig<clusterconfig-config-ref>` file.

- For information about specifying adapter-specific initialization,
  see :ref:`audit-adapter-term`.



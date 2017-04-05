
.. _serial-deserial-term:

Serializers and deserializers
-----------------------------


Serialization is the process of converting data objects and structures
into a stream of bytes or other format
that can be easily stored or transmitted.
Deserialization is the process of taking the stream of bytes
and reconstructing a semantically equivalent clone of the original structure.

Kamanja currently provides three built-in serializers:

.. list-table::
   :class: ld-wrap-fixed-table
   :widths: 15 50 35
   :header-rows: 1

   * - JSON
     - com.ligadata.kamanja.serializer.jsonserdeser
     - supports :ref:`JSON<json-term>` messages
   * - CSV or Delimited
     - com.ligadata.kamanja.serializer.csvserdeser
     - supports :ref:`CSV<csv-term>` messages
   * - KBinary
     -
     - supports the :ref:`KBinary<kbinary-term>` binary format
       that is designed principally for
       the storage representation of message data.

The serialization system to use is defined in the
:ref:`adapter message binding<adapter-binding-config-ref>` definition.

In Kamanja 1.6.3 and later releases,
the :ref:`smart-output-config-ref` supports
:ref:`Avro<avro-term>` compression (with or without the
`snappy <https://avro.apache.org/docs/1.8.1/spec.html#snappy>`_ codec).


For more information, see:

- :ref:`serial-deserial-adapters-guide`



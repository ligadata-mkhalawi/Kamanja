
.. _serial-deserial-term:

Serializers and deserializers
-----------------------------


Serialization is the process of converting data objects and structures
into a stream of bytes or other format
that can be easily stored or transmitted.
Deserialization is the process of taking the stream of bytes
and reconstructing a semantically equivalent clone of the original structure.
Kamanja supports some of the common message transport formats
such as JSON and CSV;
support for other formats such as AVRO are under development.

Kamanja also offers a binary format called :ref:`KBinary<kbinary-term>`
that is designed principally for the storage representation of message data.

For more information, see:

- :ref:`serial-deserial-adapters-guide`



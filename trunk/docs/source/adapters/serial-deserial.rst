
.. _serial-deserial-adapters-guide:

Serialization and Deserialization
=================================

Serialization is the process of converting data objects and structures
into a stream of bytes or other format
that can be easily stored or transmitted.
Deserialization is the process of taking the stream of bytes
and reconstructing a semantically equivalent clone of the original structure.

Kamanja supports some of the common message transport formats
such as JSON and CSV; support for other formats
such as :ref:`AVRO<avro-term>` are under development.
Kamanja also offers a binary format
designed principally for the storage representation of message data.

With this release, only the JSON, CSV, and KBinary serializers are available.
These are built-in to the Kamanja platform.
In a future sprint, not only will the AVRO format be supported,
but it will be possible to add a custom serialization to the platform
so that messages can be formatted and encoded to meet business requirements.

In the next section is a description of the serializers available
in this release.

.. _kbinary-guide:

Built-in Kamanja Serialization and Deserialization
--------------------------------------------------

The following serialization encodings are built into Kamanja
and available for adapter use by simply referencing their names
in the :ref:`adapter<adapter-term>` configuration.
See :ref:`message bindings<message-bindings-guide>`.

::

  KBinary (metadata name: com.ligadata.kamanja.serializer.kbinaryserdeser)

The KBinary encoding is used by Kamanja internally
to store message and container content
in the KV storage in use on the cluster.
If desired, this same encoding implementation
can be used for adapters that the customer might use
to integrate their message flows with Kamanja.
Similarly, there is no reason the internal storage serialization scheme
cannot be customized to suit a developer’s goals.
Like custom serialization and custom adapters,
the standard Kamanja serialization and adapters
are added to the metadata like any one contributed.

Java DataInputStream and DataOutputStream are used.
Nested messages, arrays, the standard scalars, strings, etc
are all managed, including arrays and maps of these types.

Both :ref:`mapped<messages-mapped-term>`
and fixed messages are supported.
In the case of the mapped messages,
often used for sparse data situations,
only the fields in the message with valid data
are written to the serialization stream.
Similarly, the KBinary does not require
that all fields are present in the input stream
that it uses to deserialize a message or container object.

For fixed messages, the expectation is that
all fields described in the message have a valid value.
This may be relaxed in a future release
when default value specifications are added to the message declarations.

::

  JSON (metadata name: com.ligadata.kamanja.serializer.jsonserdeser)

The JSON encoder supports both the standard fixed message
as well as the mapped message used in sparse data situations.
Like Kbinary, not all fields are required to be present
in a stream being ingested by the deserializer.

The keys of a JSON message are the message field names
that were used in the message declaration cataloged.
The values can be the standard scalars, Boolean, string,
message declarations, as well as arrays and maps of these.

::

  CSV (metadata name: com.ligadata.kamanja.serializer.csvserdeser)

Comma-separated values encoding is supported
for Kamanja fixed messages only with no message or container fields.
Because of the constraint placed on CSV
that all records have the same fields in the same order,
sparse data applications represented
by the mapped messages are not supported.

CSV can be configured to in several ways:

::

  lineDelimiter (default “rn”)
  field delimiter (default “,”)
  produce header (default false)
  always quote fields (default false)
  valSeparator when an array is used as a field.
           (This value can be used to separate the array items).

Array fields can be supported
by the CSV serializer if a valSeparator is specified.
It cannot be the same value as the field separator
nor should it match any character sequence in any array item.




.. _avro-term:

Avro
----

Apach Avro is a popular data
:ref:`serialization<serial-deserial-term>` system
that specifies its format with Avro schemas
instead of inline type tags.
Its capabilities include rich data structures
(maps, arrays, and a compact binary data format
that plays well in clustered (multi-machine) environments
such as Kamanja).

The :ref:`smart-output-config-ref` supports Avro compression
(with or without the
`snappy <https://avro.apache.org/docs/1.8.1/spec.html#snappy>`_ codec)
in Kamanja 1.6.3 and later releases.

See

- `Avro specification
  <https://avro.apache.org/docs/1.8.0/spec.html>`_



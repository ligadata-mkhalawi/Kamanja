
.. _message-bindings-term:

Message bindings
----------------

Adapter message bindings are independently cataloged
:ref:`metadata<metadata-term> objects
that associate three other metadata objects together:

- an adapter (input, output, or storage).
- a message that this adapter either deserializes
  from its source or serializes to send to its sink.
- the serializer to use that understands
  how to serialize and deserialize the message.

These bindings provide a flexible way to describe
the input, output, and storage streams
flowing to/from the adapter’s sink/source.
Being able to dial in the sort of serialization expected
or provided to others gives the Kamanja system administrator
a lot of flexibility to satisfy his/her objectives.

There are currently three builtin serializers provided
in the Kamanja distribution that can be used:
a JSON serializer, a CSV serializer, and a KBinary serializer.
The KBinary is used principally by the Kamanja platform
to manage data in its stores and caches.

The adapter message bindings can be ingested one at a time.
If there are multiple messages that are managed by an adapter
that all share the same serializer, a compact representation is possible.
It is also possible to organize
one or more adapter message binding specifications
in a file and have the Kamanja ingestion tools consume that.

Note that adapters, messages, and serializers
must already be cataloged in the metadata
before a user can add a binding for them.
A rather comprehensive list of errors is returned if that is not the case.

For more information, see:

- :ref:`Message Bindings Guide<message-binding-guide>`



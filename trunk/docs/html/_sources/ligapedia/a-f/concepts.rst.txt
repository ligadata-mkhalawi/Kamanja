
.. _concepts-term:

Concepts and Derived Concepts
-----------------------------

Concepts and derived concepts are global entities
that can be accessed with a key within the Kamanja engine.
There is no real distinction between a concept and derived concept;
there is either a concepts value or a derived concepts value.

The notion of a concept is borrowed from the Web Ontology Language (OWL).
It roughly describes a central idea
that has zero or more associated properties
that can decorate it and enrich its meanings.
A concept also has relationships with other related concepts.
All related concepts, their properties, relationships, constraints,
and other information of this type are used to form an ontology –
the description of some system found in the real world.
For Kamanja, a concept is an independently cataloged attribute/field
of an arbitrary type.
These can be referenced as fields in Kamanja
:ref:`messages<messages-term>` and :ref:`containers<container-term>`.


The concept and derived concept is a computed field.
For example, the models run and they accept messages.
They filter a message – they determine that this is a message of interest.
The model does computations on the message that, for example,
compare the message with a table.
It then creates some sort of an output and it can publish that output.
When a model is defined, both the inputs that the model needs
as well as the outputs that the model offers need to be described.
That’s part of the metadata.
Those outputs then can be referenced directly by other models in the system.


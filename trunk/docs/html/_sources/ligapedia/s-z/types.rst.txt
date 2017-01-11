
.. _types-term:

Types
-----

Kamanja has a rich type system similar to that of common programming languages
such as Scala and Java.
Scalars (numbers of various size), strings,
containers (these are implemented as classes with 0 or more fields),
messages (similar to containers but specifically used
for expressing incoming and outgoing messages),
and collections of these elements can all be used in a Kamanja PMML model.

Collection types closely model those found
in the Scala implementation language.
Currently supported collection types in Kamanja PMML models include
Array, ArrayBuffer, Set, ImmutableSet, Map, ImmutableMap,
SortedSet, TreeSet, and Queue.



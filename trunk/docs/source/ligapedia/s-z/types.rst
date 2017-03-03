
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

A Kamanja :ref:`message<messages-term>` or :ref:`container<container-term>`
can have fields with the following types:

- Int or System.int
- Long or System.long
- Float or System.float
- Double or System.double
- Boolean or System.boolean
- String or System.string
- Char or System.char
- ArrayOfString or System.ArrayOfString

In addition, all of these types can serve as a member type
of either array or map collections.

Kamanja Messages and Containers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Kamanja’s message (and container) compiler
not only produces type information for the message or container being added,
but also create array and map collection types automatically
that have these message and container types as their member element.

For maps, permissible key values are currently limited to String.


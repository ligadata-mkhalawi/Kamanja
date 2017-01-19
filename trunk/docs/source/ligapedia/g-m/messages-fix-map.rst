
.. _messages-fix-map-term:

Messages, fixed and mapped
--------------------------

Message definitions can be fixed or mapped. 

- Use fixed messages when all fields are available
  in the incoming data stream and they are presented to the model;
  they are represented as a Scala class instance at runtime.
- A mapped message represents the messages as a Scala map
  [String, <some data type>] at runtime).


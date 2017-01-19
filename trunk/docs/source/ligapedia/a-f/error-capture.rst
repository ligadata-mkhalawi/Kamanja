
.. _error-capture:

Error messages, capturing
-------------------------

To capture error messages, define an error capture message.
Each time an error occurs,
the message is then captured and transmitted
to the next model in the pipeline as an output message.

You can use a windowing technique to group messages from a datastream
and send them as a single output message.
To do this, capture the messages
in a local :ref:`container<container-term>` that the partition can access.
Code the model to wait
until the specified number of messages have been written to the container
and then send the container in an output message.



.. _python-arch:

How Kamanja Python Works
------------------------

Kamanja is a JVM-based real-time message processing cluster.
Most variants of Python cannot directly run on the JVM
(Jython being the exception).
Because Python is a powerful scripting language
that has highly optimized portions of its code in C and C++ libraries,
Kamanja effectively builds a parallel network of Python servers
where the Kamanja cluster nodes interact with IP communication mechanisms.

For every Kamanja cluster node,
one or more Kamanja Python servers are started
(as required by the node’s engine).
Models are added to a Python server
when a message arrives that requires processing
by the prior addition of the Python model.
The models are loaded into the Python servers
when the Python servers are started.
The messages are sent through the established connections
from the cluster to the Python server.
The server ignition and model addition are only done once.
This is a lazy approach to resource allocation.

The following diagram illustrates one Kamanja cluster node
(containing the PythonAdapter proxy models)
and the Python servers (loaded with models)
that are sent messages and respond with replies.

.. image :: /_images/Python-diagram.jpg

Kamanja is currently tested and supported for Python version 2.7.
Future releases include support for higher versions of Python
(Python 3 and above).



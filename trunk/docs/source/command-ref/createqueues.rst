

.. _createqueues-command-ref:

CreateQueues.sh
===============

Connect to local Kafka server
and create the *testin_1*, *testout_1*, and *teststatus_1*
queues for the three :ref:`adapter<adapter-term>` :ref:`topics<topic-term>`
that are configured by default when Kamanja is first
installed and initialized.

Syntax
------

::

  CreateQueues.sh

Options and arguments
---------------------

None

Usage
-----

When Kamanja is first installed and initialized,
it creates some default adapters and associates them with topics
that can be used for initial testing.
**CreateQueues.sh** creates the topics
that are associated with those adapters.


See also
--------

- :ref:`kafka-topics-command-ref` is used to create custom topics
  for an application.




.. _logging-consolidated:

Logging, consolidated
---------------------

The consolidated logging component creates a centralized queue
for all errors and warnings from all components
and all nodes in the cluster.
Kamanja uses :ref:`Log4J2<log4j2-term>` to log entries
from all Kamanja components.
If consolidated logging is not enabled,
these log entries are written to flat files.
The consolidated logging library allows for log entries
to instead be pushed to a Kafka topic from which they can be consumed.
This is implemented by setting KafkaAppender
in the :ref:`log4j2.xml<log4j2-config-ref>` file;
log events are then written to a Kafka topic.
Each log event is sent as a Kafka record.

The implementation is different for clusters
that do not run Kerberos and those that do.

Configuring consolidated logging for Kafka with no Kerberos
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To implement consolidated logging on clusters that do not run Kerberos:

#. Create the kafka topics for Logging Errors.
#. Add the Kafka Appender to the log4j2.xml file;
   see the sample on the :ref:`log4j2.xml<log4j2-config-ref>` page.
#. Include the kafka-clients.jar file
   in the class path of the :ref:`kamanja<kamanja-command-ref>` script.
#. Add the NodeID environment variable to the
   kamanja scripts.
#. Run Kamanja.
#. Check the errors that are pushed to Kafka topics.


Configuring consolidated logging for Kafka with Kerberos
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To implement consolidated logging on clusters that run Kerberos:

#. Create the kafka topics for Logging Errors.
#. Add the Kafka Appender to the log4j2.xml file;
   see the sample on the :ref:`log4j2.xml<log4j2-config-ref>` page.
#. Add the kafka-clients.jar file to the class path
   of the kamanja scripts.
#. Add the NodeID environment variable
   to the kamanja scripts.
#. Run Kamanja.
#. Check that the errors are pushed to Kafka topics.




.. _what-is-kamanja:

What is Kamanja
--------------- 

Kamanja is a continuous decisioning framework.
It is built natively on several open-source, Apache™ projects.
It comprises:

- a real-time computation engine
- ingestion streaming operators
- DAG rules as PMML models
- a console for interfaces
- search and resource management REST APIs and pre-built connectors
- out-of-the-box integration with Apache™ Hadoop®,
  data stores such as Apache HBase™ or Apache Cassandra™,
  and messaging systems such as Apache :ref:`Kafka<kafka-term>` or IBM® MQ.

The Kamanja framework is open source
with many features that support different use cases:

1. Built bottom-up in Scala, a concise, powerful, language.

2. Compiles predictive :ref:`models<model-term>`;
   saves as :ref:`PMML<pmml-term>` into
   :ref:`Scala<scala-term>` and JAR files,
   then saves into data store.
   Moves complex analytic models into production from weeks to days.

3. Performs real-time message processing (that is, from Kafka),
   real-time scoring with PMML models,
   and feeds outputs to application or messaging systems.
   It provides the benefits of both Apache Storm’s stateless,
   event streaming, and Apache Spark™ Streaming’s stateful,
   distributed processing.

4. Performance and Scale.
   Delivers 100% more message throughput than Storm
   with :ref:`DAG<dag-term>` execution of PMML models on a single node.
   Runs hundreds of models concurrently on the same node.

5. Runs event processing one-time only
   by enforcing concurrency and frequency.
   Eliminates errors and false alerts
   from running the same transactions at once
   or running transactions repeatedly.

6. Fault tolerant. Uses Kafka for messaging
   and Apache :ref:`ZooKeeper™<zookeeper-term>`
   to provide fault tolerance, processor isolation,
   cluster coordination service, and resource management.

7. Handles complex record routing in large parallelized implementations.

8. Finds and fixes problems fast with DevOps.
   Views service logs to troubleshoot.

9. Pluggable. Provides pluggable APIs
   that enable Kamanja to run with other messaging systems
   and execution environments.

10. Analytics. Kamanja exposes services and data for analytic,
    reporting, and scheduling tools.

11. Supports other high-level languages and abstractions
    for implementation (PMML, Cassandra, Hbase, DSL).

12. Quickly deploys on multi-node clusters.
    Builds, ships, and runs applications with minimal downtime.

13. Vibrant Community. Easy to deploy, provides test samples,
    developer guides, and community
    `forums <http://kamanja.org/forums/forum/kamanja-forums/>`_.
    to answer questions
    and allow users to make contributions.




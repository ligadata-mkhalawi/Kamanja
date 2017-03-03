
.. _what-is-kamanja:

What is Kamanja?
---------------- 

Kamanja is a high-performance real-time event processing engine.
The engine provides a run-time execution environment
to run programs to compute analytics and KPIs,
as well as models encapsulating machine learning methods.
It provides adapters to connect
to both synchronous and asynchronous data sources.
Kamanja is built ground-up as a distributed processing engine
that can be deployed in a cluster mode
to handle large data volumes and complex model computations.

Kamanja includes:

- Real-time computation engine
- Input, output, and storage adapters
- Support for Java, Python and Scala as model development languages
- Model run-time based on Directed Acyclic Graphs (DAG)
- Support for PMML 
- Metrics capture across all Kamanja components
- Support for serialization and deserialization of data
  using JSON, CSV, or KV formats
- Out-of-the-box support for cluster mode
  for a scalable and fault-tolerant processing environment
- search and resource management REST APIs and pre-built connectors
- Out-of-the-box integration with Apache™ Hadoop®
  and data stores such as Apache HBase™ or Apache Cassandra™
- Out-of-the-box integration with messaging systems
  such as Apache Kafka or IBM® MQ.

Kamanja is open sourced under the Apache™ license.

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




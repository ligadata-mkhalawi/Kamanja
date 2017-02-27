.. Kamanja documentation master file, created by
   sphinx-quickstart on Fri Dec  2 00:43:53 2016.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Welcome to Kamanja's documentation! (Under construction)
========================================================

Kamanja is a high-performance real-time event processing engine.
The engine provides a run-time execution environment
to run programs to compute analytics and KPIs,
as well as models encapsulating machine learning methods.
It provides adapters to connect
to both synchronous and asynchronous data sources.
Kamanja is built ground-up as a distributed processing engine
that can be deployed in a cluster mode
to handle large data volumes and complex model computations.

Kamanja is open sourced under the Apache™ license.  It includes:

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

For a more detailed description of Kamanja,
see :ref:`what-is-kamanja`.

How to use this documentation
-----------------------------

- :ref:`Ligapedia<ligapedia-top>` is a set of free-standing articles,
  arranged alphabetically, about terms and concepts
  related to using Kamanja.
  Articles tell you waht the term means
  and include references to other documents
  (within the Kamanja doc set and elsewhere)
  that give more information.
  Other documents often link to these articles
  when using specialized terms

- :ref:`install-top` gives instructions for installing
  a Kamanja cluster.  For demonstrations and development,
  you can install a single-node cluster;
  for production systems, you need to install a multi-node cluster.

- :ref:`models-top` describes the structure of Kamanja applications
  and how to create and implement
  the analytical :ref:`models<model-term>`,
  :ref:`messages<messages-term>`,
  and :ref:`containers<container-term>`
  that make up an application.

  Specialized guides are provided for implementing applications
  using :ref:`Java<java-guide-top>`,
  :ref:`Scala<scala-models-top>`,
  :ref:`PMML<pmml-guide-top>`
  (which is also used to implement trained :ref:`R<r-term>` models),
  :ref:`JTM<jtm-guide-top>`,
  and :ref:`Python<python-guide-top>`.

- :ref:`adapters-guide-top` gives instructions and examples
  of writing the adapters used to pull data into the Kamanja environment,
  export the results to another application,
  or store data in the Kamanja factory.

- :ref:`architecture-top` discusses the Kamanja architecture
  and components.

- **Reference pages** provide detailed technical reference material
  about the following:

  - :ref:`Configuration files<config-ref-top>`
  - :ref:`Commands<command-ref-top>`
  - :ref:`Messages<message-ref-top>` describes messages that are
    provided as part of the Kamanja software.
  - :ref:`API's<api-ref>`.  Full descriptive information is not yet
    available for the APIs; :ref:`legacy-api-ref` contains some useful
    information about some of the major API's.


Ligapedia
---------

.. toctree::

   ligapedia

Reference Pages
---------------

.. toctree::
   :caption: Reference Pages
   :maxdepth: 1

   config-ref
   command-ref
   adapter-ref
   message-ref

API Documentation
-----------------

.. toctree::
   :caption: API Documentation

       api
    legacy-api

Machine Learning Guides
-----------------------

.. toctree::
   :caption: Machine Learning Guides

   models
   java
   scala
   pmml
   jtm
   python
   adapters

Administration Guides
---------------------

.. toctree::
   :caption: Administration Guides

   install-plan
   admin

About this Product
------------------

.. toctree::
   :caption: About this Product

   architecture
   relnotes


Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`


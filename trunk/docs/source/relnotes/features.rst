
New features in Kamanja 1.6.2
=============================

Kamanja 1.6.2 includes a number of bug fixes
plus the following new features:

- Velocity metrics, which can be configured into your application
  to collect statistics about :ref:`message<messages-term>` processing.
  See :ref:`velocity-metrics-term` for more information.
- ElasticSearch adapter;
  see the :ref:`elasticsearch-term` LigaPedia article.
- Ability to output data to the :ref:`Parquet<parquet-term>` file format.
- Archiving functionality
  (ability to move source files
  to a different location on a different file system
  after they are processed)
  is now supported by the
  :ref:`smart-input-config-ref`.
- Addition of the :ref:`inputadaptersstatssg-msg-ref` message
  that can be specified to the **StatusMsgTypeName** parameter
  in the :ref:`smart-input-config-ref`
  to have the input adapter send a status message
  for each file it successfully processes.
- The **MsgTags** parameter in the :ref:`smart-input-config-ref`
  has been replaced by the **MsgTagsKV** parameter.
  The functionality is similar
  but now the information is sent to a map
  so that the tag also has a name
  which can be used to parse the tags part of the message (in models)
  then access the values by keys rather than order.
  The predefined tag $Msg_Start_Offset
  (representing the bite offset of the message in the input file)
  is also added.
- Additional configuration options for the
  :ref:`smart-output-config-ref`
  and the ability to consolidate files into configured maximum size.

In addition, the Kamanja documentation has been restructured
and converted to use the RST/Sphinx authoring tools.
We will be continuing to enhance the content of the documentation
over the next several releases,
but a number of new features are available now:

- PDFs are available for each title
  and for each subsection of each title
  as well as the entire doc set;
  click on the PDF icon in the upper right of each page
  to download the PDF for that title.
  Clicking the PDF icon on the documentation landing page
  gives you a PDF for the entire documentation set.
- The documentation source is now included in the kamanja github repository.
  We welcome contributions to the documentation
  as well as the software;
  see the Kamanja `Contribute <http://kamanja.org/contribute/>`_ web page.
- :ref:`LigaPedia<ligapedia-top>` is a series of free-standing articles
  that provides basic definitions of frequently-used terms
  and links to other documents with additional information.
  This is a great way to quickly locate the information you need.
- The documentation now includes reference pages for
  :ref:`Commands<command-ref-top>`,
  :ref:`Configuration files<config-ref-top>`,
  :ref:`APIs<api-ref>`,
  :ref:`adapters<adapter-ref-top>`,
  and :ref:`messages<message-ref-top>`.

Kamanja 1.6.1 was a limited release that includes
the following new features,
included in 1.6.2 for the first time in a public release:

- :ref:`Consolidated logging<logging-consolidated>` -
  creates a centralized queue for all errors and warnings
  from all components and all nodes in the cluster.
  These are implemented as a Kafka topic that the a log server can read.
- :ref:`Encrypted and encoded passwords<password-encrypt-term>` – 
  Kamanja now supports encrypted and encoded passwords
  as well as plain text passwords;
  only the RSA algorithm is supported at this time.
  In Release 1.6.2, you can use encrypted passwords
  with MetadataAPIServices or SmartFileAdapter.
  You can also add this functionality to any
  :ref:`adapters<adapter-term>` you create.
- :ref:`Failover support<failover-nodes-term>` for nodes
  in a cluster running the JDBC, HDFS, or FileDataConsumer services.

Click on the embedded links above
for more details about using each of these new features.



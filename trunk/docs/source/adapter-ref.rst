

.. _adapter-ref-top:

=======================
Adapter Reference Pages
=======================


.. list-table::
   :widths: 20 80
   :class: lg-wrap-fixed-table
   :header-rows: 1

   * - Adapter
     - Description
   * - :ref:`elastic-output-adapter-ref`
     - Output JSON data to :ref:`elasticsearch-term`
   * - :ref:`oracle-output-adapter-ref`
     - Output JSON data to an Oracle database
   * - :ref:`smart-input-config-ref`
     - ingest streaming messages automatically
   * - :ref:`smart-output-config-ref`
     - Output processed data to an outside consumer.
   * - :ref:`storage-adapter-ref`
     - Output information saved from models
       as well as :ref:`messages<messages-term>`,
       :ref:`containers<container-term>`, and :ref:`model<model-term>` results
       to an HBase, Cassandra, or Microsoft SQL Server data store.



.. toctree::
   :titlesonly:
   :maxdepth: 1

   adapter-ref/elastic-output
   adapter-ref/OracleOutputAdapter
   adapter-ref/smart-input
   adapter-ref/smart-output
   adapter-ref/storage



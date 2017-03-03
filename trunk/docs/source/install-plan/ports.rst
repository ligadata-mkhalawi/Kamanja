
.. _ports-install-top:

Reserved ports
==============

.. list-table::
   :class: lg-wrap-fixed-table
   :widths: 20 20 60
   :header-rows: 1

   * - Port
     - Use
     - Notes
   * - 1443
     - Listening port for Microsoft SQL server
     -
   * - 2181
     - Zookeeper connection;
       defined in :ref:`ClusterConfig.json<clusterconfig-config-ref>`
     -
   * - 7700
     - Global logical partition cache port;
       defined in :ref:`ClusterConfig.json<clusterconfig-config-ref>`
     -
   * - 7800
     - JGroups use to replicate and distribute Ehcached data over TCP
     -
   * - 9092
     - Kafka
     - HortonWorks uses 6667 for Kafka
   * - 9200
     - Elasticsearch
     -



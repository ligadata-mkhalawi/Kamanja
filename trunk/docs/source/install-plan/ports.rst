
.. _ports-install-top:

Reserved ports
==============

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Port
     - Use
   * - 1443
     - Listening port for Microsoft SQL server
   * - 2181
     - Zookeeper connection;
       defined in :ref:`ClusterConfig.json<clusterconfig-config-ref>`
   * - 7700
     - Global logical partition cache port;
       defined in :ref:`ClusterConfig.json<clusterconfig-config-ref>`
   * - 7800
     - JGroups use to replicate and distribute Ehcached data over TCP
   * - 9200
     - ElasticSearch



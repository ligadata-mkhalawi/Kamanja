

.. _start-stop-cluster:

Start and stop a cluster or node
================================

Use the following commands to start or stop
a cluster or node;
see the reference pages for detailed information.


.. list-table::
   :class: ld-wrap-fixed-table
   :widths: 30 50
   :header-rows: 1

   * - Start a cluster
     - :ref:`startkamanjacluster-command-ref`
   * - Stop a cluster
     - :ref:`stopkamanjacluster-command-ref`
   * - Start a single node in a cluster
     - :ref:`kamanja-command-ref` start
   * - Stop a single node in a cluster
     - :ref:`kamanja-command-ref` stop



You can start your single-node installation
with the following steps:

- Run :ref:`SetPaths.sh<setpaths-command-ref>`
  to perform string replacement on items in the template files
  and create new configuration files.

- Run :ref:`zkServer.sh`<zkserver-command-ref>`
  to start :ref:`Zookeeper<zookeeper-term>`.

- Run :ref:`kafka-server-start.sh<kafka-server-start-command-ref>`
  to start :ref:`Kafka<kafka-term>`.







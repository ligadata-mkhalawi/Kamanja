
.. _failover-nodes-term:

Failover for nodes
------------------

The Kamanja failover feature makes the Kamanja platform more robust
by providing failover capabilities for nodes in a cluster
that runs a daemon service.
In 1.6.1 and 1.6.2, failover is implemented for
the JDBC, HDFS, or FileDataConsumer features.

#. For the clusters running one of the relevant services,
   configure at least one "standby" node in addition to the active node.
   In most cases, you will want to provide
   two standby nodes in each cluster;
   if your reliability needs are extremely high,
   you can configure additional "standby" nodes.
#. The failover tools are installed on each node in each cluster
   that runs one of the supported services.
#. The configuration file for the adapters running a relevant service
   include the **adapter.weight** parameter, which is set to an integer.

   - The node with the highest value for the **adapter.weight** property
     is the active node.
   - If the active node fails for some reason,
     processing falls over to the node with the next highest value
     for the **adapter.weight** property.
   - If the secondary node fails for some reason
     and additional nodes are configured,
     processing falls over to the node
     with the next highest value for the adapter.weight property.

#. When the failed node is restored,
   the processor running on that node will begin processing
   on the restored node,
   assuming that the the **adapter.weight** property
   has the same value as before it failed.


To configure the failover feature:

1.  Define the following environment variables to appropriate values
    for your environment.
    $INSTALL_HOME is used to pick up the new
    KamanjaEndpointServices_2.11-162.jar file.

   ::

     INSTALL_HOME
     export currentKamanjaVersion=1.6.2
     export KAMANJA_HOME=/media/home2/installKamanja162/Kamanja-1.6.2_2.11
     export INSTALL_HOME=/media/home2/Kamanja/trunk/Utils/OutputUtils/target/scala-2.11
     export HADOOP_HOME=/home/hadoop/hadoop

2. Add and populate the failover parameters in the configuration files
   for the relevant adapters such as hdfs.properties and FileConsumer.conf:

   ::

     component.name=JDBCSink
     zookeeper.connect=localhost:2181
     zookeeper.leader.path=/kamanja/SITCluster/adapters
     node.id.prefix=Node2
     adapter.weight=20



   The meaning of these parameters is:

   - **component.name** refers to the type of client tool.
     This parameter is set to the same value
     for all the clients of the specified type
     (JDBC, HDFS, or FileConsumer) in a cluster.
   - **zookeeper.connect** is the IP address
     for the server running ZooKeeper
   - **zookeeper.leader.path** is the ZkPath node.id.prefix
     that represents a logical string used to name ZooKeeper nodes
     that are created to implement fail-over functionality.
     The value can be any string that represents the cluster host
     where the relevant tool is running.
   - **adapter.weight** determines the client process
     that does the actual processing of the data
     and is the most relevant for the implementation of the failover feature.

   As an example, suppose we have a three-cluster node
   with the JDBC adapter running on each node.
   Only the node with highest value for **adapter.weight**
   processes the data; all other nodes are standby nodes.
   If this adapter is stopped for some reason,
   the adapter with next highest weight takes over the processing.

3. Start the services with the appropriate scripts




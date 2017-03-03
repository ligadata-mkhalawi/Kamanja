

.. _zkserver-command-ref:

zkServer.sh
===========

Start :ref:`Zookeeper<zookeeper-term>`.

Syntax
------

::

  cd $ZOOKEEPER_HOME/conf
  cp zoo_sample.cfg zoo.cfg

  $ZOOKEEPER_HOME/bin/zkServer.sh start

Options and arguments
---------------------

Usage
-----

Output
------

The expected output is similar to:

::

  ZooKeeper JMX enabled by default
  Using config: /Users/userid/Downloads/zookeeper-version-number/bin/../conf/zoo.cfg
  Starting zookeeper ... STARTED

To test that Zookeeper is running, the command is:

::

  $ZOOKEEPER_HOME/bin/zkCli.sh -server 127.0.0.1:2181

The expected output is:

::

  Connecting to 127.0.0.1:2181
  2015-12-03 14:29:24,725 [myid:] - INFO  [main:Environment@100] - Client environment:zookeeper.version=3.4.7-1713338, built on 11/09/2015 04:32 GMT
  ...
  WATCHER::

  WatchedEvent state:SyncConnected type:None path:null

See also
--------

- :ref:`Zookeeper<zookeeper-command-ref>`



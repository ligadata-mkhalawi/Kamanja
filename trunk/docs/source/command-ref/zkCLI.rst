

.. _zkcli.sh-command-ref:

zkCli.sh
========

Test that Zookeeper is running.

Syntax
------

::

  $ZOOKEEPER_HOME/bin/zkCli.sh -server 127.0.0.1:2181


Options and arguments
---------------------

Usage
-----

Output
------

The expected output is:

::

  Connecting to 127.0.0.1:2181
  2015-12-03 14:29:24,725 [myid:] - INFO  [main:Environment@100] - Client environment:zookeeper.version=3.4.7-1713338, built on 11/09/2015 04:32 GMT
  ...
  WATCHER::

  WatchedEvent state:SyncConnected type:None path:null

See also
--------

- :ref:`zkServer.sh<zkserver-command-ref>`
- :ref:`Zookeeper<zookeeper-command-ref>`



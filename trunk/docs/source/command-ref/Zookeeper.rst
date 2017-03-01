

.. _zookeeper-command-ref:

Zookeeper
=========

:ref:`Zookeeper<zookeeper-term>` shell.


Syntax
------

::

  ZooKeeper -server host:port cmd args
	  stat path [watch]
	  set path data [version]
	  ls path [watch]
	  delquota [-n|-b] path
	  ls2 path [watch]
	  setAcl path acl
	  setquota -n|-b val path
	  history
	  redo cmdno
	  printwatches on|off
	  delete path [version]
	  sync path
	  listquota path
	  rmr path
	  get path [watch]
	  create [-s] [-e] path data acl
	  addauth scheme auth
	  quit
	  getAcl path
	  close
	  connect host:port
  [zk: 127.0.0.1:2181(CONNECTED) 1]

Options and arguments
---------------------

Usage
-----

Output
------

Expected output:

::

  Quitting...
  2015-12-03 14:30:45,746 [myid:] - INFO  [main:ZooKeeper@684] - Session: 0x15169f626110000 closed
  2015-12-03 14:30:45,748 [myid:] - INFO  [main-EventThread:ClientCnxn$EventThread@519] - EventThread shut down for session: 0x15169f626110000


See also
--------





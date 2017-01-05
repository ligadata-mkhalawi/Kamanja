

.. _setpaths-command-ref:

SetPaths.sh
===========

Perform string replacement on items in the template files
and create new configuration files in the config folder.

Syntax
------

::

  bash $KAMANJA_HOME/bin/SetPaths.sh [$KAFKA_HOME]

Options and arguments
---------------------

It is not necessary to specify $KAFKA_HOME but,
if it is omitted,
watch scripts such as
:ref:`WatchStatusQueue.sh<watchstatusqueue-command-ref>` and
:ref:`WatchOutputQueue.sh<watchoutputqueue-command-ref>`
do not set properly.


Usage
-----

See also
--------



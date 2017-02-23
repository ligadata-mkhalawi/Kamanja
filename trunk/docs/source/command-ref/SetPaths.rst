

.. _setpaths-command-ref:

SetPaths.sh
===========

Set all pathnames to reflect the installed location of Kamanja.

Syntax
------

::

  cd $KAMANJA_HOME/bin
  bash $KAMANJA_HOME/bin/SetPaths.sh [$KAFKA_HOME]

Options and arguments
---------------------

It is not necessary to specify $KAFKA_HOME but,
if it is omitted,
some scripts such as :ref:`watchqueue-command-ref`
do not set properly.


Usage
-----

**SetPaths.sh** should be run after downloading and untar'ring
the kamanja software package;
it performs string replacement on items in the template files
and creates new configuration files in the *config* folder.



See also
--------



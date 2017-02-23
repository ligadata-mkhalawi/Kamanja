

.. _setpath-install:

Run SetPaths.sh (single-node only)
----------------------------------

If you are installing a single-node cluster,
run :ref:`setpaths-command-ref`
to set all the paths to the current installed location of Kamanja:

::

    cd $KAMANJA_HOME/bin
    SetPaths.sh $KAFKA_HOME

When this has run successfully,
your single-node cluster is installed and ready to use.

- The structure of the installed software is described in
  :ref:`dir-struct-install`.
- If you installed this cluster to study and explore Kamanja,
  a good next step is to run and study the
  :ref:`sample applications<run-samples-install>`
  that are provided.
- You can also create your own application
  by creating and configuring your own messages, models, and so forth.
  See :ref:`models-top`.


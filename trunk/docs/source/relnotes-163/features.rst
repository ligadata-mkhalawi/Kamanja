
New features in Kamanja 1.6.3
=============================

Kamanja 1.6.3 includes a number of bug fixes
plus the following new features:

- Support for :ref:`Monit<monit-term>`
  to monitor the health of the Kamanja cluster
  and to start/stop nodes and the cluster itself.
  See :ref:`monit-admin` for details.
- Encrypted and encoded passwords<password-encrypt-term>` are now supported
  for :ref:`Smart File Output Adapters <smart-output-config-ref>`
  as well as for :ref:`Smart File Input Adapters<smart-input-config-ref>`
  and :ref:`metadataapiconfig-config-ref`.

New adapters
------------

Kamanja 1.6.3 includes a number of new input and output adapters:

- :ref:`dbconsumer-input-adapter-ref` to ingest data
  into Kamanja from an Oracle database.
  Note that, in support of this adapter,
  the :ref:`clusterinstallerdriver-command-ref` command
  now includes the **-externalJarsDir** argument
  to specify the external jars directory from which
  data is ingested.

- :ref:`oracle-output-adapter-ref` to feed data
  from Kamanja to an Oracle database.

- :ref:`daas-input-adapter-ref` enables you to ingest data
  using the REST API.

Click on the embedded links above
for more details about using each of these new features.



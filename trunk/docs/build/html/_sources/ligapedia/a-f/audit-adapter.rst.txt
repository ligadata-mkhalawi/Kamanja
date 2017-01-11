
.. _audit-adapter-term:

Audit adapter
-------------

The MetadataAPIConfig.properties metadata configuration file
can optionally specify a file where the adapter-specific initialization
is defined.

This is done via the AUDIT_PARMS parameter.
The name of the file is passed from Kamanja
to the audit adapter implementation in the init method.

Note: The contents of this file are adapter-specific
and the parsing/processing of the values
must be implemented in the init method of the adapter.

For example, metadata configuration file
for the Hbase AuditHbaseAdapter.scala sample audit adapter
that is shipped with Kamanja is configured as follows:

::

  AUDIT_PARMS=/Users/dan/Documents/testdata/AuditParms.txt

and the AuditParms.txt has the following content:

::

  schema=default
  hostlist=localhost
  table=metadata_audit



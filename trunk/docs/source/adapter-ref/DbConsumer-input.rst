

.. _dbconsumer-input-adapter-ref:

DbConsumer Input Adapter
========================

The DbConsumer Input Adapter is a special version of the
:ref:`smart-input-config-ref`
that is used to ingest data from an Oracle database.


ClassName
---------

com.ligadata.InputAdapters.DbConsumer$


Oracle Input Adapter structure
------------------------------

::

  {
    "Name": "DbInputAdapter1",
    "TypeString": "Input",
    "TenantId": "tenant1",
    "ClassName": "com.ligadata.InputAdapters.DbConsumer$",
    "DependencyJars": [
      "ojdbc6.jar"
    ],
    "AdapterSpecificCfg": {
      "DriverName": "oracle.jdbc.driver.OracleDriver",
      "URLString": "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)
           (HOST=myserver)(PORT=7896))(CONNECT_DATA=(SERVER=DEDICATED)
           (SERVICE_NAME=myserver)))",
      "UserId": "test1user",
      "Password": "test1password",
      "Consumers": "4",
      "Timeout": "86400",
      "RefreshInterval": "2000",
      "Serialize": {
        "Format": "kv",
        "Options": {
          "FieldDelimiter": "\u0001",
          "AlwaysQuoteFields": "false",
          "KeyDelimiter": "\u0002"
        }
      },
      "Queries": {
        "Tbl1Query": {
          "Query": "SELECT A, B, C FROM (SELECT A, B, C FROM TBL1
              WHERE ${PrimaryAndPartitionSubstitution} ORDER BY A)
              WHERE ROWNUM <= 100",
          "PrimaryKeyColumn": "A",
          "PartitionExpression": "MOD(C, ${MaxPartitions}) = ${PartitionId}",
          "PrimaryKeyExpression": "A > ${PreviousPrimaryKeyValue}"
        },
        "Tbl2Query": {
        "Query": "SELECT A, X, C FROM (SELECT A, B AS X, C FROM TBL2
              WHERE ${PrimaryAndPartitionSubstitution} AND C in (1, 2, 3) ORDER BY X DESC)
              WHERE ROWNUM <= 100",
          "PrimaryKeyColumn": "X",
          "PartitionExpression": "MOD(ORA_HASH(CONCAT(A, B)), ${MaxPartitions})
              = ${PartitionId}",
          "PrimaryKeyExpression": "X < ${PreviousPrimaryKeyValue}"
        }
      }
    }
  }



Parameters
----------

- **ClassName** - com.ligadata.InputAdapters.DbConsumer$
- **DependencyJars** - Depending on the database we connect,
  the jars will be added here.
- **DriverName** - DriverName for the corresponding database.
  For example, the driver name for Oracle
  is oracle.jdbc.driver.OracleDriver;
  for SQL server, the driver name is
  com.microsoft.sqlserver.jdbc.SQLServerDriver.
- **URLString** - URLString is specific to database and db connection.
  This identifies where to connect and the database to use.
- **UserId** - Database user
- **Password** - Database password that corresponds to the UserId specified.
- **Consumers** - Number of parallel queries to run on database
  (and get distinct data) and process on same number of threads
- **Timeout** - Connection/Query timeout (in secs)
- **RefreshInterval** - execute the query every N milliseconds.
  If this value is <= 0, it will do only once and then stop.
- **Serialize** - Define how data is sent to the engine.
  Three formats of :ref:`Serialize<serial-deserial-term>` are supported:

  - **KV** has these options:

    - **FieldDelimiter** - Delimiter between fields
    - **KeyDelimiter** - Delimiter between key & value for each field
    - **AlwaysQuoteFields** - if set to true,
      use quotation marks around the data;
      default value is false.

  - **Delimited** has these options:

    - **FieldDelimiter** - Delimiter between fields
    - **AlwaysQuoteFields** - if set to true,
      use quotation marks around the data;
      default value is false.

   -  **json** does not use any options

- **Queries** - Provide N queries, so, each partition(thread)
  is executed by one query.
  Once all queries in the section are finished,
  it waits for **RefreshInterval** milliseconds before it starts re-executing
  all the queries in each partition/thread.

  Each query is divided into four sections:

  - PartitionExpression: where the in the table can be partitioned.
    In the example Tbl1Query,
    we look for ${MaxPartitions} & ${PartitionId} and we replace them
  - PrimaryKeyExpression: where to start getting next data
    (ignoring already processed data) except for the first time.
    The first time, this section is ignored
    because no previous data is available to start from there.
    We replace ${PreviousPrimaryKeyValue}
    with the most recently processed value.
    NOTE: If the value is a string or string literal,
    you must add quotes around ${PreviousPrimaryKeyValue};
    for example, '${PreviousPrimaryKeyValue}'
  - PrimaryKeyColumn: column where the value for each processed row is saved.
    For restart, we ignore the data and restart from there.
  - Query: User query to query the data and send it to the engine.
    ${PrimaryAndPartitionSubstitution} is replaced in this section
    with a new value that is constructed
    from both the PrimaryKeyExpression & PartitionExpression sections.


Usage
-----

When installing a cluster that will use this adapter,
you must specify the **-externalJarsDir** parameter
to the :ref:`clusterinstallerdriver-command-ref` command
to identify the external directory that contains the jars
to be ingested.

Difference between versions
---------------------------

The DbConsumer input adapter is supported
in Kamanja 1.6.3 and later releases.

See also
--------

- :ref:`adapter-binding-config-ref`
- :ref:`adapter-def-config-ref`


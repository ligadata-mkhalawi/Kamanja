
.. _jtm-test:

Testing and debugging a JTM model
=================================


How to Run the Compiler Manually
--------------------------------

During initial development of a JTM,
is is useful to run the compiler directly,
outside of the Kamanja metadata ingestion system.
To do that from **sbt**, run:

::

  sbt (jtm)> run --help [info] Running com.ligadata.jtm.Transpiler --help [info] \
      -j, --jtm Sources to compile (default = ) [info] \
      --help Show help message [success]
  Total time: 3 s, completed Jan 18, 2017 10:42:03 PM

How to Add Debugging to the Transformation Section
--------------------------------------------------

The model has five logging methods with three overloads each.
The tracing can be configured through log4j.
These can be used in line in a transformation right-hand side
to print out the information that is being processed to a log.
For example,
::

  "computes": {
  "inputFields": {
      "type": "Array[String]",
      "val": "Debug(s”m1 value = %s”.format($m1.toString())); $m1.split(\",\", -1)",
      "comment": "Split the incoming data into fields"
  }
  }

The following debug functions are available:

::

  Trace(tmsg: String)
  Warning(wmsg: String)
  Info(imsg: String)
  Error(emsg: String)
  Debug(dmsg: String)

  Trace(tmsg: String,  e: Throwable)
  Warning(wmsg: String,  e: Throwable)
  Info(imsg: String,  e: Throwable)
  Error(emsg: String,  e: Throwable)
  Debug(dmsg: String,  e: Throwable)

  Trace( e: Throwable)
  Warning( e: Throwable)
  Info( e: Throwable)
  Error( e: Throwable)
  Debug( e: Throwable)



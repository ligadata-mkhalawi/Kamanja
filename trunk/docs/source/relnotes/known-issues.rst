
Known issues in Kamanja 1.6.2
=============================

All known issues in the Kamanja software and documentation
can be viewed at the `https://github.com/LigaData/Kamanja/issues` web site.

- Only JSON data can be pushed to ElasticSearch.
  This means that the :ref:`adapter-binding<adapter-binding-config-ref>`
  must use the JSON :ref:`serializer<serial-deserial-term>`.

- :ref:`Archiver<archiver-term>` files are currently created
  directly in the destination directory and given a name
  that indicates the date of creation.
  It is planned to create subdirectories for each date
  and write the archiver files to those subdirectories.

- The :ref:`elastic-output-adapter-ref` does not
  report results for each message processed
  (successful, rejected, or threw an exception).

- To stop the KamanjaManager process,
  use ctrl-c or the **kill** command;
  **kill -9** should not be used
  because it can cause data to be lost,
  especially when using Elasticsearch.

- The PDF files produced for the documentation
  display the file name rather than the document title.

- The :ref:`simplekafkaproducer-command-ref` command
  may throw spurious errors that reference
  :ref:`Velocity Metrics<velocity-metrics-term>`
  whether or not Velocity Metrics is configured.
  This does not prevent messages from being
  pushed and processed.
  (`1515 <https://github.com/LigaData/Kamanja/issues/1515>`_)

- The FileProcessor may throw spurious WARN messages
  into the logs; this does not impact processing.
  (`1507 <https://github.com/LigaData/Kamanja/issues/1507>`_)

- When configuring :ref:`adapters<adapter-def-config-ref>`,
  adding jars to the dependency jars list under "adapters"
  has no effect on the adapter.
  If the existing jars are removed from the configuration,
  the adapters run exactly the same way;
  if a jar is added that is not already in the Kamanja classpath,
  it is not included when the adapter is created.

  An individual class loader must be used for each adapter
  based on the jar dependencies in the configuration
  so that the required jars can be included at run time
  without fear of having version compatibility issues with other adapters.
  (`1493 <https://github.com/LigaData/Kamanja/issues/1493>`_)

- The :ref:`kamanja-command-ref` **add model** command
  may fail when adding a model that imports classes
  from a specified jar.
  Even though the **upload jar my.jar** command
  ran successfully and the uploaded jar is persisted in the metadata storage,
  the model fails to compile because the classes are not visible to the model
  at compile time.
  We are investigating this issue;
  it may be that *my.jar* is not being downloaded automatically
  although it should be.
  (`1492 <https://github.com/LigaData/Kamanja/issues/1492>`_)

- The Delete function for the :ref:`elastic-output-adapter-ref`
  does not work.
  (`1490 <https://github.com/LigaData/Kamanja/issues/1490>`_)

- The Kamanja *readme* file incorrectly shows that the JPMML evaluator
  is licensed under BSD.
  LigaData has negotiated a special arrangement with Villu (owner of JPMML),
  so we can embed JPMML in Kamanja and release the combination
  under the Apache license. The readme file should be corrected.
  (`1482 <https://github.com/LigaData/Kamanja/issues/1482>`_)

- No error message is generated for the error queue
  nor is an error attached to the :ref:`kamanjamessageevent-msg-ref`
  when Kamanja fails to produce an output message
  because the message could not be serialized;
  this can happen, for example, if you use a mismatched Scala version.
  Information about the error can be found by digging through the logs.
  (`1476 <https://github.com/LigaData/Kamanja/issues/1476>`_)

- When a :ref:`message definition<message-def-config-ref>`
  contains one or more containers as attributes,
  the versioned message code that is generated
  correctly shows the versioned containers as parameters
  but incorrectly shows the versioned containers as parameters
  for the unversioned message code.
  This does not affect functionality.
  (`1469 <https://github.com/LigaData/Kamanja/issues/1469>`_)

- If you upload an :ref:`adapter binding<adapter-binding-config-ref>`
  that has already been added successfully,
  an "Unexpected action!" error is returned rather than
  'Already found adapter bindings for this message on this adapter'.
  (`1468 <https://github.com/LigaData/Kamanja/issues/1468>`_)




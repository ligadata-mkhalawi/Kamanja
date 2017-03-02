
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

- The :ref:`elastic-output-adapter-ref` should be able
  to report results for each message processed
  (successful, rejected, or threw an exception).
  This functionality is not currently implemented.


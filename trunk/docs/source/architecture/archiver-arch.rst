
.. _archiver-arch:

Archiver internals
==================

The :ref:`archiver<archiver-term>` is implemented
as part of the :ref:`smart-input-config-ref`.
This section describes how the archiver functionality works.

- When the input adapter processes a file,
  it adds a record to the archive files queue.
- A directories status map is built based in the archive directory
  rather than individual files.
  For each directory, the status map includes:

  - memory stream; contents of source files are read into this stream
    until the stream is full or rollover is due
  - path of the destination file to which the memory stream
    will be flushed when it is full
  - time when data will next be flushed, based on the rollover interval
  - names of source files whose contents were copied
    into the current memory stream
  - the part of the archive index that corresponds
    to the data that is currently in the memory stream.
    This is similar to an in-memory index.
    This information will be flushed into an index file
    when the archive data is flushed to disk.
  - active flag; this is used to ensure
    that one thread does not archive a file
    when another file from the same directory
    is being processed by another thread.
    If this happens, the thread that is blocked
    searches for another file to archive.
    This means that the archive files queue is not actually a queue;
    archiving threads may skip into the next item.
  - current size
  - current offset: number of bytes written into memomry stream so far,
    which may not be the same as size if the destination is compressed.

When a thread starts to archive a file,
it temporarily uses the status map that corresponds to that file;
no specific thread owns any status map.

The memory stream is processed as follows:

- When a thread has archived a file
  (meaning it has been moved from an inactive directory
  according to the status map),
  it copies the file contents into the memory stream,
  compressing the data if necessary.
- The memory stream and its corresponding index are flushed to disk
  when one of the following occurs:

  - The memory stream is full (has reached the maximum configured size)
  - The rollover interval expires

- When the memory stream has been flushed successfully,
  all source files involved in that stream are flushed,
  with the possible exception of the last file in the stream
  which is not flushed if processing has not completed.

Basically, archiving is a two-stage process
that can be viewed through the log files.

The first stage copies the source file into memory,
compresses the data, and checks to see
if the maximum consolidation limit has been reached.
This produces a file with a name like
*billing_20170122T120901.csv.tgz*:

::

  2017-02-01 03:01:19,118 -
     com.ligadata.InputAdapters.Archiver [pool-17030-thread-5] -
     WARN - finished archiving file /home/bigdata/billing_20170122T120901.csv.tgz
     into memory. totalReadLen=204810240

The WARN string merely indicates a notification, not an error.

The second stage dumps this file into disk storage
along with other files in that archive:

::

  2017-02-01 03:02:51,253 - com.ligadata.InputAdapters.Archiver [pool-17030-thread-5] -
    WARN - archive file hdfs://jmbdcls01-ns/user/kamanjaprod/ARCHIVE/tt/reconciliation/file_20170201_025948312
    dumped successfully. last src file is /home/bigdata/billing_20170122T132420.csv.tgz .
    other finished src files are
    (/home/bigdata/billing_20170122T120901.csv.tgz,
     /home/bigdata/billing_20170122T130911.csv.tgz,
     /home/bigdata/billing_20170122T125408.csv.tgz,
     /home/bigdata/billing_20170122T133918.csv.tgz)


Each record in the archive index contains the following fields:
srcFile (path is from processed directory), destFile,
srcFileStartOffset, srcFileEndOffset, destFileStartOffset,
destFileEndOffset, ArchiveTimestamp
The archive index is rolled over daily.

The archiver preserves data through :ref:`failover<failover-nodes-term>`
by using the following techniques:

- The index is flushed before the memory stream is flushed.
- This way, when the engine is restarted,
  it checks whether the data is valid,
  indicating that the stream was completely and successfully
  written to disk as the index states.
  If the data is not valid,
  the entry is removed from the index
  and archiving is repeated for any missing parts.
  The source file should be there
  since the engine only deletes source files
  after verifying that the data was successfully flushed to disk.
- The engine also checks for files in the target directories
  that do not have records in the index
  and adds them to the archive queue.




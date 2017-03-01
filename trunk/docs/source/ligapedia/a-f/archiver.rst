
.. _archiver-term:

Archiver
--------

The Archiver moves files that have been read and processed
into another location that may use a different file system architecture;
for example, files that were processed in SFTP may be archived to HDFS.
The archiver is implemented in the
:ref:`smart-input-config-ref`.

The archiver does the following:

- Files are read from source folders by the input adapter and processed.
- Each file is then moved to "processed" folder.
- Archiving is implemented from the processed folder.
- When a file is completely archived,
  it is deleted from the processed folder.
  So archive input files are actually on processed folder

The archiver consolidates and compresses input files into larger files
rather than simply move files as they are.
The maximum size of the larger files is configurable;
the archiver merges the small files until the maximum size is reached.
This means that an input file's contents
could be in two different destination archive files
but the contents of one input file
are always be in the same archive directory.

Archive index files log each input file
and the location where its contents are found
in the destination archive files.
Archive index files have a fixed name format:
*ArchiveIndex_<yyyyMMdd>.info*;
each index file logs information about all files
that were archived that day.

Each archive index entry has the following structure:

::

  srcFile, destFile, srcFileStartOffset, srcFileEndOffset,
     destFileStartOffset, destFileEndOffset, timestamp

For example, the following entries are for two input files
that were archived into same destination archive file
(file_20170221_235229961):

::

  /home/bigdata/emm/jam/air_adjustments/kprod/processed/20170222.AIR.aircur2a.cur.0268.Adjustment,
     hdfs://jmbdcls01-ns/user/kamanjaprod/ARCHIVE/jam/air_adjustments/file_20170221_235229961,
     0,6127,0,6127,2017-02-21 23:53:27:327

  /home/bigdata/emm/jam/air_adjustments/kprod/processed/20170221.AIR.AIRCAY2A.CAY.8003.Adjustment,
     hdfs://jmbdcls01-ns/user/kamanjaprod/ARCHIVE/jam/air_adjustments/file_20170221_235229961,
     0,1132,6128,7260,2017-02-21 23:53:31:008




See also:

- :ref:`smart-input-config-ref` gives details about implementing
  the archiver, including an example configuration.
- :ref:`archiver-arch` gives details about how the archiver
  is implemented internally




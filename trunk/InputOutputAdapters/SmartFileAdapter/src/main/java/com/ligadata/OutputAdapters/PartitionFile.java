package com.ligadata.OutputAdapters;


import com.ligadata.KamanjaBase.ContainerInterface;
import com.ligadata.KamanjaBase.TransactionContext;

public interface PartitionFile {

    void init(String filePath, long flushBufferSize);

    void reopen();
    String getKey();
    String getFilePath();
    long getRecordsInBuffer();
    long getSize();
    long getFlushBufferSize();

    void close();
    void flush();
    int send(TransactionContext tnxCtxt, ContainerInterface record, SmartFileProducer serializer);
}

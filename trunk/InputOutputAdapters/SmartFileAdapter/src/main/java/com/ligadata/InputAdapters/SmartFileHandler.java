package com.ligadata.InputAdapters;

import com.ligadata.Exceptions.KamanjaException;

import java.io.InputStream;


public interface SmartFileHandler {
    String getFullPath();
    String getParentDir();

    //gets input stream based on the fs type (das/nas, hdfs, sft), usually used for file type detecting purposes
    InputStream getDefaultInputStream() throws KamanjaException;
    //prepares input stream based on the fs type and also file type itself (plain, gzip, bz2, lzo), so data can be read directly
    InputStream openForRead() throws KamanjaException;
    int read(byte[] buf, int length) throws KamanjaException;
    int read(byte[] buf, int offset, int length) throws KamanjaException;
    void close();
    boolean moveTo(String newPath) throws KamanjaException;
    boolean delete() throws KamanjaException;
    boolean deleteFile(String fileName) throws KamanjaException;
    long length() throws KamanjaException;
    long lastModified() throws KamanjaException;
    boolean exists() throws KamanjaException;
    boolean isFile() throws KamanjaException;
    boolean isDirectory() throws KamanjaException;

    boolean isAccessible();

    boolean mkdirs();
}

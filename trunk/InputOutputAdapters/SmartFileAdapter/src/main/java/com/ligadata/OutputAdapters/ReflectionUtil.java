package com.ligadata.OutputAdapters;

import java.lang.reflect.*;

public class ReflectionUtil {
    public void setParquetMinRecCount(int count){
        try{


            Class aclass = Class.forName("parquet.hadoop.InternalParquetRecordWriter");
            Field field = aclass.getDeclaredField("MINIMUM_RECORD_COUNT_FOR_CHECK");
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            //modifiersField.setInt(field, field.getModifiers() & ~Modifier.PRIVATE & ~Modifier.STATIC & ~Modifier.FINAL);

            field.setAccessible(true);
            //System.out.println("new value for MINIMUM_RECORD_COUNT_FOR_CHECK is " +(int)field.get(null));
            System.out.println(">>>>>>>>>>>>>>>setting MINIMUM_RECORD_COUNT_FOR_CHECK to "+count);
            field.set(null, count);

            /*****now test the change*****/
            /*Class anotherclass = Class.forName("parquet.hadoop.InternalParquetRecordWriter");
            Field anotherField = anotherclass.getDeclaredField("MINIMUM_RECORD_COUNT_FOR_CHECK");
            anotherField.setAccessible(true);
            System.out.println("new value for MINIMUM_RECORD_COUNT_FOR_CHECK is " +(int)anotherField.get(null));*/
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void setParquetMaxRecCount(int count){
        try{
            Class aclass = Class.forName("parquet.hadoop.InternalParquetRecordWriter");
            Field field = aclass.getDeclaredField("MAXIMUM_RECORD_COUNT_FOR_CHECK");
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.setAccessible(true);
            System.out.println(">>>>>>>>>>>>>>>setting MAXIMUM_RECORD_COUNT_FOR_CHECK to "+count);
            field.set(null, count);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void setParquetRecCount(Object obj, int count){
        try{
            Class pwclass = Class.forName("parquet.hadoop.ParquetWriter");
            Field writerField = pwclass.getDeclaredField("writer");
            writerField.setAccessible(true);
            Object writerObj = writerField.get(obj);

            Class aclass = Class.forName("parquet.hadoop.InternalParquetRecordWriter");
            Field recCountfield = aclass.getDeclaredField("recordCountForNextMemCheck");

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(recCountfield, recCountfield.getModifiers());
            //modifiersField.setInt(field, field.getModifiers() & ~Modifier.PRIVATE & ~Modifier.STATIC & ~Modifier.FINAL);

            recCountfield.setAccessible(true);
            System.out.println(">>>>>>>>>>>>>>>setting recordCountForNextMemCheck to "+count);
            recCountfield.set(writerObj, count);

        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void callFlush(Object obj){
        try{
            Class pwclass = Class.forName("parquet.hadoop.ParquetWriter");
            Field writerField = pwclass.getDeclaredField("writer");
            writerField.setAccessible(true);
            Object writerObj = writerField.get(obj);

            Class aclass = Class.forName("parquet.hadoop.InternalParquetRecordWriter");
            Method flushMethod = aclass.getDeclaredMethod("flushRowGroupToStore");
            flushMethod.setAccessible(true);
            System.out.println(">>>>>>>>>>>>>>>invoking  flushRowGroupToStore()");
            flushMethod.invoke(writerObj);

        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }
}

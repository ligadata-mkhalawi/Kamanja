package com.ligadata.tools.validator;

/**
 * Created by dhavalkolapkar on 10/24/16.
 */
import java.util.Properties;

public interface  PropertiesValidator {

    public  boolean validate(String fileLoc) throws Exception;

    public  boolean validate(Properties properties) throws Exception;

    public  void loadDefaultProperties(Properties properties);

}

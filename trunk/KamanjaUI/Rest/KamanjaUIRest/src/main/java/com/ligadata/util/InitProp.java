package com.ligadata.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class InitProp {
    private static String confFilePath;
    private Properties properties;

    public Properties properties() {
        try {
            if (properties == null) {
                InputStream inputStream = new FileInputStream(getConfFilePath());

                if (inputStream != null) {
                    properties = new Properties();
                    properties.load(inputStream);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static String getConfFilePath() {
        return confFilePath;
    }

    public static void setConfFilePath(String confFilePath) {
        InitProp.confFilePath = confFilePath;
    }

}

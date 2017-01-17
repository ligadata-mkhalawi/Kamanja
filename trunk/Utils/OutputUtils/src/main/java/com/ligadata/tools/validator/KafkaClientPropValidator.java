package com.ligadata.tools.validator;


/**
* Created by dhavalkolapkar on 10/24/16.
*/

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class KafkaClientPropValidator implements PropertiesValidator {
    private static KafkaClientPropValidator kafkaClientPropValidator=null;
    private static List<String> defaultKafkaProducerProp=new ArrayList<String>();
    static Logger logger = LogManager.getLogger(KafkaClientPropValidator.class);
    public static KafkaClientPropValidator getInstance(){
        if(kafkaClientPropValidator!=null){
            return kafkaClientPropValidator;
        }else{
            return new KafkaClientPropValidator();
        }
    }

    private KafkaClientPropValidator(){
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty("bootstrap.servers", "");
        defaultProperties.setProperty("key.serializer", "");
        defaultProperties.setProperty("value.serializer", "");
        defaultProperties.setProperty("acks", "");
        defaultProperties.setProperty("buffer.memory", "");
        defaultProperties.setProperty("compression.type", "");
        defaultProperties.setProperty("retries", "");
        defaultProperties.setProperty("ssl.key.password", "");
        defaultProperties.setProperty("ssl.keystore.location", "");
        defaultProperties.setProperty("ssl.keystore.password", "");
        defaultProperties.setProperty("ssl.truststore.location", "");
        defaultProperties.setProperty("ssl.truststore.password", "");
        defaultProperties.setProperty("batch.size", "");
        defaultProperties.setProperty("client.id", "");
        defaultProperties.setProperty("connections.max.idle.ms", "");
        defaultProperties.setProperty("linger.ms", "");
        defaultProperties.setProperty("max.block.ms", "");
        defaultProperties.setProperty("max.request.size", "");
        defaultProperties.setProperty("partitioner.class", "");
        defaultProperties.setProperty("receive.buffer.bytes", "");
        defaultProperties.setProperty("request.timeout.ms", "");
        defaultProperties.setProperty("sasl.kerberos.service.name", "");
        defaultProperties.setProperty("sasl.mechanism", "");
        defaultProperties.setProperty("security.protocol", "");
        defaultProperties.setProperty("send.buffer.bytes", "");
        defaultProperties.setProperty("ssl.enabled.protocols", "");
        defaultProperties.setProperty("ssl.keystore.type", "");
        defaultProperties.setProperty("ssl.protocol", "");
        defaultProperties.setProperty("ssl.provider", "");
        defaultProperties.setProperty("ssl.truststore.type", "");
        defaultProperties.setProperty("timeout.ms", "");
        defaultProperties.setProperty("interceptor.classes", "");
        defaultProperties.setProperty("max.in.flight.requests.per.connection", "");
        defaultProperties.setProperty("metadata.fetch.timeout.ms", "");
        defaultProperties.setProperty("metadata.max.age.ms", "");
        defaultProperties.setProperty("metric.reporters", "");
        defaultProperties.setProperty("metrics.num.samples", "");
        defaultProperties.setProperty("metrics.sample.window.ms", "");
        defaultProperties.setProperty("reconnect.backoff.ms", "");
        defaultProperties.setProperty("retry.backoff.ms", "");
        defaultProperties.setProperty("sasl.kerberos.kinit.cmd", "");
        defaultProperties.setProperty("sasl.kerberos.min.time.before.relogin", "");
        defaultProperties.setProperty("sasl.kerberos.ticket.renew.jitter", "");
        defaultProperties.setProperty("sasl.kerberos.ticket.renew.window.factor", "");
        defaultProperties.setProperty("ssl.cipher.suites", "");
        defaultProperties.setProperty("ssl.endpoint.identification.algorithm", "");
        defaultProperties.setProperty("ssl.keymanager.algorithm", "");
        defaultProperties.setProperty("ssl.secure.random.implementation", "");
        defaultProperties.setProperty("ssl.trustmanager.algorithm", "");

        Set<Object> keys=defaultProperties.keySet();
        for(Object key: keys){
            if(!defaultKafkaProducerProp.contains(String.valueOf(key))){
                defaultKafkaProducerProp.add(String.valueOf(key));
            }
        }
    }

    private boolean validateProducerProp(Properties properties) {
        boolean hasErrors = false;
        Set<Object> keys=properties.keySet();
        for(Object property: keys){
            if(!defaultKafkaProducerProp.contains(String.valueOf(property))){
                recommend(String.valueOf(property));
                hasErrors=true;
            }
        }
        if (!hasErrors) {
            logger.info("Kafka producer properties set as per the user properties");
            System.out.println("Kafka producer properties set as per the user properties");
            return true;
        }
        return false;
    }

    private void recommend(String a) {
        int min = 0;
        int[] minDistance = new int[defaultKafkaProducerProp.size()];
        for (int k = 0; k < defaultKafkaProducerProp.size(); k++) {
            String b = String.valueOf(defaultKafkaProducerProp.get(k));
            int[][] mat = new int[a.length() + 1][b.length() + 1];
            for (int i = 0; i < a.length(); i++) {
                mat[i][0] = i;
            }

            for (int j = 0; j < b.length(); j++) {
                mat[0][j] = j;
            }

            for (int i = 0; i < a.length(); i++) {
                for (int j = 0; j < b.length(); j++) {
                    char a1 = a.charAt(i);
                    char b1 = b.charAt(j);

                    if (a1 == b1) {
                        mat[i + 1][j + 1] = mat[i][j];
                    } else {
                        // min of add, remove, update
                        int add = mat[i][j + 1] + 1;
                        int remove = mat[i + 1][j] + 1;
                        int update = mat[i][j] + 1;
                        int minValue = Math.min(add, remove);
                        minValue = Math.min(minValue, update);
                        mat[i + 1][j + 1] = minValue;
                    }

                }
            }

            minDistance[k] = mat[a.length()][b.length()];
        }
        for (int i = 0; i < minDistance.length; i++) {
            if (minDistance[i] < minDistance[min]) {
                min = i;
            }
        }
        logger.error("Did you mean " + defaultKafkaProducerProp.get(min) + " instead of " + a + "?");
        System.out.println("Did you mean " + defaultKafkaProducerProp.get(min) + " instead of " + a + "?");
    }

    @Override
    public boolean validate(String fileLoc) throws IOException {
        Properties properties = new Properties();
        BufferedReader reader=null;
        try {
            reader= new BufferedReader(new FileReader(fileLoc));
            String s = null;
            while ((s = reader.readLine()) != null) {
                properties.put(s.split("=")[0], s.split("=")[1]);
            }
        } catch (FileNotFoundException e) {
            logger.error("Properties file not found");
            System.out.println("Properties file not found");
            throw e;
        } catch (IOException e1) {
            e1.printStackTrace();
            throw e1;
        }finally {
            if(reader!=null){
                try {
                    reader.close();
                } catch (IOException e) {
                    throw e;
                }
            }
        }
        return validateProducerProp(properties);
    }

    @Override
    public  boolean validate(Properties properties) {
        return validateProducerProp(properties);

    }

    @Override
    public void loadDefaultProperties(Properties properties) {
        Set<Object> keys=properties.keySet();
        for(Object key: keys){
            if(!defaultKafkaProducerProp.contains(String.valueOf(key))){
                defaultKafkaProducerProp.add(String.valueOf(key));
            }
        }
    }

}

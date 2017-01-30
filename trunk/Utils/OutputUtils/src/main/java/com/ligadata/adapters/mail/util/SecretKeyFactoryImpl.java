package com.ligadata.adapters.mail.util;

import java.io.UnsupportedEncodingException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.commons.codec.binary.Base64;

import com.ligadata.adapters.AdapterConfiguration;

@SuppressWarnings("restriction")
public class SecretKeyFactoryImpl {
    private static SecretKeyFactoryImpl skf = null;
    private AdapterConfiguration conf;

    private static final Logger LOGGER = LogManager.getLogger(SecretKeyFactoryImpl.class);

    private SecretKeyFactory keyFactory;
    private DESKeySpec keySpec;
    private SecretKey key;
    private Base64 base64;
    private Cipher cipher;
    private String encrypedPwd;
    private byte[] encrypedPwdBytes;
    private byte[] plainTextPwdBytes;


    private SecretKeyFactoryImpl(AdapterConfiguration configs) {
        conf = configs;
    }

    public static SecretKeyFactoryImpl getInstance(AdapterConfiguration configs) {
        if (skf == null) {
            synchronized (SecretKeyFactoryImpl.class) {
                if (skf == null)
                    skf = new SecretKeyFactoryImpl(configs);
            }
        }
        return skf;
    }

    public void createKey() {
        try {

            keySpec = new DESKeySpec(conf.getProperty(AdapterConfiguration.SKF_PROP_KEY).toString().getBytes(AdapterConfiguration.SKF_CHARSET));
            keyFactory = SecretKeyFactory.getInstance(AdapterConfiguration.SKF_ENCRY_TYPE);
            key = keyFactory.generateSecret(keySpec);
            base64 = new Base64();

        } catch (Exception e) {
            LOGGER.error("createKey", e);
        }
    }

    public String encoder(String pwd) {
        try {
            // cipher is not
            cipher = Cipher.getInstance(AdapterConfiguration.SKF_ENCRY_TYPE);
            // thread safe
            cipher.init(Cipher.ENCRYPT_MODE, key);
            encrypedPwd = extracted(pwd);
        } catch (Exception e) {
            LOGGER.error("encoder", e);
        }
        return encrypedPwd;
    }

    public String decoder(String encrypedPwd) {
        try {
            encrypedPwdBytes = base64.decode(encrypedPwd);
            // encrypedPwdBytes = base64decoder.decodeBuffer(encrypedPwd);
            // cipher is not
            cipher = Cipher.getInstance(AdapterConfiguration.SKF_ENCRY_TYPE);
            // thread safe
            cipher.init(Cipher.DECRYPT_MODE, key);
            plainTextPwdBytes = (cipher.doFinal(encrypedPwdBytes));
        } catch (Exception e) {
            LOGGER.error("decoder", e);
        }
        return new String(plainTextPwdBytes);
    }

    private String extracted(String pwd) throws IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        return base64.encodeAsString(cipher.doFinal(pwd.getBytes(AdapterConfiguration.SKF_CHARSET)));
    }

}

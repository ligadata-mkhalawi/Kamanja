#!/usr/bin/env bash
KAMANJA_HOME={InstallDirectory}

kafkahostname="localhost:9092"
while [ $# -gt 0 ]
do
    if [ "$1" == "--kafkahosts" ]; then
    	kafkahostname="$2"
    fi
    shift
done

if [ "$KAMANJA_SEC_CONFIG" ]; then
  JAAS_CLIENT_OPT="-Djava.security.auth.login.config="$KAMANJA_SEC_CONFIG
  echo "Using java.security.auth.login.config="$JAAS_CLIENT_OPT
fi

if [ "$KAMANJA_KERBEROS_CONFIG" ]; then
  KERBEROS_CONFIG_OPT="-Djava.security.krb5.confg="$KAMANJA_KERBEROS_CONFIG
  echo "Using java.security.krb5.confg="$KERBEROS_CONFIG_OPT
fi

if [ "$KAMANJA_SECURITY_CLIENT" ]; then
  SECURITY_PROP_OPT="--secprops "$KAMANJA_SECURITY_CLIENT
  echo "Using security client = "$SECURITY_PROP_OPT
fi

KEYSTORE_CONFIG_OPT=""
KEYSTORE_PASS_CONFIG_OPT=""

# -Djavax.net.ssl.keyStore=/tmp/config/cert/kamanja.ks -Djavax.net.ssl.keyStorePassword=password
if [ "$KAMANJA_KEYSTORE" != "" ]; then
	KEYSTORE_CONFIG_OPT="-Djavax.net.ssl.keyStore=$KAMANJA_KEYSTORE"
	KEYSTORE_PASS_CONFIG_OPT="-Djavax.net.ssl.keyStorePassword=$KAMANJA_KEYSTORE_PASS"
fi

TRUSTSTORE_CONFIG_OPT=""
TRUSTSTORE_PASS_CONFIG_OPT=""

# -Djavax.net.ssl.trustStore=/tmp/config/cert/kamanja.ts -Djavax.net.ssl.trustStorePassword=password
if [ "$KAMANJA_TRUSTSTORE" != "" ]; then
	TRUSTSTORE_CONFIG_OPT="-Djavax.net.ssl.trustStore=$KAMANJA_TRUSTSTORE"
	TRUSTSTORE_PASS_CONFIG_OPT="-Djavax.net.ssl.trustStorePassword=$KAMANJA_TRUSTSTORE_PASS"
fi

currentKamanjaVersion=1.5.2

java $JAAS_CLIENT_OPT $KERBEROS_CONFIG_OPT $KEYSTORE_CONFIG_OPT $KEYSTORE_PASS_CONFIG_OPT $TRUSTSTORE_CONFIG_OPT $TRUSTSTORE_PASS_CONFIG_OPT -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_2.11-${currentKamanjaVersion}.jar com.ligadata.tools.SimpleKafkaProducer --gz true --topics "helloworldinput" --threads 1 --topicpartitions 8 --brokerlist "$kafkahostname" --files "$KAMANJA_HOME/input/SampleApplications/data/Input_Data_HelloWorld.csv.gz" --partitionkeyidxs "0" --format CSV $SECURITY_PROP_OPT


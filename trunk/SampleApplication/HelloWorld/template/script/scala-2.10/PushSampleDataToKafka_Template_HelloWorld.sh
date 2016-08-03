#!/usr/bin/env bash
KAMANJA_HOME={InstallDirectory}

kafkahostname="localhost:9092"
kafkaversion="0.10"
while [ $# -gt 0 ]
do
    if [ "$1" == "--kafkahosts" ]; then
    	kafkahostname="$2"
    fi
    if [ "$1" == "--kafkaversion" ]; then
        kafkaversion="$2"
    fi
    shift
done

if [ "$KAMANJA_SEC_CONFIG" ]; then
  JAAS_CLIENT_OPT="-Djava.security.auth.login.config="$KAMANJA_SEC_CONFIG
  echo "Using java.security.auth.login.config="$JAAS_CLIENT_OPT
fi

if [ "$KAMANJA_KERBEROS_CONFIG" ]; then
  KERBEROS_CONFIG_OPT="-Djava.security.krb5.confg="$KAMANJA_KERBEROS_CONFIG
  echo "Using java.security.krb5.conf="$KERBEROS_CONFIG_OPT
fi

if [ "$KAMANJA_SECURITY_CLIENT" ]; then
  SECURITY_PROP_OPT="--secprops "$KAMANJA_SECURITY_CLIENT
  echo "Using security client = "$SECURITY_PROP_OPT
fi


echo "User selected: $INPUTFILE"
echo "Running kafka client version $kafkaversion"

if [$kafkaversion == "0.8"]; then
  java  $JAAS_CLIENT_OPT $KERBEROS_CONFIG_OPT $KEYSTORE_CONFIG_OPT $KEYSTORE_PASS_CONFIG_OPT $TRUSTSTORE_CONFIG_OPT $TRUSTSTORE_PASS_CONFIG_OPT -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-1.5.3.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-1.5.3.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-1.5.3.jar:$KAMANJA_HOME/lib/system/kafka-clients-0.8.2.2.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_v8_2.10-1.5.3.jar com.ligadata.tools.kafkaProducer_v8.SimpleKafkaProducer --gz true --topics "helloworldinput" --threads 1 --topicpartitions 8 --brokerlist "$kafkahostname" --files "$KAMANJA_HOME/input/SampleApplications/data/Input_Data_HelloWorld.csv.gz" --partitionkeyidxs "0" --format CSV $SECURITY_PROP_OPT
elif [$kafkaversion == "0.9"]; then
  java  $JAAS_CLIENT_OPT $KERBEROS_CONFIG_OPT $KEYSTORE_CONFIG_OPT $KEYSTORE_PASS_CONFIG_OPT $TRUSTSTORE_CONFIG_OPT $TRUSTSTORE_PASS_CONFIG_OPT -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-1.5.3.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-1.5.3.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-1.5.3.jar:$KAMANJA_HOME/lib/system/kafka-clients-0.9.0.1.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_v9_2.10-1.5.3.jar com.ligadata.tools.kafkaProducer_v9.SimpleKafkaProducer --gz true --topics "helloworldinput" --threads 1 --topicpartitions 8 --brokerlist "$kafkahostname" --files "$KAMANJA_HOME/input/SampleApplications/data/Input_Data_HelloWorld.csv.gz" --partitionkeyidxs "0" --format CSV $SECURITY_PROP_OPT
else
  java  $JAAS_CLIENT_OPT $KERBEROS_CONFIG_OPT $KEYSTORE_CONFIG_OPT $KEYSTORE_PASS_CONFIG_OPT $TRUSTSTORE_CONFIG_OPT $TRUSTSTORE_PASS_CONFIG_OPT -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-1.5.3.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-1.5.3.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-1.5.3.jar:$KAMANJA_HOME/lib/system/kafka-clients-0.10.0.0.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_v10_2.10-1.5.3.jar com.ligadata.tools.kafkaProducer_v10.SimpleKafkaProducer --gz true --topics "helloworldinput" --threads 1 --topicpartitions 8 --brokerlist "$kafkahostname" --files "$KAMANJA_HOME/input/SampleApplications/data/Input_Data_HelloWorld.csv.gz" --partitionkeyidxs "0" --format CSV $SECURITY_PROP_OPT
fi


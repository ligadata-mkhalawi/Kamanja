#!/usr/bin/env bash

KAMANJA_HOME={InstallDirectory}

currentKamanjaVersion=1.5.4
if [ "$KAMANJA_SEC_CONFIG" ]; then
  JAAS_CLIENT_OPT="-Djava.security.auth.login.config="$KAMANJA_SEC_CONFIG
  echo "Using java.security.auth.login.config="$JAAS_CLIENT_OPT
fi

if [ "$KAMANJA_KERBEROS_CONFIG" ]; then
  KERBEROS_CONFIG_OPT="-Djava.security.krb5.conf="$KAMANJA_KERBEROS_CONFIG
  echo "Using java.security.krb5.conf="$KERBEROS_CONFIG_OPT
fi

kafkahostname="localhost:9092"
kafkaversion="0.10"
configfile=""
while [ $# -gt 0 ]
do
    if [ "$1" == "--kafkahosts" ]; then
        kafkahostname="$2"
    fi
    if [ "$1" == "--kafkaversion" ]; then
        kafkaversion="$2"
    fi
    if [ "$1" == "--config" ]; then
        configfile="$2"
    fi
    shift
done

if [ "$configfile" = "" ]; then
    echo "Please provide configfile"
    exit 1
fi

if [ "$kafkaversion" = "0.8" ]; then
  java  $JAAS_CLIENT_OPT $KERBEROS_CONFIG_OPT $KEYSTORE_CONFIG_OPT $KEYSTORE_PASS_CONFIG_OPT $TRUSTSTORE_CONFIG_OPT $TRUSTSTORE_PASS_CONFIG_OPT -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-$currentKamanjaVersion.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-$currentKamanjaVersion.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-$currentKamanjaVersion.jar:$KAMANJA_HOME/lib/system/kafka-clients-0.8.2.2.jar:$KAMANJA_HOME/lib/system/kafka_2.11-0.8.2.2.jar:$KAMANJA_HOME/lib/system/filedataconsumer_2.11-${currentKamanjaVersion}.jar com.ligadata.filedataprocessor.LocationWatcher  "$configfile"
elif [ "$kafkaversion" = "0.9" ]; then
  java  $JAAS_CLIENT_OPT $KERBEROS_CONFIG_OPT $KEYSTORE_CONFIG_OPT $KEYSTORE_PASS_CONFIG_OPT $TRUSTSTORE_CONFIG_OPT $TRUSTSTORE_PASS_CONFIG_OPT -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-$currentKamanjaVersion.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-$currentKamanjaVersion.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-$currentKamanjaVersion.jar:$KAMANJA_HOME/lib/system/kafka-clients-0.9.0.1.jar:$KAMANJA_HOME/lib/system/filedataconsumer_2.11-${currentKamanjaVersion}.jar com.ligadata.filedataprocessor.LocationWatcher  "$configfile"
else
    java  $JAAS_CLIENT_OPT $KERBEROS_CONFIG_OPT $KEYSTORE_CONFIG_OPT $KEYSTORE_PASS_CONFIG_OPT $TRUSTSTORE_CONFIG_OPT $TRUSTSTORE_PASS_CONFIG_OPT -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-$currentKamanjaVersion.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-$currentKamanjaVersion.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-$currentKamanjaVersion.jar:$KAMANJA_HOME/lib/system/kafka-clients-0.10.0.0.jar:$KAMANJA_HOME/lib/system/filedataconsumer_2.11-${currentKamanjaVersion}.jar com.ligadata.filedataprocessor.LocationWatcher  "$configfile"
fi


KAMANJA_HOME={InstallDirectory}

if [ "$KAMANJA_SEC_CONFIG" ]; then
  KAFKA_KERBEROS_CLIENT_OPT="-Djava.security.auth.login.config="$KAMANJA_SEC_CONFIG
fi

if [ "$KAMANJA_KERBEROS_CONFIG" ]; then
  KERBEROS_CONFIG_OPT="-Djava.security.krb5.confg="$KAMANJA_KERBEROS_CONFIG
fi

if [ "$KAMANJA_SECURITY_CLIENT" ]; then
  SECURITY_PROP_OPT="--secprops "$KAMANJA_SECURITY_CLIENT
fi

java $JAAS_CONFIG_OPT $KERBEROS_CONFIG_OPT -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-1.4.1.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-1.4.1.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-1.4.1.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_2.10-1.4.1.jar com.ligadata.tools.SimpleKafkaProducer --gz true --topics "medicalinput" --threads 1 --topicpartitions 8 --brokerlist "localhost:9092" --files "$KAMANJA_HOME/input/SampleApplications/data/copd_demo_Medical.csv.gz" --partitionkeyidxs "1" --format CSV $SECURITY_PROP_OPT


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

java $JAAS_CLIENT_OPT $KERBEROS_CONFIG_OPT -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-1.5.0.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-1.5.0.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-1.5.0.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_2.11-1.5.0.jar com.ligadata.tools.SimpleKafkaProducer --gz true --topics "medicalinput" --threads 1 --topicpartitions 8 --brokerlist "$kafkahostname" --files "$KAMANJA_HOME/input/SampleApplications/data/copd_demo_Medical.csv.gz" --partitionkeyidxs "1" --format CSV $SECURITY_PROP_OPT


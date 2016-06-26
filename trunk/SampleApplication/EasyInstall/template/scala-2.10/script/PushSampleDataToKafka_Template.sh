#!/usr/bin/env bash
KAMANJA_HOME={InstallDirectory}
if [ "$#" -eq 1 ]; then
INPUTFILE=$@
else
count=0
FILEDIR=$KAMANJA_HOME/input/SampleApplications/data
for entry in "$FILEDIR"/*
do
count=$((count+1))
  echo "$count: $entry"
  LISTOFFILES[count-1]=$entry
done
read -p "Please select from the above options: " useroption
OPTION=useroption-1
INPUTFILE=${LISTOFFILES[OPTION]}
fi

if [ "$KAMANJA_KERBEROS_CLIENT" ]; then
  KAFKA_KERBEROS_CLIENT_OPT="-Djava.security.auth.login.config="$KAMANJA_SEC_CONFIG
fi

if [ "$KERBEROS_CONFIG" ]; then
  KERBEROS_CONFIG_OPT="-Djava.security.krb5.confg="$KAMANJA_KERBEROS_CONFIG
fi

if [ "$KAMANJA_SECURITY_CLIENT" ]; then
  SECURITY_PROP_OPT="--secprops "$KAMANJA_SECURITY_CLIENT
fi

echo "User selected: $INPUTFILE"
java $JAAS_CONFIG_OPT $KERBEROS_CONFIG_OPT  -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-1.4.1.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-1.4.1.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-1.4.1.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_2.10-1.4.1.jar com.ligadata.tools.SimpleKafkaProducer --gz true --topics "testin_1" --threads 1 --topicpartitions 8 --brokerlist "localhost:9092" --files $INPUTFILE   --partitionkeyidxs "1" --format CSV $SECURITY_PROP_OPT


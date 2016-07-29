#!/usr/bin/env bash

KAMANJA_HOME={InstallDirectory}

currentKamanjaVersion=1.5.1
if [ "$KAMANJA_SEC_CONFIG" ]; then
  JAAS_CLIENT_OPT="-Djava.security.auth.login.config="$KAMANJA_SEC_CONFIG
  echo "Using java.security.auth.login.config="$JAAS_CLIENT_OPT
fi

if [ "$KAMANJA_KERBEROS_CONFIG" ]; then
  KERBEROS_CONFIG_OPT="-Djava.security.krb5.conf="$KAMANJA_KERBEROS_CONFIG
  echo "Using java.security.krb5.conf="$KERBEROS_CONFIG_OPT
fi

java $JAAS_CLIENT_OPT $KERBEROS_CONFIG_OPT -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/filedataconsumer_2.10-${currentKamanjaVersion}.jar com.ligadata.filedataprocessor.LocationWatcher "$@"


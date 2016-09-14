#!/bin/sh
KAMANJA_HOME={InstallDirectory}
PROP_FILE=$1
if [ -z $1 ]; then
   PROP_FILE={InstallDirectory}/config/Engine1Config.properties
   echo "Using the default file, {InstallDirectory}/config/Engine1Config.properties "
fi

if [ ! -d "$KAMANJA_HOME/python/logs" ]; then
    mkdir -p $KAMANJA_HOME/python/logs
else
    now=$(date +"%m_%d_%Y")
    if [ ! -d $KAMANJA_HOME/python/logs/$now ]; then
	mkdir -p $KAMANJA_HOME/python/logs/$now
    fi
    mv -f $KAMANJA_HOME/python/logs/pythonserver.*  $KAMANJA_HOME/python/logs/$now
fi
# Start the engine with hashdb backed metadata configuration.  The zookeeper and your queue software should be running
ipport="8998"

#-Djava.security.auth.login.config=/tmp/kerberos/jaas-client.conf
if [ "$KAMANJA_SEC_CONFIG" ]; then
  JAAS_CONFIG_OPT="-Djava.security.auth.login.config="$KAMANJA_SEC_CONFIG
fi

# -Djava.security.krb5.conf=/etc/krb5.conf
if [ "$KAMANJA_KERBEROS_CONFIG" ]; then
  KERBEROS_CONFIG_OPT="-Djava.security.krb5.conf="$KAMANJA_KERBEROS_CONFIG
fi


currentKamanjaVersion=1.6.0

if [ "$1" != "debug" ]; then
	java $JAAS_CONFIG_OPT $KERBEROS_CONFIG_OPT -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/kamanjamanager_2.11-${currentKamanjaVersion}.jar com.ligadata.KamanjaManager.KamanjaManager --config $PROP_FILE
else
	java $JAAS_CONFIG_OPT $KERBEROS_CONFIG_OPT -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/kamanjamanager_2.11-${currentKamanjaVersion}.jar com.ligadata.KamanjaManager.KamanjaManager --config $PROP_FILE
fi

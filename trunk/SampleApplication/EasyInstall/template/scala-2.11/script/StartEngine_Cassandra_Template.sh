#!/bin/bash

# Start the engine with cassandra backed metadata configuration.  Cassandra must be running, not to mention the zookeeper and your queue software 
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

ENGINECONFIG="$KAMANJA_HOME/config/Engine1Config_cassandra.properties"
DEBUG="NO"
if [ "$1" != "" ]; then
    if [ "$1" = "debug" ]; then
	DEBUG="YES"
    else
	ENGINECONFIG=$1
	if [ "$2" = "debug" ]; then
	    DEBUG="YES"
	fi
    fi
fi

if [ "$1" != "debug" ]; then
	java  $JAAS_CONFIG_OPT $KERBEROS_CONFIG_OPT -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/kamanjamanager_2.11-${currentKamanjaVersion}.jar com.ligadata.KamanjaManager.KamanjaManager --config $ENGINECONFIG
else
	java  $JAAS_CONFIG_OPT $KERBEROS_CONFIG_OPT -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/kamanjamanager_2.11-${currentKamanjaVersion}.jar com.ligadata.KamanjaManager.KamanjaManager --config $ENGINECONFIG
fi

#!/bin/sh
KAMANJA_HOME={InstallDirectory}

KAMANJA_HOME={InstallDirectory}
for var in "$@"
do
    if [ $var == "debug" ]; then
         DEBUG=$var
    elif [[ $var == *.properties ]]; then
        PROP_FILE=$var
    elif [[ $var == *.properties ]]; then
        PROP_FILE=$var
    else
        JVMOPTIONS=$var
    fi
done

if [ -z $PROP_FILE ]; then
   PROP_FILE={InstallDirectory}/config/Engine1Config.properties
    echo "Using the default properties file"
fi


if [ -z $JVMOPTIONS ]; then
    echo "Using default JVM options Engine1Config.properties"
    JVMOPTIONS=`grep -i "JVM" $PROP_FILE | cut -d"=" -f2`
fi

echo "PROP_FILE: $PROP_FILE"
echo "DEBUG: $DEBUG"
echo "JVMOPTIONS: $JVMOPTIONS"

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

if [ "$DEBUG" != "debug" ]; then
    java $JVMOPTIONS  $JAAS_CONFIG_OPT $KERBEROS_CONFIG_OPT -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/kamanjamanager_2.10-${currentKamanjaVersion}.jar com.ligadata.KamanjaManager.KamanjaManager --config $PROP_FILE
else
	java $JVMOPTIONS  $JAAS_CONFIG_OPT $KERBEROS_CONFIG_OPT -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/kamanjamanager_2.10-${currentKamanjaVersion}.jar com.ligadata.KamanjaManager.KamanjaManager --config $PROP_FILE
fi

#!/usr/bin/env bash

KAMANJA_HOME={InstallDirectory}

currentKamanjaVersion=1.6.1

if [ "$1" != "debug" ]; then
	java -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaAppTester_2.11-${currentKamanjaVersion}.jar com.ligadata.kamanja.test.application.TestExecutor --kamanja-dir $KAMANJA_HOME
else
	java -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaAppTester_2.11-${currentKamanjaVersion}.jar com.ligadata.kamanja.test.application.TestExecutor --kamanja-dir $KAMANJA_HOME
fi
#!/bin/sh
KAMANJA_HOME={InstallDirectory}


if [ "$1" = "tcp" ]
then
    connectionMode='tcp'
fi

if [ "$1" = "ssl" ]
then
    connectionMode='tcpSSL'
fi


cd {InstallDirectory}/bin

currentKamanjaVersion=1.5.1

java -Dh2.baseDir=../storage -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar org.h2.tools.Server -$connectionMode -tcpPort 9100 -tcpAllowOthers
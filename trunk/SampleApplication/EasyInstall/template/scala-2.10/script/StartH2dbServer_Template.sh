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

java -Dh2.baseDir=../storage -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-1.5.0.jar org.h2.tools.Server -$connectionMode -tcpPort 9100 -tcpAllowOthers
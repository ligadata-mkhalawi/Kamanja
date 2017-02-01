#!/usr/bin/env bash

KAMANJA_HOME={InstallDirectory}

currentKamanjaVersion=1.6.1

while [[ $# -gt 1 ]]
do
    key="$1"
    case $key in
        -m|--metadata-config)
            metadataAPIConfig=$2
            if [ ! -f "$metadataAPIConfig" ]; then
                echo "ERROR: Metadata API Configuration file '$metadataAPIConfig' does not exist."
                exit 1
            fi
            shift
            ;;
        -c|--cluster-config)
            clusterConfig=$2
            if [ ! -f "$clusterConfig" ]; then
                echo "ERROR: Cluster Configuration file '$metadataAPIConfig' does not exist."
                exit 1
            fi
            shift
            ;;
        -h|--help)
            help=true
            shift
            ;;
        *)
            echo "ERROR: Unknown option $key."
            exit 1
            ;;
    esac
    shift
done

while [[ $# -eq 1 ]]
do
    key="$1"
    case $key in
        -h|--help)
            echo "You need help?"
            help=true
            shift
            ;;
        *)
            echo "ERROR: Unknown option $key."
            exit 1
            ;;
    esac
shift
done

if [ "$help" = true ]; then
    java -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaAppTester_2.10-${currentKamanjaVersion}.jar com.ligadata.kamanja.test.application.TestExecutor --help
elif [ -z "$metadataAPIConfig" ] || [ -z "$clusterConfig" ]; then
    echo "Either Metadata API Configuration or Cluster Configuration or both were not provided. Executing using embedded services."
    if [ "$1" != "debug" ]; then
	    java -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaAppTester_2.10-${currentKamanjaVersion}.jar com.ligadata.kamanja.test.application.TestExecutor --kamanja-dir $KAMANJA_HOME
    else
	    java -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaAppTester_2.10-${currentKamanjaVersion}.jar com.ligadata.kamanja.test.application.TestExecutor --kamanja-dir $KAMANJA_HOME
    fi
elif [ ! -z "$metadataAPIConfig" ] && [ ! -z "$clusterConfig" ]; then
    echo "Metadata API Configuration and Cluster Configuration files were provided. Executing using existing environment."
    if [ "$1" != "debug" ]; then
	    java -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaAppTester_2.10-${currentKamanjaVersion}.jar com.ligadata.kamanja.test.application.TestExecutor --kamanja-dir $KAMANJA_HOME --metadata-config $metadataAPIConfig --cluster-config $clusterConfig
    else
	    java -Xdebug -Xrunjdwp:transport=dt_socket,address="$ipport",server=y -Dlog4j.configurationFile=file:{InstallDirectory}/config/log4j2.xml -cp {InstallDirectory}/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:{InstallDirectory}/lib/system/KamanjaAppTester_2.10-${currentKamanjaVersion}.jar com.ligadata.kamanja.test.application.TestExecutor --kamanja-dir $KAMANJA_HOME --metadata-config $metadataAPIConfig --cluster-config $clusterConfig
    fi
fi
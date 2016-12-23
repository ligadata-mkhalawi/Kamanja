#!/bin/bash

# GenerateAdapterBindings.sh
#

Usage()
{
    echo 
    echo "Usage:"
    echo "      GenerateAdapterBindings.sh --KamanjaHome  <kamanjaHome>"
    echo "      --ClusterConfig <ClusterCfgPath> --OutputFile <OutFilePath>"
    echo                      
    echo "  NOTES: Generate Message Adapter Bindings Json using cluster configuration to locate"
    echo "         the appropriate adapter information. "
    echo 
}

name1=$1
currentKamanjaVersion=1.5.4

if [ "$#" -eq 6 ]; then
	echo
else 
    echo "Problem: Incorrect number of arguments"
    echo 
    Usage
    exit 1
fi

if [[ "$name1" != "--KamanjaHome" && "$name1" != "--ClusterConfig" && "$name1" != "--OutputFile" ]]; then
	echo "Problem: Bad arguments"
	echo 
	Usage
	exit 1
fi

# Collect the named parameters 
kamanjaHome=""
clusterConfig=""
outputFile="/tmp/AdapterMessageBindings.json"

while [ "$1" != "" ]; do
    echo "parameter is $1"
    case $1 in
        --KamanjaHome )   shift
                                kamanjaHome=$1
                                ;;
        --ClusterConfig )       shift
                                clusterConfig=$1
                                ;;
        --OutputFile )          shift
                                outputFile=$1
                                ;;

        * )                     echo "Problem: Argument $1 is invalid named parameter."
                                Usage
                                exit 1
                                ;;
    esac
    shift
done
java -Dlog4j.configurationFile=file:$kamanjaHome/config/log4j2.xml -cp $kamanjaHome/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:$kamanjaHome/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:$kamanjaHome/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:$kamanjaHome/lib/system/generateadapterbindings_2.11-${currentKamanjaVersion}.jar com.ligadata.Migrate.GenerateAdapterBindings --config $clusterConfig --outfile $outputFile
echo result = $?

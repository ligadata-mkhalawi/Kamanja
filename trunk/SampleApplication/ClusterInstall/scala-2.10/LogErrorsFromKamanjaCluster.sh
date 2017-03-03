#!/bin/bash

# RecentLogErrorsFromKamanjaCluster.sh
#

Usage()
{
    echo 
    echo "Answer any errors from the Kamanja cluster log"
    echo
    echo "Usage:"
    echo "      LogErrorsFromKamanjaCluster.sh --ClusterId <cluster name identifer> "
    echo "                                     --MetadataAPIConfig  <metadataAPICfgPath>  "
    echo "                                     --KamanjaLogPath <kamanja system log path>"
    echo "                                     [--ErrLogPath <where errors are collected> ] "
    echo 
    echo "  NOTES: Logs for the cluster specified by the cluster identifier parameter found in the metadata api "
    echo "         configuration.  "
    echo "         Default error log path is \"/tmp/errorLog.log\" .. errors collected in this file "
    echo 
}


name1=$1

if [ "$#" -ge 3 ]; then
	echo
else 
    echo "Problem: Incorrect number of arguments"
    echo 
    Usage
    exit 1
fi

if [[ "$name1" != "--MetadataAPIConfig" && "$name1" != "--ClusterId" && "$name1" != "--KamanjaLogPath" && "$name1" != "--ErrLogPath" ]]; then
	echo "Problem: Bad arguments"
	echo 
	Usage
	exit 1
fi

# Collect the named parameters 
metadataAPIConfig=""
clusterId=""
inLast=10
timeUnit="minute"
errLogPath="/tmp/errorLog.log"
logPath="/tmp/testlog.log"

while [ "$1" != "" ]; do
    #echo "parameter is $1"
    case $1 in
        --ClusterId )           shift
                                clusterId=$1
                                ;;
        --MetadataAPIConfig )   shift
                                metadataAPIConfig=$1
                                ;;
        --KamanjaLogPath )      shift
                                logPath=$1
                                ;;
        --ErrLogPath )          shift
                                errLogPath=$1
                                ;;
        * )                     echo "Problem: Argument $1 is invalid named parameter."
                                Usage
                                exit 1
                                ;;
    esac
    shift
done

echo "logPath=$logPath"
echo "errorLogPath=$errorLogPath"

if [ -z "$logPath" ]; then
    echo "Problem: Please specify the Kamanja log path.... logPath = $logPath"
    echo 
    Usage
    exit 1
fi
    
if [ -z "$errLogPath" ]; then
    echo "Problem: Please specify file path where errors are to be collected."
    echo 
    Usage
    exit 1
fi
    

# 1) Collect the relevant node information for this cluster.
workDir="/tmp" 
ipFile="ip.txt"
ipPathPairFile="ipPath.txt"
ipIdCfgTargPathQuartetFileName="ipIdCfgTarg.txt"
installDir=`cat $metadataAPIConfig | grep '[Rr][Oo][Oo][Tt]_[Dd][Ii][Rr]' | sed 's/.*=\(.*\)$/\1/g' | sed 's/[\x01-\x1F\x7F]//g'`

echo "...extract node information for the cluster to be started from the Metadata configuration information supplied"

currentKamanjaVersion=1.6.2

# info is assumed to be present in the supplied metadata store... see trunk/utils/NodeInfoExtract for details 
echo "...Command = java -cp $installDir/lib/system/jarfactoryofmodelinstancefactory_2.10-${currentKamanjaVersion}.jar:$installDir/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$installDir/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$installDir/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$installDir/lib/system/nodeinfoextract_2.10-${currentKamanjaVersion}.jar com.ligadata.installer.NodeInfoExtract --MetadataAPIConfig \"$metadataAPIConfig\" --workDir \"$workDir\" --ipFileName \"$ipFile\" --ipPathPairFileName \"$ipPathPairFile\" --ipIdCfgTargPathQuartetFileName \"$ipIdCfgTargPathQuartetFileName\" --installDir \"$installDir\" --clusterId \"$clusterId\""
java -cp $installDir/lib/system/jarfactoryofmodelinstancefactory_2.10-${currentKamanjaVersion}.jar:$installDir/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$installDir/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$installDir/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$installDir/lib/system/nodeinfoextract_2.10-${currentKamanjaVersion}.jar com.ligadata.installer.NodeInfoExtract --MetadataAPIConfig $metadataAPIConfig --workDir "$workDir" --ipFileName "$ipFile" --ipPathPairFileName "$ipPathPairFile" --ipIdCfgTargPathQuartetFileName "$ipIdCfgTargPathQuartetFileName" --installDir "$installDir" --clusterId "$clusterId"
if [ "$?" -ne 0 ]; then
    echo
    echo "Problem: Invalid arguments supplied to the NodeInfoExtract-1.0 application... unable to obtain node configuration... exiting."
    Usage
    exit 1
fi

currentDate=`date +"%Y-%m-%d %T"`

# For each cluster node examine 1) whether there are logs, 2) if the logs are have records that reflect the lookback date
# directory setup for that purpose.  The name of the pid file will always be 'node$id.pid'.  The targetPath points to the given cluster's 
# config directory where the Kamanja engine config file is located.
exec 12<&0 # save current stdin
exec < "$workDir/$ipIdCfgTargPathQuartetFileName"
while read LINE; do
    machine=$LINE
    read LINE
    id=$LINE
    read LINE
    cfgFile=$LINE
    read LINE
    targetPath=$LINE
    echo "quartet = $machine, $id, $cfgFile, $targetPath"
    
    errorsfile=errorsfile$id.txt

    # 
    # A logfile needs to exist to be of interest.  When this is so,
    # grep of the log is performed for the term "\- ERROR \-" 
    # When no such file exists, nevertheless produce an error file indicating no ERRORs found
    #
  
    ssh -o StrictHostKeyChecking=no -T $machine  <<-EOF
        if [ ! -d "$installDir/tmp" ]; then
            mkdir "$installDir/tmp"
        fi
        cd $installDir/tmp
        echo "logPath=$logPath"
        if [ -f "$logPath" ]; then
            grep "$lookBackDate" "$logPath" | wc -l >logRecsAvaialble.txt
            cat logRecsAvaialble.txt
        else
            echo 0 >logRecsAvaialble.txt
        fi
EOF
    noFileFound=""
    scp -o StrictHostKeyChecking=no "$machine:$installDir/tmp/logRecsAvaialble.txt" "$workDir/logRecsAvaialble.txt" 
    logRecCnt=`head -1 "$workDir/logRecsAvaialble.txt"`
    if [ "$logRecCnt" -gt 0 ]; then
        cmd="grep '\- ERROR \-' $logPath "
        echo "grep cmd = $cmd"

        ssh -o StrictHostKeyChecking=no -T $machine  <<-EOF
            cd $installDir/tmp
            $cmd > "$errorsfile"
EOF
    else 
        echo "Node $id (Errors detected at $currentDate)  :" >> "$errLogPath"
        echo "file $logPath not found" >> "$errLogPath"
        echo "No ERRORs found for this period"  >> "$errLogPath"
        noFileFound=1
    fi
      
    scp -o StrictHostKeyChecking=no "$machine:$installDir/tmp/$errorsfile" "$workDir/$errorsfile" 

    errCnt=`wc -l "$workDir/$errorsfile"`
    if [ "$logRecCnt" -gt 0 ]; then
        echo "Node $id (Errors detected at $currentDate) :" >> "$errLogPath"
        cat "$workDir/$errorsfile" >> "$errLogPath"
        echo >> "$errLogPath"
        rm "$workDir/$errorsfile"
    else
        if [ -z "$noFileFound" ]; then
            echo "Node $id (Errors detected at $currentDate)  :" >> "$errLogPath"
            echo "no errors found in file $logPath" >> "$errLogPath"
            echo "No ERRORs found for this period"  >> "$errLogPath"
        fi
    fi

done
exec 0<&12 12<&-

echo


#!/bin/bash

# StartKamanjaCluster.sh
#

Usage()
{
    echo 
    echo "Usage:"
    echo "      StartKamanjaCluster.sh --ClusterId <cluster name identifer> "
    echo "                           --MetadataAPIConfig  <metadataAPICfgPath> "
    echo "                           [--NodeIds  <nodeIds>] "
    echo 
    echo "  NOTES: Start the cluster specified by the cluster identifier parameter.  Use the metadata api configuration to locate"
    echo "         the appropriate metadata store.  For "
    echo 
}


scalaVersion="2.10"
name1=$1

currentKamanjaVersion=1.5.3

if [[ "$#" -eq 4 || "$#" -eq 6 ]]; then
	echo
else 
    echo "Problem: Incorrect number of arguments"
    echo 
    Usage
    exit 1
fi

if [[ "$name1" != "--MetadataAPIConfig" && "$name1" != "--ClusterId" && "$name1" != "--NodeIds" ]]; then
	echo "Problem: Bad arguments"
	echo 
	Usage
	exit 1
fi

# Collect the named parameters 
metadataAPIConfig=""
clusterId=""
nodeIds=""
valid_nodeIds=();

while [ "$1" != "" ]; do
    echo "parameter is $1"
    case $1 in
        --ClusterId )           shift
                                clusterId=$1
                                ;;
        --MetadataAPIConfig )   shift
                                metadataAPIConfig=$1
                                ;;
        --NodeIds )             shift
                                nodeIds=$1
                                ;;
        * )                     echo "Problem: Argument $1 is invalid named parameter."
                                Usage
                                exit 1
                                ;;
    esac
    shift
done

# 1) Collect the relevant node information for this cluster.
workDir="/tmp" 
ipFile="ip.txt"
ipPathPairFile="ipPath.txt"
ipIdCfgTargPathQuartetFileName="ipIdCfgTarg.txt"
installDir=`cat $metadataAPIConfig | grep '[Rr][Oo][Oo][Tt]_[Dd][Ii][Rr]' | sed 's/.*=\(.*\)$/\1/g'`

echo "...extract node information for the cluster to be started from the Metadata configuration information supplied"

# info is assumed to be present in the supplied metadata store... see trunk/utils/NodeInfoExtract for details 
echo "...Command = java -cp $installDir/lib/system/ExtDependencyLibs2_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/ExtDependencyLibs_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/KamanjaInternalDeps_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/nodeinfoextract_${scalaVersion}-${currentKamanjaVersion}.jar com.ligadata.installer.NodeInfoExtract --MetadataAPIConfig \"$metadataAPIConfig\" --workDir \"$workDir\" --ipFileName \"$ipFile\" --ipPathPairFileName \"$ipPathPairFile\" --ipIdCfgTargPathQuartetFileName \"$ipIdCfgTargPathQuartetFileName\" --installDir \"$installDir\" --clusterId \"$clusterId\""
java -cp $installDir/lib/system/ExtDependencyLibs2_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/ExtDependencyLibs_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/KamanjaInternalDeps_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/nodeinfoextract_${scalaVersion}-${currentKamanjaVersion}.jar com.ligadata.installer.NodeInfoExtract --MetadataAPIConfig $metadataAPIConfig --workDir "$workDir" --ipFileName "$ipFile" --ipPathPairFileName "$ipPathPairFile" --ipIdCfgTargPathQuartetFileName "$ipIdCfgTargPathQuartetFileName" --installDir "$installDir" --clusterId "$clusterId"
if [ "$?" -ne 0 ]; then
    echo
    echo "Problem: Invalid arguments supplied to the NodeInfoExtract-1.0 application... unable to obtain node configuration... exiting."
    Usage
    exit 1
fi

#-Djava.security.auth.login.config=/tmp/kerberos/jaas-client.conf
if [ "$KAMANJA_SEC_CONFIG" ]; then
  JAAS_CONFIG_OPT="-Djava.security.auth.login.config="$KAMANJA_SEC_CONFIG
fi

# -Djava.security.krb5.conf=/etc/krb5.conf
if [ "$KAMANJA_KERBEROS_CONFIG" ]; then
  KERBEROS_CONFIG_OPT="-Djava.security.krb5.conf="$KAMANJA_KERBEROS_CONFIG
fi

if [[ $nodeIds != "" ]]; then
    OIFS=$IFS;
    IFS=",";
    nodeIdsArray=($nodeIds);
    IFS=$OIFS;

    for ((i=0; i<${#nodeIdsArray[@]}; ++i)); do 
        tmp_str="$(echo -e "${nodeIdsArray[$i]}" | xargs)"
        if [[ "$tmp_str" != "" ]]; then
            valid_nodeIds+=($tmp_str)
        fi
    done
fi


# Start the cluster nodes using the information extracted from the metadata and supplied config.  Remember the jvm's pid in the $installDir/run
# directory setup for that purpose.  The name of the pid file will always be 'node$id.pid'.  The targetPath points to the given cluster's 
# config directory where the Kamanja engine config file is located.
echo "...start the Kamanja cluster $clusterName"
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
    read LINE
    roles=$LINE
    roles_lc=`echo $roles | tr '[:upper:]' '[:lower:]'`
    restapi_cnt=`echo $roles_lc | grep "restapi" | grep -v "grep" | wc -l`
    processingengine_cnt=`echo $roles_lc | grep "processingengine" | grep -v "grep" | wc -l`

    if [[ ${#valid_nodeIds[@]} > 0 ]]; then
        currentNodeId=""
        for ((j=0; j<${#valid_nodeIds[@]}; ++j)); do 
            if [[ ${valid_nodeIds[$j]} != $id ]]; then
              continue
            else
                currentNodeId=$id
            fi
        done
        if [[ $currentNodeId != $id ]]; then
          continue
        fi
    fi

    echo "NodeInfo = $machine, $id, $cfgFile, $targetPath, $roles"
    echo "...On machine $machine, starting Kamanja node with configuration $cfgFile for NodeId $id to $machine:$targetPath"
    nodeCfg=`echo $cfgFile | sed 's/.*\/\(.*\)/\1/g'`
    pidfile=node$id.pid
     #scp -o StrictHostKeyChecking=no "$cfgFile" "$machine:$targetPath/"
	ssh -o StrictHostKeyChecking=no -T $machine  <<-EOF
		ulimit -u 8192
		cd $targetPath
		echo "nodeCfg=$nodeCfg"
        if [ "$processingengine_cnt" -gt 0 ]; then
			java -Xmx4g -Xms4g $JAAS_CONFIG_OPT $KERBEROS_CONFIG_OPT -Dlog4j.configurationFile=file:$targetPath/engine_log4j2.xml -cp $installDir/lib/system/ExtDependencyLibs2_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/ExtDependencyLibs_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/KamanjaInternalDeps_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/kamanjamanager_${scalaVersion}-${currentKamanjaVersion}.jar com.ligadata.KamanjaManager.KamanjaManager --config "$targetPath/$nodeCfg" < /dev/null > /dev/null 2>&1 &
        fi
#        if [ "$restapi_cnt" -gt 0 ]; then
# 			java -Dlog4j.configurationFile=file:$targetPath/restapi_log4j2.xml -cp $installDir/lib/system/ExtDependencyLibs2_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/ExtDependencyLibs_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/KamanjaInternalDeps_${scalaVersion}-${currentKamanjaVersion}.jar:$installDir/lib/system/metadataapiservice_${scalaVersion}-${currentKamanjaVersion}.jar com.ligadata.metadataapiservice.APIService --config "$targetPath/MetadataAPIConfig_${id}.properties" < /dev/null > /dev/null 2>&1 &
#        fi
		if [ ! -d "$installDir/run" ]; then
			mkdir "$installDir/run"
		fi
			ps aux | egrep "KamanjaManager|MetadataAPIService" | grep -v "grep" | tr -s " " | cut -d " " -f2 | tr "\\n" "," | sed "s/,$//"   > "$installDir/run/$pidfile"
#		sleep 5
EOF

# ssh -o StrictHostKeyChecking=no -T $machine 'ps aux | grep "KamanjaManager-1.0" | grep -v "grep"' > $workDir/temppid.pid
# scp  -o StrictHostKeyChecking=no   $workDir/temppid.pid "$machine:$installDir/run/node$id.pid"

done
exec 0<&12 12<&-

echo

# Adding 
# java -Xmx50g -Xms50g -Dlog4j.configurationFile=file:$targetPath/engine_log4j2.xml -Djavax.net.ssl.trustStore=/apps/projects/jks/kamanja.jks -Djavax.net.ssl.keyStore=/apps/projects/jks/kamanja.jks -Djavax.net.ssl.keyStorePassword=kamanja -Djavax.net.ssl.trustStorePassword=kamanja -jar "$installDir/bin/KamanjaManager-1.0" --config "$targetPath/$nodeCfg" < /dev/null > /dev/null 2>&1 & 

if [ "$KAMANJA_HOME" = "" ]; then
    echo "The environment variable KAMANJA_HOME must be set"
    exit
fi

echo "KAMANJA_HOME => $KAMANJA_HOME"

currentKamanjaVersion=1.6.1

java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/encryptutils_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar com.ligadata.EncryptUtils.GenerateKeys "$@"

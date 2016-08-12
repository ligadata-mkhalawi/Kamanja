KAMANJA_HOME={InstallDirectory}

currentKamanjaVersion=1.5.3

java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.11-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.SputumCodes        --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/sputumCodes_Medical.csv       --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.11-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.SmokeCodes         --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/smokingCodes_Medical.csv      --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.11-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.EnvCodes           --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/envExposureCodes_Medical.csv  --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.11-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.CoughCodes         --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/coughCodes_Medical.csv        --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/kvinit_2.11-${currentKamanjaVersion}.jar com.ligadata.tools.kvinit.KVInit --typename com.ligadata.kamanja.samples.containers.DyspnoeaCodes      --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/dyspnoea_Medical.csv          --ignorerecords 1 --deserializer "com.ligadata.kamanja.serializer.csvserdeser" --optionsjson '{"alwaysQuoteFields":false,"fieldDelimiter":",","valDelimiter":"~"}'

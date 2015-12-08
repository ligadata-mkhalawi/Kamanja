KAMANJA_HOME={InstallDirectory}

java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -jar $KAMANJA_HOME/bin/KVInit-1.0 --typename com.ligadata.kamanja.samples.containers.SputumCodes        --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/sputumCodes_Medical.csv       --keyfields icd9Code --delimiter "," --ignorerecords 1 --format "delimited"
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -jar $KAMANJA_HOME/bin/KVInit-1.0 --typename com.ligadata.kamanja.samples.containers.SmokeCodes         --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/smokingCodes_Medical.csv      --keyfields icd9Code --delimiter "," --ignorerecords 1 --format "delimited"
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -jar $KAMANJA_HOME/bin/KVInit-1.0 --typename com.ligadata.kamanja.samples.containers.EnvCodes           --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/envExposureCodes_Medical.csv  --keyfields icd9Code --delimiter "," --ignorerecords 1 --format "delimited"
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -jar $KAMANJA_HOME/bin/KVInit-1.0 --typename com.ligadata.kamanja.samples.containers.CoughCodes         --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/coughCodes_Medical.csv        --keyfields icd9Code --delimiter "," --ignorerecords 1 --format "delimited"
java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml -jar $KAMANJA_HOME/bin/KVInit-1.0 --typename com.ligadata.kamanja.samples.containers.DyspnoeaCodes      --config $KAMANJA_HOME/config/Engine1Config.properties --datafiles $KAMANJA_HOME/input/SampleApplications/data/dyspnoea_Medical.csv          --keyfields icd9Code --delimiter "," --ignorerecords 1 --format "delimited"
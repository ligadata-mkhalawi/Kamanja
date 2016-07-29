#!/usr/bin/env bash
KAMANJA_HOME={InstallDirectory}

currentKamanjaVersion=1.5.1

java -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.10-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_2.10-${currentKamanjaVersion}.jar com.ligadata.tools.SimpleKafkaProducer --gz true --topics "financeinput" --threads 1 --topicpartitions 8 --brokerlist "localhost:9092" --files "$KAMANJA_HOME/input/SampleApplications/data/TxnData_Finance.dat.gz" --partitionkeyidxs "0" --format CSV


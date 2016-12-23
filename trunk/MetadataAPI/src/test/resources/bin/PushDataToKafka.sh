#/bin/bash

# Accept the files on the command line and send them to the kafka producer.  All files assumed to be CSV.  
# Arguments:
#	topicName to use
#	compressed flag (1 = compressed 0 = uncompressed)
#	remaining args are the files to push

currentKamanjaVersion=1.5.4

if [[ $# -gt 2 ]]; then
	topicName="$1"
	shift
	useCompressed="$1"
	shift
	dataFiles="$*"
	if [ "$useCompressed" == "1" ]; then
		compressedInput="true"
	else
		compressedInput="false"
	fi

	FORMAT="CSV"

	echo java -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_2.11-${currentKamanjaVersion}.jar com.ligadata.tools.SimpleKafkaProducer --gz "$compressedInput" --topics "$topicName" --threads 1 --topicpartitions 8 --brokerlist "localhost:9092" --files "$dataFiles" --partitionkeyidxs "1" --format $FORMAT

	java -cp $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar:$KAMANJA_HOME/lib/system/simplekafkaproducer_2.11-${currentKamanjaVersion}.jar com.ligadata.tools.SimpleKafkaProducer --gz "$compressedInput" --topics "$topicName" --threads 1 --topicpartitions 8 --brokerlist "localhost:9092" --files "$dataFiles" --partitionkeyidxs "1" --format $FORMAT

else
	echo
	echo "Push data files through Kafka Input Adapter.  Supply the kafka topic name, "
	echo "a flag indicating if the data is compressed or not followed by "
	echo "an arbitrary number of input paths to be pushed through the input adapter."
	echo
	echo "Note: all files currently assumed to be CSV format."
	echo
	echo "Usage:"
	echo "   PushDataToKafka.sh <kafka topic name> <compressedInputFlag {0|1} <filepath1> [<filepath2 <filepath3> ... <filepathN>]"
	echo
fi

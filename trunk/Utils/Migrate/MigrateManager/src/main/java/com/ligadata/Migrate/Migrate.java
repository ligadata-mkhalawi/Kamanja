/*
 * Copyright 2015 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.Migrate;

import org.apache.logging.log4j.*;

import java.io.File;

import com.ligadata.MigrateBase.*;

import java.util.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;

import com.google.gson.Gson;

import java.io.FileReader;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ligadata.KamanjaVersion.KamanjaVersion;

public class Migrate {
    String loggerName = this.getClass().getName();
    Logger logger = LogManager.getLogger(loggerName);
    List<StatusCallback> statusCallbacks = new ArrayList<StatusCallback>();
    Throwable writeFailedException = null;
    boolean sentFailedStatus = false;

    class VersionConfig {
        String version = null;
        String scalaVersion = null;
        String versionInstallPath = null;
        String implemtedClass = null;
        List<String> jars = null;
        String tenantId = null;
        String adapterMessageBindings = null;

        VersionConfig() {
        }
    }

    class Configuration {
        int dataSaveThreshold = 1000;
        String clusterConfigFile = null;
        String apiConfigFile = null;
        String unhandledMetadataDumpDir = null;
        VersionConfig migratingFrom = null;
        VersionConfig migratingTo = null;
        List<String> excludeMetadata = null;
        boolean excludeData = false;
        int parallelDegree = 0;
        boolean mergeContainersAndMessages = true;

        Configuration() {
        }
    }

    List<MetadataFormat> allMetadata = new ArrayList<MetadataFormat>();

    class DataCallback implements DataObjectCallBack {
        long cntr = 0;
        MigratableTo migrateTo = null;
        List<DataFormat> collectedData = null;
        int kSaveThreshold = 0;
        String srcVer = "0";
        String dstVer = "0";
        byte[] appendData = new byte[0];
        ExecutorService executor = null;
        AtomicInteger _limiter = null;
        int _parallelDegree = 1;

        DataCallback(MigratableTo tmigrateTo, List<DataFormat> tcollectedData,
                     int tkSaveThreshold, String tsrcVer, String tdstVer, ExecutorService texecutor, AtomicInteger limiter, int tParallelDegree) {
            migrateTo = tmigrateTo;
            collectedData = tcollectedData;
            kSaveThreshold = tkSaveThreshold;
            srcVer = tsrcVer;
            dstVer = tdstVer;
            executor = texecutor;
            _limiter = limiter;
            _parallelDegree = tParallelDegree;

            if (srcVer.equalsIgnoreCase("1.1")
                    && (dstVer.equalsIgnoreCase("1.3") ||
			dstVer.substring(0,3).equalsIgnoreCase("1.4") ||
			dstVer.substring(0,3).equalsIgnoreCase("1.5")
			)) {
                appendData = new byte[8]; // timepartition bytes at the end.
                for (int i = 0; i < 8; i++)
                    appendData[0] = 0;
            }
        }

        public boolean call(DataFormat[] objData) throws Exception {
            for (DataFormat d : objData) {
                int readLen = d.data.length;

                if ((d.containerName.equalsIgnoreCase("AdapterUniqKvData"))
                        || (d.containerName.equalsIgnoreCase("GlobalCounters"))
                        || (d.containerName.equalsIgnoreCase("ModelResults"))) {
                    // Don't change any data
                } else {
                    if (appendData.length > 0) {
                        byte[] result = new byte[d.data.length + appendData.length];
                        System.arraycopy(d.data, 0, result, 0, d.data.length);
                        System.arraycopy(appendData, 0, result, d.data.length, appendData.length);
                        d.data = result;
                    }
                }

                int writeLen = d.data.length;
                logger.debug(String.format("cntr:%d, Container:%s, TimePartitionValue:%d, BucketKey:%s, TxnId:%d, RowId:%d, SerializerName:%s, DataSize:(Read:%d, Write:%d)",
                        cntr, d.containerName, d.timePartition, Arrays.toString(d.bucketKey), d.transactionid, d.rowid, d.serializername, readLen, writeLen));
                collectedData.add(d);
                cntr += 1;
            }
            if (collectedData.size() >= kSaveThreshold) {
                String msg = String.format("Adding batch of Migrated data with " + collectedData.size() + " rows to write");
                logger.debug(msg);
                sendStatus(msg, "DEBUG");

                while (_parallelDegree < _limiter.get()) {
                    // Sleep a sec and check once again
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        // Not doing anything
                    } catch (Throwable e) {
                        // Not doing anything
                    } finally {
                    }
                }

                synchronized (migrateTo) {
                    _limiter.incrementAndGet();
                    SaveDataInBackground(executor, migrateTo, collectedData.toArray(new DataFormat[collectedData.size()]), _limiter);
                    collectedData.clear();
                }
            }
            return (writeFailedException == null); // Stop if we already got some issue to write.
        }
    }

    class MdCallback implements MetadataObjectCallBack {
        public boolean call(MetadataFormat objData) throws Exception {
            logger.debug(String.format("Got Metadata => Key:%s, JsonString:%s",
                    objData.objType, objData.objDataInJson));
            allMetadata.add(objData);
            return true;
        }
    }

    class DataSaveTask implements Runnable {
        private final MigratableTo _migrateTo;
        private final DataFormat[] _data;
        private final AtomicInteger _limiter;

        public DataSaveTask(MigratableTo migrateTo, DataFormat[] data, AtomicInteger limiter) {
            _migrateTo = migrateTo;
            _data = data;
            _limiter = limiter;
        }

        public void run() {
            try {
                String msg = String.format("Migrating final batch of data with " + _data.length + " rows");
                logger.debug(msg);
                sendStatus(msg, "DEBUG");
                _migrateTo.populateAndSaveData(_data);
                msg = String.format("Migrated final batch of data with " + _data.length + " rows");
                logger.debug(msg);
                sendStatus(msg, "DEBUG");
            } catch (Exception e) {
                SetDataWritingFailure(e);
            } catch (Throwable t) {
                SetDataWritingFailure(t);
            }
            _limiter.decrementAndGet();
        }
    }

    Configuration GetConfigurationFromCfgFile(String cfgfile) {
        FileReader reader = null;
        try {
            reader = new FileReader(cfgfile);
            try {
                Gson gson = new Gson();
                Configuration cfg = gson.fromJson(reader, Configuration.class);
                logger.info("Populated migrate configuration:" + gson.toJson(cfg));
                return cfg;
            } catch (Exception e) {
                sendStatus("Failed to load configuration. Exception message:" + e.getMessage(), "ERROR");
                logger.error("Failed to load configuration", e);
            } catch (Throwable e) {
                sendStatus("Failed to load configuration. Exception message:" + e.getMessage(), "ERROR");
                logger.error("Failed to load configuration", e);
            } finally {

            }
            return null;
        } catch (Exception e) {
            sendStatus("Failed to load configuration. Exception message:" + e.getMessage(), "ERROR");
            logger.error("Failed to load configuration", e);
        } catch (Throwable e) {
            sendStatus("Failed to load configuration. Exception message:" + e.getMessage(), "ERROR");
            logger.error("Failed to load configuration", e);
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (Exception e) {
            }
        }
        return null;
    }

    Configuration GetConfigurationFromCfgJsonString(String cfgString) {
        try {
            Gson gson = new Gson();
            Configuration cfg = gson.fromJson(cfgString, Configuration.class);
            logger.info("Populated migrate configuration:" + gson.toJson(cfg));
            return cfg;
        } catch (Exception e) {
            sendStatus("Failed to load configuration. Exception message:" + e.getMessage(), "ERROR");
            logger.error("Failed to load configuration", e);
        } catch (Throwable e) {
            sendStatus("Failed to load configuration. Exception message:" + e.getMessage(), "ERROR");
            logger.error("Failed to load configuration", e);
        }
        return null;
    }

    void usage() {
        logger.warn("Usage:\n\tmigrate --config <ConfigurationJsonFile>\n" +
                "\tmigrate --version");
    }

    public void SetDataWritingFailure(Throwable e) {
        logger.error("Failed to write data", e);
        writeFailedException = e;
        if (sentFailedStatus == false && e != null) {
            sentFailedStatus = true;
            sendStatus("Data failed to migrate. Exception message:" + e.getMessage(), "ERROR"); // Send this at least once.
        }
    }

    public void SaveDataInBackground(ExecutorService executor, MigratableTo migrateTo, DataFormat[] data, AtomicInteger limiter) {
        executor.execute(new DataSaveTask(migrateTo, data, limiter));
    }

    boolean isValidPath(String path, boolean checkForDir, boolean checkForFile, String str) {
        File fl = new File(path);
        if (fl.exists() == false) {
            sendStatus("Given " + str + ":" + path + " does not exists", "ERROR");
            logger.error("Given " + str + ":" + path + " does not exists");
            return false;
        }

        if (checkForDir && fl.isDirectory() == false) {
            sendStatus("Given " + str + ":" + path + " is not directory", "ERROR");
            logger.error("Given " + str + ":" + path + " is not directory");
            return false;
        }

        if (checkForFile && fl.isFile() == false) {
            sendStatus("Given " + str + ":" + path + " is not file", "ERROR");
            logger.error("Given " + str + ":" + path + " is not file");
            return false;
        }

        return true;
    }

    public int runFromArgs(String[] args) {
        try {
            if (args.length > 0 && args[0].equalsIgnoreCase("--version")) {
                KamanjaVersion.print();
                return 0;
            }

            if (args.length != 2) {
                usage();
                return 1;
            }

            String cfgfile = "";

            if (args[0].equalsIgnoreCase("--config")) {
                cfgfile = args[1].trim();
            } else {
                sendStatus("Unknown option " + args[0], "ERROR");
                logger.error("Unknown option " + args[0]);
                usage();
                return 1;
            }

            if (cfgfile.length() == 0) {
                sendStatus("Input required config file", "ERROR");
                logger.error("Input required config file");
                usage();
                return 1;
            }

            if (isValidPath(cfgfile, false, true, "ConfigFile") == false) {
                usage();
                return 1;
            }

            Configuration configuration = GetConfigurationFromCfgFile(cfgfile);

            if (configuration == null) {
                sendStatus("Failed to get configuration from given file:" + cfgfile, "ERROR");
                logger.error("Failed to get configuration from given file:" + cfgfile);
                usage();
                return 1;
            }

            return run(configuration);
        } catch (Exception e) {
            sendStatus("Failed to Migrate. Exception message:" + e.getMessage(), "ERROR");
            logger.error("Failed to Migrate", e);
        } catch (Throwable t) {
            sendStatus("Failed to Migrate. Exception message:" + t.getMessage(), "ERROR");
            logger.error("Failed to Migrate", t);
        }
        return 1;
    }

    public int runFromJsonConfigString(String jsonConfigString) {
        try {
            if (jsonConfigString.length() == 0) {
                sendStatus("Passed invalid json string", "ERROR");
                logger.error("Passed invalid json string");
                usage();
                return 1;
            }

            Configuration configuration = GetConfigurationFromCfgJsonString(jsonConfigString);

            if (configuration == null) {
                sendStatus("Failed to get configuration from given JSON String:" + jsonConfigString, "ERROR");
                logger.error("Failed to get configuration from given JSON String:" + jsonConfigString);
                usage();
                return 1;
            }

            return run(configuration);
        } catch (Exception e) {
            sendStatus("Failed to Migrate. Exception message:" + e.getMessage(), "ERROR");
            logger.error("Failed to Migrate", e);
        } catch (Throwable t) {
            sendStatus("Failed to Migrate. Exception message:" + t.getMessage(), "ERROR");
            logger.error("Failed to Migrate", t);
        }
        return 1;
    }

    private int run(Configuration configuration) {
        MigratableFrom migrateFrom = null;
        MigratableTo migrateTo = null;
        URLClassLoader srcKamanjaLoader = null;
        URLClassLoader dstKamanjaLoader = null;
        int retCode = 1;
        boolean foundError = false;
        java.util.List<String> msgsAndContainers = null;
        java.util.List<String> catalogTables = new java.util.ArrayList<String>();
	int tablesNotFound = 0;

        try {
            catalogTables.add("adapteruniqkvdata");
            catalogTables.add("globalcounters");

            if (configuration == null) {
                sendStatus("Found invalid configuration", "ERROR");
                logger.error("Found invalid configuration");
                usage();
                return retCode;
            }

            // Version check
            String srcVer = configuration.migratingFrom.version.trim();
            String dstVer = configuration.migratingTo.version.trim();
            String scalaFrom = configuration.migratingFrom.scalaVersion.trim();
            String scalaTo = configuration.migratingTo.scalaVersion.trim();
            if (configuration.migratingTo.tenantId == null || configuration.migratingTo.tenantId.trim().length() == 0) {
                sendStatus("Not found tenantId", "ERROR");
                logger.error("Not found tenantId");
                usage();
                return retCode;
            }

            if (configuration.migratingTo.adapterMessageBindings == null || configuration.migratingTo.adapterMessageBindings.trim().length() == 0) {
		if( srcVer.equalsIgnoreCase("1.1") ||
		    srcVer.equalsIgnoreCase("1.2") || srcVer.equalsIgnoreCase("1.3") ){
		    sendStatus("Not found adapterMessageBindings", "ERROR");
		    logger.error("Not found adapterMessageBindings");
		}
            }

            String tenantId = configuration.migratingTo.tenantId.trim();
            String adapterMessageBindings = configuration.migratingTo.adapterMessageBindings.trim();

            if (srcVer.equalsIgnoreCase("1.1") == false
                    && srcVer.equalsIgnoreCase("1.2") == false
                    && srcVer.equalsIgnoreCase("1.3") == false
		&& srcVer.substring(0,3).equalsIgnoreCase("1.4") == false
		&& srcVer.substring(0,3).equalsIgnoreCase("1.5") == false
		&& srcVer.substring(0,3).equalsIgnoreCase("1.6") == false
		) {
                sendStatus("We support source versions only 1.1 or 1.2 or 1.3 or 1.4 or 1.4.1 or 1.4.3 or 1.5.x or 1.6.x. We don't support " + srcVer, "ERROR");
                logger.error("We support source versions only 1.1 or 1.2 or 1.3 or 1.4 or 1.4.1 or 1.4.3 or 1.5.x or 1.6.x. We don't support " + srcVer);
                usage();
                return retCode;
            }

            if (dstVer.equalsIgnoreCase("1.6.2") == false) {
                sendStatus("We support destination version only 1.6.2. We don't support " + dstVer, "ERROR");
                logger.error("We support destination version only 1.6.2. We don't support " + dstVer);
                usage();
                return retCode;
            }

            if (scalaFrom.equalsIgnoreCase("2.10") == false &&
                    scalaFrom.equalsIgnoreCase("2.11") == false) {
                sendStatus("We support source scala version only 2.10 or 2.11. Given:" + scalaFrom, "ERROR");
                logger.error("We support source scala version only 2.10 or 2.11. Given:" + scalaFrom);
                usage();
                return retCode;
            }

            if (scalaTo.equalsIgnoreCase("2.10") == false && scalaTo.equalsIgnoreCase("2.11") == false) {
                sendStatus("We support destination scala version only 2.10 or 2.11. Given:" + scalaTo, "ERROR");
                logger.error("We support destination scala version only 2.10 or 2.11. Given:" + scalaTo);
                usage();
                return retCode;
            }

            String backupTblSufix = "_migrate_" + srcVer.replace('.', '_').trim() + "_to_" + dstVer.replace('.', '_').trim() + "_bak";

            String curMigrationUnhandledMetadataDumpDir = "";
            String curMigrationSummary = "";
            String sourceReadFailuresFilePath = "";

            if (isValidPath(configuration.unhandledMetadataDumpDir, true,
                    false, "unhandledMetadataDumpDir")) {
                String dirExtn = new java.text.SimpleDateFormat(
                        "yyyyMMddHHmmss").format(new java.util.Date(System
                        .currentTimeMillis()));

                String newDir = configuration.unhandledMetadataDumpDir
                        + "/Migrate_" + dirExtn;

                File fl = new File(newDir);

                if (fl.mkdir()) {
                    curMigrationUnhandledMetadataDumpDir = fl.getAbsolutePath();
                    curMigrationSummary = configuration.unhandledMetadataDumpDir
                            + "/MigrateSummary_" + dirExtn + ".log";
                    sourceReadFailuresFilePath = configuration.unhandledMetadataDumpDir
                            + "/ReadFailures_" + dirExtn + ".err";
                    logger.info("Current Migration Changes are writing into "
                            + curMigrationUnhandledMetadataDumpDir
                            + " directory. And summary is writing into "
                            + curMigrationSummary
                            + " and source read failures are writing into "
                            + sourceReadFailuresFilePath);
                } else {
                    sendStatus("Failed to create directory " + newDir, "ERROR");
                    logger.error("Failed to create directory " + newDir);
                    usage();
                    return retCode;
                }
            } else {
                usage();
                return retCode;
            }


            // From Srouce version 1.1,1.2,1.3 to Destination version 1.6.2 we do both
            // Metadata Upgrade & Data Upgrade
            // From Source Version 1.3,1.4 to Destination version 1.6.2, we only do
            // Metadata Upgrade.
            boolean canUpgradeMetadata = ((srcVer.equalsIgnoreCase("1.1") ||
                    srcVer.equalsIgnoreCase("1.2") ||
                    srcVer.equalsIgnoreCase("1.3") ||
		    srcVer.substring(0,3).equalsIgnoreCase("1.4")
					   ) &&
                    dstVer.equalsIgnoreCase("1.6.2"));

            boolean canUpgradeData = ((srcVer.equalsIgnoreCase("1.1") ||
                    srcVer.equalsIgnoreCase("1.2") ||
                    srcVer.equalsIgnoreCase("1.3")) &&
                    dstVer.equalsIgnoreCase("1.6.2"));

	    boolean canUpgradeClusterConfig =
		((srcVer.substring(0,3).equalsIgnoreCase("1.4") ||
		  srcVer.substring(0,3).equalsIgnoreCase("1.5") ||
		  srcVer.substring(0,3).equalsIgnoreCase("1.6")) &&
                    dstVer.substring(0,3).equalsIgnoreCase("1.6"));

            if (canUpgradeData && canUpgradeMetadata == false) {
                sendStatus("We don't support upgrading only data without metadata at this moment", "ERROR");
                logger.error("We don't support upgrading only data without metadata at this moment");
                usage();
                return retCode;
            }

	    logger.debug("Metadata Upgrade      => " + canUpgradeMetadata);

            // Modify canUpgradeData depending on exclude flag
            canUpgradeData = (canUpgradeData && configuration.excludeData == false);

	    logger.debug("Data Upgrade           => " + canUpgradeData);
	    logger.debug("Cluster Config Upgrade => " + canUpgradeClusterConfig);

            int srcJarsCnt = configuration.migratingFrom.jars.size();
            URL[] srcLoaderUrls = new URL[srcJarsCnt];

            int idx = 0;
            for (String jar : configuration.migratingFrom.jars) {
                logger.debug("Migration From URL => " + jar);
                srcLoaderUrls[idx++] = new File(jar).toURI().toURL();
            }

            srcKamanjaLoader = new URLClassLoader(srcLoaderUrls);

            Class<?> srcClass = srcKamanjaLoader
                    .loadClass(configuration.migratingFrom.implemtedClass);

            Constructor<?> srcConstructor = srcClass.getConstructor();
            Object tmpSrcObj = srcConstructor.newInstance();
            if (tmpSrcObj instanceof MigratableFrom) {
                migrateFrom = (MigratableFrom) tmpSrcObj;
            } else {
                String msg = String.format("Failed to Load Source. Version:%s, migrateFromClass:%s, InstallPath:%s",
                        configuration.migratingFrom.version,
                        configuration.migratingFrom.implemtedClass,
                        configuration.migratingFrom.versionInstallPath);
                sendStatus(msg, "ERROR");
                logger.error(msg);
                foundError = true;
            }

            int dstJarsCnt = configuration.migratingTo.jars.size();
            URL[] dstLoaderUrls = new URL[dstJarsCnt];

            idx = 0;
            for (String jar : configuration.migratingTo.jars) {
                logger.debug("Migration To URL => " + jar);
                dstLoaderUrls[idx++] = new File(jar).toURI().toURL();
            }

            dstKamanjaLoader = new URLClassLoader(dstLoaderUrls);

            Class<?> dstClass = dstKamanjaLoader
                    .loadClass(configuration.migratingTo.implemtedClass);

            Constructor<?> dstConstructor = dstClass.getConstructor();
            Object tmpDstObj = dstConstructor.newInstance();
            if (tmpDstObj instanceof MigratableTo) {
                migrateTo = (MigratableTo) tmpDstObj;
            } else {
                String msg = String.format("Failed to Load Destination. Version:%s, migrateToClass:%s",
                        configuration.migratingTo.version,
                        configuration.migratingTo.implemtedClass);

                sendStatus(msg, "ERROR");
                logger.error(msg);
                foundError = true;
            }

            if (foundError == false) {
                String cfgMsg = String.format("apiConfigFile:%s, clusterConfigFile:%s", configuration.apiConfigFile, configuration.clusterConfigFile);
                sendStatus("Initializing MigrationTo using " + cfgMsg, "INFO");
                logger.info(cfgMsg);
                migrateTo.init(configuration.migratingTo.versionInstallPath,
                        configuration.apiConfigFile,
                        configuration.clusterConfigFile,
                        srcVer,
                        curMigrationUnhandledMetadataDumpDir,
                        curMigrationSummary,
                        configuration.parallelDegree,
                        configuration.mergeContainersAndMessages,
                        scalaFrom,
                        scalaTo,
                        tenantId,
                        adapterMessageBindings);

                String metadataStoreInfo = migrateTo.getMetadataStoreInfo();
                String dataStoreInfo = migrateTo.getDataStoreInfo();
                String statusStoreInfo = migrateTo.getStatusStoreInfo();

                String dbsMsg = String.format("Got Datastores Information\n\tmetadataStoreInfo:%s\n\tdataStoreInfo:%s\n\tstatusStoreInfo:%s", metadataStoreInfo, dataStoreInfo, statusStoreInfo);
                logger.info(dbsMsg);
                sendStatus(dbsMsg, "INFO");
                migrateFrom.init(configuration.migratingFrom.versionInstallPath,
                        metadataStoreInfo, dataStoreInfo, statusStoreInfo,
                        sourceReadFailuresFilePath);

                String[] excludeMetadata = new String[0];
                if (configuration.excludeMetadata != null
                        && configuration.excludeMetadata.size() > 0) {
                    excludeMetadata = configuration.excludeMetadata
                            .toArray((new String[configuration.excludeMetadata
                                    .size()]));
                }

                TableName[] allMetadataTbls = new TableName[0];
                List<TableName> allDataTbls = new ArrayList<TableName>();
                TableName[] allStatusTbls = new TableName[0];

                if (canUpgradeMetadata) {
                    allMetadataTbls = migrateFrom.getAllMetadataTableNames();
                    logger.debug("Getting metadata from old version");
                    sendStatus("Getting metadata from old version", "DEBUG");
                    // Special Processing if source version > 1.1
                    // Read metadata_objects to figure out names of messages and containers
                    // We need this so we can identify the tableNames for messages and containers
                    // In 1.1 all the data objects are stored in a single table called "AllDATA"
                    // metadata_objects table is not backed up at this moment
                    // Just use the original table (backupTblSuffix => "")
                    if (!srcVer.equalsIgnoreCase("1.1")) {
                        // MdCallback fills a structure called allMetadata
                        migrateFrom.getAllMetadataObjs("", new MdCallback(), excludeMetadata);
                        logger.debug("Got all metadata");
                        sendStatus("Got all metadata", "DEBUG");
                    }
                }

                if (canUpgradeData) {
                    if (srcVer.equalsIgnoreCase("1.1")) {
                        // all the data tables are known in 1.1. "AllData", "ClusterCounts"
                        TableName[] tbls = migrateFrom.getAllDataTableNames();
                        for (TableName tbl : tbls) {
                            allDataTbls.add(tbl);
                        }
                    } else {
                        // Use the contents of metadata_objects o figure out
                        // the table names
                        MetadataFormat[] metadataArr = allMetadata
                                .toArray(new MetadataFormat[allMetadata.size()]);

                        for (MetadataFormat mdf : metadataArr) {
                            logger.info("objType => " + mdf.objType);
                            logger.info("objDataInJson => " + mdf.objDataInJson);
                        }
                        msgsAndContainers = migrateTo.getMessagesAndContainers(metadataArr, true, excludeMetadata);


                        for (String tblName : catalogTables) {
                            logger.info("TableName => " + tblName);
                            String schemaName = migrateTo.getTenantTableSchemaName();
                            String tableName = migrateTo.getDataTableName(tblName);
                            TableName tInfo = new TableName(schemaName, tableName);
                            allDataTbls.add(tInfo);
                        }

                        for (String msgName : msgsAndContainers) {
                            logger.info("Message => " + msgName);
                            String schemaName = migrateTo.getTenantTableSchemaName();
                            String tableName = migrateTo.getDataTableName(msgName);
                            TableName tInfo = new TableName(schemaName, tableName);
                            allDataTbls.add(tInfo);
                        }
                    }
                    allStatusTbls = migrateFrom.getAllStatusTableNames();
                }

                List<BackupTableInfo> metadataBackupTbls = new ArrayList<BackupTableInfo>();
                List<BackupTableInfo> dataBackupTbls = new ArrayList<BackupTableInfo>();
                List<BackupTableInfo> statusBackupTbls = new ArrayList<BackupTableInfo>();

                List<TableName> metadataDelTbls = new ArrayList<TableName>();
                List<TableName> dataDelTbls = new ArrayList<TableName>();
                List<TableName> statusDelTbls = new ArrayList<TableName>();

                boolean allTblsBackedUp = true;
                int foundMdTablesWhichBackedUp = 0;
                int foundMdTablesWhichDidnotBackedUp = 0;

                sendStatus("Checking backup status in table", "INFO");
                String backupStatusStr = "";
                try {
                    backupStatusStr = migrateTo.getStatusFromDataStore("BackupStatusFor" + backupTblSufix);
                }
		catch (Exception e) {
                }
		catch (Throwable t) {
                }

                boolean havePreviousBackup = (backupStatusStr.startsWith("Done @20"));

                sendStatus("Checking whether backup is already done or not", "INFO");
                logger.info("Checking whether backup is already done or not");

		if( allMetadataTbls.length > 0 ){
		    for (TableName tblInfo : allMetadataTbls) {
			BackupTableInfo bkup = new BackupTableInfo(tblInfo.namespace,
								   tblInfo.name,
								   tblInfo.name + backupTblSufix);

			if (migrateTo.isMetadataTableExists(tblInfo)) {
			    if (migrateTo.isMetadataTableExists(new TableName(tblInfo.namespace, bkup.dstTable)) == false) {
				allTblsBackedUp = false;
			    }
			    metadataBackupTbls.add(bkup);
			    metadataDelTbls.add(tblInfo);
			} else {
			    // If main table does not exists and backup table does not exists mean there is some issue with getting tables or something like that
			    if (migrateTo.isMetadataTableExists(new TableName(tblInfo.namespace, bkup.dstTable))) {
				foundMdTablesWhichBackedUp += 1;
			    } else {
				foundMdTablesWhichDidnotBackedUp += 1;
			    }
			}
		    }
		}
		else{
		    if( canUpgradeMetadata ){
			logger.error("No metadata tables found, Possibly connecting to incorrect source database, Check ClusterCfgMetadataAPIConfig.properties");
		    }
		}
                if (foundMdTablesWhichDidnotBackedUp > 0 && foundMdTablesWhichBackedUp == 0) {
                    // Not really found tables to backup
                    sendStatus("Did not find any metadata table and also not found any backed up tables.", "ERROR");
                    throw new Exception("Did not find any metadata table and also not found any backed up tables.");
                }

                // Back up data tables
		if( allDataTbls.size() > 0 ){
		    tablesNotFound = 0;
		    for (TableName tblInfo : allDataTbls) {
			String fullTableName = tblInfo.namespace + "." + tblInfo.name;
			logger.info("Check the backup status for the table " + fullTableName);
			BackupTableInfo bkup = new BackupTableInfo(tblInfo.namespace,
								   tblInfo.name, tblInfo.name + backupTblSufix);
			if (migrateTo.isDataTableExists(tblInfo)) {
			    if (migrateTo.isDataTableExists(new TableName(
									  tblInfo.namespace, bkup.dstTable)) == false) {
				allTblsBackedUp = false;
			    }
			    dataBackupTbls.add(bkup);
			    dataDelTbls.add(tblInfo);
			} else {
			    logger.info("The table " + fullTableName + " doesn't exist ");
			    if (!srcVer.equalsIgnoreCase("1.1")) {
				tablesNotFound = tablesNotFound + 1;
			    }
			}
		    }
		}
		else{
		    if( canUpgradeData ){
			logger.error("No data tables found, Possibly connecting to incorrect source database, Check ClusterCfgMetadataAPIConfig.properties");
		    }
		}

                // FIX: Revisit this about what do you do if one or more data tables do not exist
                if (!srcVer.equalsIgnoreCase("1.1")) {
                    if (tablesNotFound > 0) {
                        logger.error("Unable to find one or more tables, but continue ..");
                    }
                    if (tablesNotFound > 0 && tablesNotFound == allDataTbls.size()) {
                        logger.error("Did not find even one data table, but continue ..");
                    }
                }

		if( allStatusTbls.length > 0 ){
		    for (TableName tblInfo : allStatusTbls) {
			BackupTableInfo bkup = new BackupTableInfo(tblInfo.namespace,
								   tblInfo.name, tblInfo.name + backupTblSufix);
			if (migrateTo.isStatusTableExists(tblInfo)) {
			    if (migrateTo.isStatusTableExists(new TableName(
									    tblInfo.namespace, bkup.dstTable)) == false) {
				allTblsBackedUp = false;
			    }
			    statusBackupTbls.add(bkup);
			    statusDelTbls.add(tblInfo);
			}
		    }
		}
		else{
		    logger.info("No status tables found...");
		}

                String backupTblsString = "Backup tables:";

                if (havePreviousBackup) {
                    int pos = backupStatusStr.indexOf(backupTblsString);
                    String done = "";
                    String backupTblStr = "";
                    if (pos >= 0) {
                        done = backupStatusStr.substring(0, pos);
                        backupTblStr = backupStatusStr.substring(pos);
                    } else {
                        done = backupStatusStr;
                    }
                    String msg = "Found backup " + done + ". Using it to do rest of the migration.\nPrevious Backup tables Information:\n" + backupTblStr;
                    sendStatus(msg, "INFO");
                    logger.info(msg);
                } else {
                    if (allTblsBackedUp) {
                        sendStatus("Looks like all tables backup started but not completed. Going to backup again", "INFO");
                        logger.info("Looks like all tables backup started but not completed. Going to backup again");
                    }

                    // Backup all the tables, if we did not done or finish before
		    if( canUpgradeMetadata || canUpgradeData ) {
			StringBuilder sb = new StringBuilder();
			sb.append(backupTblsString + "\n");
			sb.append("\tMetadata Tables:{");
			for (BackupTableInfo bTbl : metadataBackupTbls) {
			    sb.append("\t\t(" + bTbl.namespace + "," + bTbl.srcTable + ") => (" + bTbl.namespace + "," + bTbl.dstTable + ")\n");
			}
			sb.append("}\n");

			sb.append("\tData Tables:{");
			for (BackupTableInfo bTbl : dataBackupTbls) {
			    sb.append("\t\t(" + bTbl.namespace + "," + bTbl.srcTable + ") => (" + bTbl.namespace + "," + bTbl.dstTable + ")\n");
			}
			sb.append("}\n");

			sb.append("\tStatus Tables:{");
			for (BackupTableInfo bTbl : statusBackupTbls) {
			    sb.append("\t\t(" + bTbl.namespace + "," + bTbl.srcTable + ") => (" + bTbl.namespace + "," + bTbl.dstTable + ")\n");
			}
			sb.append("}\n");

			String backupTblStr = sb.toString();
			sendStatus(backupTblStr, "INFO");
			logger.info(backupTblStr);

			migrateTo.backupAllTables(metadataBackupTbls.toArray(new BackupTableInfo[metadataBackupTbls.size()]),
						  dataBackupTbls.toArray(new BackupTableInfo[dataBackupTbls.size()]),
						  statusBackupTbls.toArray(new BackupTableInfo[statusBackupTbls.size()]), true);
			String doneTm = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis()));

			migrateTo.setStatusFromDataStore("BackupStatusFor" + backupTblSufix, "Done @" + doneTm + backupTblStr);
			sendStatus("Completed backing up. " + doneTm, "INFO");
			logger.info("Completed backing up. " + doneTm);
		    }

		    // If the source version is 1.4, we can make the necessary updates to
		    // metadata objects  directly in the database. We let the migrateTo
		    // handle those updates and return immediately. There is no need
		    // to update anything else
		    if (srcVer.substring(0,3).equalsIgnoreCase("1.4")) {
			if (canUpgradeMetadata) {
			    logger.debug("Ugrade models to new version");
			    sendStatus("Upgrade models to new version", "DEBUG");
			    MetadataFormat[] metadataArr = allMetadata
				.toArray(new MetadataFormat[allMetadata.size()]);
			    msgsAndContainers = migrateTo.addMetadata(metadataArr, true, excludeMetadata);
			    logger.debug("Done adding metadata to new version");
			    sendStatus("Done adding metadata to new version", "DEBUG");
			}
			return 0;
		    }

		    if(canUpgradeData || canUpgradeMetadata ){
			StringBuilder sb = new StringBuilder();
			sb.append("Dropping tables:\n");
			sb.append("\tMetadata Tables:{");
			for (TableName dTbl : metadataDelTbls) {
			    sb.append("(" + dTbl.namespace + "," + dTbl.name + ")");
			}
			sb.append("}\n");

			sb.append("\tData Tables:{");
			for (TableName dTbl : dataDelTbls) {
			    sb.append("(" + dTbl.namespace + "," + dTbl.name + ")");
			}
			sb.append("}\n");

			sb.append("\tStatus Tables:{");
			for (TableName dTbl : statusDelTbls) {
			    sb.append("(" + dTbl.namespace + "," + dTbl.name + ")");
			}
			sb.append("}\n");

			String delTblStr = sb.toString();
			sendStatus(delTblStr, "INFO");
			logger.info(delTblStr);

			// Drop all tables after backup
			migrateTo.dropAllTables(metadataDelTbls.toArray(new TableName[metadataDelTbls.size()]), dataDelTbls.toArray(new TableName[dataDelTbls.size()]), statusDelTbls.toArray(new TableName[statusDelTbls.size()]));
			sendStatus("Completed dropping tables", "INFO");
			logger.info("Completed dropping tables");
		    }

		    // 1.4.0 change. Because of the way we save keys in metadata tables
		    // is different from non-metadata tables, we need to create tabels
		    // ahead.
		    if (canUpgradeMetadata){
			logger.info("Create metadata tables again");
			migrateTo.createMetadataTables();
		    }

		    // For 1.2 and 1.3, clear allMetadata
		    if (!srcVer.equalsIgnoreCase("1.1")) {
			allMetadata.clear();
		    }

		    if (canUpgradeMetadata) {
			logger.debug("Getting metadata from old version");
			sendStatus("Getting metadata from old version", "DEBUG");
			migrateFrom.getAllMetadataObjs(backupTblSufix,
						       new MdCallback(), excludeMetadata);
			logger.debug("Got all metadata");
			sendStatus("Got all metadata", "DEBUG");
		    }

		    MetadataFormat[] metadataArr = allMetadata
                        .toArray(new MetadataFormat[allMetadata.size()]);

		    if (canUpgradeData) {
			logger.info("Dropping saved messages/container tables if there are any");
			sendStatus("Dropping saved messages/container tables if there are any", "DEBUG");
			migrateTo.dropMessageContainerTablesFromMetadata(metadataArr);
			logger.info("Dropped saved messages/container tables if there are any");
			sendStatus("Dropped saved messages/container tables if there are any", "DEBUG");
		    }

		    msgsAndContainers = null;
		    if (canUpgradeMetadata) {
			logger.debug("Adding metadata to new version");
			sendStatus("Adding metadata to new version", "DEBUG");
			msgsAndContainers = migrateTo.addMetadata(metadataArr, true, excludeMetadata);

			FailedMetadataKey[] failedMetadataKeys = migrateTo.getFailedMetadataKeys();
			if (failedMetadataKeys != null && failedMetadataKeys.length > 0) {
			    StringBuilder sb = new StringBuilder();
			    sb.append("Failed Metadata Keys:{\n");
			    for (int i = 0; i < failedMetadataKeys.length; i++) {
				FailedMetadataKey key = failedMetadataKeys[i];
				sb.append("\t(" + key.objType + "," + key.mdKey + "," + key.version + ")\n");
			    }
			    sb.append("}\n");
			    String failedMdKeys = sb.toString();
			    logger.error(failedMdKeys);
			    sendStatus(failedMdKeys, "ERROR");
			}
			logger.debug("Done adding metadata to new version");
			sendStatus("Done adding metadata to new version", "DEBUG");
		    } else {
			logger.debug("No need to migrate metadatadata");
			sendStatus("No need to migrate metadatadata", "DEBUG");
		    }

		    if (canUpgradeData) {
			if (msgsAndContainers != null) {
			    StringBuilder sb = new StringBuilder();
			    sb.append("MessagesAndContainers To create Tables from Data Migration:{\n");
			    for (String cName : msgsAndContainers) {
				sb.append("\t" + cName + "\n");
			    }
			    sb.append("}\n");

			    String newTablesStr = sb.toString();
			    sendStatus(newTablesStr, "INFO");
			    logger.info(newTablesStr);
			}
			int parallelDegree = 1;
			if (configuration.parallelDegree > 1)
			    parallelDegree = configuration.parallelDegree;
			ExecutorService executor = Executors.newFixedThreadPool(parallelDegree);
			AtomicInteger limiter = new AtomicInteger(0);
			int kSaveThreshold = 1024;

			if (configuration.dataSaveThreshold > 0)
			    kSaveThreshold = configuration.dataSaveThreshold;

			logger.debug("Migrating data from old version to new version. Each time we are writing minimum " + kSaveThreshold + " rows as a batch.");
			sendStatus("Migrating data from old version to new version. Each time we are writing minimum " + kSaveThreshold + " rows as a batch.", "DEBUG");
			List<DataFormat> collectedData = new ArrayList<DataFormat>();

			DataCallback dataCallback = new DataCallback(migrateTo,
								     collectedData, kSaveThreshold, srcVer, dstVer, executor, limiter, parallelDegree);
			migrateFrom.getAllDataObjs(backupTblSufix, metadataArr, msgsAndContainers, catalogTables, dataCallback);


			if (collectedData.size() > 0) {
			    String msg = String.format("Adding final batch of Migrated data with " + collectedData.size() + " rows to write");
			    logger.debug(msg);
			    sendStatus(msg, "DEBUG");

			    synchronized (migrateTo) {
				limiter.incrementAndGet();
				SaveDataInBackground(executor, migrateTo, collectedData.toArray(new DataFormat[collectedData.size()]), limiter);
				collectedData.clear();
			    }
			}

			logger.info("Waiting to flush all data");
			sendStatus("Waiting to flush all data", "INFO");
			executor.shutdown();
			executor.awaitTermination(86400, TimeUnit.SECONDS); // 1 day waiting at the max

			if (writeFailedException != null) {
			    if (sentFailedStatus == false) {
				// logger.error("Data failed to migrate.", writeFailedException);
				sendStatus("Data failed to migrate. Exception message:" + writeFailedException.getMessage(), "ERROR");
			    }
			    throw writeFailedException;
			}

			logger.info("Completed migrating data");
			sendStatus("Completed migrating data", "INFO");
		    } else {
			logger.debug("Skipping data migration. May not be required or turned off");
			sendStatus("Skipping data migration. May not be required or turned off", "DEBUG");
		    }
		}

		// uploadClusterConfig for special scenarios
		if( ! canUpgradeMetadata && canUpgradeClusterConfig ){
		    migrateTo.uploadClusterConfig();
		}

		logger.info("Migration is done. Failed summary is written to "
			    + curMigrationSummary + " and failed to read data written to " + sourceReadFailuresFilePath);
		if (logger.isInfoEnabled() == false)
		    System.out.println("Migration is done. Failed summary is written to "
				       + curMigrationSummary + " and failed to read data written to " + sourceReadFailuresFilePath);
		sendStatus("Successfully migrated. Failed summary is written to " + curMigrationSummary + " and failed to read data written to " + sourceReadFailuresFilePath, "INFO");
		retCode = 0;
	    }
        } catch (Exception e) {
            logger.error("Failed to Migrate", e);
            sendStatus("Failed to migrate with exception message:" + e.getMessage(), "ERROR");
        } catch (Throwable t) {
            logger.error("Failed to Migrate", t);
            sendStatus("Failed to migrate with throwable message:" + t.getMessage(), "ERROR");
        } finally {
            if (migrateFrom != null)
                migrateFrom.shutdown();
            if (migrateTo != null)
                migrateTo.shutdown();
            try {
                if (srcKamanjaLoader != null)
                    srcKamanjaLoader.close();
            } catch (Exception e) {
            } catch (Throwable t) {
            }

            try {
                if (dstKamanjaLoader != null)
                    dstKamanjaLoader.close();
            } catch (Exception e) {
            } catch (Throwable t) {
            }
        }
        return retCode;
    }

    public void registerStatusCallback(StatusCallback callback) {
        if (callback != null) {
            statusCallbacks.add(callback);
        }
    }

    private void sendStatus(String statusText, String typStr) {
        for (StatusCallback callback : statusCallbacks) {
            callback.call(statusText, typStr);
        }
    }

    public static void main(String[] args) {
        System.exit(new Migrate().runFromArgs(args));
    }
}


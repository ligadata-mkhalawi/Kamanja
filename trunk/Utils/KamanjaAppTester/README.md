# Kamanja Application Tester

### Overview

  The Kamanja Application Tester is designed to allow the user to configure a drop-in application for testing. Each application should include the metadata (Container Definitions, Message Definitions, Model Definitions), configuration (excluding cluster configuration), input data for testing (including data for populating lookup tables), expected output data (or results data) and an application configuration file that indicates the metadata elements being added and the data being used for testing.
  
  When the tests are actually run, all services required for testing will be run in an embedded fashion. This relieves the user of the need to setup an environment for test so they can focus on testing the application itself rather than the environment it's being run in.

### Kamanja Application Directory Structure

  Kamanja applications to be tested are automatically recognized by dropping them in to the test directly located in your Kamanja isntallation directory. For example, if you have installed Kamanja in "*/opt/Kamanja*", then any applications you want tested should be placed in "*/opt/Kamanja/test*".
  While the name of the directory containing the application is up to the user, the directory structure of a test application is fixed, meaning the application must conform to a specific structure. Assuming the name of your application directory is CustomerTransaction, the directory structure would be as follows:
  * opt
    * Kamanja
      * test
        * CustomerTransactionApplication
          * ApplicationConfiguration.json - This name is fixed. Absense of this configuration file will result in the application not being tested.
          * data - The location of your test input data, expected results, and lookup table data
          * metadata - The location of your metadata elements
            * configuration - The location of your configuration files such as adapter message bindings and model configuration.
            * container - The location of your container definitions.
            * message - The location of your message definitions.
            * model - The location of your model definitions.
            
  Any files not in their proper location will either not be found or fail to be loaded properly.
  
### Kamanja Application Configuration

  The application configuration file dictates what metadata is added, what order it is added in, if lookup tables should be populated, what input data to test with and what that input data's expected results should be. This file must be named either ApplicationConfiguration.json or AppConfig.json. Any other file names will not be accepted as proper configuration files. An example configuration looks like this:
  
  ```
  {
  "Application": {
    "Name": "TestApp1",
    "MetadataElements": [
      {
        "Type": "Container",
        "Filename": "testApp1Container.json",
        "KVFile": "testApp1ContainerData.csv"
      },
      {
        "Type": "Message",
        "Filename": "testApp1Message.json"
      },
      {
        "Type": "Message",
        "Filename": "testApp1OutputMessage.json"
      },
      {
        "Type": "ModelConfiguration",
        "Filename": "testApp1ModelConfiguration.json"
      },
      {
        "Type": "Model",
        "ModelType": "Scala",
        "Filename": "testApp1Model.scala",The
        "ModelConfiguration": "TestApp1ModelConfiguration"
      },
      {
        "Type": "AdapterMessageBindings",
        "Filename": "testApp1AdapterMessageBindings.json"
      }
    ],
    "DataSets": [
      {
        "InputDataFile": "testApp1InputFile.csv",
        "InputDataFormat": "CSV",
        "ExpectedResultsFile": "testApp1ExpectedResults.csv",
        "ExpectedResultsFormat": "CSV",
        "PartitionKey": "1"
      }
    ]
  }
}
  ```
  Application Configuration Elements:
  * Header - This is where you see "Application" at the top. This indicates the json structure of an "Application".
  * Name - This is the name you've given the application. This name is arbitrary and will be used only for logging purposes.
  * MetadataElements - This is a list of metadata elements you would like to add (or lookup table you'd like to populate).
    * Type - The type of metadata. Valid types are:
      * Container
      * Message
      * Model
      * ModelConfiguration
    * Filename - The name of the file containing the definition. You should not include the full path as the file will always be searched for in the predefined location for that metadata type as mentioned under section **Kamanja Application Directory Strcuture**
    * KVFile - This is a configuration element specific to metadata element type "*Container*". After the container is added, the kvfile specified will be searched for (in the data directory) and a lookup table will be populated.
    * ModelType - ModelType determines what type of model is being added. Valid choices are as follows:
      * Scala
      * Java
      * KPMML **NOT SUPPORTED YET**
      * PMML **NOT SUPPORTED YET**
      * Python **NOT SUPPORTED YET**
    * ModelConfiguration - This is a configuration element specific to the element type "*ModelType*" with subtypes "*Scala*" and "*Java*". This should be the name of the model configuration given in a previously loaded ModelConfiguration file.

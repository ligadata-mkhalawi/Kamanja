package com.ligadata.test.configuration.cluster.python

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

case class PythonConfiguration(serverBasePort: Int = com.ligadata.test.utils.TestUtils.getAvailablePort,
                               serverPortLimit: Int = 40,
                               serverHost: String = "localhost",
                               pythonPath: String,
                               pythonBinDir: String,
                               pythonLogConfigPath: String,
                               pythonLogPath: String
                              ) {
  override def toString: String = {
    //TODO: Need to convert this, along with all other cluster configuration related toString methods to a cleaner method of generating json
    /*
    val json =
      ("PYTHON_CONFIG" ->
        ("PYTHON_PATH" -> pythonPath) ~
        ("SERVER_BASE_PORT" -> serverBasePort) ~
        ("SERVER_PORT_LIMIT" -> serverPortLimit) ~
        ("SERVER_HOST" -> serverHost) ~
        ("PYTHON_LOG_CONFIG_PATH" -> logConfigPath) ~
        ("PYTHON_LOG_PATH" -> pythonLogPath) ~
        ("PYTHON_BIN_DIR" -> pythonBinDir)
      )

    return pretty(render(json))
    */

    val builder = new StringBuilder
    builder.append(s""""PYTHON_CONFIG": { """ + "\n")
    builder.append(s"""   "PYTHON_PATH": "$pythonPath",""" + "\n")
    builder.append(s"""   "SERVER_BASE_PORT": $serverBasePort,""" + "\n")
    builder.append(s"""   "SERVER_PORT_LIMIT": $serverPortLimit,""" + "\n")
    builder.append(s"""   "SERVER_HOST": "$serverHost",""" + "\n")
    builder.append(s"""   "PYTHON_LOG_CONFIG_PATH": "$pythonLogConfigPath",""" + "\n")
    builder.append(s"""   "PYTHON_LOG_PATH": "$pythonLogPath",""" + "\n")
    builder.append(s"""   "PYTHON_BIN_DIR": "$pythonBinDir"""" + "\n")
    builder.append(s"}")
    return builder.toString()
  }
}

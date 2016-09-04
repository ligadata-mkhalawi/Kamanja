import os
import os.path
import json
from common.CommandBase import CommandBase
import logging
import logging.config
import logging.handlers

class addModel(CommandBase): 
	"""
	The addModel command ...uh... adds models to the server.
	"""

	def __init__(self, pkgCmdName, host, port , logger):
		super(addModel, self).__init__(pkgCmdName, host, port, logger)

	def handler(self, modelDict, host, port, cmdOptions, modelOptions):
		if "ModelName" in cmdOptions:
			modelName = str(cmdOptions["ModelName"])
		else:
			modelName = ""
		#
		if "Module" in cmdOptions:
			modelFileName = str(cmdOptions["Module"]) + ".py"
		else:
			modelFileName = "noModuleNameWasGiven.py"
		#
                if self.logger.isEnabledFor(logging.DEBUG): 
		   self.logger.debug("Entered addModel... model to be added = {} ... file = {}".format(modelName,modelFileName))
		
		pypath = modelDict["PythonInstallPath"]
		modelSrcPath = "{}/models/{}".format(pypath,modelFileName)
                if self.logger.isEnabledFor(logging.DEBUG): 
		   self.logger.debug("addModel.handler entered ... modelSrcPath = {}".format(modelSrcPath))

		result = ""
		inputfields = ""
		outputfields = ""
		reasonablePath = os.path.exists(modelSrcPath) and os.path.isfile(modelSrcPath) and modelName != "" and modelFileName != "noModuleNameWasGiven.py"
		if reasonablePath:
			#(parentDir, file) = os.path.split(modelSrcPath)
			moduleName = str.split(modelFileName,'.')[0]
                        if self.logger.isEnabledFor(logging.DEBUG): 
			   self.logger.debug("model to be added = {}.{}".format(moduleName, modelName))
			#all models found in models subdir of the pypath
			HandlerClass = self.importName("models." + moduleName, modelName)
			handler = HandlerClass(str(host), str(port), cmdOptions, self.logger)
                        if self.logger.isEnabledFor(logging.DEBUG): 
			   self.logger.debug("handler produced")
			modelDict[str(modelName)] = handler
                        if self.logger.isEnabledFor(logging.DEBUG): 
			   self.logger.debug("model {}.{} added!".format(moduleName, modelName))
			modelAddMsg = "model {}.{} added".format(moduleName,modelName)
                        (inputfields) = handler.getInputFields()
			result = json.dumps({'Cmd' : 'addModel', 'Server' : host, 'Port' : str(port), 'Code' : 0, 'Result' : modelAddMsg, 'InputFields' : inputfields, 'OutputFields' : outputfields })
#			result = json.dumps({'Cmd' : 'addModel', 'Server' : host, 'Port' : str(port), 'Code' : 0, 'Result' : modelAddMsg, 'InputFields' : [] , 'OutputFields' : [] })
		else:
			inputfields = []
			outputfields = []
			if "Module" in cmdOptions:
				moduleName = str(cmdOptions["Module"])
			else:
				moduleName = "no module supplied"
			modelAddMsg = "ModuleName.ModelName '{}.{}' is invalid...it does not reference a valid class".format(moduleName, modelName)
			result = json.dumps({'Cmd' : 'addModel', 'Server' : host, 'Port' : str(port), 'Code' : -1, 'Result' : modelAddMsg, 'InputFields' : inputfields, 'OutputFields' : outputfields })

		if self.logger.isEnabledFor(logging.INFO):
                   self.logger.info("AddModel results = {}".format(result))
		return result

	def importName(self, moduleName, name):
		"""
		Import a named object from a module in the context of this function 
		"""
		try:
			if self.logger.isEnabledFor(logging.DEBUG):
                           self.logger.debug("load model = " + moduleName)
			module = __import__(moduleName, globals(), locals(), [name])
                        if self.logger.isEnabledFor(logging.DEBUG): 
			   self.logger.debug("module obtained")
		except ImportError:
			return None
		return getattr(module, name)


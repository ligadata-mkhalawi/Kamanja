import os
import os.path
import json
from common.CommandBase import CommandBase
import logging
import sys 
import logging.config
import logging.handlers

class removeModel(CommandBase): 
	"""
	AddModelCmd input is formatted like this Scala string : 
		s"$cmd\n$modelName\n$modelInfo\n$modelSrc"
	"""
	
	def __init__(self, pkgCmdName, host, port, logger):
		super(removeModel, self).__init__(pkgCmdName, host, port, logger)


	def handler(self, modelDict, host, port, cmdOptions, modelOptions):
		if "ModelName" in cmdOptions:
			modelName = str(cmdOptions["ModelName"])
		else:
			modelName = ""

		if modelName in modelDict:
			del modelDict[modelName]
			removeResult = 'model {} removed'.format(modelName)
			rc = 0
		else:
			modelKey = modelName
			removeResult = "key '{}' was not found in the model dictionary...remove failed".format(modelKey)
			rc = -1

		result = json.dumps({'Cmd' : 'removeModel', 'Server' : host, 'Port' : str(port), 'Code' : str(rc), 'Result' : removeResult })
		self.logger.info(result)

                if ( moduleName in sys.modules):
                   self.logger.debug("model  unloading first = " + moduleName)
                   del sys.modules[moduleName]
                   self.logger.debug("again checking  = " + moduleName)
                   if ( moduleName not in sys.modules):
                      self.logger.debug("model unloaded = " + moduleName)
                   else:
                      self.logger.debug("not unloaded  model = " + moduleName)
		return result



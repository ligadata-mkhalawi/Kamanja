import os
import os.path
import json
from common.CommandBase import CommandBase
import logging
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
		else:
			modelKey = modelName
			modelName = "{} was not found in the model dictionary...not".format(modelKey)

		removeResult = 'model {} removed'.format(modelName)
		result = json.dumps({'Cmd' : 'removeModel', 'Server' : host, 'Port' : str(port), 'Result' : removeResult })
		self.logger.info(result)

		return result



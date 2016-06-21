import os
import os.path
import json
from common.CommandBase import CommandBase
import logging
import logging.config
import logging.handlers

class serverStatus(CommandBase): 
	"""
	serverStatus will answer the models that are managed by this
	this command instance's pythonserver.
	"""
	
	def __init__(self, pkgCmdName, host, port, logger):
		super(serverStatus, self).__init__(pkgCmdName, host, port, logger)

	def handler(self, modelDict, host, port, cmdOptions, modelOptions):
		modelNameView = modelDict.viewkeys()
		modelNames = ["{}".format(v) for v in modelNameView]
		svrstatus = 'Active models are: {}'.format(modelNames)
		result = json.dumps({'Cmd' : 'serverStatus', 'Server' : host, 'Port' : str(port), 'Result' : svrstatus })
		self.logger.info(result)
		return result

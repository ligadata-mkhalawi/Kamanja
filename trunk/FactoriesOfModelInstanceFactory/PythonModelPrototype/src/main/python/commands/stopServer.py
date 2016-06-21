import sys
import json
from common.CommandBase import CommandBase
import logging
import logging.config
import logging.handlers

class stopServer(CommandBase):
	"""
	Execute the model mentioned in the cmdOptions feeding it the
	message also found there (key = "InputDictionary")
	"""
	def __init__(self, pkgCmdName, host, port, logger):
		super(stopServer, self).__init__(pkgCmdName, host, port, logger)

	def handler(self, modelDict, host, port, cmdOptions, modelOptions):
		msg = "Host {} listening on port {} to be stopped by user command".format(self.host, self.port)
		result = json.dumps({'Cmd' : 'stopServer', 'Server' : host, 'Port' : str(port), 'Result' : msg })
		self.logger.info(result)
		return 'kill-9' #no server going message returned for now.

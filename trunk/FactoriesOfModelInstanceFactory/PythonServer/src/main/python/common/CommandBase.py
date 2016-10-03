
import json
import sys
import logging
import logging.config
import logging.handlers

class CommandBase(object): 
	""" 
	CommandBase is parent class to the commands in the pythonserver
	(addModel, executeModel, et al).  It contains a number of useful
	methods that are shared by all commands... e.g., exceptionMsg
	"""

	def __init__(self, pkgCmdName, host, port, logger):
		self.cmdName = pkgCmdName
		self.host = host
		self.port = port
		self.logger = logger
	#
	def exceptionMsg(self, infoTag):
		"""
		print failure locally and
		answer the exception as json dict
		"""
		prettycmd = json.dumps({'Server' : self.host, 'Port' : str(self.port), 'Cmd' : self.cmdName, 'Result' : infoTag, 'Exception' : str(sys.exc_info()[0]), 'FailedClass' : str(sys.exc_info()[1])}, sort_keys=True, indent=4, separators=(',', ': '))
#                if self.logger.isEnabledFor(logging.DEBUG): 
#		   self.logger.debug(prettycmd)
		xeptMsg = json.dumps({'Server' : self.host, 'Port' : str(self.port), 'Cmd' : self.cmdName, 'Result' : infoTag, 'Exception' : str(sys.exc_info()[0]), 'FailedClass' : str(sys.exc_info()[1])})
		return xeptMsg

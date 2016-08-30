import sys
import json
from common.CommandBase import CommandBase
import logging
import logging.config
import logging.handlers


# ExecuteModelCmd is formatted like this:
# {
#   "Cmd": "executeModel",
#   "CmdVer": 1,
#   "CmdOptions": {
#     "ModelName": "a",
#     "InputDictionary": {
#       "a": 1,
#       "b": 2
#     }
#   },
#   "ModelOptions": {}
# },

class executeModel(CommandBase):
	"""
	Execute the model mentioned in the cmdOptions feeding it the
	message also found there (key = "InputDictionary")
	"""
	def __init__(self, pkgCmdName, host, port, logger):
		super(executeModel, self).__init__(pkgCmdName, host, port, logger)

	def handler(self, modelDict, host, port, cmdOptions, modelOptions):
		"""
		diagnostics: dump the model name keys and values from the modelDict
		"""
		modelNameView = modelDict.viewkeys()
		modelNames = ["{}".format(v) for v in modelNameView]
		modelValueView = modelDict.viewvalues()
		modelInsts =  ["{}".format(str(v)) for v in modelValueView]
#                if self.logger.isEnabledFor(logging.DEBUG): 
#		   self.logger.debug("{} models in modelDict = {}".format(len(modelNameView),modelNames))
#		   self.logger.debug("{} instances in modelDict = {}".format(len(modelValueView),modelInsts))

		if "ModelName" in cmdOptions:
			modelName = str(cmdOptions["ModelName"])
		else:
			modelName = "no model name supplied"

		results = ""
		try:
			msg = cmdOptions["InputDictionary"]
		except:
			results = super(executeModel, self).exceptionMsg("No message value with key 'InputDictionary' for model {} ... it should be in the supplied cmdOptions dictionary for executeModel".format(modelName))
		#
		if results == "":
			try:
				model = modelDict.get(modelName)
				msg = cmdOptions["InputDictionary"]
#                                if self.logger.isEnabledFor(logging.DEBUG): 
#				   self.logger.debug("model instance selected = {}".format(str(model)))
				results = model.execute(msg)
			except:
				results = super(executeModel, self).exceptionMsg("The model '{}' is having a bad day...".format(modelName))
		return results




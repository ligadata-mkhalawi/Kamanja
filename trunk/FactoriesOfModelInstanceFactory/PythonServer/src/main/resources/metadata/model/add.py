import abc
from common.ModelInstance import ModelInstance
import json
import logging

class AddTuple(ModelInstance): 
	""" Model AddTuple will sum msg["a"] and msg["b"] """
	def execute(self, msg):
		""" 
		A real implementation would use the output fields to 
		determine what should be returned. 
		"""
		sumofTup = int(msg["a"])
		sumofTup += int(msg["b"])
		outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"], 'result' : sumofTup})
		return outMsg

	def __init__(self, host, port, modelOptions, logger):
		super(AddTuple, self).__init__(host, port, modelOptions, logger)

	def getInputOutputFields(self):
		"""The fields and their types are returned  """
		"""This is looking for dict item "TypeInfo" ... really it """
		"""should be some other key... like InputFields and OutputFields"""
		self.logger.debug("Entered AddTuple.getInputOutputFields")
		modelOptions = super(AddTuple, self).ModelOptions()
		inputFields = dict()
		outputFields = dict()
		if "TypeInfo" in modelOptions:
			inputFields.update(modelOptions["TypeInfo"])
			outputFields.update(modelOptions["TypeInfo"])
			outputFields["result"] = "Int"
		else:
			inputFields["a"] = "Int"
			inputFields["b"] = "Int"
			outputFields["a"] = "Int"
			outputFields["b"] = "Int"
			outputFields["result"] = "Int"

		return (inputFields , outputFields)

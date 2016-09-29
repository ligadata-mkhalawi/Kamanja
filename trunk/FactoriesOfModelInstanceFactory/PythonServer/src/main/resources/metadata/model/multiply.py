import abc
from common.ModelInstance import ModelInstance
import json
import logging

class MultiplyTuple(ModelInstance): 
	""" Model MultiplyTuple will multiply msg["a"] and msg["b"] """
	def execute(self, msg):
		""" 
		A real implementation would use the output fields to 
		determine what should be returned. 
		"""
		prodofTups = int(msg["a"])
		prodofTups *= int(msg["b"])
		outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"], 'result' : prodofTups})
		return outMsg

	def __init__(self, host, port, modelOptions, logger):
		super(MultiplyTuple, self).__init__(host, port, modelOptions, logger)

	def getInputOutputFields(self):
		"""The fields and their types are returned  """
		"""This is looking for dict item "TypeInfo" ... really it """
		"""should be some other key... like InputFields and OutputFields"""
		self.logger.debug("Entered MultiplyTuple.getInputOutputFields")
		modelOptions = super(MultiplyTuple, self).ModelOptions()
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

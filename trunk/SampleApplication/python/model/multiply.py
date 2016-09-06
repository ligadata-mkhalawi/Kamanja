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
		outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"], 'operator' : '*', 'result' : prodofTups})
		return outMsg

        def getInputFields(self):
		"""The field names and their types needed by the model are returned to """
		"""the python proxy (model stub communicating with this server). """
		"""Feel free to just hard code the type info if that is best. """
		"""The returned dictionaries are used by the python proxy to choose """
		"""which fields from the associated messages(s) to send to the python server """
		"""when the model is executed.  This is appropriate when the message contains"""
		"""a thousand fields, but the model only uses five of them. """

		"""As shown, conceivably the information could be configured in the model """
		"""options. """

		self.logger.debug("Entered MultiplyTuple.getInputFields")
		modelOptions = super(MultiplyTuple, self).ModelOptions()
		inputFields = dict()
		if "InputTypeInfo" in modelOptions:
			inputFields.update(modelOptions["InputTypeInfo"])
		else:
			inputFields["a"] = "Int"
			inputFields["b"] = "Int"

	        return (inputFields)

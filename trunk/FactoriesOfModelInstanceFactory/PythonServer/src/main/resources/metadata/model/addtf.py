import abc
from common.ModelInstance import ModelInstance
import json
import logging
import tensorflow as tf

class AddTupleTF(ModelInstance):
	""" Model AddTuple will sum msg["a"] and msg["b"] """
	def execute(self, msg):
		"""
		A real implementation would use the output fields to
		determine what should be returned.
		"""
                a = tf.placeholder(tf.int32)
                b = tf.placeholder(tf.int32)
                sumofTup = tf.placeholder(tf.int32)
                add = tf.add(a, b)
                sess = tf.session()
                sumofTup = sess.run (add, feed_dict={a: int(msg["a"]) ,b: int(msg["a"]) })
                self.logger.debug("sumof Tup" + str(sumofTup))
                outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"], 'result' : sumofTup})
                self.logger.debug("sumof Tup" + outMsg)
                return outMsg

	def __init__(self, host, port, modelOptions, logger):
		""" One might want to configure the model here with modelOptions info"""
		""" See getInputOutputFields(self) below for an example of how to access"""
		""" modelOptions"""
		super(AddTupleTF, self).__init__(host, port, modelOptions, logger)

	def getInputOutputFields(self):
		"""The field names and their types needed by the model are returned to """
		"""the python proxy (model stub communicating with this server). """
		"""Feel free to just hard code the type info if that is best. """
		"""The returned dictionaries are used by the python proxy to choose """
		"""which fields from the associated messages(s) to send to the python server """
		"""when the model is executed.  This is appropriate when the message contains"""
		"""a thousand fields, but the model only uses five of them. """

		"""As shown, conceivably the information could be configured in the model """
		"""options. """

		self.logger.debug("Entered AddTupleTF.getInputOutputFields")
		modelOptions = super(AddTupleTF, self).ModelOptions()
		inputFields = dict()
		outputFields = dict()
		if "InputTypeInfo" in modelOptions and "OutputTypeInfo" in modelOptions:
			inputFields.update(modelOptions["InputTypeInfo"])
			outputFields.update(modelOptions["OutputTypeInfo"])
		else:
			inputFields["a"] = "Int"
			inputFields["b"] = "Int"
			outputFields["a"] = "Int"
			outputFields["b"] = "Int"
			outputFields["result"] = "Int"

		return (inputFields , outputFields)

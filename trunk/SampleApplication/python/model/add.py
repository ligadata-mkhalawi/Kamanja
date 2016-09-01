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


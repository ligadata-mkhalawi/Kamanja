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


import abc
from common.ModelInstance import ModelInstance
import json
import logging

class SubtractTuple(ModelInstance): 
	""" Model SubtractTuple will subtract msg["b"] from msg["a"] """
	def execute(self, msg):
		""" 
		A real implementation would use the output fields to 
		determine what should be returned. 
		"""
		diffofTups = int(msg["a"])
		diffofTups -= int(msg["b"])
		outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"], 'operator' : '-', 'result' : diffofTups})
		return outMsg


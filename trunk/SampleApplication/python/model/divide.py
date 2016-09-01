import abc
from common.ModelInstance import ModelInstance
import json
import logging

class DivideTuple(ModelInstance): 
	""" Model DivideTuple will divide msg["a"] by msg["b"] """
	def execute(self, msg):
		""" 
		A real implementation would use the output fields to 
		determine what should be returned. 
		"""
		qutotientofTup = int(msg["a"])
		qutotientofTup /= msg["b"]

		outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"], 'result' : qutotientofTup})	
		return outMsg


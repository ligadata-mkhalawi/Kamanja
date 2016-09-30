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
                a = int(msg["a"])
                b = int(msg["b"])
                if ( a > b ) :
                   quotientofTup = a / b
		   outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"], 'operator' : '/', 'result' : quotientofTup})	
                else :
                   outMsg = ""

		return outMsg


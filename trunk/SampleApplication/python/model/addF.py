import abc
from common.ModelInstance import ModelInstance
import json
import logging

class AddFTuple(ModelInstance): 
        """ Model AddFTuple will sum msg["a"] and msg["b"] """
        def execute(self, msg):
                """ 
                A real implementation would use the output fields to 
                determine what should be returned. 
                """
                sumofTup = float(msg["a"])
                sumofTup += float(msg["b"])
                outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"], 'operator' : '+', 'result' : sumofTup})
                return outMsg


import abc
from common.ModelInstance import ModelInstance
import json
import logging

import theano
from theano import tensor

class AddTheanoTuple(ModelInstance): 
        """ Model AddTheanoTuple will sum msg["a"] and msg["b"] """
        def execute(self, msg):
                """ 
                A real implementation would use the output fields to 
                determine what should be returned. 
                """
                a = tensor.dscalar ()
                b = tensor.dscalar()
                c = a+ b
                f = theano.function([a,b], c)
                sumofTup = f(float(msg["a"]) , float(msg["b"]) )
                outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"], 'result' : sumofTup.item(0)})
                return outMsg


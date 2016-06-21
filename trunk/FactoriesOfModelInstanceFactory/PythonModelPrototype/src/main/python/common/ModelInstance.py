
import abc
from common.ModelBase import ModelBase
import json
import sys
import logging
import logging.config
import logging.handlers


class ModelInstance(ModelBase): 
    """ 
    Initialize the model instance (the super class for 
    all the python models) with the supplied model info JSON
    popped off the front of supplied addModel inputs 
    """

    def __init__(self, host, port, modelOptions, logger):
        self.host = host
        self.port = port
        self.modelOptions = modelOptions
        self.logger = logger
        if "PartitionHash" in modelOptions:
            self.partitionHash = modelOptions["PartitionHash"]
        else:
            self.partitionHash = 0

    @abc.abstractmethod
    def execute(self, outputDefault):
        """if outputDefault is true we will output the default value if nothing matches, otherwise null."""

    @abc.abstractmethod
    def getInputOutputFields(self):
        """answer two dicts - one for input and output 
            (inputdict, outputdict) 
            each dict consists of fldName/fld type pairs
            this is a portion of the AddModel command result,
            effectively advertising which fields are needed by the 
            model to process and what can be expected when output
            sent.
        """

    def isModelInstanceReusable(self):
        """Can the instance created for this model be reused on subsequent transactions?"""
        return super(ModelInstance,self).isModelInstanceReusable()

    def ModelOptions(self):
        #make the options dictionary available to the concrete implementors of ModelBase
        return self.modelOptions


    def PartitionHash(self):
        #Answer which hash this model is dedicated to.
        return self.partitionHash

    #
    def exceptionMsg(self, infoTag):
        """
        print failure locally and
        answer the exception as json dict
        """
        prettycmd = json.dumps({'Server' : self.host, 'Port' : str(self.port), 'Result' : infoTag, 'Exception' : str(sys.exc_info()[0]), 'FailedClass' : str(sys.exc_info()[1])}, sort_keys=True, indent=4, separators=(',', ': '))
        self.logger.debug(prettycmd)
        xeptMsg = json.dumps({'Server' : self.host, 'Port' : str(self.port), 'Result' : infoTag, 'Exception' : str(sys.exc_info()[0]), 'FailedClass' : str(sys.exc_info()[1])})
        return xeptMsg


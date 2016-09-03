import abc
from abc import ABCMeta

class ModelBase:
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def execute(self, outputDefault):
        """if outputDefault is true we will output the default value if nothing matches, otherwise null."""

    @abc.abstractmethod
    def getInputFields(self):
        """answer two lists - one for input and output 
            (inputList, outputList) 
            each list consists of [(fldName, fld type, descr)]
            this is the add model result ... from the AddModel command
        """
        



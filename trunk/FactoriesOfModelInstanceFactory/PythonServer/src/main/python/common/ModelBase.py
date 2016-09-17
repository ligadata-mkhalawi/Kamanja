import abc
from abc import ABCMeta

class ModelBase:
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def execute(self, outputDefault):
        """if outputDefault is true we will output the default value if nothing matches, otherwise null."""




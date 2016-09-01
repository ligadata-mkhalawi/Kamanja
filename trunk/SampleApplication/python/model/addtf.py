import abc
from common.ModelInstance import ModelInstance
import json
import logging
import tensorflow as tf

class AddTupleTF(ModelInstance):
	""" Model AddTuple will sum msg["a"] and msg["b"] """
	def execute(self, msg):
		"""
		A real implementation would use the output fields to
		determine what should be returned.
		"""
                a = tf.placeholder(tf.int32)
                b = tf.placeholder(tf.int32)
                sumofTup = tf.placeholder(tf.int32)
                add = tf.add(a, b)
                sess = tf.session()
                sumofTup = sess.run (add, feed_dict={a: int(msg["a"]) ,b: int(msg["a"]) })
                self.logger.debug("sumof Tup" + str(sumofTup))
                outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"], 'result' : sumofTup})
                self.logger.debug("sumof Tup" + outMsg)
                return outMsg




Running Python Models on Kamanja
================================

:ref:`Python<python-term>` models can be run on Kamanja
much as other types of models are run.
Use the :ref:`kamanja-command-ref`
to add, remove, and update models.
These commands are illustrated here.

Limitations
-----------

Kamanja is currently tested and supported for Python version 2.7.
Future releases may include support for higher versions of Python
(Python 3 and above).

Kamanja Python Metadata Commands
--------------------------------

Add Model
~~~~~~~~~

The following command illustrates how to add a model
to the Kamanja system:

::

  $KAMANJA_HOME/bin/kamanja add model python \
      $KAMANJA_HOME/input/SampleApplications/metadata/model/subtract.py \
      MODELNAME subtract.SubtractTuple \
      MESSAGENAME org.kamanja.arithmetic.arithmeticMsg \
      OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg \
      TENANTID tenant1 \
      MODELVERSION 0.00001 \
      MODELOPTIONS ’{”InputTypeInfo”: {”a” : ”Float”, ”b” : ” Float”}}’

or

::

  $KAMANJA_HOME/bin/kamanja add model python \
      $KAMANJA_HOME/input/SampleApplications/metadata/model/subtract.py \
      MODELNAME subtract.SubtractTuple \
      MESSAGENAME org.kamanja.arithmetic.arithmeticMsg \
      OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg \
      TENANTID tenant1 \
      MODELVERSION 0.00001

where

- MODELNAME <modulename>.<classname> – name of the module name
  without .py.Classname is the name of the class inside the module
  that contains the execute method.
- MESSAGENAME – name of the input message
  for the consumption of the input message.
- OUTMESSAGE – name of the output queue where produced messages are sent.
- MODELOPTIONS – (optional) contains the active input fields
  from the consumed input message in JSON format.

For Python models, a user may also specify model options
in the form of a JSON string
or the full path name of a file containing JSON.
The options can be quite complex,
including JSON dictionaries and JSON lists of dictionaries.

Note that this information is strictly for the consumption and use
of the model being catalogued.
It is saved in the Kamanja metadata model definition
associated with the model being added.

The MODELOPTIONS JSON string is supplied to the model
whenever it is added to a server.
The information it contains can be used to complete
initialization of the model in some way.


Update Model
~~~~~~~~~~~~

Updates are similar to the add model:

::

  kamanja update model python \
     $KAMANJA_HOME/input/SampleApplications/metadata/model/subtract.py \
     MODELNAME subtract.SubtractTuple \
     MESSAGENAME org.kamanja.arithmetic.arithmeticMsg \
     OUTMESSAGE org.kamanja.arithmetic.arithmeticOutMsg \
     TENANTID tenant1 \
     MODELVERSION 0.00003 \
     MODELOPTIONS ’{”InputTypeInfo”: {”a” : ”Float”, ”b” : ” Float”}}’

In the example, the model options have had a number of their values changed.
The filters from the instrusionFilters2 directory are to be used
and the port to the spark.ml cluster has been changed.
An upgraded version of the Python model has also been chosen
to replace the current one.

Remove Model
~~~~~~~~~~~~

All Kamanja remove model commands are similar:

::

  kamanja <metadata api properties file> remove model <namespace.name.version>

Kamanja Python Program Structure
--------------------------------

In this section, the structure and semantics of a Kamanja Python model
are explained and illustrated.


Python Model using ModelInstance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Model DivideTuple divides msg["a"] by msg["b"].
A real implementation would use the output fields
to determine what should be returned.

::

  import abc
  from common.ModelInstance import ModelInstance
  import json
  import logging

  class DivideTuple(ModelInstance):
       def execute(self, msg):

       a = int(msg["a"])
       b = int(msg["b"])
       if (a > b):

            qutotientofTup = a/b
            outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"],
                 'operator' : '/', 'result' : qutotientofTup})

       else :
            outMsg = ""

       return outMsg


The Python example can return either output or NULL
as shown in the above program.
The model can install external libraries and use them in the Python program.
The models are stored in $KAMANJA_HOME/python/model once they are compiled.
MODELNAME in the command-line is given the name of the <modulename>.<classname>.
The divide.py MODELNAME is divide.DivideTuple.

Theano Model using ModelInstance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following code gives another example using
a third-party Python scientific library called Theano:

::

  import abc
  from common.ModelInstance import ModelInstance
  import json
  import logging
  import theano
  from theano import theano

  class AddTheanoTuple(ModelInstance):
       """ Model AddTheanoTuple will sum msg["a"] and msg["b"] """
       def execute(self, msg):
            """
            A real implementation would use the output fields to
            determine what should be returned.
            """
            a = theanodscalar ()
            b = theanodscalar()
            c = a+ b
            f = theano.function([a,b], c)
            sumofTup = f(float(msg["a"]) , float(msg["b"]) )

            outMsg = json.dumps({'a' : msg["a"], 'b' : msg["b"],
                 'operator' : '+', 'result' : sumofTup.item(0)})
            return outMsg

Examples
--------

The Kamanja distribution comes with a few Python examples
in the following directory:

::

  $KAMANJA_HOME/input/SampleApplications


Simple Example
~~~~~~~~~~~~~~

Steps to run the sample:

#. Start Zookeeper, Kafka
#. Call the :ref:`createqueues-command-ref` command
   to connect to the Kafka server and create some queues for simple testing.
#. Run the Python_Simple.sh script
   in $KAMANJA_HOME/input/SampleApplications/bin.
   to load all :ref:`messages<messages-term>`, :ref:`models<model-term>`,
   and :ref:`adapter bindings<adapter-binding-config-ref>`.
#. Start Kamanja; see :ref:`start-stop-cluster`.
#. Run PushSimpleData.sh in $KAMANJA_HOME/input/SampleApplications/bin.
#. Watch the output queue.

The simple example consists of four models which
1) add, 2)subract, 3)multiply, and 4) divide two numbers.

Sample input and output are given below.

::

  Type of Data 	Actual Data 	Description of Actual Data
  Input 	880, 235 	csv input (a,b)
  Output 	{“a”:880,”b”:235,”operator”:”+”,”result”:1115}
  {“a”:880,”b”:235,”operator”:”-“,”result”:645}
  {“a”:880,”b”:235,”operator”:”*”,”result”:206800}
  {“a”:941,”b”:372,”operator”:”:”,”result”:2} 	Add, subtrract, multiply, divide

Example Using Python Libraries
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The example shows another use of the Theano library.
It takesj advantage of underlying hardware
(that includes recent graphics processing units (GPUs))
and hence has high performance that is comparable to C implementations.

The example is just a simple floating point addition
done functionally using Theano.

Steps to run the sample:

#. Start Zookeeper, Kafka
#. Call the :ref:`createqueues-command-ref` command
   to connect to the Kafka server and create some queues for simple testing.
#. Run the Python_Theano.sh script
   in the *$KAMANJA_HOME/input/SampleApplications/bin* directory
   to load all :ref:`messages<messages-term>`, :ref:`models<model-term>`,
   and :ref:`adapter bindings<adapter-binding-config-ref>`.
   This loads all messages, models, and adapter bindings.
#. Start Kamanja; see :ref:`start-stop-cluster`.
#. Run the PushSimpleData.sh in $KAMANJA_HOME/input/SampleApplications/bin.
#. Watch the output queue.

::

  Type of Data 	Actual Data 	  escription of Actual Data
  Input 	22, 802           csv input (a,b)
  Output 	{“a”:22.0,”b”:803.0,”operator”:”+”,”result”:825.0} (addf.py)
                                  {“a”:22.0,”b”:803.0,”operator”:”+”,”result”:825.0} (Theano) 	Add


Multiple Logistic Regression Example
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The script calculates the risk score for a given customer
using his/her job, payment, and mortgage history.

Steps to run the example:

#. Start Zookeeper, Kafka
#. Call the :ref:`createqueues-command-ref` command
   to connect to the Kafka server and create some queues for simple testing.
#. Run the Python_Reg.sh script
   in the *$KAMANJA_HOME/input/SampleApplications/bin* directory
   to load all :ref:`messages<messages-term>`, :ref:`models<model-term>`,
   and :ref:`adapter bindings<adapter-binding-config-ref>`.
#. Start Kamanja.
#. Run the PushHmeqData.sh script
   in the *$KAMANJA_HOME/input/SampleApplications/bin* directory.
#. Watch the output queue.

::

  Type of Data 	Actual Data 	Description of Actual Data
  Input 	53,0,0,1,1,0,0,0,0,0,0,0.0236,0.165,
  0.0785,0.243,0.025,0.0666,0.176,
  0,0.352,0.0896,0.189,0.109 	csv input “REC ID”,
  “LOAN”,”MORTDUE”,
  “VALUE”,”REASON”,
  “JOB”,”YOJ”,
  “DEROG”,”DELINQ”,
  “CLAGE”,”NINQ”,
  “CLNO”,”DEBTINC”
  Output 	{“rec id”: 53,”python risk score”: 0.2493699} 	Predict risk

To understand the terms in this table,
see `HMEQ-mortgage-applic-SAS-data-doc.pdf
<http://kamanja.org/wp-content/uploads/2016/09/HMEQ-mortgage-applic-SAS-data-doc.pdf>`_
and
`HMEQ-Sta6704-Data-Mining-Methods.pdf
<http://kamanja.org/wp-content/uploads/2016/09/HMEQ-Sta6704-Data-Mining-Methods.pdf>`_.

Bibliography
~~~~~~~~~~~~

Theano 0.8.2 Documentation. LISA Lab, University of Montreal, 2008-2016.
Web. 23 Sep 2016.
(`<http://deeplearning.net/software/theano/introduction.html>`_)


.. _pmml-guide-intro:

Introduction to PMML
====================

PMML (Predictive Model Markup Language) is a language written in XML
that describes the rules that to be given to the engine.

LigaData chooses an augmented version of the PMML standard
from the Data Mining Consortium for writing LigaData models.
PMML is an XML-based language for specifying
a wide variety of data mining model types.
The latest release (4.2.1) defines fifteen different model types.
The industry consortium,
`Data Mining Group <http://dmg.org/>`_ (DMG),
maintains the standard specification.
Leading corporations, such as IBM, SAS, MicroStrategy, and SPSS,
support this effort and are principals in the consortium membership.
Visit www.dmg.org for more details.

Kamanja supports all PMML models that can be consumed
by Openscoring.io’s PMML evaluator library called Kamanja’s PMML evaluator.
The Kamanja PMML evaluator can consume models
from several of the PMML producers including KNIME, R/Rattle, and Rapid Miner.
These three have active test cases
as part of the Kamanja/s PMML evaluator repository.

This is a conservative view of the PMML evaluator’s abilities.
It is likely that other providers found on the product page
can also be consumed.

To interface the PMML technology to Kamanja,
an adapter model called the JpmmlAdapter
has been developed and is part of the Kamanja distribution.
It implements the ModelInstance and ModelInstanceFactory interfaces
for all PMML models that are ingested
via the Kamanja metadata API service.
As such, there are only a few details needed to understand PMML models.
These are covered in the next sections.

Below is the list of the model types that are currently supported. This information was taken from https://github.com/jpmml/jpmml-evaluator.

Model evaluation:

- association rules
- cluster model
- general regression
- Naive Bayes
- k-nearest neighbors
- neural network
- regression
- rule set
- scorecard
- support vector machine
- tree model
- ensemble model

The links cover a better description of model evaluation
such as rule set and scorecard.


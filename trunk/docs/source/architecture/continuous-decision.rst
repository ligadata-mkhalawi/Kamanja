
.. _continuous-decision-arch:

Continuous decisioning overview
===============================

Enterprises are racing to capitalize on three major advancements
in the data space:

- big data (that is, massive, inexpensive storage and distributed computing)
- continuous decisioning
- data science

While an explosion of tools has developed within each area,
the effective integration of these technologies
for maximum business impact remains a significant challenge.
This introduction describes the basic Lambda architecture
that enables real‑time analytics
through batch and continuous decisioning of big data.
The section then presents the extended Lambda architecture,
which has an additional continuous decisioning layer
The section then introduces the Kamanja open-source product
that was designed specifically to meet
all the various requirements of the decisioning layer,
including the latency, robustness, and operability requirements
demanded by the most complex and stringent use cases
within financial services, security, healthcare, and communications.

What is Lambda Architecture?
----------------------------

Lambda architecture has gained popularity recently
as an organizing framework for synthesizing data processing and analysis.
Lambda architecture, presented in the figure below,
is primarily aimed at enabling advanced,
real‑time analytics through batch and continuous decisioning of big data.
It offers an approach to providing views of the data
that optimally combine the comprehensiveness and accuracy
of batch processing with the real-time visibility of stream processing.

.. image:: /_images/lambda-arch.png

Lambda architecture has proven itself a powerful framework
for analytics (see references 1,2,3,4,5,6,7),
but the approach is not directly extensible
to all classes of applications,
including continuous decisioning.
With a single additional processing layer,
termed the decisioning layer,
Lambda architecture can be generalized and extended
to provide real-time decisioning (RTD)
all the same benefits of batch and stream processing
that standard Lambda architecture provides to real-time analytics.
This extension of the architecture also provides
a feedback mechanism for continuously enhancing
the power of the Lambda architecture for all applications.
This is called Lambda architecture with continuous decisioning.

Sample Use Case: Real-time Bank Alerts and Offers
-------------------------------------------------

To illustrate the concept of continuous decisioning,
consider this highly simplified use case from banking:
Upon each account transaction,
decide whether to send the customer an overdraft protection offer
or a low balance alert.
For this use case,
the bank needs to perform the following actions or decisions:

- Upon each new transaction,
  predict if a future overdraft is likely
  based on historic spend and deposit patterns of that customer.

- If an overdraft is likely,
  decide whether to offer overdraft protection based on credit analysis.
  If yes and customer settings permit,
  send an overdraft protection offer optimized for acceptance.
  If no and customer settings permit, send the customer a low balance alert.

This example is typical of many real-world decisioning problems, where:

#. A decision must be made in real-time.
   The perceived value of an overdraft protection product,
   or even a low balance alert,
   exponentially declines with time following a triggering transaction.
#. Decisions are based upon incoming event data
   and multiple sources of stored data.
   New transaction data must be combined with historical transaction data,
   current balances, customer settings, pre-computed thresholds,
   and credit qualification data,
   all of which are maintained on operational data stores.
#. Changes to stored data must immediately impact decision-making.
   New customer notification settings or credit thresholds computed offline
   should take immediate effect in the decisioning process
   as soon as they are available.
#. Decision models are complicated and based upon many data points.
   Models determining whether to extend credit
   are typically complicated functions of many independent variables,
   the product of substantial analysis of offline data.
   Historical behavior, customer preferences, and product offerings
   must be constantly matched
   to optimize customer satisfaction and profitability.
#. Models must adaptively evolve to optimize a decision’s performance.
   Credit models should be adjusted periodically by analysts
   to optimize the credit decision based on analysis of past performance.
   The model that determines the particular overdraft protection product
   to offer a customer should automatically adjust through A/B testing,
   in order to optimize offer acceptance rates.

In a typical continuous decisioning architecture,
there are generally two distinct data processing channels,
shown in the following figure:
one for the processing of events through a decision engine,
potentially with access to an offline data store;
the other an offline process
where decision models are constructed and optimized.


.. image:: /_images/decision-example.png

Where such an architecture typically falls short
is in meeting requirements three through five,
which require a higher level of integration and processing capability
than is generally achievable with this design.
It is precisely in meeting these requirements
where the Lambda architecture with continuous decisioning
may be brought to bear.

Lambda Architecture with Continuous Decisioning
-----------------------------------------------

Three key elements distinguish Lambda architecture
with continuous decisioning from a typical RTD architecture:

- Decisioning is applied to all data immediately upon availability
  (that is, continuously, according to the natural timescale
  of each type of data event, whether the data generates a decision
  or informs a future decision).
- Decisioning leverages all available data,
  including data stored in other layers.
- Enhancements to the decisioning process are enabled
  through continuous feedback of data and model updates.

These key elements are illustrated in the following figure
in the context of the Lambda architecture,
where the new decisioning layer is introduced
as a processing layer through which all new data passes
prior to any subsequent processing.


.. image:: /_images/decision-continuous.png


By introducing this layer at the very front of the Lambda architecture,
decisioning is applied at the very earliest point in the processing chain,
minimizing latency to its theoretical limit.
What’s more, through the continuous feedback mechanism,
increasingly sophisticated analysis may be applied
before passing data to the speed layer,
increasing the relative value and contribution of the speed views
in the serving layer relative to batch views.
In this way, the Lambda Architecture with continuous decisioning
serves not just to make real-time decisioning possible,
but it enhances the basic architecture
for the purposes of real-time analytics as well.

Implementing the Decisioning Layer with Kamanja
-----------------------------------------------

A number of open-source technologies are currently available
to implement Lambda Architecture with continuous decisioning,
with low delivery risk and quick time-to-market.
A sample of these technologies is illustrated in the following figure:


.. image:: /_images/decision-implementation.png

While many off-the-shelf open-source products readily implement
the key elements of the speed, batch, and serving layers,
the decisioning layer introduces a number of requirements
that would generally involve the need to integrate multiple tools.
The Kamanja open-source product was designed specifically
to meet all the various requirements of the decisioning layer,
including the latency, robustness, and operability requirements
demanded by the most complex and stringent use cases
within financial services, security, healthcare, and communications.
Kamanja makes it easy to create, run, and continuously enhance
potentially thousands of models
that are applied against all new data events that enter the decision layer.
Models can range from simple rules-based decision trees written in Java
to sophisticated non-linear classifiers implemented
in Python, R, or PMML.
Models can continuously leverage the most recent and all past data
to make arbitrarily complex decisions at any given moment.
Simply by adding nodes,
Kamanja scales to meet virtually any volume of data
or number and complexity of models.
And as Kamanja is open-source,
support and contributions are crowd-sourced by an ever-growing,
global community of developers and users.

Architecture References
-----------------------

- Marz, Nathan, `How to beat the CAP theorem
  <http://nathanmarz.com/blog/how-to-beat-the-cap-theorem.html>`_.
- `Questioning the Lambda Architecture (oreilly.com)
  <https://news.ycombinator.com/item?id=7976785>`_.
- Marz, Nathan, `lambda architecture
  <http://lambda-architecture.net>`_
- Schuster, Werner (InfoQ),
  `Nathan Marz on Storm, Immutability in the Lambda Architecture, Clojure
  <http://www.infoq.com/interviews/marz-lambda-architecture>`_
- Jebaraj, Daniel (InfoQ),
  `Lambda Architecture: Design Simpler, Resilient, Maintainable
  and Scalable Big Data Solutions
  <http://www.infoq.com/articles/lambda-architecture-scalable-big-data-solutions>`_
- Hausenblas, Michael (Dr.Dobb's),
  `Applying the Big Data Lambda Architecture
  <http://www.drdobbs.com/database/applying‑the‑big‑data‑lambda‑architectur/240162604>`_
- Kreps, Jay (O'Reilly), `Questioning the Lambda Architecture
  <https://www.oreilly.com/ideas/questioning-the-lambda-architecture>`_



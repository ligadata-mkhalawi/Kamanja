
.. _naive-bayes-term:

Naive Bayes classifier
----------------------

A Naive Bayes classifier is a simple probabilistic classifier
based on applying Bayes' theorem
(which is a formula that corrects test results for the "skew"
that is introduced by false positives)
with strong (naive) independence assumptions.
This classifier assumes that the presence (or absence)
of a particular feature of a class is unrelated
to the presence (or absence) of any other feature.
For example, a fruit may be considered to be an apple
if it is red, round, and about 2-4 inches in diameter.
Even if these features depend on each other
or upon the existence of the other features,
a naive Bayes classifier considers all of these properties
to independently contribute to the probability that this fruit is an apple.
Depending on the precise nature of the probability model,
naive Bayes classifiers can be trained very efficiently
in a supervised learning setting.




.. _pmml-guide-modelresults:

Choosing model results
======================

For models written in PMML that should always emit output,
special coding is required to ensure that they do always emit output.

In order for Kamanja to emit output,
one of the rules in RuleSet must return its score value
as opposed to the defaultScore value given in RuleSet.
When this is accomplished,
all of the fields described in the MiningSchema section
that have a usageType of either predicted or supplementary
are sent to the engine for further processing,
including the formation of any output messages
that are associated with the model.
The output message is generated based on
how the models are computed and the input message.

Either of the following techniques ensure that output is always emitted:

- Make sure that at least one of the rules is satisfied.
  This could be a faux rule that always returns its score.
  Use the firstHit criterion on RuleSetSelectionMethod
  and make that faux always TRUE rule the last one in RuleSet.

- Set the predictions by putting them into a DataField
  designed for this purpose;
  then it is just a matter of setting up the SimplePredicate
  to fire in a positive way, producing the non-default score.



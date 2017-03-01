
.. _pmml-guide-missingvalue:

Using missingValueReplacement
=============================

The **missingValueReplacement** parameter
provides default values to PMML model variables
that remain unset after execution completes.
DataField and DerivedField element values
that are intended to be emitted as part of the model output
should have some value,
even when the model execution did not set the field explicitly.
In PMML, the DMG language designers offer
the **missingValueReplacement** mining field attribute for this purpose.

Kamanja supports this mechanism for
the scalars, Boolean, string, date, dateTime, and time types.
Initialization of more complex types
that are possible in Kamanja-based PMML is also possible.
This includes the initialization of collections of arbitrary types.

Motivation
----------

It is always prudent to supply a meaningful typed value
for each field that is going to be emitted and consumed by other applications.
Depending upon the consumer of the model output,
not all values are workable.
For example, can the consuming application handle a NULL value
when it expects an array of strings?

To this end, Kamanja has implemented
the basic **missingValueReplacement** semantics
as described in the `DMG specification
<http://www.dmg.org/v4-2-1/pmml-4-2.xsd>`_
and offers some extensions that help provide initialization service
to more complex types that Kamanja supports.

missingValueReplacement example
-------------------------------

::

  <miningField name="Age"
    usageType="supplementary"
    missingValueReplacement="29"/>
  <miningField name="Today"
    usageType="supplementary"
    missingValueReplacement="20150401"/>
  <miningField name="AYearAgo"
    usageType="supplementary"
    missingValueReplacement="20140101"/>
  <miningField name="COPDSymptoms"
    usageType="supplementary"
    missingValueReplacement="false"/>
  <miningField name="outPatientClaimCostsByDate"
    usageType="supplementary"
    missingValueReplacement="!compile:Map[Int,Double]()"/>
  <miningField name="someCostsByDate"
    usageType="supplementary"
    missingValueReplacement="!compile:Map[Int,Double]()"/>
  <miningField name="inPatientClaimTotalCostEachDate"
    usageType="supplementary"
    missingValueReplacement="!compile:Array[Double]()"/>
  <miningField name="yyyyMMddDateMs"
    usageType="supplementary"
    missingValueReplacement="2015-04-01"/>
  <miningField name="yyyyMMddDate"
    usageType="supplementary"/>
  <miningField name="yyyyMMddHHmmssSSSDateTimeMs"
    usageType="supplementary"
    missingValueReplacement="2015-04-01 23:59:59:999"/>
  <miningField name="yyyyMMddHHmmssSSSDateTime"
    usageType="supplementary"/>
  <miningField name="milDateTimeMs"
    usageType="supplementary"
    missingValueReplacement="15-Apr-2015 23:59:59"/>
  <miningField name="morningTimeMs"
    usageType="supplementary"
    missingValueReplacement="3:59:59 AM"/>
    <miningField name="eveningTimeMs"
    usageType="supplementary"
    missingValueReplacement="6:59:59 PM"/>
  <miningField name="milTimeMs"
    usageType="supplementary"
    missingValueReplacement="16:59:59"/>
  <miningField name="milTime"
    usageType="supplementary"/>

In the above example, the new wrinkle is the use of the !compile:expr syntax.
When the missingValueReplacement begins with !compile:,
the expression immediately following is used
to instantiate a value that is used as the default value
should the corresponding field not be set by other means during execution.

For example, consider the **outPatientClaimCostsByDate** mining field.
The code generated for this mining field is:
::

  ruleSetModel.AddMiningField(
      "outPatientClaimCostsByDate",
    new MiningField("outPatientClaimCostsByDate",
  "supplementary", "", 0.0, "", 0.0, 0.0,
  DataValue.make(Map[Int,Double]()), "", ""))

This snippet from the Scala produced by the PMML compiler
simply adds a MiningField to the ruleSetModel‘s mining schema dictionary.
The compiler uses the the expression part
of the missingValueReplacement value
(the expr in !compile:expr) as an argument
to a runtime library’s factory method,
**DataValue.make(Map[Int,Double]())**.
Were the default value required,
an empty Map[Int,Double] would be returned for outPatientClaimCostsByDate.



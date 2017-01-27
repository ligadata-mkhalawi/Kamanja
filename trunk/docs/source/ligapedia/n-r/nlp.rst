
.. _nlp-term:

Natural Language Processing (NLP)
---------------------------------

Natural Language Processing (NLP) is a component of
:ref:`machine learning<machine-learning-term>`
that uses computers to analyze and understand
human language in an intelligent way.
It is classified as a hard problem in computer science
because of the vagaries of human speech
and the difficulties involved in extracting useful information
out of imprecise text.
NLP is being used for language translation, speech recognition,
topic segmentation,
automatic summarization of trends and sentiments in social media,
and many other projects.

This article gives an overview of some of the major techniques
used for NLP.

Keyword statistics
~~~~~~~~~~~~~~~~~~

Keyword statistics draw inference
by counting the frequency of certain keywords in the text being studied.
For most text classification problems,
keywords statistics is sufficient to determine a class;
they can determine the topic of a text or document,
such as software vs hardware, or pop rock vs punk.
However, some classification problems have distinct classes
that share the same keywords, and document phrasing;
style and other kinds of text structure information
must also be used
to perform text classification in such domains.

Anaphora resolution
~~~~~~~~~~~~~~~~~~~

In NLP, anaphora resolution is
the process of associating a word (or words) with its antecedents.
For example, in the sentence,
"The Queen has just arrived with Prince Phillip at her side,"
her is an anaphora that resolves to indicate the Queen as its antecedent.
As another example, in the sentences
"The heads of state have arrived for a conference about climate change.
The participants are expected to agree on some proposals
and disagree on others,"
participants is an anaphora that resolves to the antecedent "heads of state".
Anaphora resolution is a complicated computational task
that is not always successful.
For example, in the sentence, "The mother hugged her daughter tightly;
I was stunned by her beauty,"
it is ambiguous whether the anaphora her refers to the mother or the daughter.

Rhetorical Structure Theory (RST)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Rhetorical Structure Theory (RST) evaluates
the rhetorical function of each phrase in a text
and the relationship of different phrases to each other.
Some of the relationships used are Elaboration, Evaluation,
Contrast, Condition and Concession.
The theory was originally developed for computer-based authoring
but has proved to be a very useful component in natural language analysis,
providing a way to look at the structure of a text
rather than just the lexical and grammatical forms that comprise the text.
For example, consider the following text;
text segments are numbered for reference:

(1) Vitamin D is an important nutrient;
(2) inadequate amounts are correlated with
    an increased risk of several diseases, including osteoporosis,
    cancer, and diabetes.
(3) Humans can synthesize Vitamin D from the sun
(4) although too much exposure to the sun can cause other health problems.
(5) In many cases, taking Vitamin D supplements is the best solution.

Fragment (1) is characterized as Background.
Fragment (2) is Elaboration.  Fragment (3) is Circumstance.
Fragment 4 is Contrast. Fragment 5 is Evaluation.

Full RST analysis includes many more elements than discussed here.
RST processing involves constructing large, compex tree structures
with branches and leaves that represent each text element
and its classification and show the rhetorical relations
between the text elements.






.. _jtm-elements:

JTM Language Elements
=====================

The following list describes the basic language elements:

- **header section** contains identifying information about the model
  including its namespace, name, version, and language used.
  The variable identifiers that are not otherwise qualified in the model
  are treated as elements of the declared namespace.
- **imports section** declares the Scala/Java packages that are used in the model
  and is used to generate the import statements in the Scala-generated source.
  This is no different from what a standard Scala or Java source file would declare
  and serves the same purpose when the generated Scala is compiled
  by the Scala compiler.
- **aliases section** is used to assign handles to the messages and containers
  that are used in the model.
  Kamanja messages are all namespace-qualified and can be quite long
  when used in an expression.
  The alias can be used in place of the long name in the model expressions.
- **transformations section** contains a map of the transformations available
  in the JTM. Each transformation has a name.

  - Each named transformation in the transformations section
    contains the following elements inside it.
    For most, one or more instances of each can occur except as noted:

    - **dependsOn** (one) describes the input message or messages
      that the transformation section depends on and uses in its transformations.
    - **computes** (one or more) describes a computation of some sort
      that transforms the incoming value or values in some way
      to an output variable that can be emitted
      to one of the output messages as needed.
      They are scoped to the section they are declared
      as denoted by the parent’s enclosing braces.

      Of particular interest is the value member in the map.
      It is a Scala expression and can be as simple as a message field reference
      or some complex map expression that creates a collection of some type
      that is to be used for the output message generation.
      Examples of this are presented in the next section.

      - **grok** (currently one) describes a set of patterns
        that extract values from the specified variable.
        The JTM compiler uses the Grok library for pattern matching support.
        See `<https://github.com/aicer/grok>` for more information.
        It was inspired by the grok functionality found in Elastic’s logstash
        and developed by the American Institute for Computing Education and Research
        (hence the ‘aicer’ moniker).

        - **outputs** can describe one or more named output specifications
          in its JSON map, where each output in the outputs section contains
          the following elements inside of it:

          - a computes (one or more) – scoped to the output message.
          - a join on key.
          - a filter expression (zero or one) – similar to the where clause
            in an SQL query. An arbitrary Boolean expression can be used
            to decide whether this particular output message should be emitted.
            An output message is only generated when the Boolean expression
            evaluates to TRUE. Note that the value of the filter
            (that is, the right-hand side after the ‘:’) is a Scala code snippet
            that refers to the input message.
            Complex Boolean expressions, use of function libraries, table lookups,
            and other Scala constructions can be used to compute the Boolean value.
          - a projection from the inputs and computed expression variables
            to assign to the output fields.
            These can be currently derived in two ways,
            depending on the nature of the input message being processed:

            - by mapbyposition – available when the input message
              is a fixed message where all fields are ordered.
              It is a form of shorthand for assigning input message values
              to the output message.
              This is not available when an input message is a mapped type
              used in sparse data applications.
            - by mapping – used to make output variable assignments
              either by index or by name as desired.
              Note that assignments from mapped messages always need to be
              in the form of ${alias.element}
              because the key-value pairs of mapped messages are unordered.
              Indexed references are not supported in this case.

    - Dereferencing identifiers are found in the current namespace:

      - An identifier (or variable) prefixed with a $ (for example, $element)
        refers to the value of the identifier.
      - A message field is dereferenced with an expression such as this:
        ${msg1.fieldname}. The msg1 is the alias of the message
        declared in the aliases section and fieldname is a field in that message.
      - An $element or ${element} references a variable in the current name space
        such as an alias or a generated variable.
        If the element doesn’t exist in the current context,
        it is sought in the surrounding context that encloses it.
        The referenced element must be unique.
        It is scoped to the enclosing braces of the parent object
        in which it is contained.

        - An example is variables computed by transformations
          declared as child elements of the transformations section.
          Variables can also be scoped to
          the declaration of an output message transformation.
          In that case, they are only known to that output transformation context.

          - If an expression needs to reference an external element,
            it needs to be marked with a $element.
          - Variable names are case-sensitive
            because the target language (that is, Scala) being generated
            has case-sensitive identifiers.

- The comment or comments section makes it possible
  to document the JTM source file with comment items.
  These are ignored by the JTM compiler,
  but quite useful in understanding what the JTM does.
  Single and multiple line comment types are supported:

  ::
    { comment: "single line comment" }
    { comments: ["multiple", "line", "comment"] }





.. _develop-install-top:

Install development tools
=========================

If you are doing development work on Kamanja,
you need to set up a development environment.
This requires:

- Install and configure a Kamanja node to use for your development;
  follow the instructions in :ref:`kamanja-install-top`.
  Most development and initial testing can be done on a single node;
  eventually, you may also need to set up a multi-node cluster
  for testing; follow the instructions in :ref:`cluster-install`.


- Be sure that your environment has all the requisite software installed.
  In general, this is the same software used for the Kamanja runtime platform
  but verify the list in :ref:`key-dev-components`.

.. _key-dev-components:

Key components for development environment
------------------------------------------

- JDK 1.8. The platform uses java.nio and other features
  found only in 1.7 and above.
- ZooKeeper 3.4.6+. Kafka 2.11-0.9.
- Scala 2.11.7.  If you are using an IDE for development,
  be sure that the Scala plugin for the IDE is using
  the same version of Scala as you have installed.
- Download and install **sbt** (Scala Build Tool) from the
  `SBT download <http://www.scala-sbt.org/download.html>`_ page.
  Even if you are working with Python, R, or another non-JVM language,
  you will probably need **sbt** at some point.
- Only the sbt 0.13.7 build tool is supported
  to generate the deployable system.
  Several sbt plugins are also necessary. Use the latest versions.
- Sublime project editor. This plugin helps build Sublime projects.
- SBT assembly plugin. Kamanja scripts build fat JARs
  to ease the deployment of the platform executable
  and several other tools that are part of the platform.
  Install this SBT assembly plugin.
- SBT Dependency Graph Plugin.
  This plugin helps manage the JAR madness endemic
  with any Java JVM-based system.
  It installs a number of graphical viewers
  including those that create image representations.
  Note: To display the graphic forms of these
  (for example, a dot file), install the appropriate tools.
  Graphviz works well and offers a number of display tools
  and image type coercion services.
- You can develop applications on Kamanja
  using only :ref:`command line tools<command-line-dev>`
  without using an IDE.
  You can, of course, use the IDE of your choice for development.
  Each IDE has its own method for importing SBT projects;
  see the documentation for each IDE for details.
  Some popular IDEs are discussed here:

  - :ref:`eclipse-dev` is perhaps the most popular IDE
    in the industry.  If you are familiar with Eclipse, you can
    use it as an editor but it is awkward to build Scala and Java
    code within Eclipse; you can use the command line tool for builds.

  - JetBrain :ref:`intellij-dev` works well to edit and build
    Java, Scala, and Python code.

  - :ref:`netbeans-dev` also works well to edit and build
    Java, Scala, and Python code.

  - :ref:`rstudio-dev` is the most popular IDE for R development.

- wget is a prerequisite for the
  :ref:`easyinstallKamanja.sh<easyinstallkamanja-command-ref>` script.
- Eclipse project plugin. This plugin generates Eclipse project files
  to import into the Eclipse workspace.
  The strategy is to make sure any project builds with sbt first,
  then generates the Eclipse project from it.
- Kamanja can also be built with other IDEs
  including JetBrains's IntelliJ and Net Beans.
  Each has its own method for importing SBT projects.
  See each IDE for specifics.


.. _command-line-dev:

Develop at the command line
---------------------------

Java and Scala models can be developed using only command-line tools:

- Create and edit the model code using a text editor
  such as **vim** or **emacs**.
- Use the  :ref:`easyinstallKamanja.sh<easyinstallkamanja-command-ref>` script
  to compile the model.

.. _eclipse-dev:

Eclipse IDE
-----------

To install the Eclipse IDE:

- Go to the `Eclipse home page <https://eclipse.org>`_.
- Click on the Download button.
- Click on Eclipse IDE for Java EE Developers.
- Click on: EclipseSource and then Eclipse IDE for Eclipse Committers,
  which is the standard Elipse package.
- Click on the appropriate machine type.
- Once downloaded, untar it.
- Click on the Eclipse icon in the download directory and Eclipse will launch.

.. _intellij-dev:

IntelliJ IDE
------------

IntelliJ is an IDE with full support for editing and building
Java, Scala and Python code.  You can download it from the
`IntelliJ download site <https://www.jetbrains.com/idea/download/>`_
web page.

If you are doing predictive analysis in Python,
you probably want to import an appropriate Python library
such as `scikit-learn <http://scikit-learn.org/stable/>`_

.. _netbeans-dev:

NetBeans IDE
------------

NetBeans is an open-source extensible IDE
that works well for developing Java, Scala, and Python code.
Download it from the `NetBeans download site
<https://netbeans.org/features/platform/download.html>`_

.. _rstudio-dev:

RStudio IDE
-----------

RStudio is the most popular R IDE.
You can download it from the
`RStudio download page <https://www.rstudio.com/products/rstudio/download/>`_.



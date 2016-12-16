
.. _bashrc-term:

.bashrc and .bash_profile
-------------------------

The Linux *.bashrc* and the Mac *.bash_profile* files
are the scripts that are executed every time
a new **bash** shell is started interactively.
When installing Kamanja, you must modify this file
to define the $KAMANJA_HOME environment variable
as well as environment variables for other components
that are required by Kamanja.

To customize the *.bash_profile* file on a Mac,
do the following:

#. Verify that you have admin privleges:

   ::

     sudo -v

   If the Password prompt is returned,
   You have admin privileges.

#. Edit the file with the sudo command:

   :: sudo vim ~/.bash_profile

   You could instead modify the owner (UID) of the file
   to make yourself owner of the file;
   since the file permissions allow the owner to modify the file,
   this grants you access
   although it does incur some security vulerabilities:

   ::

     chown <your-username> ~/.bash_profile

#. Define the full path that corresponds ot $KAMANJA_HOME
   and export the environment variable:

   ::

     export KAMANJA_HOME=<top-directory-of-Kamanja-tree>
     export PATH=$KAMANJA_HOME/bin:$PATH

For example:

    ::

      export KAMANJA_HOME=/Users/userid/Downloads/installKamanja/Kamanja-1.6.0_2.11
      export PATH=$KAMANJA_HOME/bin:$PATH

The new definitions in the file take effect the next time
you open a new bash shell interactively.
To force the current shell to recognize the new definitions,
issue the following command:

::

  source .bash_profile

To verify that the $KAMANJA_HOME is set correctly,
issue the following command and check the output:

::

  echo $KAMANJA_HOME
  /Users/userid/Downloads/installKamanja/Kamanja-1.6.0_2.11




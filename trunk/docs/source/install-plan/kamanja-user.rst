
.. _kamanja-user-install:

Create the kamanja user (optional)
----------------------------------

These instructions use the **sudo** command for a number of steps.
If you prefer, you can instead set up the "kamanja" user
to have **sudo** permissions
and use that ID to install and configure the Kamanja software.
The commands to do this are:

::

  sudo useradd kamanja
  sudo passwd kamanja

For non-production systems, you can delete the password for the kamanja user:

::

  sudo passwd --delete kamanja

For production systems, you should always use a password for the kamanja user.


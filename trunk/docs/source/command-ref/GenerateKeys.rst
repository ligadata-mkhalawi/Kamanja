

.. _generatekeys-command-ref:

GenerateKeys.sh
===============

Generate a sample key pair and encrypt a password

Syntax
------

Generate a *private.key* and a *public.key* in the current directory:

::

  bash GenerateKeys.sh --algorithm RSA [--generateSampleKeys]

Encrypt your password for the MetadataAPIService:

::

  bash GenerateKeys.sh --algorithm RSA [--privateKeyFile private.key] \
     [--publicKeyFile public.key] --password <ascii-pw> 

List arguments:

::

  bash GenerateKeys.sh --help

Options and arguments
---------------------

- **algorithm** -- encryption algorithm to use to generate the key;
  only RSA is currently supported.

  The encrypt/decrypt functions can also use the following functions:

  - RSA|ECB|PKCS1Padding
  - RSA|ECB|OAEPWithSHA (1AndMCG1Padding)
  - RSA|ECB|OAEPWithSHA (256AndMGF1Padding)

- **password** - password to be encrypted, specified as ASCII text.

- **publicKeyFile** - file containing the public key;
  default value is "public.key".
  You must create this file before running the command. 

- **privateKeyFile** - file containing the private key;
  default value is "private.key".
  This file must be created before running the command;
  it is used to ensure that the encrypt/decrypt utilities are in sync.

- **generateSampleKeys** - If set, key files are generated for testing.
	

Usage
-----

The **GenerateKeys.sh** script first creates an encrypted string (cypher)
then applies base64 encoding on the cipher and outputs an ASCII string. 

When run with the **--GenerateSampleKeys** option,
the command generates two files (public.key and private.key)
in the current directory.
These files can be passed as parameters to the other commands.
Note that the command does not support overwriting existing files;
you must remove the existing files before creating a new pair in the expected location.

To use the encrypted and encoded password in output/OutputUtils,
the encrypted passwords must include three properties:
in the properties file:

- encrypted.encoded.password; for example:

::

  encrypted.encoded.password=dp/2N4VRZVVMIGVgVXg5ndJUgYTFEZ10U6HCr/zLomzM/vuIZ4IA7jagi3BYVkjfAgKAAzEcy+CaAs8/cAStd5W+PUi5VBpjI3xE2UwqsNXzl5oDg67DcA6lLKHcV6tu6S/UVANFYJ2pHNqL1bqXB41TS9a8mSAa7J+f+R9ldc4= 

- private.key.file; for example, private.key.file=/tmp/keys/private.key 

- encrypt.decrypt.algorithm; for example,
  encrypt.decrypt.algorithm=RSA 

When using only the Base64 encoded password,
the properly encoded password must appear in the properties file;
for example:

::

  encoded.password=dp/2N4VRZVVMIGVgVXg5ndJUgYTFEZ10U6HCr/zLomzM/vuIZ4IA7jagi3BYVkjfAgKAAzEcy+CaAs8/cAStd5W+PUi5VBpjI3xE2UwqsNXzl5oDg67DcA6lLKHcV6tu6S/UVANFYJ2pHNqL1bqXB41TS9a8mSAa7J+f+R9ldc4= 

To deduce the password, the Password Decryption Logic
uses one of the following options, in this order:

- Use the encrypted and encoded password
  if the three required properties are supplied.
- Use the properly encoded.password if it is available
  without the other two properties.
- Use the plain text password if the properties are not defined.
- If the properties are supplied but are not complete and accurate,
  a null password is returned.
  You should always comment out any properties
  that are not being used to avoid problems.

Example
-------

An example command to generate an encrypted password is:

::

  # GenerateKeys.sh --algorithm RSA --publicKeyFile public.key --password db_password

  Encrypted Password => BjlM8PVF3rt39r/QLSZzeVEE0zf3v1DDj2qiVOkwuZsVhyQOPtpxw8PTzkNf0UDBaCvav
     Vd7VB9s39wUhPyyaG5OMkEunfMsQyBSHuwkrLnhx1SztK3pUqqx8FpD/LRMDn3dBOj78A+qAl1M81Ysm8NsF6vIL
     YxSXW21LT0ttfo=

An example command to generate a pair of public/key keys is:

::

  # GenerateKeys.sh --algorithm RSA --generateSampleKeys

Files
-----

$KAMANJA_HOME/bin/GenerateKeys.sh

It depends on the fat jars that are located
in the $KAMANJA_HOME/bin/ directory and have names
that reflect the Scala version and the Kamanja release being used:

::

  encryptutils_<scala-version>-<kamanja-release>.jar

  ExtDependencyLibs2_<scala-version>-<kamanja-release>.jar

  ExtDependencyLibs_<scala-version>-<kamanga-release>.jar

  KamanjaInternalDeps_<scala-version>-<kamanja-release>.jar

For example:

- If using Scala version 2.11 on Kamanja 1.6.2,
  use the files named encryptutils_2.11-1.6.2.jar,
  ExtDependencyLibs2_2.11-1.6.2.jar, and so forth.
- If using Scala version 2.10.4 on Kamanja 1.6.2,
  use the files named encryptutils_2.10-1.6.2.jar,
  ExtDependencyLibs2_2.10-1.6.2.jar, and so forth.
- The sub-version number for the Scala version is dropped.

See also
--------



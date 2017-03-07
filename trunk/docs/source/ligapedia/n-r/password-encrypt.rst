
.. _password-encrypt-term:

Passwords, encrypted
--------------------

Encrypted and encoded passwords are supported in Kamanja 1.6.1 and later.
The :ref:`metadataapiconfig-config-ref`
and :ref:`smart-input-adapter-ref`
can be configured to use encrypted passwords
or can be run with plain-text passwords as in earlier releases.
You can also add encrypted password support to your adapters.

To implement encrypted and encoded passwords
for the MetadataAPIService:

- Locate a private.key and a public.key file in the
  *$KAMANJA_HOME/config* directory or the *$KAMANJA_HOME/bin* directory.
  You can use :ref:`generatekeys-command-ref` to create these files
  for testing if you do not have a digital certificate.
- Use :ref:`generatekeys-command-ref` to generate an encrypted password.
- Add the following fields to your
  :ref:`MetadataAPIConfig.properties<metadataapiconfig-config-ref>`
  configuration file:

  ::

    SSL_ENCRYPTED_ENCODED_PASSWD=<YourEncryptedPassword>
    SSL_PRIVATE_KEY_FILE=<PathToYourPrivateKey>

  - <PathToYourPrivateKey> is the full path of the private key
  - <YourEncryptedPassword> is the encrypted password generated above.

For more information:

- :ref:`smart-input-adapter-ref` for information about the
  **Encryted.Encoded.Password** and **PrivateKeyFiles** attributes
  you must enable in the smart file adapter used for your application
  to implement the encrypted and encoded password feature.


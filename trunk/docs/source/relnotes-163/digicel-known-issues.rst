
Known issues in Digicel Phoenix release
=======================================

Output API
----------

- When running a command such as the following
  on message or container data that already contains quotes:

  ::

    curl -k https://localhost:8081/data/com.ligadata.kamanja.samples.containers.coughcodes/ \
        ?icd9code=0330

  Kamanja properly populates the Response Topic.
  However, upon returning to the API Service,
  the API Service throws an exception when it attempts to parse the data.
  The quotes are causing a malformed JSON.
  Messages and containers that do not include quotes in the data
  are parsed and returned correctly.


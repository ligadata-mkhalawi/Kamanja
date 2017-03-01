

.. _log4j2-config-ref:

log4j2.xml
==========

Kamanja uses Apache Log4J2 to log entries from all Kamanja
system and application components.
This is the configuration file that controls how logging is implemented.

File structure
--------------

Parameters
----------

Usage
-----

You can modify this file for the following:

- Implement :ref:`consolidated logging<logging-consolidated>`
- Customize the logging done for your models.
  See :ref:`monitor-exec` for more information.

Examples
--------

Sample log4j2.xml file (no Kerberos)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A sample log4j2.xml file for consolidated logging without Kerberos is:

::

  <?xml version="1.0" encoding="utf-8"?>
  <Configuration monitorInterval="300">
    <Properties>
      <Property name="log-path">/home//Binaries/dev/Kamanja-1.6.1_2.11/logs/<Property>
      <Property name="MyHost">${env:HOSTNAME}/<Property>
     <Property name="NodeId">${env:NodeId}/<Property>
      <Property name="ComponentName">MetadataAPI/<Property>
    </Properties>
    <Appenders>
       <Kafka name="Kafka1" topic="topic1" ignoreExceptions="true">
         <PatternLayout>
           <pattern>%d - Host:${MyHost} - NodeId:${NodeId} - Component:${ComponentName} - %c [%t] - %p - %m %n"</pattern>
       </PatternLayout>    
              <Property name="bootstrap.servers">hostname:9093</Property>
       </Kafka>
      <Console name="CONSOLE" target="SYSTEM_OUT">
        <PatternLayout pattern="%5p [%t] - %m%n" />
      </Console>
    </Appenders>
    <Loggers>
        <Logger name="com" level="DEBUG" additivity="false">
            <AppenderRef ref="CONSOLE"/>
            <AppenderRef ref="Kafka1" level="error"/>
        </Logger>
         <Root level="DEBUG">
            <AppenderRef ref="CONSOLE" />
          </Root>
    </Loggers>
  </Configuration> 


Sample log4j2.xml file (no Kerberos)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A sample log4j2.xml file for consolidated logging without Kerberos is:

::

  <?xml version="1.0" encoding="utf-8"?>
  <Configuration monitorInterval="300">
    <Properties>
      <Property name="log-path">/home//Binaries/dev/Kamanja-1.6.1_2.11/logs/<Property>
      <Property name="MyHost">${env:HOSTNAME}/<Property>
     <Property name="NodeId">${env:NodeId}/<Property>
      <Property name="ComponentName">MetadataAPI/<Property>
    </Properties>
    <Appenders>
       <Kafka name="Kafka1" topic="topic1" ignoreExceptions="true">
         <PatternLayout>
           <pattern>%d - Host:${MyHost} - NodeId:${NodeId} - Component:${ComponentName} - %c [%t] - %p - %m %n"</pattern>
       </PatternLayout>    
              <Property name="bootstrap.servers">hostname:9093</Property>
       </Kafka>
      <Console name="CONSOLE" target="SYSTEM_OUT">
        <PatternLayout pattern="%5p [%t] - %m%n" />
      </Console>
    </Appenders>
    <Loggers>
        <Logger name="com" level="DEBUG" additivity="false">
            <AppenderRef ref="CONSOLE"/>
            <AppenderRef ref="Kafka1" level="error"/>
        </Logger>
         <Root level="DEBUG">
            <AppenderRef ref="CONSOLE" />
          </Root>
    </Loggers>
  </Configuration> 

See also
--------

- `Log4J2 Configuration Manual
  <https://logging.apache.org/log4j/2.x/manual/configuration.html>`_



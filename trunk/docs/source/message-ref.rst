
.. _message-ref-top:

=======================
Message Reference Pages
=======================

This section provides reference pages for the messages
that are included in the Kamanja software distribution.

.. list-table::
   :class: ld-wrap-fixed-table
   :widths: 30 70
   :header-rows: 1

   * - Message
     - Description
   * - :ref:`inputadaptersstatssg-msg-ref`
     - record statistical data about information being
       ingested to the Kamanja cluster.
   * - :ref:`kamanjamessageevent-msg-ref`
     - internal message created each time a message comes into the Kamanja engine.
       This message includes the :ref:`kamanjamodelevent-msg-ref` array`
       of model events and the :ref:`kamanjaexceptionevent-msg-ref` array
       about exceptions that occurred in processing the event.
   * - :ref:`kamanjavelocitymetrics-msg-ref`
     - Implement :ref:`velocity metrics<velocity-metrics-term>`

.. toctree::
   :titlesonly:

   message-ref/InputAdapterStatsMsg
   message-ref/KamanjaMessageEvent
   message-ref/KamanjaVelocityMetrics



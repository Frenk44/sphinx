sphinx
======

Run-time infrastructure for distribution, monitoring, stimulating XML data over UDP broadcast networks.

An UDP broadcast network enables the developer to use multiple interacting programs to interact during execution time. This approach has the advantage to develop separate smaller interacting programs to achieve a high complexity rather than one big program. Smaller programs are easier to maintain than one large complex program.

However debugging the functionality now takes place during execution time when the smaller programs are integrated. To facilitate this, monitoring and stimulating data interactions are required. The data is defined by XML (ASCII) strings and is thereby (virtual) machine and language independent. 

The SPHINX infrastructure consists of a number of tools, which can be used by application developers to:
1) define XML-messages
2) MONITOR XML-messages
3) STIMULATE XML-messages on event
4) PERSIST XML-messages
5) store XML-messages as CONTEXT data
6) RETRIEVE persisted XML-messages
7) RETRIEVE contextual XML-messages
8) MANAGE persisted and contextual messages


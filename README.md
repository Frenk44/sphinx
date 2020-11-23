SPHINX
======
Sphinx stands for Server Paradigm for Hosting Interconnecting Nodes using XML.
It is a run-time infrastructure for the distribution, monitoring and stimulating of XML data over UDP multicast networks.<br>
<br>
An UDP multicast network enables the developer to use multiple interacting programs to interact during execution time. This approach has the advantage to develop separate smaller interacting programs to achieve a high complexity rather than one large program. A system, which is broken up into multiple smaller programs are easier to dmaintain than one large complex program.<br>
<br>
However debugging the functionality now takes place during execution time when the smaller programs are integrated. To facilitate this, monitoring and stimulating data interactions are required. The data is defined by XML (ASCII) strings and is thereby (virtual) machine and language independent. <br>
<br><br>
To build:
<br> install jdk
<br> install maven
<br> install git
<br> git clone https://github.com/Frenk44/sphinx.git
<br> mvn clean install
<br><br>
To execute:
<br> go to the test directory and copy the generated jar from the target directory: copy ..\target\*.jar .
<br> execute: java -jar monitor-1.0-SNAPSHO-jar-with-dependencies.jar
The SPHINX infrastructure consists of a number of tools, which can be used by application developers to:<br>
<br> 1) define XML-messages
<br> 2) MONITOR XML-messages
<br> 3) STIMULATE XML-messages


SPHINX Tool
======
Sphinx stands for System Paradigm for Hosting Interconnecting Nodes using XML.
It is a design pattern to setup a run-time distributed infrastructure, where the subsystem nodes are communicating by exchanging XML data over UDP multicast networks. There are numerous advantages of distributed systems when compared to a non-distributed system. However the main challenge in a large distributed system is to manage the datamodel used and the system dynamics. To facilitate this, the SPHINX Tool has been developed.<br>
<br>
An UDP multicast network enables the developer to use multiple interacting programs to interact during execution time. This approach has the advantage to develop separate smaller interacting programs to achieve a high complexity rather than one large program. A system, which is broken up into multiple smaller programs is easier to maintain than one large complex program. With UDP multicast, we can assign an executable to an UDP multicast address to listen to. And each executable is able to write to any UDP multicast address. With this distributed architecture, designers are able to break up a large executable into a set of smaller executables. These executables can run on any hardware in the system and can be written in the programming language, which is best suited for the required functionality. Since the data is described by ASCII XML, there is no machine dependencies like bigendian, little endian conversions.
<br>
<br>
The Tool will help the developer to test these executables individually and integrate them together. The Tool is written in Java and can be compiled and executed on any Linux or Windows machine.
<br>
<br>
The tool can be used in online realtime operation:
  <li> monitor data </li>
  <li> stimulate data </li>
<br>
The tool can also be used in offline non-realtime operation:
  <li> load PCAP file for offline analysis </li>
  
  
<br>
<br>
<br>
To build:

  <li> install jdk</li>
  <li> install maven</li>
  <li> install git</li>
  <li> git clone https://github.com/Frenk44/sphinx.git</li>
  <li> goto sphinx directory and build: mvn clean install</li>

<br> 
To execute:
<li> go to the test directory and copy the generated jar from the target directory:<b> copy ..\target\*.jar . </b></li>
<li> to add new messages, go to the templates\model1 directory and create your own message by modifying an existing one or create a new one</li>
<li> execute: <b>java -jar monitor-1.0-SNAPSHOT-jar-with-dependencies.jar</b></li>



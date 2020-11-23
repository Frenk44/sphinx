SPHINX
======
Sphinx stands for Server Paradigm for Hosting Interconnecting Nodes using XML.
It is a run-time infrastructure for the distribution, monitoring and stimulating of XML data over UDP multicast networks.<br>
<br>
An UDP multicast network enables the developer to use multiple interacting programs to interact during execution time. This approach has the advantage to develop separate smaller interacting programs to achieve a high complexity rather than one large program. A system, which is broken up into multiple smaller programs are easier to maintain than one large complex program.<br> With UDP multicast, we can assign an executable to an UDP multicast address to listen to. And each executable is able to write to any UDP multicast address. With this distributed architecture, designers are able to test individual executables or a set of executables. These executables can run on any hardware in the system and can be written in any programming language. Since the data is described by ASCII XML, there is no machine dependencies like bigendian, little endian conversions.  
<br>
To build:
<li>
  <ul> install jdk</ul>
  <ul> install maven</ul>
  <ul> install git</ul>
  <ul> git clone https://github.com/Frenk44/sphinx.git</ul>
  <ul> mvn clean install</ul>
</li>
To execute:
<br> go to the test directory and copy the generated jar from the target directory: copy ..\target\*.jar .
<br> execute: java -jar monitor-1.0-SNAPSHO-jar-with-dependencies.jar



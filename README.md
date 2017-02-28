<p class="josman-to-strip">
WARNING: THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE <a href="http://davidleoni.github.io/diversicon/" target="_blank">PROJECT WEBSITE</a>
</p>

<p class="josman-to-strip" align="center">
<img alt="Diversicon" src="docs/img/diversicon-core-writing-100px.png" >
<br/>
</p>

#### About Core

Java library that provides a multilingual SQL knowledge base for lexicons and concepts, based upon <a href="https://github.com/dkpro/dkpro-uby" target="_blank">DKPRO UBY</a>


|**Usage**|**License**|**Roadmap**|**Contributing**|
|-----------|---------|-----------|----------------|
| See [docs](docs) |Business-friendly [Apache License v2.0](LICENSE.txt) | See [project milestones](../../milestones) | See [the wiki](../../wiki)|

**Features**
  
  * Additions to <a href="https://github.com/dkpro/dkpro-uby" target="_blank">DKPRO UBY</a>:
    - much stronger XML validation
	- XML schema, and a simplified DTD    
    - XML namespaces management	
  	- simplified API for compressed XML I/O and SQL backup  
  	- more metadata about DB and imports 
  	- improved logging
  	- transitive closure computation for `SynsetRelations`  	
  	- Ships with [Wordnet 3.1 dump](https://github.com/diversicon-kb/diversicon-wordnet-3.1)
  	- a testing framework to quickly create graphs via api
  	- improved domain modeling 
  	- adoption of a small default lexical resource `DivUpper`   	  
  * access to DB via Hibernate
  * for now fully supports <a href="http://www.h2database.com/html/main.html" target="_blank">H2 DB</a>, a pure Java database (both in-memory and on disk). No separate installation is required.
  * dependency handling with Maven    
  * unit tested with proper integration tests    
  * supports Java 7+


#### Dependencies

* DKPRO <a href="https://github.com/dkpro/dkpro-uby" target="_blank">UBY</a>
* Hibernate 
* <a href="http://www.h2database.com/html/main.html" target="_blank">H2</a> database
* Apache Commons IO and Compress, XZ Archiver
* <a href="https://github.com/diversicon-kb/diversicon-model" target="_blank">Diversicon Model</a>

#### Projects using Diversicon Core

* [DiverCLI](https://github.com/diversicon-kb/divercli): a command line tool to manage Diversicons
* [DivMaker](https://github.com/diversicon-kb/divmaker): XML LMF converter
* [S-Match Diversicon](https://github.com/s-match/s-match-uby): a connector for <a href="http://semanticmatching.eu/s-match.html" target="_blank">S-match</a> semantic framework

#### Credits

Design:

* Fiona McNeill - Heriot-Watt University, Edinburgh - f.mcneill at hw.ac.uk 
* Gabor Bella - DISI, University of Trento -  gabor.bella at unitn.it
* David Leoni - Heriot Watt University, Edinburgh - david.leoni at unitn.it

Programming:  

* David Leoni - Heriot Watt University, Edinburgh - david.leoni at unitn.it


Based on:

<a href="http://dkpro.github.io/dkpro-uby/" target="blank">DKPRO UBY framework</a>, by UKP Lab, Technische Universität Darmstadt.

Made possible thanks to:

&emsp;&emsp;&emsp;<a href="https://www.hw.ac.uk/schools/mathematical-computer-sciences/departments/computer-science.htm" target="_blank"> <img src="docs/img/hw.webp" width="80px" style="vertical-align:middle;"> </a> &emsp;&emsp;&emsp;<a href="https://www.hw.ac.uk/schools/mathematical-computer-sciences/departments/computer-science.htm" target="_blank"> Heriot-Watt Computer Science Department </a>  

&emsp;<a href="http://kidf.eu" target="_blank"> <img style="vertical-align:middle;" width="140px" src="docs/img/kidf-scientia.png"> </a> &emsp; <a href="http://kidf.eu" target="_blank"> Knowledge in Diversity Foundation </a> <br/>

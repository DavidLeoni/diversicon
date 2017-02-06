<p class="josman-to-strip">
WARNING: THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE <a href="http://davidleoni.github.io/diversicon/" target="_blank">PROJECT WEBSITE</a>
</p>

<p class="josman-to-strip" align="center">
<img alt="DiverCLI" src="docs/img/diversicon-core-writing-textpath-75px.svg" height="200%">
<br/>
</p>

#### About

Simple Java library to provide a multilingual SQL knowledge base for lexicons and concepts, based upon <a href="https://github.com/dkpro/dkpro-uby" target="_blank">UBY</a>. 


|**Usage**|**License**|**Roadmap**|**Contributing**|
|-----------|---------|-----------|----------------|
| See [docs](docs) |Business-friendly [Apache License v2.0](LICENSE.txt) | See [project milestones](../../milestones) | See [the wiki](../../wiki)|

**Features:**
  
  * thin wrapper over <a href="https://github.com/dkpro/dkpro-uby" target="_blank">UBY</a>, adds:	
  	- transitive closure computation for `SynsetRelations`
  	- simplified API for compressed XML I/O (in LMF-UBY format) and SQL backup  
  	- a testing framework to quickly create graphs via api
  	- more metadata about DB and imports 
  	- improved logging
  	- Ships with [Wordnet 3.1 dump](https://github.com/diversicon-kb/diversicon-wordnet-3.1)  
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
* Diversicon Model <a href="https://github.com/diversicon-kb/diversicon-model" target="_blank">Diversicon Model</a>

#### Projects using Diversicon

* [DiverCLI](https://github.com/diversicon-kb/divercli): a command line tool to manage Diversicons
* [S-Match UBY](https://github.com/s-match/s-match-uby): a connector for <a href="http://semanticmatching.org/s-match.html" target="_blank">S-match</a> semantic framework

#### Credits

* Programming:  David Leoni - Heriot Watt University, Edinburgh - david.leoni at unitn.it
* Architecture: Gabor Bella - DISI, University of Trento -  gabor.bella at unitn.it

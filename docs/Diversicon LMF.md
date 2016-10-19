### Introduction

One of Diversicon goals is to allow importing and merging LMF XMLs produced by different people,
preventing potential clashes. So, while Diversicon should be able to read all the XML files 
created with UBY 0.7.0, sometimes it can warn the user about potential conflicts and ask the user to how
to deal with them. 

 
If an LMF follows the guidelines reported here, clashes should never occur. 
  

### Namespaces
  
XML allows to declare namespaces for tags and attributes 
(so you can write stuff like <my-namespace-prefix.mytag my-namespace-prefix:myAttribute="bla bla">) but
we abuse them to give a scope also to tag IDs: <tag id="my-namespace-prefix.bla">
Namespaced IDs are necessary because in UBY IDs are global, and when merging multiple sources into te db 
conflicts might occur. Note that currently we don't require any specific format for prefixes,
nor namespace urls, so version numbers are not mandatory and urls are not required 
to be resolvable nor persistent (although of course it is very desirable).
Note that when ids are inserted into the database, prefixes _are not_ expanded.

   
In Diversicon LMF you can declare namespaces at the beginning of the file with the `xmlns`
attribute:

```xml
<?xml version="1.0" encoding="UTF-8"?
  xmlns:sm="https://github.com/DavidLeoni/diversicon/tree/master/src/main/resources/smartphones-lmf.xml"
  xmlns:wn31="https://github.com/DavidLeoni/diversicon-wordnet-3.1/blob/master/src/main/resources/it/unitn/disi/diversicon/data/div-wn31.xml.xz">
```
 

### Default namespace

The prefix used in the value of a LexicalResource `name` attribute is intended to be
the default prefix of the document. Such prefix must be also defined in the `xmlns` 
section of the document, like 'sm' here:

```    
<?xml version="1.0" encoding="UTF-8"?
  xmlns:sm="https://github.com/DavidLeoni/diversicon/tree/master/src/main/resources/smartphones-lmf.xml"
...
      
<LexicalResource name="sm:lr">
...
</LexicalResource>

```

### IDs

Each `id` attribute in the XML must be prefixed with a namespace. If it is not, when importing the 
document Diversicon will complain. You have to 


TODO Each Synset must be associated to at least one Sense respective LexicalEntry
 


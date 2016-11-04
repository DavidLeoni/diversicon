### Introduction

Diversicon should allow importing and merging LMF XMLs produced by different people,
preventing the clashes that may arise. 
Diversicon should be able to read the XML files created with UBY 0.7.0, provided you
add some bookkeeping information to the files to indicate to which namespace they belong.  
  

### Namespaces
  
XML allows to declare namespaces for tags and attributes 
(so you can write stuff like `<my-pfx:my-tag my-pfx:my-attribute="bla bla">`)
 but we abuse them to give a scope also to tag IDs: `<tag id="my-pfx_bla">`. 
Namespaced IDs are necessary because in UBY IDs are global, and when merging multiple sources into te db 
conflicts might occur. 

There are a few things to keep in mind:

- in tag IDs there is an underscore `_` separating the prefix from the name like in `<tag id="my-pfx_bla">`
- although we use prefixes like 'wn31` we don't require version numbers in them
- we don't enforce any specific format for namespace urls, and urls are not required 
to be resolvable nor to be persistent (although of course it is very desirable)
- when ids are inserted into the database, prefixes _are not_ expanded

   
In Diversicon LMF you can declare namespace in `LexicalResource` tag the `xmlns`
attribute:

```xml

<?xml version="1.0" encoding="UTF-8"?>

<LexicalResource name="div-smartphones"				 
				 prefix="sm"				 
  				 xmlns:sm="https://github.com/diversicon-kb/diversicon-model/blob/master/src/main/resources/smartphones.xml"
  				 xmlns:wn31="https://github.com/diversicon-kb/diversicon-wordnet-3.1"
				 				 				 
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				 xsi:noNamespaceSchemaLocation="http://diversicon-kb.eu/schema/1.0/diversicon.xsd">
  
  
```
 

### Default namespace

The prefix used in the value of a LexicalResource `name` attribute is intended to be
the default prefix of the document. Such prefix must be also defined in the `xmlns` 
section of the document, like 'sm' here:

```    
<?xml version="1.0" encoding="UTF-8"?
  xmlns:sm="https://github.com/diversicon-kb/diversicon/tree/master/src/main/resources/smartphones-lmf.xml"
...
      
<LexicalResource name="sm:lr">
...
</LexicalResource>

```

### IDs

Each `id` attribute in the XML must be prefixed with a namespace. If it is not, when importing the 
document Diversicon will complain. You have to 


TODO Each Synset must be associated to at least one Sense respective LexicalEntry
 


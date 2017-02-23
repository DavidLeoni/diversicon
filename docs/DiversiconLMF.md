### Introduction

Diversicon should allow importing and merging LMF XMLs produced by different people,
preventing the clashes that may arise. 
Diversicon should be able to read the XML files created with UBY 0.7.0, provided you
add some bookkeeping information to the files to indicate to which namespace they belong.  
  
### LexicalResource name

The `LexicalResource` attribute `name` must be worldwide unique. In order , you should pick a 
reasonable long and unique prefix for your organization. In the case of Diversicon example resources, 
we allowed ourselves the luxury of picking a short name like `div`. So for example, the resource 
[smartphones](https://github.com/diversicon-kb/diversicon-model/blob/master/src/main/resources/smartphones.xml) 
declaration begins this: 

```
<LexicalResource name="div-smartphones"
  .
  .
  .
				
```


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

The prefix used in the value of a `LexicalResource` `prefix` attribute is intended to be
the default prefix of the document. Such prefix must be also defined in the `xmlns` 
section of the document, like 'sm' here:

```    
<?xml version="1.0" encoding="UTF-8"?
  xmlns:sm="https://github.com/diversicon-kb/diversicon/tree/master/src/main/resources/smartphones-lmf.xml"
...
      
<LexicalResource name="div-smartphones">
...
</LexicalResource>

```



### IDs

Each `id` attribute in the XML must be prefixed with a namespace. If it is not, when importing the 
document Diversicon will complain. You have to 


TODO Each Synset must be associated to at least one Sense respective LexicalEntry
 

### Schema
TODO

### Canonical relations
Canonical relations are privileged with reference to the inverse they might have, in the sense that Diversicon algorithms will only consider canonical relations and not their inverses.
For example, since hypernymy is considered as canonical, transitive closure graph is computed only for hypernym, not hyponym. To avoid missing inforation, after an import Diversicon will make sure canonical relations are materialized in the db from the inverses with provenance $eval{eu.kidf.diversicon.core.Diversicons.getProvenanceId()}. 


### Domains

Wordnet 3.1 ships with information about domains, and UBY converter recognize and convert such domains.
Still, we needed to work a bit on the domain representation. First we describe domains as implmented in Wordnet, 
then how they are converted in UBY and then we introduce how we modelled them in Diversicon. 

#### Wordnet domains

* Wordnet partitions domain relations into  `usage` (pointer key `;r`) , `region` (pointer key `;u`) or `topic` (pointer key `;c`) relations. 
* None of these relations is hierarchical. 
* Only `topic ` is transitive. 

#### UBY Wordnet domains 

Reading [UBY Wordnet converter](https://github.com/diversicon-kb/dkpro-uby/issues/3) code, looks like
you can't directly state that a synset is a domain.  You can know if a `Synset` is a domain if
a)  has associated at least one `Sense` that is linked in turn to a `SemanticLabel` of type `ELabelTypeSemantics.domain, regionOfUsage or usage`
b) OR other synsets point to it with one of `usage` (pointer key `;r`) , `region` (pointer key `;u`) or `topic` (pointer key `;c`) relations. 

Note also that LMF converters use the generic word `ELabelTypeSemantics.domain` in `SemanticLabel.type`.

#### Diversicon domains

For the reasons stated above and given that:

* We need to express hierarchies
* we prefer talking about 'domain' instead of topics
* Wordnet 3.1 domains look a bit confusing


We did the following modifications:

1) introduced new `domain` and `superDomain` relations, plus the respective inverses `domainOf` and `subDomain`. Example of usage:

`ss_train` `domain` `ss_transportation` `superDomain` `ss_applied-sciences` `superDomain` `div_ss_domain`

NOTE: computation of transitive closure [won't consider leaves](https://github.com/diversicon-kb/diversicon-core/issues/32).

2) Established a new a root domain synset in `$eval{eu.kidf.diversicon.data.DivUpper.SYNSET_ROOT_DOMAIN}` in [DivUpper LexicalResource](https://github.com/diversicon-kb/diversicon-model/blob/master/src/main/resources/div-upper.xml) and made sure existing domains point to it. If a lexical resource has topics expressed only via UBY b) method (like Wordnet 3.1) during import normalization substep, edges pointing to the root marked with `div` provenance will be automatically added.

3) When importing, Diversicon will:

* preserve existing edges as usual
* detect domain synsets and link them to root `domain` synset 
* normalize Wordnet `topic` relation into `domain`. If in input graph there are inverses but not canonical edges, add the canonical `domain` and `superDomain`
* calculate transitive closure for `superDomain`

4) When exporting:

As usual, Diversicon will try to export a graph very similar to input one, and avoid exporting computed domain edges.


### Normalized LMF
TODO

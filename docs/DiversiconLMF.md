### Introduction

Diversicon allows importing and merging LMF XMLs produced by different people,
preventing the clashes that may arise. Diversicon should be able to read the XML files created with UBY 0.7.0, provided you add some bookkeeping information to the files to indicate to which namespace they belong.  
  
### LexicalResource name

The `LexicalResource` attribute `name` must be worldwide unique. so you should pick a 
reasonable long and unique prefix for your organization. In the case of Diversicon example resources, we allowed ourselves the luxury of picking a short name like `div`. So for example, the resource 
[smartphones](https://github.com/diversicon-kb/diversicon-model/blob/master/src/main/resources/smartphones.xml) declaration begins like this: 

```
<LexicalResource name="div-smartphones"
  .
  .
  .
				
```


### Namespaces
  
XML allows declaring namespaces only for tags and attributes (so you can write stuff like `<my-pfx:my-tag my-pfx:my-attribute="bla bla">`) but we abuse them to give a scope also to tag IDs: `<tag id="my-pfx_bla">`. Namespaced IDs are necessary because in UBY IDs are global, and when merging multiple sources into te db conflicts might occur. 

There are a few things to keep in mind:

- in tag IDs there is an underscore `_` separating the prefix (i.e. 'my-pfx') from the name ('i.e. `bla`) like in
 `<tag id="my-pfx_bla">`
- although we use prefixes like `wn31` we don't require version numbers in them
- we don't enforce any specific format for namespace urls, and urls are not required 
to be resolvable nor to be persistent (although of course it is very desirable)
- when ids are inserted into the database, prefixes _are not_ expanded

   
In Diversicon LMF you can declare namespaces in `LexicalResource` tag the `xmlns`
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
 

### Document namespace

The value of a `LexicalResource` `prefix` attribute is intended to be
the prefix of the document. Such prefix must be also defined in the `xmlns` 
section of the document, like `sm` here:

```xml   
<LexicalResource name="div-smartphones"				 
				 prefix="sm"				 
  				 xmlns:sm="https://github.com/diversicon-kb/diversicon-model/blob/master/src/main/resources/smartphones.xml"
  				 xmlns:wn31="https://github.com/diversicon-kb/diversicon-wordnet-3.1"
				 				 				 
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				 xsi:noNamespaceSchemaLocation="http://diversicon-kb.eu/schema/1.0/diversicon.xsd">

...
...

```

Note _all_ document tag ids must begin with the declared document prefix followed by an underscore, like `sm_ss_tablet` in the following example. All referenced external ids must begin with a declared prefix, like `wn31_ss_n3086983`.   

```xml

        <Synset id="sm_ss_tablet">
            			           
            <SynsetRelation target="wn31_ss_n3086983" 
            				relType="taxonomic" 
            				relName="hypernym"/>
        </Synset>
```


TODO Each Synset must be associated to at least one Sense respective LexicalEntry
 

### Schema

Schema is provided as DTD and XSD at these addresses:

**DTD:** $eval{eu.kidf.diversicon.core.Diversicons.DTD_1_PUBLIC_URL}
**XSD:** $eval{eu.kidf.diversicon.core.Diversicons.SCHEMA_1_PUBLIC_URL}

If you need specific stable versions, you can use `x.y` format like these: 
$eval{eu.kidf.diversicon.core.Diversicons.DTD_1_0_PUBLIC_URL}
$eval{eu.kidf.diversicon.core.Diversicons.SCHEMA_1_0_PUBLIC_URL}

The DTD is an improved version of the DKPRO one ([see changes](https://github.com/diversicon-kb/diversicon-core/issues/15)). The schema is derived automatically from the DTD and then fixed with an [XQuery transform](../src/main/resources/internals/fix-div-schema.xql). 

### Canonical relations

Canonical relations are privileged with reference to the inverse they might have, because Diversicon algorithms only consider canonical relations, and not their inverses.
For example, since _hypernymy_ is considered as canonical, transitive closure graph is computed only for hypernyms, not hyponyms. To avoid missing information, after an import Diversicon makes sure canonical relations are materialized in the db from the inverses, using provenance `$eval{eu.kidf.diversicon.core.Diversicons.getProvenanceId()}`. 


### Domains

Wordnet 3.1 ships with information about domains, and UBY converter recognize and convert such domains. Still, we needed to work a bit on the domain representation. These were our desiderata: 

* need to express hierarchies
* preference to talk about 'domain' instead of 'topics'
* Wordnet 3.1 domains look a bit confusing


First we describe domains as implemented in Wordnet, then how they are converted in UBY and finally we introduce they are modelled in Diversicon. 

#### Wordnet domains

* Wordnet partitions domain relations into
	- `usage` (pointer key `;r`)
	- `region` (pointer key `;u`)
	- `topic` (pointer key `;c`) 
* None of these relations are hierarchical 
* Only `topic ` is transitive

#### UBY Wordnet domains 

In UBY seems like you can't directly state that a synset is a domain (see issue about [UBY Wordnet converter](https://github.com/diversicon-kb/dkpro-uby/issues/3)).  You can know if a `Synset` is a domain if
a) it has associated at least one `Sense` that is linked in turn to a `SemanticLabel` of type `ELabelTypeSemantics.domain`, `regionOfUsage` or `usage`
b) OR other synsets point to it with one relation among `usage` (pointer key `;r`) , `region` (pointer key `;u`) or `topic` (pointer key `;c`) 

Note also that LMF converters use the generic word `ELabelTypeSemantics.domain` in `SemanticLabel.type`.

#### Diversicon domains

We did the following modifications:

1) introduced new `domain` and `superDomain` relations, plus the respective inverses `domainOf` and `subDomain`. Usage example:

`ss_train` `domain` `ss_transportation`
`ss_transportation` `superDomain` `ss_applied-sciences`
`ss_applied-sciences` `superDomain` `div_ss_domain`

(note computation of transitive closure [won't consider leaves](https://github.com/diversicon-kb/diversicon-core/issues/32)).

2) Established a new a root domain synset as `$eval{eu.kidf.diversicon.data.DivUpper.SYNSET_ROOT_DOMAIN}` in [DivUpper LexicalResource](https://github.com/diversicon-kb/diversicon-model/blob/master/src/main/resources/div-upper.xml), and made sure existing domains point to it. If a lexical resource has topics expressed only via UBY _b)_ method (like Wordnet 3.1) during import normalization substep, edges pointing to the root marked with `div` provenance will be automatically added.

3) When importing, Diversicon will:

* preserve existing edges as usual
* detect domain synsets and link them to root `domain` synset 
* normalize Wordnet `topic` relation into `domain`. If in input graph there are inverses but not canonical edges, add the canonical `domain` and `superDomain`
* calculate transitive closure for `superDomain`

4) When exporting:

As usual, Diversicon will try to export a graph very similar to input one, and avoid exporting computed domain edges.


### Normalized LMF

When an LMF is normalized, these conditions are met: 

* if there is a non-canonical relation, there is also its [canonical](#canonical-relations) inverse. 
* transitive relations don't have self-loops
* for each `topic` edge there must be also a `domain` edge
* domain synsets are marked as such by being linked with a `superDomain` relation to `div-upper` synset `$eval{eu.kidf.diversicon.data.DivUpper.SYNSET_ROOT_DOMAIN}`





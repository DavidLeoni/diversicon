### Introduction

Diversicon supports `LexicalResource` reading, importing XMLs and some limited form of update. Delete is currently not supported. These operations rely on two principles: 

#### Principle 1: One LexicalResource per XML

As per LMF specs, each LMF XML file contains exactly one `LexicalResource`. This also simplifies file management and provenance.

#### Principle 2: Imported LexicalResources shouldn't change

A Diversicon database should always contain a faithful representation of the imported XMLs. To allow this, eventual changes to an imported `LexicalResource`  should be done in a controlled manner by Diversicon (i.e. ID renaming or edges added for computing transitive closure). All edges automatically added by Diversicon during normalization and transitive closure computation are marked as having provenance `$eval{eu.kidf.diversicon.data.DivUpper.PREFIX}`, and during export they are filtered out. This way at any time you should be able to export a `LexicalResource` to obtain something nearly identical to the original XML it came from. Note some difference with the original could be admitted for provenance purposes, like i.e. additional metadata documentating the passage into Diversicon. 

If you still try directly updating an already imported `LexicalResource` (i.e. by manually editing the db), then it becomes your responability to keep the database in a consistent state.


### XML Validation

XML Validation is configured by  [`XmlValidationConfig`](../src/main/java/eu/kidf/diversicon/core/XmlValidationConfig.java) and done in two steps:

1. XML Schema 1.0 validation, according to $eval{eu.kidf.diversicon.core.Diversicons.SCHEMA_1_PUBLIC_URL}
2. the custom class `DivXmlValidator` performs further validation in two substeps:	 
	1. XML structure and metadata are coherent
	2. internal XML references are satisfied (i.e. _phablet_ links to existing _smartphone_ synset)
	
By default, plain XML validation is done in _non-strict_ mode, that is, it doesn't fail on warnings.


### XML Import

Creation of `LexicalResource` should be done only through import functions of Diversicon, as this preserves database integrity and allows keeping properly track of metadata.


XML import in Diversicon is a complex process divided into 6 steps. Notice first steps are similar to XML 
validation but with important differences:

1. XML Schema 1.0 validation, according to $eval{eu.kidf.diversicon.core.Diversicons.SCHEMA_1_PUBLIC_URL}
   
   NOTE: during import XML is validated in _strict_ mode, so on warnings it does fail.
   
2. the custom class `[DivXmlValidator](../src/main/java/eu/kidf/diversicon/core/internal/DivXmlValidator.java)` performs further validation in three steps:
	 
	1. XML structure and metadata are coherent
	2. internal XML references are satisfied (i.e. _phablet_ links to existing _smartphone_ synset)
	3. external references are satisfied and present in the db (i.e. links to Wordnet _computer_ synset are already present in the db ). If external references are not satisfied (i.e. in the XML Wordnet is referenced but was not imported yet) WARNINGs are emitted. 

3. a new [ImportJob](src/main/java/eu/kidf/diversicon/core/ImportJob.java  is created,
 flags in `[DbInfo](../src/main/java/eu/kidf/diversicon/core/DbInfo.java) class are reset, logging is redirected to db 
4. lexical resource is written to the db
5. resulting graph is validated to prevent problems with augmentations (i.e. it is checked for self-loops in hypernyms) 
5. graph is [normalized](DiversiconLMF.md#normalized-lmf)
	5.1 [Domains](DiversiconLMF.md#domains) are identified and linked to $eval{eu.kidf.diversicon.data.SYNSET_ROOT_DOMAIN} 
	5.2 [Canonical relations](DiversiconLMF.md#canonical-relations) are materialized	
6. graph is augmented by calculating the transitive closure of canonical relations.

Steps after 3 are optional and configurable with 
the `skipAugment` flag in `[ImportConfig](../src/main/java/eu/kidf/diversicon/core/ImportConfig.java)`.
The idea is that you can import many files skipping them and only after the batch import validate and enrich the graph by calling `Diversicon.processGraph()`
	
If any ERROR is reported, import fails. If any WARNING is reported, import fails, because by default during import there is strict validation. To change this behaviour, see the following `force` flag.

#### `force` flag	

If you try to import resources in the wrong order (i.e. resource A depends on B, and you import first B) or resources with circular references (A depends on B and viceversa), import will fail because of validation warnings. To still proceed with the import in these cases, you can enable a `force` flag in the `[ImportConfig](../src/main/java/eu/kidf/diversicon/core/ImportConfig.java)`.

#### Import errors

After validation has passed, actual import will start. If errors occur after writing to db has started, Diversicon will try some strategy for rolling back: unfortunately at this time complete rollback is **not** always
possible due to [issue 38](https://github.com/diversicon-kb/diversicon-core/issues/38). To avoid surprises
it can be a good idea to use the dry run flag in `[ImportConfig](../src/main/java/eu/kidf/diversicon/core/ImportConfig.java)`.


### Update

There are different scenarios in which a user might want to update an existing `LexicalResource`. He might want to modify internal links, links to other resources, or just change fields such as synset descriptions. We now discuss in detail some relevant case for synsets.


#### Appending synsets to an upper ontology

If you just want to append synsets under existing ones in an upper ontology, without actually modifying that ontology, It is sufficient to create a new `LexicalResource` XML and link synsets of another XML by using `SynsetRelations`:

<img src="img/extension-2.png">

#### Inserting middle synsets

Suppose you have an upper ontology with two `Synsets` linked by a `hypernym` relation, and you want to extend the relation by inserting a third node between the two:

<img src="img/extension-0.png">

Currently, to achieve this goal without manually modifying the original resource, you can create a new `LexicalResource` holding the middle synset linked with a `hypernym` (a canonical relation) to the top node, and a `hyponym` (a non-canonical relation) to the bottom node. When importing the new `LexicalResource` Diversicon will automatically run a normalization procedure, which will create edges of canonical relations such as `hypernym`:

<img src="img/extension-1.png">


#### Updating existing synsets

In some cases you might be forced to directly change the original `LexicalResource`. For example, you found a nice WordNet in your favourite language but quickly discovered some synsets have wrong relations and others don't have any description at all. So you want to fix relations and add missing descriptions: currently the best way to do this would be to create your own version of the `LexicalResource` and assign a different namespace to the resource. To do the changes, you could either do a manual DB edit, edit the original XML or :


##### Updating existing synsets: Manual DB edit

1. modify the original XML to assign a new namespace
2. import the XML into Diversicon
3. do other changes with some [Database browser](http://diversicon-eu.kb/manual/divercli/latest/Tools.html)
4. manually run the function `processGraph` to validate, normalize, and compute the transitive closure of the graph.

To keep track of changes, you could use some DB diff tool

##### Updating existing synsets: Edit original XML

1. modify the original XML to assign a new namespace and do other changes
2. import the modified XML into Diversicon

To keep track of changes, you could use some diff tool, even versioning with git (not ideal especially if 
the XML is huge, but could still work).


### Delete

Currently, there is no special facility for deleting stuff. If you try to do it manually the DB might also complain that you are violating some constraint (for `SynsetRelation` you don't have constraints). Probably in many cases if you need to get rid of a `LexicalResource` you could just create an empty database and reimport all the XMLs.




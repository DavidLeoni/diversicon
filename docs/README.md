<p class="josman-to-strip">
WARNING: WORK IN PROGRESS - THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE <a href="http://davidleoni.github.io/diversicon/" target="_blank">PROJECT WEBSITE</a>
</p>

This release allows to TODO. <!--If you are upgrading from previous version, see [Release notes](CHANGES.md).-->

### Getting started

**With Maven**: If you use Maven as build system, put this in the `dependencies` section of your `pom.xml`:

```xml
    <dependency>
        <groupId>eu.kidf</groupId>
        <artifactId>diversicon-core</artifactId>
        <version>#{version}</version>
    </dependency>
```

**Without Maven**: you can download Diversicon jar and its dependencies <a href="/releases/download/diversicon-#{version}/diversicon-#{version}.zip" target="_blank"> from here</a>, then copy the jars to your project classpath.


In case updates are available, version numbers follow <a href="http://semver.org/" target="_blank">semantic versioning</a> rules.


### XML Validation

XML Validation is [`XmlValidationConfig`](../src/main/java/eu/kidf/diversicon/core/XmlValidationConfig.java)

1. XML Schema 1.0 validation, according to $eval{eu.kidf.diversicon.core.Diversicons.SCHEMA_1_PUBLIC_URL}
2. the custom class `DivXmlValidator` performs further validation in three steps:	 
	1. XML structure and metadata are coherent
	2. internal XML references are satisfied (i.e. _phablet_ links to existing _smartphone_ synset)
	
By default, plain XML validation is done in _non-strict_ mode, that is, it doesn't fail on warnings.


### XML Import

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
5. graph is normalized
	5.1 [Domains](DiversiconLMF.md#Domains) are identified and linked to $eval{eu.kidf.diversicon.data.SYNSET_ROOT_DOMAIN} 
	5.2 [Canonical relations](DiversiconLMF.md#Canonical-relations) are materialized	
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



### Logging

Diversicon uses <a href="http://www.slf4j.org" target="_blank">SLF4J </a> logging system. Library has only slf4j-api as dependency. In tests we provide <a href="http://logback.qos.ch/" target="_blank"> logback</a> as slf4j implementation. They are configured by default via xml files looked upon  in this order :

1. whatever is passed by command line: ` mvn test -Dlogback.configurationFile=path-to-my-logback.xml`
2. `conf/logback-test.xml` as indicated in Maven surefire plugin configuration 
3. `logback-test.xml` in [test resources](src/test/resources/logback-test.xml). 

CAVEAT: stupid Eclipse doesn't pick those Surefire properties [by design](https://bugs.eclipse.org/bugs/show_bug.cgi?id=388683) , nor allows to apply run settings to all tests (O_o) so I went to `Windows->Preferences->Java->Installed JREs->Default one->Edit` and set default VM arguments to `-Dlogback.configurationFile=conf/logback-test.xml`. It's silly but could somewhat make sense for other projects too. 
 

<p class="josman-to-strip">
WARNING: WORK IN PROGRESS - THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE <a href="http://davidleoni.github.io/diversicon/" target="_blank">PROJECT WEBSITE</a>
</p>

This release allows to TODO. <!--If you are upgrading from previous version, see [Release notes](CHANGES.md).-->

### Getting started

**With Maven**: If you use Maven as build system, put this in the `dependencies` section of your `pom.xml`:

```xml
    <dependency>
        <groupId>it.unitn.disi</groupId>
        <artifactId>diversicon</artifactId>
        <version>#{version}</version>
    </dependency>
```

**Without Maven**: you can download Diversicon jar and its dependencies <a href="../releases/download/diversicon-#{version}/diversicon-#{version}.zip" target="_blank"> from here</a>, then copy the jars to your project classpath.


In case updates are available, version numbers follow <a href="http://semver.org/" target="_blank">semantic versioning</a> rules.

### Search ckan

#### Get the dataset list of dati.trentino.it:

TODO 

Code can be found in <a href="../src/test/java/eu/trentorise/opendata/jackan/test/ckan/TestApp1.java" target="_blank">TestApp1.java</a>

```java

import eu.trentorise.opendata.jackan.CkanClient;

public class TestApp1 {

    public static void main(String[] args) {

        CkanClient cc = new CkanClient("http://dati.trentino.it");
        System.out.println(cc.getDatasetList());

    }
}

```



### Logging

Diversicon uses <a href="http://www.slf4j.org" target="_blank">SLF4J </a> logging system. Library has only slf4j-api as dependency. In tests we provide <a href="http://logback.qos.ch/" target="_blank"> logback</a> as slf4j implementation. They are configured by default via xml files looked upon  in this order :

1. whatever is passed by command line: ` mvn test -Dlogback.configurationFile=path-to-my-logback.xml`
2. `conf/logback-test.xml` as indicated in Maven surefire plugin configuration 
3. `logback-test.xml` in [test resources](src/test/resources/logback-test.xml). 

CAVEAT: stupid Eclipse doesn't pick those surefire properties [by design](https://bugs.eclipse.org/bugs/show_bug.cgi?id=388683) , nor allows to apply run settings to all tests (O_o) so I went to `Windows->Preferences->Java->Installed JREs->Default one->Edit` and set default VM arguments to `-Dlogback.configurationFile=conf/logback-test.xml`. It's silly but could somewhat make sense for other projects too. 
 

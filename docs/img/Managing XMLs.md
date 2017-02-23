
### Generating the schema from the dtd

Assuming you have this folder structure:

```
diversicon/prj
diversicon-model/prj
```

From `diversicon/prj` folder, run

```
mvn exec:java  -Dexec.args="s"
```

This will generate `diversicon-model/prj/src/main/resources/website/schema/1.0/diversicon.xsd`


### XML Schema 1.1

* [Definitive XML Schema book](http://www.datypic.com/books/defxmlschema/chapter14.html):  A precious cookbook for xml schema 1.1 assertions!
* [IBM guide](http://www.ibm.com/developerworks/library/x-xml11pt2/)
* [Important point](http://stackoverflow.com/a/25344978) about child/parent assertion dependencies
* [Quckit tutorial]( http://www.quackit.com/xml/tutorial/xml_default_namespace.cfm) on understanding namespaces
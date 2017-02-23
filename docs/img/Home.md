
[Ide setup](#ide-setup)

[Guidelines](#guidelines)

[Documentation](#documentation)
_________________________


In this wiki we only put information about development of Diversicon. For usage information please refer to [the project website](http://davidleoni.github.io/diversicon)


### IDE setup

As IDE we currently prefer Eclipse

#### Java and Maven
We use JDK 7 and Maven 3.3.9

#### Eclipse

Eclipse seems to have less problems than Netbeans in handling Immutables annotations.

##### Eclipse Setup

* install Eclipse Mars
* install `Checkstyle` plugin http://eclipse-cs.sourceforge.net
* Assuming you have the downloaded source files with git on your computer, do 

`
File->Import->Maven->Existing Maven project -> select directory where project is -> Click finish
`

##### Eclipse formatting

* download <a href="https://github.com/opendatatrentino/tod-commons/raw/master/tod-eclipse-format-style.xml" target="_blank">
tod-eclipse-format-style.xml</a> file from <a href="../blob/master/tod-eclipse-format-style.xml" target=_"blank">tod-commons repository </a>
* set the project format style:

```
right click on newly created project 
-> Properties 
-> Java Code Style 
-> Check Enable project specific settings 

Then as Active Profile, choose Import...  and select  the previously downloaded file.
```

##### Eclipse issues

If there are issues in compiling, try the following:
* update the project frequently with Project->Clean
* select the project in Project Explorer and hitting F5
* select the project in Project Explorer and select `Delete` (will not delete project from disk unless you explicitly say so in pop up). Then do File-Import -> from Maven

#### Netbeans

Setup
* Install Netbeans 8.0.2
* todo write about md plugin?

In theory you just need to open the project and that's it.


### Guidelines

#### Relation with Uby

Diversicon should only be a thin wrapper over [UBY 0.7.0](https://github.com/dkpro/dkpro-uby), so we don't want to add many dependencies or do weird stuff. [Diversicon](https://github.com/DavidLeoni/diversicon/blob/master/src/main/java/it/unitn/disi/diversicon/Diversicon.java) class extends Uby.
for LMF XML to import we use the format specified by the [Uby DTD 0.7.0](https://github.com/dkpro/dkpro-uby/blob/de.tudarmstadt.ukp.uby-0.7.0/de.tudarmstadt.ukp.uby.lmf.model-asl/src/main/resources/dtd/UBY_LMF.dtd) .

#### Name casing
We follow Google's <a href="http://google-styleguide.googlecode.com/svn/trunk/javaguide.html#s5.3-camel-case" target="_blank">naming coding style</a>.

#### Libraries
We try to keep Diversicon as self-contained as possible. When a feature is not satisfactorily covered by vanilla Java 7 we copy/paste from  <a href="https://github.com/google/guava" target="_blank"> Guava library </a>. Code must work in a Java 7 environment.

#### Null values
Null values are generally avoided and empty objects used instead. When no good empty object is found (for example for dates), we use `null`, marking type with JSR-305 `@Nullable` annotation (we considered using <a href="https://code.google.com/p/guava-libraries/wiki/UsingAndAvoidingNullExplained#Optional">Guava's Optional&lt;T&gt;</a> but is a bit verbose and might give issues with json serialization)

#### Logging
We use SLF4J API. As implementation during testing, we use Logback.

#### Exceptions

We don't use checked exceptions (the ones inheriting from `Exception`) and prefer runtime exceptions (that inherit from `RuntimeException`), so users can avoid try-catch boilerplate. Thrown exceptions _MUST_ be properly documented.


### Documentation

For documentation we use the program <a href="https://github.com/opendatatrentino/josman" target="_blank">Josman</a> and follow its guidelines.


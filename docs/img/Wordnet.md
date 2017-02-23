
[Wordnet](#wordnet)

[Uby Wordnet](#uby-wordnet)

______________________



### Wordnet

Wordnet 3.1 comes with textual db files in custom text format, see [WordNet manual for reference](https://wordnet.princeton.edu/man/wninput.5WN.html). They can hold several semantic relations, but the most important ones for us are the commonly called _IS-A_ and _PART-OF_, which we now describe in detail. Some good explanation can also be found [here](http://www.text-analytics101.com/2014/10/demystifying-nouns-in-wordnet.html).

#### IS-A relation

The _IS-A_ relation (for example _collie IS-A shepherd_dog_), in linguistic terms can be also rephrased as _collie hasHypernym shepherd_dog_. Specifically, in WordNet we can find written a relation called `hypernym` (which uses `@` as shorthand), that can be actually read like  _hasHypernym_.  So in the db you would find the edge

_collie_ `hypernym` _shepherd_dog_

stated in a format like:

```
02106030 05 n 01 collie 0 001 @ 02104523 n 0000 | a silky-coated sheepdog with a long ruff and long narrow head developed in Scotland
```
where _02104523_ is the ID of _shepherd_dog_

The inverse of `hypernym` would be `hyponym` (with shorthand `~`)

Note original Wordnet db files have BOTH `hypernym` and `hyponym` edges.

##### IS-A for instances

There is also the special case for when an instance IS-A somenthing, like _Italy IS-A European_Country_.  In linguistic terms can be also rephrased as _Italy hasInstanceHypernym European_country_. Specifically, in WordNet we can find written a relation called `instance hypernym` (which uses `@i` as shorthand), that can be actually read like  _hasInstanceHypernym_.  So in the db you would find the edge


_Italy_ `instanceHypernym` _European_country_

stated in a format like:

```
08819530 15 n 03 Italy 0 Italian_Republic 0 Italia 0 078 @i 08714745 n 0000 #p 09298379 n 0000 #m 08190414 n 0000 #m 08191297 n 0000 + 02968612 a 0101 -r 01154838 a 0000 -r 01276493 n 0000 -r 01276664 n 0000 -r 01285678 n 0000 -r ...<REDACTED>.. | a republic in southern Europe on the Italian Peninsula; was the core of the Roman Republic and the Roman Empire between the 4th century BC and the 5th century AD
```
where _08714745_ is the ID of _European_country_

The inverse of `instance hypernym` would be `instance hyponym` (with shorthand `~i`)

Note original Wordnet db files have BOTH `instance hypernym` and `instance hyponym` edges.


#### PART-OF relation

The _PART-OF_ relation (for example _arm PART-OF armchair_) in linguistic terms can be also be rephrased as _arm hasPartHolonym armchair_. Specifically, in WordNet we can find written three kind of PART-OF relations, `part holonym`, `member holonym` and `substance holonym`. We will focus on the only transitive one, `part holonym`.  It uses `#p` as shorthand, and can be actually read like  _hasPartHolonym_.  So in the db you would find the edge

_arm_ `part holonym` _shepherd_dog_

stated in a format like:

```
02737660 06 n 01 arm 2 003 @ 02741475 n 0000 #p 02738535 n 0000 ~ 04607982 n 0000 | the part of an armchair or sofa that supports the elbow and forearm of a seated person
```
where _02741475_ is the ID of _armchair_.

The inverse of `part holonym` would be `part meronym` (with shorthand `%p`)

Note original Wordnet db files have BOTH `part holonym` and `part meronym` edges.

### Uby Wordnet

Uby converter 0.7.0 on Wordnet 3.0 produces these synset relations. 
Notice that although `antonym` relations are present in Wordnet, they don't get imported!

```
usage
meronymSubstance
synonymNear
verbGroup
hyponymInstance
meronymMember
holonymPart
hypernymInstance
isUsageOf
topic
hyponym
holonymMember
region
isRegionOf
hypernym
holonymSubstance
entails
seeAlso
isTopicOf
attribute
causedBy
meronymPart
```





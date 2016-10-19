declare variable $in-file external;

copy $root := doc($in-file)
modify (        


    insert node comment{concat(
" 

       Note this is XML Schema 1.1 version, and contains assertions to check constraints among *values*.
       For this reason it requires XSD 1.1 compatible parsers. A plain XML Schema 1.0 version is
       available at http://diversicon-kb.eu/schema/1/diversicon.xsd
        

")} as second into $root,


    insert node (
    <xs:assert test="every $prefixed-id in ./*//@id satisfies fn:starts-with($prefixed-id, fn:concat(@prefix, '_'))">		
        <xs:annotation>        
                <xs:appinfo>
                    Internal ids must begin with the "prefix" assigned to the "LexicalResource"
                </xs:appinfo>
                <xs:documentation>
                    When this assertion fails, the content of the above "appinfo" is used
                    to produce the error message.
                </xs:documentation>
        </xs:annotation>		
    </xs:assert>
    ) as last into $root//xs:element[@name="LexicalResource"]/xs:complexType,

    
    insert node (    
    <xs:assert test="every $target in .//SynsetRelation[fn:starts-with(@target, fn:concat(../../../@prefix, '_'))]/@target satisfies fn:boolean(.//Synset[@id=$target])">

        <!--
            We can't use keyrefs as tests are not allowed in keyref selector, see
            9.2.5. Permitted XPath Expressions in
            http://docstore.mik.ua/orelly/xml/schema/ch09_02.htm

            Also, notice that above I couldn't use /@prefix nor /LexicalResource/@prefix to acess the root 
        --> 
            		
        <xs:annotation>
                <xs:appinfo>
                    SynsetRelation target that begins with document prefix must point to a Synset declared within the document  
                </xs:appinfo>
                <xs:documentation>
                TODO
                </xs:documentation>
        </xs:annotation>		
    </xs:assert>                     
    ) as last into $root//xs:element[@name="LexicalResource"]/xs:complexType,    

    insert node (    
    <xs:assert test="every $synset in .//Sense[fn:starts-with(@synset, fn:concat(../../../@prefix, '_'))]/@synset satisfies fn:boolean(.//Synset[@id=$synset])">

        <!--
            We can't use keyrefs as tests are not allowed in keyref selector, see
            9.2.5. Permitted XPath Expressions in
            http://docstore.mik.ua/orelly/xml/schema/ch09_02.htm

            Also, notice that above I couldn't use /@prefix nor /LexicalResource/@prefix to acess the root 
        --> 
            		
        <xs:annotation>
                <xs:appinfo>
                    Senses linking to synsets that begins with document prefix, must point to a Synset declared within the document  
                </xs:appinfo>
                <xs:documentation>
                TODO
                </xs:documentation>
        </xs:annotation>		
    </xs:assert>                     
    ) as last into $root//xs:element[@name="LexicalResource"]/xs:complexType    

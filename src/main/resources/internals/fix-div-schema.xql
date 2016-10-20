declare variable $in-file external;

copy $root := doc($in-file)
modify (    
    
    insert node comment{concat(
" 
       SCHEMA GENERATED FROM DIVERSICON DTD ON ", fn:adjust-date-to-timezone(current-date(), ()),"
       DO *NOT* EDIT DIRECTLY.
       FOR CHANGES MADE TO ORIGINAL UBY DTD AND COPYRIGHT, SEE BELOW 

")} as first into $root,

	insert node (      	
	<xs:annotation>
	    <xs:documentation xml:lang="en">
	     	XML Schema for Diversicon LMF XMLs
	     		     		     
	     	Derived from DTD
	     	https://github.com/diversicon-kb/diversicon/blob/master/src/main/resources/diversicon-1.0.dtd
	     	
	     	Which in turn is derived from original UBY DTD at v0.7.0:
	     	https://github.com/dkpro/dkpro-uby/blob/de.tudarmstadt.ukp.uby-0.7.0/de.tudarmstadt.ukp.uby.lmf.model-asl/src/main/resources/dtd/UBY_LMF.dtd
	     		     		    
	    </xs:documentation>
	  </xs:annotation> 				
    ) as first into $root/xs:schema     


)
return $root 

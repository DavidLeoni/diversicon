declare variable $prefix := 'wn31';
declare variable $in-file := 'src/test/resources/experiments/xml/basex-2.xml';
declare variable $new-prefix := 'my-id';

declare variable $prefix-colon := concat($prefix, '_');

copy $root := doc($in-file)
modify (		
	for $d in $root//@*[starts-with(.,$prefix-colon)]
		return replace value of node $d with replace($d, $prefix-colon, concat($new-prefix, "."))	
			    
) 
return $root




declare variable $prefix := 'wn31';
declare variable $filepath := 'src/test/resources/experiments/xml/basex-2.xml';
declare variable $new-prefix := 'my-id';

declare variable $prefix-colon := concat($prefix, ':');

copy $c := doc($filepath)
modify (		
	for $d in $c//@*[starts-with(.,$prefix-colon)]
		return replace value of node $d with replace($d, $prefix-colon, concat($new-prefix, ":"))	
			    
) 
return $c




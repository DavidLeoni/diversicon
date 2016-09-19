declare variable $old-prefix external;
declare variable $new-prefix external;
declare variable $infile external;


declare variable $old-prefix-colon := concat($old-prefix, ':');
copy $c := doc($infile)
modify (
	for $d in $c//@*[starts-with(.,$old-prefix-colon)]
	return replace value of node $d with replace($d, $old-prefix-colon, concat($new-prefix, ":")),
	insert node <author>Joey</author> into $c
)
return $c

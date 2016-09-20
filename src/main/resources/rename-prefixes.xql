declare variable $old-prefix external;
declare variable $new-prefix external;
declare variable $in-file external;


declare variable $old-prefix-colon := concat($old-prefix, ':');
copy $root := doc($in-file)
modify (
	for $d in $root//@*[starts-with(.,$old-prefix-colon)]
	return replace value of node $d with replace($d, $old-prefix-colon, concat($new-prefix, ":")),
	insert node <author>Joey</author> into $root
)
return $root

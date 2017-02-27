declare variable $old-prefix external;
declare variable $new-prefix external;
declare variable $in-file external;

(: 
   Replaces old-prefix with new-prefix in file in-file..
   
   NOTE: you will still have to fix manually the xmlns: with the old prefix !
:)

declare variable $old-prefix-colon := concat($old-prefix, '_');
copy $root := doc($in-file)
modify (
        replace value of node $root//@prefix with $new-prefix,
<------>for $d in $root//@*[starts-with(.,$old-prefix-colon)]
<------>return replace value of node $d with replace($d, $old-prefix-colon, concat($new-prefix, "_"))
<------>...........
)
return $root

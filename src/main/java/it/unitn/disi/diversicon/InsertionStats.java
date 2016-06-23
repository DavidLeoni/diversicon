package it.unitn.disi.diversicon;

import static it.unitn.disi.diversicon.internal.Internals.checkArgument;
import static it.unitn.disi.diversicon.internal.Internals.checkNotEmpty;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Statistics about edge insertions into the SynsetRelation graph.
 * 
 * @since 0.1
 *
 */
class InsertionStats {
    
    private long edgesPriorTransitiveClosure;
    private int maxLevel;
    private Map<String, Long> map;
    
    
    /**
     * @since 0.1
     */
    public InsertionStats(){
        this.setEdgesPriorTransitiveClosure(0);
        this.setMaxLevel(0);
        this.map = new HashMap();
    }

    /**
     * Increments provided relation name
     * 
     * @since 0.1
     */
    public void inc(String relName){
        checkNotEmpty(relName, "Invalid key!");
        if (map.containsKey(relName)){
            map.put(relName, map.get(relName) + 1);
        } else {
            map.put(relName, 1L);
        }
    }

    public Set<String> relNames() {
        return map.keySet();
    }
    
    /**
     * The number of relations {@code relName}
     * @since 0.1
     */    
    public long count(String relName){
        Long ret = map.get(relName);
        if (ret == null){
            return 0L;
        } else {
            return ret;
        }
    }

    /**
     * Returns total number of added edges. 
     * 
     * @since 0.1
     */    
    public long totEdges() {
        long ret = 0;
        for (Long v : map.values()){
            ret += v;
        }
        return ret;
    }

    /**
     * Returns a nice report of the insertions.
     * 
     * @since 0.1
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
                
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        long tot = totEdges();
        if (tot == 0){
            sb.append("   No edges were added to the " + nf.format(edgesPriorTransitiveClosure) + " existing ones. \n");
        } else {
            
            sb.append("   Max level:      " + maxLevel + "\n");
            sb.append("   Initial edges:  " + nf.format(edgesPriorTransitiveClosure) + "\n");
            sb.append("   Inserted edges: " + nf.format(tot)+ "  . Details:\n");
            for (String relName : relNames()){
                sb.append("        " + relName + ":   " + nf.format(count(relName)) + "\n");
            }
        }
        sb.append("\n");
        
        return sb.toString();
    }

    /**
     * 
     * @since 0.1
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * 
     * @param maxLevel must be >= 0;
     * 
     * @since 0.1 
     */
    public void setMaxLevel(int maxLevel) {
        checkArgument(maxLevel >= 0, "Invalid level, must be >= 0, found instead ", maxLevel);
        this.maxLevel = maxLevel;
    }

    public long getEdgesPriorTransitiveClosure() {
        return edgesPriorTransitiveClosure;
    }

    /**
     * @since 0.1
     */
    public void setEdgesPriorTransitiveClosure(long edgesPriorTransitiveClosure) {
        checkArgument(maxLevel >= 0, "Invalid number of edges prior closure, must be >= 0, found instead ", edgesPriorTransitiveClosure);
        this.edgesPriorTransitiveClosure = edgesPriorTransitiveClosure;
    }    
    
}
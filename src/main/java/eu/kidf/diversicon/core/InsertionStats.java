package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkArgument;
import static eu.kidf.diversicon.core.internal.Internals.checkNotEmpty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.kidf.diversicon.core.internal.Internals;


/**
 * Statistics about edge insertions into the SynsetRelation graph.
 * 
 * @since 0.1.0
 *
 */
public class InsertionStats {
    
    private long edgesPriorInsertion;
    private int maxLevel;
    private Map<String, Long> map;
    
    
    /**
     * @since 0.1.0
     */
    public InsertionStats(){
        this.setEdgesPriorInsertion(0);
        this.setMaxLevel(0);
        this.map = new HashMap<>();
    }

    /**
     * Increments provided relation name
     * 
     * @since 0.1.0
     */
    public void inc(String relName){
        checkNotEmpty(relName, "Invalid key!");
        if (map.containsKey(relName)){
            map.put(relName, map.get(relName) + 1);
        } else {
            map.put(relName, 1L);
        }
    }

    /**
     * @since 0.1.0
     */    
    public Set<String> relNames() {
        return map.keySet();
    }
    
    /**
     * The number of relations {@code relName}
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
                
        
        long tot = totEdges();
        if (tot == 0){
            sb.append("   No edges were added to the " + Internals.formatInteger(edgesPriorInsertion) + " existing ones. \n");
        } else {
            
            sb.append("   Max level:      " + maxLevel + "\n");
            sb.append("   Initial edges:  " + Internals.formatInteger(edgesPriorInsertion) + "\n");
            sb.append("   Inserted edges: " + Internals.formatInteger(tot)+"\n");
            for (String relName : relNames()){
                sb.append("        " + relName + ":   " + Internals.formatInteger(count(relName)) + "\n");
            }
        }
        
        return sb.toString();
    }

    /**
     * 
     * @since 0.1.0
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * 
     * @param maxLevel must be >= 0;
     * 
     * @since 0.1.0 
     */
    public void setMaxLevel(int maxLevel) {
        checkArgument(maxLevel >= 0, "Invalid level, must be >= 0, found instead ", maxLevel);
        this.maxLevel = maxLevel;
    }

    /**
     * @since 0.1.0
     */    
    public long getEdgesPriorInsertion() {
        return edgesPriorInsertion;
    }

    /**
     * @since 0.1.0
     */
    public void setEdgesPriorInsertion(long edgesPriorInsertion) {
        checkArgument(edgesPriorInsertion >= 0, "Invalid number of edges prior closure, must be >= 0, found instead ", edgesPriorInsertion);
        this.edgesPriorInsertion = edgesPriorInsertion;
    }   
    
}
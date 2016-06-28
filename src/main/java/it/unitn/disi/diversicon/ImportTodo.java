package it.unitn.disi.diversicon;

import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;





/**
 * Class to describe an import process of a {@link LexicalResource} into the db.
 * 
 * @see ImportConfig
 * @since 0.1.0
 */
public class ImportTodo {

    private ImportConfig importConfig;
    private List<ImportJob> singleImports;

    /**
     * @since 0.1.0
     */
    public ImportTodo() {
        this.importConfig = new ImportConfig();
        this.singleImports = new ArrayList();
    }

    /**
     * @since 0.1.0
     */
    public ImportConfig getImportConfig() {
        return importConfig;
    }

    /**
     * @see {@link #getImportConfig()}
     * @since 0.1.0
     */
    public void setImportConfig(ImportConfig importConfig) {
        checkNotNull(importConfig);
        this.importConfig = importConfig;
    }

    /**
     * @since 0.1.0
     */
    public List<ImportJob> getSingleImports() {
        return singleImports;
    }

    /**
     * @see {@link #getSingleImports()}
     * @since 0.1.0
     */
    public void setSingleImports(List<ImportJob> singleImports) {
        checkNotNull(singleImports);
        this.singleImports = singleImports;
    }
    
    /**
     * @throws DivNotFoundException
     */
    public Date startDate(){
        if (singleImports.isEmpty()){
            throw new DivNotFoundException("No import has been made!");
        }
        Date ret = singleImports.get(0).getStartDate();
        if (ret == null){
            throw new DivNotFoundException("No import has started yet!");
        }
        
        return ret;
        
    }

    public boolean started(){
        return !(singleImports.isEmpty()) 
                && (singleImports.get(0).getStartDate() != null);                  
    }

    public boolean finished(){
        return !(singleImports.isEmpty()) 
                && (singleImports.get(singleImports.size()-1).getEndDate() != null);
    }
    
    
    /**
     * @throws DivNotFoundException
     */
    public Date endDate(){
        if (singleImports.isEmpty()){
            throw new DivNotFoundException("No import has been made!");
        }
        Date ret = singleImports.get(singleImports.size() - 1).getEndDate();
        if (ret == null){
            throw new DivNotFoundException("Import is not ended yet!");
        }
        
        return ret;
        
    }
    
}

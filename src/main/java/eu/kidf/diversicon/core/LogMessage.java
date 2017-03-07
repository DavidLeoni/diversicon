package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

import javax.annotation.Nullable;

import org.slf4j.event.Level;

public class LogMessage {
    
    // backlink
    private ImportJob importJob;
    
    private Level level;
    private String message;    
    
    
    public LogMessage(@Nullable ImportJob importJob, Level level, String message){
        checkNotNull(level);
        checkNotNull(message);
        checkNotNull(importJob);
        
        this.level = level;
        this.message = message;
        this.setImportJob(importJob);
    }
    
    public LogMessage(){
        this.setMessage("");        
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        checkNotNull(message);
        this.message = message;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        checkNotNull(level);
        this.level = level;
    }

    public ImportJob getImportJob() {
        return importJob;
    }

    public void setImportJob(ImportJob importJob) {
        checkNotNull(importJob);
        this.importJob = importJob;
    }
    
    
}

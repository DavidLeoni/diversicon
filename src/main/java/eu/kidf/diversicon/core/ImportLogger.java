package eu.kidf.diversicon.core;

import static eu.kidf.diversicon.core.internal.Internals.checkNotNull;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;


/**
 * @deprecated Probably useless
 * 
 * @since 0.1.0
 */
public class ImportLogger implements Logger {
    
    private Diversicon diversicon;
    
    public ImportLogger(Diversicon diversicon){
        checkNotNull(diversicon);
        this.diversicon = diversicon;
    }
        
    @Override
    public String getName() {        
        return "Diversicon logger."; 
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String msg) {
        
    }

    @Override
    public void trace(String format, Object arg) {
        
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        
    }

    @Override
    public void trace(String format, Object... arguments) {
        
    }

    @Override
    public void trace(String msg, Throwable t) {
        
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    @Override
    public void trace(Marker marker, String msg) {
        
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(String msg) {
        
    }

    @Override
    public void debug(String format, Object arg) {
        
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        
    }

    @Override
    public void debug(String format, Object... arguments) {
        
    }

    @Override
    public void debug(String msg, Throwable t) {
        
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    @Override
    public void debug(Marker marker, String msg) {
        
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public void info(String msg) {
        
    }

    @Override
    public void info(String format, Object arg) {
        
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        
    }

    @Override
    public void info(String format, Object... arguments) {
        
    }

    @Override
    public void info(String msg, Throwable t) {
        
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    @Override
    public void info(Marker marker, String msg) {
        
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        ImportJob importJob = diversicon.getDbInfo().getCurrentImportJob();
        importJob.addLogMessage(new LogMessage(importJob, Level.WARN, msg));
    }

    @Override
    public void warn(String format, Object arg) {
        ImportJob importJob = diversicon.getDbInfo().getCurrentImportJob();
        importJob.addLogMessage(new LogMessage(importJob, Level.WARN,  MessageFormatter.format(format, arg).toString()));
    }

    @Override
    public void warn(String format, Object... arguments) {
        ImportJob importJob = diversicon.getDbInfo().getCurrentImportJob();
        importJob.addLogMessage(new LogMessage(importJob, Level.WARN,  MessageFormatter.arrayFormat(format, arguments).toString()));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        ImportJob importJob = diversicon.getDbInfo().getCurrentImportJob();
        importJob.addLogMessage(new LogMessage(importJob, Level.WARN,  MessageFormatter.format(format, arg1, arg2).toString()));
    }

    @Override
    public void warn(String msg, Throwable t) {
        ImportJob importJob = diversicon.getDbInfo().getCurrentImportJob();
        importJob.addLogMessage(new LogMessage(importJob, Level.WARN,  msg + t.toString()));
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public void warn(Marker marker, String msg) {       
        this.warn(msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        this.warn(format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        this.warn(format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        this.warn(format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        this.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        ImportJob importJob = diversicon.getDbInfo().getCurrentImportJob();
        importJob.addLogMessage(new LogMessage(importJob, Level.ERROR, msg));
    }

    @Override
    public void error(String format, Object arg) {
        ImportJob importJob = diversicon.getDbInfo().getCurrentImportJob();
        importJob.addLogMessage(new LogMessage(importJob, Level.ERROR,  MessageFormatter.format(format, arg).toString()));
    }

    @Override
    public void error(String format, Object... arguments) {
        ImportJob importJob = diversicon.getDbInfo().getCurrentImportJob();
        importJob.addLogMessage(new LogMessage(importJob, Level.ERROR,  MessageFormatter.arrayFormat(format, arguments).toString()));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        ImportJob importJob = diversicon.getDbInfo().getCurrentImportJob();
        importJob.addLogMessage(new LogMessage(importJob, Level.ERROR,  MessageFormatter.format(format, arg1, arg2).toString()));
    }

    @Override
    public void error(String msg, Throwable t) {
        ImportJob importJob = diversicon.getDbInfo().getCurrentImportJob();
        importJob.addLogMessage(new LogMessage(importJob, Level.ERROR,  msg + t.toString()));
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    @Override
    public void error(Marker marker, String msg) {
        this.error(msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        this.error(format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        this.error(format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        this.error(format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        this.error(msg, t);
    }


}

package it.unitn.disi.diversicon;

import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;


class DivLoggerFactory implements ILoggerFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getLogger(String name) {
        /* Just not supported!
          
         if (Logger.ROOT_LOGGER_NAME.equals(name)){
            throw new UnsupportedOperationException("There is no " + Logger.ROOT_LOGGER_NAME + " for diversicon logging system!");
        }
        try {
            Integer.parseInt(name);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Supported logger names are ");
        }
        Diversicon.ofSession();
        */
        throw new UnsupportedOperationException("Getting logger by name si just unsupported!");
    }
    
}

public class DivLogger implements Logger {

    private ImportJob importJob;
    private Diversicon diversicon;

    public DivLogger(ImportJob importJob, Diversicon diversicon){
        checkNotNull(importJob);
        this.importJob = importJob;
    }
        
    @Override
    public String getName() {
        return diversicon.getDbConfig().getJdbc_url() + " importJobId- " + importJob; 
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
        importJob.addLogMessage(new LogMessage(importJob, Level.WARN, msg));
    }

    @Override
    public void warn(String format, Object arg) {
        importJob.addLogMessage(new LogMessage(importJob, Level.WARN,  MessageFormatter.format(format, arg).toString()));
    }

    @Override
    public void warn(String format, Object... arguments) {
        importJob.addLogMessage(new LogMessage(importJob, Level.WARN,  MessageFormatter.arrayFormat(format, arguments).toString()));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        importJob.addLogMessage(new LogMessage(importJob, Level.WARN,  MessageFormatter.format(format, arg1, arg2).toString()));
    }

    @Override
    public void warn(String msg, Throwable t) {
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
        importJob.addLogMessage(new LogMessage(importJob, Level.ERROR, msg));
    }

    @Override
    public void error(String format, Object arg) {
        importJob.addLogMessage(new LogMessage(importJob, Level.ERROR,  MessageFormatter.format(format, arg).toString()));
    }

    @Override
    public void error(String format, Object... arguments) {
        importJob.addLogMessage(new LogMessage(importJob, Level.ERROR,  MessageFormatter.arrayFormat(format, arguments).toString()));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        importJob.addLogMessage(new LogMessage(importJob, Level.ERROR,  MessageFormatter.format(format, arg1, arg2).toString()));
    }

    @Override
    public void error(String msg, Throwable t) {
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

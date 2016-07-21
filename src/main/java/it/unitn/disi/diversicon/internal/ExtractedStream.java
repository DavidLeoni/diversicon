package it.unitn.disi.diversicon.internal;

import static it.unitn.disi.diversicon.internal.Internals.checkNotEmpty;
import static it.unitn.disi.diversicon.internal.Internals.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.disi.unitn.diversicon.exceptions.DivIoException;
import it.unitn.disi.diversicon.Diversicons;

/**
 * A stream possibly extracted from a compressed {@code sourceUrl}. Use
 * {#stream()} to get the stream to consume. If you need to consume the stream several times, 
 * call {@link #toTempFile()} first.
 * 
 * @since 0.1.0
 *
 */
public class ExtractedStream {
    
    private static final Logger LOG = LoggerFactory.getLogger(Diversicons.class);    
    
    private String filepath;
    private InputStream inputStream;
    private String sourceUrl;
    private boolean extracted;

    @Nullable
    private File tempFile;

    /**
     * @param inputStream must not be already consumed.
     * @param sourceUrl
     *            may have special {@code classpath} protocol
     * 
     * @since 0.1.0
     */
    public ExtractedStream(String filepath, InputStream inputStream, String sourceUrl, boolean extracted) {
        super();
        checkNotEmpty(filepath, "invalid filepath!");
        checkNotNull(inputStream);
        checkNotEmpty(sourceUrl, "Invalid sourceUrl!");
        this.filepath = filepath;
        this.inputStream = inputStream;
        this.sourceUrl = sourceUrl;
        this.extracted = extracted;
    }

    /**
     * 
     * Returns the name of item as found inside the compressed resource.
     * 
     * @since 0.1.0
     */
    public String getFilepath() {
        return filepath;
    }

    /**
     * The url of the original possibly compressed resource. May start with
     * special {@code classpath:} protocol.
     * 
     * @since 0.1.0
     */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * 
     * The stream of the possibly extracted file. If {@link #toTempFile()} was already called, 
     * each call will produce a new stream.
     * 
     * @throws DivIoException
     * 
     * @since 0.1.0
     */
    public InputStream stream() {
        
        if (tempFile == null) {            
            return inputStream;
        } else {
            try {
                return new FileInputStream(tempFile);
            } catch (FileNotFoundException ex) {
                throw new DivIoException("Error while creating stream!", ex);
            }
        }

    }

    /**
     * Returns true if the stream was extracted from another compressed
     * file.
     * 
     * @since 0.1.0
     */
    public boolean isExtracted() {
        return extracted;
    }

    /**
     * Returns a file pointing to a physical location in the computer.
     * Calling this method will consume the stream. Do not write to the generated file.
     *
     * @throws DivIoException
     * 
     * @since 0.1.0
     */
    public File toTempFile() {
        try {
            if (this.tempFile == null) {
                if (extracted) {
                    Path tempDir = Internals.createTempDivDir("extracted");
                    this.tempFile = new File(tempDir.toFile(), FilenameUtils.getName(this.filepath));
                    LOG.debug("Writing stream to " + tempFile.getAbsolutePath() + " ...");
                    FileUtils.copyInputStreamToFile(this.inputStream, tempFile);
                    LOG.debug("Done writing stream to " + tempFile.getAbsolutePath());
                } else {
                    if (sourceUrl.startsWith("classpath:")) {
                        Path tempDir = Files.createTempDirectory("extracted");
                        this.tempFile = new File(tempDir.toFile(), FilenameUtils.getName(this.filepath));
                        LOG.debug("Writing stream to " + tempFile.getAbsolutePath() + " ...");
                        FileUtils.copyInputStreamToFile(this.inputStream, tempFile);

                    } else if (sourceUrl.startsWith("file:") || !Internals.withProtocol(sourceUrl)) {
                        this.tempFile = new File(sourceUrl);
                    } else {

                        Path tempDir = Internals.createTempDivDir("extracted");
                        this.tempFile = new File(tempDir.toFile(), FilenameUtils.getName(this.filepath));
                        LOG.debug("Writing url to " + tempFile.getAbsolutePath() + " ...");
                        FileUtils.copyURLToFile(new URL(sourceUrl), tempFile, 20000, 10000);
                        LOG.debug("Done writing url to " + tempFile.getAbsolutePath());
                    }

                }
                LOG.debug("Using file at " + tempFile.getAbsolutePath());
            }

            return this.tempFile;

        } catch (IOException ex) {
            throw new DivIoException("Error while creating file!", ex);
        }
    }
}
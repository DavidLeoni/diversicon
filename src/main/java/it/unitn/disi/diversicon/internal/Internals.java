package it.unitn.disi.diversicon.internal;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rits.cloning.Cloner;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.unitn.disi.diversicon.DivIoException;
import it.unitn.disi.diversicon.Diversicons;

/**
 * 
 * Utility toolbox for Diversicon internal usage.
 * 
 * DO NOT USE THIS CLASS OUTSIDE OF DIVERSICON PROJECT. THANKS.
 *
 */
public final class Internals {

    private static final Logger LOG = LoggerFactory.getLogger(Diversicons.class);

    private static final @Nullable Cloner cloner = new Cloner();

    private Internals() {

    }

    /**
     *
     * Checks if provided string is non null and non empty.
     *
     * @param errorMessageTemplate
     *            a template for the exception message should the check fail.
     *            The message is formed by replacing each {@code %s} placeholder
     *            in the template with an argument. These are matched by
     *            position - the first {@code %s} gets {@code
     *     errorMessageArgs[0]}, etc. Unmatched arguments will be appended to
     *            the formatted message in square braces. Unmatched placeholders
     *            will be left as-is.
     * @param errorMessageArgs
     *            the arguments to be substituted into the message template.
     *            Arguments are converted to strings using
     *            {@link String#valueOf(Object)}.
     * @throws IllegalArgumentException
     *             if {@code expression} is false
     * @throws NullPointerException
     *             if the check fails and either {@code errorMessageTemplate} or
     *             {@code errorMessageArgs} is null (don't let this happen)
     *
     *
     * @throws IllegalArgumentException
     *             if provided string fails validation
     *
     * @return the non-empty string that was validated
     * @since 0.1
     */
    public static String checkNotEmpty(String string, @Nullable String errorMessageTemplate,
            @Nullable Object... errorMessageArgs) {
        String formattedMessage = format(errorMessageTemplate, errorMessageArgs);
        checkArgument(string != null, "%s -- Reason: Found null string.", formattedMessage);
        if (string.length() == 0) {
            throw new IllegalArgumentException(formattedMessage + " -- Reason: Found empty string.");
        }
        return string;
    }

    /**
     *
     * Checks if provided string is non null and {@link #isBlank(String) non
     * blank}.
     *
     * @param errorMessageTemplate
     *            a template for the exception message should the check fail.
     *            The message is formed by replacing each {@code %s} placeholder
     *            in the template with an argument. These are matched by
     *            position - the first {@code %s} gets {@code
     *     errorMessageArgs[0]}, etc. Unmatched arguments will be appended to
     *            the formatted message in square braces. Unmatched placeholders
     *            will be left as-is.
     * @param errorMessageArgs
     *            the arguments to be substituted into the message template.
     *            Arguments are converted to strings using
     *            {@link String#valueOf(Object)}.
     * @throws IllegalArgumentException
     *             if {@code expression} is false
     * @throws NullPointerException
     *             if the check fails and either {@code errorMessageTemplate} or
     *             {@code errorMessageArgs} is null (don't let this happen)
     *
     *
     * @throws IllegalArgumentException
     *             if provided string fails validation
     *
     * @return the non-blank string that was validated
     * @since 0.1
     */
    public static String checkNotBlank(String string, @Nullable String errorMessageTemplate,
            @Nullable Object... errorMessageArgs) {
        String formattedMessage = format(errorMessageTemplate, errorMessageArgs);
        checkArgument(string != null, "%s -- Reason: Found null string.", formattedMessage);
        if (string.isEmpty()) {
            throw new IllegalArgumentException(formattedMessage + " -- Reason: Found empty string.");
        }

        if (string.trim()
                  .isEmpty()) {
            throw new IllegalArgumentException(formattedMessage + " -- Reason: Found string with blank characters!");
        }

        return string;
    }

    /**
     *
     * Checks if provided iterable is non null and non empty .
     *
     * @param prependedErrorMessage
     *            the exception message to use if the check
     *            fails; will be converted to a string using
     *            String.valueOf(Object) and
     *            prepended to more specific error messages.
     *
     * @throws IllegalArgumentException
     *             if provided collection fails validation
     *
     * @return a non-null non-empty iterable
     * 
     * @since 0.1
     */
    public static <T> Iterable<T> checkNotEmpty(@Nullable Iterable<T> iterable,
            @Nullable Object prependedErrorMessage) {
        checkArgument(iterable != null, "%s -- Reason: Found null iterable.", prependedErrorMessage);
        if (isEmpty(iterable)) {
            throw new IllegalArgumentException(
                    String.valueOf(prependedErrorMessage) + " -- Reason: Found empty collection.");
        }
        return iterable;
    }

    /**
     *
     * Checks if provided array is non null and non empty .
     *
     * @param prependedErrorMessage
     *            the exception message to use if the check
     *            fails; will be converted to a string using
     *            String.valueOf(Object) and
     *            prepended to more specific error messages.
     *
     * @throws IllegalArgumentException
     *             if provided array fails validation
     *
     * @return a non-null non-empty array
     * 
     * @since 0.1
     */
    public static <T> T[] checkNotEmpty(@Nullable T[] array, @Nullable Object prependedErrorMessage) {
        checkArgument(array != null, "%s -- Reason: Found null array.", prependedErrorMessage);
        if (array.length == 0) {
            throw new IllegalArgumentException(
                    String.valueOf(prependedErrorMessage) + " -- Reason: Found empty array.");
        }
        return array;
    }

    /**
     *
     * Checks if provided iterable is non null and non empty .
     *
     * @param errorMessageTemplate
     *            a template for the exception message should
     *            the check fail. The message is formed by replacing each
     *            {@code %s}
     *            placeholder in the template with an argument. These are
     *            matched by
     *            position - the first {@code %s} gets {@code
     *     errorMessageArgs[0]}, etc. Unmatched arguments will be appended to
     *            the
     *            formatted message in square braces. Unmatched placeholders
     *            will be left
     *            as-is.
     * @param errorMessageArgs
     *            the arguments to be substituted into the message
     *            template. Arguments are converted to strings using
     *            {@link String#valueOf(Object)}.
     * @throws IllegalArgumentException
     *             if {@code iterable} is empty or null
     * @throws NullPointerException
     *             if the check fails and either
     *             {@code errorMessageTemplate} or {@code errorMessageArgs} is
     *             null (don't
     *             let this happen)
     *
     * @return a non-null non-empty iterable
     * 
     * @since 0.1
     */
    public static <T> Iterable<T> checkNotEmpty(@Nullable Iterable<T> iterable,
            @Nullable String errorMessageTemplate,
            @Nullable Object... errorMessageArgs) {
        String formattedMessage = format(errorMessageTemplate, errorMessageArgs);

        checkArgument(iterable != null, "%s -- Reason: Found null iterable.", formattedMessage);
        if (isEmpty(iterable)) {
            throw new IllegalArgumentException(formattedMessage + " -- Reason: Found empty iterable.");
        }
        return iterable;
    }

    /**
     *
     * Checks if provided array is non null and non empty .
     *
     * @param errorMessageTemplate
     *            a template for the exception message should
     *            the check fail. The message is formed by replacing each
     *            {@code %s}
     *            placeholder in the template with an argument. These are
     *            matched by
     *            position - the first {@code %s} gets
     *            {@code errorMessageArgs[0]}, etc.
     *            Unmatched arguments will be appended to the formatted message
     *            in square
     *            braces. Unmatched placeholders will be left as-is.
     * @param errorMessageArgs
     *            the arguments to be substituted into the message
     *            template. Arguments are converted to strings using
     *            {@link String#valueOf(Object)}.
     * @throws IllegalArgumentException
     *             if {@code array} is empty or null
     * @throws NullPointerException
     *             if the check fails and either
     *             {@code errorMessageTemplate} or {@code errorMessageArgs} is
     *             null (don't
     *             let this happen)
     *
     * @return a non-null non-empty array
     * 
     * @since 0.1
     */
    public static <T> T[] checkNotEmpty(@Nullable T[] array,
            @Nullable String errorMessageTemplate,
            @Nullable Object... errorMessageArgs) {

        String formattedMessage = format(errorMessageTemplate, errorMessageArgs);

        checkArgument(array != null, "%s -- Reason: Found null iterable.", formattedMessage);
        if (array.length == 0) {
            throw new IllegalArgumentException(formattedMessage + " -- Reason: Found empty array.");
        }
        return array;
    }

    /**
     * Determines if the given iterable contains no elements.
     *
     * (copied from Guava Preconditions)
     *
     * <p>
     * There is no precise {@link Iterator} equivalent to this method, since
     * one can only ask an iterator whether it has any elements <i>remaining</i>
     * (which one does using {@link Iterator#hasNext}).
     *
     * @return {@code true} if the iterable contains no elements
     * 
     * @since 0.1
     */
    public static boolean isEmpty(Iterable<?> iterable) {
        if (iterable instanceof Collection) {
            return ((Collection<?>) iterable).isEmpty();
        }
        return !iterable.iterator()
                        .hasNext();
    }

    /**
     *
     * Checks if provided string is non null and non empty.
     *
     * @param prependedErrorMessage
     *            the exception message to use if the check fails; will be
     *            converted to a string using String.valueOf(Object) and
     *            prepended to more specific error messages.
     *
     * @throws IllegalArgumentException
     *             if provided string fails validation
     *
     * @return the non-empty string that was validated
     * @since 0.1
     */
    public static String checkNotEmpty(String string, @Nullable Object prependedErrorMessage) {
        checkArgument(string != null, "%s -- Reason: Found null string.", prependedErrorMessage);
        if (string.length() == 0) {
            throw new IllegalArgumentException(
                    String.valueOf(prependedErrorMessage) + " -- Reason: Found empty string.");
        }
        return string;
    }

    /**
     *
     * Substitutes each {@code %s} in {@code template} with an argument. These
     * are matched by position: the first {@code %s} gets {@code args[0]}, etc.
     * If there are more arguments than placeholders, the unmatched arguments
     * will be appended to the end of the formatted message in square braces.
     * <br/>
     * <br/>
     * (Copied from Guava's
     * {@link com.google.common.base.Preconditions#format(java.lang.String, java.lang.Object...) }
     * )
     *
     * @param template
     *            a non-null string containing 0 or more {@code %s}
     *            placeholders.
     * @param args
     *            the arguments to be substituted into the message template.
     *            Arguments are converted to strings using
     *            {@link String#valueOf(Object)}. Arguments can be null.
     *
     * @since 0.1
     */
    public static String format(String template, @Nullable Object... args) {
        if (template == null) {
            LOG.warn("Found null template while formatting, converting it to \"null\"");
        }
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;
        int i = 0;
        while (i < args.length) {
            int placeholderStart = template.indexOf("%s", templateStart);
            if (placeholderStart == -1) {
                break;
            }
            builder.append(template.substring(templateStart, placeholderStart));
            builder.append(args[i++]);
            templateStart = placeholderStart + 2;
        }
        builder.append(template.substring(templateStart));

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            builder.append(" [");
            builder.append(args[i++]);
            while (i < args.length) {
                builder.append(", ");
                builder.append(args[i++]);
            }
            builder.append(']');
        }

        return builder.toString();
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to
     * the calling method.
     *
     * (Copied from Guava Preconditions)
     * 
     * @param expression
     *            a boolean expression
     * @throws IllegalArgumentException
     *             if {@code expression} is false
     * @since 0.1
     */
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to
     * the calling method.
     *
     * (Copied from Guava Preconditions)
     * 
     * @param expression
     *            a boolean expression
     * @param errorMessage
     *            the exception message to use if the check fails; will be
     *            converted to a string using {@link String#valueOf(Object)}
     * @throws IllegalArgumentException
     *             if {@code expression} is false
     * @since 0.1
     */
    public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to
     * the calling method.
     *
     * (Copied from Guava Preconditions)
     *
     * @param expression
     *            a boolean expression
     * @param errorMessageTemplate
     *            a template for the exception message should the check fail.
     *            The message is formed by replacing each {@code %s} placeholder
     *            in the template with an argument. These are matched by
     *            position - the first {@code %s} gets {@code
     *     errorMessageArgs[0]}, etc. Unmatched arguments will be appended to
     *            the formatted message in square braces. Unmatched placeholders
     *            will be left as-is.
     * @param errorMessageArgs
     *            the arguments to be substituted into the message template.
     *            Arguments are converted to strings using
     *            {@link String#valueOf(Object)}.
     * @throws IllegalArgumentException
     *             if {@code expression} is false
     * @throws NullPointerException
     *             if the check fails and either {@code errorMessageTemplate} or
     *             {@code errorMessageArgs} is null (don't let this happen)
     * @since 0.1
     */
    public static void checkArgument(boolean expression, @Nullable String errorMessageTemplate,
            @Nullable Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
        }
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling
     * method is not null. (Copied from Guava)
     * 
     * @param reference
     *            an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException
     *             if {@code reference} is null
     * @since 0.1
     */
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling
     * method is not null.
     * 
     * (Copied from Guava)
     * 
     * @param reference
     *            an object reference
     * @param errorMessage
     *            the exception message to use if the check fails; will be
     *            converted to a string using {@link String#valueOf(Object)}
     * @return the non-null reference that was validated
     * @throws NullPointerException
     *             if {@code reference} is null
     * @since 0.1
     */
    public static <T> T checkNotNull(T reference, @Nullable Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }

    /**
     * Creates a map from key value pairs
     * 
     * @since 0.1
     */
    public static <V, W> HashMap<V, W> newMap(V v, W w, Object... data) {
        HashMap<V, W> result = new HashMap();

        if (data.length % 2 != 0)
            throw new IllegalArgumentException("Odd number of arguments");

        V key = null;
        Integer step = -1;

        if (v == null) {
            throw new IllegalArgumentException("Null key value");
        }

        result.put(v, w);

        for (Object d : data) {
            step++;
            switch (step % 2) {
            case 0:
                if (d == null) {
                    throw new IllegalArgumentException("Null key value");
                }

                if (!v.getClass()
                      .isInstance(d)) {
                    throw new IllegalArgumentException(
                            "Expected key " + d + " to be instance of class " + v.getClass());
                }
                key = (V) d;
                continue;
            case 1:
                if (w != null && !w.getClass()
                                   .isInstance(d)) {
                    throw new IllegalArgumentException(
                            "Expected value " + d + " to be instance of class " + w.getClass());
                }

                W val = (W) d;
                result.put(key, val);
                break;
            }
        }

        return result;
    }

    /**
     * Returns a deep copy of any object, including non-serializable ones.
     * 
     * @since 0.1
     */
    public static <T> T deepCopy(T orig) {
        return cloner.deepClone(orig);
    }

    /**
     * Returns a new ArrayList, filled with provided objects.
     * 
     * (Note {@code Arrays.asList(T...)} returns an {@code Arrays.ArrayList}
     * instead)
     * 
     * @since 0.1
     */
    public static <T> ArrayList<T> newArrayList(T... objs) {
        ArrayList<T> ret = new ArrayList();

        for (T obj : objs) {
            ret.add(obj);
        }
        return ret;
    }

    /**
     * Returns a new HashSet, filled with provided objects.
     * 
     * @since 0.1
     */
    public static <T> HashSet<T> newHashSet(T... objs) {
        HashSet<T> ret = new HashSet();

        for (T obj : objs) {
            ret.add(obj);
        }
        return ret;
    }

    /**
     * Checks provided depth.
     * 
     * @param depth
     *            must be >= -1, otherwise IllegalArgumentException is thrown.
     * 
     * @since 0.1
     */
    public static void checkDepth(int depth) {
        if (depth < -1) {
            throw new IllegalArgumentException("depth must be greater or equal to -1, found instead " + depth);
        }
    }

    /**
     * Returns true if provided string is null or blank
     * 
     * @since 0.1
     */
    public static boolean isBlank(@Nullable String string) {
        return string == null || (string != null && string.trim()
                                                          .isEmpty());
    }

    /**
     * Checks provided {@code dbConfig} points to an H2 database.
     * 
     * @throws IllegalArgumentException
     * 
     * @since 0.1
     */
    public static void checkH2(DBConfig dbConfig) {
        checkNotNull(dbConfig);
        if (!dbConfig.getJdbc_driver_class()
                     .contains("h2")) {
            throw new IllegalArgumentException("Only H2 database is supported for now! Found instead "
                    + Diversicons.toString(dbConfig, false));
        }
    }

    /**
     * @since 0.1
     */
    private static boolean isFormatSupported(String filePath,
            String[] formats) {
        checkNotEmpty(filePath, "Invalid filepath!");

        for (String s : formats) {
            if (filePath.toLowerCase()
                        .endsWith("." + s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets input stream from a url pointing to data. If data is compressed in
     * one of
     * {@link Diversicons#SUPPORTED_COMPRESSION_FORMATS} it is uncompressed, but
     * no check is done to verify
     * the archive contains only one file.
     * 
     * Supported compression formats are
     * 
     * @param dataUrl
     *            can be like:
     *            <ul>
     *            <li>classpath:/my/package/name/data.zip</li
     *            <li>file:/my/path/data.zip</li>
     *            <li>http://... or whatever protocol..</li>
     *            </ul>
     * @throws DivIoException
     *             on error.
     * 
     * @since 0.1
     */
    // todo should check archives have only one file...
    public static ExtractedStream readData(String dataUrl) {
        checkNotNull(dataUrl, "Invalid resource path!");

        @Nullable
        InputStream inputStream = null;

        URI uri;
        try {
            uri = new URI(dataUrl);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Couldn't parse input url!", ex);
        }

        LOG.trace("reading data from " + dataUrl + " ...");
        
        if ("classpath".equals(uri.getScheme())) {
            String q = dataUrl.substring("classpath:".length());          
            
            inputStream = Diversicons.class.getResourceAsStream(q);
            if (inputStream == null) {

                try {
                    
                    String candidatePathTest = "src/test/resources" + q;
                    LOG.trace("Searching data in " + candidatePathTest + " ...");
                    inputStream = new FileInputStream(candidatePathTest);
                    LOG.debug("Located data in " + candidatePathTest);
                } catch (FileNotFoundException ex1) {
                    try {
                        String candidatePathMain = "src/main/resources" + q;
                        LOG.trace("Searching data in " + candidatePathMain + " ...");
                        inputStream = new FileInputStream(candidatePathMain);
                        LOG.debug("Located data in " + candidatePathMain);
                    } catch (FileNotFoundException ex2) {
                        throw new DivIoException("Couldn't find input stream: " + dataUrl.toString());
                    }
                }

            } else {
                LOG.debug("Located data in " + dataUrl);
            }
        } else {
            try {
                inputStream = new URL(dataUrl).openStream();
                LOG.debug("Located data in " + dataUrl);
            } catch (IOException ex) {
                throw new DivIoException("Error while opening lexical resource " + dataUrl + "  !!", ex);
            }
        }

        if (isFormatSupported(uri.getPath(), Diversicons.SUPPORTED_COMPRESSION_FORMATS)) {

            try {
                                
                if (isFormatSupported(uri.getPath(), Diversicons.SUPPORTED_ARCHIVE_FORMATS)){
                    
                    ArchiveInputStream zin = new ArchiveStreamFactory()
                            .createArchiveInputStream(inputStream);
                    for (ArchiveEntry e; (e = zin.getNextEntry()) != null;) {
                        return new ExtractedStream(e.getName(), zin, dataUrl, true);
                    }
                       
                } else {
                    
                    CompressorInputStream cin = new CompressorStreamFactory()
                            .createCompressorInputStream(inputStream);
                      String fname = FilenameUtils.getBaseName(uri.getPath());
                      return new ExtractedStream(fname, cin, dataUrl, true);                                                             
                }                              

            } catch (IOException | ArchiveException | CompressorException e) {
                throw new DivIoException("Error while iterating through " + dataUrl.toString() + " !", e);
            }

            throw new DivIoException("Found empty stream in archive " + dataUrl.toString() + " !");

        } else {
            return new ExtractedStream(uri.getPath(), inputStream, dataUrl, false);
        }
    }

    /**
     * A stream possibly extracted from a compressed {@code sourceUrl}. Use
     * {#stream()} to get streams.
     * 
     * @since 0.1
     *
     */
    public static class ExtractedStream {
        private String filepath;
        private InputStream inputStream;
        private String sourceUrl;
        private boolean extracted;
        @Nullable
        private File outFile;

        /**
         * @param sourceUrl
         *            may have special {@code classpath} protocol
         * 
         * @since 0.1
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
         * @since 0.1
         */
        public String getFilepath() {
            return filepath;
        }

        /**
         * The url of the original possibly compressed resource. May start with
         * special {@code classpath:} protocol.
         * 
         * @since 0.1
         */
        public String getSourceUrl() {
            return sourceUrl;
        }

        /**
         * 
         * The stream of the possibly extracted file. Each call will produce a
         * new stream.
         * 
         * @throws DivIoException
         * 
         * @since 0.1
         */
        public InputStream stream() {
            if (outFile == null) {
                return inputStream;
            } else {
                try {
                    return new FileInputStream(outFile);
                } catch (FileNotFoundException ex) {
                    throw new DivIoException("Error while creating stream!", ex);
                }
            }

        }

        /**
         * Returns true if the stream was extracted from another compressed
         * file.
         * 
         * @since 0.1
         */
        public boolean isExtracted() {
            return extracted;
        }

        /**
         * Returns a file pointing to a physical location in the computer.
         * Calling this method will consume the stream.
         *
         * @throws DivIoException
         * 
         * @since 0.1
         */
        public File toFile() {
            try {
                if (this.outFile == null) {
                    if (extracted) {
                        this.outFile = Files.createTempFile("diversicon", this.filepath)
                                            .toFile();
                        FileUtils.copyInputStreamToFile(this.inputStream, outFile);
                    } else {
                        if (sourceUrl.startsWith("classpath:")) {
                            this.outFile = Files.createTempFile("diversicon", this.filepath)
                                                .toFile();
                            FileUtils.copyInputStreamToFile(this.inputStream, outFile);

                        } else if (sourceUrl.startsWith("file:")) {
                            this.outFile = new File(sourceUrl);
                        } else {
                            this.outFile = Files.createTempFile("diversicon", this.filepath)
                                                .toFile();
                            FileUtils.copyURLToFile(new URL(sourceUrl), outFile, 20000, 10000);
                        }

                    }
                    LOG.debug("created tempfile at " + outFile.getAbsolutePath());
                }

                return this.outFile;

            } catch (IOException ex) {
                throw new DivIoException("Error while creating file!", ex);
            }
        }
    }

    /**
     * if outPath is something like a/b/c.sql.zip and ext is sql, it becomes c.sql 
     * 
     * If it is something like a/b/c.zip and ext is sql, it becomes c.sql
     * 
     * @since 0.1
     */
    public static String makeExtension(String path, String ext) {
        checkNotBlank(path, "Invalid path!");
        checkNotBlank(ext, "Invalid extension!");
        
        String entryName = FilenameUtils.getBaseName(path);
        if (entryName.endsWith("." + ext)){
            return entryName;            
        } else {
            return entryName.concat("." + ext);
        }
    }

}

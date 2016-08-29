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
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rits.cloning.Cloner;

import de.tudarmstadt.ukp.lmf.transform.DBConfig;
import it.disi.unitn.diversicon.exceptions.DivException;
import it.disi.unitn.diversicon.exceptions.DivIoException;
import it.unitn.disi.diversicon.Diversicon;
import it.unitn.disi.diversicon.Diversicons;

/**
 * 
 * Utility toolbox for Diversicon internal usage.
 * 
 * DO NOT USE THIS CLASS OUTSIDE OF DIVERSICON PROJECT. THANKS.
 *
 */
public final class Internals {

    public final static String DIVERSICON_STRING = "diversicon";

    private static final Logger LOG = LoggerFactory.getLogger(Internals.class);

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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
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
     * @since 0.1.0
     */
    public static void checkDepth(int depth) {
        if (depth < -1) {
            throw new IllegalArgumentException("depth must be greater or equal to -1, found instead " + depth);
        }
    }

    /**
     * Returns true if provided string is null or blank
     * 
     * @since 0.1.0
     */
    public static boolean isBlank(@Nullable String string) {
        return string == null || (string != null && string.trim()
                                                          .isEmpty());
    }

    /**
     * @since 0.1.0
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
     * Gets input stream from a url pointing to possibly compressed data.
     * 
     * @param dataUrl
     *            can be like:
     *            <ul>
     *            <li>{@code classpath:/my/package/name/data.zip}</li>
     *            <li>{@code file:/my/path/data.zip}</li>
     *            <li>{@code http://... }</li>
     *            <li>{@code jar:file:///home/user/data.jar!/my-file.txt}</li>
     *            <li>{@code jar:file:///home/user/data.jar!/my-file.txt.zip}</li>
     *            <li>whatever protocol..</li>
     *            </ul>
     * @param decompress
     *            if true and data is actually compressed in one of
     *            {@link Diversicons#SUPPORTED_COMPRESSION_FORMATS} returns the
     *            uncompressed stream (note no check is done to verify the
     *            archive contains only one file).
     *            In all other cases data stream is returned verbatim.
     * 
     * @throws DivIoException
     *             on error.
     * 
     * @since 0.1.0
     */
    // todo should check archives have only one file...
    public static ExtractedStream readData(String dataUrl, boolean decompress) {
        checkNotNull(dataUrl, "Invalid resource path!");

        @Nullable
        InputStream inputStream = null;

        URI uri;
        
        String uriPath;
        
        try {
            uri = new URI(dataUrl);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Couldn't parse input url!", ex);
        }

        LOG.trace("reading data from " + dataUrl + " ...");

        if ("classpath".equals(uri.getScheme())) {
            String q = dataUrl.substring("classpath:".length());
            uriPath = q;
            inputStream = Diversicons.class.getResourceAsStream(q);
            if (inputStream == null) {

                try {

                    String candidatePathTest = "src/test/resources/" + q;
                    LOG.trace("    Searching data in " + candidatePathTest + " ...");
                    inputStream = new FileInputStream(candidatePathTest);
                    LOG.debug("    Located data in " + candidatePathTest);
                } catch (FileNotFoundException ex1) {
                    try {
                        String candidatePathMain = "src/main/resources/" + q;
                        LOG.trace("    Searching data in " + candidatePathMain + " ...");
                        inputStream = new FileInputStream(candidatePathMain);
                        LOG.debug("    Located data in " + candidatePathMain);
                    } catch (FileNotFoundException ex2) {
                        throw new DivIoException("Couldn't find input stream: " + dataUrl.toString());
                    }
                }

            } else {
                LOG.debug("    Located data in " + dataUrl);
            }
        } else {
            
            if ("jar".equals(uri.getScheme())){                
                uriPath = getJarPath(dataUrl);
            } else {
                uriPath = uri.getPath();
            }
            
            try {

                if (hasProtocol(dataUrl)) {
                    inputStream = new URL(dataUrl).openStream();
                } else {
                    inputStream = new FileInputStream(dataUrl);
                }

                LOG.debug("    Located data in " + dataUrl);
            } catch (IOException ex) {
                throw new DivIoException("Error while opening lexical resource " + dataUrl + "  !!", ex);
            }
        }
        
        
        

        if (decompress && isFormatSupported(uriPath, Diversicons.SUPPORTED_COMPRESSION_FORMATS)) {

            try {

                BufferedInputStream buffered = inputStream instanceof BufferedInputStream
                        ? (BufferedInputStream) inputStream
                        : new BufferedInputStream(inputStream);

                if (isFormatSupported(uriPath, Diversicons.SUPPORTED_ARCHIVE_FORMATS)) {

                    ArchiveInputStream zin = new ArchiveStreamFactory()
                                                                       .createArchiveInputStream(buffered);
                    for (ArchiveEntry e; (e = zin.getNextEntry()) != null;) {
                        return new ExtractedStream(e.getName(), zin, dataUrl, true);
                    }

                } else {

                    CompressorInputStream cin = new CompressorStreamFactory()
                                                                             .createCompressorInputStream(buffered);
                    String fname = FilenameUtils.getBaseName(uriPath);
                    return new ExtractedStream(fname, cin, dataUrl, true);
                }

            } catch (IOException | ArchiveException | CompressorException e) {
                throw new DivIoException("Error while iterating through " + dataUrl.toString() + " !", e);
            }

            throw new DivIoException("Found empty stream in archive " + dataUrl.toString() + " !");

        } else {                                        
            return new ExtractedStream(uriPath, inputStream, dataUrl, false);                
            
        }
    }
    
    /**
     * @since 0.1.0
     */
    private static String getJarPath(String dataUrl){
        checkArgument(dataUrl.startsWith("jar:"), "Expected input to start with 'jar:', found instead " + dataUrl);
        String subDataUri = dataUrl.replace("jar:", "");
        try {
            return new URI(subDataUri).getPath();
        } catch (URISyntaxException e) {                
            throw new DivException("Couldn't parse subDataUrl " + subDataUri, e);
        }        
    }

    /**
     * @since 0.1.0
     */
    static boolean hasProtocol(String dataUrl) {
        checkNotBlank(dataUrl, "Invalid data url!");
        Pattern p = Pattern.compile("^(\\w)+:(.*)");
        Matcher m = p.matcher(dataUrl);

        return m.matches();
    }

    /**
     * if outPath is something like {@code a/b/c.sql.zi}p and {@code ext} is
     * {@code sql}, it becomes
     * {@code c.sql}
     * 
     * If it is something like {@code a/b/c.zip} and {@code ext} is {@code sql},
     * it becomes {@code c.sql}
     * 
     * @since 0.1.0
     */
    public static String makeExtension(String path, String ext) {
        checkNotBlank(path, "Invalid path!");
        checkNotBlank(ext, "Invalid extension!");

        String entryName = FilenameUtils.getBaseName(path);
        if (entryName.endsWith("." + ext)) {
            return entryName;
        } else {
            return entryName.concat("." + ext);
        }
    }

    /**
     *
     * Extracts the files starting with dirPath from {@code file} to
     * {@code destDir}
     *
     * (copied from Josman)
     *
     * @param dirPath
     *            the prefix used for filtering. If empty the whole jar
     *            content is extracted.
     *
     * @throws DivIoException
     * @since 0.1.0
     */
    public static void copyDirFromJar(File jarFile, File destDir, String dirPath) {
        checkNotNull(jarFile);
        checkNotNull(destDir);
        checkNotNull(dirPath);

        String normalizedDirPath;
        if (dirPath.startsWith("/")) {
            normalizedDirPath = dirPath.substring(1);
        } else {
            normalizedDirPath = dirPath;
        }

        try {
            JarFile jar = new JarFile(jarFile);
            java.util.Enumeration enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumEntries.nextElement();
                if (jarEntry.getName()
                            .startsWith(normalizedDirPath)) {
                    File f = new File(
                            destDir
                                    + File.separator
                                    + jarEntry
                                              .getName()
                                              .substring(normalizedDirPath.length()));

                    if (jarEntry.isDirectory()) { // if its a directory, create
                                                  // it
                        f.mkdirs();
                        continue;
                    } else {
                        f.getParentFile()
                         .mkdirs();
                    }

                    InputStream is = jar.getInputStream(jarEntry); // get the
                                                                   // input
                                                                   // stream
                    FileOutputStream fos = new FileOutputStream(f);
                    IOUtils.copy(is, fos);
                    fos.close();
                    is.close();
                }

            }
        } catch (IOException ex) {
            throw new DivIoException("Error while extracting jar file! Jar source: " + jarFile.getAbsolutePath()
                    + " destDir = " + destDir.getAbsolutePath(), ex);
        }
    }

    /**
     * Extracts the directory at resource path to target directory. First
     * directory is searched in local "src/test/resources" and
     * "src/main/resources" so the thing also
     * works when developing in the IDE. If not found then searches in jar file.
     * 
     * (adapted from Josman 0.7)
     * 
     * @throws DivIoException
     * 
     * @since 0.1.0
     */
    public static void copyDirFromResource(Class clazz, String sourceDirPath, File destDir) {

        String sep = File.separator;
        @Nullable
        File sourceDirFile = null;

        File testDir = new File("src" + sep + "test" + sep + "resources", sourceDirPath);
        if (testDir.exists()) {
            sourceDirFile = testDir;
        } else {
            File mainDir = new File("src" + sep + "main" + sep + "resources", sourceDirPath);
            if (mainDir.exists()) {
                sourceDirFile = mainDir;
            }
        }

        if (sourceDirFile != null) {
            LOG.debug("\nCopying directory ...\n"
                    + "  from:   {} \n"
                    + "  to  :   {}", 
                    sourceDirFile.getAbsolutePath(),
                    destDir.getAbsolutePath());
            try {
                FileUtils.copyDirectory(sourceDirFile, destDir);
                LOG.debug("Done copying directory");
            } catch (IOException ex) {
                throw new DivIoException("Couldn't copy the directory!", ex);
            }
        } else {

            File jarFile = new File(clazz.getProtectionDomain()
                                         .getCodeSource()
                                         .getLocation()
                                         .getPath());
            if (jarFile.isDirectory() && jarFile.getAbsolutePath()
                                                .endsWith("target" + File.separator + "classes")) {
                LOG.info("Seems like you have sources, will take resources from there");
                try {
                    FileUtils.copyDirectory(
                            new File(jarFile.getAbsolutePath() + "/../../src/main/resources", sourceDirPath), destDir);
                    LOG.info("Done copying directory");
                } catch (IOException ex) {
                    throw new DivIoException("Couldn't copy the directory!", ex);
                }
            } else {
                LOG.info("Extracting jar {} to {}", jarFile.getAbsolutePath(), destDir.getAbsolutePath());
                copyDirFromJar(jarFile, destDir, sourceDirPath);
                LOG.debug("Done copying directory from JAR.");
            }

        }
    }

    /**
     * 
     * @param millisecs
     *            time interval expressed in number of milliseconds
     * 
     * @since 0.1.0
     * 
     */
    public static String formatInterval(Date start, Date end) {
        checkNotNull(start);
        checkNotNull(end);

        return formatInterval(end.getTime() - start.getTime());
    }

    /**
     * Tries to return a correct eventually plural form
     * of {@code str} according to {@code quantity}
     * 
     * @since 0.1.0
     */
    private static String pluralize(String str, long quantity) {
        return (quantity > 1 ? str + "s" : str);
    }

    /**
     * 
     * @param millisecs
     *            time interval expressed in number of milliseconds
     * 
     * @since 0.1.0
     * 
     */
    public static String formatInterval(final long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        int hours = (int) ((millis / (1000 * 60 * 60)) % 24);
        int days = (int) ((millis / (1000 * 60 * 60 * 24)) % 365);
        int years = (int) (millis / (1000 * 60 * 60 * 24 * 365));

        ArrayList<String> timeArray = new ArrayList<String>();

        if (years > 0) {
            timeArray.add(String.valueOf(years) + pluralize("year", years));
        }

        if (days > 0) {
            timeArray.add(String.valueOf(days) + pluralize("day", days));
        }

        if (hours > 0) {

            timeArray.add(String.valueOf(hours) + pluralize("hour", hours));
        }

        if (minutes > 0) {
            timeArray.add(String.valueOf(minutes) + pluralize("min", minutes));
        }

        if (seconds > 0) {
            timeArray.add(String.valueOf(seconds) + pluralize("sec", seconds));
        }

        String time = "";
        for (int i = 0; i < timeArray.size(); i++) {
            time = time + timeArray.get(i);
            if (i != timeArray.size() - 1)
                time = time + ", ";
        }

        if (time == "") {
            time = "0 secs";
        }
        return time;
    }

    /**
     * 
     * 
     * @since 0.1.0
     */
    public static String humanByteCount(long bytes) {
        boolean si = true;

        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Formats a date in human readable format.
     * 
     * @since 0.1.0
     */
    public static String formatDate(Date date) {
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return dt.format(date);
    }

    /**
     * Returns a temporary file which is deleted on exit.
     * (to prevent deletion, set system property
     * {@link Diversicon#PROPERTY_DEBUG_KEEP_TEMP_FILES} to true).
     * 
     * @throws DivIoException
     * @since 0.1.0
     */
    public static Path createTempFile(Path dir, String prefix, String suffix) {
        checkNotNull(dir);
        checkNotNull(prefix);
        checkNotNull(suffix);

        final Path ret;
        try {
            ret = Files.createTempFile(prefix, suffix);
        } catch (IOException e) {
            throw new DivIoException("Couldn't create temporary directory!", e);
        }

        addDeleteHook(ret);

        return ret;

    }

    /**
     * Returns a temporary file which is deleted on exit
     * (to prevent deletion, set system property
     * {@link Diversicon#PROPERTY_DEBUG_KEEP_TEMP_FILES} to true).
     * 
     * @throws DivIoException
     * @since 0.1.0
     * 
     */
    public static Path createTempFile(String prefix, String suffix) {
        checkNotNull(prefix);
        checkNotNull(suffix);

        final Path ret;
        try {
            ret = Files.createTempFile(prefix, suffix);
        } catch (IOException e) {
            throw new DivIoException("Couldn't create temporary directory!", e);
        }

        addDeleteHook(ret);
        return ret;

    }

    /**
     * Quietly deletes the path at the end of the program
     * (to prevent deletion, set system property
     * {@link Diversicon#PROPERTY_DEBUG_KEEP_TEMP_FILES} to true).
     *
     * @since 0.1.0
     */
    private static void addDeleteHook(final Path path) {
        checkNotNull(path);

        @Nullable
        String prop = System.getProperty(Diversicon.PROPERTY_DEBUG_KEEP_TEMP_FILES);
        boolean keepTempFiles = Boolean.parseBoolean(prop);

        if (!(keepTempFiles)) {
            Runtime.getRuntime()
                   .addShutdownHook(new Thread() {
                       @Override
                       public void run() {
                           // According to this better solutions are
                           // problematic: http://stackoverflow.com/a/35212952
                           FileUtils.deleteQuietly(path.toFile());
                       }
                   });
        }
    }

    /**
     * Returns a temporary directory which is deleted on exit
     * (to prevent deletion, set system property
     * {@link Diversicon#PROPERTY_DEBUG_KEEP_TEMP_FILES} to true).
     * 
     * @throws DivIoException
     * @since 0.1.0
     * 
     */
    public static Path createTempDir(String prefix) {
        checkNotNull(prefix);

        final Path ret;
        try {
            ret = Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw new DivIoException("Couldn't create temporary directory!", e);
        }

        addDeleteHook(ret);
        return ret;

    }

    /**
     * Returns {@link #createTempDir(String) createTempDir("diversicon-" +
     * prefix)}
     *
     * @throws DivIoException
     * @since 0.1.0
     * 
     */
    public static Path createTempDivDir(String prefix) {
        return createTempDir("diversicon-" + prefix);
    }

    /**
     * @since 0.1.0
     */
    public static String formatInteger(long c) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        return nf.format(c);
    }

    /**
     * This method calculates edit distance between two words.
     * 
     *  <p>
     * There are three operations permitted on a word: replace, delete, insert. For example, the edit distance between "a" and "b" is 1, the edit distance between "abc" and "def" is 3. 
     * </p>
     * <p>
     * Taken from here: http://www.programcreek.com/2013/12/edit-distance-in-java/
     * </p>
     * @since 0.1.0
     */
    public static int editDistance(String word1, String word2) {
        int len1 = word1.length();
        int len2 = word2.length();

        // len1+1, len2+1, because finally return dp[len1][len2]
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        // iterate though, and check last char
        for (int i = 0; i < len1; i++) {
            char c1 = word1.charAt(i);
            for (int j = 0; j < len2; j++) {
                char c2 = word2.charAt(j);

                // if last two chars equal
                if (c1 == c2) {
                    // update dp value for +1 length
                    dp[i + 1][j + 1] = dp[i][j];
                } else {
                    int replace = dp[i][j] + 1;
                    int insert = dp[i][j + 1] + 1;
                    int delete = dp[i + 1][j] + 1;

                    int min = replace > insert ? insert : replace;
                    min = delete > min ? min : delete;
                    dp[i + 1][j + 1] = min;
                }
            }
        }

        return dp[len1][len2];
    }

}

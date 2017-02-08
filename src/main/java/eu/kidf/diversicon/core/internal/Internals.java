package eu.kidf.diversicon.core.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.HttpContext;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.dtd.DTDGrammar;
import org.apache.xerces.impl.dtd.XMLDTDLoader;
import org.apache.xerces.impl.dtd.XMLElementDecl;
import org.apache.xerces.util.SAXInputSource;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.rits.cloning.Cloner;
import de.tudarmstadt.ukp.lmf.model.core.LexicalResource;
import de.tudarmstadt.ukp.lmf.model.core.Sense;
import de.tudarmstadt.ukp.lmf.model.interfaces.IHasID;
import de.tudarmstadt.ukp.lmf.model.miscellaneous.EVarType;
import de.tudarmstadt.ukp.lmf.model.morphology.Lemma;
import de.tudarmstadt.ukp.lmf.model.semantics.SynsetRelation;
import de.tudarmstadt.ukp.lmf.transform.UBYLMFClassMetadata;
import de.tudarmstadt.ukp.lmf.transform.UBYLMFClassMetadata.UBYLMFFieldMetadata;
import eu.kidf.diversicon.core.DivSynsetRelation;
import eu.kidf.diversicon.core.DivXmlValidator;
import eu.kidf.diversicon.core.Diversicon;
import eu.kidf.diversicon.core.Diversicons;
import eu.kidf.diversicon.core.ImportConfig;
import eu.kidf.diversicon.core.LexResPackage;
import eu.kidf.diversicon.core.DivConfig;
import eu.kidf.diversicon.core.exceptions.DivException;
import eu.kidf.diversicon.core.exceptions.DivIoException;
import eu.kidf.diversicon.core.exceptions.DivNotFoundException;

/**
 * 
 * Utility toolbox for Diversicon internal usage.
 * 
 * <p>
 * <strong>DO NOT USE THIS CLASS OUTSIDE OF DIVERSICON PROJECT. THANKS.</strong>
 * </p>
 * 
 * @since 0.1.0
 */
public final class Internals {

    @Nullable
    private static Map<Class, List<UBYLMFFieldMetadata>> ORDER_TABLE = new HashMap<>();

    @Nullable
    private static DTDGrammar DTD_GRAMMAR;

    /**
     * @since 0.1.0
     */
    public static final String NON_EXISTING_URL_1 = "http://a-probe-non-existent.eu";

    /**
     * @since 0.1.0
     */
    public static final String NON_EXISTING_URL_2 = "http://b-probe-non-existent.eu";

    /**
     * 
     * 
     * @since 0.1.0
     */
    private static boolean checkedIsp = false;

    /**
     * Sinful ISPs that redirect on issed pages,
     * see <a href="https://github.com/diversicon-kb/diversicon-core/issues/29"
     * target="_blank">
     * issue 29 on Github</a>
     * 
     * <p>
     * Can be updated during program execution.
     * </p>
     * 
     * @since 0.1.0
     */
    private final static ConcurrentSkipListSet<String> redirectingISPs = new ConcurrentSkipListSet<>();

    /**
     * @since 0.1.0
     */
    public final static String DIVERSICON_STRING = "diversicon";

    /**
     * @since 0.1.0
     */
    private final static String FIX_DIV_SCHEMA_XQL = "classpath:/internals/fix-div-schema.xql";

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
        HashMap<V, W> result = new HashMap<>();

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
    @Nullable
    public static <T> T deepCopy(@Nullable T orig) {
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
        ArrayList<T> ret = new ArrayList<>();

        for (T obj : objs) {
            ret.add(obj);
        }
        return ret;
    }
    
    /**
     * Returns a new ArrayList, filled with provided objects.
     * 
     * (Note {@code Arrays.asList(T...)} returns an {@code Arrays.ArrayList}
     * instead)
     * 
     * @since 0.1.0
     */
    public static <T> ArrayList<T> newArrayList(Iterator<T> iter) {
        ArrayList<T> ret = new ArrayList<>();

        while (iter.hasNext()){            
            ret.add(iter.next());
        }
        return ret;
    }    

    /**
     * Returns a new HashSet, filled with provided objects.
     * 
     * @since 0.1.0
     */
    public static <T> HashSet<T> newHashSet(T... objs) {
        HashSet<T> ret = new HashSet<>();

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
     * Removes all forward slashes '/' at the beginning of {@code str}
     * 
     * @since 0.1.0
     */
    public static String removeStartSlashes(String str) {
        Pattern p = Pattern.compile("/*");
        Matcher m = p.matcher(str);

        if (m.find()) {
            return str.substring(m.end(), str.length());
        } else {
            return str;
        }
    }

    /**
     * @since 0.1.0
     */
    public static boolean hasProtocol(String dataUrl) {
        checkNotBlank(dataUrl, "Invalid data url!");
        Pattern p = Pattern.compile("^(\\w)+:(.*)");
        Matcher m = p.matcher(dataUrl);

        return m.matches();
    }

    /**
     * If outPath is something like {@code a/b/c.sql.zip} and {@code ext} is
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
     * 
     * @since 0.1.0
     */
    public static void copyDirFromJar(File jarFile, File destDir, String dirPath) {
        checkNotNull(jarFile);
        checkNotNull(destDir);
        checkNotNull(dirPath);

        String normalizedDirPath = removeStartSlashes(dirPath);

        try (JarFile jar = new JarFile(jarFile)) {

            java.util.Enumeration<JarEntry> enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumEntries.nextElement();
                if (jarEntry.getName()
                            .startsWith(normalizedDirPath)) {
                    File destFile = new File(
                            destDir.getAbsolutePath() + File.separator + jarEntry
                                                                                 .getName()
                                                                                 .substring(
                                                                                         normalizedDirPath.length()));

                    if (jarEntry.isDirectory()) { // if its a directory, create
                                                  // it
                        destFile.mkdirs();
                        continue;
                    } else {
                        destFile.getParentFile()
                                .mkdirs();
                    }

                    InputStream is = jar.getInputStream(jarEntry);
                    FileOutputStream destStream = new FileOutputStream(destFile);
                    IOUtils.copy(is, destStream);
                    destStream.close();
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
                LOG.debug("Extracting from jar {} to {}", relPath(jarFile), relPath(destDir));
                copyDirFromJar(jarFile, destDir, sourceDirPath);
                LOG.debug("Done copying directory from JAR.");
            }

        }
    }

    /**
     * Returns a short relative path to file, if possible, otherwise returns the
     * absolute path.
     * 
     * @since 0.1.0
     */
    public static String relPath(File file) {
        checkNotNull(file);

        if (file.toString()
                .equals(".")) {
            return file.toString() + "/";
        }

        File userDir = new File(System.getProperty("user.dir"));

        String userDirPath;
        if (userDir.getAbsolutePath()
                   .endsWith("/")) {
            userDirPath = userDir.getAbsolutePath();
        } else {
            userDirPath = userDir.getAbsolutePath() + "/";
        }

        try {
            if (file.getCanonicalPath()
                    .startsWith(userDirPath)) {
                return file.getAbsolutePath()
                           .substring(userDirPath.length());
            } else {
                return file.getAbsolutePath();
            }
        } catch (IOException e) {
            return file.getAbsolutePath();
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
        String prop = System.getProperty(Diversicons.PROPERTY_DEBUG_KEEP_TEMP_FILES);
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
     * <p>
     * There are three operations permitted on a word: replace, delete, insert.
     * For example, the edit distance between "a" and "b" is 1, the edit
     * distance between "abc" and "def" is 3.
     * </p>
     * <p>
     * Taken from here:
     * http://www.programcreek.com/2013/12/edit-distance-in-java/
     * </p>
     * 
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

    /**
     * Copied this from
     * {@link de.tudarmstadt.ukp.lmf.transform.UBYXMLTransform#doWriteElement}
     * 
     * <p>
     * Modified in Diversicon to
     * <ul>
     * <li>prevent writing wrong tag name DivSynsetRelation</li>
     * <li>add prefix to the LexicalResource</li>
     * <li>add namespaces to the LexicalResource</li>
     * <li>skip computed transitive relations</li>
     * <li>skip non-canonical relations</li>
     * </ul>
     * 
     * </p>
     * 
     * @return the element tagname. If {@code null}, the element must be
     *         skipped.
     * 
     * @throws SAXException
     * 
     * @since 0.1.0
     */
    @Nullable
    public static String prepareXmlElement(Object inputLmfObject,
            boolean closeTag,
            LexResPackage pack,
            UBYLMFClassMetadata classMetadata,
            AttributesImpl atts,
            List<Object> children) throws SAXException {

        checkNotNull(inputLmfObject);
        checkNotNull(atts);
        checkNotNull(children);
        // TODO think if it is necessary
        // Internals.checkLexResPackage(lexResPackage);
        checkNotNull(pack);
        
        
        Object lmfObject = inputLmfObject;
        String elementName = lmfObject.getClass()
                                      .getSimpleName();

        if (inputLmfObject instanceof LexicalResource) {

            atts.addAttribute("", "", "prefix", "CDATA",
                    pack.getPrefix());

            // note at the end of the method we put xmlns
        } else if (inputLmfObject instanceof DivSynsetRelation) {

            DivSynsetRelation dsr = (DivSynsetRelation) inputLmfObject;
            lmfObject = dsr.toSynsetRelation();
            elementName = SynsetRelation.class.getSimpleName();

            if (Diversicons.getProvenanceId()
                          .equals(dsr.getProvenance())) {
                if (dsr.getDepth() > 1) {
                    return null;
                }
                if (!Diversicons.isCanonicalRelation(dsr.getRelName())) {
                    return null;
                }
            }
        }

        int hibernateSuffixIdx = elementName.indexOf("_$$");
        if (hibernateSuffixIdx > 0)
            elementName = elementName.substring(0, hibernateSuffixIdx);

        for (UBYLMFFieldMetadata fieldMeta : extractFieldsInDtdOrder(classMetadata, elementName)) {
            EVarType varType = fieldMeta.getVarType();
            if (varType == EVarType.NONE)
                continue;

            String xmlFieldName = fieldMeta.getName()
                                           .replace("_", "");
            Method getter = fieldMeta.getGetter();
            Object retObj;
            try {
                retObj = getter.invoke(lmfObject);
            } catch (IllegalAccessException e) {
                throw new SAXException(e);
            } catch (InvocationTargetException e) {
                throw new SAXException(e);
            }

            if (retObj != null) {
                switch (fieldMeta.getVarType()) {
                case ATTRIBUTE:
                case ATTRIBUTE_OPTIONAL:
                    if (lmfObject instanceof Sense
                            && xmlFieldName.equals("index")
                            && "0".equals(retObj.toString())) {
                        break; // workaround for
                               // https://github.com/diversicon-kb/diversicon/issues/24
                    }
                    atts.addAttribute("", "", xmlFieldName, "CDATA", retObj.toString());
                    break;
                case CHILD:
                    // Transform children of the new element to XML
                    children.add(retObj);
                    break;
                case CHILDREN:
                    if (closeTag)
                        for (Object obj : (Iterable<Object>) retObj)
                            children.add(obj);
                    break;
                case IDREF:
                    if (lmfObject instanceof Lemma
                            && xmlFieldName.equals("lexicalEntry")) {
                        break; // workaround for
                               // https://github.com/diversicon-kb/diversicon/issues/22
                    }
                    // Save IDREFs as attribute of the new element
                    atts.addAttribute("", "", xmlFieldName, "CDATA", ((IHasID) retObj).getId());
                    break;
                case IDREFS:
                    StringBuilder attrValue = new StringBuilder();
                    for (Object obj : (Iterable<Object>) retObj)
                        attrValue.append(attrValue.length() > 0 ? " " : "")
                                 .append(((IHasID) obj).getId());
                    if (attrValue.length() > 0)
                        atts.addAttribute("", "", xmlFieldName, "CDATA", attrValue.toString());
                    break;
                case NONE:
                    break;
                }
            }
        }

        boolean foundXsi = false;
        String xsiPrefix = "xsi";

        if (inputLmfObject instanceof LexicalResource) {

            // note at the beginning of the method we put id and prefix
            for (String prefix : pack.getNamespaces()
                                              .keySet()) {
                String url = pack.getNamespaces()
                                          .get(prefix);
                atts.addAttribute("", "", "xmlns:" + prefix, "CDATA",
                        url);

                if (XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI.equals(url)) {
                    foundXsi = true;
                    xsiPrefix = prefix;
                }
            }

            if (!foundXsi) {
                atts.addAttribute("", "", "xmlns:" + xsiPrefix, "CDATA",
                        XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
            }

            atts.addAttribute("", "", xsiPrefix + ":noNamespaceSchemaLocation", "CDATA",
                    Diversicons.SCHEMA_1_0_PUBLIC_URL);

            // default namespace is too complicated for users!
            // atts.addAttribute("", "", "xmlns", "CDATA",
            // Diversicons.SCHEMA_1_NAMESPACE );

        }

        return elementName;
    }

    /**
     * 
     * @param normalizedElementName
     *            the normalized elementName,
     *            i.e. DivSynsetRelation should be SynsetRelation
     * 
     * @since 0.1.0
     */
    private static List<UBYLMFFieldMetadata> extractFieldsInDtdOrder(
            UBYLMFClassMetadata classMetadata,
            String normalizedElementName) {

        if (DTD_GRAMMAR == null) {
            String dtdText = Diversicons.readData(Diversicons.DTD_1_0_CLASSPATH_URL)
                                        .streamToString();
            DTD_GRAMMAR = parseDtd(dtdText);
        }

        if (ORDER_TABLE.containsKey(classMetadata.getClazz())) {
            return ORDER_TABLE.get(classMetadata.getClazz());
        } else {
            int elementDeclIndex = 0;
            XMLElementDecl elDec = new XMLElementDecl();

            List<UBYLMFFieldMetadata> ret = new ArrayList<>(10);
            UBYLMFFieldMetadata[] reorderedSubelems = new UBYLMFFieldMetadata[] {};
            String[] reorderedSubelemsNames = new String[] {};

            int nSubElemsFound = 0;
            List<UBYLMFFieldMetadata> attrs = new ArrayList<>();

            String[] dtdSubElementNames = new String[] {};

            while (DTD_GRAMMAR.getElementDecl(elementDeclIndex++, elDec)) {

                if (elDec.name.rawname.equals(normalizedElementName)) {

                    String dtdContentSpec = DTD_GRAMMAR.getContentSpecAsString(elementDeclIndex - 1);
                    if (dtdContentSpec == null) {
                        dtdSubElementNames = new String[] {};
                    } else {
                        dtdSubElementNames = dtdContentSpec.replace("(", "")
                                                           .replace(")", "")
                                                           .replace("?", "")
                                                           .replace("+", "")
                                                           .replace("*", "")
                                                           .split(",");
                    }

                    reorderedSubelems = new UBYLMFFieldMetadata[dtdSubElementNames.length];
                    reorderedSubelemsNames = new String[dtdSubElementNames.length];

                    for (UBYLMFFieldMetadata fm : classMetadata.getFields()) {
                        Class clazz;
                        if (Collection.class.isAssignableFrom(fm.getType())) {
                            clazz = fm.getGenericElementType();
                        } else {
                            clazz = fm.getType();
                        }

                        checkNotNull(clazz, "Found null clazz for UBYLMFFieldMetadata " + fm.getName());

                        boolean found = false;

                        for (int i = 0; i < dtdSubElementNames.length; i++) {
                            if (clazz.getSimpleName()
                                     .equals(dtdSubElementNames[i])) {
                                nSubElemsFound += 1;
                                found = true;
                                reorderedSubelems[i] = fm;
                                reorderedSubelemsNames[i] = dtdSubElementNames[i];
                                break;
                            }
                        }

                        if (!found) {
                            attrs.add(fm);
                        }
                    }

                    break;
                }

            }

            if (nSubElemsFound != dtdSubElementNames.length) {
                throw new DivException("Error while reordering elements according to DTD!"
                        + " \nDTD declares " + dtdSubElementNames.length + " fields, found " + nSubElemsFound
                        + " \nin classMetadata = " +
                        classMetadata.getClazz() + ", normalizedElementName = " +
                        normalizedElementName
                        + "\nDTD fields   :  " + Arrays.toString(dtdSubElementNames)
                        + "\nFound fields :  " + Arrays.toString(reorderedSubelemsNames));
            }
            ret.addAll(attrs);
            ret.addAll(Arrays.asList(reorderedSubelems));

            ORDER_TABLE.put(classMetadata.getClazz(), ret);

            return ret;
        }
    }

    /**
     * Performs a coherence check on provided lexical resource package. 
     * If the check succeeds, return the package.
     * 
     * To match it against a LexicalResource, see
     * {@link #checkLexResPackage(LexResPackage, LexicalResource)}
     * 
     * <p>
     * <strong>
     * NOTE: This check partially duplicates the checking done in {@link DivXmlValidator} and so 
     * it should be done only when that validation is not performed.
     * </strong> 
     * </p>
     * 
     * @throws IllegalArgumentException
     * 
     * @since 0.1.0
     */
    public static LexResPackage checkLexResPackage(LexResPackage lexResPackage) {        
        return checkLexResPackage(lexResPackage, null);
    }

    /**
     * Check provided {@code LexicalResourcePackage} matches fields in
     * {@code LexicalResource}. If check passes, return the validated package.
     * 
     * <p>
     * <strong>
     * NOTE: This check partially duplicates the checking done in {@link DivXmlValidator} and so 
     * it should be done only when that validation is not performed.
     * </strong> 
     * </p>
     * 
     * @throws IllegalArgumentException
     * 
     * @since 0.1.0
     */
    public static LexResPackage checkLexResPackage(
            LexResPackage pack,
            @Nullable LexicalResource lexRes) {
        
        Diversicons.checkLexResName(pack.getName(), "Invalid lexical resource 'name' field!");
        if (lexRes != null){
            Internals.checkEquals("Lexical resource name " + lexRes.getName() 
            + " doesn't match name in pack: " + pack.getName(), pack.getName(), lexRes.getName());
        }
        Diversicons.checkPrefix(pack.getPrefix());
        
        checkNotBlank(pack.getLabel(), "Invalid lexical resource label!");

        Diversicons.checkNamespaces(pack.getNamespaces());

        if (!pack.getNamespaces()
                 .containsKey(pack.getPrefix())) {
            throw new IllegalArgumentException(
                    "Couldn't find LexicalResource prefix '" + pack.getPrefix() + "' among namespace prefixes! ");
                           //  TODO put good docs link
                           //  + "See " + build.docsAtVersion() + "/diversicon-lmf.html"
                           //  + " for info on how to structure a Diversicon XML!");
        }
              
        return pack;
    }

    /**
     * Copied from Junit 'format'
     * 
     * @since 0.1.0
     */
    static String formatCheck(String message, Object expected, Object actual) {
        String formatted = "";
        if (message != null && !message.equals("")) {
            formatted = message + " ";
        }
        String expectedString = String.valueOf(expected);
        String actualString = String.valueOf(actual);
        if (expectedString.equals(actualString)) {
            return formatted + "expected: "
                    + formatClassAndValue(expected, expectedString)
                    + " but was: " + formatClassAndValue(actual, actualString);
        } else {
            return formatted + "expected:<" + expectedString + "> but was:<"
                    + actualString + ">";
        }
    }

    /**
     * Copied from Junit
     * 
     * @since 0.1.0
     */
    private static String formatClassAndValue(Object value, String valueString) {
        String className = value == null ? "null" : value.getClass()
                                                         .getName();
        return className + "<" + valueString + ">";
    }

    /**
     * Adapted from Junit
     * 
     * See {@link #checkEquals(String, Object, Object)}
     * 
     * @since 0.1.0
     */
    static public void checkEquals(
            @Nullable Object expected,
            @Nullable Object actual) {
        checkEquals("", expected, actual);
    }

    /**
     * Adapted from Junit
     * 
     * Asserts that two objects are equal. If they are not, an
     * {@link IllegalArgumentError} is thrown with the given message. If
     * <code>expected</code> and <code>actual</code> are <code>null</code>,
     * they are considered equal.
     *
     * @param message
     *            the identifying message for the {@link AssertionError} (
     *            <code>null</code>
     *            okay)
     * @param expected
     *            expected value
     * @param actual
     *            actual value
     * 
     * @throws IllegalArgumentException
     * 
     * @since 0.1.0
     */
    static public void checkEquals(
            @Nullable String message,
            @Nullable Object expected,
            @Nullable Object actual) {
        if (equalsRegardingNull(expected, actual)) {
            return;
        } else if (expected instanceof String && actual instanceof String) {
            String cleanMessage = message == null ? "" : message;
            throw new IllegalArgumentException(cleanMessage + " Expected:-->" + (String) expected
                    + "<-- Found:-->" + (String) actual + "<--");
        } else {
            String formsg = formatCheck(message, expected, actual);
            if (formsg == null) {
                throw new AssertionError();
            }
            throw new IllegalArgumentException(formsg);
        }
    }

    /**
     * Copied from Junit
     * 
     * @since 0.1.0
     */
    private static boolean equalsRegardingNull(Object expected, Object actual) {
        if (expected == null) {
            return actual == null;
        }

        return expected.equals(actual);
    }

    /**
     * Parses a Diversicon DTD and creates a corresponding XML Schema,
     * adding further constraints
     * 
     * @throws DivNotFoundException
     * @throws DivIoException
     * 
     * @since 0.1.0
     */
    // NOTE: you can't add namespace declarations with Xquery update!
    // See
    // http://stackoverflow.com/questions/36865118/add-namespace-declaration-to-xml-element-using-xquery
    public static void generateXmlSchemaFromDtd(File inputDtd, File output) {

        LOG.info("Going to generate xsd:   " + output.getAbsolutePath() + "   ...");

        checkNotNull(output);
        checkNotNull(inputDtd);

        checkArgument(inputDtd.exists(), "Can't find input DTD " + inputDtd.getAbsolutePath());

        File tempDir = createTempDivDir("trang").toFile();

        /**
         * First pass to generate first crude xsd
         */
        File firstPass = new File(tempDir, "first-pass.xsd");

        // xmlns:fn="http://www.w3.org/2005/xpath-functions"
        // xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning"
        // vc:minVersion="1.1"

        new com.thaiopensource.relaxng.translate.Driver().run(new String[] {
                "-I", "dtd", "-O", "xsd",
                // default namespace is too complicated for users
                // "-i", "xmlns=" + Diversicons.SCHEMA_1_NAMESPACE,
                // since they are not used in first pass, trang kindly removes
                // them with a warning :-/
                // "-i", "xmlns:fn=http://www.w3.org/2005/xpath-functions",
                // "-i", "xmlns:vc=http://www.w3.org/2007/XMLSchema-versioning",
                inputDtd.getAbsolutePath(),
                firstPass.getAbsolutePath() });

        if (!firstPass.exists()) {
            throw new DivException("Failed first transformation pass!!"
                    + "             Check the console for errors emitted by trang module!");
        }

        /**
         * Second pass is needed because Trang is soo smart to remove unused
         * namespaces :-/
         */
        File secondPass = new File(tempDir, "second-pass.xsd");

        SAXReader reader = new SAXReader();
        try {
            org.dom4j.Document document = reader.read(firstPass);
            document.getRootElement()
                    // default namespace is too complicated for users!
                    // .addAttribute("targetNamespace",
                    // Diversicons.SCHEMA_1_NAMESPACE)
                    .addAttribute("xmlns:fn", "http://www.w3.org/2005/xpath-functions")
                    .addAttribute("xmlns:vc", "http://www.w3.org/2007/XMLSchema-versioning")
                    .addAttribute("vc:minVersion", "1.0");
            org.dom4j.io.XMLWriter writer = new org.dom4j.io.XMLWriter(new FileOutputStream(secondPass),
                    org.dom4j.io.OutputFormat.createPrettyPrint());
            writer.write(document);
            LOG.debug("Wrote second pass to " + secondPass.getAbsolutePath());
        } catch (DocumentException | IOException ex) {
            throw new DivException("Error while adding namespaces in second pass!", ex);
        }

        // last pass!

        String xquery;
        try {
            xquery = IOUtils.toString(Diversicons.readData(Internals.FIX_DIV_SCHEMA_XQL)
                                                 .stream());
        } catch (IOException ex) {
            throw new DivIoException(ex);
        }

        Diversicons.transformXml(xquery, secondPass, output);

        LOG.info("Done.");

        /*
         * 
         * String query;
         * try {
         * query = IOUtils.toString(Internals.readData(Diversicons.
         * UBY_DTD_TO_SCHEMA_XQUERY_CLASSPATH_URL)
         * .stream());
         * } catch (IOException e) {
         * throw new DivIoException("Error while reading xquery !", e);
         * }
         * 
         * Diversicons.transform(query, file, output);
         */

    }

    /**
     * @deprecated we don't really use it, it's here just as an experiment
     * 
     * @since 0.1.0
     */
    // Taken from here: http://stackoverflow.com/a/26414914
    public static DTDGrammar parseDtd(String dtdText) {

        // LOG.debug("dtdText= " + dtdText);

        // read DTD
        // InputStream dtdStream = new ByteArrayInputStream(sw.toString()
        // .getBytes());
        // InputStream dtdStream =
        // So26391485.class.getResourceAsStream("your.dtd");
        /*
         * Scanner scanner = new Scanner(dtdStream);
         * String dtdText = scanner.useDelimiter("\\z")
         * .next();
         * scanner.close();
         */

        // DIRTY: use Xerces internals to parse the DTD
        Pattern dtdPattern = Pattern.compile("^\\s*<!DOCTYPE\\s+(.*?)>(.*)", Pattern.DOTALL);
        Matcher m = dtdPattern.matcher(dtdText);
        InputSource is;
        String docType;
        if (m.matches()) {
            docType = m.group(1);
            is = new InputSource(new StringReader(m.group(2)));
        } else {
            docType = null;
            is = new InputSource(new StringReader(dtdText));
        }
        XMLInputSource source = new SAXInputSource(is);
        XMLDTDLoader d = new XMLDTDLoader();
        try {
            return (DTDGrammar) d.loadGrammar(source);
        } catch (XNIException | IOException e) {
            throw new DivException(e);
        }

    }

    /**
     * Performs HTTP GET on server.
     *
     * @param divConfig
     *            if unknown use {@link DivConfig#of()}
     *
     * @throws DivIoException
     * 
     * @since 0.1.0
     */
    public static InputStream httpGet(DivConfig divConfig, URI url) {

        try {

            if (!checkedIsp) {
                LOG.debug("First get, checking if behind a redirecting ISP...");
                detectRedirectingISP(divConfig);
                checkedIsp = true;
            }

            LOG.debug("Fetching " + url);

            Request request = Request.Get(url);

            configureRequest(divConfig, request);
            DivRedirectStrategy stra = new DivRedirectStrategy();
            CloseableHttpClient client = HttpClientBuilder.create()
                                                          .setRedirectStrategy(stra)
                                                          .build();
            Executor executor = Executor.newInstance(client);

            Response response = executor.execute(request);

            List<URI> uris1 = stra.getRedirectUris();
            if (uris1.size() > 0) {
                URI lastUri = uris1.get(uris1.size() - 1);
                if (isIspRedirectedUrl(lastUri.toString())) {
                    throw new DivIoException("Couldn't find the request url " + url
                            + " (seems like your ISP redirected to this landing page: " + lastUri + "   )");
                }
            }

            HttpResponse httpResponse = response.returnResponse();

            StatusLine statusLine = httpResponse.getStatusLine();
            if (statusLine.getStatusCode() >= 400) {
                throw new DivIoException("ERROR: " + statusLine.getStatusCode() + ": " + statusLine.getReasonPhrase());
            }

            return httpResponse
                               .getEntity()
                               .getContent();
        } catch (Exception e) {
            throw new DivIoException(e);
        }

    }



    /**
     * Detects if behind a redirecting ISP and updates {@link #redirectingISPs}
     * accordingly. Never throws.
     * 
     * <p>
     * Some bastards ISP redirect on ad pages instead of giving 404. ,
     * see <a href="https://github.com/diversicon-kb/diversicon-core/issues/29"
     * target="_blank">
     * issue 29 on Github</a>
     * </p>
     * 
     * @since 0.1.0
     */
    private static void detectRedirectingISP(DivConfig config) {

        URI lastUri_1 = null;
        URI lastUri_2 = null;
        
        try {

            String url1 = NON_EXISTING_URL_1;
            LOG.debug("Fetching non-existent url " + url1);

            Request request1 = configureRequest(config, Request.Get(url1));
            DivRedirectStrategy stra1 = new DivRedirectStrategy();
            CloseableHttpClient client1 = HttpClientBuilder.create()
                                                           .setRedirectStrategy(stra1)
                                                           .build();
            Executor executor1 = Executor.newInstance(client1);

            Response resp1 = executor1.execute(request1);
            LOG.trace(resp1.returnContent()
                           .asString());
            List<URI> uris1 = stra1.getRedirectUris();
            lastUri_1 = uris1.get(uris1.size() - 1);

        } catch (IOException e) {

            LOG.trace("Couldn't fetch non existing page " + NON_EXISTING_URL_1
                    + "\nWith well-behaved ISPs"
                    + " this is the expected behaviour !", e);

        
        } catch (Exception ex) {
            LOG.debug("Caught some unexpected exception while probing if behind a redirecting ISP with url " + NON_EXISTING_URL_1, ex);
        }
        
        try {
            String url2 = NON_EXISTING_URL_2;
            LOG.debug("Fetching non-existent url " + url2);
            DivRedirectStrategy stra2 = new DivRedirectStrategy();
            CloseableHttpClient client2 = HttpClientBuilder.create()
                                                           .setRedirectStrategy(stra2)
                                                           .build();
            Executor executor2 = Executor.newInstance(client2);

            Request request2 = configureRequest(config, Request.Get(url2));
            Response resp2 = executor2.execute(request2);
            LOG.trace(resp2.returnContent()
                           .asString());
            List<URI> uris2 = stra2.getRedirectUris();
            
            lastUri_2 = uris2.get(uris2.size() - 1);
            
        } catch (IOException e) {

            LOG.trace("Couldn't fetch non existing page " + NON_EXISTING_URL_2
                    + "\nWith well-behaved ISPs"
                    + " this is the expected behaviour !", e);

        } catch (Exception ex) {
            LOG.debug("Caught some unexpected exception while probing if behind a redirecting ISP with url " + NON_EXISTING_URL_2, ex);
        }
    
    
        String commonUrl = "";
        
        if (lastUri_1 != null && lastUri_2 != null){
            commonUrl = longestCommonPrefix(lastUri_1.toString(), lastUri_2.toString());
        } else {
            if (lastUri_1 != null){
                commonUrl = lastUri_1.toString(); 
            } else if (lastUri_2 != null){                    
                commonUrl = lastUri_2.toString();                     
            }
        }
        if (lastUri_1 != null || lastUri_2 != null ){
            LOG.debug("***  Seems like your nasty ISP is hacking HTTP to redirect missed pages to " + commonUrl);
            LOG.debug("***  This may cause troubles when downloading some files !");
            redirectingISPs.add(commonUrl);
        }                      
        
    }

    /**
     * Returns true if {@code url} could be the result of an inappropriate
     * reidrection from the ISP.
     * 
     * See https://github.com/diversicon-kb/diversicon-core/issues/29
     * 
     * @since 0.1.0
     */
    private static boolean isIspRedirectedUrl(String url) {
        for (String u : redirectingISPs) {
            if (url.startsWith(u)) {
                return true;
            }
        }
        return false;
    }

   

    /**
     * Configures the request. Should work both for GETs and POSTs.
     * 
     * @since 0.1.0
     */
    private static Request configureRequest(DivConfig config, Request request) {
        checkNotNull(config, "Invalid null config, if unknown use DivConfig.of()");
        checkNotNull(request);

        if (config.getHttpProxy() != null) {

            request.viaProxy(config.getHttpProxy());
        }
        request.socketTimeout(config.getTimeout())
               .connectTimeout(config.getTimeout());

        return request;
    }

    /**
     * 
     * Returns the id of the items in the provided collection.
     * Null collection items are reported with warnings.
     * 
     * @since 0.1.0
     */
    public static <T extends IHasID> List<String> getIds(Collection<T> col){
        checkNotNull(col);        
        
        List<String> ret = new ArrayList<>();
        
       for (IHasID hasId : col){
           if (hasId == null){
               LOG.warn("Found collection with null item, skipping it !");
           } else {
               ret.add(hasId.getId());
           }
       }
       
       return ret;
        
    }
    
    /**
     * Returns the longest common prefix among strings {@code a} and {@code b}.
     * If any of them is empty, returns the empty string.
     * 
     * @since 0.1.0
     */
    public static String longestCommonPrefix(String a, String b) {
        checkNotNull(a);
        checkNotNull(b);
        if (a.isEmpty() || b.isEmpty()) {
            return "";
        }
        int minLength = Math.min(a.length(), b.length());
        for (int i = 0; i < minLength; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a.substring(0, minLength);
    }

    /**
     * @since 0.1.0
     */
    public static ImportConfig createImportConfig(LexicalResource lexRes){
        checkNotNull(lexRes);
        
        ImportConfig importConfig = new ImportConfig()
                .setAuthor(Diversicons.DEFAULT_AUTHOR)        
                .addLexResFileUrl(Diversicons.MEMORY_PROTOCOL + ":" + lexRes.hashCode());
        return importConfig;

    }

    /**
     * Return a list of the exception messages.
     * 
     * @since 0.1.0
     */
    public static List<String> getExceptionMessagesList(Throwable throwable) {
        List<String> result = new ArrayList<String>();
        while (throwable != null) {
            result.add(throwable.getMessage());
            throwable = throwable.getCause();
        }
        return result; //["THIRD EXCEPTION", "SECOND EXCEPTION", "FIRST EXCEPTION"]
    }
    
    /**
     * Return the exception messages concatenated by '\n'
     * 
     * @since 0.1.0
     */
    public static String getExceptionMessages(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : getExceptionMessagesList(throwable))
        {
           if (first)
              first = false;
           else
              sb.append("\n");
           sb.append(item);
        }
        return sb.toString();
 
    }
    
}

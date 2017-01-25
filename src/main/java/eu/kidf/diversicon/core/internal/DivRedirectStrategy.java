package eu.kidf.diversicon.core.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Little hack for tracking redirects.
 * 
 * <p>
 * Taken from http://stackoverflow.com/q/32612620
 * </p>
 * 
 * @see <a href="https://github.com/diversicon-kb/diversicon-core/issues/29"
 *      target="_blank">
 *      issue 29 on Github</a>
 * 
 * @since 0.1.0
 *
 */
class DivRedirectStrategy extends DefaultRedirectStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(DivRedirectStrategy.class);

    @Nullable
    private HttpContext context;

    /**
     * @since 0.1.0
     */
    public List<URI> getRedirectUris() {
        if (context == null) {
            LOG.debug("Couldn't get the redirect locations, returning an empty array !");
            return new ArrayList<>();
        } else {
            try {
                return ((RedirectLocations) context.getAttribute(HttpClientContext.REDIRECT_LOCATIONS)).getAll();
            } catch (Exception ex) {
                LOG.debug("Couldn't get the redirect locations, returning an empty array !");
                return new ArrayList<>();
            }
        }
    }

    /**
     * @since 0.1.0
     */    
    @Override
    public URI getLocationURI(final HttpRequest request, final HttpResponse response, final HttpContext context)
            throws ProtocolException {
        this.context = context; // to keep the HttpContext!
        return super.getLocationURI(request, response, context);
    }
}

package org.jenkinsci.plugins.pipeline.github.client;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.GitHubResponse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;

/**
 * @author Aaron Whiteside
 */
public class ExtendedGitHubClient extends GitHubClient {

    public ExtendedGitHubClient() {
    }

    public ExtendedGitHubClient(final String hostname) {
        super(hostname);
    }

    public ExtendedGitHubClient(final String hostname, final int port, final String scheme) {
        super(hostname, port, scheme);
    }

    public <V> V patch(final String uri, final Object params, final Type type) {
        return patch(uri, params, type, null);
    }

    public <V> V patch(final String uri, final Object params, final Type type, final String accept) {
        try {
            final HttpURLConnection request = createPatch(uri);
            if (accept != null) {
                request.setRequestProperty("Accept", accept);
            }
            return this.sendJson(request, params, type);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected HttpURLConnection createPatch(final String uri) throws IOException {
        return this.createConnection(uri, "PATCH");
    }

    public <V> V post(final String uri, final Object params, final Type type, final String accept) throws IOException {
        HttpURLConnection request = this.createPost(uri);
        if (accept != null) {
            request.setRequestProperty("Accept", accept);
        }
        return this.sendJson(request, params, type);
    }

    public <V> V put(final String uri, final Object params, final Type type, final String accept) throws IOException {
        HttpURLConnection request = this.createPut(uri);
        if (accept != null) {
            request.setRequestProperty("Accept", accept);
        }
        return this.sendJson(request, params, type);
    }

    // duplicated here because it's private in the super class.
    private <V> V sendJson(final HttpURLConnection request, final Object params, final Type type) throws IOException {
        this.sendParams(request, params);
        int code = request.getResponseCode();
        this.updateRateLimits(request);
        if(this.isOk(code)) {
            return type != null?this.parseJson(this.getStream(request), type):null;
        } else if(this.isEmpty(code)) {
            return null;
        } else {
            throw this.createException(this.getStream(request), code, request.getResponseMessage());
        }
    }

    // UncheckedIOException version of get(GitHubRequest)
    public GitHubResponse getUnchecked(final GitHubRequest request) {
        try {
            return get(request);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public <V> V delete(final String uri, final Type type, final String accept) throws IOException {
        HttpURLConnection request = this.createDelete(uri);
        if (accept != null) {
            request.setRequestProperty("Accept", accept);
        }

        int code = request.getResponseCode();
        this.updateRateLimits(request);
        if (this.isOk(code)) {
            return type != null ? this.parseJson(this.getStream(request), type) : null;
        } else if (this.isEmpty(code)) {
            return null;
        } else {
            throw this.createException(this.getStream(request), code, request.getResponseMessage());
        }
    }

}

package com.turbomanage.httpclient;

/**
 * Minimal HTTP client that facilitates simple GET, POST, PUT, and DELETE
 * requests. To implement buffering, streaming, or set other request properties,
 * set an alternate {@link RequestHandler}.
 * 
 * <p>
 * Sample usage:
 * </p>
 * 
 * <pre>
 * BasicHttpClient httpClient = new BasicHttpClient(&quot;http://www.google.com&quot;);
 * ParameterMap params = httpClient.newParams().add(&quot;q&quot;, &quot;GOOG&quot;);
 * HttpResponse httpResponse = httpClient.get(&quot;/finance&quot;, params);
 * System.out.println(httpResponse.getBodyAsString());
 * </pre>
 * 
 * @author David M. Chandler
 */
public class BasicHttpClient extends AbstractHttpClient {

    /**
     * Constructs the default client with empty baseUrl.
     */
    public BasicHttpClient() {
        this("");
    }

    /**
     * Constructs the default client with baseUrl.
     * 
     * @param baseUrl
     */
    public BasicHttpClient(String baseUrl) {
        this(baseUrl, new BasicRequestHandler() {
        });
    }

    /**
     * Constructs a client with baseUrl and custom {@link RequestHandler}.
     * 
     * @param baseUrl
     * @param requestHandler
     */
    public BasicHttpClient(String baseUrl, RequestHandler requestHandler) {
        super(baseUrl, requestHandler);
    }

}

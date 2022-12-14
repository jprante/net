package org.xbib.net.oauth;

import org.xbib.net.http.HttpParameters;

/**
 * <p>
 * Supplies an interface that can be used to retrieve request and access elements
 * from an OAuth 1.0(a) service provider. A provider object requires an
 * {@link OAuthConsumer} to sign the token request message; after a token has
 * been retrieved, the consumer is automatically updated with the token and the
 * corresponding secret.
 * </p>
 * <p>
 * To initiate the token exchange, create a new provider instance and configure
 * it with the URLs the service provider exposes for requesting elements and
 * resource authorization, e.g.:
 * </p>
 * 
 * <pre>
 * OAuthProvider provider = new DefaultOAuthProvider(&quot;http://twitter.com/oauth/request_token&quot;,
 *         &quot;http://twitter.com/oauth/access_token&quot;, &quot;http://twitter.com/oauth/authorize&quot;);
 * </pre>
 * <p>
 * Depending on the HTTP library you use, you may need a different provider
 * type, refer to the website documentation for how to do that.
 * </p>
 * <p>
 * To receive a request token which the user must authorize, you invoke it using
 * a consumer instance and a callback URL:
 * </p>
 * <p>
 * 
 * <pre>
 * String url = provider.retrieveRequestToken(consumer, &quot;http://www.example.com/callback&quot;);
 * </pre>
 * 
 * </p>
 * <p>
 * That url must be opened in a Web browser, where the user can grant access to
 * the resources in question. If that succeeds, the service provider will
 * redirect to the callback URL and append the blessed request token.
 * </p>
 * <p>
 * That token must now be exchanged for an access token, as such:
 * </p>
 * <p>
 * 
 * <pre>
 * provider.retrieveAccessToken(consumer, nullOrVerifierCode);
 * </pre>
 * 
 * </p>
 * <p>
 * where nullOrVerifierCode is either null if your provided a callback URL in
 * the previous step, or the pin code issued by the service provider to the user
 * if the request was out-of-band (cf. {@link OAuth#OUT_OF_BAND}.
 * </p>
 * <p>
 * The consumer used during token handshakes is now ready for signing.
 * </p>
 * 
 * @see OAuthProviderListener
 */
public interface OAuthProvider {

    /**
     * Queries the service provider for a request token.
     * <p>
     * <b>Pre-conditions:</b> the given {@link OAuthConsumer} must have a valid
     * consumer key and consumer secret already set.
     * </p>
     * <p>
     * <b>Post-conditions:</b> the given {@link OAuthConsumer} will have an
     * unauthorized request token and token secret set.
     * </p>
     * 
     * @param consumer
     *        the {@link OAuthConsumer} that should be used to sign the request
     * @param callbackUrl
     *        Pass an actual URL if your app can receive callbacks and you want
     *        to get informed about the result of the authorization process.
     *        Pass OUT_OF_BAND if the service provider implements
     *        OAuth 1.0a and your app cannot receive callbacks. Pass null if the
     *        service provider implements OAuth 1.0 and your app cannot receive
     *        callbacks. Please note that some services (among them Twitter)
     *        will fail authorization if you pass a callback URL but register
     *        your application as a desktop app (which would only be able to
     *        handle OOB requests).
     * @param customOAuthParams
     *        you can pass custom OAuth parameters here which will go directly
     *        into the signer, i.e. you don't have to put them into the request
     *        first. This is useful for pre-setting OAuth params for signing.
     *        Pass them sequentially in key/value order.
     * @return The URL to which the user must be sent in order to authorize the
     *         consumer. It includes the unauthorized request token (and in the
     *         case of OAuth 1.0, the callback URL -- 1.0a clients send along
     *         with the token request).
     * @throws OAuthMessageSignerException
     *         if signing the request failed
     * @throws OAuthNotAuthorizedException
     *         if the service provider rejected the consumer
     * @throws OAuthExpectationFailedException
     *         if required parameters were not correctly set by the consumer or
     *         service provider
     * @throws OAuthCommunicationException
     *         if server communication failed
     */
    String retrieveRequestToken(OAuthConsumer consumer, String callbackUrl,
                                       String... customOAuthParams) throws OAuthMessageSignerException,
            OAuthNotAuthorizedException, OAuthExpectationFailedException,
            OAuthCommunicationException;

    /**
     * Queries the service provider for an access token.
     * <p>
     * <b>Pre-conditions:</b> the given {@link OAuthConsumer} must have a valid
     * consumer key, consumer secret, authorized request token and token secret
     * already set.
     * </p>
     * <p>
     * <b>Post-conditions:</b> the given {@link OAuthConsumer} will have an
     * access token and token secret set.
     * </p>
     * 
     * @param consumer
     *        the {@link OAuthConsumer} that should be used to sign the request
     * @param oauthVerifier
     *        <b>NOTE: Only applies to service providers implementing OAuth
     *        1.0a. Set to null if the service provider is still using OAuth
     *        1.0.</b> The verification code issued by the service provider
     *        after the the user has granted the consumer authorization. If the
     *        callback method provided in the previous step was
     *        OUT_OF_BAND, then you must ask the user for this
     *        value. If your app has received a callback, the verfication code
     *        was passed as part of that request instead.
     * @param customOAuthParams
     *        you can pass custom OAuth parameters here which will go directly
     *        into the signer, i.e. you don't have to put them into the request
     *        first. This is useful for pre-setting OAuth params for signing.
     *        Pass them sequentially in key/value order.
     * @throws OAuthMessageSignerException
     *         if signing the request failed
     * @throws OAuthNotAuthorizedException
     *         if the service provider rejected the consumer
     * @throws OAuthExpectationFailedException
     *         if required parameters were not correctly set by the consumer or
     *         service provider
     * @throws OAuthCommunicationException
     *         if server communication failed
     */
    void retrieveAccessToken(OAuthConsumer consumer, String oauthVerifier,
                                    String... customOAuthParams) throws OAuthMessageSignerException,
            OAuthNotAuthorizedException, OAuthExpectationFailedException,
            OAuthCommunicationException;

    /**
     * Any additional non-OAuth parameters returned in the response body of a
     * token request can be obtained through this method. These parameters will
     * be preserved until the next token request is issued. The return value is
     * never null.
     */
    HttpParameters getResponseParameters();

    /**
     * Subclasses must use this setter to preserve any non-OAuth query
     * parameters contained in the server response. It's the caller's
     * responsibility that any OAuth parameters be removed beforehand.
     * 
     * @param parameters
     *        the map of query parameters served by the service provider in the
     *        token response
     */
    void setResponseParameters(HttpParameters parameters);

    /**
     * @param isOAuth10aProvider
     *        set to true if the service provider supports OAuth 1.0a. Note that
     *        you need only call this method if you reconstruct a provider
     *        object in between calls to retrieveRequestToken() and
     *        retrieveAccessToken() (i.e. if the object state isn't preserved).
     *        If instead those two methods are called on the same provider
     *        instance, this flag will be deducted automatically based on the
     *        server response during retrieveRequestToken(), so you can simply
     *        ignore this method.
     */
    void setOAuth10a(boolean isOAuth10aProvider);

    /**
     * @return true if the service provider supports OAuth 1.0a. Note that the
     *         value returned here is only meaningful after you have already
     *         performed the token handshake, otherwise there is no way to
     *         determine what version of the OAuth protocol the service provider
     *         implements.
     */
    boolean isOAuth10a();

    String getRequestTokenEndpointUrl();

    String getAccessTokenEndpointUrl();

    String getAuthorizationWebsiteUrl();

    void setListener(OAuthProviderListener listener);

    void removeListener(OAuthProviderListener listener);
}

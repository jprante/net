package org.xbib.net.oauth;

import org.xbib.net.PercentDecoder;
import org.xbib.net.PercentEncoder;
import org.xbib.net.PercentEncoders;
import org.xbib.net.http.HttpParameters;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OAuth {

    public static final String VERSION_1_0 = "1.0";

    public static final String ENCODING = "UTF-8";

    public static final String FORM_ENCODED = "application/x-www-form-urlencoded";

    public static final String HTTP_AUTHORIZATION_HEADER = "Authorization";

    public static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";

    public static final String OAUTH_TOKEN = "oauth_token";

    public static final String OAUTH_TOKEN_SECRET = "oauth_token_secret";

    public static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";

    public static final String OAUTH_SIGNATURE = "oauth_signature";

    public static final String OAUTH_TIMESTAMP = "oauth_timestamp";

    public static final String OAUTH_NONCE = "oauth_nonce";

    public static final String OAUTH_VERSION = "oauth_version";

    public static final String OAUTH_CALLBACK = "oauth_callback";

    public static final String OAUTH_CALLBACK_CONFIRMED = "oauth_callback_confirmed";

    public static final String OAUTH_VERIFIER = "oauth_verifier";

    /**
     * Pass this value as the callback "url" upon retrieving a request token if
     * your application cannot receive callbacks (e.g. because it's a desktop
     * app). This will tell the service provider that verification happens
     * out-of-band, which basically means that it will generate a PIN code (the
     * OAuth verifier) and display that to your user. You must obtain this code
     * from your user and pass it to
     * {@link OAuthProvider#retrieveAccessToken(OAuthConsumer, String, String...)} in order
     * to complete the token handshake.
     */
    public static final String OUT_OF_BAND = "oob";

    public static final PercentEncoder percentEncoder = PercentEncoders.getQueryEncoder(StandardCharsets.UTF_8);

    public static final PercentDecoder percentDecoder = new PercentDecoder();

    /**
     * Construct a x-www-form-urlencoded document containing the given sequence
     * of name/value pairs. Use OAuth percent encoding (not exactly the encoding
     * mandated by x-www-form-urlencoded).
     */
    public static <T extends Map.Entry<String, String>> void formEncode(Collection<T> parameters,
            OutputStream into) throws IOException {
        if (parameters != null) {
            boolean first = true;
            for (Map.Entry<String, String> entry : parameters) {
                if (first) {
                    first = false;
                } else {
                    into.write('&');
                }
                into.write(percentEncoder.encode(safeToString(entry.getKey())).getBytes());
                into.write('=');
                into.write(percentEncoder.encode(safeToString(entry.getValue())).getBytes());
            }
        }
    }

    /**
     * Construct a x-www-form-urlencoded document containing the given sequence
     * of name/value pairs. Use OAuth percent encoding (not exactly the encoding
     * mandated by x-www-form-urlencoded).
     */
    public static <T extends Map.Entry<String, String>> String formEncode(Collection<T> parameters)
            throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        formEncode(parameters, b);
        return new String(b.toByteArray());
    }

    /**
     * Parse a form-urlencoded document.
     */
    public static HttpParameters decodeForm(String form)
            throws MalformedInputException, UnmappableCharacterException {
        HttpParameters params = new HttpParameters();
        if (isEmpty(form)) {
            return params;
        }
        for (String nvp : form.split("\\&")) {
            int equals = nvp.indexOf('=');
            String name;
            String value;
            if (equals < 0) {
                name = percentDecoder.decode(nvp);
                value = null;
            } else {
                name = percentDecoder.decode(nvp.substring(0, equals));
                value = percentDecoder.decode(nvp.substring(equals + 1));
            }

            params.put(name, value);
        }
        return params;
    }

    public static HttpParameters decodeForm(InputStream content)
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                content));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            sb.append(line);
            line = reader.readLine();
        }

        return decodeForm(sb.toString());
    }

    /**
     * Construct a Map containing a copy of the given parameters. If several
     * parameters have the same name, the Map will contain the first value,
     * only.
     */
    public static <T extends Map.Entry<String, String>> Map<String, String> toMap(Collection<T> from) {
        HashMap<String, String> map = new HashMap<String, String>();
        if (from != null) {
            for (Map.Entry<String, String> entry : from) {
                String key = entry.getKey();
                if (!map.containsKey(key)) {
                    map.put(key, entry.getValue());
                }
            }
        }
        return map;
    }

    public static final String safeToString(Object from) {
        return (from == null) ? null : from.toString();
    }

    public static boolean isEmpty(String str) {
        return (str == null) || (str.length() == 0);
    }

    /**
     * Appends a list of key/value pairs to the given URL, e.g.:
     * 
     * <pre>
     * String url = OAuth.addQueryParameters(&quot;http://example.com?a=1&quot;, b, 2, c, 3);
     * </pre>
     * 
     * which yields:
     * 
     * <pre>
     * http://example.com?a=1&b=2&c=3
     * </pre>
     * 
     * All parameters will be encoded according to OAuth's percent encoding
     * rules.
     * 
     * @param url
     *        the URL
     * @param kvPairs
     *        the list of key/value pairs
     * @return string
     */
    public static String addQueryParameters(String url, String... kvPairs)
            throws MalformedInputException, UnmappableCharacterException {
        String queryDelim = url.contains("?") ? "&" : "?";
        StringBuilder sb = new StringBuilder(url + queryDelim);
        for (int i = 0; i < kvPairs.length; i += 2) {
            if (i > 0) {
                sb.append("&");
            }
            sb.append(percentEncoder.encode(kvPairs[i])).append("=")
                    .append(percentEncoder.encode(kvPairs[i + 1]));
        }
        return sb.toString();
    }

    public static String addQueryParameters(String url, Map<String, String> params)
            throws MalformedInputException, UnmappableCharacterException {
        String[] kvPairs = new String[params.size() * 2];
        int idx = 0;
        for (String key : params.keySet()) {
            kvPairs[idx] = key;
            kvPairs[idx + 1] = params.get(key);
            idx += 2;
        }
        return addQueryParameters(url, kvPairs);
    }

    public static String addQueryString(String url, String queryString) {
        String queryDelim = url.contains("?") ? "&" : "?";
        return url + queryDelim + queryString;
    }

    /**
     * Builds an OAuth header from the given list of header fields. All
     * parameters starting in 'oauth_*' will be percent encoded.
     * 
     * <pre>
     * String authHeader = OAuth.prepareOAuthHeader(&quot;realm&quot;, &quot;http://example.com&quot;, &quot;oauth_token&quot;, &quot;x%y&quot;);
     * </pre>
     * 
     * which yields:
     * 
     * <pre>
     * OAuth realm=&quot;http://example.com&quot;, oauth_token=&quot;x%25y&quot;
     * </pre>
     * 
     * @param kvPairs
     *        the list of key/value pairs
     * @return a string eligible to be used as an OAuth HTTP Authorization
     *         header.
     */
    public static String prepareOAuthHeader(String... kvPairs)
            throws MalformedInputException, UnmappableCharacterException {
        StringBuilder sb = new StringBuilder("OAuth ");
        for (int i = 0; i < kvPairs.length; i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            boolean isOAuthElem = kvPairs[i].startsWith("oauth_")
                    || kvPairs[i].startsWith("x_oauth_");
            String value = isOAuthElem ? percentEncoder.encode(kvPairs[i + 1]) : kvPairs[i + 1];
            sb.append(percentEncoder.encode(kvPairs[i])).append("=\"").append(value).append("\"");
        }
        return sb.toString();
    }

    public static HttpParameters oauthHeaderToParamsMap(String oauthHeader)
            throws MalformedInputException, UnmappableCharacterException {
        HttpParameters params = new HttpParameters();
        if (oauthHeader == null || !oauthHeader.startsWith("OAuth ")) {
            return params;
        }
        String[] elements = oauthHeader.substring("OAuth ".length()).split(",");
        for (String keyValuePair : elements) {
            String[] keyValue = keyValuePair.split("=");
            params.put(keyValue[0].trim(), keyValue[1].replace("\"", "").trim());
        }
        return params;
    }

    /**
     * Helper method to concatenate a parameter and its value to a pair that can
     * be used in an HTTP header. This method percent encodes both parts before
     * joining them.
     * 
     * @param name
     *        the OAuth parameter name, e.g. oauth_token
     * @param value
     *        the OAuth parameter value, e.g. 'hello oauth'
     * @return a name/value pair, e.g. oauth_token="hello%20oauth"
     */
    public static String toHeaderElement(String name, String value)
            throws MalformedInputException, UnmappableCharacterException {
        return percentEncoder.encode(name) + "=\"" + percentEncoder.encode(value) + "\"";
    }
}

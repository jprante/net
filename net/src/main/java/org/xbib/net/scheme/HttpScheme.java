package org.xbib.net.scheme;

import org.xbib.net.URL;
import org.xbib.net.PathNormalizer;

import java.util.Locale;

/**
 * HTTP scheme.
 */
class HttpScheme extends AbstractScheme {

    HttpScheme() {
        super("http", 80);
    }

    HttpScheme(String name, int port) {
        super(name, port);
    }

    @Override
    public URL normalize(URL url) {
        String host = url.getHost();
        if (host != null) {
            host = host.toLowerCase(Locale.ROOT);
        }
        return URL.builder()
                .scheme(url.getScheme())
                .userInfo(url.getUserInfo())
                .host(host, url.getProtocolVersion())
                .port(url.getPort())
                .path(PathNormalizer.normalize(url.getPath()))
                .query(url.getQuery())
                .fragment(url.getFragment())
                .build();
    }
}

package org.xbib.net.oauth;

import org.xbib.net.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class HttpURLConnectionResponseAdapter implements HttpResponse {

    private HttpURLConnection connection;

    public HttpURLConnectionResponseAdapter(HttpURLConnection connection) {
        this.connection = connection;
    }

    public InputStream getContent() {
        try {
            return connection.getInputStream();
        } catch (IOException e) {
            return connection.getErrorStream();
        }
    }

    public int getStatusCode() throws IOException {
        return connection.getResponseCode();
    }

    public String getReasonPhrase() throws Exception {
        return connection.getResponseMessage();
    }

    public Object unwrap() {
        return connection;
    }
}

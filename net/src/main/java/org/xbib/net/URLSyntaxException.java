package org.xbib.net;

/**
 * URL syntax exception.
 */
@SuppressWarnings("serial")
public class URLSyntaxException extends Exception {

    URLSyntaxException(String message) {
        super(message);
    }

    URLSyntaxException(Throwable cause) {
        super(cause);
    }

}

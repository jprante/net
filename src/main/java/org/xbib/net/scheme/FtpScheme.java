package org.xbib.net.scheme;

/**
 *
 */
class FtpScheme extends HttpScheme {

    FtpScheme() {
        super("ftp", 21);
    }

    FtpScheme(String name, int port) {
        super(name, port);
    }

}

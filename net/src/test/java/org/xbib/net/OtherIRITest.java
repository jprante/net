package org.xbib.net;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Disabled
public class OtherIRITest {

    @Test
    public void testSimple() throws Exception {
        IRI iri = IRI.create("http://validator.w3.org/check?uri=http%3A%2F%2Fr\u00E9sum\u00E9.example.org");
        assertEquals("http", iri.getScheme());
        assertEquals("validator.w3.org", iri.getHost());
        assertEquals("/check", iri.getPath());
        assertEquals("//validator.w3.org/check?uri=http%3A%2F%2Frésumé.example.org", iri.getSchemeSpecificPart());

        assertEquals("http://validator.w3.org/check?uri=http%3A%2F%2Fr\u00E9sum\u00E9.example.org", iri.toString());
        assertEquals("http://validator.w3.org/check?uri=http%3A%2F%2Fr%C3%A9sum%C3%A9.example.org", iri.toURI().toString());
    }

    @Test
    public void testIpv4() throws Exception {
        IRI iri = IRI.create("http://127.0.0.1");
        assertEquals("http://127.0.0.1", iri.toURI().toString());
    }

    @Test
    public void testIpv6() throws Exception {
        IRI iri = IRI.create("http://[2001:0db8:85a3:08d3:1319:8a2e:0370:7344]");
        assertEquals("http://[2001:0db8:85a3:08d3:1319:8a2e:0370:7344]", iri.toURI().toString());
    }

    @Test
    public void testUnderscore() throws Exception{
        IRI iri = IRI.create("http://its_gbsc.cn.ibm.com/");
        assertEquals("http://its_gbsc.cn.ibm.com/", iri.toURI().toString());
    }

    @Test
    public void testIpv6Invalid() throws URISyntaxException {
        IRI iri = IRI.create("http://[2001:0db8:85a3:08d3:1319:8a2e:0370:734o]");
        iri.toURI().toString();
    }

    @Test
    public void testFile() throws Exception {
        IRI iri = IRI.create("file:///tmp/test/foo");
        assertEquals("file:///tmp/test/foo", iri.toURI().toString());
    }

    @Test
    public void testSimple2() throws Exception {
        IRI iri = IRI.create("http://www.example.org/red%09ros\u00E9#red");
        assertEquals("http://www.example.org/red%09ros%C3%A9#red", iri.toURI().toString());
    }

    @Test
    public void testNotSoSimple() throws Exception {
        IRI iri = IRI.create("http://example.com/\uD800\uDF00\uD800\uDF01\uD800\uDF02");
        assertEquals("http://example.com/%F0%90%8C%80%F0%90%8C%81%F0%90%8C%82", iri.toURI().toString());
    }

    @Test
    public void testIRItoURI() throws Exception {
        IRI iri = IRI.create("http://\u7D0D\u8C46.example.org/%E2%80%AE");
        URI uri = iri.toURI();
        assertEquals("http://xn--99zt52a.example.org/%E2%80%AE", uri.toString());
    }

    @Test
    public void testComparison() throws Exception {
        IRI iri1 = IRI.create("http://www.example.org/");
        IRI iri2 = IRI.create("http://www.example.org/..");
        IRI iri3 = IRI.create("http://www.Example.org:80");

        assertFalse(iri1.equals(iri2)); // false
        assertFalse(iri1.equals(iri3)); // false
        assertFalse(iri2.equals(iri1)); // false
        assertFalse(iri2.equals(iri3)); // false
        assertFalse(iri3.equals(iri1)); // false
        assertFalse(iri3.equals(iri2)); // false

        /*assertTrue(iri1.normalize().equals(iri2.normalize()));
        assertTrue(iri1.normalize().equals(iri3.normalize()));
        assertTrue(iri2.normalize().equals(iri1.normalize()));
        assertTrue(iri2.normalize().equals(iri3.normalize()));
        assertTrue(iri3.normalize().equals(iri1.normalize()));
        assertTrue(iri3.normalize().equals(iri2.normalize()));*/

    }

    @Test
    public void testUCN() throws Exception {
        //IRI iri1 = IRI.create("http://www.example.org/r\u00E9sum\u00E9.html");
        //IRI iri2 = IRI.create("http://www.example.org/re\u0301sume\u0301.html", Normalizer.Form.NFC);
        //assertEquals(iri2, iri1);
    }

    @Test
    public void testPercent() throws Exception {
        IRI iri1 = IRI.create("http://example.org/%7e%2Fuser?%2f");
        IRI iri2 = IRI.create("http://example.org/%7E%2fuser?/");
        //assertTrue(iri1.normalize().equals(iri2.normalize()));
    }

    @Test
    public void testIDN() throws Exception {
        IRI iri1 = IRI.create("http://r\u00E9sum\u00E9.example.org");
        assertEquals("xn--rsum-bpad.example.org", iri1.getASCIIHost());
    }

    @Test
    public void testRelative() throws Exception {
        IRI base = IRI.create("http://example.org/foo/");

        assertEquals("http://example.org/", base.resolve("/").toString());
        assertEquals("http://example.org/test", base.resolve("/test").toString());
        assertEquals("http://example.org/foo/test", base.resolve("test").toString());
        assertEquals("http://example.org/test", base.resolve("../test").toString());
        assertEquals("http://example.org/foo/test", base.resolve("./test").toString());
        assertEquals("http://example.org/foo/", base.resolve("test/test/../../").toString());
        assertEquals("http://example.org/foo/?test", base.resolve("?test").toString());
        assertEquals("http://example.org/foo/#test", base.resolve("#test").toString());
        assertEquals("http://example.org/foo/", base.resolve(".").toString());
    }

    /**
     * Try a variety of URI schemes. If any problematic schemes pop up, we should add a test for 'em here
     */
    @Test
    public void testSchemes() throws Exception {

        IRI iri = IRI.create("http://a:b@c.org:80/d/e?f#g");
        assertEquals("http", iri.getScheme());
        assertEquals("a:b", iri.getUserInfo());
        assertEquals("c.org", iri.getHost());
        assertEquals(80, iri.getPort());
        assertEquals("/d/e", iri.getPath());
        assertEquals("f", iri.getQuery());
        assertEquals("g", iri.getFragment());

        iri = IRI.create("https://a:b@c.org:80/d/e?f#g");
        assertEquals("https", iri.getScheme());
        assertEquals("a:b", iri.getUserInfo());
        assertEquals("c.org", iri.getHost());
        assertEquals(80, iri.getPort());
        assertEquals("/d/e", iri.getPath());
        assertEquals("f", iri.getQuery());
        assertEquals("g", iri.getFragment());

        iri = IRI.create("ftp://a:b@c.org:80/d/e?f#g");
        assertEquals("ftp", iri.getScheme());
        assertEquals("a:b", iri.getUserInfo());
        assertEquals("c.org", iri.getHost());
        assertEquals(80, iri.getPort());
        assertEquals("/d/e", iri.getPath());
        assertEquals("f", iri.getQuery());
        assertEquals("g", iri.getFragment());

        iri = IRI.create("mailto:joe@example.org?subject=foo");
        assertEquals("mailto", iri.getScheme());
        assertEquals(null, iri.getUserInfo());
        assertEquals(null, iri.getHost());
        assertEquals(-1, iri.getPort());
        assertEquals("joe@example.org", iri.getPath());
        assertEquals("subject=foo", iri.getQuery());
        assertEquals(null, iri.getFragment());

        iri = IRI.create("tag:example.org,2006:foo");
        assertEquals("tag", iri.getScheme());
        assertEquals(null, iri.getUserInfo());
        assertEquals(null, iri.getHost());
        assertEquals(-1, iri.getPort());
        assertEquals("example.org,2006:foo", iri.getPath());
        assertEquals(null, iri.getQuery());
        assertEquals(null, iri.getFragment());

        iri = IRI.create("urn:lsid:ibm.com:example:82437234964354895798234d");
        assertEquals("urn", iri.getScheme());
        assertEquals(null, iri.getUserInfo());
        assertEquals(null, iri.getHost());
        assertEquals(-1, iri.getPort());
        assertEquals("lsid:ibm.com:example:82437234964354895798234d", iri.getPath());
        assertEquals(null, iri.getQuery());
        assertEquals(null, iri.getFragment());

        iri = IRI.create("data:image/gif;base64,R0lGODdhMAAwAPAAAAAAAP");
        assertEquals("data", iri.getScheme());
        assertEquals(null, iri.getUserInfo());
        assertEquals(null, iri.getHost());
        assertEquals(-1, iri.getPort());
        assertEquals("image/gif;base64,R0lGODdhMAAwAPAAAAAAAP", iri.getPath());
        assertEquals(null, iri.getQuery());
        assertEquals(null, iri.getFragment());

    }
}


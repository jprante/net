package org.xbib.net.scheme;

/**
 * The LDAPS scheme.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4516">LDAP RFC</a>
 */
class SecureLdapScheme extends LdapScheme {

    SecureLdapScheme() {
        super("ldaps", 636);
    }

}

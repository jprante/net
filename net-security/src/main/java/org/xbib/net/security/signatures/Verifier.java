package org.xbib.net.security.signatures;

import javax.crypto.Mac;
import java.io.IOException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A new instance of the Verifier class needs to be created for each signature.
 */
public class Verifier {

    private final Verify verify;

    private final Signature signature;

    private final Algorithm algorithm;

    private final Provider provider;

    /**
     * Constructs a verifier object with the specified key and signature object.
     *
     * @param key       The key used to verify the signature.
     * @param signature The signature object.
     */
    public Verifier(final Key key, final Signature signature) {
        this(key, signature, null);
    }

    public Verifier(final Key key, final Signature signature, final Provider provider) {
        requireNonNull(key, "Key cannot be null");
        this.signature = requireNonNull(signature, "Signature cannot be null");
        this.algorithm = signature.getAlgorithm();
        this.provider = provider;
        if (java.security.Signature.class.equals(algorithm.getType())) {
            this.verify = new Asymmetric((PublicKey) key);
        } else if (Mac.class.equals(algorithm.getType())) {
            this.verify = new Symmetric(key);
        } else {
            throw new UnsupportedAlgorithmException(String.format("Unknown Algorithm type %s %s", algorithm.getPortableName(), algorithm.getType().getName()));
        }
        try {
            verify.verify("validation".getBytes());
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException("Can't initialise the Signer using the provided algorithm and key", e);
        }
    }

    public boolean verify(final String method, final String uri, final Map<String, String> headers) throws IOException, NoSuchAlgorithmException, SignatureException {
        signature.verifySignatureValidityDates();
        final String signingString = createSigningString(method, uri, headers);
        return verify.verify(signingString.getBytes());
    }

    public String createSigningString(final String method, final String uri, final Map<String, String> headers) throws IOException {
        return Signatures.createSigningString(signature.getHeaders(), method, uri, headers,
                signature.getSignatureCreationTimeMilliseconds(),
                signature.getSignatureExpirationTimeMilliseconds());
    }

    private interface Verify {
        boolean verify(byte[] signingStringBytes);
    }

    private class Asymmetric implements Verify {

        private final PublicKey key;

        private Asymmetric(final PublicKey key) {
            this.key = key;
        }

        @Override
        public boolean verify(final byte[] signingStringBytes) {
            try {
                final java.security.Signature instance = provider == null ?
                        java.security.Signature.getInstance(algorithm.getJvmName()) :
                        java.security.Signature.getInstance(algorithm.getJvmName(), provider);
                if (signature.getParameterSpec() != null) {
                    instance.setParameter(signature.getParameterSpec());
                }
                instance.initVerify(key);
                instance.update(signingStringBytes);
                return instance.verify(Base64.decodeBase64(signature.getSignature().getBytes()));
            } catch (final NoSuchAlgorithmException e) {
                throw new UnsupportedAlgorithmException(algorithm.getJvmName());
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private class Symmetric implements Verify {

        private final Key key;

        private Symmetric(final Key key) {
            this.key = key;
        }

        @Override
        public boolean verify(final byte[] signingStringBytes) {
            try {
                final Mac mac = provider == null ? Mac.getInstance(algorithm.getJvmName()) : Mac.getInstance(algorithm.getJvmName(), provider);
                mac.init(key);
                final byte[] hash = mac.doFinal(signingStringBytes);
                final byte[] encoded = Base64.encodeBase64(hash);
                return MessageDigest.isEqual(encoded, signature.getSignature().getBytes());
            } catch (final NoSuchAlgorithmException e) {
                throw new UnsupportedAlgorithmException(algorithm.getJvmName());
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}

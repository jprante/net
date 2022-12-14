package org.xbib.net.security.cookie;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;

/**
 * A utility class for invoking encryption methods and returning password strings,
 * using {@link MessageDigest} and {@link Mac}.
 */
public class CryptUtil {

    private static final Random random = new SecureRandom();

    private CryptUtil() {
    }

    public static String randomHex(int length) {
        byte[] b = new byte[length];
        random.nextBytes(b);
        return encodeHex(b);
    }

    public static String sha256(String plainText) throws NoSuchAlgorithmException {
        return digest(Codec.BASE64, plainText.getBytes(StandardCharsets.UTF_8), null, Algo.SHA256.algo, Algo.SHA256.prefix);
    }

    public static String sha512(String plainText) throws NoSuchAlgorithmException {
        return digest(Codec.BASE64, plainText.getBytes(StandardCharsets.UTF_8), null, Algo.SHA512.algo, Algo.SHA512.prefix);
    }

    public static String ssha(String plainText, byte[] salt) throws NoSuchAlgorithmException {
        return digest(Codec.BASE64, plainText.getBytes(StandardCharsets.UTF_8), salt, Algo.SSHA.algo, Algo.SSHA.prefix);
    }

    public static String ssha256(String plainText, byte[] salt) throws NoSuchAlgorithmException {
        return digest(Codec.BASE64, plainText.getBytes(StandardCharsets.UTF_8), salt, Algo.SSHA256.algo, Algo.SSHA256.prefix);
    }

    public static String ssha512(String plainText, byte[] salt) throws NoSuchAlgorithmException {
        return digest(Codec.BASE64, plainText.getBytes(StandardCharsets.UTF_8), salt, Algo.SSHA512.algo, Algo.SSHA512.prefix);
    }

    public static String hmacSHA256(Charset charset, String plainText, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        return hmac(HMac.HMAC_SHA256, Codec.BASE64, plainText.getBytes(charset), secret.getBytes(charset));
    }

    public static String hmacSHA256(Charset charset, byte[] plainText, String secret) throws InvalidKeyException, NoSuchAlgorithmException {
        return hmac(HMac.HMAC_SHA256, Codec.BASE64, plainText, secret.getBytes(charset));
    }

    public static String hmacSHA256(byte[] plainText, byte[] secret) throws InvalidKeyException, NoSuchAlgorithmException {
        return hmac(HMac.HMAC_SHA256, Codec.BASE64, plainText, secret);
    }

    public static String hmac(Charset charset, HMac hmac, Codec codec, String plainText, String secret) throws InvalidKeyException, NoSuchAlgorithmException {
        return hmac(hmac, codec, plainText.getBytes(charset), secret.getBytes(charset));
    }

    public static String digest(Codec codec, byte[] plainText, byte[] salt, String algo, String prefix) throws NoSuchAlgorithmException {
        Objects.requireNonNull(plainText);
        MessageDigest digest = MessageDigest.getInstance(algo);
        digest.update(plainText);
        byte[] bytes = digest.digest();
        if (salt != null) {
            digest.update(salt);
            byte[] hash = digest.digest();
            bytes = new byte[salt.length + hash.length];
            System.arraycopy(hash, 0, bytes, 0, hash.length);
            System.arraycopy(salt, 0, bytes, hash.length, salt.length);
        }
        return '{' + prefix + '}' +
                (codec == Codec.BASE64 ? Base64.getEncoder().encodeToString(bytes) :
                        codec == Codec.HEX ? encodeHex(bytes) : null);
    }

    public static String hmac(HMac hmac, Codec codec, byte[] plainText, byte[] secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Objects.requireNonNull(plainText);
        Objects.requireNonNull(secret);
        Mac mac = Mac.getInstance(hmac.getAlgo());
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret, hmac.getAlgo());
        mac.init(secretKeySpec);
        return codec == Codec.BASE64 ? Base64.getEncoder().encodeToString(mac.doFinal(plainText)) :
                codec == Codec.HEX ? encodeHex(mac.doFinal(plainText)) : null;
    }

    public static String encodeHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append(Integer.toHexString((int) b & 0xFF));
        }
        return stringBuilder.toString();
    }

    /**
     * Decodes the hex-encoded bytes and returns their value a byte string.
     *
     * @param hex hexidecimal code
     * @return string
     */
    public static byte[] decodeHex(String hex) {
        Objects.requireNonNull(hex);
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("unexpected hex string " + hex);
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int d1 = decodeHexDigit(hex.charAt(i * 2)) << 4;
            int d2 = decodeHexDigit(hex.charAt(i * 2 + 1));
            result[i] = (byte) (d1 + d2);
        }
        return result;
    }

    private static int decodeHexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        throw new IllegalArgumentException("unexpected hex digit " + c);
    }
}

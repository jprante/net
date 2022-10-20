package org.xbib.net.security.ssl.util;

import org.xbib.net.security.ssl.exception.GenericKeyStoreException;

import javax.net.ssl.X509TrustManager;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class KeyStoreUtils {

    private static final String KEYSTORE_TYPE = "PKCS12";

    private KeyStoreUtils() {}

    public static KeyStore loadKeyStore(String keystorePath, char[] keystorePassword) {
        return loadKeyStore(keystorePath, keystorePassword, KeyStore.getDefaultType());
    }

    public static KeyStore loadKeyStore(String keystorePath, char[] keystorePassword, String keystoreType) {
        try (InputStream keystoreInputStream = KeyStoreUtils.class.getClassLoader().getResourceAsStream(keystorePath)) {
            return loadKeyStore(keystoreInputStream, keystorePassword, keystoreType);
        } catch (Exception e) {
            throw new GenericKeyStoreException(e);
        }
    }

    public static KeyStore loadKeyStore(Path keystorePath, char[] keystorePassword) {
        return loadKeyStore(keystorePath, keystorePassword, KeyStore.getDefaultType());
    }

    public static KeyStore loadKeyStore(Path keystorePath, char[] keystorePassword, String keystoreType) {
        try (InputStream keystoreInputStream = Files.newInputStream(keystorePath, StandardOpenOption.READ)) {
            return loadKeyStore(keystoreInputStream, keystorePassword, keystoreType);
        } catch (Exception e) {
            throw new GenericKeyStoreException(e);
        }
    }

    public static KeyStore loadKeyStore(InputStream keystoreInputStream, char[] keystorePassword) {
        return loadKeyStore(keystoreInputStream, keystorePassword, KeyStore.getDefaultType());
    }

    public static KeyStore loadKeyStore(InputStream keystoreInputStream, char[] keystorePassword, String keystoreType) {
        if (isNull(keystoreInputStream)) {
            throw new GenericKeyStoreException("KeyStore is not present for the giving input");
        }
        try {
            KeyStore keystore = KeyStore.getInstance(keystoreType);
            keystore.load(keystoreInputStream, keystorePassword);
            return keystore;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new GenericKeyStoreException(e);
        }
    }

    public static KeyStore createIdentityStore(Key privateKey,
                                               char[] privateKeyPassword,
                                               String alias,
                                               Collection<? extends Certificate> certificateChain) {
        return createIdentityStore(privateKey, privateKeyPassword, alias, certificateChain.toArray(new Certificate[]{}));
    }

    public static KeyStore createIdentityStore(Key privateKey,
                                               char[] privateKeyPassword,
                                               Collection<? extends Certificate> certificateChain) {
        return createIdentityStore(privateKey, privateKeyPassword, null, certificateChain.toArray(new Certificate[]{}));
    }

    public static <T extends Certificate> KeyStore createIdentityStore(Key privateKey,
                                                                       char[] privateKeyPassword,
                                                                       T[] certificateChain) {
        return createIdentityStore(privateKey, privateKeyPassword, null, certificateChain);
    }

    public static <T extends Certificate> KeyStore createIdentityStore(Key privateKey,
                                                                       char[] privateKeyPassword,
                                                                       String alias,
                                                                       T[] certificateChain) {
        try {
            KeyStore keyStore = createKeyStore();
            String privateKeyAlias = StringUtils.isBlank(alias) ? CertificateUtils.generateAlias(certificateChain[0]) : alias;
            keyStore.setKeyEntry(privateKeyAlias, privateKey, privateKeyPassword, certificateChain);
            return keyStore;
        } catch (KeyStoreException e) {
            throw new GenericKeyStoreException(e);
        }
    }

    public static KeyStore createKeyStore() {
        return createKeyStore("".toCharArray());
    }

    public static KeyStore createKeyStore(char[] keyStorePassword) {
        return createKeyStore(KEYSTORE_TYPE, keyStorePassword);
    }

    public static KeyStore createKeyStore(String keyStoreType, char[] keyStorePassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, keyStorePassword);
            return keyStore;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new GenericKeyStoreException(e);
        }
    }

    @SafeVarargs
    public static <T extends X509TrustManager> KeyStore createTrustStore(T... trustManagers) {
        List<X509Certificate> certificates = new ArrayList<>();
        for (T trustManager : trustManagers) {
            certificates.addAll(Arrays.asList(trustManager.getAcceptedIssuers()));
        }
        return createTrustStore(ValidationUtils.requireNotEmpty(certificates, "Could not create TrustStore because the provided TrustManager does not contain any trusted certificates"));
    }

    public static <T extends Certificate> KeyStore createTrustStore(List<T> certificates) {
        try {
            KeyStore trustStore = createKeyStore();
            for (T certificate : ValidationUtils.requireNotEmpty(certificates, "Could not create TrustStore because certificate is absent")) {
                String alias = CertificateUtils.generateAlias(certificate);

                if (trustStore.containsAlias(alias)) {
                    for (int number = 0; number < 1000; number++) {
                        String mayBeUniqueAlias = alias + "-" + number;
                        if (!trustStore.containsAlias(mayBeUniqueAlias)) {
                            alias = mayBeUniqueAlias;
                            break;
                        }
                    }
                }

                trustStore.setCertificateEntry(alias, certificate);
            }
            return trustStore;
        } catch (KeyStoreException e) {
            throw new GenericKeyStoreException(e);
        }
    }

    public static List<KeyStore> loadSystemKeyStores() {
        List<KeyStore> keyStores = new ArrayList<>();
        String operatingSystem = System.getProperty("os.name").toLowerCase();
        if (operatingSystem.contains("windows")) {
            KeyStore windowsRootKeyStore = createKeyStore("Windows-ROOT", null);
            KeyStore windowsMyKeyStore = createKeyStore("Windows-MY", null);
            keyStores.add(windowsRootKeyStore);
            keyStores.add(windowsMyKeyStore);
        }
        if (operatingSystem.contains("mac")) {
            KeyStore macKeyStore = createKeyStore("KeychainStore", null);
            keyStores.add(macKeyStore);
        }
        return Collections.unmodifiableList(keyStores);
    }

    public static int countAmountOfTrustMaterial(KeyStore keyStore) {
        return amountOfSpecifiedMaterial(keyStore, KeyStore::isCertificateEntry, Integer.MAX_VALUE);
    }

    public static int countAmountOfIdentityMaterial(KeyStore keyStore) {
        return amountOfSpecifiedMaterial(keyStore, KeyStore::isKeyEntry, Integer.MAX_VALUE);
    }

    public static boolean containsTrustMaterial(KeyStore keyStore) {
        return amountOfSpecifiedMaterial(keyStore, KeyStore::isCertificateEntry, 1) > 0;
    }

    public static boolean containsIdentityMaterial(KeyStore keyStore) {
        return amountOfSpecifiedMaterial(keyStore, KeyStore::isKeyEntry, 1) > 0;
    }

    private static int amountOfSpecifiedMaterial(KeyStore keyStore,
                                          KeyStoreBiPredicate<KeyStore, String> predicate,
                                          int upperBoundaryForMaterialCounter) {

        try {
            int materialCounter = 0;
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements() && materialCounter < upperBoundaryForMaterialCounter) {
                String alias = aliases.nextElement();
                if (predicate.test(keyStore, alias)) {
                    materialCounter++;
                }
            }
            return materialCounter;
        } catch (KeyStoreException e) {
            throw new GenericKeyStoreException(e);
        }
    }

    private interface KeyStoreBiPredicate<T extends KeyStore, U> {
        boolean test(T t, U u) throws KeyStoreException;
    }
}

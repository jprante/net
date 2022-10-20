package org.xbib.net.security.ssl.keymanager;

import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 */
interface CombinableX509ExtendedKeyManager extends X509KeyManager {

    List<X509ExtendedKeyManager> getKeyManagers();

    default <T> T extractInnerField(Function<X509ExtendedKeyManager, T> keyManagerMapper, Predicate<T> predicate) {
        return getKeyManagers().stream()
                .map(keyManagerMapper)
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    default String[] getAliases(Function<X509ExtendedKeyManager, String[]> aliasExtractor) {
        List<String> aliases = getKeyManagers().stream()
                .map(aliasExtractor)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());

        return aliases.isEmpty() ? null : aliases.toArray(new String[]{});
    }

}

package com.wsss.market.maker.domain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CacheMap<K, V> extends ConcurrentHashMap<K, V> {
    private Function<? super K, ? extends V> mappingFunction;

    public CacheMap(Function<? super K, ? extends V> mappingFunction) {
        this.mappingFunction = mappingFunction;
    }

    @Override
    public V get(Object key) {
        V v = super.get(key);
        if (v == null) {
            v = this.computeIfAbsent((K) key, mappingFunction);
        }
        return v;
    }
}

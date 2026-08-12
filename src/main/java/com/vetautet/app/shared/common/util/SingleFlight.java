package com.vetautet.app.shared.common.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SingleFlight <K, V>{

    private final ConcurrentHashMap<K, CompletableFuture<V>> map = new ConcurrentHashMap<>();

    public V doCall(K key, Supplier<V> function) {
        CompletableFuture<V> future = map.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(function));

        try {
            return future.join();
        } finally {
            map.remove(key);
        }
    }
}

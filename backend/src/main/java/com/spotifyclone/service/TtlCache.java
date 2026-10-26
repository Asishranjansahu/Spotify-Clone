package com.spotifyclone.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Small bounded in-memory cache with per-entry expiry. */
class TtlCache<K, V> {
    private record Slot<V>(V value, long expiresAtNanos) {}

    private final long ttlNanos;
    private final Map<K, Slot<V>> map;

    TtlCache(Duration ttl, int maxEntries) {
        this.ttlNanos = ttl.toNanos();
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Slot<V>> eldest) {
                return size() > maxEntries;
            }
        };
    }

    synchronized Optional<V> get(K key) {
        Slot<V> e = map.get(key);
        if (e == null) {
            return Optional.empty();
        }
        if (System.nanoTime() - e.expiresAtNanos() >= 0) {
            map.remove(key);
            return Optional.empty();
        }
        return Optional.of(e.value());
    }

    synchronized void put(K key, V value) {
        map.put(key, new Slot<>(value, System.nanoTime() + ttlNanos));
    }
}

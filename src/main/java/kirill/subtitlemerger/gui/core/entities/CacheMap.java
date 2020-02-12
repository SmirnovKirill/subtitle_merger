package kirill.subtitlemerger.gui.core.entities;

import lombok.AllArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@AllArgsConstructor
public class CacheMap<K,V> extends LinkedHashMap<K, V> {
    private int maxSize;

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > maxSize;
    }
}

package kitnapper.view;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decoded-photo memo, keyed by {@code file@edge} so the 160 px card and the
 * 640 px preview keep their own copies. Access-ordered LRU with a small cap:
 * scrolling the catalog must not re-decode, and the cache must never grow
 * with the dataset. {@link #clear} runs after a demo reset.
 */
final class PhotoCache {

    private static final int MAX_ENTRIES = 32;

    private static final Map<String, BufferedImage> ENTRIES = new Store();

    private PhotoCache() {
    }

    /** The cached image for this file at this edge, or null. */
    static synchronized BufferedImage get(String photoFile, int maxEdge) {
        return ENTRIES.get(key(photoFile, maxEdge));
    }

    static synchronized void put(String photoFile, int maxEdge, BufferedImage image) {
        if (image != null) {
            ENTRIES.put(key(photoFile, maxEdge), image);
        }
    }

    /** Drops every entry; the photo files may have been replaced or wiped. */
    static synchronized void clear() {
        ENTRIES.clear();
    }

    static synchronized int size() {
        return ENTRIES.size();
    }

    private static String key(String photoFile, int maxEdge) {
        return photoFile + "@" + maxEdge;
    }

    /** Access-ordered LRU so a re-decode only happens for cold entries. */
    private static final class Store extends LinkedHashMap<String, BufferedImage> {

        private static final long serialVersionUID = 1L;

        Store() {
            super(16, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > MAX_ENTRIES;
        }
    }
}

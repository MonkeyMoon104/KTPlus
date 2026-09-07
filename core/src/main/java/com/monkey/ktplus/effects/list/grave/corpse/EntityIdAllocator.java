package com.monkey.ktplus.effects.list.grave.corpse;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

final class EntityIdAllocator {
    private static final Set<Integer> ACTIVE = ConcurrentHashMap.newKeySet();

    private EntityIdAllocator() {}

    static int allocate() {
        int id;
        do {
            id = ThreadLocalRandom.current().nextInt(1_000_000, Integer.MAX_VALUE);
        } while (!ACTIVE.add(id));
        return id;
    }

    static void release(int entityId) {
        ACTIVE.remove(entityId);
    }
}

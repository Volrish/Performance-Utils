package com.volrish.meteorhud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MerchantManager {

    private static final MerchantManager INSTANCE = new MerchantManager();
    public  static MerchantManager getInstance() { return INSTANCE; }

    private final List<MerchantWaypoint>  merchants = new ArrayList<>();
    private final AtomicInteger           counter   = new AtomicInteger(0);

    private MerchantManager() {}

    public synchronized void add(MerchantWaypoint wp) {
        merchants.add(wp);
    }

    private static final long KILLED_LINGER_MS = 30_000L;  // 30 seconds

    /** Mark the closest matching type merchant as killed. */
    public synchronized void markKilled(MerchantWaypoint.MerchantType type, int x, int z) {
        MerchantWaypoint closest = null;
        double           best    = Double.MAX_VALUE;
        for (MerchantWaypoint wp : merchants) {
            if (wp.type == type && wp.alive) {
                double dx = wp.x - x, dz = wp.z - z;
                double d  = dx*dx + dz*dz;
                if (d < best) { best = d; closest = wp; }
            }
        }
        if (closest != null) {
            closest.alive      = false;
            closest.killedAtMs = System.currentTimeMillis();
        }
    }

    /** Remove merchants that have been killed for longer than the linger window. */
    public synchronized void pruneExpired() {
        long now = System.currentTimeMillis();
        merchants.removeIf(wp -> !wp.alive && wp.killedAtMs > 0
                && (now - wp.killedAtMs) > KILLED_LINGER_MS);
    }

    /** True if any merchant is still visible (alive OR in linger window). */
    public synchronized boolean hasVisible() {
        long now = System.currentTimeMillis();
        for (MerchantWaypoint wp : merchants) {
            if (wp.alive) return true;
            if (wp.killedAtMs > 0 && (now - wp.killedAtMs) <= KILLED_LINGER_MS) return true;
        }
        return false;
    }

    public synchronized List<MerchantWaypoint> getAll() {
        return new ArrayList<>(merchants);
    }

    public synchronized void clear() { merchants.clear(); }

    public String nextId() { return "merchant_" + counter.getAndIncrement(); }
}

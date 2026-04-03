package com.volrish.meteorhud.client.auto;

/**
 * AUTO MANAGER — single access point for all auto mods.
 *
 * Toggle via keybind (raw GLFW, not in Options > Controls):
 *   AutoAbsorber : K (toggle) / J (manual absorb)
 *   AutoCombine  : O
 *   AutoSell     : L
 */
public class AutoManager {
    private static final AutoManager INSTANCE = new AutoManager();
    public static AutoManager getInstance() { return INSTANCE; }

    public final AutoAbsorber absorber = new AutoAbsorber();
    public final AutoCombine  combine  = new AutoCombine();
    public final AutoSell     sell     = new AutoSell();

    private AutoManager() {}
}

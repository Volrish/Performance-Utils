package com.volrish.meteorhud.client.screen;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;

/**
 * Data for one feature card in ModSettingsScreen.
 */
public class FeatureCard {

    public final ItemStack       icon;
    public final String          title;
    public final String          description;
    public       boolean         enabled;
    public final Consumer<Boolean> onToggle;
    public final Runnable        onDetails;

    // Layout (set by ModSettingsScreen.layoutCards)
    public int          x, y;
    public ButtonWidget toggleBtn;

    public FeatureCard(ItemStack icon, String title, String description,
                       boolean enabled, Consumer<Boolean> onToggle, Runnable onDetails) {
        this.icon        = icon;
        this.title       = title;
        this.description = description;
        this.enabled     = enabled;
        this.onToggle    = onToggle;
        this.onDetails   = onDetails;
    }
}

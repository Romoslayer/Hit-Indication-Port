package com.rosymaple.hitindication.fabric.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Gives Mod Menu a "Configure" button when Cloth Config is also installed. Both are optional: this
 * class is only loaded by Mod Menu itself, and the Cloth Config screen class is only touched once
 * Cloth Config is known to be present.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // A factory that makes no screen is Mod Menu 11's way of saying "no config screen".
        if (!FabricLoader.getInstance().isModLoaded("cloth-config"))
            return parent -> null;

        return ClothConfigScreen::create;
    }
}

package elytraplus;

import com.mojang.blaze3d.platform.InputConstants;
import elytraplus.feature.FireworkHotkey;
import elytraplus.feature.Freelook;
import elytraplus.feature.FreelookPlusMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class ElytraPlusFabric implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"));

    @Override
    public void onInitializeClient() {
        ElytraPlusCommon.init();

        // Unbound by default; set it in Controls.
        KeyMapping fireworkHotkey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.elytraplus.firework_hotkey", InputConstants.UNKNOWN.getValue(), CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            while (fireworkHotkey.consumeClick()) {
                FireworkHotkey.use(minecraft);
            }
        });

        // Unbound by default. With the Freelook Plus mod installed, its key and Freelook are used instead (see FreelookPlusMod).
        if (!FreelookPlusMod.isLoaded()) {
            KeyMapping freelook = KeyMappingHelper.registerKeyMapping(
                    new KeyMapping(Freelook.KEY_NAME, InputConstants.UNKNOWN.getValue(), CATEGORY));
            Freelook.setKey(freelook);
            ClientTickEvents.END_CLIENT_TICK.register(Freelook::update);
        }
    }
}

package elytraplus.config;

import elytraplus.Constants;
import elytraplus.feature.FreelookPlusMod;
import elytraplus.platform.Services;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/** Settings saved to config/elytraplus.json5. Add a field + option per feature. */
public final class ElytraPlusConfig {
    public static final ConfigClassHandler<ElytraPlusConfig> HANDLER = ConfigClassHandler.createBuilder(ElytraPlusConfig.class)
            .id(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(Services.PLATFORM.getConfigDir().resolve(Constants.MOD_ID + ".json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry
    public boolean autoSwitchElytra = true;

    @SerialEntry
    public boolean autoSwitchThirdPerson = true;

    @SerialEntry
    public boolean fireworkHotkey = true;

    @SerialEntry
    public boolean freelook = true;

    @SerialEntry
    public boolean elytraSlot = true;

    @SerialEntry
    public boolean fireworkSlot = true;

    public static ElytraPlusConfig get() {
        return HANDLER.instance();
    }

    public static Screen createScreen(Screen parent) {
        return YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> builder
                .title(Component.translatable("elytraplus.config.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("elytraplus.config.category.features"))
                        .option(toggle("auto_switch_elytra", defaults.autoSwitchElytra,
                                () -> config.autoSwitchElytra, value -> config.autoSwitchElytra = value))
                        .option(toggle("auto_switch_third_person", defaults.autoSwitchThirdPerson,
                                () -> config.autoSwitchThirdPerson, value -> config.autoSwitchThirdPerson = value))
                        .option(toggle("firework_hotkey", defaults.fireworkHotkey,
                                () -> config.fireworkHotkey, value -> config.fireworkHotkey = value))
                        .option(freelookOption(defaults, config))
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("elytraplus.config.category.hud"))
                        .option(toggle("elytra_slot", defaults.elytraSlot,
                                () -> config.elytraSlot, value -> config.elytraSlot = value))
                        .option(toggle("firework_slot", defaults.fireworkSlot,
                                () -> config.fireworkSlot, value -> config.fireworkSlot = value))
                        .build()))
                .generateScreen(parent);
    }

    /**
     * Without the Freelook Plus mod, this is the built-in Freelook's setting. With it installed, the value is its
     * elytra setting; if its overall setting is off, this one has no effect, so the name and description say so.
     */
    private static Option<Boolean> freelookOption(ElytraPlusConfig defaults, ElytraPlusConfig config) {
        if (!FreelookPlusMod.isLoaded()) {
            return toggle("freelook", defaults.freelook, () -> config.freelook, value -> config.freelook = value);
        }
        MutableComponent name = Component.translatable("elytraplus.config.freelook");
        MutableComponent description = Component.translatable("elytraplus.config.freelook.freelookplus.desc");
        if (!FreelookPlusMod.isEnabled()) {
            name.append(Component.translatable("elytraplus.config.freelook.freelookplus_off").withStyle(ChatFormatting.RED));
            description = Component.empty()
                    .append(Component.translatable("elytraplus.config.freelook.freelookplus_off.desc").withStyle(ChatFormatting.RED))
                    .append("\n\n")
                    .append(description);
        }
        return Option.<Boolean>createBuilder()
                .name(name)
                .description(OptionDescription.of(description))
                .binding(defaults.freelook, FreelookPlusMod::isElytraEnabled, FreelookPlusMod::setElytraEnabled)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Boolean> toggle(String key, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("elytraplus.config." + key))
                .description(OptionDescription.of(Component.translatable("elytraplus.config." + key + ".desc")))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }
}

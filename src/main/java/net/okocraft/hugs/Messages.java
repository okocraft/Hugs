package net.okocraft.hugs;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

final class Messages {

    static final Component PREFIX =
            text()
                    .append(text().content("[").color(DARK_GRAY))
                    .append(translatable().key("hugs.prefix").color(LIGHT_PURPLE).build())
                    .append(text().content("]").color(DARK_GRAY))
                    .append(space())
                    .build();

    static final Component NO_PERMISSION =
            PREFIX.append(translatable().key("hugs.no-permission").color(RED).build());

    static final Component ONLY_PLAYER =
            PREFIX.append(translatable().key("hugs.only-player").color(RED).build());

    static final Component COMMAND_USAGE =
            PREFIX.append(translatable().key("hugs.command-usage").color(GRAY).build());

    static final Component PLAYER_NOT_FOUND =
            PREFIX.append(translatable().key("hugs.player-not-found").color(RED).build());

    static final Component HUG_SELF =
            PREFIX.append(translatable().key("hugs.hug-self").color(GRAY).build());

    static final Function<String, Component> HUG_ENTITY =
            name -> PREFIX.append(
                    translatable()
                            .key("hugs.hug-entity")
                            .args(text().content(name).color(YELLOW).build())
                            .color(GRAY)
                            .build()
            );

    static final Function<String, Component> HUG_PLAYER =
            name -> PREFIX.append(
                    translatable()
                            .key("hugs.hug-player")
                            .args(text().content(name).color(AQUA).build())
                            .color(GRAY)
                            .build()
            );

    static final Function<String, Component> HUG_HUGGED =
            name -> PREFIX.append(
                    translatable()
                            .key("hugs.hug-hugged")
                            .args(text().content(name).color(AQUA).build())
                            .color(GRAY)
                            .build()
            );


    private static final TranslationRegistry REGISTRY = TranslationRegistry.create(Key.key("hugs", "language"));

    static void register(@NotNull Hugs plugin) throws IOException {
        REGISTRY.defaultLocale(Locale.ENGLISH);

        load(plugin, Locale.ENGLISH);
        load(plugin, Locale.JAPAN);

        GlobalTranslator.get().addSource(REGISTRY);
    }

    static void unregister() {
        GlobalTranslator.get().removeSource(REGISTRY);
    }

    private static void load(@NotNull Hugs plugin, @NotNull Locale locale) throws IOException {
        var input = plugin.getResource(locale + ".properties");

        if (input != null) {
            var bundle = new PropertyResourceBundle(input);
            REGISTRY.registerAll(locale, bundle, true);
        }
    }
}

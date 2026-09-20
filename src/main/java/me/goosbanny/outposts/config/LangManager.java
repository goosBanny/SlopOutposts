package me.goosbanny.outposts.config;

import me.goosbanny.outposts.api.arena.OutpostArena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

/**
 * Manages plugin localization, configurable messages, and PlaceholderAPI token formatting.
 */
public class LangManager {

    private final Plugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, Component> staticComponentCache = new java.util.concurrent.ConcurrentHashMap<>();
    private YamlConfiguration langConfig;
    private String prefix = "<#F07DB5><bold>OUTPOSTS</bold></#F07DB5> <gray>▶</gray> ";

    public LangManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String locale = plugin.getConfig().getString("system.locale", "en_US");
        String fileName = locale != null && !locale.equalsIgnoreCase("en_US") && new File(plugin.getDataFolder(), "lang_" + locale + ".yml").exists()
                ? "lang_" + locale + ".yml"
                : "lang.yml";
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try (InputStream in = plugin.getResource(fileName)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else if (!fileName.equals("lang.yml")) {
                    try (InputStream defaultIn = plugin.getResource("lang.yml")) {
                        if (defaultIn != null) {
                            Files.copy(defaultIn, file.toPath());
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not extract default " + fileName + ": " + e.getMessage());
            }
        }

        this.langConfig = YamlConfiguration.loadConfiguration(file);
        this.prefix = langConfig.getString("prefix", "<#F07DB5><bold>OUTPOSTS</bold></#F07DB5> <gray>▶</gray> ");
        this.staticComponentCache.clear();
    }

    public String getPrefix() {
        return prefix;
    }

    @NotNull
    public String getRaw(@NotNull String path, @NotNull String def) {
        if (langConfig == null) return def;
        return langConfig.getString(path, def);
    }

    @NotNull
    public String getRaw(@NotNull String path, @Nullable OutpostArena arena, @NotNull String def) {
        if (arena != null) {
            String custom = arena.getCustomLang(path);
            if (custom != null && !custom.isBlank()) {
                return custom;
            }
        }
        return getRaw(path, def);
    }

    public boolean isSuppressed(@NotNull String path) {
        return isSuppressed(path, null);
    }

    public boolean isSuppressed(@NotNull String path, @Nullable OutpostArena arena) {
        String raw = getRaw(path, arena, "");
        return raw.isBlank() || raw.equalsIgnoreCase("none");
    }

    @NotNull
    public Component get(@NotNull String path) {
        return staticComponentCache.computeIfAbsent(path, p -> {
            String template = getRaw(p, p);
            return miniMessage.deserialize(formatString(template, Collections.emptyMap()));
        });
    }

    @NotNull
    public Component get(@NotNull String path, @NotNull Map<String, String> replacements) {
        if (replacements.isEmpty()) {
            return get(path);
        }
        return get(path, (OutpostArena) null, replacements);
    }

    @NotNull
    public Component get(@NotNull String path, @Nullable OutpostArena arena, @NotNull Map<String, String> replacements) {
        if (replacements.isEmpty() && arena == null) {
            return get(path);
        }
        String template = getRaw(path, arena, path);
        String formatted = formatString(template, replacements);
        return miniMessage.deserialize(formatted);
    }

    @NotNull
    public String getFormattedString(@NotNull String path, @NotNull String def, @NotNull Map<String, String> replacements) {
        return getFormattedString(path, null, def, replacements);
    }

    @NotNull
    public String getFormattedString(@NotNull String path, @Nullable OutpostArena arena, @NotNull String def, @NotNull Map<String, String> replacements) {
        String template = getRaw(path, arena, def);
        return formatString(template, replacements);
    }

    @NotNull
    public Component formatComponent(@NotNull String raw, @NotNull Map<String, String> replacements) {
        return miniMessage.deserialize(formatString(raw, replacements));
    }

    public String formatString(@NotNull String raw, @NotNull Map<String, String> replacements) {
        String result = raw.replace("<prefix>", prefix);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue())
                    .replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }

    @NotNull
    public String getPlaceholder(@NotNull String key, @NotNull String def) {
        return getRaw("placeholders." + key, def);
    }

    @NotNull
    public String getPlaceholderState(@NotNull String stateKey, @NotNull String def) {
        return getRaw("placeholders.states." + stateKey.toLowerCase(), def);
    }
}

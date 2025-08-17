package com.conaxgames.poll;

import com.conaxgames.libraries.config.CommentedConfiguration;
import org.bukkit.Bukkit;

import java.io.File;

public class Settings {

    public String sqliteFile;
    public boolean useFillerglass;
    public boolean autoUpdate;
    public int updateInterval;

    public Settings() {
        reload();
    }

    public void reload() {
        File file = new File(PollPlugin.getInstance().getDataFolder(), "settings.yml");
        if (!file.exists()) {
            PollPlugin.getInstance().saveResource("settings.yml", false);
        }

        CommentedConfiguration settings = CommentedConfiguration.loadConfiguration(file);

        try {
            settings.syncWithConfig(file, PollPlugin.getInstance().getResource("settings.yml"));
        } catch (Exception exception) {
            Bukkit.getLogger().info("Unable to load settings.yml");
        }

        // SQLite database file
        sqliteFile = settings.getString("sqlite_file", "polls.db");

        // Poll settings
        // No restrictions

        // GUI settings
        useFillerglass = settings.getBoolean("gui.use_fillerglass", true);
        autoUpdate = settings.getBoolean("gui.auto_update", true);
        updateInterval = settings.getInt("gui.update_interval", 20);
    }
}

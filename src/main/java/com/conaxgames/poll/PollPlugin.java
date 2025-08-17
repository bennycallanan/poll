package com.conaxgames.poll;

import com.conaxgames.libraries.LibraryPlugin;
import com.conaxgames.poll.commands.CommandRegistry;
import com.conaxgames.poll.data.PollDataManager;
import com.conaxgames.poll.listeners.PollListener;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class PollPlugin extends JavaPlugin {

    @Getter
    private static PollPlugin instance;
    
    private LibraryPlugin libraryPlugin;
    private Settings settings;
    private PollDataManager dataManager;
    private PollListener listener;

    @Override
    public void onEnable() {
        instance = this;
        this.libraryPlugin = new LibraryPlugin()
                .onEnable(this, "PollPlugin", "Poll", "poll", "poll.admin");
        
        this.settings = new Settings();
        this.dataManager = new PollDataManager(this);
        this.dataManager.initialize();
        this.listener = new PollListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        new CommandRegistry(this);
        
        getLogger().info("PollPlugin has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.shutdown();
        }
        
        getLogger().info("PollPlugin has been disabled!");
    }
}

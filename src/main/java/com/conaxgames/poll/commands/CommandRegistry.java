package com.conaxgames.poll.commands;

import com.conaxgames.libraries.LibraryPlugin;
import com.conaxgames.poll.PollPlugin;
import com.conaxgames.poll.commands.impl.CreatePollCommand;
import com.conaxgames.poll.commands.impl.PollCommands;
import com.conaxgames.poll.data.Poll;

import java.util.ArrayList;
import java.util.List;

public class CommandRegistry {
    
    private final PollPlugin plugin;
    private final LibraryPlugin libraryPlugin;
    
    public CommandRegistry(PollPlugin plugin) {
        this.plugin = plugin;
        this.libraryPlugin = plugin.getLibraryPlugin();
        registerCommands();
    }

    private void registerCommands() {
        libraryPlugin.getPaperCommandManager().registerCommand(new PollCommands(plugin));
        libraryPlugin.getPaperCommandManager().registerCommand(new CreatePollCommand(plugin));

        registerCompletions();
    }

    private void registerCompletions() {
        libraryPlugin.getPaperCommandManager().getCommandCompletions().registerAsyncCompletion("pollids", c -> {
            List<String> pollIds = new ArrayList<>();
            for (Poll poll : plugin.getDataManager().getActivePolls()) {
                pollIds.add(poll.getId());
            }
            return pollIds;
        });
        
        libraryPlugin.getPaperCommandManager().getCommandCompletions().registerAsyncCompletion("duration", c -> {
            List<String> durations = new ArrayList<>();
            durations.add("30s");
            durations.add("1m");
            durations.add("5m");
            durations.add("15m");
            durations.add("30m");
            durations.add("1h");
            durations.add("2h");
            durations.add("6h");
            durations.add("12h");
            durations.add("1d");
            durations.add("2d");
            durations.add("1w");
            return durations;
        });
    }
}

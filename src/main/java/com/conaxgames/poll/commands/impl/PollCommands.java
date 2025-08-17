package com.conaxgames.poll.commands.impl;

import com.conaxgames.libraries.acf.BaseCommand;
import com.conaxgames.libraries.acf.annotation.*;
import com.conaxgames.libraries.util.CC;
import com.conaxgames.poll.PollPlugin;
import com.conaxgames.poll.data.Poll;
import com.conaxgames.poll.menus.PollListMenu;
import com.conaxgames.poll.util.TimeUtil;
import org.bukkit.entity.Player;

import java.util.Collection;

@CommandAlias("poll|polls")
public class PollCommands extends BaseCommand {

    private final PollPlugin plugin;

    public PollCommands(PollPlugin plugin) {
        this.plugin = plugin;
        plugin.getLibraryPlugin().getPaperCommandManager().registerCommand(this);
    }

    @Default
    @CommandPermission("poll.use")
    @Description("Open the poll list menu to view and vote on active polls")
    public void onPoll(Player player) {
        new PollListMenu(plugin).openMenu(player);
    }

    @Subcommand("close")
    @CommandPermission("poll.close")
    @CommandCompletion("@pollids")
    @Syntax("<pollId>")
    @Description("Close a specific poll (make it inactive)")
    public void onClosePoll(Player player, String pollId) {
        Poll poll = plugin.getDataManager().getPoll(pollId);

        if (poll == null) {
            player.sendMessage(CC.RED + "Poll not found!");
            return;
        }

        if (!poll.isActive()) {
            player.sendMessage(CC.RED + "This poll is already closed!");
            return;
        }

        plugin.getDataManager().updatePollStatus(pollId, false);
        player.sendMessage(CC.GREEN + "Poll closed successfully!");
        player.sendMessage(CC.GRAY + "Question: " + poll.getQuestion());
    }

    @Subcommand("remove")
    @CommandPermission("poll.remove")
    @CommandCompletion("@pollids")
    @Syntax("<pollId>")
    @Description("Remove a poll completely from the database")
    public void onRemovePoll(Player player, String pollId) {
        Poll poll = plugin.getDataManager().getPoll(pollId);

        if (poll == null) {
            player.sendMessage(CC.RED + "Poll not found!");
            return;
        }

        plugin.getDataManager().deletePoll(pollId);
        player.sendMessage(CC.GREEN + "Poll removed successfully!");
        player.sendMessage(CC.GRAY + "Question: " + poll.getQuestion());
    }

    @Subcommand("list")
    @CommandPermission("poll.use")
    @Description("List all active polls in chat")
    public void onListPolls(Player player) {
        Collection<Poll> activePolls = plugin.getDataManager().getActivePolls();

        if (activePolls.isEmpty()) {
            player.sendMessage(CC.WHITE + "No active polls found.");
            return;
        }

        player.sendMessage(CC.WHITE + "Active polls:");
        for (Poll poll : activePolls) {
            player.sendMessage(CC.WHITE + poll.getQuestion() + " " + CC.GRAY + "(ID: " + poll.getId() + ")");
            player.sendMessage("  " + CC.GRAY + "Created by: " + CC.WHITE + poll.getCreatedBy());
            player.sendMessage("  " + CC.GRAY + "Expires: " + CC.WHITE + TimeUtil.formatDateTime(poll.getExpiresAt()));
            player.sendMessage("  " + CC.GRAY + "Votes: " + CC.WHITE + poll.getTotalVotes());
        }
    }

    @Subcommand("info")
    @CommandPermission("poll.use")
    @CommandCompletion("@pollids")
    @Syntax("<pollId>")
    @Description("Show detailed information about a specific poll")
    public void onPollInfo(Player player, String pollId) {
        Poll poll = plugin.getDataManager().getPoll(pollId);

        if (poll == null) {
            player.sendMessage(CC.RED + "Poll not found!");
            return;
        }

        player.sendMessage(CC.WHITE + "Poll Information:");
        player.sendMessage(CC.GRAY + "Question: " + CC.WHITE + poll.getQuestion());
        player.sendMessage(CC.GRAY + "Created by: " + CC.WHITE + poll.getCreatedBy());
        player.sendMessage(CC.GRAY + "Expires: " + CC.WHITE + TimeUtil.formatDateTime(poll.getExpiresAt()));
        player.sendMessage(CC.GRAY + "Status: " + CC.WHITE + (poll.isActive() ? "Active" : "Inactive"));
        player.sendMessage(CC.GRAY + "Total votes: " + CC.WHITE + poll.getTotalVotes());
        player.sendMessage(CC.GRAY + "Options:");

        for (String option : poll.getOptions()) {
            int votes = poll.getVotes().getOrDefault(option, 0);
            double percentage = poll.getOptionPercentage(option) * 100;
            player.sendMessage("  " + CC.GRAY + "- " + CC.WHITE + option + " " + CC.GRAY + "(" + votes + " votes, " + String.format("%.1f", percentage) + "%)");
        }
    }
}

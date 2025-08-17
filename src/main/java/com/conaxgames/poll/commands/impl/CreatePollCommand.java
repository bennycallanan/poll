package com.conaxgames.poll.commands.impl;

import com.conaxgames.libraries.acf.BaseCommand;
import com.conaxgames.libraries.acf.annotation.*;
import com.conaxgames.libraries.util.CC;
import com.conaxgames.poll.PollPlugin;
import com.conaxgames.poll.menus.PollCreationMenu;
import com.conaxgames.poll.util.TimeUtil;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;

@CommandAlias("createpoll")
public class CreatePollCommand extends BaseCommand {

    private final PollPlugin plugin;

    public CreatePollCommand(PollPlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @CommandPermission("poll.create")
    @CommandCompletion("@duration @nothing")
    @Syntax("<duration> <question>")
    @Description("Create a new poll with the specified duration and question")
    public void onCreatePoll(Player player, String duration, String question) {
        try {
            if (!TimeUtil.isValidDuration(duration)) {
                player.sendMessage(CC.RED + "Invalid duration format! Use: <number><unit> (e.g., 1d, 2h, 30m)");
                return;
            }

            LocalDateTime expiresAt = TimeUtil.parseDuration(duration);
            if (question == null || question.trim().isEmpty()) {
                player.sendMessage(CC.RED + "Question cannot be empty!");
                return;
            }

            new PollCreationMenu(plugin, question, expiresAt, player).openMenu(player);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c" + e.getMessage());
        }
    }
}

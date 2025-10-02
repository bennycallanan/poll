package com.conaxgames.poll.menus;

import com.conaxgames.libraries.menu.Button;
import com.conaxgames.libraries.menu.Menu;
import com.conaxgames.libraries.message.FormatUtil;
import com.conaxgames.libraries.util.CC;
import com.conaxgames.libraries.xseries.XMaterial;
import com.conaxgames.poll.PollPlugin;
import com.conaxgames.poll.data.Poll;
import com.conaxgames.poll.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class BasePollMenu extends Menu {
    
    protected final PollPlugin plugin;
    
    protected BasePollMenu(PollPlugin plugin) {
        this.plugin = plugin;
        this.setPlaceholder(plugin.getSettings().useFillerglass);
    }
    
    protected Button createPollInfoButton(Poll poll, Player player) {
        return new Button() {
            @Override
            public String getName(Player player) {
                return CC.YELLOW + "Poll #" + poll.getId();
            }

            @Override
            public List<String> getDescription(Player player) {
                List<String> lore = new ArrayList<>();
                
                List<String> wrappedQuestion = FormatUtil.wordWrap(CC.YELLOW + poll.getQuestion(), 40);
                lore.addAll(wrappedQuestion);
                lore.add("");
                
                lore.add(CC.GRAY + "Created by: " + CC.WHITE + poll.getCreatedBy());
                lore.add(CC.GRAY + "Expires: " + CC.WHITE + TimeUtil.formatDateTime(poll.getExpiresAt()));
                lore.add(CC.GRAY + "Time remaining: " + CC.WHITE + TimeUtil.getTimeRemaining(poll.getExpiresAt()));
                lore.add(CC.GRAY + "Total votes: " + CC.WHITE + poll.getTotalVotes());
                lore.add("");
                
                if (poll.hasVoted(player.getUniqueId())) {
                    String votedOption = poll.getPlayerVote(player.getUniqueId());
                    lore.add(CC.GREEN + "✓ You voted for: " + CC.WHITE + votedOption);
                } else {
                    lore.add(CC.GRAY + "Click an option below to vote!");
                }
                
                return lore;
            }

            @Override
            public Material getMaterial(Player player) {
                return XMaterial.BOOK.get();
            }
        };
    }
}

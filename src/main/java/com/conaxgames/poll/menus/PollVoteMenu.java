package com.conaxgames.poll.menus;

import com.conaxgames.libraries.menu.Button;
import com.conaxgames.libraries.message.FormatUtil;
import com.conaxgames.libraries.util.CC;
import com.conaxgames.libraries.xseries.XMaterial;
import com.conaxgames.poll.PollPlugin;
import com.conaxgames.poll.data.Poll;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PollVoteMenu extends BasePollMenu {
    
    @Getter
    private final Poll poll;
    
    public PollVoteMenu(Poll poll) {
        super(PollPlugin.getInstance());
        this.poll = poll;
    }

    @Override
    public String getTitle(Player player) {
        return CC.DARK_GRAY + "Poll #" + poll.getId();
    }
    
    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        
        buttons.put(4, createPollInfoButton(poll, player));
        
        int slot = 19;
        int optionIndex = 1;
        for (String option : poll.getOptions()) {
            if (slot >= 43) break;
            
            buttons.put(slot, new VoteButton(option, optionIndex));
            slot++;
            if ((slot - 19) % 9 == 7) {
                slot += 2;
            }
            optionIndex++;
        }
        
        return buttons;
    }

    private class VoteButton extends Button {
        
        private final String option;
        private final int optionIndex;
        
        public VoteButton(String option, int optionIndex) {
            this.option = option;
            this.optionIndex = optionIndex;
        }
        
        @Override
        public String getName(Player player) {
            if (poll.hasVoted(player.getUniqueId()) && poll.getPlayerVote(player.getUniqueId()).equals(option)) {
                return CC.GREEN + "✓ Answer #" + optionIndex + ": " + option;
            }
            return CC.WHITE + "Answer #" + optionIndex + ": " + option;
        }

        @Override
        public List<String> getDescription(Player player) {
            List<String> lore = new ArrayList<>();
            
            List<String> wrappedOption = FormatUtil.wordWrap(CC.YELLOW + option, 40);
            lore.addAll(wrappedOption);
            lore.add("");
            
            int votes = poll.getVotes().getOrDefault(option, 0);
            int total = poll.getTotalVotes();
            double percentage = total > 0 ? (double) votes / total * 100 : 0;
            
            lore.add(CC.GRAY + "Votes: " + CC.WHITE + votes);
            lore.add(CC.GRAY + "Percentage: " + CC.WHITE + String.format("%.1f", percentage) + "%");
            lore.add("");
            
            if (poll.hasVoted(player.getUniqueId())) {
                if (poll.getPlayerVote(player.getUniqueId()).equals(option)) {
                    lore.add(CC.GREEN + "✓ You voted for this option");
                } else {
                    lore.add(CC.RED + "You already voted for a different option");
                }
            } else {
                lore.add(CC.GRAY + "Click to vote for this option");
            }
            
            return lore;
        }

        @Override
        public Material getMaterial(Player player) {
            if (poll.hasVoted(player.getUniqueId()) && poll.getPlayerVote(player.getUniqueId()).equals(option)) {
                return XMaterial.LIME_WOOL.get();
            }
            return XMaterial.WHITE_WOOL.get();
        }
        
        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            // Voting is handled in the listener
        }
    }
}

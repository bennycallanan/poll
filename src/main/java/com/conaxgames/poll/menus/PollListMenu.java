package com.conaxgames.poll.menus;

import com.conaxgames.libraries.menu.Button;
import com.conaxgames.libraries.menu.Menu;
import com.conaxgames.libraries.util.CC;
import com.conaxgames.libraries.xseries.XMaterial;
import com.conaxgames.poll.PollPlugin;
import com.conaxgames.poll.data.Poll;
import com.conaxgames.poll.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PollListMenu extends Menu {
    
    private final PollPlugin plugin;
    
    public PollListMenu(PollPlugin plugin) {
        this.plugin = plugin;
        this.setPlaceholder(plugin.getSettings().useFillerglass);
        this.setAutoUpdate(plugin.getSettings().autoUpdate);
    }
    
    @Override
    public String getTitle(Player player) {
        return CC.DARK_GRAY + "Active Polls";
    }
    
    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        
        List<Poll> activePolls = new ArrayList<>(plugin.getDataManager().getActivePolls());
        
        if (activePolls.isEmpty()) {
            buttons.put(13, new Button() {
                @Override
                public String getName(Player player) {
                    return CC.RED + "No Active Polls";
                }

                @Override
                public List<String> getDescription(Player player) {
                    List<String> lore = new ArrayList<>();
                    lore.add(CC.GRAY + "There are currently no active polls.");
                    lore.add(CC.GRAY + "Check back later!");
                    return lore;
                }

                @Override
                public Material getMaterial(Player player) {
                    return XMaterial.BARRIER.get();
                }
            });
        } else {
            int slot = 10;
            for (Poll poll : activePolls) {
                if (slot >= 35) break;
                
                buttons.put(slot, new PollButton(poll));
                slot++;
                if ((slot - 10) % 9 == 7) {
                    slot += 2;
                }
            }
        }
        
        return buttons;
    }

    private class PollButton extends Button {
        
        private final Poll poll;
        
        public PollButton(Poll poll) {
            this.poll = poll;
        }
        
        @Override
        public String getName(Player player) {
            return CC.YELLOW + poll.getQuestion();
        }

        @Override
        public List<String> getDescription(Player player) {
            List<String> lore = new ArrayList<>();
            lore.add(CC.GRAY + "Created by: " + CC.WHITE + poll.getCreatedBy());
            lore.add(CC.GRAY + "Expires: " + CC.WHITE + TimeUtil.formatDateTime(poll.getExpiresAt()));
            lore.add(CC.GRAY + "Time remaining: " + CC.WHITE + TimeUtil.getTimeRemaining(poll.getExpiresAt()));
            lore.add(CC.GRAY + "Total votes: " + CC.WHITE + poll.getTotalVotes());
            lore.add("");
            
            if (poll.hasVoted(player.getUniqueId())) {
                String votedOption = poll.getPlayerVote(player.getUniqueId());
                lore.add(CC.GREEN + "✓ You voted for: " + CC.WHITE + votedOption);
            } else {
                lore.add(CC.GRAY + "Click to view and vote!");
            }
            
            return lore;
        }

        @Override
        public Material getMaterial(Player player) {
            return XMaterial.PAPER.get();
        }
        
        @Override
        public void clicked(Player player, int slot, ClickType clickType) {
            new PollVoteMenu(poll).openMenu(player);
        }
    }
}

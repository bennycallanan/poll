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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PollCreationMenu extends Menu {
    
    private final PollPlugin plugin;
    private final String question;
    private final LocalDateTime expiresAt;
    private final List<String> options;
    private final Player creator;
    
    public PollCreationMenu(PollPlugin plugin, String question, LocalDateTime expiresAt, Player creator) {
        this.plugin = plugin;
        this.question = question;
        this.expiresAt = expiresAt;
        this.creator = creator;
        this.options = new ArrayList<>();
        this.setPlaceholder(plugin.getSettings().useFillerglass);
    }

    public void addOption(String option) {
        options.add(option);
    }
    
    @Override
    public String getTitle(Player player) {
        return CC.DARK_GRAY + "Create Poll";
    }
    
    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        
        buttons.put(4, new Button() {
            @Override
            public String getName(Player player) {
                return CC.YELLOW + "Question: " + question;
            }

            @Override
            public List<String> getDescription(Player player) {
                List<String> lore = new ArrayList<>();
                lore.add(CC.GRAY + "Duration: " + CC.WHITE + TimeUtil.formatDateTime(expiresAt));
                lore.add(CC.GRAY + "Options: " + CC.WHITE + options.size() + "/6");
                return lore;
            }

            @Override
            public Material getMaterial(Player player) {
                return XMaterial.BOOK.get();
            }
        });
        
        buttons.put(20, new Button() {
            @Override
            public String getName(Player player) {
                return CC.GREEN + "+ Add Option";
            }

            @Override
            public List<String> getDescription(Player player) {
                List<String> lore = new ArrayList<>();
                lore.add(CC.GRAY + "Click to add a new option");
                lore.add(CC.GRAY + "You'll be prompted to type the option text");
                return lore;
            }

            @Override
            public Material getMaterial(Player player) {
                return XMaterial.EMERALD.get();
            }
            
            @Override
            public void clicked(Player player, int slot, ClickType clickType) {
                plugin.getListener().registerOptionInput(player, PollCreationMenu.this);
            }
        });
        
        buttons.put(24, new Button() {
            @Override
            public String getName(Player player) {
                return options.size() >= 2 ? CC.GREEN + "✓ Create Poll" : CC.RED + "Need at least 2 options";
            }

            @Override
            public List<String> getDescription(Player player) {
                List<String> lore = new ArrayList<>();
                if (options.size() >= 2) {
                    lore.add(CC.GRAY + "Click to create the poll");
                    lore.add(CC.GRAY + "This will make it active immediately");
                } else {
                    lore.add(CC.GRAY + "You need at least 2 options");
                    lore.add(CC.GRAY + "to create a poll");
                }
                return lore;
            }

            @Override
            public Material getMaterial(Player player) {
                return options.size() >= 2 ? XMaterial.LIME_WOOL.get() : XMaterial.RED_WOOL.get();
            }
            
            @Override
            public void clicked(Player player, int slot, ClickType clickType) {
                if (options.size() < 2) {
                    player.sendMessage(CC.RED + "You need at least 2 options to create a poll!");
                    return;
                }
                
                Poll poll = new Poll(question, new ArrayList<>(options), expiresAt, creator.getName());
                plugin.getDataManager().savePoll(poll);
                
                player.sendMessage(CC.GREEN + "Poll created successfully!");
                player.sendMessage(CC.GRAY + "Question: " + question);
                player.sendMessage(CC.GRAY + "Options: " + String.join(", ", options));
                player.closeInventory();
            }
        });
        
        int slot = 28;
        for (int i = 0; i < options.size(); i++) {
            if (slot >= 43) break;
            
            final int index = i;
            final String option = options.get(i);
            
            buttons.put(slot, new Button() {
                @Override
                public String getName(Player player) {
                    return CC.WHITE + option;
                }

                @Override
                public List<String> getDescription(Player player) {
                    List<String> lore = new ArrayList<>();
                    lore.add(CC.GRAY + "Click to remove this option");
                    return lore;
                }

                @Override
                public Material getMaterial(Player player) {
                    return XMaterial.PAPER.get();
                }
                
                @Override
                public void clicked(Player player, int slot, ClickType clickType) {
                    options.remove(index);
                    player.sendMessage(CC.GREEN + "Removed option: " + option);
                    openMenu(player);
                }
            });
            
            slot++;
            if ((slot - 28) % 9 == 7) {
                slot += 2;
            }
        }
        return buttons;
    }
}

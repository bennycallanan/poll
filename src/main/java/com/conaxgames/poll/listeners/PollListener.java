package com.conaxgames.poll.listeners;

import com.conaxgames.libraries.menu.Menu;
import com.conaxgames.libraries.util.CC;
import com.conaxgames.poll.PollPlugin;
import com.conaxgames.poll.data.Poll;
import com.conaxgames.poll.menus.PollCreationMenu;
import com.conaxgames.poll.menus.PollVoteMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PollListener implements Listener {
    
    private final PollPlugin plugin;
    private final Map<UUID, PollCreationMenu> playersAddingOptions = new HashMap<>();
    
    public PollListener(PollPlugin plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getDataManager().cleanupExpiredPolls();
            }
        }.runTaskTimer(plugin, 20L * 60 * 5, 20L * 60 * 5);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Menu menu = Menu.currentlyOpenedMenus.get(player.getName());
        if (menu instanceof PollVoteMenu) {
            PollVoteMenu voteMenu = (PollVoteMenu) menu;
            Poll poll = voteMenu.getPoll();
            
            if (poll == null) {
                return;
            }
            
            if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
                String cleanOption = null;
                for (String pollOption : poll.getOptions()) {
                    if (displayName.contains(pollOption)) {
                        cleanOption = pollOption;
                        break;
                    }
                }
                
                if (cleanOption != null) {
                    event.setCancelled(true);
                    handleVote(player, poll, cleanOption);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (playersAddingOptions.containsKey(playerId)) {
            event.setCancelled(true);
            
            String message = event.getMessage();
            if (message.equalsIgnoreCase("cancel")) {
                playersAddingOptions.remove(playerId);
                player.sendMessage(CC.RED + "Option creation cancelled.");
                return;
            }

            if (message.trim().isEmpty()) {
                player.sendMessage(CC.RED + "Option text cannot be empty! Type 'cancel' to cancel.");
                return;
            }
            
            if (message.length() > 50) {
                player.sendMessage(CC.RED + "Option text is too long! Maximum 50 characters. Type 'cancel' to cancel.");
                return;
            }

            PollCreationMenu menu = playersAddingOptions.remove(playerId);
            menu.addOption(message.trim());

            new BukkitRunnable() {
                @Override
                public void run() {
                    menu.openMenu(player);
                }
            }.runTask(plugin);
            
            player.sendMessage(CC.GREEN + "Added option: " + message.trim());
        }
    }

    private void handleVote(Player player, Poll poll, String option) {
        UUID playerId = player.getUniqueId();
        
        if (poll.isExpired()) {
            player.sendMessage(CC.RED + "This poll has expired!");
            player.closeInventory();
            return;
        }
        
        if (poll.hasVoted(playerId)) {
            player.sendMessage(CC.RED + "You have already voted on this poll!");
            return;
        }
        
        if (poll.vote(playerId, option)) {
            plugin.getDataManager().saveVote(poll.getId(), playerId, option);
            player.sendMessage(CC.GREEN + "Your vote has been recorded!");
            player.closeInventory();
        } else {
            player.sendMessage(CC.RED + "Failed to record your vote. Please try again.");
        }
    }

    public void registerOptionInput(Player player, PollCreationMenu menu) {
        playersAddingOptions.put(player.getUniqueId(), menu);
        player.closeInventory();
        player.sendMessage(CC.WHITE + "Type the option text in chat:");
        player.sendMessage(CC.RED + "Type 'cancel' to cancel");
    }
}

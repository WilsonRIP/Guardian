package com.anticheat.guardian;

import com.anticheat.guardian.commands.AlertsCommand;
import com.anticheat.guardian.commands.DebugCommand;
import com.anticheat.guardian.commands.ViolationsCommand;
import com.anticheat.guardian.commands.SpectateCommand;
import com.anticheat.guardian.commands.StopSpectateCommand;
import com.anticheat.guardian.commands.PerformanceCommand;
import com.anticheat.guardian.managers.CheckManager;
import com.anticheat.guardian.managers.PlayerDataManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Guardian extends JavaPlugin {
    
    @Getter
    private static Guardian instance;
    
    @Getter
    private CheckManager checkManager;
    
    @Getter
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.checkManager = new CheckManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Register commands
        registerCommands();
        
        getLogger().info("Guardian Anti-Cheat has been enabled!");
    }

    @Override
    public void onDisable() {
        // Clean up resources
        if (playerDataManager != null) {
            playerDataManager.cleanup();
        }
        
        getLogger().info("Guardian Anti-Cheat has been disabled!");
    }
    
    private void registerCommands() {
        getCommand("guardian").setExecutor(this);
        getCommand("alerts").setExecutor(new AlertsCommand(this));
        getCommand("violations").setExecutor(new ViolationsCommand(this));
        getCommand("debug").setExecutor(new DebugCommand(this));
        getCommand("spectate").setExecutor(new SpectateCommand(this));
        getCommand("stopspectate").setExecutor(new StopSpectateCommand(this));
        getCommand("performance").setExecutor(new PerformanceCommand(this));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("guardian")) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }
            
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "help":
                    sendHelp(sender);
                    break;
                case "reload":
                    if (!sender.hasPermission("guardian.reload")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                        return true;
                    }
                    // TODO: Add reload functionality
                    sender.sendMessage(ChatColor.GREEN + "Guardian has been reloaded!");
                    break;
                case "version":
                    sender.sendMessage(ChatColor.GRAY + "Guardian version: " + ChatColor.GREEN + getDescription().getVersion());
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown command. Type /guardian help for help.");
            }
            return true;
        }
        return false;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Guardian Anti-Cheat Commands:");
        sender.sendMessage(ChatColor.GRAY + "/guardian help " + ChatColor.WHITE + "- Show this help message");
        sender.sendMessage(ChatColor.GRAY + "/guardian reload " + ChatColor.WHITE + "- Reload the plugin");
        sender.sendMessage(ChatColor.GRAY + "/guardian version " + ChatColor.WHITE + "- Show plugin version");
        sender.sendMessage(ChatColor.GRAY + "/alerts " + ChatColor.WHITE + "- Toggle anticheat alerts");
        sender.sendMessage(ChatColor.GRAY + "/violations <player> " + ChatColor.WHITE + "- Check player violations");
        sender.sendMessage(ChatColor.GRAY + "/debug <player> " + ChatColor.WHITE + "- Show detailed debug information");
        sender.sendMessage(ChatColor.GRAY + "/spectate <player> " + ChatColor.WHITE + "- Spectate a player");
        sender.sendMessage(ChatColor.GRAY + "/stopspectate " + ChatColor.WHITE + "- Exit spectator mode");
        sender.sendMessage(ChatColor.GRAY + "/performance " + ChatColor.WHITE + "- View performance metrics");
    }
    
    public void broadcastAlert(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("guardian.alerts") && AlertsCommand.hasAlertsEnabled(player.getUniqueId())) {
                player.sendMessage(ChatColor.GRAY + "[" + ChatColor.RED + "Guardian" + ChatColor.GRAY + "] " + 
                                 ChatColor.WHITE + message);
            }
        }
    }
} 
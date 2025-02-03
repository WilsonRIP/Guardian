package com.anticheat.guardian;

import com.anticheat.guardian.commands.AlertsCommand;
import com.anticheat.guardian.commands.DebugCommand;
import com.anticheat.guardian.commands.ViolationsCommand;
import com.anticheat.guardian.commands.SpectateCommand;
import com.anticheat.guardian.commands.StopSpectateCommand;
import com.anticheat.guardian.commands.PerformanceCommand;
import com.anticheat.guardian.managers.CheckManager;
import com.anticheat.guardian.managers.PlayerDataManager;
import com.anticheat.guardian.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.Collection;

public class Guardian extends JavaPlugin {
    
    @Getter
    private static Guardian instance;
    
    @Getter
    private CheckManager checkManager;
    
    @Getter
    private PlayerDataManager playerDataManager;
    
    @Getter
    private FileConfiguration messages;

    @Override
    public void onEnable() {
        instance = this;
        
        // Load messages
        loadMessages();
        
        // Initialize managers
        this.checkManager = new CheckManager();
        this.playerDataManager = new PlayerDataManager();
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Register commands
        registerCommands();
        
        getLogger().info(MessageUtils.getMessage("plugin.enabled"));
    }

    @Override
    public void onDisable() {
        // Clean up resources
        if (playerDataManager != null) {
            playerDataManager.cleanup();
        }
        
        getLogger().info(MessageUtils.getMessage("plugin.disabled"));
    }
    
    private void loadMessages() {
        saveResource("messages.yml", false);
        File messagesFile = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
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
                        MessageUtils.sendMessage(sender, "commands.no-permission");
                        return true;
                    }
                    reloadConfig();
                    loadMessages();
                    playerDataManager.cleanupAll();
                    checkManager = new CheckManager();
                    MessageUtils.sendMessage(sender, "commands.reload.success");
                    break;
                case "version":
                    MessageUtils.sendMessage(sender, "commands.version", "{version}", getDescription().getVersion());
                    break;
                default:
                    MessageUtils.sendMessage(sender, "commands.unknown");
            }
            return true;
        }
        return false;
    }
    
    private void sendHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, "commands.help.header");
        MessageUtils.sendMessage(sender, "commands.help.help");
        MessageUtils.sendMessage(sender, "commands.help.reload");
        MessageUtils.sendMessage(sender, "commands.help.version");
        MessageUtils.sendMessage(sender, "commands.help.alerts");
        MessageUtils.sendMessage(sender, "commands.help.violations");
        MessageUtils.sendMessage(sender, "commands.help.debug");
        MessageUtils.sendMessage(sender, "commands.help.spectate");
        MessageUtils.sendMessage(sender, "commands.help.stopspectate");
        MessageUtils.sendMessage(sender, "commands.help.performance");
    }
    
    public void broadcastAlert(String message) {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for (Player player : onlinePlayers) {
            if (player.hasPermission("guardian.alerts") && AlertsCommand.hasAlertsEnabled(player.getUniqueId())) {
                MessageUtils.sendMessage(player, message);
            }
        }
    }
} 
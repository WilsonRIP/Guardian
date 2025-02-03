package com.anticheat.guardian.utils;

import com.anticheat.guardian.Guardian;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {
    
    public static String getMessage(String path) {
        String message = Guardian.getInstance().getMessages().getString(path);
        return message != null ? ChatColor.translateAlternateColorCodes('&', message) : path;
    }
    
    public static String getMessage(String path, String... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be in pairs of placeholder and value");
        }
        
        String message = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }
    
    public static void sendMessage(CommandSender sender, String path) {
        sender.sendMessage(getMessage(path));
    }
    
    public static void sendMessage(CommandSender sender, String path, String... replacements) {
        sender.sendMessage(getMessage(path, replacements));
    }
    
    public static String formatMessage(String message, String... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be in pairs of placeholder and value");
        }
        
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static String stripColor(String message) {
        return ChatColor.stripColor(message);
    }
} 
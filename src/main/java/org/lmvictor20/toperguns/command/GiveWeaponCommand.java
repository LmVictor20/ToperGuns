package org.lmvictor20.toperguns.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lmvictor20.toperguns.ToperGunsPlugin;

public class GiveWeaponCommand implements CommandExecutor {

    private final ToperGunsPlugin plugin;

    public GiveWeaponCommand(ToperGunsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " <player> <weaponTitle> [amount]");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found");
            return true;
        }
        String title = args[1];
        int amount = 1;
        if (args.length >= 3) {
            try { amount = Math.max(1, Integer.parseInt(args[2])); } catch (Exception ignored) {}
        }
        if (plugin.getWeaponManager() == null) {
            sender.sendMessage("Weapon manager not ready, try again in a moment.");
            return true;
        }
        ItemStack item = plugin.getWeaponManager().generateWeapon(title);
        if (item == null) {
            sender.sendMessage("Unknown weapon: " + title);
            return true;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage("Gave " + amount + "x " + title + " to " + target.getName());
        return true;
    }
}



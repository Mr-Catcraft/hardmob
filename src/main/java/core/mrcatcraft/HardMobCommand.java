package core.mrcatcraft;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the {@code /hardmob} command with subcommands:
 * <ul>
 *   <li>{@code reload} – reloads configuration</li>
 *   <li>{@code spawn <mob> <level> [elite]} – spawns a custom HardMob mob</li>
 *   <li>{@code spawnboss <mob> <level>} – spawns an elite boss (same as spawn with elite flag)</li>
 * </ul>
 * Also provides tab completion for subcommands, mob types, level values, and the "elite" flag.
 */
public class HardMobCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "spawn", "spawnboss");
    private final HardMob plugin;

    /**
     * Constructs the command executor.
     *
     * @param plugin the main plugin instance
     */
    public HardMobCommand(HardMob plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender);
            case "spawn":
                return handleSpawn(sender, args);
            case "spawnboss":
                return handleSpawnBoss(sender, args);
            default:
                sendUsage(sender, label);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partialMatch(args[0], SUBCOMMANDS);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("spawnboss"))) {
            List<String> mobs = Arrays.stream(EntityType.values())
                    .filter(EntityType::isAlive)
                    .filter(t -> Mob.class.isAssignableFrom(t.getEntityClass()))
                    .map(EntityType::name)
                    .collect(Collectors.toList());
            return partialMatch(args[1], mobs);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("spawn")) {
            return partialMatch(args[2], List.of("1", "10", "25", "50", "75", "99"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("spawnboss")) {
            return partialMatch(args[2], List.of("1", "10", "25", "50", "75", "99"));
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("spawn")) {
            return partialMatch(args[3], List.of("elite"));
        }
        return Collections.emptyList();
    }

    /**
     * Filters a list of options, keeping only those that start with the given token (case‑insensitive).
     */
    private List<String> partialMatch(String token, List<String> options) {
        String lower = token.toLowerCase();
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(lower))
                .sorted()
                .collect(Collectors.toList());
    }

    /** Handles the reload subcommand. */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("hardmob.reload")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        plugin.reloadPlugin();
        sender.sendMessage(ChatColor.GREEN + "HardMob reloaded.");
        return true;
    }

    /** Handles the spawn subcommand. */
    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can spawn mobs.");
            return true;
        }
        if (!sender.hasPermission("hardmob.spawn")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /hardmob spawn <mob> <level> [elite]");
            return true;
        }

        EntityType type = parseMobType(args[1]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Invalid mob type: " + args[1]);
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
            if (level < 1 || level > 99) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Level must be 1-99.");
            return true;
        }

        boolean elite = args.length > 3 && args[3].equalsIgnoreCase("elite");
        spawnMob(player, type, level, elite);
        sender.sendMessage(ChatColor.GREEN + "Spawned " + type.name() + (elite ? " (ELITE)" : " level " + level));
        return true;
    }

    /** Handles the spawnboss subcommand (always elite). */
    private boolean handleSpawnBoss(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can spawn mobs.");
            return true;
        }
        if (!sender.hasPermission("hardmob.spawnboss")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /hardmob spawnboss <mob> <level>");
            return true;
        }

        EntityType type = parseMobType(args[1]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Invalid mob type: " + args[1]);
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
            if (level < 1 || level > 99) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Level must be 1-99.");
            return true;
        }

        spawnMob(player, type, level, true);
        sender.sendMessage(ChatColor.GREEN + "Spawned BOSS " + type.name() + " level " + level);
        return true;
    }

    /**
     * Attempts to parse a string to an {@link EntityType} that is both alive and assignable to {@link Mob}.
     *
     * @param name the entity type name (case‑insensitive)
     * @return the matching EntityType, or null if invalid
     */
    private EntityType parseMobType(String name) {
        try {
            EntityType type = EntityType.valueOf(name.toUpperCase());
            if (type.isAlive() && Mob.class.isAssignableFrom(type.getEntityClass())) {
                return type;
            }
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    /**
     * Spawns a mob at the player's location, sets its level, elite status, applies modifiers,
     * size, equipment, and updates the custom name.
     */
    private void spawnMob(Player player, EntityType type, int level, boolean elite) {
        Location loc = player.getLocation();
        Mob mob = (Mob) player.getWorld().spawnEntity(loc, type);
        mob.getPersistentDataContainer().set(plugin.getMobLevelManager().getLevelKey(), PersistentDataType.INTEGER, level);
        if (elite) {
            mob.getPersistentDataContainer().set(plugin.getMobLevelManager().getEliteKey(), PersistentDataType.BOOLEAN, true);
        }
        plugin.getMobLevelManager().applyLevelModifiers(mob, level, elite);
        if (plugin.getConfigManager().isDynamicSizeEnabled()) {
            double size = elite
                    ? plugin.getConfigManager().getEliteSize()
                    : plugin.getConfigManager().getDynamicSizeMin()
                      + Math.random() * (plugin.getConfigManager().getDynamicSizeMax() - plugin.getConfigManager().getDynamicSizeMin());
            mob.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(size);
        }
        if (elite && plugin.getConfigManager().isEliteAlwaysEquipped()) {
            plugin.getEliteMobManager().applyEliteEquipment(mob, level);
        }
        NameUpdater.updateName(mob);
    }

    /** Sends a help message with available subcommands. */
    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "=== HardMob Help ===");
        sender.sendMessage("/" + label + " reload");
        sender.sendMessage("/" + label + " spawn <mob> <level> [elite]");
        sender.sendMessage("/" + label + " spawnboss <mob> <level>");
    }
}
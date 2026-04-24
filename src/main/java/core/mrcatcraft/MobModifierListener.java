package core.mrcatcraft;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Core listener that intercepts mob spawns, damage, and death events
 * to apply HardMob logic.
 */
public class MobModifierListener implements Listener {

    private final HardMob plugin;

    /**
     * Constructs the listener.
     *
     * @param plugin the main plugin instance
     */
    public MobModifierListener(HardMob plugin) {
        this.plugin = plugin;
    }

    /**
     * When a creature spawns, we roll a level, decide whether it is elite,
     * apply stat modifiers, size, equipment, and set the custom name.
     * Only affects allowed mobs in non‑blacklisted worlds.
     */
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!plugin.getConfigManager().isEnabled()) return;

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob mob)) return;
        if (!isAllowed(entity)) return;

        World world = entity.getWorld();
        if (plugin.getConfigManager().getWorldBlacklist().contains(world.getName())) return;

        int level = plugin.getMobLevelManager().rollLevel(world);
        boolean elite = plugin.getEliteMobManager().shouldBeElite(level);

        // Store data in persistent data container
        mob.getPersistentDataContainer().set(plugin.getMobLevelManager().getLevelKey(), PersistentDataType.INTEGER, level);
        if (elite) {
            mob.getPersistentDataContainer().set(plugin.getMobLevelManager().getEliteKey(), PersistentDataType.BOOLEAN, true);
        }

        // Apply attribute modifiers (preserves health percentage)
        plugin.getMobLevelManager().applyLevelModifiers(mob, level, elite);

        // Dynamic scaling
        if (plugin.getConfigManager().isDynamicSizeEnabled()) {
            applyDynamicSize(mob, elite);
        }

        // Equipment for elites
        if (elite && plugin.getConfigManager().isEliteAlwaysEquipped()) {
            plugin.getEliteMobManager().applyEliteEquipment(mob, level);
        }

        // Custom name above mob
        NameUpdater.updateName(mob);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Spawned " + entity.getType().name() + " lv." + level
                    + (elite ? " (ELITE)" : "") + " via " + event.getSpawnReason());
        }
    }

    /**
     * Sets the entity's {@code GENERIC_SCALE} attribute based on elite status and config bounds.
     */
    private void applyDynamicSize(LivingEntity entity, boolean elite) {
        double size;
        if (elite) {
            size = plugin.getConfigManager().getEliteSize();
        } else {
            double min = plugin.getConfigManager().getDynamicSizeMin();
            double max = plugin.getConfigManager().getDynamicSizeMax();
            size = min + ThreadLocalRandom.current().nextDouble() * (max - min);
        }
        AttributeInstance scaleAttr = entity.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(size);
        }
    }

    /**
     * Forces a name update whenever a HardMob entity takes damage, so the health display refreshes instantly.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && entity instanceof Mob && isAllowed(entity)) {
            NameUpdater.forceUpdateName(entity);
        }
    }

    /**
     * Handles elite drops and clears the custom name upon death to avoid cluttering the death message.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isAllowed(entity)) return;

        int level = plugin.getMobLevelManager().getLevelFromMob(entity);
        boolean elite = plugin.getMobLevelManager().isElite(entity);

        if (level == 0 && !elite) return;

        // Remove custom name immediately so it doesn't appear in the death message
        entity.setCustomName(null);
        entity.setCustomNameVisible(false);

        if (!elite) return;
        if (!plugin.getConfigManager().isEliteEnabled()) return;

        ConfigManager cfg = plugin.getConfigManager();

        // Diamonds
        int diamonds = ThreadLocalRandom.current().nextInt(cfg.getEliteDiamondsMin(), cfg.getEliteDiamondsMax() + 1);
        if (diamonds > 0) event.getDrops().add(new ItemStack(Material.DIAMOND, diamonds));

        // Gold
        int gold = ThreadLocalRandom.current().nextInt(cfg.getEliteGoldMin(), cfg.getEliteGoldMax() + 1);
        if (gold > 0) event.getDrops().add(new ItemStack(Material.GOLD_INGOT, gold));

        // Music discs
        if (!cfg.getEliteMusicDiscs().isEmpty()) {
            String discName = cfg.getEliteMusicDiscs().get(ThreadLocalRandom.current().nextInt(cfg.getEliteMusicDiscs().size()));
            Material disc = Material.getMaterial(discName);
            if (disc != null) event.getDrops().add(new ItemStack(disc, 1));
        }

        // Bonus XP proportional to level
        event.setDroppedExp(event.getDroppedExp() + level * cfg.getEliteExpPerLevel());
    }

    /**
     * Checks whether the given entity should be processed by the plugin.
     * It must not be a player, and if the whitelist is non‑empty, its type must be listed.
     */
    private boolean isAllowed(LivingEntity entity) {
        if (entity instanceof Player) return false;
        var whitelist = plugin.getConfigManager().getMobWhitelist();
        if (whitelist.isEmpty()) return true;
        return whitelist.contains(entity.getType().name());
    }
}
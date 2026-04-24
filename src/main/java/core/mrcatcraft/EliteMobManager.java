package core.mrcatcraft;

import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles elite mob logic: determines whether a mob should be elite,
 * and applies full equipment sets based on the mob's level and the
 * thresholds defined in config.yml.
 */
public class EliteMobManager {

    private final HardMob plugin;

    /**
     * Constructs the elite mob manager.
     *
     * @param plugin the main plugin instance
     */
    public EliteMobManager(HardMob plugin) {
        this.plugin = plugin;
    }

    /** Called on /hardmob reload; currently no state to clear. */
    public void reload() {}

    /**
     * Decides whether a mob should become elite based on the configured spawn chance.
     *
     * @param level the mob's level (not directly used, but available for future logic)
     * @return true if the mob is elite
     */
    public boolean shouldBeElite(int level) {
        if (!plugin.getConfigManager().isEliteEnabled()) return false;
        double chance = plugin.getConfigManager().getEliteSpawnChancePercent() / 100.0;
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    /**
     * Applies a full set of armour to an elite mob according to its level
     * and the equipment thresholds in the configuration.
     *
     * @param mob   the target mob
     * @param level the mob's level, used to determine the tier
     */
    public void applyEliteEquipment(Mob mob, int level) {
        EntityEquipment equip = mob.getEquipment();
        if (equip == null) return;

        String tier = getEquipmentTier(level);
        if (tier == null) return;

        Material helmet = null, chestplate = null, leggings = null, boots = null;
        switch (tier.toLowerCase()) {
            case "chainmail":
                helmet = Material.CHAINMAIL_HELMET;
                chestplate = Material.CHAINMAIL_CHESTPLATE;
                leggings = Material.CHAINMAIL_LEGGINGS;
                boots = Material.CHAINMAIL_BOOTS;
                break;
            case "iron":
                helmet = Material.IRON_HELMET;
                chestplate = Material.IRON_CHESTPLATE;
                leggings = Material.IRON_LEGGINGS;
                boots = Material.IRON_BOOTS;
                break;
            case "diamond":
                helmet = Material.DIAMOND_HELMET;
                chestplate = Material.DIAMOND_CHESTPLATE;
                leggings = Material.DIAMOND_LEGGINGS;
                boots = Material.DIAMOND_BOOTS;
                break;
            case "netherite":
                helmet = Material.NETHERITE_HELMET;
                chestplate = Material.NETHERITE_CHESTPLATE;
                leggings = Material.NETHERITE_LEGGINGS;
                boots = Material.NETHERITE_BOOTS;
                break;
        }

        if (helmet != null) equip.setHelmet(new ItemStack(helmet));
        if (chestplate != null) equip.setChestplate(new ItemStack(chestplate));
        if (leggings != null) equip.setLeggings(new ItemStack(leggings));
        if (boots != null) equip.setBoots(new ItemStack(boots));
    }

    /**
     * Returns the highest equipment tier the mob qualifies for.
     * The thresholds map is iterated and the last matching tier is returned.
     *
     * @param level the mob's level
     * @return the tier name (e.g. "iron") or null if no thresholds are met
     */
    public String getEquipmentTier(int level) {
        Map<Integer, String> thresholds = plugin.getConfigManager().getEquipmentThresholds();
        if (thresholds == null || thresholds.isEmpty()) return null;

        String currentTier = null;
        for (Map.Entry<Integer, String> entry : thresholds.entrySet()) {
            if (level >= entry.getKey()) {
                currentTier = entry.getValue();
            }
        }
        return currentTier;
    }
}
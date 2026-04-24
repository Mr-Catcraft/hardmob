package core.mrcatcraft;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Scheduled task that periodically refreshes the custom names of all
 * HardMob entities in all worlds. This ensures that the displayed
 * health stays accurate even without a damage event.
 */
public class NameUpdateTask extends BukkitRunnable {

    @Override
    public void run() {
        HardMob plugin = HardMob.getInstance();
        if (!plugin.getConfigManager().isCustomNameEnabled()) return;

        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class)) {
                if (entity instanceof Mob && !(entity instanceof org.bukkit.entity.Player)) {
                    int level = plugin.getMobLevelManager().getLevelFromMob(entity);
                    boolean elite = plugin.getMobLevelManager().isElite(entity);
                    if (level != 0 || elite) {
                        NameUpdater.updateName(entity); // respects internal health cache
                    }
                }
            }
        }
    }

    /**
     * Immediately runs the task once, useful after a configuration reload.
     */
    public void updateAllNames() {
        run();
    }
}
package dev.vaileks.runesaltarv.weapons.nightpiercer.managers;

import dev.vaileks.runesaltarv.RunesAltarV;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class NightHealthStealManager implements Listener {
   private final RunesAltarV plugin;
   private final Map<UUID, Double> baseHealth = new HashMap<>();

   public NightHealthStealManager(RunesAltarV plugin) {
      this.plugin = plugin;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void applySteal(final Player attacker, final LivingEntity victim) {
      double amount = this.plugin.getWeaponsConfig().getDouble("nightpiercer.bite.health-steal", 4.0);
      int durationTicks = this.plugin.getWeaponsConfig().getInt("nightpiercer.bite.duration", 10) * 20;
      this.modifyMaxHealth(attacker, amount);
      if (victim instanceof Player targetPlayer) {
         this.modifyMaxHealth(targetPlayer, -amount);
      }

      (new BukkitRunnable() {
         @Override
         public void run() {
            NightHealthStealManager.this.resetHealth(attacker);
            if (victim instanceof Player targetPlayer) {
               NightHealthStealManager.this.resetHealth(targetPlayer);
            }
         }
      }).runTaskLater(this.plugin, (long)durationTicks);
   }

   private void modifyMaxHealth(Player p, double delta) {
      if (p != null && p.isOnline()) {
         AttributeInstance maxHealthAttr = p.getAttribute(Attribute.MAX_HEALTH);
         if (maxHealthAttr != null) {
            double currentBase = maxHealthAttr.getBaseValue();
            if (!this.baseHealth.containsKey(p.getUniqueId())) {
               this.baseHealth.put(p.getUniqueId(), currentBase);
            }

            double newHealth = currentBase + delta;
            if (newHealth < 1.0) {
               newHealth = 1.0;
            }

            maxHealthAttr.setBaseValue(newHealth);
            if (delta > 0.0) {
               p.setHealth(Math.min(p.getHealth() + delta, maxHealthAttr.getValue()));
            }
         }
      }
   }

   private void resetHealth(Player p) {
      if (p != null && this.baseHealth.containsKey(p.getUniqueId())) {
         AttributeInstance maxHealthAttr = p.getAttribute(Attribute.MAX_HEALTH);
         if (maxHealthAttr != null) {
            double original = this.baseHealth.get(p.getUniqueId());
            maxHealthAttr.setBaseValue(original);
            if (p.getHealth() > original) {
               p.setHealth(original);
            }
         }

         this.baseHealth.remove(p.getUniqueId());
      }
   }

   public void revertAll() {
      for (UUID id : new HashSet<>(this.baseHealth.keySet())) {
         Player p = Bukkit.getPlayer(id);
         if (p != null) {
            this.resetHealth(p);
         }
      }

      this.baseHealth.clear();
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent e) {
      this.resetHealth(e.getPlayer());
   }

   @EventHandler
   public void onDeath(EntityDeathEvent e) {
      if (e.getEntity() instanceof Player p) {
         this.resetHealth(p);
      }
   }
}

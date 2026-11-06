package dev.vaileks.runesaltarv.weapons.hyperion.listeners;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.weapons.hyperion.managers.HyperionCooldownManager;
import dev.vaileks.runesaltarv.weapons.hyperion.tasks.HyperionTasks;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class HyperionListener implements Listener {
   private boolean isHyperion(ItemStack item) {
      return item != null && item.hasItemMeta()
         ? item.getItemMeta().getPersistentDataContainer().has(RunesAltarV.getInstance().getHyperionKey(), PersistentDataType.BYTE)
         : false;
   }

   @EventHandler
   public void onDamage(EntityDamageEvent e) {
      if (e.getEntity() instanceof Player p) {
         if (this.isHyperion(p.getInventory().getItemInMainHand())) {
            DamageCause c = e.getCause();
            if (c == DamageCause.FIRE || c == DamageCause.FIRE_TICK || c == DamageCause.LAVA || c == DamageCause.HOT_FLOOR) {
               e.setCancelled(true);
               p.setFireTicks(0);
            }
         }
      }
   }

   @EventHandler
   public void onHit(EntityDamageByEntityEvent e) {
      if (e.getCause() == DamageCause.ENTITY_ATTACK || e.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
         if (e.getDamager() instanceof Player p && e.getEntity() instanceof LivingEntity victim && this.isHyperion(p.getInventory().getItemInMainHand())) {
            this.playHyperionSweep(p, victim);
         }
      }
   }

   private void playHyperionSweep(Player p, LivingEntity victim) {
      Vector dir = p.getLocation().getDirection().normalize();
      Location center = victim.getLocation().add(0.0, 1.0, 0.0).subtract(dir.clone().multiply(0.8));
      center.getWorld().playSound(center, Sound.ITEM_TRIDENT_THROW, 1.0F, 0.6F);
      Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
      if (right.lengthSquared() == 0.0) {
         right.copy(new Vector(1, 0, 0));
      }

      Vector up = right.clone().crossProduct(dir).normalize();
      double rotAngle = Math.random() * Math.PI;
      double cos = Math.cos(rotAngle);
      double sin = Math.sin(rotAngle);

      for (double i = -1.4; i <= 1.4; i += 0.05) {
         double x = i * cos;
         double y = i * sin;
         double z = -(i * i) * 0.25;
         Vector point = right.clone().multiply(x).add(up.clone().multiply(y)).add(dir.clone().multiply(z));
         point.multiply(1.15);
         Location pLoc = center.clone().add(point);
         if (Math.random() > 0.5) {
            center.getWorld().spawnParticle(Particle.FLAME, pLoc, 1, 0.0, 0.0, 0.0, 0.0);
         }

         center.getWorld().spawnParticle(Particle.CRIT, pLoc, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   @EventHandler
   public void onSwap(PlayerSwapHandItemsEvent e) {
      if (!e.isCancelled()) {
         Player p = e.getPlayer();
         if (this.isHyperion(p.getInventory().getItemInMainHand())) {
            e.setCancelled(true);
            if (p.isSneaking()) {
               if (HyperionCooldownManager.isOnScorchingCooldown(p) || HyperionTasks.ScorchingChargeTask.isCharging(p)) {
                  return;
               }

               new HyperionTasks.ScorchingChargeTask(p).runTaskTimer(RunesAltarV.getInstance(), 0L, 1L);
            } else {
               if (HyperionCooldownManager.isOnHolyLanceCooldown(p)) {
                  return;
               }

               HyperionCooldownManager.setHolyLanceCooldown(p);
               new HyperionTasks.HolyLanceTask(p).runTaskTimer(RunesAltarV.getInstance(), 0L, 1L);
            }
         }
      }
   }
}

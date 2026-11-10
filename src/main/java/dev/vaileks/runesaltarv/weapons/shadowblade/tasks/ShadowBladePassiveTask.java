package dev.vaileks.runesaltarv.weapons.shadowblade.tasks;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.utils.SpeedBuffUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import dev.vaileks.runesaltarv.weapons.shadowblade.listeners.ShadowBladeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ShadowBladePassiveTask extends BukkitRunnable {
   private final Map<UUID, PotionEffect> savedSpeed = new HashMap<>();
   private final Map<UUID, Long> savedSpeedTime = new HashMap<>();
   private final Map<UUID, Boolean> wasHolding = new HashMap<>();
   private final DustOptions subtleBlackDust = new DustOptions(Color.fromRGB(15, 15, 20), 1.35F);

   @Override
   public void run() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         ItemStack item = p.getInventory().getItemInMainHand();
         boolean isHolding = ShadowBladeListener.isShadowBlade(item);
         PotionEffect curSpeed = p.getPotionEffect(PotionEffectType.SPEED);
         boolean isWeaponSpeed = curSpeed != null && curSpeed.getAmplifier() == 1 && curSpeed.getDuration() <= 80;
         if (isHolding) {
            if (!this.wasHolding.getOrDefault(p.getUniqueId(), false) && curSpeed != null && !isWeaponSpeed) {
               this.savedSpeed.put(p.getUniqueId(), curSpeed);
               this.savedSpeedTime.put(p.getUniqueId(), System.currentTimeMillis());
            }

            if (curSpeed == null || isWeaponSpeed || curSpeed.getAmplifier() <= 1) {
               p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, AltarTuning.integer("shadow_blade","tuning.shadow-blade-passive-task.effects.speed.duration-ticks",40,1,72000), AltarTuning.integer("shadow_blade","tuning.shadow-blade-passive-task.effects.speed.amplifier",1,0,10), false, false, true));
            }

            this.wasHolding.put(p.getUniqueId(), true);
            Location playerLoc = p.getLocation();
            Vector facing = playerLoc.getDirection().setY(0);
            if (facing.lengthSquared() < 0.001) {
               facing = new Vector(0, 0, 1);
            } else {
               facing.normalize();
            }

            Location trailLoc = playerLoc.clone().add(0.0, 0.12, 0.0).subtract(facing.clone().multiply(0.4));
            p.getWorld().spawnParticle(Particle.DUST, trailLoc,AltarTuning.particles("shadow_blade",8), 0.22, 0.1, 0.22, 0.0, this.subtleBlackDust);
         } else {
            boolean heldBefore = this.wasHolding.getOrDefault(p.getUniqueId(), false);
            if (heldBefore) {
               ItemStack currentHeld = p.getInventory().getItemInMainHand();
               boolean switchedToSpeedWeapon = WeaponFactory.isHoldingPassiveSpeedWeapon(currentHeld);
               if (!switchedToSpeedWeapon && isWeaponSpeed) {
                  p.removePotionEffect(PotionEffectType.SPEED);
               }

               if (!switchedToSpeedWeapon && this.savedSpeed.containsKey(p.getUniqueId())) {
                  PotionEffect old = this.savedSpeed.remove(p.getUniqueId());
                  Long startTime = this.savedSpeedTime.remove(p.getUniqueId());
                  if (old != null && startTime != null) {
                     if (old.getDuration() == -1) {
                        p.addPotionEffect(old);
                     } else {
                        long elapsed = (System.currentTimeMillis() - startTime) / 50L;
                        int rem = old.getDuration() - (int)elapsed;
                        if (rem > 0) {
                           p.addPotionEffect(new PotionEffect(old.getType(), rem, old.getAmplifier(), old.isAmbient(), old.hasParticles(), old.hasIcon()));
                        }
                     }
                  }
               }

               this.wasHolding.put(p.getUniqueId(), false);
            }
         }
      }
   }
}

package dev.vaileks.runesaltarv.weapons.bloodlust.tasks;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.SpeedBuffUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import dev.vaileks.runesaltarv.weapons.bloodlust.listeners.BloodLustListener;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PassiveAuraTask extends BukkitRunnable {
   private final RunesAltarV plugin;
   private final Map<UUID, Integer> trackerCooldowns = new HashMap<>();
   private final Map<UUID, PotionEffect> savedSpeed = new HashMap<>();
   private final Map<UUID, Long> savedSpeedTime = new HashMap<>();
   private final Map<UUID, PotionEffect> savedStrength = new HashMap<>();
   private final Map<UUID, Long> savedStrengthTime = new HashMap<>();
   private final Map<UUID, Boolean> wasHolding = new HashMap<>();

   public PassiveAuraTask(RunesAltarV plugin) {
      this.plugin = plugin;
   }

   @Override
   public void run() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         this.plugin.getBloodAbilityManager().updateHandCheck(p);
         ItemStack item = p.getInventory().getItemInMainHand();
         boolean isHoldingBloodlust = BloodLustListener.isBloodLust(item);
         boolean heldBefore = this.wasHolding.getOrDefault(p.getUniqueId(), false);
         if (isHoldingBloodlust) {
            int kills = BloodLustListener.getKills(item);
            int speedAmp = this.plugin.getWeaponsConfig().getInt("bloodlust.passive.speed-amplifier", 1);
            int strAmp = this.plugin.getWeaponsConfig().getInt("bloodlust.passive.strength-amplifier", 0);
            if (!heldBefore) {
               PotionEffect curSpeed = p.getPotionEffect(PotionEffectType.SPEED);
               if (curSpeed != null && !this.isBloodlustPassiveEffect(curSpeed, speedAmp)) {
                  this.savedSpeed.put(p.getUniqueId(), curSpeed);
                  this.savedSpeedTime.put(p.getUniqueId(), System.currentTimeMillis());
               }

               PotionEffect curStr = p.getPotionEffect(PotionEffectType.STRENGTH);
               if (curStr != null && !this.isBloodlustPassiveEffect(curStr, strAmp)) {
                  this.savedStrength.put(p.getUniqueId(), curStr);
                  this.savedStrengthTime.put(p.getUniqueId(), System.currentTimeMillis());
               }
            }

            if (kills >= AltarTuning.integer("bloodlust","abilities.progression.speed-kills",1,0,1000)) {
               PotionEffect curSpeedx = p.getPotionEffect(PotionEffectType.SPEED);
               if (curSpeedx == null || this.isBloodlustPassiveEffect(curSpeedx, speedAmp) || curSpeedx.getAmplifier() <= speedAmp) {
                  p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, AltarTuning.integer("bloodlust","tuning.passive-aura-task.effects.speed.duration-ticks",40,1,72000), speedAmp, false, false, true));
               }
            }

            if (kills >= AltarTuning.integer("bloodlust","abilities.progression.strength-kills",4,0,1000)) {
               PotionEffect curStr = p.getPotionEffect(PotionEffectType.STRENGTH);
               if (curStr == null || this.isBloodlustPassiveEffect(curStr, strAmp) || curStr.getAmplifier() <= strAmp) {
                  p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, AltarTuning.integer("bloodlust","tuning.passive-aura-task.effects.strength.duration-ticks",40,1,72000), strAmp, false, false, true));
               }
            }

            this.wasHolding.put(p.getUniqueId(), true);
            if (kills >= AltarTuning.integer("bloodlust","abilities.progression.infection-kills",2,0,1000)) {
               int cooldown = this.trackerCooldowns.getOrDefault(p.getUniqueId(), 0);
               if (cooldown > 0) {
                  this.trackerCooldowns.put(p.getUniqueId(), cooldown - 1);
               } else {
                  Player target = this.findNearestTarget(p);
                  if (target != null) {
                     this.plugin.getBloodAbilityManager().launchTrackerProjectile(p, target);
                     this.trackerCooldowns.put(p.getUniqueId(), 30);
                  }
               }
            }
         } else if (heldBefore) {
            PotionEffect activeSpeed = p.getPotionEffect(PotionEffectType.SPEED);
            PotionEffect activeStrength = p.getPotionEffect(PotionEffectType.STRENGTH);
            int speedAmpx = this.plugin.getWeaponsConfig().getInt("bloodlust.passive.speed-amplifier", 1);
            int strAmpx = this.plugin.getWeaponsConfig().getInt("bloodlust.passive.strength-amplifier", 0);
            ItemStack currentHeld = p.getInventory().getItemInMainHand();
            boolean switchedToSpeedWeapon = WeaponFactory.isHoldingPassiveSpeedWeapon(currentHeld);
            if (!switchedToSpeedWeapon && this.isBloodlustPassiveEffect(activeSpeed, speedAmpx)) {
               p.removePotionEffect(PotionEffectType.SPEED);
            }

            if (this.isBloodlustPassiveEffect(activeStrength, strAmpx)) {
               p.removePotionEffect(PotionEffectType.STRENGTH);
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

            if (this.savedStrength.containsKey(p.getUniqueId())) {
               PotionEffect old = this.savedStrength.remove(p.getUniqueId());
               Long startTime = this.savedStrengthTime.remove(p.getUniqueId());
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

   private boolean isBloodlustPassiveEffect(PotionEffect effect, int amplifier) {
      return effect != null && effect.getAmplifier() == amplifier && effect.getDuration() <= 80;
   }

   private Player findNearestTarget(Player p) {
      Player nearest = null;
      double minDist = AltarTuning.number("bloodlust","tuning.passive-aura-task.min-dist",80.0,0.01,128);

      for (Player other : Bukkit.getOnlinePlayers()) {
         if (!other.equals(p)
            && other.getGameMode() != GameMode.SPECTATOR
            && !BloodLustListener.isBloodLust(other.getInventory().getItemInMainHand())
            && other.getWorld().equals(p.getWorld())) {
            double dist = p.getLocation().distance(other.getLocation());
            if (dist < minDist) {
               minDist = dist;
               nearest = other;
            }
         }
      }

      return nearest;
   }
}

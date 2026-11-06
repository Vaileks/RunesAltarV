package dev.vaileks.runesaltarv.weapons.knightfall.tasks;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.SpeedBuffUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class KnightfallPassiveTask extends BukkitRunnable {
   private final RunesAltarV plugin;
   private final Map<UUID, PotionEffect> savedSpeed = new HashMap<>();
   private final Map<UUID, Long> savedSpeedTime = new HashMap<>();
   private final Map<UUID, Boolean> wasHolding = new HashMap<>();

   public KnightfallPassiveTask(RunesAltarV plugin) {
      this.plugin = plugin;
   }

   @Override
   public void run() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         ItemStack item = p.getInventory().getItemInMainHand();
         boolean isHoldingKnightfall = false;
         if (WeaponFactory.isAltarWeapon(item, "knightfall")
            && item.getItemMeta().getPersistentDataContainer().has(this.plugin.getKillsKey(), PersistentDataType.INTEGER)) {
            int kills = item.getItemMeta().getPersistentDataContainer().get(this.plugin.getKillsKey(), PersistentDataType.INTEGER);
            int cmd = item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : 1;
            boolean hasDensity = item.getItemMeta().hasEnchant(Enchantment.DENSITY);
            int tier = item.getItemMeta()
               .getPersistentDataContainer()
               .getOrDefault(
                  this.plugin.getKnightfallTierKey(),
                  PersistentDataType.INTEGER,
                  Integer.valueOf(cmd >= 3003 || cmd == 3 ? 3 : (hasDensity ? 2 : (cmd < 3002 && cmd != 2 ? 0 : 1)))
               );
            boolean needsUpgrade = kills >= 4 && tier < 1 || kills >= 8 && tier < 2 || kills >= 10 && tier < 3;
            if (needsUpgrade) {
               TextUtils.sendActionBar(
                  p, TextUtils.legacy(this.plugin.tr("&aНажмите &2[Смена руки] &aчтобы улучшить оружие.", "&aPress &2[OffHand] &ato upgrade the weapon."))
               );
            }

            if (kills >= 6) {
               isHoldingKnightfall = true;
            }
         }

         PotionEffect curSpeed = p.getPotionEffect(PotionEffectType.SPEED);
         boolean isWeaponSpeed = curSpeed != null && curSpeed.getAmplifier() == 1 && curSpeed.getDuration() <= 80;
         if (isHoldingKnightfall) {
            if (!this.wasHolding.getOrDefault(p.getUniqueId(), false) && curSpeed != null && !isWeaponSpeed) {
               this.savedSpeed.put(p.getUniqueId(), curSpeed);
               this.savedSpeedTime.put(p.getUniqueId(), System.currentTimeMillis());
            }

            if (curSpeed == null || isWeaponSpeed || curSpeed.getAmplifier() <= 1) {
               p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, true));
            }

            this.wasHolding.put(p.getUniqueId(), true);
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

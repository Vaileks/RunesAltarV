package dev.vaileks.runesaltarv.weapons.nightpiercer.tasks;

import dev.vaileks.runesaltarv.utils.WeaponFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class NightPassiveTask extends BukkitRunnable {
   private boolean isNightpiercer(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "nightpiercer");
   }

   @Override
   public void run() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         if (this.isNightpiercer(p.getInventory().getItemInMainHand())) {
            long time = p.getWorld().getTime();
            if (time >= 13000L && time <= 23000L) {
               p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 50, 0, false, false, false));
            }
         }
      }
   }
}

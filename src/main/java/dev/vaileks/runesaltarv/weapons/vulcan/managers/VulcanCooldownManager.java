package dev.vaileks.runesaltarv.weapons.vulcan.managers;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.TextUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class VulcanCooldownManager {
   private static final Map<UUID, Long> cooldownEnds = new HashMap<>();
   private static final Map<UUID, BossBar> activeBars = new HashMap<>();

   public static boolean isOnCooldown(Player player) {
      return remainingMillis(player) > 0;
   }

   public static long remainingMillis(Player player) {
      return Math.max(0L, cooldownEnds.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis());
   }

   public static void startCooldown(final Player player, final int seconds) {
      final UUID uuid = player.getUniqueId();
      if (seconds <= 0) return;
      cooldownEnds.put(uuid, System.currentTimeMillis() + seconds * 1000L);
      dev.vaileks.runesaltarv.integration.AltarCooldowns.observe(player,"vulcan_crossbow","magma",()->remainingMillis(player),seconds*1000L);
      String title = RunesAltarV.getInstance().tr("§6§lГнев Вулкана", "§6§lᴠᴜʟᴄᴀɴ's ᴡʀᴀᴛʜ");
      final BossBar bossBar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(ChatColor.YELLOW + ChatColor.stripColor(title), BarColor.YELLOW, BarStyle.SOLID);
      bossBar.addPlayer(player);
      BossBar oldBar = activeBars.put(uuid, bossBar);
      if (oldBar != null) oldBar.removeAll();
      (new BukkitRunnable() {

         @Override
         public void run() {
            if (activeBars.get(uuid) != bossBar) {
               bossBar.removeAll();
               this.cancel();
            } else if (!player.isOnline()) {
               bossBar.removeAll();
               VulcanCooldownManager.activeBars.remove(uuid);
               this.cancel();
            } else {
               double progress = remainingMillis(player) / (seconds * 1000.0);
               if (progress <= 0.0) {
                  bossBar.removeAll();
                  VulcanCooldownManager.activeBars.remove(uuid);
                  this.cancel();
               } else {
                  bossBar.setProgress(Math.min(1.0, Math.max(0.0, progress)));
               }
            }
         }
      }).runTaskTimer(RunesAltarV.getInstance(), 0L, 1L);
   }

   public static void clearAllBars() {
      for (BossBar bossBar : activeBars.values()) {
         bossBar.removeAll();
      }

      activeBars.clear();
      cooldownEnds.clear();
   }

   public static void resetPlayer(Player player) {
      cooldownEnds.remove(player.getUniqueId());
      BossBar bossBar = activeBars.remove(player.getUniqueId());
      if (bossBar != null) {
         bossBar.removeAll();
      }
   }
}

package dev.vaileks.runesaltarv.weapons.pureblade.managers;

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

public class PureBossBarManager {
   private final RunesAltarV plugin;
   private final Map<UUID, Map<String, Long>> cooldownEnds = new HashMap<>();
   private final Map<UUID, Map<String, BossBar>> activeBars = new HashMap<>();

   public PureBossBarManager(RunesAltarV plugin) {
      this.plugin = plugin;
   }

   public boolean isOnCooldown(Player player, String ability) {
      return this.remainingMillis(player, ability) > 0;
   }

   public long remainingMillis(Player player, String ability) {
      return Math.max(0L, this.cooldownEnds.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(ability, 0L) - System.currentTimeMillis());
   }

   public void setCooldown(final Player player, final String ability, String title, final int seconds) {
      final UUID uuid = player.getUniqueId();
      if (seconds <= 0) return;
      this.cooldownEnds.computeIfAbsent(uuid, key -> new HashMap<>()).put(ability, System.currentTimeMillis() + seconds * 1000L);
      dev.vaileks.runesaltarv.integration.AltarCooldowns.observe(player,"pure_blade",ability,()->this.remainingMillis(player,ability),seconds*1000L);
      this.activeBars.putIfAbsent(uuid, new HashMap<>());
      final BossBar bossBar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(ChatColor.YELLOW + ChatColor.stripColor(title), BarColor.YELLOW, BarStyle.SOLID);
      bossBar.addPlayer(player);
      BossBar oldBar = this.activeBars.get(uuid).put(ability, bossBar);
      if (oldBar != null) oldBar.removeAll();
      (new BukkitRunnable() {

         @Override
         public void run() {
            if (PureBossBarManager.this.activeBars.getOrDefault(uuid, Map.of()).get(ability) != bossBar) {
               bossBar.removeAll();
               this.cancel();
            } else if (!player.isOnline()) {
               this.removeBarSafely();
               bossBar.removeAll();
               this.cancel();
            } else {
               double progress = PureBossBarManager.this.remainingMillis(player, ability) / (seconds * 1000.0);
               if (progress <= 0.0) {
                  bossBar.removeAll();
                  this.removeBarSafely();
                  this.cancel();
               } else {
                  bossBar.setProgress(Math.min(1.0, Math.max(0.0, progress)));
               }
            }
         }

         private void removeBarSafely() {
            Map<String, BossBar> playerBars = PureBossBarManager.this.activeBars.get(uuid);
            if (playerBars != null) {
               playerBars.remove(ability);
               if (playerBars.isEmpty()) {
                  PureBossBarManager.this.activeBars.remove(uuid);
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   public void clearAll() {
      for (Map<String, BossBar> bars : this.activeBars.values()) {
         for (BossBar bar : bars.values()) {
            bar.removeAll();
         }
      }

      this.activeBars.clear();
      this.cooldownEnds.clear();
   }

   public void resetPlayer(Player player) {
      this.cooldownEnds.remove(player.getUniqueId());
      Map<String, BossBar> bars = this.activeBars.remove(player.getUniqueId());
      if (bars != null) {
         for (BossBar bar : bars.values()) {
            bar.removeAll();
         }
      }
   }
}

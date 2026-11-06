package dev.vaileks.runesaltarv.weapons.frostscythe.managers;

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

public class FrostBossBarManager {
   private final RunesAltarV plugin;
   private final Map<UUID, Map<String, BossBar>> activeBars = new HashMap<>();
   private final Map<UUID, Map<String, Long>> cooldownEnds = new HashMap<>();

   public FrostBossBarManager(RunesAltarV plugin) {
      this.plugin = plugin;
   }

   public boolean isOnCooldown(Player player, String ability) {
      if (!this.cooldownEnds.containsKey(player.getUniqueId())) {
         return false;
      } else {
         Long end = this.cooldownEnds.get(player.getUniqueId()).get(ability);
         return end != null && System.currentTimeMillis() < end;
      }
   }

   public void setCooldown(final Player player, final String ability, String title, final int seconds) {
      dev.vaileks.runesaltarv.integration.AltarCooldowns.observe(player,"frost_scythe",ability,()->this.cooldownEnds.getOrDefault(player.getUniqueId(),java.util.Map.of()).getOrDefault(ability,0L)-System.currentTimeMillis(),seconds*1000L);
      final UUID uuid = player.getUniqueId();
      this.cooldownEnds.computeIfAbsent(uuid, key -> new HashMap<>()).put(ability, System.currentTimeMillis() + (long)seconds * 1000L);
      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(ChatColor.YELLOW + ChatColor.stripColor(this.resolveTitle(ability, title)), BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(player);
      this.activeBars.computeIfAbsent(uuid, key -> new HashMap<>()).put(ability, bar);
      (new BukkitRunnable() {
            int ticksLeft = seconds * 20;
            final int totalTicks = seconds * 20;

            @Override
            public void run() {
               if (player.isOnline()
                  && this.ticksLeft > 0
                  && FrostBossBarManager.this.activeBars.containsKey(uuid)
                  && FrostBossBarManager.this.activeBars.get(uuid).get(ability) == bar) {
                  bar.setProgress((double)this.ticksLeft / (double)this.totalTicks);
                  this.ticksLeft--;
               } else {
                  bar.removePlayer(player);
                  bar.removeAll();
                  Map<String, BossBar> playerBars = FrostBossBarManager.this.activeBars.get(uuid);
                  if (playerBars != null) {
                     playerBars.remove(ability);
                     if (playerBars.isEmpty()) {
                        FrostBossBarManager.this.activeBars.remove(uuid);
                     }
                  }

                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   public void resetPlayer(Player player) {
      UUID uuid = player.getUniqueId();
      this.cooldownEnds.remove(uuid);
      Map<String, BossBar> bars = this.activeBars.remove(uuid);
      if (bars != null) {
         for (BossBar bar : bars.values()) {
            bar.removeAll();
         }
      }
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

   private String resolveTitle(String ability, String fallback) {
      return switch (ability) {
         case "ScytheThrow" -> this.plugin.tr("§b§lброᴄоᴋ ᴋоᴄы", "§b§lsᴄʏᴛʜᴇ ᴛʜʀᴏᴡ");
         case "CommandOfIce" -> this.plugin.tr("§b§lпоʙᴇлᴇниᴇ льдᴀ", "§b§lᴄᴏᴍᴍᴀɴᴅ ᴏғ ɪᴄᴇ");
         default -> fallback;
      };
   }
}

package dev.vaileks.runesaltarv.weapons.earthgauntlet.managers;
import dev.vaileks.runesaltarv.integration.AltarTuning;

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

public class EarthBossBarManager {
   private final RunesAltarV plugin;
   private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
   private final Map<UUID, Map<String, BossBar>> cooldownBars = new HashMap<>();
   private final Map<UUID, Map<String, BossBar>> activeBars = new HashMap<>();
   private final Map<UUID, BossBar> debuffBars = new HashMap<>();

   public EarthBossBarManager(RunesAltarV plugin) {
      this.plugin = plugin;
   }

   public boolean isOnCooldown(Player player, String ability) {
      if (!this.cooldowns.containsKey(player.getUniqueId())) {
         return false;
      } else if (!this.cooldowns.get(player.getUniqueId()).containsKey(ability)) {
         return false;
      } else {
         long expireTime = this.cooldowns.get(player.getUniqueId()).get(ability);
         return System.currentTimeMillis() < expireTime;
      }
   }

   public void setCooldown(final Player player, final String ability, String name, final int seconds) {
      dev.vaileks.runesaltarv.integration.AltarCooldowns.observe(player,"earth_gauntlet",ability,()->this.cooldowns.getOrDefault(player.getUniqueId(),java.util.Map.of()).getOrDefault(ability,0L)-System.currentTimeMillis(),seconds*1000L);
      final long expireTime = System.currentTimeMillis() + (long)seconds * 1000L;
      this.cooldowns.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).put(ability, expireTime);
      BossBar oldBar = this.cooldownBars.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).remove(ability);
      if (oldBar != null) {
         oldBar.removeAll();
      }

      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(ChatColor.YELLOW + ChatColor.stripColor(this.resolveCooldownTitle(ability, name)), BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(player);
      this.cooldownBars.get(player.getUniqueId()).put(ability, bar);
      (new BukkitRunnable() {
            @Override
            public void run() {
               if (player.isOnline()
                  && EarthBossBarManager.this.cooldownBars.containsKey(player.getUniqueId())
                  && EarthBossBarManager.this.cooldownBars.get(player.getUniqueId()).get(ability) == bar) {
                  long timeLeft = expireTime - System.currentTimeMillis();
                  if (timeLeft <= 0L) {
                     bar.removeAll();
                     Map<String, BossBar> pBars = EarthBossBarManager.this.cooldownBars.get(player.getUniqueId());
                     if (pBars != null) {
                        pBars.remove(ability);
                        if (pBars.isEmpty()) {
                           EarthBossBarManager.this.cooldownBars.remove(player.getUniqueId());
                        }
                     }

                     Map<String, Long> pCooldowns = EarthBossBarManager.this.cooldowns.get(player.getUniqueId());
                     if (pCooldowns != null) {
                        pCooldowns.remove(ability);
                        if (pCooldowns.isEmpty()) {
                           EarthBossBarManager.this.cooldowns.remove(player.getUniqueId());
                        }
                     }

                     this.cancel();
                  } else {
                     double progress = (double)timeLeft / (double)((long)seconds * 1000L);
                     bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                  }
               } else {
                  bar.removeAll();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 2L);
   }

   public void setActiveBar(final Player player, final String id, String title, final int seconds) {
      this.removeActiveBar(player, id);
      final BossBar bar = TextUtils.bossBar(title, BarColor.GREEN, BarStyle.SOLID);
      bar.addPlayer(player);
      this.activeBars.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).put(id, bar);
      final long expireTime = System.currentTimeMillis() + (long)seconds * 1000L;
      (new BukkitRunnable() {
         @Override
         public void run() {
            if (player.isOnline() && EarthBossBarManager.this.activeBars.getOrDefault(player.getUniqueId(), new HashMap<>()).containsKey(id)) {
               long timeLeft = expireTime - System.currentTimeMillis();
               if (timeLeft <= 0L) {
                  EarthBossBarManager.this.removeActiveBar(player, id);
                  this.cancel();
               } else {
                  double progress = (double)timeLeft / (double)((long)seconds * 1000L);
                  bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
               }
            } else {
               bar.removeAll();
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 2L);
   }

   public void removeActiveBar(Player player, String id) {
      if (this.activeBars.containsKey(player.getUniqueId())) {
         BossBar bar = this.activeBars.get(player.getUniqueId()).remove(id);
         if (bar != null) {
            bar.removeAll();
         }
      }
   }

   public void setDebuffBar(final Player player, String title, final int seconds) {
      this.removeDebuffBar(player);
      final BossBar bar = TextUtils.bossBar(title, BarColor.RED, BarStyle.SOLID);
      bar.addPlayer(player);
      this.debuffBars.put(player.getUniqueId(), bar);
      final long expireTime = System.currentTimeMillis() + (long)seconds * 1000L;
      (new BukkitRunnable() {
            @Override
            public void run() {
               if (player.isOnline()
                  && EarthBossBarManager.this.debuffBars.containsKey(player.getUniqueId())
                  && EarthBossBarManager.this.debuffBars.get(player.getUniqueId()).equals(bar)) {
                  long timeLeft = expireTime - System.currentTimeMillis();
                  if (timeLeft <= 0L) {
                     EarthBossBarManager.this.removeDebuffBar(player);
                     this.cancel();
                  } else {
                     double progress = (double)timeLeft / (double)((long)seconds * 1000L);
                     bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                  }
               } else {
                  bar.removeAll();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 2L);
   }

   public void removeDebuffBar(Player player) {
      BossBar bar = this.debuffBars.remove(player.getUniqueId());
      if (bar != null) {
         bar.removeAll();
      }
   }

   public void resetPlayer(Player player) {
      UUID uuid = player.getUniqueId();
      this.cooldowns.remove(uuid);
      Map<String, BossBar> cdBars = this.cooldownBars.remove(uuid);
      if (cdBars != null) {
         for (BossBar bar : cdBars.values()) {
            bar.removeAll();
         }
      }

      Map<String, BossBar> bars = this.activeBars.remove(uuid);
      if (bars != null) {
         for (BossBar bar : bars.values()) {
            bar.removeAll();
         }
      }

      this.removeDebuffBar(player);
   }

   public void clearAll() {
      for (Map<String, BossBar> map : this.cooldownBars.values()) {
         for (BossBar bar : map.values()) {
            bar.removeAll();
         }
      }

      this.cooldownBars.clear();

      for (Map<String, BossBar> map : this.activeBars.values()) {
         for (BossBar bar : map.values()) {
            bar.removeAll();
         }
      }

      this.activeBars.clear();

      for (BossBar bar : this.debuffBars.values()) {
         bar.removeAll();
      }

      this.debuffBars.clear();
      this.cooldowns.clear();
   }

   private String resolveCooldownTitle(String ability, String fallback) {
      return switch (ability) {
         case "MeteorStrike" -> AltarTuning.text("earth_gauntlet","messages.earth-boss-bar-manager.message-1","§e§lмᴇᴛᴇориᴛный удᴀр","§e§lᴍᴇᴛᴇᴏʀ sᴛʀɪᴋᴇ");
         case "Mudslide" -> AltarTuning.text("earth_gauntlet","messages.earth-boss-bar-manager.message-2","§e§lоползᴇнь","§e§lᴍᴜᴅsʟɪᴅᴇ");
         default -> fallback;
      };
   }
}

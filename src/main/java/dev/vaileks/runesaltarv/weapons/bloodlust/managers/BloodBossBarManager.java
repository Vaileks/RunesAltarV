package dev.vaileks.runesaltarv.weapons.bloodlust.managers;

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

public class BloodBossBarManager {
   private final RunesAltarV plugin;
   private final Map<UUID, Map<String, BossBar>> activeBars = new HashMap<>();
   private final Map<UUID, Map<String, BossBar>> debuffBars = new HashMap<>();
   private final Map<UUID, Map<String, Long>> cooldownEnds = new HashMap<>();

   public BloodBossBarManager(RunesAltarV plugin) {
      this.plugin = plugin;
   }

   public boolean isOnCooldown(Player p, String ability) {
      if (!this.cooldownEnds.containsKey(p.getUniqueId())) {
         return false;
      } else {
         Long end = this.cooldownEnds.get(p.getUniqueId()).get(ability);
         return end != null && System.currentTimeMillis() < end;
      }
   }

   public void setCooldown(final Player p, final String ability, String title, final int seconds) {
      dev.vaileks.runesaltarv.integration.AltarCooldowns.observe(p,"bloodlust",ability,()->this.cooldownEnds.getOrDefault(p.getUniqueId(),java.util.Map.of()).getOrDefault(ability,0L)-System.currentTimeMillis(),seconds*1000L);
      final UUID uuid = p.getUniqueId();
      this.cooldownEnds.computeIfAbsent(uuid, k -> new HashMap<>()).put(ability, System.currentTimeMillis() + (long)seconds * 1000L);
      this.removeBar(this.activeBars, uuid, ability);
      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(this.yellowTitle(this.resolveCooldownTitle(ability, title)), BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(p);
      this.activeBars.computeIfAbsent(uuid, k -> new HashMap<>()).put(ability, bar);
      (new BukkitRunnable() {
         int ticksLeft = seconds * 20;
         final int totalTicks = seconds * 20;

         @Override
         public void run() {
            if (p.isOnline() && this.ticksLeft > 0) {
               bar.setProgress((double)this.ticksLeft / (double)this.totalTicks);
               this.ticksLeft--;
            } else {
               BloodBossBarManager.this.removeBar(BloodBossBarManager.this.activeBars, uuid, ability);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   public void setDebuffBar(Player p, String id, String title, int totalHits) {
      UUID uuid = p.getUniqueId();
      this.removeBar(this.debuffBars, uuid, id);
      BossBar bar = TextUtils.bossBar(this.resolveDebuffTitle(id, title), BarColor.RED, BarStyle.SEGMENTED_10);
      bar.addPlayer(p);
      bar.setProgress(1.0);
      this.debuffBars.computeIfAbsent(uuid, k -> new HashMap<>()).put(id, bar);
   }

   public void updateProgress(Player p, String id, double progress) {
      Map<String, BossBar> playerBars = this.debuffBars.get(p.getUniqueId());
      if (playerBars != null) {
         BossBar bar = playerBars.get(id);
         if (bar != null) {
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
         }
      }
   }

   public void removeDebuffBar(Player p, String id) {
      this.removeBar(this.debuffBars, p.getUniqueId(), id);
   }

   public void clearAll() {
      this.clearMap(this.activeBars);
      this.clearMap(this.debuffBars);
      this.cooldownEnds.clear();
   }

   public void resetPlayer(Player player) {
      UUID uuid = player.getUniqueId();
      this.cooldownEnds.remove(uuid);
      this.clearBars(this.activeBars.remove(uuid));
      this.clearBars(this.debuffBars.remove(uuid));
   }

   private void clearMap(Map<UUID, Map<String, BossBar>> source) {
      for (Map<String, BossBar> map : source.values()) {
         for (BossBar bar : map.values()) {
            bar.removeAll();
         }
      }

      source.clear();
   }

   private void clearBars(Map<String, BossBar> bars) {
      if (bars != null) {
         for (BossBar bar : bars.values()) {
            bar.removeAll();
         }
      }
   }

   private void removeBar(Map<UUID, Map<String, BossBar>> source, UUID uuid, String id) {
      Map<String, BossBar> playerBars = source.get(uuid);
      if (playerBars != null) {
         BossBar old = playerBars.remove(id);
         if (old != null) {
            old.removeAll();
         }

         if (playerBars.isEmpty()) {
            source.remove(uuid);
         }
      }
   }

   private String yellowTitle(String title) {
      String translated = ChatColor.translateAlternateColorCodes('&', title == null ? "" : title);
      return ChatColor.YELLOW + ChatColor.stripColor(translated);
   }

   private String resolveCooldownTitle(String ability, String fallback) {
      return switch (ability) {
         case "Infection" -> this.plugin.tr("§4§lинɸᴇᴋция", "§4§lɪɴꜰᴇᴄᴛɪᴏɴ");
         case "BloodTrail" -> this.plugin.tr("§4§lᴋроʙᴀʙый ᴄлᴇд", "§4§lʙʟᴏᴏᴅ ᴛʀᴀɪʟ");
         case "BloodHook" -> this.plugin.tr("§4§lᴋроʙᴀʙый ᴋрюᴋ", "§4§lʙʟᴏᴏᴅ ʜᴏᴏᴋ");
         default -> fallback;
      };
   }

   private String resolveDebuffTitle(String id, String fallback) {
      return "BloodInfection".equals(id) ? this.plugin.tr("§4§l!ᴋроʙоᴛᴇчᴇниᴇ!", "§4§l!ʙʟᴇᴇᴅɪɴɢ!") : fallback;
   }
}

package dev.vaileks.runesaltarv.weapons.boneblade.managers;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.TextUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BoneCooldownManager {
   private final RunesAltarV plugin;
   private final Map<UUID, Long> dashCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("bone_blade","dash");
   private final Map<UUID, Long> cageCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("bone_blade","cage");
   private final Map<UUID, BossBar> dashBars = new HashMap<>();
   private final Map<UUID, BossBar> cageBars = new HashMap<>();

   public BoneCooldownManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.startUpdateTask();
   }

   public boolean isOnDashCooldown(Player player) {
      return this.dashCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
   }

   public boolean isOnCageCooldown(Player player) {
      return this.cageCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
   }

   public void setDashCooldown(Player player, int seconds) {
      this.dashCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (long)seconds * 1000L);
      if (!this.dashBars.containsKey(player.getUniqueId())) {
         BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar("§e" + AltarTuning.text("bone_blade","messages.bone-cooldown-manager.message-1","Скелетный прыжок","Sᴋᴇʟᴇᴛᴏɴ ʟᴇᴀᴘ"), BarColor.YELLOW, BarStyle.SOLID);
         bar.addPlayer(player);
         this.dashBars.put(player.getUniqueId(), bar);
      }
   }

   public void setCageCooldown(Player player, int seconds) {
      this.cageCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (long)seconds * 1000L);
      if (!this.cageBars.containsKey(player.getUniqueId())) {
         BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar("§e" + AltarTuning.text("bone_blade","messages.bone-cooldown-manager.message-2","Костяная клетка","Bᴏɴᴇ ᴄᴀɢᴇ"), BarColor.YELLOW, BarStyle.SOLID);
         bar.addPlayer(player);
         this.cageBars.put(player.getUniqueId(), bar);
      }
   }

   private void startUpdateTask() {
      (new BukkitRunnable() {
         @Override
         public void run() {
            long now = System.currentTimeMillis();
            BoneCooldownManager.this.dashBars.entrySet().removeIf(entry -> {
               UUID uuid = entry.getKey();
               BossBar bar = entry.getValue();
               long expire = BoneCooldownManager.this.dashCooldowns.getOrDefault(uuid, 0L);
               if (now >= expire) {
                  bar.removeAll();
                  return true;
               } else {
                  double total = (double)BoneCooldownManager.this.plugin.getWeaponsConfig().getInt("bone-blade.dash.cooldown", 10) * 1000.0;
                  bar.setProgress(Math.max(0.0, Math.min(1.0, (double)(expire - now) / total)));
                  return false;
               }
            });
            BoneCooldownManager.this.cageBars.entrySet().removeIf(entry -> {
               UUID uuid = entry.getKey();
               BossBar bar = entry.getValue();
               long expire = BoneCooldownManager.this.cageCooldowns.getOrDefault(uuid, 0L);
               if (now >= expire) {
                  bar.removeAll();
                  return true;
               } else {
                  double total = (double)BoneCooldownManager.this.plugin.getWeaponsConfig().getInt("bone-blade.cage.cooldown", 35) * 1000.0;
                  bar.setProgress(Math.max(0.0, Math.min(1.0, (double)(expire - now) / total)));
                  return false;
               }
            });
         }
      }).runTaskTimer(this.plugin, 0L, 2L);
   }

   public void removeAllBars() {
      this.dashBars.values().forEach(BossBar::removeAll);
      this.cageBars.values().forEach(BossBar::removeAll);
      this.dashBars.clear();
      this.cageBars.clear();
   }

   public void resetPlayer(Player player) {
      UUID uuid = player.getUniqueId();
      this.dashCooldowns.remove(uuid);
      this.cageCooldowns.remove(uuid);
      BossBar dashBar = this.dashBars.remove(uuid);
      BossBar cageBar = this.cageBars.remove(uuid);
      if (dashBar != null) {
         dashBar.removeAll();
      }

      if (cageBar != null) {
         cageBar.removeAll();
      }
   }
}

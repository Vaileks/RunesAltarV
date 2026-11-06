package dev.vaileks.runesaltarv.weapons.nightpiercer.managers;

import dev.vaileks.runesaltarv.utils.TextUtils;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class NightBossBarCooldown extends BukkitRunnable {
   private static final Map<String, BossBar> activeBars = new HashMap<>();
   private final BossBar bar;
   private final String key;
   private final int total;
   private int current;

   public NightBossBarCooldown(JavaPlugin plugin, Player p, String title, int sec) {
      this.total = sec * 20;
      this.current = this.total;
      String translated = ChatColor.translateAlternateColorCodes('&', title == null ? "" : title);
      this.bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(ChatColor.YELLOW + ChatColor.stripColor(translated), BarColor.YELLOW, BarStyle.SOLID);
      this.bar.addPlayer(p);
      this.key = p.getUniqueId() + ":" + ChatColor.stripColor(translated);
      BossBar oldBar = activeBars.put(this.key, this.bar);
      if (oldBar != null) {
         oldBar.removeAll();
      }

      this.runTaskTimer(plugin, 0L, 1L);
   }

   @Override
   public void run() {
      if (!activeBars.containsKey(this.key) || activeBars.get(this.key) != this.bar) {
         this.bar.removeAll();
         this.cancel();
      } else if (this.current <= 0) {
         this.bar.removeAll();
         activeBars.remove(this.key, this.bar);
         this.cancel();
      } else {
         this.bar.setProgress((double)this.current / (double)this.total);
         this.current--;
      }
   }

   public void start() {
   }

   public static void resetPlayer(Player player) {
      String prefix = player.getUniqueId() + ":";
      activeBars.entrySet().removeIf(entry -> {
         if (entry.getKey().startsWith(prefix)) {
            entry.getValue().removeAll();
            return true;
         } else {
            return false;
         }
      });
   }

   public static void clearAll() {
      for (BossBar bar : activeBars.values()) {
         bar.removeAll();
      }

      activeBars.clear();
   }
}

package dev.vaileks.runesaltarv.weapons.witherblade.managers;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class WitherManager {
   private final RunesAltarV plugin;
   private final Map<UUID, Integer> dashCharges = new HashMap<>();
   private final Map<UUID, Long> dashCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("wither_blade","dash");
   private final Map<UUID, BossBar> dashBars = new HashMap<>();
   private final Map<UUID, Long> auraCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("wither_blade","aura");
   private final Map<UUID, BossBar> auraBars = new HashMap<>();
   private final Map<UUID, Integer> attackCharges = new HashMap<>();

   public WitherManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.startPassiveTask();
   }

   public int getDashCharges(Player player) {
      return this.dashCharges.getOrDefault(player.getUniqueId(), 3);
   }

   public boolean isOnAuraCooldown(Player player) {
      return this.auraCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
   }

   public void useDashCharge(final Player player) {
      int charges = this.getDashCharges(player);
      if (charges > 0) {
         this.dashCharges.put(player.getUniqueId(), --charges);
         if (charges == 0) {
            final int cooldown = this.plugin.getWeaponsConfig().getInt("wither-blade.dash.cooldown", 30);
            this.dashCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (long)cooldown * 1000L);
            final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(this.yellowTitle(this.plugin.tr("иссушительный прыжок", "ᴡɪᴛʜᴇʀ ʟᴇᴀᴘ")), BarColor.YELLOW, BarStyle.SOLID);
            bar.addPlayer(player);
            this.dashBars.put(player.getUniqueId(), bar);
            (new BukkitRunnable() {
                  double progress = 1.0;
                  final double step = 1.0 / ((double)cooldown * 20.0);

                  @Override
                  public void run() {
                     if (player.isOnline()
                        && WitherManager.this.dashBars.containsKey(player.getUniqueId())
                        && WitherManager.this.dashBars.get(player.getUniqueId()) == bar) {
                        this.progress = this.progress - this.step;
                        if (this.progress <= 0.0) {
                           bar.removeAll();
                           WitherManager.this.dashBars.remove(player.getUniqueId());
                           WitherManager.this.dashCharges.put(player.getUniqueId(), 3);
                           this.cancel();
                        } else {
                           bar.setProgress(Math.max(0.0, this.progress));
                        }
                     } else {
                        bar.removeAll();
                        this.cancel();
                     }
                  }
               })
               .runTaskTimer(this.plugin, 0L, 1L);
         }
      }
   }

   public void setAuraCooldown(final Player player) {
      final int cooldown = this.plugin.getWeaponsConfig().getInt("wither-blade.aura.cooldown", 50);
      this.auraCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (long)cooldown * 1000L);
      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(this.yellowTitle(this.plugin.tr("иссушительное высвобождение", "ᴡɪᴛʜᴇʀ ʀᴇʟᴇᴀsᴇ")), BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(player);
      this.auraBars.put(player.getUniqueId(), bar);
      (new BukkitRunnable() {
            double progress = 1.0;
            final double step = 1.0 / ((double)cooldown * 20.0);

            @Override
            public void run() {
               if (player.isOnline()
                  && WitherManager.this.auraBars.containsKey(player.getUniqueId())
                  && WitherManager.this.auraBars.get(player.getUniqueId()) == bar) {
                  this.progress = this.progress - this.step;
                  if (this.progress <= 0.0) {
                     bar.removeAll();
                     WitherManager.this.auraBars.remove(player.getUniqueId());
                     this.cancel();
                  } else {
                     bar.setProgress(Math.max(0.0, this.progress));
                  }
               } else {
                  bar.removeAll();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   public int getAttackCharge(Player player) {
      return this.attackCharges.getOrDefault(player.getUniqueId(), 0);
   }

   public void addAttackCharge(Player player) {
      int current = this.getAttackCharge(player);
      if (current < 30) {
         this.attackCharges.put(player.getUniqueId(), current + 1);
      }
   }

   public void fillMaxCharge(Player player) {
      this.attackCharges.put(player.getUniqueId(), 30);
   }

   public void resetAttackCharge(Player player) {
      this.attackCharges.put(player.getUniqueId(), 0);
   }

   private void startPassiveTask() {
      (new BukkitRunnable() {
         @Override
         public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
               ItemStack item = player.getInventory().getItemInMainHand();
               if (WeaponFactory.isAltarWeapon(item, "wither_blade")) {
                  int charge = WitherManager.this.getAttackCharge(player);
                  StringBuilder bar = new StringBuilder("§8☠ §7- ");

                  for (int i = 0; i < 30; i++) {
                     bar.append(i < charge ? "&f|" : "&8|");
                  }

                  TextUtils.sendActionBar(player, TextUtils.legacy(bar.toString()));
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 5L);
   }

   private String yellowTitle(String title) {
      return ChatColor.YELLOW + ChatColor.stripColor(title);
   }

   public void resetCooldowns(Player player) {
      UUID uuid = player.getUniqueId();
      this.dashCharges.remove(uuid);
      this.dashCooldowns.remove(uuid);
      this.auraCooldowns.remove(uuid);
      this.attackCharges.remove(uuid);
      BossBar dashBar = this.dashBars.remove(uuid);
      BossBar auraBar = this.auraBars.remove(uuid);
      if (dashBar != null) {
         dashBar.removeAll();
      }

      if (auraBar != null) {
         auraBar.removeAll();
      }
   }

   public void clearAllBars() {
      for (BossBar bar : this.dashBars.values()) {
         bar.removeAll();
      }

      for (BossBar bar : this.auraBars.values()) {
         bar.removeAll();
      }

      this.dashBars.clear();
      this.auraBars.clear();
      this.dashCharges.clear();
      this.dashCooldowns.clear();
      this.auraCooldowns.clear();
      this.attackCharges.clear();
   }
}

package dev.vaileks.runesaltarv.weapons.windweaver.listeners;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.SpeedBuffUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WindWeaverListener implements Listener {
   private static final int MAX_CHARGE = 100;
   private final RunesAltarV plugin;
   private final Map<UUID, WindWeaverListener.WindBurstSession> burstSessions = new HashMap<>();
   private final Map<UUID, BossBar> leapCooldownBars = new HashMap<>();
   private final Map<UUID, BossBar> burstCooldownBars = new HashMap<>();
   private final Map<UUID, Long> leapCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("windweaver","leap");
   private final Map<UUID, Long> burstCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("windweaver","burst");

   public WindWeaverListener(RunesAltarV plugin) {
      this.plugin = plugin;
   }

   public static boolean isWindWeaver(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "windweaver");
   }

   public boolean onLeapCooldown(Player player) {
      return this.leapCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
   }

   public boolean onBurstCooldown(Player player) {
      return this.burstCooldowns.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
   }

   public void resetPlayer(Player player) {
      UUID uuid = player.getUniqueId();
      this.leapCooldowns.remove(uuid);
      this.burstCooldowns.remove(uuid);
      BossBar leapBar = this.leapCooldownBars.remove(uuid);
      if (leapBar != null) {
         leapBar.removeAll();
      }

      BossBar burstBar = this.burstCooldownBars.remove(uuid);
      if (burstBar != null) {
         burstBar.removeAll();
      }

      WindWeaverListener.WindBurstSession session = this.burstSessions.remove(uuid);
      if (session != null && session.task != null) {
         session.task.cancel();
      }
   }

   public void removeAllBars() {
      this.leapCooldownBars.values().forEach(BossBar::removeAll);
      this.burstCooldownBars.values().forEach(BossBar::removeAll);
      this.leapCooldownBars.clear();
      this.burstCooldownBars.clear();
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.resetPlayer(event.getPlayer());
   }

   @EventHandler
   public void onSwapHand(PlayerSwapHandItemsEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         ItemStack item = player.getInventory().getItemInMainHand();
         if (isWindWeaver(item)) {
            event.setCancelled(true);
            UUID id = player.getUniqueId();
            WindWeaverListener.WindBurstSession session = this.burstSessions.get(id);
            if (player.isSneaking()) {
               if (!this.onBurstCooldown(player)) {
                  if (session == null) {
                     this.startBurstCharge(player);
                  }
               }
            } else {
               this.castWindLeap(player);
            }
         }
      }
   }

   private void castWindLeap(final Player player) {
      if (!this.onLeapCooldown(player)) {
         Vector dir = player.getLocation().getDirection().normalize();
         int cooldown = this.plugin.getWeaponsConfig().getInt("windweaver.wind-leap.cooldown", 10);
         double velocity = this.plugin.getWeaponsConfig().getDouble("windweaver.wind-leap.velocity", 1.25);
         player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0F, 0.8F);
         player.setVelocity(dir.multiply(velocity));
         player.setFallDistance(0.0F);
         int duration = this.plugin.getWeaponsConfig().getInt("windweaver.wind-leap.speed-duration", 3) * 20;
         int amplifier = this.plugin.getWeaponsConfig().getInt("windweaver.wind-leap.speed-amplifier", 4) - 1;
         SpeedBuffUtils.applyBurstSpeed(player, duration, amplifier, this.plugin);
         Location start = player.getLocation().add(0.0, 1.0, 0.0);

         for (int i = 0; i < 5; i++) {
            Location gust = start.clone().add((Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 0.5);
            gust.getWorld().spawnParticle(Particle.GUST, gust, 1, 0.02, 0.02, 0.02, 0.0);
         }

         this.leapCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (long)cooldown * 1000L);
         this.startCooldownBar(player, cooldown, this.plugin.tr("§eВетровой Прыжок", "§eᴡɪɴᴅ ʟᴇᴀᴘ"), this.leapCooldownBars, this.leapCooldowns);
         (new BukkitRunnable() {
            int elapsedTicks = 0;

            @Override
            public void run() {
               this.elapsedTicks++;
               if (!player.isOnline() || player.isDead() || this.elapsedTicks > 200) {
                  this.cancel();
               } else if (this.elapsedTicks > 5 && player.isOnGround()) {
                  this.cancel();
               } else {
                  Location loc = player.getLocation().add(0.0, 0.95, 0.0);
                  Vector back = player.getLocation().getDirection().normalize().multiply(-0.3);
                  Location trail = loc.clone().add(back);
                  trail.getWorld().spawnParticle(Particle.CLOUD, trail, 2, 0.15, 0.12, 0.15, 0.01);
                  trail.getWorld().spawnParticle(Particle.SMALL_GUST, trail, 1, 0.1, 0.1, 0.1, 0.01);
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void startBurstCharge(final Player player) {
      if (!this.onBurstCooldown(player)) {
         final UUID id = player.getUniqueId();
         final WindWeaverListener.WindBurstSession session = new WindWeaverListener.WindBurstSession();
         session.percent = 0;
         session.ticks = 0;
         this.burstSessions.put(id, session);
         player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_INHALE, 1.0F, 0.8F);
         session.task = new BukkitRunnable() {
            @Override
            public void run() {
               if (player.isOnline() && !player.isDead() && WindWeaverListener.isWindWeaver(player.getInventory().getItemInMainHand())) {
                  if (player.isSneaking()) {
                     session.ticks++;
                     if (session.percent < 100) {
                        session.percent++;
                        if (session.ticks % 12 == 0) {
                           player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_IDLE_GROUND, 1.0F, 1.2F);
                        }

                        WindWeaverListener.this.spawnChargeParticles(player);
                     }

                     WindWeaverListener.this.updateBurstActionBar(player, session.percent);
                  } else {
                     WindWeaverListener.this.releaseBurst(player, session);
                  }
               } else {
                  WindWeaverListener.this.burstSessions.remove(id);
                  this.cancel();
               }
            }
         };
         session.task.runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void releaseBurst(final Player player, WindWeaverListener.WindBurstSession session) {
      UUID id = player.getUniqueId();
      this.burstSessions.remove(id);
      if (session.task != null) {
         session.task.cancel();
      }

      int percent = Math.max(0, Math.min(100, session.percent));
      double powerScale = (double)percent / 100.0;
      int cooldown = this.plugin.getWeaponsConfig().getInt("windweaver.wind-burst.cooldown", 35);
      double knockbackPower = this.plugin.getWeaponsConfig().getDouble("windweaver.wind-burst.knockback-power", 3.6);
      double upwardPower = this.plugin.getWeaponsConfig().getDouble("windweaver.wind-burst.upward-power", 1.2);
      double radius = this.plugin.getWeaponsConfig().getDouble("windweaver.wind-burst.radius", 25.0);
      this.burstCooldowns.put(id, System.currentTimeMillis() + (long)cooldown * 1000L);
      this.startCooldownBar(player, cooldown, this.plugin.tr("§eПорыв Ветра", "§eᴡɪɴᴅ ʙᴜʀsᴛ"), this.burstCooldownBars, this.burstCooldowns);
      Location center = player.getLocation().add(0.0, 1.0, 0.0);
      player.playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0F, 0.8F);
      int cloudCount = Math.max(80, (int)(1200.0 * powerScale));

      for (int i = 0; i < cloudCount; i++) {
         double phi = Math.random() * Math.PI * 2.0;
         double theta = Math.acos(Math.random() * 2.0 - 1.0);
         double speed = (0.35 + Math.random() * 0.85) * (0.8 + powerScale * 1.8);
         double dx = Math.sin(theta) * Math.cos(phi) * speed;
         double dy = Math.sin(theta) * Math.sin(phi) * speed;
         double dz = Math.cos(theta) * speed;
         center.getWorld().spawnParticle(Particle.CLOUD, center, 0, dx, dy, dz, 1.0);
      }

      for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
         if (entity instanceof LivingEntity) {
            LivingEntity target = (LivingEntity)entity;
            if (!target.equals(player)) {
               if (target instanceof Player) {
                  Player targetPlayer = (Player)target;
                  if (targetPlayer.getGameMode().name().equalsIgnoreCase("SPECTATOR")
                     || this.plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) {
                     continue;
                  }
               }

               double dist = target.getLocation().distance(center);
               double factor = Math.max(0.0, 1.0 - dist / radius) * powerScale;
               if (!(factor <= 0.0) && dev.vaileks.runesaltarv.utils.CombatUtils.clearPath(center,target)) {
                  Vector away = target.getLocation().toVector().subtract(center.toVector());
                  if (away.lengthSquared() == 0.0) {
                     away = new Vector(0, 1, 0);
                  } else {
                     away.normalize();
                  }

                  Vector velocity = away.multiply(knockbackPower * factor);
                  velocity.setY(upwardPower * factor + 0.35);
                  target.setVelocity(target.getVelocity().add(velocity));
               }
            }
         }
      }

      this.updateBurstActionBar(player, percent);
      (new BukkitRunnable() {
         @Override
         public void run() {
            if (player.isOnline()) {
               TextUtils.sendActionBar(player, TextUtils.legacy(" "));
            }
         }
      }).runTaskLater(this.plugin, 20L);
   }

   private void spawnChargeParticles(Player player) {
      Location base = player.getLocation().add(0.0, 1.0, 0.0);

      for (int i = 0; i < 25; i++) {
         double radius = 1.6 + Math.random() * 1.0;
         double theta = Math.random() * Math.PI * 2.0;
         double phi = Math.acos(Math.random() * 2.0 - 1.0);
         double x = radius * Math.sin(phi) * Math.cos(theta);
         double y = radius * Math.sin(phi) * Math.sin(theta);
         double z = radius * Math.cos(phi);
         Location loc = base.clone().add(x, y, z);
         loc.getWorld().spawnParticle(Particle.SMALL_GUST, loc, 1, 0.0, 0.0, 0.0, 0.0);
         if (Math.random() < 0.08) {
            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private void updateBurstActionBar(Player player, int percent) {
      int filled = Math.max(0, Math.min(10, percent / 10));
      StringBuilder bar = new StringBuilder();

      for (int i = 0; i < 10; i++) {
         if (i < filled) {
            bar.append("&a&l|");
         } else {
            bar.append("&c&l|");
         }
      }

      String percentText = percent >= 100 ? "&a&l100%" : (percent < 30 ? "&c" + percent + "%" : "&a" + percent + "%");
      TextUtils.sendActionBar(player, TextUtils.legacy(percentText + " " + bar));
   }

   private void startCooldownBar(final Player player, final int seconds, String title, final Map<UUID, BossBar> barMap, final Map<UUID, Long> cooldowns) {
      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(title, BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(player);
      barMap.put(player.getUniqueId(), bar);
      (new BukkitRunnable() {
         double progress = 1.0;
         final double step = 1.0 / ((double)seconds * 20.0);

         @Override
         public void run() {
            if (player.isOnline() && barMap.containsKey(player.getUniqueId()) && barMap.get(player.getUniqueId()) == bar) {
               if (cooldowns.getOrDefault(player.getUniqueId(), 0L) <= System.currentTimeMillis()) {
                  bar.removeAll();
                  barMap.remove(player.getUniqueId());
                  this.cancel();
               } else {
                  this.progress = this.progress - this.step;
                  bar.setProgress(Math.max(0.0, this.progress));
               }
            } else {
               bar.removeAll();
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private static class WindBurstSession {
      int percent = 0;
      int ticks = 0;
      BukkitRunnable task;
   }
}

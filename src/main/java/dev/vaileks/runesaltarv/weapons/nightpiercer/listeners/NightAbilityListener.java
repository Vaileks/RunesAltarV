package dev.vaileks.runesaltarv.weapons.nightpiercer.listeners;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import dev.vaileks.runesaltarv.weapons.nightpiercer.managers.NightBossBarCooldown;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class NightAbilityListener implements Listener {
   private final RunesAltarV plugin;
   private final Map<UUID, Long> biteCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("nightpiercer","bite");
   private final Map<UUID, Long> transformationCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("nightpiercer","transformation");
   private final Set<UUID> preparedBites = new HashSet<>();

   public NightAbilityListener(RunesAltarV plugin) {
      this.plugin = plugin;
   }

   private boolean isNightpiercer(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "nightpiercer");
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onSwapHand(PlayerSwapHandItemsEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         ItemStack item = player.getInventory().getItemInMainHand();
         if (this.isNightpiercer(item)) {
            event.setCancelled(true);
            if (!this.plugin.isAboveLegendaryHeight(player)) {
               if (player.isSneaking()) {
                  if (!this.isOnCooldown(player, this.transformationCooldowns)) {
                     this.executeTransformation(player);
                  }
               } else if (!this.isOnCooldown(player, this.biteCooldowns)) {
                  if (!this.preparedBites.contains(player.getUniqueId())) {
                     this.preparedBites.add(player.getUniqueId());
                     player.playSound(player.getLocation(),AltarTuning.sound("nightpiercer","visuals.sounds.entity-phantom-ambient.sound",Sound.ENTITY_PHANTOM_AMBIENT),AltarTuning.volume("nightpiercer","visuals.sounds.entity-phantom-ambient",1.0F),AltarTuning.pitch("nightpiercer","visuals.sounds.entity-phantom-ambient",1.0F));
                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      if (this.isNightpiercer(player.getInventory().getItemInMainHand())) {
         if ((event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) && this.preparedBites.contains(player.getUniqueId())) {
            this.executeBite(player, null);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onEntityHit(EntityDamageByEntityEvent event) {
      if (CombatUtils.isDirectMeleeHit(event)) {
         if (event.getDamager() instanceof Player player) {
            if (this.preparedBites.contains(player.getUniqueId())) {
               if (event.getEntity() instanceof LivingEntity victim) {
                  if (victim instanceof Player targetPlayer) {
                     if (targetPlayer.getGameMode() == GameMode.SPECTATOR) {
                        return;
                     }

                     if (this.plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) {
                        return;
                     }
                  }

                  event.setCancelled(true);
                  this.executeBite(player, victim);
               }
            }
         }
      }
   }

   private void executeBite(Player player, LivingEntity target) {
      this.preparedBites.remove(player.getUniqueId());
      int cooldown = this.plugin.getWeaponsConfig().getInt("nightpiercer.bite.cooldown", 45);
      double damage = this.plugin.getWeaponsConfig().getDouble("nightpiercer.bite.damage", 4.0);
      double range = AltarTuning.number("nightpiercer","tuning.night-ability-listener.range",2.5,0.01,128);
      this.biteCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (long)cooldown * 1000L);
      new NightBossBarCooldown(this.plugin, player, AltarTuning.text("nightpiercer","messages.night-ability-listener.message-1","багровый укус","ᴄʀɪᴍsᴏɴ ʙɪᴛᴇ"), cooldown).start();
      Vector viewDirection = player.getLocation().getDirection();
      Location effectLocation;
      if (target != null) {
         effectLocation = target.getLocation().add(0.0, 1.1, 0.0).subtract(viewDirection.multiply(0.2));
         this.applyBiteEffects(player, target, damage);
      } else {
         effectLocation = player.getEyeLocation().add(viewDirection.multiply(1.2));

         for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity) {
               LivingEntity victim = (LivingEntity)entity;
               if (!entity.equals(player)) {
                  if (victim instanceof Player) {
                     Player targetPlayer = (Player)victim;
                     if (targetPlayer.getGameMode() == GameMode.SPECTATOR
                        || this.plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) {
                        continue;
                     }
                  }

                  Vector toTarget = victim.getLocation().toVector().subtract(player.getLocation().toVector());
                  if ((double)viewDirection.angle(toTarget) < 0.8) {
                     effectLocation = victim.getLocation().add(0.0, 1.1, 0.0).subtract(viewDirection.multiply(0.2));
                     this.applyBiteEffects(player, victim, damage);
                     break;
                  }
               }
            }
         }
      }

      this.spawnMassiveCross(effectLocation, viewDirection);
      player.getWorld().playSound(effectLocation,AltarTuning.sound("nightpiercer","visuals.sounds.entity-phantom-bite.sound",Sound.ENTITY_PHANTOM_BITE),AltarTuning.volume("nightpiercer","visuals.sounds.entity-phantom-bite",1.5F),AltarTuning.pitch("nightpiercer","visuals.sounds.entity-phantom-bite",1.0F));
   }

   private void applyBiteEffects(Player attacker, LivingEntity victim, double damage) {
      this.plugin.getNightHealthManager().applySteal(attacker, victim);
      CombatUtils.runSyntheticDamage(() -> victim.damage(damage, attacker));
      if (victim instanceof Player victimPlayer) {
         int durationSeconds = this.plugin.getWeaponsConfig().getInt("nightpiercer.bite.duration", 10);
         final int totalTicks = durationSeconds * 20;
         final BossBar victimBar = TextUtils.bossBar(AltarTuning.text("nightpiercer","messages.night-ability-listener.message-2","§6§l!! §c§lУКУШЕН §6§l!!","§6§l!! §c§lBITTEN §6§l!!"), BarColor.RED, BarStyle.SOLID);
         victimBar.addPlayer(victimPlayer);
         (new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
               if (victimPlayer.isOnline() && this.ticks < totalTicks) {
                  double progress = 1.0 - (double)this.ticks / (double)totalTicks;
                  victimBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                  this.ticks++;
               } else {
                  victimBar.removeAll();
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void spawnMassiveCross(Location center, Vector direction) {
      World world = center.getWorld();
      if (world != null) {
         Vector side = new Vector(-direction.getZ(), 0.0, direction.getX()).normalize();
         Vector up = side.clone().crossProduct(direction).normalize();
         Vector diagonalOne = up.clone().add(side).normalize();
         Vector diagonalTwo = up.clone().subtract(side).normalize();
         double size = AltarTuning.number("nightpiercer","tuning.night-ability-listener.size",2.5,0.01,128);

         for (double i = -size; i <= size; i += 0.2) {
            double bulge = Math.cos(i / size * (Math.PI / 2)) * 2.0;
            Location pointOne = center.clone().add(diagonalOne.clone().multiply(i)).add(direction.clone().multiply(bulge));
            Location pointTwo = center.clone().add(diagonalTwo.clone().multiply(i)).add(direction.clone().multiply(bulge));
            world.spawnParticle(Particle.BLOCK, pointOne,AltarTuning.particles("nightpiercer",2), 0.05, 0.05, 0.05, 0.0, Bukkit.createBlockData(Material.REDSTONE_BLOCK));
            world.spawnParticle(Particle.BLOCK, pointTwo,AltarTuning.particles("nightpiercer",2), 0.05, 0.05, 0.05, 0.0, Bukkit.createBlockData(Material.REDSTONE_BLOCK));
         }
      }
   }

   private void executeTransformation(final Player player) {
      int cooldown = this.plugin.getWeaponsConfig().getInt("nightpiercer.transformation.cooldown", 20);
      final int maxTicks = this.plugin.getWeaponsConfig().getInt("nightpiercer.transformation.duration", 2) * 20;
      final double speed = this.plugin.getWeaponsConfig().getDouble("nightpiercer.transformation.speed", 1.0);
      final double damage = this.plugin.getWeaponsConfig().getDouble("nightpiercer.transformation.damage", 4.0);
      this.transformationCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (long)cooldown * 1000L);
      new NightBossBarCooldown(this.plugin, player, AltarTuning.text("nightpiercer","messages.night-ability-listener.message-3","трансформация","ᴛʀᴀɴsꜰᴏʀᴍᴀᴛɪᴏɴ"), cooldown).start();
      player.getWorld().playSound(player.getLocation(),AltarTuning.sound("nightpiercer","visuals.sounds.entity-illusioner-mirror-move.sound",Sound.ENTITY_ILLUSIONER_MIRROR_MOVE),AltarTuning.volume("nightpiercer","visuals.sounds.entity-illusioner-mirror-move",1.0F),AltarTuning.pitch("nightpiercer","visuals.sounds.entity-illusioner-mirror-move",1.0F));
      player.getWorld().playSound(player.getLocation(),AltarTuning.sound("nightpiercer","visuals.sounds.entity-firework-rocket-launch.sound",Sound.ENTITY_FIREWORK_ROCKET_LAUNCH),AltarTuning.volume("nightpiercer","visuals.sounds.entity-firework-rocket-launch",1.0F),AltarTuning.pitch("nightpiercer","visuals.sounds.entity-firework-rocket-launch",1.0F));
      player.getWorld().playSound(player.getLocation(),AltarTuning.sound("nightpiercer","visuals.sounds.entity-zombie-break-wooden-door.sound",Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR),AltarTuning.volume("nightpiercer","visuals.sounds.entity-zombie-break-wooden-door",1.0F),AltarTuning.pitch("nightpiercer","visuals.sounds.entity-zombie-break-wooden-door",1.2F));
      this.spawnBlackDust(player.getLocation(), 140);
      final List<Bat> bats = new ArrayList<>();

      for (int i = 0; i < 30; i++) {
         Bat bat = player.getWorld().spawn(player.getLocation().add(0.0, 1.0, 0.0), Bat.class);
         bat.setInvulnerable(true);
         bat.setSilent(true);
         bat.setAI(true);
         bat.setPersistent(true);
         bat.setCollidable(false);
         bats.add(bat);
      }

      final Set<UUID> hitEntities = new HashSet<>();
      (new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
               if (this.ticks < maxTicks && player.isOnline() && NightAbilityListener.this.isNightpiercer(player.getInventory().getItemInMainHand())) {
                  player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, AltarTuning.integer("nightpiercer","tuning.night-ability-listener.effects.invisibility.duration-ticks",5,1,72000), AltarTuning.integer("nightpiercer","tuning.night-ability-listener.effects.invisibility.amplifier",1,0,10), false, false, false));

                  for (Player online : Bukkit.getOnlinePlayers()) {
                     if (online.canSee(player)) {
                        online.hidePlayer(NightAbilityListener.this.plugin, player);
                     }
                  }

                  player.setVelocity(player.getLocation().getDirection().normalize().multiply(speed));
                  if (this.ticks == 0 || this.ticks == 5 || this.ticks == 15 || this.ticks == 25 || this.ticks == 35) {
                     player.getWorld().playSound(player.getLocation(),AltarTuning.sound("nightpiercer","visuals.sounds.entity-bat-ambient.sound",Sound.ENTITY_BAT_AMBIENT),AltarTuning.volume("nightpiercer","visuals.sounds.entity-bat-ambient",0.8F),AltarTuning.pitch("nightpiercer","visuals.sounds.entity-bat-ambient",1.0F));
                  }

                  Location center = player.getLocation().add(0.0, 0.8, 0.0);

                  for (Bat bat : bats) {
                     if (bat.getLocation().distance(center) > 0.1) {
                        Vector offset = new Vector(
                           ThreadLocalRandom.current().nextDouble(-1.5, 1.5),
                           ThreadLocalRandom.current().nextDouble(-0.5, 1.0),
                           ThreadLocalRandom.current().nextDouble(-1.5, 1.5)
                        );
                        bat.teleport(center.clone().add(offset));
                     }
                  }

                  for (Entity entity : player.getNearbyEntities(AltarTuning.number("nightpiercer","tuning.night-ability-listener.hit-radius-x",1.5,0,128),AltarTuning.number("nightpiercer","tuning.night-ability-listener.hit-radius-y",1.5,0,128),AltarTuning.number("nightpiercer","tuning.night-ability-listener.hit-radius-z",1.5,0,128))) {
                     if (entity instanceof LivingEntity) {
                        LivingEntity victim = (LivingEntity)entity;
                        if (!entity.equals(player) && !bats.contains(victim) && !hitEntities.contains(victim.getUniqueId())) {
                           if (victim instanceof Player) {
                              Player targetPlayer = (Player)victim;
                              if (targetPlayer.getGameMode() == GameMode.SPECTATOR
                                 || NightAbilityListener.this.plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) {
                                 continue;
                              }
                           }

                           hitEntities.add(victim.getUniqueId());
                           NightAbilityListener.this.plugin.getCleanDamageManager().apply(victim, player, damage);
                           victim.getWorld().playSound(victim.getLocation(),AltarTuning.sound("nightpiercer","visuals.sounds.entity-zombie-attack-iron-door.sound",Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR),AltarTuning.volume("nightpiercer","visuals.sounds.entity-zombie-attack-iron-door",1.0F),AltarTuning.pitch("nightpiercer","visuals.sounds.entity-zombie-attack-iron-door",1.0F));
                        }
                     }
                  }

                  this.ticks++;
               } else {
                  bats.forEach(Entity::remove);
                  player.removePotionEffect(PotionEffectType.INVISIBILITY);

                  for (Player onlinex : Bukkit.getOnlinePlayers()) {
                     onlinex.showPlayer(NightAbilityListener.this.plugin, player);
                  }

                  NightAbilityListener.this.spawnBlackDust(player.getLocation(), 140);
                  player.getWorld().playSound(player.getLocation(),AltarTuning.sound("nightpiercer","visuals.sounds.entity-illusioner-mirror-move.sound",Sound.ENTITY_ILLUSIONER_MIRROR_MOVE),AltarTuning.volume("nightpiercer","visuals.sounds.entity-illusioner-mirror-move",1.0F),AltarTuning.pitch("nightpiercer","visuals.sounds.entity-illusioner-mirror-move",1.0F));
                  player.getWorld().playSound(player.getLocation(),AltarTuning.sound("nightpiercer","visuals.sounds.entity-zombie-break-wooden-door.sound",Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR),AltarTuning.volume("nightpiercer","visuals.sounds.entity-zombie-break-wooden-door",1.0F),AltarTuning.pitch("nightpiercer","visuals.sounds.entity-zombie-break-wooden-door",1.2F));
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void spawnBlackDust(Location location, int count) {
      DustOptions blackDust = new DustOptions(Color.fromRGB(0, 0, 0), 2.8F);
      location.getWorld().spawnParticle(Particle.DUST, location.clone().add(0.0, 1.0, 0.0),AltarTuning.particles("nightpiercer",count), 0.7, 0.9, 0.7, blackDust);
   }

   private boolean isOnCooldown(Player player, Map<UUID, Long> cooldownMap) {
      return cooldownMap.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
   }

   public void resetCooldowns(Player player) {
      UUID uuid = player.getUniqueId();
      this.biteCooldowns.remove(uuid);
      this.transformationCooldowns.remove(uuid);
      this.preparedBites.remove(uuid);
   }
}

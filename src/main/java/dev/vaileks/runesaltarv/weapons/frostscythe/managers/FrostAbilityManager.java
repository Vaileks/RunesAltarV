package dev.vaileks.runesaltarv.weapons.frostscythe.managers;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.ParticleUtils;
import dev.vaileks.runesaltarv.weapons.frostscythe.listeners.FrostListener;
import dev.vaileks.runesaltarv.weapons.frostscythe.tasks.FrostScytheTask;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class FrostAbilityManager {
   private final RunesAltarV plugin;
   private final FrostBossBarManager bossBarManager;
   private final BlockData blueIceData = Bukkit.createBlockData(Material.PACKED_ICE);
   private final BlockData packedIceData = Bukkit.createBlockData(Material.PACKED_ICE);

   public FrostAbilityManager(RunesAltarV plugin, FrostBossBarManager bossBarManager) {
      this.plugin = plugin;
      this.bossBarManager = bossBarManager;
   }

   public RunesAltarV getPlugin() {
      return this.plugin;
   }

   public boolean isOnCooldown(Player p, String ability) {
      return this.bossBarManager.isOnCooldown(p, ability);
   }

   public void playSweepEffect(Location center, Vector dir) {
      center.getWorld().playSound(center,AltarTuning.sound("frost_scythe","visuals.sounds.item-trident-throw.sound",Sound.ITEM_TRIDENT_THROW),AltarTuning.volume("frost_scythe","visuals.sounds.item-trident-throw",1.0F),AltarTuning.pitch("frost_scythe","visuals.sounds.item-trident-throw",0.2F));
      Vector forward = dir.clone().normalize();
      if (forward.lengthSquared() == 0.0) {
         forward = new Vector(1, 0, 0);
      }

      Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
      if (right.lengthSquared() == 0.0) {
         right = new Vector(1, 0, 0);
      }

      Vector up = right.clone().crossProduct(forward).normalize();
      double rotAngle = Math.random() * Math.PI;
      double cos = Math.cos(rotAngle);
      double sin = Math.sin(rotAngle);

      for (double i = -1.2; i <= 1.2; i += 0.05) {
         double x = i * cos;
         double y = i * sin;
         double z = -(i * i) * 0.25;
         Vector point = right.clone().multiply(x).add(up.clone().multiply(y)).add(forward.clone().multiply(z));
         Location pLoc = center.clone().add(point);
         center.getWorld().spawnParticle(Particle.BLOCK, pLoc,AltarTuning.particles("frost_scythe",1), 0.0, 0.0, 0.0, 0.0, this.blueIceData);
      }
   }

   public void playMassiveDustSweep(Location center, Vector dir) {
      center.getWorld().playSound(center,AltarTuning.sound("frost_scythe","visuals.sounds.item-trident-throw.sound",Sound.ITEM_TRIDENT_THROW),AltarTuning.volume("frost_scythe","visuals.sounds.item-trident-throw",1.1F),AltarTuning.pitch("frost_scythe","visuals.sounds.item-trident-throw",0.55F));
      Vector forward = dir.clone().setY(0).normalize();
      if (forward.lengthSquared() == 0.0) {
         forward = new Vector(1, 0, 0);
      }

      Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

      for (double i = -2.5; i <= 2.5; i += 0.05) {
         double z = -(i * i) * 0.25;
         Vector point = right.clone().multiply(i).add(forward.clone().multiply(z));
         Location spawnLoc = center.clone().add(point);
         center.getWorld().spawnParticle(Particle.SNOWFLAKE, spawnLoc,AltarTuning.particles("frost_scythe",1), 0.0, 0.0, 0.0, 0.0);
         center.getWorld().spawnParticle(Particle.ENCHANTED_HIT, spawnLoc,AltarTuning.particles("frost_scythe",1), 0.0, 0.0, 0.0, 0.0);
      }
   }

   public void playImpactBurst(Location center, Vector dir) {
      center.getWorld().playSound(center,AltarTuning.sound("frost_scythe","visuals.sounds.item-trident-throw.sound",Sound.ITEM_TRIDENT_THROW),AltarTuning.volume("frost_scythe","visuals.sounds.item-trident-throw",1.15F),AltarTuning.pitch("frost_scythe","visuals.sounds.item-trident-throw",0.4F));
      center.getWorld().playSound(center,AltarTuning.sound("frost_scythe","visuals.sounds.block-glass-break.sound",Sound.BLOCK_GLASS_BREAK),AltarTuning.volume("frost_scythe","visuals.sounds.block-glass-break",1.5F),AltarTuning.pitch("frost_scythe","visuals.sounds.block-glass-break",1.0F));
      Location baseLoc = center.clone().add(0.0, 0.2, 0.0);
      center.getWorld().spawnParticle(Particle.SNOWFLAKE, baseLoc,AltarTuning.particles("frost_scythe",30), 0.4, 0.4, 0.4, 0.08);
      ParticleUtils.spawnBlockDispersion(this.plugin, baseLoc, this.blueIceData, 4.2, 85);
   }

   public void executeScytheThrow(Player p, ItemStack scytheItem, EquipmentSlot hand) {
      if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().canUseAbilities(p)) {
         this.plugin.getWorldGuardManager().notifyDenied(p);
      } else {
         int throwCd = this.plugin.getWeaponsConfig().getInt("frost-scythe.throw.cooldown", 45);
         this.bossBarManager.setCooldown(p, "ScytheThrow", "§b§lsᴄʏᴛʜᴇ ᴛʜʀᴏᴡ", throwCd);
         ItemStack throwItem = scytheItem.clone();
         throwItem.setType(Material.TRIDENT);
         FrostListener.setScytheModel(throwItem, false);
         if (this.plugin.getCrazySlotsManager() != null && this.plugin.getCrazySlotsManager().isTransformedItem(throwItem)) {
            UUID instId = this.plugin.getCrazySlotsManager().getTransformedInstanceId(throwItem);
            this.plugin.getCrazySlotsManager().markInFlight(instId);
         }

         if (hand == EquipmentSlot.OFF_HAND) {
            p.getInventory().setItemInOffHand(null);
         } else {
            p.getInventory().setItemInMainHand(null);
         }

         Location start = p.getEyeLocation();
         Vector dir = start.getDirection().normalize().multiply(this.plugin.getWeaponsConfig().getDouble("frost-scythe.throw.flight-speed", 1.65));
         Location sweepCenter = start.clone().add(dir.clone().normalize().multiply(1.5));
         this.playMassiveDustSweep(sweepCenter, dir);
         new FrostScytheTask(this.plugin, p, this, throwItem).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   public void castCommandOfIce(final Player p) {
      if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().canUseAbilities(p)) {
         this.plugin.getWorldGuardManager().notifyDenied(p);
      } else if (!this.isOnCooldown(p, "CommandOfIce")) {
         int cd = this.plugin.getWeaponsConfig().getInt("frost-scythe.ice-command.cooldown", 45);
         this.bossBarManager.setCooldown(p, "CommandOfIce", "§b§lᴄᴏᴍᴍᴀɴᴅ ᴏғ ɪᴄᴇ", cd);
         p.getWorld().playSound(p.getLocation(),AltarTuning.sound("frost_scythe","visuals.sounds.block-trial-spawner-about-to-spawn-item.sound",Sound.BLOCK_TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM),AltarTuning.volume("frost_scythe","visuals.sounds.block-trial-spawner-about-to-spawn-item",1.0F),AltarTuning.pitch("frost_scythe","visuals.sounds.block-trial-spawner-about-to-spawn-item",1.0F));
         final List<BlockDisplay> iceBlocks = new ArrayList<>();
         final java.util.Set<UUID> volleyHits = new java.util.HashSet<>();
         final Vector[] targetOffsets = new Vector[]{new Vector(1.2, 3.0, 1.2), new Vector(0.0, 3.6, 1.5), new Vector(-1.2, 3.0, 1.2)};
         (new BukkitRunnable() {
               int spawnTick = 0;
               int launchTick = 0;
               boolean allSpawned = false;

               @Override
               public void run() {
                  if (p.isOnline() && !p.isDead()) {
                     if (!this.allSpawned) {
                        if (this.spawnTick % 10 == 0 && iceBlocks.size() < 3) {
                           p.getWorld().playSound(p.getLocation(),AltarTuning.sound("frost_scythe","visuals.sounds.block-trial-spawner-spawn-item.sound",Sound.BLOCK_TRIAL_SPAWNER_SPAWN_ITEM),AltarTuning.volume("frost_scythe","visuals.sounds.block-trial-spawner-spawn-item",1.0F),AltarTuning.pitch("frost_scythe","visuals.sounds.block-trial-spawner-spawn-item",1.0F));
                           BlockDisplay block = p.getWorld().spawn(p.getLocation().add(0.0, 1.0, 0.0), BlockDisplay.class, ent -> {
                              ent.setBlock(FrostAbilityManager.this.packedIceData);
                              Transformation t = ent.getTransformation();
                              t.getScale().set(0.0F, 0.0F, 0.0F);
                              t.getTranslation().set(-0.265F, -0.265F, -0.265F);
                              ent.setTransformation(t);
                              ent.setTeleportDuration(3);
                           });
                           FrostAbilityManager.this.plugin.getVisualCleanupManager().track(block);
                           block.setInterpolationDelay(0);
                           block.setInterpolationDuration(36);
                           Transformation t = block.getTransformation();
                           t.getScale().set(0.53F, 0.53F, 0.53F);
                           t.getLeftRotation().rotationXYZ(0.785F, 0.785F, 0.0F);
                           Vector3f center = new Vector3f(0.265F, 0.265F, 0.265F);
                           Vector3f trans = new Vector3f(center);
                           t.getLeftRotation().transform(trans);
                           trans.mul(-1.0F);
                           t.getTranslation().set(trans);
                           block.setTransformation(t);
                           iceBlocks.add(block);
                        }

                        Vector pDir = p.getLocation().getDirection().setY(0);
                        if (!(pDir.lengthSquared() < 1.0E-4) && !Double.isNaN(pDir.getX())) {
                           pDir.normalize();
                        } else {
                           pDir = new Vector(1, 0, 0);
                        }

                        Vector pRight = pDir.clone().crossProduct(new Vector(0, 1, 0));
                        if (!(pRight.lengthSquared() < 1.0E-4) && !Double.isNaN(pRight.getX())) {
                           pRight.normalize();
                        } else {
                           pRight = new Vector(0, 0, 1);
                        }

                        Location pLoc = p.getLocation();

                        for (int i = 0; i < iceBlocks.size(); i++) {
                           BlockDisplay b = iceBlocks.get(i);
                           Vector offset = targetOffsets[i];
                           double bobbing = Math.sin((double)(this.spawnTick + i * 30) * 0.05) * 0.25;
                           Location targetLoc = pLoc.clone()
                              .add(pRight.clone().multiply(offset.getX()))
                              .add(0.0, offset.getY() + bobbing, 0.0)
                              .add(pDir.clone().multiply(offset.getZ()));
                           b.teleport(targetLoc);
                           if (this.spawnTick % 5 == 0) {
                              b.getWorld().spawnParticle(Particle.SNOWFLAKE, targetLoc,AltarTuning.particles("frost_scythe",2), 0.3, 0.3, 0.3, 0.0);
                              b.getWorld().spawnParticle(Particle.ENCHANTED_HIT, targetLoc,AltarTuning.particles("frost_scythe",6), 0.9, 0.9, 0.9, 0.0);
                              b.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, targetLoc,AltarTuning.particles("frost_scythe",2), 0.3, 0.3, 0.3, 0.0);
                           }
                        }

                        if (iceBlocks.size() == 3 && this.spawnTick >= 60) {
                           this.allSpawned = true;
                        }

                        this.spawnTick++;
                     } else {
                        if (this.launchTick % 3 == 0) {
                           int indexToLaunch = this.launchTick / 3;
                           if (indexToLaunch < 3) {
                              BlockDisplay blockToLaunch = iceBlocks.get(indexToLaunch);
                              if (blockToLaunch.isValid()) {
                                 FrostAbilityManager.this.launchIceMissile(p, blockToLaunch, targetOffsets[indexToLaunch],volleyHits);
                              }
                           } else {
                              this.cancel();
                           }
                        }

                        this.launchTick++;
                     }
                  } else {
                     iceBlocks.forEach(Entity::remove);
                     this.cancel();
                  }
               }
            })
            .runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void launchIceMissile(final Player p, final BlockDisplay block, final Vector offset, final java.util.Set<UUID> volleyHits) {
      p.getWorld().playSound(p.getLocation(),AltarTuning.sound("frost_scythe","visuals.sounds.entity-witch-throw.sound",Sound.ENTITY_WITCH_THROW),AltarTuning.volume("frost_scythe","visuals.sounds.entity-witch-throw",1.0F),AltarTuning.pitch("frost_scythe","visuals.sounds.entity-witch-throw",1.0F));
      final float rotX = (ThreadLocalRandom.current().nextFloat() * 0.6F + 0.2F) * (float)(ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
      final float rotY = (ThreadLocalRandom.current().nextFloat() * 0.6F + 0.2F) * (float)(ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
      final float rotZ = (ThreadLocalRandom.current().nextFloat() * 0.6F + 0.2F) * (float)(ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
      final double speed = this.plugin.getWeaponsConfig().getDouble("frost-scythe.ice-command.speed", 1.25);
      (new BukkitRunnable() {
            int life = 0;
            double distanceTraveled = 0.0;

            @Override
            public void run() {
               if (p.isOnline() && !p.isDead() && this.life <= 200 && block.isValid() && !(this.distanceTraveled >= FrostAbilityManager.this.plugin.getWeaponsConfig().getDouble("frost-scythe.ice-command.radius",16.0))) {
                  this.distanceTraveled = this.distanceTraveled + speed;
                  Vector dynamicDir = p.getEyeLocation().getDirection();
                  if (!(dynamicDir.lengthSquared() < 1.0E-4) && !Double.isNaN(dynamicDir.getX())) {
                     dynamicDir.normalize();
                  } else {
                     dynamicDir = new Vector(0, 0, 1);
                  }

                  Vector dynamicRight = dynamicDir.clone().crossProduct(new Vector(0, 1, 0));
                  if (!(dynamicRight.lengthSquared() < 1.0E-4) && !Double.isNaN(dynamicRight.getX())) {
                     dynamicRight.normalize();
                  } else {
                     dynamicRight = new Vector(1, 0, 0);
                  }

                  Location current = p.getLocation()
                     .clone()
                     .add(dynamicRight.clone().multiply(offset.getX()))
                     .add(0.0, offset.getY(), 0.0)
                     .add(dynamicDir.clone().multiply(offset.getZ() + this.distanceTraveled));
                  if (!CombatUtils.clearPath(block.getLocation(),current)) {
                     FrostAbilityManager.this.breakIce(block.getLocation());block.remove();this.cancel();return;
                  }
                  block.setInterpolationDelay(0);
                  block.setInterpolationDuration(1);
                  block.teleport(current);
                  Transformation t = block.getTransformation();
                  t.getLeftRotation().rotateXYZ(rotX, rotY, rotZ);
                  Vector3f center = new Vector3f(0.265F, 0.265F, 0.265F);
                  Vector3f trans = new Vector3f(center);
                  t.getLeftRotation().transform(trans);
                  trans.mul(-1.0F);
                  t.getTranslation().set(trans);
                  block.setTransformation(t);
                  current.getWorld().spawnParticle(Particle.SNOWFLAKE, current,AltarTuning.particles("frost_scythe",10), 0.4, 0.4, 0.4, 0.05);
                  if (!current.getBlock().getType().isSolid()
                     && (
                        FrostAbilityManager.this.plugin.getWorldGuardManager() == null
                           || FrostAbilityManager.this.plugin.getWorldGuardManager().isLocationAllowed(current, p)
                     )) {
                     for (Entity e : current.getWorld().getNearbyEntities(current,AltarTuning.number("frost_scythe","tuning.frost-ability-manager.hit-radius-x",1.0,0,128),AltarTuning.number("frost_scythe","tuning.frost-ability-manager.hit-radius-y",1.0,0,128),AltarTuning.number("frost_scythe","tuning.frost-ability-manager.hit-radius-z",1.0,0,128))) {
                        if (e instanceof LivingEntity victim
                           && e != p
                           && !volleyHits.contains(victim.getUniqueId())
                           && (
                              !(victim instanceof Player targetPlayer)
                                 || targetPlayer.getGameMode() != GameMode.SPECTATOR
                                    && !FrostAbilityManager.this.plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())
                           )
                           && (
                              FrostAbilityManager.this.plugin.getWorldGuardManager() == null
                                 || FrostAbilityManager.this.plugin.getWorldGuardManager().canAffectTarget(p, victim)
                           )) {
                           volleyHits.add(victim.getUniqueId());
                           FrostAbilityManager.this.breakIce(current);
                           CombatUtils.runSyntheticDamage(
                              () -> victim.damage(FrostAbilityManager.this.plugin.getWeaponsConfig().getDouble("frost-scythe.ice-command.damage", 15.0), p)
                           );
                           FrostAbilityManager.this.applyFreeze(p, victim, 3, 1);
                           block.remove();
                           this.cancel();
                           return;
                        }
                     }

                     this.life++;
                  } else {
                     FrostAbilityManager.this.breakIce(current);
                     block.remove();
                     this.cancel();
                  }
               } else {
                  FrostAbilityManager.this.breakIce(block.getLocation());
                  block.remove();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void breakIce(Location loc) {
      loc.getWorld().playSound(loc,AltarTuning.sound("frost_scythe","visuals.sounds.block-glass-break.sound",Sound.BLOCK_GLASS_BREAK),AltarTuning.volume("frost_scythe","visuals.sounds.block-glass-break",1.5F),AltarTuning.pitch("frost_scythe","visuals.sounds.block-glass-break",1.0F));
      Location baseLoc = loc.clone().add(0.0, 0.2, 0.0);
      loc.getWorld().spawnParticle(Particle.SNOWFLAKE, baseLoc,AltarTuning.particles("frost_scythe",25), 0.35, 0.35, 0.35, 0.06);
      ParticleUtils.spawnBlockDispersion(this.plugin, baseLoc, this.blueIceData, 4.2, 85);
   }

   public void applyFreeze(final Player p, final LivingEntity victim, final int seconds, int slownessAmp) {
      this.breakIce(victim.getLocation().add(0.0, 1.0, 0.0));
      victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, seconds * 20, slownessAmp, false, false));
      victim.setFreezeTicks(victim.getMaxFreezeTicks() + seconds * 20);
      (new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
               if (this.ticks < seconds * 20 && !victim.isDead()) {
                  if (this.ticks > 0 && this.ticks % 60 == 0) {
                     CombatUtils.runSyntheticDamage(
                        () -> victim.damage(FrostAbilityManager.this.plugin.getWeaponsConfig().getDouble("frost-scythe.freeze.damage", 2.0), p)
                     );
                     victim.getWorld().playSound(victim.getLocation(),AltarTuning.sound("frost_scythe","visuals.sounds.entity-player-hurt-freeze.sound",Sound.ENTITY_PLAYER_HURT_FREEZE),AltarTuning.volume("frost_scythe","visuals.sounds.entity-player-hurt-freeze",1.0F),AltarTuning.pitch("frost_scythe","visuals.sounds.entity-player-hurt-freeze",1.0F));
                  }

                  this.ticks++;
               } else {
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }
}

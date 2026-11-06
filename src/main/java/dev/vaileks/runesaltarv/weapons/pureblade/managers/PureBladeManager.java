package dev.vaileks.runesaltarv.weapons.pureblade.managers;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.SpeedBuffUtils;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

public class PureBladeManager {
   private final RunesAltarV plugin;
   private final PureBossBarManager bossBarManager;
   private final Random random = new Random();
   private static final double BLADE_PHASE = 0.0;

   public PureBladeManager(RunesAltarV plugin, PureBossBarManager bossBarManager) {
      this.plugin = plugin;
      this.bossBarManager = bossBarManager;
   }

   private void applyPureDamage(LivingEntity victim, Player attacker, double damage) {
      this.plugin.getCleanDamageManager().apply(victim, attacker, damage);
   }

   public void playMassiveSweep(Player p, LivingEntity victim) {
      if (victim instanceof Player targetPlayer && this.plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) {
         return;
      }

      Vector dir = p.getLocation().getDirection().normalize();
      Location center = victim.getLocation().add(0.0, 1.0, 0.0).subtract(dir.clone().multiply(0.8));
      center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.8F);
      Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
      Vector up = right.clone().crossProduct(dir).normalize();
      double rotAngle = Math.random() * Math.PI;
      double cos = Math.cos(rotAngle);
      double sin = Math.sin(rotAngle);
      DustOptions miniWhiteDust = new DustOptions(Color.fromRGB(255, 255, 255), 0.5F);

      for (double i = -1.4; i <= 1.4; i += 0.05) {
         double x = i * cos;
         double y = i * sin;
         double z = -(i * i) * 0.25;
         Vector point = right.clone().multiply(x).add(up.clone().multiply(y)).add(dir.clone().multiply(z));
         Location sparkLoc = center.clone().add(point);
         center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc, 1, 0.0, 0.0, 0.0, 0.0);
         center.getWorld().spawnParticle(Particle.DUST, sparkLoc, 1, 0.0, 0.0, 0.0, 0.0, miniWhiteDust);
      }
   }

   public void castShadeSoul(final Player p) {
      if (!this.bossBarManager.isOnCooldown(p, "ShadeSoul")) {
         this.bossBarManager
            .setCooldown(
               p,
               "ShadeSoul",
               this.plugin.tr("§f§lТень души", "§f§lsʜᴀᴅᴇ ᴏꜰ sᴏᴜʟ"),
               this.plugin.getWeaponsConfig().getInt("pure-blade.shade-soul.cooldown", 45)
            );
         final ItemStack cubeItem = new ItemStack(Material.CLAY_BALL);
         ItemMeta meta = cubeItem.getItemMeta();
         if (meta != null) {
            meta.setCustomModelData(6);
            cubeItem.setItemMeta(meta);
         }

         p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0F, 1.0F);
         (new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
               if (!p.isOnline() || p.isDead()) {
                  this.cancel();
               } else if (this.ticks >= 30) {
                  this.cancel();
                  PureBladeManager.this.fireShadeSoulBeam(p, cubeItem);
               } else {
                  if (Math.random() < 0.3) {
                     final ItemDisplay popCube = (ItemDisplay)p.getWorld().spawnEntity(p.getLocation().add(0.0, 1.0, 0.0), EntityType.ITEM_DISPLAY);
                     popCube.setItemStack(cubeItem);
                     popCube.setTeleportDuration(1);
                     PureBladeManager.this.plugin.getVisualCleanupManager().track(popCube);
                     final float initialSize = 0.2F + PureBladeManager.this.random.nextFloat() * 0.35F;
                     final Vector randomDir = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).normalize().multiply(0.15);
                     (new BukkitRunnable() {
                        float size = initialSize;

                        @Override
                        public void run() {
                           this.size -= 0.015F;
                           if (!(this.size <= 0.0F) && popCube.isValid()) {
                              popCube.teleport(popCube.getLocation().add(randomDir));
                              popCube.setInterpolationDelay(0);
                              popCube.setInterpolationDuration(1);
                              Transformation t = popCube.getTransformation();
                              t.getScale().set(this.size, this.size, this.size);
                              popCube.setTransformation(t);
                           } else {
                              PureBladeManager.this.plugin.getVisualCleanupManager().untrack(popCube);
                              popCube.remove();
                              this.cancel();
                           }
                        }
                     }).runTaskTimer(PureBladeManager.this.plugin, 0L, 1L);
                  }

                  this.ticks++;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void fireShadeSoulBeam(final Player p, final ItemStack cubeItem) {
      p.swingMainHand();

      try {
         Color whiteColor = Color.fromRGB(255, 255, 255);
         p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(0.0, 1.0, 0.0), 3, 0.2, 0.5, 0.2, 0.0, whiteColor);
      } catch (Throwable var10) {
         try {
            p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(0.0, 1.0, 0.0), 3, 0.2, 0.5, 0.2, 0.0);
         } catch (Throwable var9) {
         }
      }

      p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0F, 1.0F);
      final Location startLoc = p.getEyeLocation();

      for (int c = 0; c < 30; c++) {
         int r = 180 + this.random.nextInt(75);
         int g = 220 + this.random.nextInt(35);
         int b = 220 + this.random.nextInt(35);
         DustOptions paleDust = new DustOptions(Color.fromRGB(r, g, b), 1.5F);
         p.getWorld().spawnParticle(Particle.DUST, startLoc, 1, 0.5, 0.5, 0.5, paleDust);
      }

      final Vector dir = startLoc.getDirection().normalize();
      final Set<UUID> hitTargets = new HashSet<>();
      final int numCubes = 20;
      (new BukkitRunnable() {
         int spawned = 0;

         @Override
         public void run() {
            if (p.isOnline() && !p.isDead() && this.spawned < numCubes) {
               for (int i = 0; i < 2 && this.spawned < numCubes; i++) {
                  double dist = (double)(this.spawned + 1) * 1.35;
                  Location cubeStartLoc = startLoc.clone().add(dir.clone().multiply(dist));
                  PureBladeManager.this.spawnShadeSoulCube(p, cubeStartLoc, 0.65F, cubeItem, hitTargets, dir);
                  if (PureBladeManager.this.random.nextBoolean()) {
                     Location smallLoc = cubeStartLoc.clone().add((Math.random() - 0.5) * 1.3, (Math.random() - 0.5) * 1.3, (Math.random() - 0.5) * 1.3);
                     float smallScale = 0.2F + PureBladeManager.this.random.nextFloat() * 0.2F;
                     PureBladeManager.this.spawnShadeSoulCube(p, smallLoc, smallScale, cubeItem, hitTargets, dir);
                  }

                  this.spawned++;
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void spawnShadeSoulCube(final Player p, Location baseLoc, final float scaleStart, ItemStack item, final Set<UUID> hitTargets, final Vector launchDir) {
      final Location currentLoc = baseLoc.clone().add((Math.random() - 0.5) * 0.4, (Math.random() - 0.5) * 0.4, (Math.random() - 0.5) * 0.4);
      final ItemDisplay display = (ItemDisplay)p.getWorld().spawnEntity(currentLoc, EntityType.ITEM_DISPLAY);
      display.setItemStack(item);
      display.setInterpolationDelay(0);
      display.setInterpolationDuration(3);
      display.setTeleportDuration(3);
      this.plugin.getVisualCleanupManager().track(display);
      Transformation t = display.getTransformation();
      t.getScale().set(scaleStart, scaleStart, scaleStart);
      display.setTransformation(t);
      final double yDrift = (double)(this.random.nextBoolean() ? 1 : -1) * (0.008 + this.random.nextDouble() * 0.012);
      Vector sidewaysBase = new Vector(-launchDir.getZ(), 0.0, launchDir.getX());
      if (sidewaysBase.lengthSquared() > 1.0E-4) {
         sidewaysBase.normalize();
      }

      final Vector sideways;
      if (this.random.nextDouble() < 0.4) {
         sideways = sidewaysBase.multiply((double)(this.random.nextBoolean() ? 1 : -1) * (0.006 + this.random.nextDouble() * 0.01));
      } else {
         sideways = new Vector();
      }

      final int evaporateDelay = 25 + this.random.nextInt(20);
      final DustOptions blackDust = new DustOptions(Color.fromRGB(0, 0, 0), 1.6F);
      (new BukkitRunnable() {
            int life = 0;
            float scale = scaleStart;

            @Override
            public void run() {
               if (!(this.scale <= 0.0F) && display.isValid()) {
                  if (this.life < evaporateDelay) {
                     currentLoc.add(0.0, yDrift, 0.0);
                     currentLoc.add(sideways);
                     if (this.life % 6 == 0) {
                        currentLoc.add(0.0, Math.sin((double)this.life * 0.35) * 0.01, 0.0);
                     }

                     display.setInterpolationDelay(0);
                     display.setInterpolationDuration(3);
                     display.setTeleportDuration(3);
                     display.teleport(currentLoc);
                     if (Math.random() > 0.5) {
                        currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 1, 0.3, 0.3, 0.3, blackDust);
                     }
                  } else {
                     this.scale -= 0.05F;
                     display.setInterpolationDelay(0);
                     display.setInterpolationDuration(3);
                     display.setTeleportDuration(3);
                     currentLoc.add(launchDir.clone().multiply(-0.04));
                     currentLoc.add(0.0, yDrift, 0.0);
                     currentLoc.add(sideways.clone().multiply(0.5));
                     display.teleport(currentLoc);
                     Transformation trans = display.getTransformation();
                     trans.getScale().set(Math.max(0.01F, this.scale), Math.max(0.01F, this.scale), Math.max(0.01F, this.scale));
                     display.setTransformation(trans);
                  }

                  for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                     if (e instanceof LivingEntity) {
                        LivingEntity victim = (LivingEntity)e;
                        if (!victim.equals(p) && !hitTargets.contains(victim.getUniqueId())) {
                           if (victim instanceof Player) {
                              Player targetPlayer = (Player)victim;
                              if (targetPlayer.getGameMode() == GameMode.SPECTATOR
                                 || PureBladeManager.this.plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) {
                                 continue;
                              }
                           }

                           PureBladeManager.this.applyPureDamage(
                              victim, p, PureBladeManager.this.plugin.getWeaponsConfig().getDouble("pure-blade.shade-soul.damage", 8.0)
                           );
                           int durationTicks = PureBladeManager.this.plugin.getWeaponsConfig().getInt("pure-blade.shade-soul.effect-duration", 5) * 20;
                           int slowAmp = PureBladeManager.this.plugin.getWeaponsConfig().getInt("pure-blade.shade-soul.slowness-amplifier", 1);
                           victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0, false, false));
                           victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, slowAmp, false, false));
                           p.getWorld().playSound(victim.getLocation(), Sound.ENTITY_VEX_HURT, 1.0F, 0.5F);
                           hitTargets.add(victim.getUniqueId());
                        }
                     }
                  }

                  this.life++;
               } else {
                  PureBladeManager.this.plugin.getVisualCleanupManager().untrack(display);
                  display.remove();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   public void castCycloneSlash(final Player p) {
      if (!this.bossBarManager.isOnCooldown(p, "CycloneSlash")) {
         this.bossBarManager
            .setCooldown(
               p,
               "CycloneSlash",
               this.plugin.tr("§f§lВихревой Разрез", "§f§lᴄʏᴄʟᴏɴᴇ sʟᴀsʜ"),
               this.plugin.getWeaponsConfig().getInt("pure-blade.cyclone-slash.cooldown", 30)
            );
         p.getWorld().playSound(p.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1.0F, 1.0F);
         int duration = this.plugin.getWeaponsConfig().getInt("pure-blade.cyclone-slash.speed-duration", 5) * 20;
         int amplifier = this.plugin.getWeaponsConfig().getInt("pure-blade.cyclone-slash.speed-amplifier", 10);
         SpeedBuffUtils.applyBurstSpeed(p, duration, amplifier, this.plugin);
         ItemStack slashItem = new ItemStack(Material.CLAY_BALL);
         ItemMeta meta = slashItem.getItemMeta();
         if (meta != null) {
            meta.setCustomModelData(7);
            slashItem.setItemMeta(meta);
         }

         Location spawnLoc = p.getLocation();
         spawnLoc.setPitch(0.0F);
         spawnLoc.setYaw(0.0F);
         final ItemDisplay display = p.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
            d.setItemStack(slashItem);
            d.setItemDisplayTransform(ItemDisplayTransform.FIXED);
            d.setInterpolationDuration(1);
            d.setInterpolationDelay(0);
            Transformation initialTrans = d.getTransformation();
            initialTrans.getScale().set(12.0F, 12.0F, 12.0F);
            initialTrans.getTranslation().set(0.0F, 4.95F, 0.0F);
            d.setTransformation(initialTrans);
         });
         p.addPassenger(display);
         this.plugin.getVisualCleanupManager().track(display);
         (new BukkitRunnable() {
               int ticks = 0;
               float entityYaw = 0.0F;
               float currentScale = 12.0F;
               float minScale = 3.0F;
               int shrinkDuration = 40;
               int totalDuration = 50;

               @Override
               public void run() {
                  if (p.isOnline() && !p.isDead() && this.ticks <= this.totalDuration && display.isValid()) {
                     if (this.ticks % 2 == 0) {
                        float randomPitch = 0.8F + (float)(Math.random() * 0.3F);
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 1.0F, randomPitch);
                     }

                     if (this.ticks <= this.shrinkDuration) {
                        this.currentScale = this.currentScale - (12.0F - this.minScale) / (float)this.shrinkDuration;
                     }

                     this.entityYaw += 65.0F;
                     if (this.entityYaw >= 360.0F) {
                        this.entityYaw -= 360.0F;
                     }

                     display.setInterpolationDelay(0);
                     display.setInterpolationDuration(1);
                     float yTranslation = 0.45F + (this.currentScale - this.minScale) * 0.5F;
                     Transformation t = display.getTransformation();
                     t.getScale().set(this.currentScale, this.currentScale, this.currentScale);
                     t.getTranslation().set(0.0F, yTranslation, 0.0F);
                     t.getLeftRotation().set(new Quaternionf().rotateY((float)Math.toRadians((double)this.entityYaw)));
                     display.setTransformation(t);
                     Location baseLoc = p.getLocation();
                     baseLoc.setPitch(0.0F);
                     double particleRadius = (double)this.currentScale / 2.0 + 0.2;
                     double fixedParticleY = baseLoc.getY() + 1.25;
                     double curA = -Math.toRadians((double)this.entityYaw) + 0.0;
                     double sweep = Math.toRadians(65.0);
                     int trailSteps = 14;

                     for (int side = 0; side < 2; side++) {
                        double tipA = curA + (double)side * Math.PI;

                        for (int s = 0; s < trailSteps; s++) {
                           double f = (double)s / (double)trailSteps;
                           double a = tipA + sweep * f;
                           double r = particleRadius * (1.0 - f * 0.06) + f * f * 0.28;
                           double cos = Math.cos(a);
                           double sin = Math.sin(a);
                           double px = baseLoc.getX() + cos * r;
                           double py = fixedParticleY + 0.05 + Math.sin(f * Math.PI) * 0.06;
                           double pz = baseLoc.getZ() + sin * r;
                           Location trailLoc = new Location(p.getWorld(), px, py, pz);
                           double tangential = 0.12 * (1.0 - f * 0.5);
                           double outward = 0.04 + f * 0.05;
                           double vx = sin * tangential + cos * outward;
                           double vz = -cos * tangential + sin * outward;
                           if (s == 0) {
                              p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, trailLoc, 1, 0.0, 0.0, 0.0, 0.0);
                              p.getWorld().spawnParticle(Particle.CRIT, trailLoc, 1, 0.0, 0.0, 0.0, 0.0);
                           } else if (s % 2 == 0) {
                              p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, trailLoc, 1, vx * 0.3, 0.01, vz * 0.3, 0.01);
                           } else {
                              p.getWorld().spawnParticle(Particle.CRIT, trailLoc, 1, vx * 0.3, 0.01, vz * 0.3, 0.01);
                           }
                        }
                     }

                     for (int c = 0; c < 4; c++) {
                        double randAngle = Math.random() * Math.PI * 2.0;
                        double randR = particleRadius * (0.35 + Math.random() * 0.75);
                        double cx = baseLoc.getX() + Math.cos(randAngle) * randR;
                        double cy = fixedParticleY + (Math.random() - 0.45) * 1.2;
                        double cz = baseLoc.getZ() + Math.sin(randAngle) * randR;
                        Location cloudLoc = new Location(p.getWorld(), cx, cy, cz);
                        p.getWorld().spawnParticle(Particle.CLOUD, cloudLoc, 1, 0.08, 0.05, 0.08, 0.02);
                     }

                     if (this.ticks % 2 == 0) {
                        int checkRad = (int)Math.ceil(particleRadius);

                        for (int x = -checkRad; x <= checkRad; x++) {
                           for (int z = -checkRad; z <= checkRad; z++) {
                              for (int y = 0; y <= 2; y++) {
                                 Block b = baseLoc.clone().add((double)x, (double)y, (double)z).getBlock();
                                 if (PureBladeManager.this.isPlantOrWeb(b.getType())
                                    && b.getLocation().distance(baseLoc.clone().add(0.0, (double)y, 0.0)) <= particleRadius + 0.5) {
                                    b.breakNaturally();
                                 }
                              }
                           }
                        }
                     }

                     if (this.ticks % 10 == 0) {
                        for (Entity e : p.getWorld().getNearbyEntities(baseLoc.clone().add(0.0, 1.1, 0.0), particleRadius + 1.0, 2.0, particleRadius + 1.0)) {
                           if (e instanceof LivingEntity) {
                              LivingEntity victim = (LivingEntity)e;
                              if (!victim.equals(p)) {
                                 if (victim instanceof Player) {
                                    Player targetPlayer = (Player)victim;
                                    if (targetPlayer.getGameMode() == GameMode.SPECTATOR
                                       || PureBladeManager.this.plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) {
                                       continue;
                                    }
                                 }

                                 CombatUtils.runSyntheticDamage(
                                    () -> victim.damage(PureBladeManager.this.plugin.getWeaponsConfig().getDouble("pure-blade.cyclone-slash.damage", 10.0), p)
                                 );
                              }
                           }
                        }
                     }

                     this.ticks++;
                  } else {
                     p.removePassenger(display);
                     PureBladeManager.this.plugin.getVisualCleanupManager().untrack(display);
                     display.remove();
                     if (p.isOnline()) {
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 1.0F, 1.0F);
                     }

                     this.cancel();
                  }
               }
            })
            .runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private boolean isPlantOrWeb(Material m) {
      if (m.isAir() || m == Material.GRASS_BLOCK || m == Material.DIRT || m == Material.PODZOL || m == Material.MYCELIUM) {
         return false;
      } else if (m == Material.COBWEB) {
         return true;
      } else {
         String name = m.name();
         return !name.contains("FLOWER")
               && !name.contains("FERN")
               && !name.contains("BUSH")
               && !name.contains("BERRY")
               && !name.contains("VINE")
               && !name.contains("MUSHROOM")
               && !name.contains("FUNGUS")
               && !name.contains("ROOTS")
               && !name.contains("SPROUTS")
               && !name.contains("LILY")
               && !name.contains("DRIPLEAF")
               && !name.contains("LICHEN")
               && !name.contains("SAPLING")
               && !name.contains("BAMBOO")
               && !name.contains("CACTUS")
               && !name.contains("SUGAR_CANE")
               && !name.contains("KELP")
               && !name.contains("SEAGRASS")
               && !name.contains("WHEAT")
               && !name.contains("CARROT")
               && !name.contains("POTATO")
               && !name.contains("BEETROOT")
               && (!name.contains("GRASS") || m == Material.GRASS_BLOCK)
            ? !m.isSolid()
               && !name.contains("WATER")
               && !name.contains("LAVA")
               && m != Material.FIRE
               && m != Material.SOUL_FIRE
               && m != Material.LIGHT
               && !name.contains("SIGN")
               && !name.contains("BANNER")
               && !name.contains("TORCH")
               && !name.contains("LANTERN")
               && !name.contains("CAMPFIRE")
               && !name.contains("TRAPDOOR")
               && !name.contains("DOOR")
               && !name.contains("BUTTON")
               && !name.contains("PRESSURE_PLATE")
               && !name.contains("RAIL")
               && !name.contains("REDSTONE")
            : true;
      }
   }

   public void resetCooldowns(Player player) {
      this.bossBarManager.resetPlayer(player);
   }
}

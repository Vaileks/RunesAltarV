package dev.vaileks.runesaltarv.weapons.witherblade.tasks;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.SpeedBuffUtils;
import dev.vaileks.runesaltarv.weapons.witherblade.managers.WitherManager;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class WitherTasks {
   private static final Random random = new Random();
   private static final DustOptions BLACK_DUST = new DustOptions(Color.fromRGB(0, 0, 0), 1.5F);
   public static final Set<UUID> activeSlimeTraps = new HashSet<>();

   public static void castDash(final RunesAltarV plugin, final Player p, WitherManager manager) {
      if (manager.getDashCharges(p) > 0) {
         manager.useDashCharge(p);
         p.swingMainHand();
         p.playSound(p.getLocation(),AltarTuning.sound("wither_blade","visuals.sounds.entity-wither-shoot.sound",Sound.ENTITY_WITHER_SHOOT),AltarTuning.volume("wither_blade","visuals.sounds.entity-wither-shoot",1.0F),AltarTuning.pitch("wither_blade","visuals.sounds.entity-wither-shoot",1.2F));
         p.playSound(p.getLocation(),AltarTuning.sound("wither_blade","visuals.sounds.entity-wither-skeleton-hurt.sound",Sound.ENTITY_WITHER_SKELETON_HURT),AltarTuning.volume("wither_blade","visuals.sounds.entity-wither-skeleton-hurt",1.0F),AltarTuning.pitch("wither_blade","visuals.sounds.entity-wither-skeleton-hurt",1.0F));
         double velocity = plugin.getWeaponsConfig().getDouble("wither-blade.dash.velocity", 1.5);
         p.setVelocity(p.getLocation().getDirection().normalize().multiply(velocity));
         int duration = plugin.getWeaponsConfig().getInt("wither-blade.dash.speed-duration", 4) * 20;
         int amplifier = plugin.getWeaponsConfig().getInt("wither-blade.dash.speed-amplifier", 1);
         SpeedBuffUtils.applyBurstSpeed(p, duration, amplifier, plugin);
         (new BukkitRunnable() {
               int ticks = 0;
               final Set<UUID> hitDuringDash = new HashSet<>();

               @Override
               public void run() {
                  if (this.ticks <= 15 && p.isOnline() && (this.ticks <= 5 || !p.isOnGround())) {
                     Location loc = p.getLocation().add(0.0, 0.8, 0.0);
                     loc.getWorld().spawnParticle(Particle.DUST, loc,AltarTuning.particles("wither_blade",8), 0.3, 0.3, 0.3, 0.1, WitherTasks.BLACK_DUST);

                     for (Entity entity : p.getWorld().getNearbyEntities(p.getLocation(),AltarTuning.number("wither_blade","tuning.wither-tasks.hit-radius-x",1.5,0,128),AltarTuning.number("wither_blade","tuning.wither-tasks.hit-radius-y",1.5,0,128),AltarTuning.number("wither_blade","tuning.wither-tasks.hit-radius-z",1.5,0,128))) {
                        if (entity instanceof LivingEntity) {
                           LivingEntity target = (LivingEntity)entity;
                           if (target != p && !this.hitDuringDash.contains(target.getUniqueId())) {
                              if (target instanceof Player) {
                                 Player targetPlayer = (Player)target;
                                 if (targetPlayer.getGameMode() == GameMode.SPECTATOR
                                    || plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) {
                                    continue;
                                 }
                              }

                              this.hitDuringDash.add(target.getUniqueId());
                              target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.wither.duration-ticks",120,1,72000), AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.wither.amplifier",3,0,10), false, false));
                           }
                        }
                     }

                     final ItemDisplay drop = loc.getWorld().spawn(loc, ItemDisplay.class);
                     plugin.getVisualCleanupManager().track(drop);
                     drop.setItemStack(new ItemStack(Material.BONE));
                     drop.setItemDisplayTransform(ItemDisplayTransform.FIXED);
                     drop.setInterpolationDuration(1);
                     drop.setTeleportDuration(1);
                     Transformation t = drop.getTransformation();
                     t.getScale().set(0.5F, 0.5F, 0.5F);
                     drop.setTransformation(t);
                     final Vector3f rot = new Vector3f(
                        WitherTasks.random.nextFloat() * 0.3F, WitherTasks.random.nextFloat() * 0.3F, WitherTasks.random.nextFloat() * 0.3F
                     );
                     final Vector v = new Vector(
                        (WitherTasks.random.nextDouble() - 0.5) * 0.25, WitherTasks.random.nextDouble() * 0.2, (WitherTasks.random.nextDouble() - 0.5) * 0.25
                     );
                     (new BukkitRunnable() {
                        int life = 0;

                        @Override
                        public void run() {
                           Location c = drop.getLocation();
                           v.add(new Vector(0.0, -0.02, 0.0));
                           c.add(v);
                           Transformation trans = drop.getTransformation();
                           trans.getLeftRotation().rotateXYZ(rot.x, rot.y, rot.z);
                           drop.setTransformation(trans);
                           drop.teleport(c);
                           if (c.getY() < -64.0 || this.life++ > 40) {
                              drop.remove();
                              this.cancel();
                           }
                        }
                     }).runTaskTimer(plugin, 0L, 1L);
                     this.ticks++;
                  } else {
                     this.cancel();
                  }
               }
            })
            .runTaskTimer(plugin, 0L, 1L);
      }
   }

   public static void castAura(RunesAltarV plugin, final Player p, WitherManager manager) {
      int charge = manager.getAttackCharge(p);
      if (charge <= 0) {
         p.playSound(p.getLocation(),AltarTuning.sound("wither_blade","visuals.sounds.entity-wither-break-block.sound",Sound.ENTITY_WITHER_BREAK_BLOCK),AltarTuning.volume("wither_blade","visuals.sounds.entity-wither-break-block",0.7F),AltarTuning.pitch("wither_blade","visuals.sounds.entity-wither-break-block",1.2F));
      } else {
         p.swingMainHand();
         manager.setAuraCooldown(p);
         manager.resetAttackCharge(p);
         p.getWorld().playSound(p.getLocation(),AltarTuning.sound("wither_blade","visuals.sounds.entity-wither-ambient.sound",Sound.ENTITY_WITHER_AMBIENT),AltarTuning.volume("wither_blade","visuals.sounds.entity-wither-ambient",1.5F),AltarTuning.pitch("wither_blade","visuals.sounds.entity-wither-ambient",0.8F));
         p.getWorld().playSound(p.getLocation(),AltarTuning.sound("wither_blade","visuals.sounds.entity-slime-jump.sound",Sound.ENTITY_SLIME_JUMP),AltarTuning.volume("wither_blade","visuals.sounds.entity-slime-jump",1.0F),AltarTuning.pitch("wither_blade","visuals.sounds.entity-slime-jump",0.5F));
         double maxDamage = plugin.getWeaponsConfig().getDouble("wither-blade.aura.max-damage", 10.0);
         final double range = Math.min(Math.max(1.0,(double)charge),plugin.getWeaponsConfig().getDouble("wither-blade.aura.radius",4.0));
         (new BukkitRunnable() {
            double currentRadius = 0.0;

            @Override
            public void run() {
               if (this.currentRadius >= range) {
                  this.cancel();
               } else {
                  this.currentRadius++;
                  int particles = (int)(this.currentRadius * 40.0);

                  for (int i = 0; i < particles; i++) {
                     double r = this.currentRadius - WitherTasks.random.nextDouble() * 1.5;
                     if (r < 0.0) {
                        r = 0.0;
                     }

                     double angle = WitherTasks.random.nextDouble() * 2.0 * Math.PI;
                     double x = Math.cos(angle) * r;
                     double z = Math.sin(angle) * r;
                     double y = WitherTasks.random.nextDouble() * 2.0;
                     p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(x, y, z),AltarTuning.particles("wither_blade",2), 0.4, 0.4, 0.4, 0.0, WitherTasks.BLACK_DUST);
                  }
               }
            }
         }).runTaskTimer(plugin, 0L, 1L);

         for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity) {
               LivingEntity victim = (LivingEntity)e;
               if (e != p) {
                  if (victim instanceof Player) {
                     Player targetPlayer = (Player)victim;
                     if (targetPlayer.getGameMode() == GameMode.SPECTATOR || plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) {
                        continue;
                     }
                  }

                  double dist = p.getLocation().distance(victim.getLocation());
                  if (!(dist > range) && CombatUtils.clearPath(p.getEyeLocation(),victim)) {
                     double distFactor = Math.max(0.1, 1.0 - dist / range);
                     if (dist < 2.0) {
                        distFactor = 1.0;
                     }

                     double chargeFactor = (double)charge / 30.0;
                     double finalDamage = maxDamage * chargeFactor * distFactor;
                     int witherTicks = (int)(plugin.getWeaponsConfig().getInt("wither-blade.aura.wither-duration-ticks",60) * chargeFactor * distFactor);
                     plugin.getCleanDamageManager().apply(victim, p, finalDamage);
                     victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherTicks, plugin.getWeaponsConfig().getInt("wither-blade.aura.wither-amplifier",0), false, false));
                     victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, witherTicks, AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.slowness.amplifier",1,0,10), false, false));
                     victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("wither_blade",1));
                  }
               }
            }
         }

         int baseSlimeCount = Math.min(35, charge + 15);
         int slimeCount = (int)((double)baseSlimeCount * 1.4);

         for (int i = 0; i < slimeCount; i++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            boolean isFlowing = random.nextDouble() < 0.4;
            Location startLoc = p.getLocation().add(0.0, 0.3 + random.nextDouble() * 1.2, 0.0);
            Vector dir;
            if (isFlowing) {
               double speed = 0.1 + random.nextDouble() * 0.15;
               dir = new Vector(Math.cos(angle) * speed, -0.1 - random.nextDouble() * 0.2, Math.sin(angle) * speed);
            } else {
               double speed = 0.4 + random.nextDouble() * 0.4;
               dir = new Vector(Math.cos(angle) * speed, 0.2 + random.nextDouble() * 0.6, Math.sin(angle) * speed);
            }

            launchSlimeProjectile(plugin, p, startLoc, dir, range, false);
         }
      }
   }

   public static void launchSlimeProjectile(
      final RunesAltarV plugin, final Player shooter, final Location start, final Vector dir, final double maxDist, final boolean isDrip
   ) {
      final ItemDisplay slime = start.getWorld().spawn(start, ItemDisplay.class);
      plugin.getVisualCleanupManager().track(slime);
      ItemStack slimeItem = new ItemStack(Material.FEATHER);
      ItemMeta meta = slimeItem.getItemMeta();
      if (meta != null) {
         meta.setCustomModelData(4);
         slimeItem.setItemMeta(meta);
      }

      slime.setItemStack(slimeItem);
      slime.setItemDisplayTransform(ItemDisplayTransform.FIXED);
      slime.setTeleportDuration(1);
      slime.setInterpolationDuration(1);
      Transformation t = slime.getTransformation();
      t.getScale().set(0.3F, 0.3F, 0.3F);
      t.getTranslation().set(-0.15F, -0.15F, -0.15F);
      slime.setTransformation(t);
      (new BukkitRunnable() {
            double traveled = 0.0;
            Location curr = start.clone();

            @Override
            public void run() {
               if (shooter.isOnline() && !(this.traveled >= maxDist) && slime.isValid() && !(this.curr.getY() < -64.0)) {
                  Location prevLoc = this.curr.clone();
                  dir.setY(dir.getY() - 0.05);
                  this.curr.add(dir);
                  this.traveled = this.traveled + dir.length();
                  slime.teleport(this.curr);
                  Transformation flyTransform = slime.getTransformation();
                  flyTransform.getLeftRotation().rotateXYZ(0.1F, 0.2F, 0.15F);
                  slime.setTransformation(flyTransform);
                  if (this.traveled % 1.0 < dir.length()) {
                     this.curr.getWorld().spawnParticle(Particle.DUST, this.curr,AltarTuning.particles("wither_blade",1), 0.1, 0.1, 0.1, 0.0, WitherTasks.BLACK_DUST);
                  }

                  RayTraceResult ray = this.curr.getWorld().rayTraceBlocks(prevLoc, dir.clone().normalize(), dir.length(), FluidCollisionMode.NEVER, true);
                  if (ray != null && ray.getHitBlock() != null) {
                     WitherTasks.createSlimeTrap(plugin, shooter, slime, ray.getHitPosition().toLocation(this.curr.getWorld()), ray.getHitBlockFace(), isDrip);
                     this.cancel();
                  } else {
                     for (Entity e : this.curr.getWorld().getNearbyEntities(this.curr,AltarTuning.number("wither_blade","tuning.wither-tasks.hit-radius-x-2",0.6,0,128),AltarTuning.number("wither_blade","tuning.wither-tasks.hit-radius-y-2",0.6,0,128),AltarTuning.number("wither_blade","tuning.wither-tasks.hit-radius-z-2",0.6,0,128))) {
                        if (e instanceof LivingEntity victim
                           && !victim.equals(shooter)
                           && (
                              !(victim instanceof Player p)
                                 || p.getGameMode() != GameMode.SPECTATOR && !plugin.getFriendManager().isFriend(shooter.getUniqueId(), p.getUniqueId())
                           )) {
                           WitherTasks.stickSlimeToEntity(plugin, shooter, slime, victim);
                           this.cancel();
                           return;
                        }
                     }
                  }
               } else {
                  slime.remove();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(plugin, 0L, 1L);
   }

   private static void createSlimeTrap(
      final RunesAltarV plugin, final Player shooter, final ItemDisplay slime, Location loc, BlockFace face, boolean isDrip
   ) {
      final Location offsetLoc = loc.clone().add(face.getDirection().multiply(0.01));
      slime.teleport(offsetLoc);
      slime.getWorld().playSound(offsetLoc,AltarTuning.sound("wither_blade","visuals.sounds.entity-slime-squish.sound",Sound.ENTITY_SLIME_SQUISH),AltarTuning.volume("wither_blade","visuals.sounds.entity-slime-squish",1.0F),AltarTuning.pitch("wither_blade","visuals.sounds.entity-slime-squish",0.8F));
      slime.getWorld().spawnParticle(Particle.DUST, offsetLoc,AltarTuning.particles("wither_blade",10), 0.15, 0.15, 0.15, 0.0, BLACK_DUST);
      slime.setInterpolationDelay(0);
      slime.setInterpolationDuration(6);
      Transformation t = slime.getTransformation();
      float thick = 0.03F + random.nextFloat() * 0.03F;
      float stretch1 = 0.5F + random.nextFloat() * 0.3F;
      float stretch2 = 0.5F + random.nextFloat() * 0.3F;
      t.getLeftRotation().identity();
      t.getRightRotation().identity();
      if (face == BlockFace.UP || face == BlockFace.DOWN) {
         t.getScale().set(stretch1, thick, stretch2);
      } else if (face != BlockFace.NORTH && face != BlockFace.SOUTH) {
         t.getScale().set(thick, stretch1, stretch2);
      } else {
         t.getScale().set(stretch1, stretch2, thick);
      }

      t.getTranslation().set(-t.getScale().x() / 2.0F, -t.getScale().y() / 2.0F, -t.getScale().z() / 2.0F);
      slime.setTransformation(t);
      if (!isDrip) {
         Location checkLoc = offsetLoc.clone().add(0.0, -0.6, 0.0);
         if (!checkLoc.getBlock().getType().isSolid()) {
            Vector dripDir = new Vector(0.0, -0.15, 0.0);
            launchSlimeProjectile(plugin, shooter, checkLoc, dripDir, 8.0, true);
         }
      }

      final UUID trapId = slime.getUniqueId();
      activeSlimeTraps.add(trapId);
      (new BukkitRunnable() {
            int life = 0;

            @Override
            public void run() {
               if (this.life < 200 && slime.isValid()) {
                  if (this.life % 10 == 0) {
                     offsetLoc.getWorld().spawnParticle(Particle.DUST, offsetLoc,AltarTuning.particles("wither_blade",1), 0.2, 0.2, 0.2, 0.0, WitherTasks.BLACK_DUST);
                  }

                  for (Entity e : offsetLoc.getWorld().getNearbyEntities(offsetLoc,AltarTuning.number("wither_blade","tuning.wither-tasks.hit-radius-x-3",0.7,0,128),AltarTuning.number("wither_blade","tuning.wither-tasks.hit-radius-y-3",0.7,0,128),AltarTuning.number("wither_blade","tuning.wither-tasks.hit-radius-z-3",0.7,0,128))) {
                     if (e instanceof LivingEntity victim
                        && !victim.equals(shooter)
                        && (
                           !(victim instanceof Player targetPlayer)
                              || targetPlayer.getGameMode() != GameMode.SPECTATOR
                                 && !plugin.getFriendManager().isFriend(shooter.getUniqueId(), targetPlayer.getUniqueId())
                        )) {
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.wither.duration-ticks-100",100,1,72000), AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.wither.amplifier-1",1,0,10), false, false));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.slowness.duration-ticks",100,1,72000), AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.slowness.amplifier",1,0,10), false, false));
                        victim.getWorld().playSound(victim.getLocation(),AltarTuning.sound("wither_blade","visuals.sounds.entity-slime-hurt.sound",Sound.ENTITY_SLIME_HURT),AltarTuning.volume("wither_blade","visuals.sounds.entity-slime-hurt",1.0F),AltarTuning.pitch("wither_blade","visuals.sounds.entity-slime-hurt",1.0F));
                        WitherTasks.activeSlimeTraps.remove(trapId);
                        slime.remove();
                        this.cancel();
                        return;
                     }
                  }

                  this.life++;
               } else {
                  WitherTasks.activeSlimeTraps.remove(trapId);
                  slime.remove();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(plugin, 0L, 1L);
   }

   private static void stickSlimeToEntity(RunesAltarV plugin, Player shooter, final ItemDisplay slime, final LivingEntity victim) {
      victim.getWorld().playSound(victim.getLocation(),AltarTuning.sound("wither_blade","visuals.sounds.entity-slime-attack.sound",Sound.ENTITY_SLIME_ATTACK),AltarTuning.volume("wither_blade","visuals.sounds.entity-slime-attack",1.0F),AltarTuning.pitch("wither_blade","visuals.sounds.entity-slime-attack",1.0F));
      (new BukkitRunnable() {
         int life = 0;

         @Override
         public void run() {
            if (this.life < 100 && victim.isValid() && !victim.isDead() && slime.isValid()) {
               slime.teleport(victim.getLocation().add(0.0, victim.getHeight() / 2.0, 0.0));
               if (this.life % 20 == 0) {
                  victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.wither.duration-ticks-40",40,1,72000), AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.wither.amplifier-1",1,0,10), false, false));
                  victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.slowness.duration-ticks-40",40,1,72000), AltarTuning.integer("wither_blade","tuning.wither-tasks.effects.slowness.amplifier",1,0,10), false, false));
                  victim.getWorld().playSound(victim.getLocation(),AltarTuning.sound("wither_blade","visuals.sounds.entity-slime-squish.sound",Sound.ENTITY_SLIME_SQUISH),AltarTuning.volume("wither_blade","visuals.sounds.entity-slime-squish",0.5F),AltarTuning.pitch("wither_blade","visuals.sounds.entity-slime-squish",1.5F));
                  victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("wither_blade",5), 0.3, 0.3, 0.3, 0.0, WitherTasks.BLACK_DUST);
               }

               this.life++;
            } else {
               slime.remove();
               this.cancel();
            }
         }
      }).runTaskTimer(plugin, 0L, 1L);
   }
}

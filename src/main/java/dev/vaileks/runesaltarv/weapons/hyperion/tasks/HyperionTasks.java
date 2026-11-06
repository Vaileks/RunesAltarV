package dev.vaileks.runesaltarv.weapons.hyperion.tasks;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.weapons.hyperion.managers.HyperionCooldownManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class HyperionTasks {
   public static final Set<UUID> hallowedVictims = new HashSet<>();

   public static void ignite(LivingEntity victim, Player attacker) {
      if (!hallowedVictims.contains(victim.getUniqueId()))
         new HallowedFlamesTask(victim,attacker).runTaskTimer(RunesAltarV.getInstance(),0L,1L);
   }

   public static class HallowedFlamesTask extends BukkitRunnable {
      private final LivingEntity target;
      private final Player attacker;
      private int ticks = 0;
      private BossBar bar;

      public HallowedFlamesTask(LivingEntity target) {
         this(target, null);
      }

      public HallowedFlamesTask(LivingEntity target, Player attacker) {
         this.target = target;
         this.attacker = attacker;
         HyperionTasks.hallowedVictims.add(target.getUniqueId());
         if (target instanceof Player p) {
            this.bar = TextUtils.bossBar(
               RunesAltarV.getInstance().tr("§6§l!! §c§lСВЯТОЕ ПЛАМЯ §6§l!!", "§6§l!! §c§lHOLY FLAME §6§l!!"), BarColor.RED, BarStyle.SOLID
            );
            this.bar.addPlayer(p);
         }
      }

      @Override
      public void run() {
         if (this.ticks < RunesAltarV.getInstance().getWeaponsConfig().getInt("hyperion.hallowed-flames.duration-ticks",60) && !this.target.isDead() && this.target.isValid()) {
            this.target.setFireTicks(20);
            if (this.ticks % 20 == 0) {
               double reducedFireDamage = RunesAltarV.getInstance().getWeaponsConfig().getDouble("hyperion.hallowed-flames.damage-per-second",0.6);
               if (this.attacker != null && this.attacker.isValid()) {
                  RunesAltarV.getInstance().getCleanDamageManager().apply(this.target, this.attacker, reducedFireDamage);
               } else {
                  CombatUtils.runSyntheticDamage(() -> this.target.damage(reducedFireDamage));
               }

               this.target.getWorld().playSound(this.target.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0F, 0.6F);
            }

            if (this.bar != null) {
               this.bar.setProgress(Math.max(0.0, 1.0 - (double)this.ticks / Math.max(1,RunesAltarV.getInstance().getWeaponsConfig().getInt("hyperion.hallowed-flames.duration-ticks",60))));
            }

            if (this.ticks % 4 == 0) {
               this.target.getWorld().spawnParticle(Particle.FLAME, this.target.getLocation().add(0.0, 1.0, 0.0), 4, 0.3, 0.4, 0.3, 0.02);
               this.target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, this.target.getLocation().add(0.0, 1.0, 0.0), 3, 0.3, 0.4, 0.3, 0.02);
            }

            this.ticks++;
         } else {
            if (this.bar != null) {
               this.bar.removeAll();
            }

            this.target.setFireTicks(0);
            HyperionTasks.hallowedVictims.remove(this.target.getUniqueId());
            this.cancel();
         }
      }
   }

   public static class HolyLanceTask extends BukkitRunnable {
      private final Player shooter;
      private final Location targetLoc;
      private int ticks = 0;
      private ItemDisplay lance;

      public HolyLanceTask(Player shooter) {
         this.shooter = shooter;
         Entity targetEntity = shooter.getTargetEntity(30);
         Block targetBlock = shooter.getTargetBlockExact(30, FluidCollisionMode.NEVER);
         if (targetEntity instanceof LivingEntity living && CombatUtils.clearPath(shooter.getEyeLocation(),living)) {
            this.targetLoc = targetEntity.getLocation().clone().add(0.0, targetEntity.getHeight() / 2.0, 0.0);
         } else if (targetBlock != null) {
            this.targetLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);
         } else {
            this.targetLoc = shooter.getEyeLocation().add(shooter.getLocation().getDirection().multiply(15));
         }

         this.targetLoc.setYaw(0.0F);
         this.targetLoc.setPitch(0.0F);
         this.targetLoc.getWorld().playSound(this.targetLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 1.0F);
         double radius = RunesAltarV.getInstance().getWeaponsConfig().getDouble("hyperion.holy-lance.radius",2.5);

         for (int i = 0; i < 360; i += 12) {
            double rad = Math.toRadians((double)i);
            this.targetLoc
               .getWorld()
               .spawnParticle(Particle.FIREWORK, this.targetLoc.clone().add(Math.cos(rad) * radius, 0.2, Math.sin(rad) * radius), 1, 0.0, 0.0, 0.0, 0.0);
         }

         this.targetLoc.getWorld().spawnParticle(Particle.FIREWORK, this.targetLoc.clone().add(0.0, 0.2, 0.0), 15, 0.2, 0.2, 0.2, 0.05);
      }

      @Override
      public void run() {
         if (this.ticks == 30) {
            RunesAltarV plugin = RunesAltarV.getInstance();
            double radDmg = plugin.getWeaponsConfig().getDouble("hyperion.holy-lance.radius-damage", 10.0);
            double centerDmg = plugin.getWeaponsConfig().getDouble("hyperion.holy-lance.center-damage", 14.0);
            this.targetLoc.getWorld().playSound(this.targetLoc, Sound.BLOCK_CONDUIT_DEACTIVATE, 1.0F, 1.0F);
            Block hitBlock = this.targetLoc.clone().subtract(0.0, 1.0, 0.0).getBlock();
            Location effectLoc = this.targetLoc.clone().add(0.0, 0.5, 0.0);

            try {
               this.targetLoc.getWorld().spawnParticle(Particle.valueOf("BLOCK_CRACK"), effectLoc, 150, 1.2, 0.5, 1.2, 0.15, hitBlock.getBlockData());
            } catch (Exception var17) {
               this.targetLoc.getWorld().spawnParticle(Particle.valueOf("BLOCK"), effectLoc, 150, 1.2, 0.5, 1.2, 0.15, hitBlock.getBlockData());
            }

            DustOptions yellowDust = new DustOptions(Color.YELLOW, 1.5F);

            try {
               this.targetLoc.getWorld().spawnParticle(Particle.DUST, effectLoc, 100, 1.5, 1.0, 1.5, 0.1, yellowDust);
            } catch (IllegalArgumentException var16) {
               this.targetLoc.getWorld().spawnParticle(Particle.valueOf("REDSTONE"), effectLoc, 100, 1.5, 1.0, 1.5, 0.1, yellowDust);
            }

            for (Entity e : this.targetLoc.getWorld().getNearbyEntities(this.targetLoc, plugin.getWeaponsConfig().getDouble("hyperion.holy-lance.radius",2.5), 3.5, plugin.getWeaponsConfig().getDouble("hyperion.holy-lance.radius",2.5))) {
               if (e instanceof LivingEntity) {
                  LivingEntity victim = (LivingEntity)e;
                  if (e != this.shooter && CombatUtils.clearPath(this.targetLoc.clone().add(0,0.5,0),victim)) {
                     if (victim instanceof Player) {
                        Player targetPlayer = (Player)victim;
                        if (plugin.getFriendManager().isFriend(this.shooter.getUniqueId(), targetPlayer.getUniqueId())) {
                           continue;
                        }
                     }

                     double dist = victim.getLocation().distance(this.targetLoc);
                     double pureDmg = dist <= 1.5 ? centerDmg : radDmg;
                     RunesAltarV.getInstance().getCleanDamageManager().apply(victim, this.shooter, pureDmg);
                     ignite(victim, this.shooter);
                  }
               }
            }

            this.lance = this.targetLoc.getWorld().spawn(this.targetLoc, ItemDisplay.class);
            RunesAltarV.getInstance().getVisualCleanupManager().track(this.lance);
            ItemStack customLanceItem = new ItemStack(Material.FEATHER);
            ItemMeta meta = customLanceItem.getItemMeta();
            if (meta != null) {
               meta.setCustomModelData(3);
               customLanceItem.setItemMeta(meta);
            }

            this.lance.setItemStack(customLanceItem);
            this.lance.setItemDisplayTransform(ItemDisplayTransform.HEAD);
            Transformation tStart = this.lance.getTransformation();
            tStart.getScale().set(0.01F, 0.01F, 0.01F);
            tStart.getTranslation().set(0.0F, 0.0F, 0.0F);
            this.lance.setTransformation(tStart);
            float lanceScale = 16.0F;
            float yOffset = 8.0F;
            Bukkit.getScheduler().runTaskLater(RunesAltarV.getInstance(), () -> {
               if (this.lance != null && this.lance.isValid()) {
                  this.lance.setInterpolationDelay(0);
                  this.lance.setInterpolationDuration(8);
                  Transformation t = this.lance.getTransformation();
                  t.getScale().set(16.0F, 16.0F, 16.0F);
                  t.getTranslation().set(0.0F, 8.0F, 0.0F);
                  t.getLeftRotation().rotationY((float)Math.toRadians(90.0));
                  this.lance.setTransformation(t);
               }
            }, 3L);
            Bukkit.getScheduler().runTaskLater(RunesAltarV.getInstance(), () -> {
               if (this.lance != null && this.lance.isValid()) {
                  this.lance.setInterpolationDelay(0);
                  this.lance.setInterpolationDuration(10);
                  Transformation t = this.lance.getTransformation();
                  t.getScale().set(16.0F, 16.0F, 16.0F);
                  t.getTranslation().set(0.0F, 8.0F, 0.0F);
                  t.getLeftRotation().rotationY((float)Math.toRadians(180.0));
                  this.lance.setTransformation(t);
               }
            }, 11L);
            Bukkit.getScheduler().runTaskLater(RunesAltarV.getInstance(), () -> {
               if (this.lance != null && this.lance.isValid()) {
                  this.lance.setInterpolationDelay(0);
                  this.lance.setInterpolationDuration(8);
                  Transformation t = this.lance.getTransformation();
                  t.getScale().set(0.01F, 0.01F, 0.01F);
                  t.getTranslation().set(0.0F, 0.0F, 0.0F);
                  t.getLeftRotation().rotationY((float)Math.toRadians(270.0));
                  this.lance.setTransformation(t);
               }
            }, 21L);
            Bukkit.getScheduler().runTaskLater(RunesAltarV.getInstance(), () -> {
               if (this.lance != null) {
                  this.lance.remove();
               }
            }, 30L);
         }

         if (this.ticks >= 60) {
            this.cancel();
         }

         this.ticks++;
      }
   }

   public static class ScorchingBladeTask extends BukkitRunnable {
      private static final int DISPLAY_COUNT = 45;
      private final Player shooter;
      private final BlockDisplay[] blocks = new BlockDisplay[45];
      private final Location currentLoc;
      private final Vector dir;
      private final Vector tiltRight;
      private int ticks = 0;
      private double totalWidth = 1.25;
      private boolean isLavaPhase = false;
      private final Set<UUID> hitVictims = new HashSet<>();

      public ScorchingBladeTask(Player shooter) {
         this.shooter = shooter;
         this.currentLoc = shooter.getEyeLocation().clone();
         this.dir = this.currentLoc.getDirection().normalize();
         if (this.dir.lengthSquared() == 0.0) {
            this.dir.copy(new Vector(1, 0, 0));
         }

         Vector up = new Vector(0, 1, 0);
         if (Math.abs(this.dir.getY()) > 0.99) {
            up = new Vector(1, 0, 0);
         }

         Vector right = this.dir.clone().crossProduct(up).normalize();
         double radAngle = Math.toRadians(22.5);
         double cosAngle = Math.cos(radAngle);
         double sinAngle = Math.sin(radAngle);
         this.tiltRight = right.clone().multiply(cosAngle).add(up.clone().multiply(sinAngle)).normalize();
         shooter.getWorld().playSound(this.currentLoc, Sound.ENTITY_BLAZE_SHOOT, 2.0F, 0.8F);

         for (int i = 0; i < 45; i++) {
            BlockDisplay bd = shooter.getWorld().spawn(this.currentLoc, BlockDisplay.class);
            RunesAltarV.getInstance().getVisualCleanupManager().track(bd);
            bd.setBlock(Material.FIRE.createBlockData());
            bd.setInterpolationDelay(0);
            bd.setInterpolationDuration(1);
            Transformation t = bd.getTransformation();
            t.getScale().set(1.375F, 2.2F, 1.375F);
            t.getTranslation().set(-0.6875F, -1.1F, -0.6875F);
            float yaw = (float)Math.toRadians((double)this.currentLoc.getYaw());
            float pitch = (float)Math.toRadians((double)this.currentLoc.getPitch());
            t.getLeftRotation().rotationY(-yaw).rotationX(-pitch);
            bd.setTransformation(t);
            this.blocks[i] = bd;
         }
      }

      @Override
      public void run() {
         int maxTicks = 32;
         int lavaPhaseTick = 24;
         if (this.ticks >= maxTicks) {
            for (int i = 0; i < 45; i++) {
               if (this.blocks[i] != null && this.blocks[i].isValid()) {
                  if (this.isLavaPhase) {
                     this.spawnRealLavaColumn(this.blocks[i].getLocation(), i, 44);
                  }

                  RunesAltarV.getInstance().getVisualCleanupManager().untrack(this.blocks[i]);
                  this.blocks[i].remove();
               }
            }

            this.cancel();
         } else {
            if (this.ticks >= lavaPhaseTick && !this.isLavaPhase) {
               this.isLavaPhase = true;
               this.currentLoc.getWorld().playSound(this.currentLoc, Sound.ITEM_BUCKET_EMPTY_LAVA, 1.2F, 0.8F);

               for (int ix = 0; ix < 45; ix++) {
                  if (this.blocks[ix] != null && this.blocks[ix].isValid()) {
                     this.blocks[ix].setBlock(Material.LAVA.createBlockData());
                  }
               }
            }

            this.currentLoc.add(this.dir.clone().multiply(1.2));
            this.totalWidth += 0.95;
            float lengthShrink = 1.0F;
            if (this.ticks >= maxTicks - 4) {
               lengthShrink = (float)(maxTicks - this.ticks) / 4.0F;
               if (lengthShrink < 0.05F) {
                  lengthShrink = 0.05F;
               }
            }

            double pureDamage = RunesAltarV.getInstance().getWeaponsConfig().getDouble("hyperion.scorching-blade.pure-damage", 6.0);
            double curveDepth = 1.3;

            for (int ixx = 0; ixx < 45; ixx++) {
               if (this.blocks[ixx] != null && this.blocks[ixx].isValid()) {
                  double norm = -1.0 + 2.0 * (double)ixx / 44.0;
                  double transverseOffset = norm * (this.totalWidth / 2.0);
                  double depthOffset = -(norm * norm - 1.0) * curveDepth;
                  Location bLoc = this.currentLoc.clone().add(this.tiltRight.clone().multiply(transverseOffset)).add(this.dir.clone().multiply(depthOffset));
                  this.blocks[ixx].setInterpolationDelay(0);
                  this.blocks[ixx].setInterpolationDuration(1);
                  Transformation t = this.blocks[ixx].getTransformation();
                  t.getScale().set(1.375F, 2.2F, 1.375F * lengthShrink);
                  this.blocks[ixx].setTransformation(t);
                  this.blocks[ixx].teleport(bLoc);
                  RayTraceResult ray = bLoc.getWorld()
                     .rayTrace(bLoc, this.dir, 1.3, FluidCollisionMode.NEVER, true, 0.5, e -> e != this.shooter && e instanceof LivingEntity);
                  if (ray != null) {
                     Entity isFriend = ray.getHitEntity();
                     if (isFriend instanceof LivingEntity) {
                        LivingEntity victim = (LivingEntity)isFriend;
                        if (!this.hitVictims.contains(victim.getUniqueId())) {
                           boolean isFriendx = false;
                           if (victim instanceof Player) {
                              Player targetPlayer = (Player)victim;
                              if (RunesAltarV.getInstance().getFriendManager().isFriend(this.shooter.getUniqueId(), targetPlayer.getUniqueId())) {
                                 isFriendx = true;
                              }
                           }

                           if (!isFriendx && CombatUtils.clearPath(this.shooter.getEyeLocation(),victim)) {
                              this.hitVictims.add(victim.getUniqueId());
                              RunesAltarV.getInstance().getCleanDamageManager().apply(victim, this.shooter, pureDamage);
                              ignite(victim, this.shooter);
                           }
                        }

                        if (this.isLavaPhase) {
                           this.spawnRealLavaColumn(bLoc, ixx, 44);
                        }
                     }
                  }
               }
            }

            if (this.isLavaPhase) {
               if (this.ticks % 2 == 0) {
                  for (int ixxx = 0; ixxx < 45; ixxx += 4) {
                     if (this.blocks[ixxx] != null && this.blocks[ixxx].isValid()) {
                        this.spawnRealLavaColumn(this.blocks[ixxx].getLocation(), ixxx, 44);
                     }
                  }
               }

               this.currentLoc.getWorld().spawnParticle(Particle.DRIPPING_LAVA, this.currentLoc, 25, this.totalWidth / 2.0, 0.8, this.totalWidth / 2.0, 0.1);
               this.currentLoc.getWorld().spawnParticle(Particle.FALLING_LAVA, this.currentLoc, 15, this.totalWidth / 2.0, 0.8, this.totalWidth / 2.0, 0.1);
            } else {
               this.currentLoc.getWorld().spawnParticle(Particle.FLAME, this.currentLoc, 30, this.totalWidth / 2.0, 0.8, this.totalWidth / 2.0, 0.1);
               this.currentLoc.getWorld().spawnParticle(Particle.CRIT, this.currentLoc, 25, this.totalWidth / 2.0, 0.8, this.totalWidth / 2.0, 0.1);
            }

            this.ticks++;
         }
      }

      private void spawnRealLavaColumn(Location loc, int index, int maxIndex) {
         if (!RunesAltarV.getInstance().getWeaponsConfig().getBoolean("hyperion.scorching-blade.place-real-lava",false)) return;
         Block baseBlock = loc.getBlock();
         int edgeMargin = 4;
         boolean isEdge = index < edgeMargin || index > maxIndex - edgeMargin;
         loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 0.5F, 1.2F);
         loc.getWorld().playSound(loc, Sound.ENTITY_MAGMA_CUBE_HURT, 0.5F, 0.8F);
         this.setTempLava(baseBlock);
         if (!isEdge) {
            Block topBlock = baseBlock.getRelative(BlockFace.UP);
            this.setTempLava(topBlock);
         }
      }

      private void setTempLava(Block block) {
         if (block.getType().isAir() || block.getType() == Material.SHORT_GRASS || block.getType() == Material.TALL_GRASS) {
            BlockData oldData = block.getBlockData().clone();
            block.setType(Material.LAVA);
            Bukkit.getScheduler().runTaskLater(RunesAltarV.getInstance(), () -> {
               if (block.getType() == Material.LAVA) {
                  block.setBlockData(oldData);
               }
            }, 12L);
         }
      }
   }

   public static class ScorchingChargeTask extends BukkitRunnable {
      private static final Set<Player> chargingPlayers = new HashSet<>();
      private final Player p;
      private final BossBar bar;
      private final Location startLoc;
      private int ticks = 0;

      public static boolean isCharging(Player p) {
         return chargingPlayers.contains(p);
      }

      public ScorchingChargeTask(Player p) {
         this.p = p;
         chargingPlayers.add(p);
         this.bar = TextUtils.bossBar(RunesAltarV.getInstance().tr("§6!! §e§lОБЖИГАНИЕ §6!!", "§6!! §e§lSCORCHING §6!!"), BarColor.YELLOW, BarStyle.SOLID);
         this.bar.addPlayer(p);
         this.bar.setProgress(1.0);
         this.startLoc = p.getLocation().clone();
         p.getWorld().playSound(this.startLoc, Sound.BLOCK_FIRE_AMBIENT, 1.0F, 1.0F);
      }

      @Override
      public void run() {
         if (this.p.isOnline() && !this.p.isDead() && this.ticks < 20) {
            if (this.ticks == 0) {
               Location center = this.startLoc.clone().add(0.0, 0.3, 0.0);
               int rays = 64;

               for (int i = 0; i < rays; i++) {
                  double angle = (double)i * (Math.PI * 2) / (double)rays;
                  double dx = Math.cos(angle);
                  double dz = Math.sin(angle);
                  Location spawnPoint = center.clone().add(dx * 0.4, 0.0, dz * 0.4);
                  center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, spawnPoint, 0, dx, 0.02, dz, 0.15);
                  center.getWorld().spawnParticle(Particle.FLAME, spawnPoint, 0, dx, 0.02, dz, 0.25);
                  center.getWorld().spawnParticle(Particle.FLAME, spawnPoint, 0, dx, 0.02, dz, 0.35);
               }
            }

            this.ticks++;
         } else {
            this.bar.removeAll();
            chargingPlayers.remove(this.p);
            if (this.ticks >= 20) {
               this.p.swingMainHand();

               try {
                  this.p.resetCooldown();
               } catch (Throwable var11) {
               }

               Location eyeLoc = this.p.getEyeLocation();
               Location sweepLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(1.5));
               this.p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, sweepLoc, 1, 0.0, 0.0, 0.0, 0.0);
               this.p.getWorld().playSound(this.p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
               HyperionCooldownManager.setScorchingCooldown(this.p);
               new HyperionTasks.ScorchingBladeTask(this.p).runTaskTimer(RunesAltarV.getInstance(), 0L, 1L);
            }

            this.cancel();
         }
      }
   }
}

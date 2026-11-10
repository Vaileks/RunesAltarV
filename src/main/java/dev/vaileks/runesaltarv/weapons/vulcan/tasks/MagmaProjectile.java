package dev.vaileks.runesaltarv.weapons.vulcan.tasks;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class MagmaProjectile extends BukkitRunnable {
   private final Player shooter;
   private final Vector velocity;
   private final BlockDisplay display;
   private Location currentLoc;
   private int ticks = 0;
   private static final Set<Material> PROTECTED_BLOCKS = new HashSet<>();

   public MagmaProjectile(Player shooter, Location start, Vector velocity) {
      this.shooter = shooter;
      this.currentLoc = start.clone();
      this.velocity = velocity.multiply(1.8);
      this.display = (BlockDisplay)start.getWorld().spawnEntity(start, EntityType.BLOCK_DISPLAY);
      RunesAltarV.getInstance().getVisualCleanupManager().track(this.display);
      this.display.setBlock(Material.MAGMA_BLOCK.createBlockData());
      this.display.setInterpolationDuration(1);
      this.display.setTeleportDuration(1);
      this.display.setInterpolationDelay(0);
      Matrix4f transform = new Matrix4f().translate(-0.5F, -0.5F, -0.5F);
      this.display.setTransformationMatrix(transform);
   }

   @Override
   public void run() {
      if (this.ticks++ <= 150 && !this.display.isDead()) {
         this.velocity.setY(this.velocity.getY() - 0.05);
         Vector direction = this.velocity.clone().normalize();
         double traceDistance = this.velocity.length() + 0.1;
         RunesAltarV plugin = RunesAltarV.getInstance();
         RayTraceResult blockHit = this.currentLoc.getWorld().rayTraceBlocks(this.currentLoc, direction, traceDistance, FluidCollisionMode.NEVER, true);
         RayTraceResult entityHit = this.currentLoc
            .getWorld()
            .rayTraceEntities(
               this.currentLoc,
               direction,
               traceDistance,
               0.35,
               entity -> entity != this.shooter
                     && entity != this.display
                     && (entity instanceof LivingEntity || plugin.getPaleGunAbilityManager().isPaleRootDisplay(entity))
            );
         RayTraceResult rayResult = this.nearestHit(this.currentLoc, blockHit, entityHit);
         if (rayResult != null) {
            Location hitLocation = rayResult.getHitPosition().toLocation(this.currentLoc.getWorld());
            if (rayResult.getHitEntity() != null) {
               plugin.getPaleGunAbilityManager().handleRootDisplayHit(rayResult.getHitEntity(), hitLocation);
            }

            this.impact(hitLocation, rayResult.getHitBlockFace());
            this.cancel();
         } else {
            this.currentLoc.add(this.velocity);
            this.display.teleport(this.currentLoc);
            Vector particleOffset = this.velocity.clone().normalize().multiply(0.9);
            Location trailLoc = this.currentLoc.clone().subtract(particleOffset);
            this.currentLoc.getWorld().spawnParticle(Particle.FLAME, trailLoc,AltarTuning.particles("vulcan_crossbow",5), 0.02, 0.02, 0.02, 0.05);
         }
      } else {
         this.cancel();
      }
   }

   private void impact(Location loc, BlockFace hitFace) {
      this.display.remove();
      RunesAltarV plugin = RunesAltarV.getInstance();
      float power = (float)plugin.getWeaponsConfig().getDouble("vulcan_crossbow.magma-attack.explosion-power", 2.0);
      loc.getWorld().createExplosion(loc, power, false, false, this.shooter);
      loc.getWorld().playSound(loc,AltarTuning.sound("vulcan_crossbow","visuals.sounds.block-sculk-spread.sound",Sound.BLOCK_SCULK_SPREAD),AltarTuning.volume("vulcan_crossbow","visuals.sounds.block-sculk-spread",1.0F),AltarTuning.pitch("vulcan_crossbow","visuals.sounds.block-sculk-spread",1.0F));
      loc.getWorld().playSound(loc,AltarTuning.sound("vulcan_crossbow","visuals.sounds.block-fire-extinguish.sound",Sound.BLOCK_FIRE_EXTINGUISH),AltarTuning.volume("vulcan_crossbow","visuals.sounds.block-fire-extinguish",1.0F),AltarTuning.pitch("vulcan_crossbow","visuals.sounds.block-fire-extinguish",0.8F));
      int radius = plugin.getWeaponsConfig().getInt("vulcan_crossbow.magma-attack.radius", 3);
      double spreadChance = plugin.getWeaponsConfig().getDouble("vulcan_crossbow.magma-attack.spread-chance", 0.8);
      boolean autoReset = plugin.getWeaponsConfig().getBoolean("vulcan_crossbow.magma-attack.auto-reset", false);
      loc.getWorld().spawnParticle(Particle.FLAME, loc,AltarTuning.particles("vulcan_crossbow",150), (double)radius / 1.5, (double)radius / 1.5, (double)radius / 1.5, 1.2);
      loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc,AltarTuning.particles("vulcan_crossbow",1));
      Map<Location, Material> oldBlocks = new HashMap<>();
      boolean isFloor = hitFace == BlockFace.UP;
      if (autoReset) {
         int saveRadius = isFloor ? radius + 2 : radius;

         for (int x = -saveRadius; x <= saveRadius; x++) {
            for (int y = -saveRadius; y <= saveRadius; y++) {
               for (int z = -saveRadius; z <= saveRadius; z++) {
                  if (x * x + y * y + z * z <= saveRadius * saveRadius) {
                     Block b = loc.clone().add((double)x, (double)y, (double)z).getBlock();
                     oldBlocks.put(b.getLocation(), b.getType());
                     Block up = b.getRelative(0, 1, 0);
                     oldBlocks.putIfAbsent(up.getLocation(), up.getType());
                  }
               }
            }
         }
      }

      for (Entity e : loc.getWorld().getNearbyEntities(loc, (double)(radius + 1), (double)(radius + 1), (double)(radius + 1))) {
         if (e instanceof LivingEntity) {
            LivingEntity target = (LivingEntity)e;
            if (e != this.shooter) {
               if (target instanceof Player) {
                  Player targetPlayer = (Player)target;
                  if (plugin.getFriendManager().isFriend(this.shooter.getUniqueId(), targetPlayer.getUniqueId())) {
                     continue;
                  }
               }

               target.setFireTicks(100);
            }
         }
      }

      for (int x = -radius; x <= radius; x++) {
         for (int y = -radius; y <= 0; y++) {
            for (int zx = -radius; zx <= radius; zx++) {
               if (x * x + y * y + zx * zx <= radius * radius) {
                  Block b = loc.clone().add((double)x, (double)y, (double)zx).getBlock();
                  Material type = b.getType();
                  if (type != Material.AIR
                     && type != Material.CAVE_AIR
                     && !PROTECTED_BLOCKS.contains(type)
                     && (b.getType().isSolid() || type == Material.WATER)
                     && Math.random() < spreadChance) {
                     if (Math.random() > 0.5) {
                        b.setType(Material.MAGMA_BLOCK);
                     } else {
                        b.setType(Material.LAVA);
                     }
                  }
               }
            }
         }
      }

      if (isFloor) {
         int moundRadius = Math.max(1, radius - 1);

         for (int x = -moundRadius; x <= moundRadius; x++) {
            for (int zxx = -moundRadius; zxx <= moundRadius; zxx++) {
               if (x * x + zxx * zxx <= moundRadius * moundRadius) {
                  Block b = loc.clone().add((double)x, 0.0, (double)zxx).getBlock();
                  Material type = b.getType();
                  if ((
                        type == Material.AIR
                           || type == Material.CAVE_AIR
                           || type == Material.WATER
                           || type == Material.SHORT_GRASS
                           || type == Material.TALL_GRASS
                           || type == Material.SNOW
                           || type == Material.FIRE
                     )
                     && Math.random() < spreadChance * 0.75) {
                     if (Math.random() > 0.4) {
                        b.setType(Material.MAGMA_BLOCK);
                     } else {
                        b.setType(Material.LAVA);
                     }
                  }
               }
            }
         }
      }

      for (int x = -radius; x <= radius; x++) {
         for (int zxxx = -radius; zxxx <= radius; zxxx++) {
            for (int y = -radius; y <= 2; y++) {
               Block b = loc.clone().add((double)x, (double)y, (double)zxxx).getBlock();
               if (b.getType() == Material.MAGMA_BLOCK || b.getType() == Material.LAVA) {
                  Block up = b.getRelative(0, 1, 0);
                  if (up.getType() == Material.AIR && Math.random() > 0.4) {
                     up.setType(Material.FIRE);
                  }
               }
            }
         }
      }

      if (autoReset && !oldBlocks.isEmpty()) {
         int delaySeconds = plugin.getWeaponsConfig().getInt("vulcan_crossbow.magma-attack.reset-delay", 20);
         Location holoLoc = loc.clone().add(0.0, (double)radius + 1.5, 0.0);
         if (isFloor) {
            holoLoc.add(0.0, 1.5, 0.0);
         }

         new VulcanRestoreTask(plugin, oldBlocks, holoLoc, delaySeconds).start();
      }
   }

   private RayTraceResult nearestHit(Location origin, RayTraceResult first, RayTraceResult second) {
      if (first == null) {
         return second;
      } else if (second == null) {
         return first;
      } else {
         double firstDistance = first.getHitPosition().distanceSquared(origin.toVector());
         double secondDistance = second.getHitPosition().distanceSquared(origin.toVector());
         return firstDistance <= secondDistance ? first : second;
      }
   }

   static {
      PROTECTED_BLOCKS.add(Material.BEDROCK);
      PROTECTED_BLOCKS.add(Material.OBSIDIAN);
      PROTECTED_BLOCKS.add(Material.REINFORCED_DEEPSLATE);
      PROTECTED_BLOCKS.add(Material.CRYING_OBSIDIAN);
   }
}

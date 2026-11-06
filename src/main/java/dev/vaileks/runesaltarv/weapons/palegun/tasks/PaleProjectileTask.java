package dev.vaileks.runesaltarv.weapons.palegun.tasks;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.ParticleUtils;
import dev.vaileks.runesaltarv.weapons.palegun.managers.PaleGunAbilityManager;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

public class PaleProjectileTask extends BukkitRunnable {
   private final Player shooter;
   private final Vector velocity;
   private final BlockDisplay display;
   private final RunesAltarV plugin;
   private final PaleGunAbilityManager abilityManager;
   private final BlockData resinData = Material.RESIN_BLOCK.createBlockData();
   private final BlockData paleOakData = Material.PALE_OAK_LOG.createBlockData();
   private Location currentLoc;
   private int ticks;
   private boolean cleaned;

   public PaleProjectileTask(Player shooter, Location start, Vector velocity, RunesAltarV plugin, PaleGunAbilityManager abilityManager) {
      this.shooter = shooter;
      this.currentLoc = start.clone();
      this.velocity = velocity.clone();
      this.plugin = plugin;
      this.abilityManager = abilityManager;
      this.display = (BlockDisplay)start.getWorld().spawnEntity(start, EntityType.BLOCK_DISPLAY);
      plugin.getVisualCleanupManager().track(this.display);
      this.display.setBlock(this.resinData);
      this.display.setInterpolationDuration(1);
      this.display.setTeleportDuration(1);
      this.display.setInterpolationDelay(0);
      Matrix4f transform = new Matrix4f().translate(-0.5F, -0.5F, -0.5F);
      this.display.setTransformationMatrix(transform);
   }

   @Override
   public void run() {
      if (!this.cleaned && this.shooter.isOnline() && !this.shooter.isDead() && !this.display.isDead()) {
         if (this.ticks++ > this.plugin.getWeaponsConfig().getInt("pale-gun.shoot.lifetime-ticks", 150)) {
            this.forceCleanup();
         } else {
            double gravity = this.plugin.getWeaponsConfig().getDouble("pale-gun.shoot.gravity-per-tick", 0.05);
            this.velocity.setY(this.velocity.getY() - gravity);
            Vector traceDirection = this.velocity.clone().normalize();
            double traceDistance = this.velocity.length() + 0.1;
            RayTraceResult blockHit = this.currentLoc.getWorld().rayTraceBlocks(this.currentLoc, traceDirection, traceDistance, FluidCollisionMode.NEVER, true);
            RayTraceResult entityHit = this.currentLoc
               .getWorld()
               .rayTraceEntities(
                  this.currentLoc,
                  traceDirection,
                  traceDistance,
                  0.35,
                  entity -> entity != this.shooter
                        && entity != this.display
                        && (entity instanceof LivingEntity || this.abilityManager.isPaleRootDisplay(entity))
               );
            RayTraceResult hit = this.nearestHit(this.currentLoc, blockHit, entityHit);
            if (hit != null) {
               Location hitLocation = hit.getHitPosition().toLocation(this.currentLoc.getWorld());
               if (hit.getHitEntity() != null) {
                  this.abilityManager.handleRootDisplayHit(hit.getHitEntity(), hitLocation);
               }

               this.impact(hitLocation);
            } else {
               Location previousLoc = this.currentLoc.clone();
               this.currentLoc.add(this.velocity);
               this.display.teleport(this.currentLoc);
               double distance = this.velocity.length();
               double step = 0.2;
               if (distance > 0.0) {
                  Vector direction = this.velocity.clone().normalize();

                  for (double d = 0.0; d < distance; d += step) {
                     Location trailLoc = previousLoc.clone().add(direction.clone().multiply(d));
                     this.currentLoc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, trailLoc, 2, 0.05, 0.05, 0.05, 0.0, this.resinData);
                  }
               }
            }
         }
      } else {
         this.forceCleanup();
      }
   }

   public void forceCleanup() {
      if (!this.cleaned) {
         this.cleaned = true;
         this.display.remove();
         this.abilityManager.untrackProjectile(this);
         this.cancel();
      }
   }

   private void impact(Location loc) {
      if (!this.cleaned) {
         World world = loc.getWorld();
         if (world == null) {
            this.forceCleanup();
         } else {
            float power = (float)this.plugin.getWeaponsConfig().getDouble("pale-gun.shoot.explosion-power", 2.0);
            world.createExplosion(loc, power, false, false, this.shooter);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
            world.playSound(loc, Sound.BLOCK_RESIN_BREAK, 1.0F, 0.8F);
            world.playSound(loc, Sound.BLOCK_WOOD_BREAK, 1.0F, 0.5F);
            Location baseLoc = loc.clone().add(0.0, 0.2, 0.0);
            ParticleUtils.spawnBlockDispersion(this.plugin, baseLoc, this.resinData, 8.0, 200);
            ParticleUtils.spawnBlockDispersion(this.plugin, baseLoc, this.paleOakData, 8.0, 100);
            this.createMistCloud(loc);
            this.forceCleanup();
         }
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

   private void createMistCloud(Location loc) {
      World world = loc.getWorld();
      if (world != null) {
         int duration = this.plugin.getWeaponsConfig().getInt("pale-gun.shoot.cloud-duration-ticks", 600);
         double radius = this.plugin.getWeaponsConfig().getDouble("pale-gun.shoot.cloud-radius", 3.0);
         int effectDuration = this.plugin.getWeaponsConfig().getInt("pale-gun.shoot.effect-duration-ticks", 12000);
         int effectAmp = this.plugin.getWeaponsConfig().getInt("pale-gun.shoot.effect-amplifier", 1);
         AreaEffectCloud cloud = (AreaEffectCloud)world.spawnEntity(loc.clone().add(0.0, 0.1, 0.0), EntityType.AREA_EFFECT_CLOUD);
         cloud.setDuration(duration);
         cloud.setWaitTime(0);
         cloud.setRadius((float)radius);
         cloud.setRadiusPerTick(-((float)radius / (float)duration));
         cloud.setRadiusOnUse(-0.5F);
         cloud.setBasePotionType(PotionType.AWKWARD);
         cloud.setColor(Color.fromRGB(132, 155, 153));
         PotionEffect luckEffect = new PotionEffect(PotionEffectType.LUCK, effectDuration, effectAmp, false, true, true);
         cloud.addCustomEffect(luckEffect, true);
      }
   }
}

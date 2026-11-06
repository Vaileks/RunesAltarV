package dev.vaileks.runesaltarv.weapons.palegun.tasks;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.ParticleUtils;
import dev.vaileks.runesaltarv.weapons.palegun.managers.PaleGunAbilityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PaleRootsTask extends BukkitRunnable {
   private static final Vector[] OFFSETS = new Vector[]{
      new Vector(-1.0, 0.0, 0.0), new Vector(-0.7, 0.7, 0.0), new Vector(0.0, 1.0, 0.0), new Vector(0.7, 0.7, 0.0), new Vector(1.0, 0.0, 0.0)
   };
   private static final float[] ROLLS = new float[]{0.0F, -45.0F, 90.0F, 45.0F, 0.0F};
   private final Player owner;
   private final int lifetimeTicks;
   private final double damage;
   private final RunesAltarV plugin;
   private final PaleGunAbilityManager abilityManager;
   private final List<BlockDisplay> spawnedDisplays = new ArrayList<>();
   private final List<Location> archCenters = new ArrayList<>();
   private final Map<UUID, Integer> hits = new HashMap<>();
   private final Map<UUID, Integer> hitCooldowns = new HashMap<>();
   private Location currentArchBase;
   private Vector currentForward;
   private Vector currentRight;
   private int ageTicks = 0;
   private boolean cleaned = false;
   private boolean cleaningUp = false;

   public PaleRootsTask(Player owner, int durationSeconds, double damage, RunesAltarV plugin, PaleGunAbilityManager abilityManager) {
      this.owner = owner;
      this.lifetimeTicks = durationSeconds * 20;
      this.damage = damage;
      this.plugin = plugin;
      this.abilityManager = abilityManager;
      this.currentArchBase = owner.getLocation().clone();
      this.currentArchBase.setPitch(0.0F);
      this.updateDirection();

      while (this.currentArchBase.getY() > (double)owner.getWorld().getMinHeight() && !this.currentArchBase.getBlock().getType().isSolid()) {
         this.currentArchBase.subtract(0.0, 1.0, 0.0);
      }

      this.currentArchBase.add(0.0, 1.0, 0.0);
   }

   @Override
   public void run() {
      if (!this.cleaned && !this.cleaningUp && this.owner.isOnline() && !this.owner.isDead() && this.owner.getWorld() != null) {
         this.hitCooldowns.replaceAll((uuid, ticks) -> ticks - 1);
         this.hitCooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);
         if (this.ageTicks < 60) {
            int archIndex = this.ageTicks / 5;
            int partIndex = this.ageTicks % 5;
            if (partIndex == 0) {
               this.updateDirection();
               if (archIndex > 0) {
                  this.currentArchBase.add(this.currentForward.clone().multiply(3.1));
                  this.currentArchBase.setY(this.currentArchBase.getY() + 2.0);

                  while (this.currentArchBase.getY() > (double)this.owner.getWorld().getMinHeight() && !this.currentArchBase.getBlock().getType().isSolid()) {
                     this.currentArchBase.subtract(0.0, 1.0, 0.0);
                  }

                  if (this.currentArchBase.getBlock().getType().isSolid()) {
                     this.currentArchBase.add(0.0, 1.0, 0.0);
                  }
               }

               this.archCenters.add(this.currentArchBase.clone());
               this.currentArchBase.getWorld().playSound(this.currentArchBase, Sound.ENTITY_CREAKING_ATTACK, 1.0F, 1.0F);
            }

            this.spawnArchPart(partIndex);
         }

         this.applyContacts();
         this.ageTicks++;
         if (this.ageTicks >= this.lifetimeTicks) {
            this.forceCleanup();
         }
      } else {
         this.forceCleanup();
      }
   }

   public void forceCleanup() {
      if (!this.cleaned && !this.cleaningUp) {
         this.cleaningUp = true;
         this.abilityManager.untrackRootTask(this);
         this.cancel();
         BlockData paleOakData = Material.PALE_OAK_WOOD.createBlockData();
         if (!this.spawnedDisplays.isEmpty()) {
            Location soundLoc = this.spawnedDisplays.get(0).getLocation();
            soundLoc.getWorld().playSound(soundLoc, Sound.ENTITY_CREAKING_ATTACK, 1.0F, 0.8F);
         }

         for (BlockDisplay display : this.spawnedDisplays) {
            if (display != null && display.isValid()) {
               Location loc = display.getLocation();
               ParticleUtils.spawnBlockDispersion(this.plugin, loc, paleOakData, 2.0, 36);
               display.remove();
            }
         }

         this.cleaned = true;
         this.spawnedDisplays.clear();
         this.archCenters.clear();
         this.hitCooldowns.clear();
      }
   }

   private void updateDirection() {
      Vector dir = this.owner.getEyeLocation().getDirection().setY(0);
      if (dir.lengthSquared() > 1.0E-4) {
         this.currentForward = dir.normalize();
         this.currentRight = this.currentForward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
      } else if (this.currentForward == null) {
         this.currentForward = new Vector(1, 0, 0);
         this.currentRight = new Vector(0, 0, 1);
      }
   }

   private void spawnArchPart(int partIndex) {
      Vector offset = OFFSETS[partIndex];
      float roll = ROLLS[partIndex];
      Location targetLoc = this.currentArchBase.clone().add(this.currentForward.clone().multiply(offset.getX())).add(0.0, offset.getY(), 0.0);
      BlockData paleOakData = Material.PALE_OAK_WOOD.createBlockData();
      Location particleLoc = targetLoc.clone().add(this.currentForward.clone().multiply(1.2));
      ParticleUtils.spawnBlockDispersion(this.plugin, particleLoc, paleOakData, 0.5, 4);
      Location dirLoc = this.currentArchBase.clone();
      dirLoc.setDirection(this.currentForward);
      float dynamicYaw = dirLoc.getYaw();
      float yawRad = (float)Math.toRadians((double)(-dynamicYaw));
      Location initialLoc;
      if (partIndex == 0) {
         initialLoc = targetLoc.clone().subtract(0.0, 1.0, 0.0);
      } else {
         Vector prevOffset = OFFSETS[partIndex - 1];
         initialLoc = this.currentArchBase.clone().add(this.currentForward.clone().multiply(prevOffset.getX())).add(0.0, prevOffset.getY(), 0.0);
      }

      initialLoc.setYaw(0.0F);
      initialLoc.setPitch(0.0F);
      targetLoc.setYaw(0.0F);
      targetLoc.setPitch(0.0F);
      Quaternionf targetRot = new Quaternionf().rotateY(yawRad).rotateX((float)Math.toRadians((double)roll));
      Vector3f uniformScale = new Vector3f(1.0F, 1.0F, 1.0F);
      Vector3f targetTransVec = new Vector3f(-0.5F, -0.5F, -0.5F).rotate(targetRot);
      Transformation targetTrans = new Transformation(targetTransVec, targetRot, uniformScale, new Quaternionf());
      Transformation initialTrans;
      if (partIndex == 0) {
         initialTrans = targetTrans;
      } else {
         float prevRoll = ROLLS[partIndex - 1];
         Quaternionf prevRot = new Quaternionf().rotateY(yawRad).rotateX((float)Math.toRadians((double)prevRoll));
         Vector3f prevTransVec = new Vector3f(-0.5F, -0.5F, -0.5F).rotate(prevRot);
         initialTrans = new Transformation(prevTransVec, prevRot, uniformScale, new Quaternionf());
      }

      BlockDisplay display = (BlockDisplay)targetLoc.getWorld().spawnEntity(initialLoc, EntityType.BLOCK_DISPLAY);
      display.setBlock(paleOakData);
      display.setTransformation(initialTrans);
      this.plugin.getVisualCleanupManager().track(display);
      this.spawnedDisplays.add(display);
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (display.isValid()) {
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(4);
            display.setTeleportDuration(4);
            display.setTransformation(targetTrans);
            display.teleport(targetLoc);
         }
      }, 1L);
   }

   private void applyContacts() {
      for (Location center : this.archCenters) {
         for (Entity entity : center.getWorld().getNearbyEntities(center, 1.0, 1.0, 1.0)) {
            if (entity instanceof LivingEntity) {
               LivingEntity target = (LivingEntity)entity;
               if (!target.equals(this.owner) && !this.hitCooldowns.containsKey(target.getUniqueId())
                  && this.hits.getOrDefault(target.getUniqueId(),0) < this.plugin.getWeaponsConfig().getInt("pale-gun.pale-roots.max-hits-per-target",3)
                  && dev.vaileks.runesaltarv.utils.CombatUtils.clearPath(center.clone().add(0,0.3,0),target)) {
                  if (target instanceof Player) {
                     Player targetPlayer = (Player)target;
                     if (targetPlayer.getGameMode() == GameMode.SPECTATOR
                        || this.plugin.getFriendManager().isFriend(this.owner.getUniqueId(), targetPlayer.getUniqueId())) {
                        continue;
                     }
                  }

                  target.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 600, 1, false, false, true));
                  target.getWorld().playSound(target.getLocation(), Sound.ENCHANT_THORNS_HIT, 1.0F, 1.0F);
                  Vector knockback = target.getLocation().toVector().subtract(center.toVector()).setY(0);
                  if (knockback.lengthSquared() < 1.0E-4) {
                     knockback = this.currentForward.clone();
                  }

                  target.setVelocity(knockback.normalize().multiply(1.0).setY(0.25));
                  this.plugin.getCleanDamageManager().apply(target, this.owner, this.damage);
                  this.hits.merge(target.getUniqueId(),1,Integer::sum);
                  this.hitCooldowns.put(target.getUniqueId(), Math.max(1,this.plugin.getWeaponsConfig().getInt("pale-gun.pale-roots.contact-hit-cooldown-ticks",20)));
               }
            }
         }
      }
   }

   public void breakDisplay(BlockDisplay display, Location hitLocation) {
      if (display != null && this.spawnedDisplays.remove(display)) {
         Location loc = hitLocation != null ? hitLocation : display.getLocation();
         BlockData paleOakData = Material.PALE_OAK_WOOD.createBlockData();
         ParticleUtils.spawnBlockDispersion(this.plugin, loc, paleOakData, 3.0);
         this.abilityManager.untrackRootDisplay(display);
         this.plugin.getVisualCleanupManager().untrack(display);
         display.remove();
      }
   }
}

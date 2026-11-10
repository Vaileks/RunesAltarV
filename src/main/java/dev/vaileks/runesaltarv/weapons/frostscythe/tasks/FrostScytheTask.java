package dev.vaileks.runesaltarv.weapons.frostscythe.tasks;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.weapons.frostscythe.listeners.FrostListener;
import dev.vaileks.runesaltarv.weapons.frostscythe.managers.FrostAbilityManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

public class FrostScytheTask extends BukkitRunnable {
   private static final int MOTION_INTERPOLATION_TICKS = 2;
   private final RunesAltarV plugin;
   private final Player owner;
   private final FrostAbilityManager abilityManager;
   private final ItemDisplay display;
   private final Vector direction;
   private final ItemStack item;
   private final double returnSpeed;
   private boolean returning = false;
   private float roll = 0.0F;
   private int orbitTicks = 0;
   private int ticksLived = 0;
   private int returnTicks = 0;
   private final Set<UUID> hitOutward = new HashSet<>();
   private final Set<UUID> hitOnReturn = new HashSet<>();

   public FrostScytheTask(RunesAltarV plugin, Player owner, FrostAbilityManager abilityManager, ItemStack item) {
      this.plugin = plugin;
      this.owner = owner;
      this.abilityManager = abilityManager;
      this.item = item;
      double flightSpeed = plugin.getWeaponsConfig().getDouble("frost-scythe.throw.flight-speed", 1.65);
      this.returnSpeed = plugin.getWeaponsConfig().getDouble("frost-scythe.throw.return-speed", 1.55);
      this.direction = owner.getEyeLocation().getDirection().normalize().multiply(flightSpeed);
      this.display = (ItemDisplay)owner.getWorld().spawnEntity(owner.getEyeLocation(), EntityType.ITEM_DISPLAY);
      plugin.getVisualCleanupManager().track(this.display);
      Material projMat = Material.NETHERITE_AXE;
      int projCmd = 1;
      String projModel = "";
      if (plugin.getModelsConfig() != null) {
         String matStr = plugin.getModelsConfig().getString("weapons.frost_scythe.projectile.material");
         if (matStr != null && !matStr.isBlank()) {
            try {
               projMat = Material.valueOf(matStr.toUpperCase().trim());
            } catch (IllegalArgumentException var13) {
            }
         }

         projCmd = plugin.getModelsConfig().getInt("weapons.frost_scythe.projectile.custom-model-data", 1);
         projModel = plugin.getModelsConfig().getString("weapons.frost_scythe.projectile.item-model", "");
      }

      ItemStack visualItem = new ItemStack(projMat);
      ItemMeta meta = visualItem.getItemMeta();
      if (meta != null) {
         if (projCmd > 0) {
            meta.setCustomModelData(projCmd);
         }

         if (projModel != null && !projModel.isBlank()) {
            NamespacedKey key = NamespacedKey.fromString(projModel.trim());
            if (key != null) {
               meta.setItemModel(key);
            }
         }

         visualItem.setItemMeta(meta);
      }

      this.display.setItemStack(visualItem);
      this.display.setInterpolationDuration(2);
      this.display.setTeleportDuration(2);
      this.display.setInterpolationDelay(0);
      Transformation transformation = this.display.getTransformation();
      transformation.getLeftRotation().set(new Quaternionf());
      this.display.setTransformation(transformation);
   }

   @Override
   public void run() {
      this.ticksLived++;
      if (this.ticksLived > 300) {
         this.forceFinish();
      } else if (!this.owner.isOnline()) {
         this.dropItem(this.display.isValid() ? this.display.getLocation() : null);
      } else if (this.owner.isDead()) {
         this.dropItem(this.owner.getLocation());
      } else {
         Location current = this.display.getLocation();
         if (current.getWorld() == null) {
            this.forceFinish();
         } else {
            current.getWorld().spawnParticle(Particle.SNOWFLAKE, current,AltarTuning.particles("frost_scythe",6), 0.22, 0.22, 0.22, 0.03);
            if (this.returning) {
               this.handleReturn(current);
            } else {
               this.handleFlight(current);
            }
         }
      }
   }

   private void handleFlight(Location current) {
      Location nextLocation = current.clone().add(this.direction);
      if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().isLocationAllowed(nextLocation, this.owner)) {
         this.abilityManager.playImpactBurst(current, this.direction.clone().normalize());
         this.startReturn();
      } else {
         double speed = this.direction.length();
         RayTraceResult result = current.getWorld()
            .rayTrace(
               current,
               this.direction.clone().normalize(),
               speed + 0.2,
               FluidCollisionMode.NEVER,
               true,
               0.8,
               entity -> entity instanceof LivingEntity && !entity.equals(this.owner) && !entity.equals(this.display)
            );
         if (result != null) {
            if (result.getHitBlock() != null) {
               this.abilityManager.playImpactBurst(result.getHitPosition().toLocation(current.getWorld()), this.direction.clone().normalize());
               this.startReturn();
               return;
            }

            if (result.getHitEntity() instanceof LivingEntity victim) {
               if (this.isValidTarget(victim) && !this.hitOutward.contains(victim.getUniqueId())) {
                  if (this.plugin.getWorldGuardManager() == null || this.plugin.getWorldGuardManager().canAffectTarget(this.owner, victim)) {
                     Location sweepLoc = victim.getLocation().clone().add(0.0, 1.2, 0.0).subtract(this.direction.clone().normalize().multiply(1.2));
                     this.abilityManager.playSweepEffect(sweepLoc, this.direction);
                     this.abilityManager.playImpactBurst(victim.getLocation().clone().add(0.0, 1.0, 0.0), this.direction.clone().normalize());
                     this.abilityManager.applyFreeze(this.owner, victim, 3, 0);
                     CombatUtils.runSyntheticDamage(
                        () -> victim.damage(this.plugin.getWeaponsConfig().getDouble("frost-scythe.throw.damage", 12.0), this.owner)
                     );
                  }

                  this.hitOutward.add(victim.getUniqueId());
               }

               this.startReturn();
               return;
            }
         }

         this.roll -= 0.6F;
         this.display.setInterpolationDelay(0);
         this.display.setInterpolationDuration(2);
         this.display.setTeleportDuration(2);
         Transformation transformation = this.display.getTransformation();
         transformation.getLeftRotation().set(new Quaternionf().rotateY(this.roll));
         this.display.setTransformation(transformation);
         this.display.teleport(nextLocation);
         if (current.distance(this.owner.getLocation()) > 40.0) {
            this.startReturn();
         }
      }
   }

   private void handleReturn(Location current) {
      this.returnTicks++;
      if (this.returnTicks > 140) {
         this.forceFinish();
      } else {
         Location target = this.owner.getEyeLocation().subtract(0.0, 0.5, 0.0);
         Vector toOwner = target.toVector().subtract(current.toVector());
         double distance = toOwner.length();

         for (Entity entity : current.getWorld().getNearbyEntities(current,AltarTuning.number("frost_scythe","tuning.frost-scythe-task.hit-radius-x",1.5,0,128),AltarTuning.number("frost_scythe","tuning.frost-scythe-task.hit-radius-y",1.5,0,128),AltarTuning.number("frost_scythe","tuning.frost-scythe-task.hit-radius-z",1.5,0,128))) {
            if (entity instanceof LivingEntity) {
               LivingEntity victim = (LivingEntity)entity;
               if (!victim.equals(this.owner) && this.isValidTarget(victim) && !this.hitOnReturn.contains(victim.getUniqueId())) {
                  if (this.plugin.getWorldGuardManager() == null || this.plugin.getWorldGuardManager().canAffectTarget(this.owner, victim)) {
                     Location sweepLoc = victim.getLocation().clone().add(0.0, 1.2, 0.0).subtract(toOwner.clone().normalize().multiply(1.2));
                     this.abilityManager.playSweepEffect(sweepLoc, toOwner.clone().normalize());
                     this.abilityManager.playImpactBurst(victim.getLocation().clone().add(0.0, 1.0, 0.0), toOwner.clone().normalize());
                     this.abilityManager.applyFreeze(this.owner, victim, 3, 0);
                     CombatUtils.runSyntheticDamage(
                        () -> victim.damage(this.plugin.getWeaponsConfig().getDouble("frost-scythe.throw.damage", 12.0), this.owner)
                     );
                  }

                  this.hitOnReturn.add(victim.getUniqueId());
               }
            }
         }

         if (distance <= 1.5) {
            this.forceFinish();
         } else {
            Location nextReturnLoc = current.clone().add(toOwner.normalize().multiply(this.returnSpeed));
            if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().isLocationAllowed(nextReturnLoc, this.owner)) {
               this.forceFinish();
            } else {
               this.roll += 0.6F;
               this.display.setInterpolationDelay(0);
               this.display.setInterpolationDuration(2);
               this.display.setTeleportDuration(2);
               Transformation transformation = this.display.getTransformation();
               transformation.getLeftRotation().set(new Quaternionf().rotateY(this.roll));
               this.display.setTransformation(transformation);
               this.display.teleport(nextReturnLoc);
            }
         }
      }
   }

   private void forceFinish() {
      if (this.owner != null && this.owner.isOnline() && !this.owner.isDead()) {
         ItemStack returnItem = this.item.clone();
         if (this.plugin.getCrazySlotsManager() != null && this.plugin.getCrazySlotsManager().isTransformedItem(returnItem)) {
            UUID instId = this.plugin.getCrazySlotsManager().getTransformedInstanceId(returnItem);
            this.plugin.getCrazySlotsManager().unmarkInFlight(instId);
            if (!this.plugin.getCrazySlotsManager().hasActiveTransformation(instId)) {
               returnItem = this.plugin.getCrazySlotsManager().getCleanCrazySlots(instId);
               this.plugin.getCrazySlotsManager().endTransformationAndApplyCooldown(this.owner, instId);
            } else {
               returnItem.setType(Material.TRIDENT);
               FrostListener.setScytheModel(returnItem, false);
            }
         } else {
            returnItem.setType(Material.TRIDENT);
            FrostListener.setScytheModel(returnItem, false);
         }

         if (this.owner.getInventory().firstEmpty() != -1) {
            this.owner.getInventory().addItem(new ItemStack[]{returnItem});
            this.owner.playSound(this.owner.getLocation(),AltarTuning.sound("frost_scythe","visuals.sounds.entity-item-pickup.sound",Sound.ENTITY_ITEM_PICKUP),AltarTuning.volume("frost_scythe","visuals.sounds.entity-item-pickup",1.0F),AltarTuning.pitch("frost_scythe","visuals.sounds.entity-item-pickup",1.2F));
         } else {
            this.owner.getWorld().dropItemNaturally(this.owner.getLocation(), returnItem);
         }
      } else if (this.display.isValid()) {
         this.dropItem(this.display.getLocation());
         return;
      }

      this.cleanup();
   }

   private boolean isValidTarget(LivingEntity victim) {
      return !(victim instanceof Player targetPlayer)
         ? true
         : targetPlayer.getGameMode() != GameMode.SPECTATOR && !this.plugin.getFriendManager().isFriend(this.owner.getUniqueId(), targetPlayer.getUniqueId());
   }

   private void startReturn() {
      this.returning = true;
   }

   private void dropItem() {
      this.dropItem(null);
   }

   private void dropItem(Location dropLoc) {
      Location loc = dropLoc != null ? dropLoc : (this.display.isValid() ? this.display.getLocation() : (this.owner != null ? this.owner.getLocation() : null));
      if (loc != null && loc.getWorld() != null) {
         ItemStack drop = this.item.clone();
         if (this.plugin.getCrazySlotsManager() != null && this.plugin.getCrazySlotsManager().isTransformedItem(drop)) {
            UUID instId = this.plugin.getCrazySlotsManager().getTransformedInstanceId(drop);
            this.plugin.getCrazySlotsManager().unmarkInFlight(instId);
            drop = this.plugin.getCrazySlotsManager().getCleanCrazySlots(instId);
            this.plugin.getCrazySlotsManager().endTransformationAndApplyCooldown(this.owner, instId);
         } else {
            drop.setType(Material.TRIDENT);
            FrostListener.setScytheModel(drop, false);
         }

         loc.getWorld().dropItemNaturally(loc, drop);
         loc.getWorld().playSound(loc,AltarTuning.sound("frost_scythe","visuals.sounds.entity-item-pickup.sound",Sound.ENTITY_ITEM_PICKUP),AltarTuning.volume("frost_scythe","visuals.sounds.entity-item-pickup",1.0F),AltarTuning.pitch("frost_scythe","visuals.sounds.entity-item-pickup",0.6F));
         this.cleanup();
      } else {
         this.cleanup();
      }
   }

   private void cleanup() {
      if (this.plugin.getCrazySlotsManager() != null && this.plugin.getCrazySlotsManager().isTransformedItem(this.item)) {
         UUID instId = this.plugin.getCrazySlotsManager().getTransformedInstanceId(this.item);
         this.plugin.getCrazySlotsManager().unmarkInFlight(instId);
      }

      if (this.display.isValid()) {
         this.display.remove();
      }

      try {
         this.cancel();
      } catch (IllegalStateException var2) {
      }
   }
}

package dev.vaileks.runesaltarv.weapons.boneblade.listeners;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.SpeedBuffUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import java.time.Duration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BladeListener implements Listener {
   private final RunesAltarV plugin;
   private final Set<UUID> stunnedEntities = new HashSet<>();
   private final Random random = new Random();
   private final DustOptions BONE_COLOR = new DustOptions(Color.fromRGB(183, 168, 134), 1.8F);
   private final DustOptions RING_BONE_COLOR = new DustOptions(Color.fromRGB(183, 168, 134), 0.9F);

   public BladeListener(RunesAltarV plugin) {
      this.plugin = plugin;
   }

   private boolean isBoneBlade(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "bone_blade");
   }

   @EventHandler
   public void onSwapHand(PlayerSwapHandItemsEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         ItemStack item = player.getInventory().getItemInMainHand();
         if (this.isBoneBlade(item)) {
            event.setCancelled(true);
            if (player.isSneaking()) {
               if (this.plugin.getBoneCooldownManager().isOnCageCooldown(player)) {
                  return;
               }

               player.swingMainHand();
               this.castBoneCage(player);
            } else {
               if (this.plugin.getBoneCooldownManager().isOnDashCooldown(player)) {
                  return;
               }

               player.swingMainHand();
               this.castSkeletalLeap(player);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerMove(PlayerMoveEvent event) {
      if (this.stunnedEntities.contains(event.getPlayer().getUniqueId())) {
         Location from = event.getFrom();
         Location to = event.getTo();
         if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
            Location newLoc = from.clone();
            newLoc.setPitch(to.getPitch());
            newLoc.setYaw(to.getYaw());
            event.setTo(newLoc);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onPlayerJump(PlayerJumpEvent event) {
      if (this.stunnedEntities.contains(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.stunnedEntities.remove(event.getPlayer().getUniqueId());
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      if (this.stunnedEntities.contains(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onBlockBreak(BlockBreakEvent event) {
      if (this.stunnedEntities.contains(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerInteract(PlayerInteractEvent event) {
      if (this.stunnedEntities.contains(event.getPlayer().getUniqueId())) {
         ItemStack item = event.getItem();
         if (item != null && item.getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
      if (this.stunnedEntities.contains(event.getPlayer().getUniqueId())) {
         ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
         if (item != null && item.getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onProjectileLaunch(ProjectileLaunchEvent event) {
      if (event.getEntity() instanceof EnderPearl pearl && pearl.getShooter() instanceof Player p && this.stunnedEntities.contains(p.getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      if (event.getCause() == TeleportCause.ENDER_PEARL && this.stunnedEntities.contains(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onStunnedEntityDamage(EntityDamageEvent event) {
      if (this.stunnedEntities.contains(event.getEntity().getUniqueId())) {
         if (event.getEntity() instanceof Player p) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
               return;
            }

            if (p.isInvulnerable()) {
               p.setInvulnerable(false);
            }
         } else if (event.getEntity().isInvulnerable()) {
            event.getEntity().setInvulnerable(false);
         }

         if (event instanceof EntityDamageByEntityEvent byEntity
            && byEntity.getDamager() instanceof Player damager
            && event.getEntity() instanceof Player victim
            && this.plugin.getFriendManager().isFriend(damager.getUniqueId(), victim.getUniqueId())) {
            return;
         }

         if (event.isCancelled()) {
            event.setCancelled(false);
         }

         if (event.getEntity() instanceof LivingEntity living) {
            living.setNoDamageTicks(0);
         }
      }
   }

   private void castSkeletalLeap(final Player player) {
      this.plugin.getBoneCooldownManager().setDashCooldown(player, this.plugin.getWeaponsConfig().getInt("bone-blade.dash.cooldown", 15));
      player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1.0F, 0.8F);
      (new BukkitRunnable() {
         @Override
         public void run() {
            if (player.isOnline()) {
               player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1.0F, 1.2F);
            }
         }
      }).runTaskLater(this.plugin, 2L);
      double velocity = this.plugin.getWeaponsConfig().getDouble("bone-blade.dash.velocity", 1.5);
      player.setVelocity(player.getLocation().getDirection().normalize().multiply(velocity));
      int duration = this.plugin.getWeaponsConfig().getInt("bone-blade.dash.speed-duration", 3) * 20;
      int amplifier = this.plugin.getWeaponsConfig().getInt("bone-blade.dash.speed-amplifier", 2) - 1;
      SpeedBuffUtils.applyBurstSpeed(player, duration, amplifier, this.plugin);
      (new BukkitRunnable() {
            int ticks = 0;
            final Set<UUID> hitDuringDash = new HashSet<>();
            final double dashDamage = BladeListener.this.plugin.getWeaponsConfig().getDouble("bone-blade.dash.damage", 4.0);
            final double knockbackY = BladeListener.this.plugin.getWeaponsConfig().getDouble("bone-blade.dash.knockback-y", 1.1);

            @Override
            public void run() {
               if (this.ticks <= 100 && player.isOnline() && (this.ticks <= 5 || !player.isOnGround())) {
                  Location loc = player.getLocation().add(0.0, 0.8, 0.0);
                  player.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.25, 0.25, 0.25, 0.02, BladeListener.this.BONE_COLOR);
                  BladeListener.this.spawnSmallFallingBone(
                     loc.clone().add(BladeListener.this.random.nextDouble() - 0.5, -0.2, BladeListener.this.random.nextDouble() - 0.5), false
                  );

                  for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 1.5, 1.5, 1.5)) {
                     if (entity instanceof LivingEntity) {
                        final LivingEntity target = (LivingEntity)entity;
                        if (target != player && !this.hitDuringDash.contains(target.getUniqueId())) {
                           if (target instanceof Player) {
                              Player targetPlayer = (Player)target;
                              if (targetPlayer.getGameMode() == GameMode.SPECTATOR
                                 || BladeListener.this.plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) {
                                 continue;
                              }
                           }

                           this.hitDuringDash.add(target.getUniqueId());
                           CombatUtils.runSyntheticDamage(() -> target.damage(this.dashDamage, player));
                           target.setVelocity(target.getVelocity().add(new Vector(0.0, this.knockbackY, 0.0)));
                           target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SKELETON_HURT, 1.0F, 0.8F);
                           (new BukkitRunnable() {
                                 int kTicks = 0;

                                 @Override
                                 public void run() {
                                    if (this.kTicks <= 15 && target.isValid()) {
                                       Location tLoc = target.getLocation().add(0.0, 1.0, 0.0);
                                       target.getWorld().spawnParticle(Particle.DUST, tLoc, 8, 0.4, 0.4, 0.4, 0.1, BladeListener.this.BONE_COLOR);
                                       if (this.kTicks % 3 == 0) {
                                          BladeListener.this.spawnSmallFallingBone(
                                             tLoc.clone()
                                                .add(
                                                   BladeListener.this.random.nextDouble() - 0.5,
                                                   BladeListener.this.random.nextDouble() - 0.5,
                                                   BladeListener.this.random.nextDouble() - 0.5
                                                ),
                                             false
                                          );
                                       }

                                       this.kTicks++;
                                    } else {
                                       this.cancel();
                                    }
                                 }
                              })
                              .runTaskTimer(BladeListener.this.plugin, 0L, 1L);
                        }
                     }
                  }

                  this.ticks++;
               } else {
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void castBoneCage(final Player player) {
      int fullCd = this.plugin.getWeaponsConfig().getInt("bone-blade.cage.cooldown", 45);
      this.plugin.getBoneCooldownManager().setCageCooldown(player, fullCd);
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1.0F, 0.8F);
      final Location startLoc = player.getEyeLocation();
      final Vector dir = startLoc.getDirection().normalize().multiply(1.2);
      (new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();

            @Override
            public void run() {
               if (this.ticks++ > 15) {
                  this.cancel();
               } else {
                  Location previousLoc = this.currentLoc.clone();
                  this.currentLoc.add(dir);
                  this.currentLoc.getWorld().spawnParticle(Particle.DUST, this.currentLoc, 4, 0.2, 0.2, 0.2, 0.02, BladeListener.this.BONE_COLOR);

                  for (int i = 0; i < 3; i++) {
                     BladeListener.this.spawnSmallFallingBone(
                        this.currentLoc
                           .clone()
                           .add(
                              BladeListener.this.random.nextDouble() - 0.5,
                              BladeListener.this.random.nextDouble() - 0.5,
                              BladeListener.this.random.nextDouble() - 0.5
                           ),
                        false
                     );
                  }

                  RayTraceResult ray = this.currentLoc.getWorld().rayTraceBlocks(previousLoc, dir, dir.length() + 0.5, FluidCollisionMode.NEVER, true);
                  if (ray != null && ray.getHitBlock() != null) {
                     BladeListener.this.playImpactExplosion(ray.getHitPosition().toLocation(this.currentLoc.getWorld()));
                     this.cancel();
                  } else {
                     for (Entity entity : this.currentLoc.getWorld().getNearbyEntities(this.currentLoc, 0.9, 0.9, 0.9)) {
                        if (entity instanceof LivingEntity target
                           && target != player
                           && !BladeListener.this.stunnedEntities.contains(target.getUniqueId())
                           && (
                              !(target instanceof Player targetPlayer)
                                 || targetPlayer.getGameMode() != GameMode.SPECTATOR
                                    && !BladeListener.this.plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())
                           )) {
                           BladeListener.this.playImpactExplosion(this.currentLoc);
                           BladeListener.this.applyBoneStun(target);
                           this.cancel();
                           return;
                        }
                     }
                  }
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void applyBoneStun(final LivingEntity target) {
      this.stunnedEntities.add(target.getUniqueId());
      final int stunTicks = this.plugin.getWeaponsConfig().getInt("bone-blade.cage.stun-duration", 4) * 20;
      target.setInvulnerable(false);
      target.setNoDamageTicks(0);
      if (target instanceof Player p) {
         Component titleText = TextUtils.legacy(this.plugin.tr("&eОглушен!", "&eStunned!"));
         Title title = Title.title(
            titleText, TextUtils.empty(), Title.Times.times(Duration.ofMillis(250L), Duration.ofMillis((long)stunTicks * 50L / 2L), Duration.ofMillis(250L))
         );
         TextUtils.showTitle(p, title);
      } else {
         target.setAI(false);
      }

      target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0F, 1.0F);
      final BlockDisplay[] blocks = new BlockDisplay[3];
      final Vector3f[] rotationSpeeds = new Vector3f[3];

      for (int i = 0; i < 3; i++) {
         blocks[i] = target.getWorld().spawn(target.getLocation().add(0.0, 1.0, 0.0), BlockDisplay.class);
         this.plugin.getVisualCleanupManager().track(blocks[i]);
         blocks[i].setBlock(Material.BONE_BLOCK.createBlockData());
         blocks[i].setTeleportDuration(1);
         rotationSpeeds[i] = new Vector3f(
            this.random.nextFloat() * 0.25F - 0.125F, this.random.nextFloat() * 0.25F - 0.125F, this.random.nextFloat() * 0.25F - 0.125F
         );
         Transformation trans = new Transformation(new Vector3f(-0.15F, 0.0F, -0.15F), new Quaternionf(), new Vector3f(0.0F, 0.0F, 0.0F), new Quaternionf());
         blocks[i].setTransformation(trans);
      }

      (new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
               boolean var10000;
               label82: {
                  if (target instanceof Player p && p.getGameMode() == GameMode.SPECTATOR) {
                     var10000 = true;
                     break label82;
                  }

                  var10000 = false;
               }

               boolean isSpectator = var10000;
               if (target.isValid() && !isSpectator && (!(target instanceof Player) || ((Player)target).isOnline()) && this.ticks < stunTicks) {
                  if (target.isInvulnerable() && (!(target instanceof Player player) || player.getGameMode() != GameMode.CREATIVE)) {
                     target.setInvulnerable(false);
                  }

                  double currentScale = 0.35;
                  double targetRadius = 1.2;
                  double heightY = 1.0;
                  if (this.ticks < 10) {
                     currentScale = (double)this.ticks / 10.0 * 0.35;
                  } else if (this.ticks > stunTicks - 10) {
                     currentScale = (double)(stunTicks - this.ticks) / 10.0 * 0.35;
                  }

                  double radius = currentScale / 0.35 * targetRadius;
                  if (this.ticks >= 10 && this.ticks <= stunTicks - 10) {
                     double wave = Math.sin((double)this.ticks * 0.2) * 0.22;
                     radius += wave;
                     currentScale += wave * 0.15;
                  }

                  double angle = (double)this.ticks * 0.25;
                  Location center = target.getLocation();
                  if (radius > 0.05) {
                     for (int i = 0; i < 15; i++) {
                        double pAngle = 0.41887902047863906 * (double)i;
                        double px = Math.cos(pAngle) * radius;
                        double pz = Math.sin(pAngle) * radius;
                        target.getWorld()
                           .spawnParticle(Particle.DUST, center.clone().add(px, heightY, pz), 1, 0.0, 0.0, 0.0, 0.0, BladeListener.this.RING_BONE_COLOR);
                     }
                  }

                  for (int i = 0; i < 3; i++) {
                     double offset = (Math.PI * 2.0 / 3.0) * (double)i;
                     blocks[i].setInterpolationDelay(0);
                     blocks[i].setInterpolationDuration(1);
                     Transformation t = blocks[i].getTransformation();
                     t.getScale().set((float)Math.max(0.01, currentScale), (float)Math.max(0.01, currentScale), (float)Math.max(0.01, currentScale));
                     t.getLeftRotation().rotateXYZ(rotationSpeeds[i].x, rotationSpeeds[i].y, rotationSpeeds[i].z);
                     blocks[i].setTransformation(t);
                     blocks[i].teleport(center.clone().add(Math.cos(angle + offset) * radius, heightY, Math.sin(angle + offset) * radius));
                  }

                  this.ticks++;
               } else {
                  for (BlockDisplay b : blocks) {
                     b.remove();
                  }

                  BladeListener.this.stunnedEntities.remove(target.getUniqueId());
                  if (!(target instanceof Player)) {
                     target.setAI(true);
                  }

                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void playImpactExplosion(Location loc) {
      loc.getWorld().playSound(loc, Sound.ENTITY_SKELETON_STEP, 1.0F, 0.5F);
      loc.getWorld().spawnParticle(Particle.DUST, loc, 15, 0.4, 0.4, 0.4, 0.1, this.BONE_COLOR);

      for (int i = 0; i < 45; i++) {
         final ItemDisplay bone = loc.getWorld().spawn(loc, ItemDisplay.class);
         this.plugin.getVisualCleanupManager().track(bone);
         bone.setItemStack(new ItemStack(Material.BONE));
         bone.setInterpolationDuration(1);
         bone.setTeleportDuration(1);
         Transformation t = bone.getTransformation();
         t.getScale().set(0.46F, 0.46F, 0.46F);
         bone.setTransformation(t);
         final Vector v = new Vector((this.random.nextDouble() - 0.5) * 0.3, this.random.nextDouble() * 0.5, (this.random.nextDouble() - 0.5) * 0.3);
         final Vector3f rot = new Vector3f(this.random.nextFloat() * 0.4F, this.random.nextFloat() * 0.4F, this.random.nextFloat() * 0.4F);
         (new BukkitRunnable() {
            final Vector vel = v.clone();
            int life = 0;

            @Override
            public void run() {
               Location c = bone.getLocation();
               this.vel.add(new Vector(0.0, -0.025, 0.0));
               c.add(this.vel);
               Transformation trans = bone.getTransformation();
               trans.getLeftRotation().rotateXYZ(rot.x, rot.y, rot.z);
               bone.setTransformation(trans);
               bone.teleport(c);
               if (c.getY() < -64.0 || this.life++ > 60) {
                  bone.remove();
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void spawnSmallFallingBone(Location loc, boolean hasTrail) {
      final ItemDisplay bone = loc.getWorld().spawn(loc, ItemDisplay.class);
      this.plugin.getVisualCleanupManager().track(bone);
      bone.setItemStack(new ItemStack(Material.BONE));
      bone.setInterpolationDuration(1);
      bone.setTeleportDuration(1);
      Transformation t = bone.getTransformation();
      t.getScale().set(0.4025F, 0.4025F, 0.4025F);
      bone.setTransformation(t);
      final Vector3f rot = new Vector3f(this.random.nextFloat() * 0.3F, this.random.nextFloat() * 0.3F, this.random.nextFloat() * 0.3F);
      (new BukkitRunnable() {
            int life = 0;
            final Vector v = new Vector(
               (BladeListener.this.random.nextDouble() - 0.5) * 0.25,
               BladeListener.this.random.nextDouble() * 0.2,
               (BladeListener.this.random.nextDouble() - 0.5) * 0.25
            );

            @Override
            public void run() {
               Location c = bone.getLocation();
               this.v.add(new Vector(0.0, -0.02, 0.0));
               c.add(this.v);
               Transformation trans = bone.getTransformation();
               trans.getLeftRotation().rotateXYZ(rot.x, rot.y, rot.z);
               bone.setTransformation(trans);
               bone.teleport(c);
               if (c.getY() < -64.0 || this.life++ > 40) {
                  bone.remove();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   @EventHandler
   public void onDeath(EntityDeathEvent e) {
      LivingEntity victim = e.getEntity();
      this.stunnedEntities.remove(victim.getUniqueId());
      if (victim.getKiller() != null) {
         Player killer = victim.getKiller();
         ItemStack item = killer.getInventory().getItemInMainHand();
         if (this.isBoneBlade(item)) {
            Location loc = victim.getLocation().add(0.0, 1.0, 0.0);
            victim.getWorld().playSound(loc, Sound.ENTITY_SKELETON_DEATH, 1.2F, 0.8F);
            victim.getWorld().spawnParticle(Particle.DUST, loc, 20, 0.4, 0.4, 0.4, 0.1, this.BONE_COLOR);

            for (int i = 0; i < 8; i++) {
               this.spawnSmallFallingBone(
                  loc.clone().add(this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5), false
               );
            }
         }
      }
   }
}

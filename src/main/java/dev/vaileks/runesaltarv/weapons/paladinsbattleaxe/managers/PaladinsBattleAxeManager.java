package dev.vaileks.runesaltarv.weapons.paladinsbattleaxe.managers;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.ParticleUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

public class PaladinsBattleAxeManager {
   private final RunesAltarV plugin;
   private final NamespacedKey weaponKey;
   private final Map<String, Long> cooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("paladins_battle_axe","");
   private final Map<String, BossBar> cooldownBars = new ConcurrentHashMap<>();
   private final Set<UUID> absorbingPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private final Map<UUID, Double> absorbedDamage = new ConcurrentHashMap<>();
   private static final float HAMMER_HALF_HEIGHT = 0.5F;

   public PaladinsBattleAxeManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.weaponKey = new NamespacedKey("runesaltarv", "paladins_battle_axe");
   }

   public NamespacedKey getWeaponKey() {
      return this.weaponKey;
   }

   public boolean isPaladinsBattleAxe(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "paladins_battle_axe");
   }

   public void setPaladinsBattleAxeAttributes(ItemStack item, double damage, double attackSpeed) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
         meta.removeAttributeModifier(Attribute.ATTACK_SPEED);
         NamespacedKey dmgKey = new NamespacedKey("runesaltarv", "paladin_damage");
         NamespacedKey spdKey = new NamespacedKey("runesaltarv", "paladin_speed");
         meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(dmgKey, damage - 1.0, Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
         meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(spdKey, attackSpeed - 4.0, Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
         meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
         item.setItemMeta(meta);
      }
   }

   public boolean isOnCooldown(Player player, String ability) {
      String key = player.getUniqueId() + ":" + ability;
      return this.cooldowns.getOrDefault(key, 0L) > System.currentTimeMillis();
   }

   public void applyCooldown(final Player player, String ability, final int seconds, String title) {
      final String key = player.getUniqueId() + ":" + ability;
      final long expireTime = System.currentTimeMillis() + (long)seconds * 1000L;
      this.cooldowns.put(key, expireTime);
      BossBar oldBar = this.cooldownBars.remove(key);
      if (oldBar != null) {
         oldBar.removeAll();
      }

      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(ChatColor.YELLOW + ChatColor.stripColor(title), BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(player);
      this.cooldownBars.put(key, bar);
      (new BukkitRunnable() {
            @Override
            public void run() {
               if (player.isOnline()
                  && PaladinsBattleAxeManager.this.cooldownBars.containsKey(key)
                  && PaladinsBattleAxeManager.this.cooldownBars.get(key).equals(bar)) {
                  long timeLeft = expireTime - System.currentTimeMillis();
                  if (timeLeft <= 0L) {
                     bar.removeAll();
                     PaladinsBattleAxeManager.this.cooldownBars.remove(key);
                     this.cancel();
                  } else {
                     double progress = (double)timeLeft / ((double)seconds * 1000.0);
                     bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                  }
               } else {
                  bar.removeAll();
                  PaladinsBattleAxeManager.this.cooldownBars.remove(key);
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 2L);
   }

   public void resetCooldowns(Player player) {
      UUID uuid = player.getUniqueId();
      this.cooldowns.keySet().removeIf(k -> k.startsWith(uuid.toString() + ":"));
      this.cooldownBars.keySet().removeIf(k -> {
         if (k.startsWith(uuid.toString() + ":")) {
            BossBar bar = this.cooldownBars.get(k);
            if (bar != null) {
               bar.removeAll();
            }

            return true;
         } else {
            return false;
         }
      });
      this.absorbingPlayers.remove(uuid);
      this.absorbedDamage.remove(uuid);
   }

   public void clearAllBars() {
      this.cooldowns.clear();

      for (BossBar bar : this.cooldownBars.values()) {
         bar.removeAll();
      }

      this.cooldownBars.clear();
      this.absorbingPlayers.clear();
      this.absorbedDamage.clear();
   }

   public boolean isAbsorbing(UUID uuid) {
      return this.absorbingPlayers.contains(uuid);
   }

   public void recordAbsorbedDamage(UUID uuid, double damage) {
      double cap = this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.damage-cap", 30.0);
      double ratio = this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.absorption-ratio", 1.0);
      this.absorbedDamage.compute(uuid, (k, current) -> {
         double newVal = (current == null ? 0.0 : current) + damage * ratio;
         return Math.min(cap, newVal);
      });
   }

   public void useEarthShatter(final Player player) {
      int cdSec = this.plugin.getWeaponsConfig().getInt("paladins-battle-axe.earth-shatter.cooldown", 45);
      if (!this.isOnCooldown(player, "earth_shatter")) {
         if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().canUseAbilities(player)) {
            this.plugin.getWorldGuardManager().notifyDeniedChat(player);
         } else {
            Location candidateTarget = null;
            RayTraceResult entityTrace = player.getWorld()
               .rayTrace(player.getEyeLocation(), player.getEyeLocation().getDirection(), this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.earth-shatter.range",24.0), org.bukkit.FluidCollisionMode.NEVER, true, 0.6, e -> e != player && e instanceof LivingEntity);
            if (entityTrace != null && entityTrace.getHitEntity() instanceof LivingEntity living) {
               Block ground = this.getSurfaceBlock(
                  living.getWorld(), living.getLocation().getBlockX(), living.getLocation().getBlockY(), living.getLocation().getBlockZ()
               );
               if (ground != null) {
                  candidateTarget = ground.getLocation().add(0.5, 1.0, 0.5);
               } else {
                  candidateTarget = living.getLocation().clone();
               }
            } else {
               RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.earth-shatter.range",24.0));
               if (rayTrace != null && rayTrace.getHitBlock() != null) {
                  candidateTarget = rayTrace.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
               }
            }

            final Location resolvedTarget=candidateTarget;
            if (resolvedTarget != null) {
               if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().isLocationAllowed(resolvedTarget, player)) {
                  this.plugin.getWorldGuardManager().notifyDeniedChat(player);
               } else {
                  this.applyCooldown(player, "earth_shatter", cdSec, this.plugin.tr("§6§lᴄоᴋрушᴇниᴇ зᴇмли", "§6§lᴇᴀʀᴛʜsʜᴀᴛᴛᴇʀ"));
                  player.swingMainHand();
                  ItemStack clayBall = new ItemStack(Material.CLAY_BALL);
                  ItemMeta meta = clayBall.getItemMeta();
                  if (meta != null) {
                     meta.setCustomModelData(8);
                     clayBall.setItemMeta(meta);
                  }

                  double baseY = 4.0;
                  Location spawnLoc = resolvedTarget.clone().add(0.0, 4.0, 0.0);
                  spawnLoc.setYaw(0.0F);
                  spawnLoc.setPitch(0.0F);
                  final World world = resolvedTarget.getWorld();
                  if (world != null) {
                     final double groundY = resolvedTarget.getY();
                     world.playSound(resolvedTarget, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 0.5F);
                     world.playSound(resolvedTarget, Sound.ENTITY_BREEZE_CHARGE, 1.0F, 0.1F);
                     world.playSound(resolvedTarget, Sound.ENTITY_IRON_GOLEM_STEP, 1.0F, 0.6F);
                     player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 0.5F);
                     player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 1.0F, 0.1F);
                     player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_STEP, 1.0F, 0.6F);
                     Vector dirToOwner = player.getLocation().toVector().subtract(resolvedTarget.toVector()).setY(0);
                     if (dirToOwner.lengthSquared() < 0.001) {
                        dirToOwner = new Vector(0, 0, 1);
                     }

                     final float yawToOwner = (float)Math.toDegrees(Math.atan2(-dirToOwner.getX(), dirToOwner.getZ()));
                     final ItemDisplay display = world.spawn(spawnLoc, ItemDisplay.class, ent -> {
                        ent.setItemStack(clayBall);
                        ent.setViewRange(4.0F);
                        ent.setTeleportDuration(0);
                        this.applyHammerTransform(ent, 6.0F, yawToOwner, 0, 0.0F);
                     });
                     this.plugin.getVisualCleanupManager().track(display);
                     this.spawnTargetPillar(world, resolvedTarget);
                     double riseHeight = 10.5;
                     final int[] fallStart = new int[]{0, 6, 10};
                     final int[] fallDur = new int[]{6, 4, 2};
                     final float[] fallTarget = new float[]{8.5F, 4.0F, 0.0F};
                     (new BukkitRunnable() {
                           int tick = 0;

                           @Override
                           public void run() {
                              int curTick = this.tick++;
                              if (display.isValid() && !display.isDead()) {
                                 int t1 = 18;
                                 int t2 = 24;
                                 int t3 = 36;
                                 int t4 = 61;

                                 try {
                                    if (curTick < t1) {
                                       double progress = (double)curTick / 18.0;
                                       double ease = 1.0 - Math.pow(1.0 - progress, 2.0);
                                       float h = (float)(10.5 * ease);
                                       float rotY = yawToOwner + (float)((1.0 - progress) * 360.0);
                                       PaladinsBattleAxeManager.this.applyHammerTransform(display, 6.0F, rotY, 2, h);
                                       PaladinsBattleAxeManager.this.spawnTargetPillar(world, resolvedTarget);
                                       PaladinsBattleAxeManager.this.spawnAxeDust(world, resolvedTarget, groundY + 4.0 + (double)h);
                                    } else if (curTick < t2) {
                                       float hoverBob = (float)(Math.sin((double)(curTick - t1) * 0.8) * 0.35);
                                       float h = 10.5F + hoverBob;
                                       if (curTick == t2 - 1) {
                                          h = 10.5F;
                                       }

                                       PaladinsBattleAxeManager.this.applyHammerTransform(display, 6.0F, yawToOwner, 2, h);
                                       PaladinsBattleAxeManager.this.spawnTargetPillar(world, resolvedTarget);
                                       PaladinsBattleAxeManager.this.spawnAxeDust(world, resolvedTarget, groundY + 4.0 + (double)h);
                                    } else if (curTick < t3) {
                                       int ft = curTick - t2;

                                       for (int s = 0; s < fallStart.length; s++) {
                                          if (ft == fallStart[s]) {
                                             PaladinsBattleAxeManager.this.applyHammerTransform(display, 6.0F, yawToOwner, fallDur[s], fallTarget[s]);
                                          }
                                       }

                                       PaladinsBattleAxeManager.this.spawnTargetPillar(world, resolvedTarget);
                                       double progress = (double)ft / 12.0;
                                       double h = 10.5 * (1.0 - progress * progress);
                                       PaladinsBattleAxeManager.this.spawnAxeDust(world, resolvedTarget, groundY + 4.0 + h);
                                    } else if (curTick == t3) {
                                       PaladinsBattleAxeManager.this.applyHammerTransform(display, 6.0F, yawToOwner, 0, 0.0F);
                                       world.playSound(resolvedTarget, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 3.5F, 0.1F);
                                       Set<UUID> directHit = new HashSet<>();

                                       try {
                                          double directRadius = PaladinsBattleAxeManager.this.plugin
                                             .getWeaponsConfig()
                                             .getDouble("paladins-battle-axe.earth-shatter.direct-radius", 1.6);
                                          double directDamage = PaladinsBattleAxeManager.this.plugin
                                             .getWeaponsConfig()
                                             .getDouble("paladins-battle-axe.earth-shatter.direct-damage", 12.0);
                                          double topY = groundY + 4.0 + 3.0;

                                          for (Entity ent : world.getNearbyEntities(resolvedTarget, directRadius + 0.5, 7.0, directRadius + 0.5)) {
                                             if (ent instanceof LivingEntity) {
                                                LivingEntity target = (LivingEntity)ent;
                                                if (!target.equals(player)
                                                   && !PaladinsBattleAxeManager.this.plugin
                                                      .getFriendManager()
                                                      .isFriend(player.getUniqueId(), target.getUniqueId())
                                                   && (
                                                      PaladinsBattleAxeManager.this.plugin.getWorldGuardManager() == null
                                                         || PaladinsBattleAxeManager.this.plugin.getWorldGuardManager().canAffectTarget(player, target)
                                                   )) {
                                                   Location tl = target.getLocation();
                                                   double dx = tl.getX() - resolvedTarget.getX();
                                                   double dz = tl.getZ() - resolvedTarget.getZ();
                                                   if (!(dx * dx + dz * dz > directRadius * directRadius)
                                                      && !(tl.getY() < groundY - 1.0)
                                                      && !(tl.getY() > topY)) {
                                                      if (!CombatUtils.clearPath(resolvedTarget.clone().add(0,0.8,0),target)) continue;
                                                      PaladinsBattleAxeManager.this.plugin.getCleanDamageManager().apply(target, player, directDamage);
                                                      directHit.add(target.getUniqueId());
                                                   }
                                                }
                                             }
                                          }
                                       } catch (Throwable var23) {
                                          PaladinsBattleAxeManager.this.plugin.getLogger().warning("Earthshatter damage error: " + var23);
                                       }

                                       try {
                                          int radius = PaladinsBattleAxeManager.this.plugin
                                             .getWeaponsConfig()
                                             .getInt("paladins-battle-axe.earth-shatter.radius", 8);
                                          int maxR = Math.min(16, Math.max(1, radius));
                                          PaladinsBattleAxeManager.this.createRisingBlockWave(resolvedTarget, maxR, player, directHit);
                                       } catch (Throwable var22) {
                                          PaladinsBattleAxeManager.this.plugin.getLogger().warning("Earthshatter wave error: " + var22);
                                       }

                                       Block centerGround = world.getBlockAt(
                                          resolvedTarget.getBlockX(), resolvedTarget.getBlockY() - 1, resolvedTarget.getBlockZ()
                                       );
                                       BlockData hitBData = centerGround.getType().isAir()
                                          ? Material.COBBLESTONE.createBlockData()
                                          : centerGround.getBlockData();

                                       try {
                                          PaladinsBattleAxeManager.this.spawnFlash(world, resolvedTarget, player);
                                          PaladinsBattleAxeManager.this.spawnHolyParticleDispersion(world, resolvedTarget, player);
                                          ParticleUtils.spawnBlockDispersion(PaladinsBattleAxeManager.this.plugin, resolvedTarget, hitBData, 6.0, 150);
                                       } catch (Throwable var21) {
                                          PaladinsBattleAxeManager.this.plugin.getLogger().warning("Earthshatter particles error: " + var21);
                                       }
                                    } else if (curTick >= t4) {
                                       if (display.isValid()) {
                                          (new BukkitRunnable() {
                                             float scale = 6.0F;
                                             int count = 0;

                                             @Override
                                             public void run() {
                                                if (this.count < 10 && display.isValid()) {
                                                   this.scale -= 0.6F;
                                                   if (this.scale < 0.0F) {
                                                      this.scale = 0.0F;
                                                   }

                                                   Transformation trans = display.getTransformation();
                                                   trans.getScale().set(this.scale, this.scale, this.scale);
                                                   trans.getTranslation().set(0.0F, -0.5F * (6.0F - this.scale), 0.0F);
                                                   display.setInterpolationDuration(2);
                                                   display.setInterpolationDelay(0);
                                                   display.setTransformation(trans);
                                                   this.count++;
                                                } else {
                                                   if (display.isValid()) {
                                                      PaladinsBattleAxeManager.this.plugin.getVisualCleanupManager().untrack(display);
                                                      display.remove();
                                                   }

                                                   this.cancel();
                                                }
                                             }
                                          }).runTaskTimer(PaladinsBattleAxeManager.this.plugin, 0L, 1L);
                                       }

                                       this.cancel();
                                       return;
                                    }
                                 } catch (Throwable var24) {
                                    PaladinsBattleAxeManager.this.plugin
                                       .getLogger()
                                       .warning("Error in Earthshatter tick " + curTick + ": " + var24.getMessage());
                                 }
                              } else {
                                 this.cancel();
                              }
                           }
                        })
                        .runTaskTimer(this.plugin, 0L, 1L);
                  }
               }
            }
         }
      }
   }

   private void spawnFlash(World world, Location loc, Player player) {
      Location flashLoc = loc.clone().add(0.0, 1.5, 0.0);
      Color whiteColor = Color.fromRGB(255, 255, 255);

      try {
         world.spawnParticle(Particle.FLASH, flashLoc, 9, 0.6, 1.5, 0.6, 0.0, whiteColor);
         if (player != null && player.isOnline()) {
            player.spawnParticle(Particle.FLASH, flashLoc, 9, 0.6, 1.5, 0.6, 0.0, whiteColor);
         }
      } catch (Throwable var9) {
         try {
            world.spawnParticle(Particle.FLASH, flashLoc, 9, 0.6, 1.5, 0.6, 0.0);
            if (player != null && player.isOnline()) {
               player.spawnParticle(Particle.FLASH, flashLoc, 9, 0.6, 1.5, 0.6, 0.0);
            }
         } catch (Throwable var8) {
         }
      }
   }

   public void spawnHolyParticleDispersion(World world, Location centerLoc, Player player) {
      if (world != null && centerLoc != null) {
         Location origin = centerLoc.clone().add(0.0, 1.2, 0.0);
         int count = 72;

         for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2) * (double)i / (double)count + (Math.random() - 0.5) * 0.25;
            double h = 0.18 + Math.random() * 0.28;
            double vx = Math.cos(angle) * h;
            double vz = Math.sin(angle) * h;
            double r = Math.random();
            double vy;
            if (r < 0.4) {
               vy = 0.25 + Math.random() * 0.35;
            } else if (r < 0.7) {
               vy = -0.05 + Math.random() * 0.15;
            } else {
               vy = -0.15 - Math.random() * 0.3;
            }

            world.spawnParticle(Particle.FIREWORK, origin, 0, vx, vy, vz, 1.0, null, true);
            world.spawnParticle(Particle.END_ROD, origin, 0, vx * 0.85, vy * 0.85, vz * 0.85, 1.0, null, true);
            world.spawnParticle(Particle.ELECTRIC_SPARK, origin, 0, vx * 1.15, vy * 1.15, vz * 1.15, 1.0, null, true);
            if (player != null && player.isOnline()) {
               player.spawnParticle(Particle.FIREWORK, origin, 0, vx, vy, vz, 1.0);
               player.spawnParticle(Particle.END_ROD, origin, 0, vx * 0.85, vy * 0.85, vz * 0.85, 1.0);
               player.spawnParticle(Particle.ELECTRIC_SPARK, origin, 0, vx * 1.15, vy * 1.15, vz * 1.15, 1.0);
            }
         }
      }
   }

   private void spawnAxeDust(World world, Location targetLoc, double axeCenterY) {
      double bottomY = axeCenterY - 3.0;
      Location fx = new Location(world, targetLoc.getX(), bottomY, targetLoc.getZ());
      world.spawnParticle(Particle.DUST, fx, 6, 0.35, 0.15, 0.35, 0.0, new DustOptions(Color.fromRGB(255, 215, 0), 1.4F));
   }

   private void spawnTargetPillar(World world, Location targetLoc) {
      for (int i = 0; i < 10; i++) {
         double sy = 0.1 + (double)i * 0.22;
         world.spawnParticle(Particle.ELECTRIC_SPARK, targetLoc.clone().add(0.0, sy, 0.0), 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   public void applyHammerTransform(ItemDisplay display, float scale, float rotYDegrees) {
      this.applyHammerTransform(display, scale, rotYDegrees, 1, 0.0F);
   }

   public void applyHammerTransform(ItemDisplay display, float scale, float rotYDegrees, int interpDuration) {
      this.applyHammerTransform(display, scale, rotYDegrees, interpDuration, 0.0F);
   }

   public void applyHammerTransform(ItemDisplay display, float scale, float rotYDegrees, int interpDuration, float translationY) {
      Transformation trans = display.getTransformation();
      trans.getScale().set(scale, scale, scale);
      trans.getTranslation().set(0.0F, translationY, 0.0F);
      Quaternionf q = new Quaternionf();
      q.rotateY((float)Math.toRadians((double)(-rotYDegrees) + 180.0));
      q.rotateX((float) Math.PI);
      trans.getRightRotation().set(q);
      display.setInterpolationDelay(0);
      display.setInterpolationDuration(interpDuration);
      display.setTransformation(trans);
   }

   private void circleWaveParticle(Location center, int r, World world) {
      Block groundBlock = center.clone().add(0.0, -1.0, 0.0).getBlock();
      if (groundBlock.getType().isAir()) {
         Material.COBBLESTONE.createBlockData();
      } else {
         groundBlock.getBlockData();
      }

      DustOptions dustOpt = new DustOptions(Color.fromRGB(220, 180, 80), 1.2F);

      for (double angle = 0.0; angle < Math.PI * 2; angle += 0.2) {
         double deg = Math.toDegrees(angle) % 360.0;
         boolean skip = false;

         for (int i = 0; i < 8; i++) {
            double targetDeg = (double)i * 45.0;
            double diff = Math.abs(deg - targetDeg);
            if (diff > 180.0) {
               diff = 360.0 - diff;
            }

            if (diff < 10.0) {
               skip = true;
               break;
            }
         }

         if (!skip) {
            double px = center.getX() + (double)r * Math.cos(angle);
            double pz = center.getZ() + (double)r * Math.sin(angle);
            Location pLoc = new Location(world, px, center.getY() + 0.2, pz);
            world.spawnParticle(Particle.DUST, pLoc, 2, 0.0, 0.1, 0.0, 0.0, dustOpt);
         }
      }
   }

   public void createRisingBlockWave(Location center, int maxRadius, Player player) {
      this.createRisingBlockWave(center, maxRadius, player, Collections.emptySet());
   }

   public void createRisingBlockWave(Location center, int maxRadius, Player player, Set<UUID> directHit) {
      World world = center.getWorld();
      if (world != null) {
         Map<Integer, List<Block>> blocksByDistance = new HashMap<>();
         int centerBX = center.getBlockX();
         int centerBZ = center.getBlockZ();
         int baseY = center.getBlockY() - 1;

         for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dz = -maxRadius; dz <= maxRadius; dz++) {
               if (dx * dx + dz * dz <= maxRadius * maxRadius && (dx != 0 || dz != 0)) {
                  int dist = (int)Math.round(Math.sqrt((double)(dx * dx + dz * dz)));
                  if (dist >= 1) {
                     int x = centerBX + dx;
                     int z = centerBZ + dz;
                     Block surface = this.getSurfaceBlock(world, x, baseY + 3, z);
                     if (surface != null) {
                        blocksByDistance.computeIfAbsent(dist, k -> new ArrayList<>()).add(surface);
                     }
                  }
               }
            }
         }

         double shatterDamage = this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.earth-shatter.damage", 7.0);
         double shatterKb = this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.earth-shatter.knockback", 0.85);
         Set<UUID> hitByWave = new HashSet<>();

         for (Entry<Integer, List<Block>> entry : blocksByDistance.entrySet()) {
            int dist = entry.getKey();
            List<Block> blocks = entry.getValue();
            long delay = (long)(dist - 1) * 2L;
            Bukkit.getScheduler()
               .runTaskLater(
                  this.plugin,
                  () -> {
                     try {
                        for (Block block : blocks) {
                           Material mat = block.getType();
                           if (!mat.isAir() && mat != Material.WATER && mat != Material.LAVA) {
                              BlockData bData = block.getBlockData();
                              Location loc = block.getLocation();
                              int dx = block.getX() - centerBX;
                              int dzx = block.getZ() - centerBZ;
                              boolean isPlus = Math.abs(dx) <= 1 || Math.abs(dzx) <= 1;
                              boolean isCross = Math.abs(Math.abs(dx) - Math.abs(dzx)) <= 1;
                              double jumpH;
                              if (isPlus) {
                                 jumpH = Math.max(1.3, 2.0 - (double)dist * 0.08);
                              } else if (isCross) {
                                 jumpH = Math.max(1.0, 1.6 - (double)dist * 0.08);
                              } else {
                                 jumpH = Math.max(0.6, 1.1 - (double)dist * 0.08);
                              }

                              double finalJumpH = jumpH + (Math.random() - 0.5) * 0.35;
                              Location blockCenter = loc.clone().add(0.5, 0.5, 0.5);
                              Material itemMat = mat.isItem() ? mat : Material.STONE;
                              ItemDisplay blockDisplay = world.spawn(blockCenter, ItemDisplay.class, entx -> {
                                 entx.setItemStack(new ItemStack(itemMat));
                                 entx.setBrightness(new Brightness(15, 15));
                                 entx.setTeleportDuration(5);
                              });
                              this.plugin.getVisualCleanupManager().track(blockDisplay);
                              Location targetUp = blockCenter.clone().add(0.0, finalJumpH, 0.0);
                              Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                 if (blockDisplay.isValid()) {
                                    blockDisplay.teleport(targetUp);
                                    world.spawnParticle(Particle.ELECTRIC_SPARK, targetUp.clone().add(0.0, 0.4, 0.0), 1, 0.05, 0.05, 0.05, 0.01);
                                 }
                              }, 2L);
                              Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                 if (blockDisplay.isValid()) {
                                    blockDisplay.teleport(blockCenter);
                                 }
                              }, 9L);
                              Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                 if (blockDisplay.isValid()) {
                                    this.plugin.getVisualCleanupManager().untrack(blockDisplay);
                                    blockDisplay.remove();
                                 }
                              }, 15L);
                           }
                        }

                        for (Entity ent : world.getNearbyEntities(center, (double)dist + 1.2, 4.0, (double)dist + 1.2)) {
                           if (ent instanceof LivingEntity) {
                              LivingEntity victim = (LivingEntity)ent;
                              if (!victim.equals(player)
                                 && (directHit == null || !directHit.contains(victim.getUniqueId()))
                                 && !hitByWave.contains(victim.getUniqueId())) {
                                 if (victim instanceof Player) {
                                    Player pVictim = (Player)victim;
                                    if (this.plugin.getFriendManager().isFriend(player.getUniqueId(), pVictim.getUniqueId())) {
                                       continue;
                                    }
                                 }

                                 if (this.plugin.getWorldGuardManager() == null || this.plugin.getWorldGuardManager().canAffectTarget(player, victim)) {
                                    double dX = victim.getLocation().getX() - center.getX();
                                    double dZ = victim.getLocation().getZ() - center.getZ();
                                    double d2 = dX * dX + dZ * dZ;
                                    if (d2 >= ((double)dist - 1.2) * ((double)dist - 1.2) && d2 <= ((double)dist + 1.2) * ((double)dist + 1.2)) {
                                       if (!CombatUtils.clearPath(center.clone().add(0,0.8,0),victim)) continue;
                                       hitByWave.add(victim.getUniqueId());
                                       this.plugin.getCleanDamageManager().apply(victim, player, shatterDamage);
                                       victim.setVelocity(new Vector(0.0, shatterKb, 0.0));
                                    }
                                 }
                              }
                           }
                        }
                     } catch (Throwable var31) {
                        this.plugin.getLogger().warning("Error in block wave: " + var31.getMessage());
                     }
                  },
                  delay
               );
         }
      }
   }

   public void spawnMaceSmashBlockDispersal(Location center, BlockData blockData) {
      ParticleUtils.spawnBlockDispersion(this.plugin, center, blockData, 4.5);
   }

   private Block getSurfaceBlock(World world, int x, int startY, int z) {
      for (int y = startY; y >= startY - 20; y--) {
         Block block = world.getBlockAt(x, y, z);
         Material mat = block.getType();
         if (!mat.isAir()
            && mat != Material.SHORT_GRASS
            && mat != Material.TALL_GRASS
            && mat != Material.FERN
            && mat != Material.LARGE_FERN
            && mat != Material.WATER
            && mat != Material.LAVA) {
            return block;
         }
      }

      return null;
   }

   public void useStalwartAbsorption(final Player player) {
      int cdSec = this.plugin.getWeaponsConfig().getInt("paladins-battle-axe.stalwart-absorption.cooldown", 45);
      if (!this.isOnCooldown(player, "stalwart_absorption")) {
         if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().canUseAbilities(player)) {
            this.plugin.getWorldGuardManager().notifyDeniedChat(player);
         } else {
            final UUID uuid = player.getUniqueId();
            if (!this.absorbingPlayers.contains(uuid)) {
               this.applyCooldown(player, "stalwart_absorption", cdSec, this.plugin.tr("§6§lᴄᴛойᴋоᴇ поглощᴇниᴇ", "§6§lsᴛᴀʟᴡᴀʀᴛ ᴀʙsᴏʀᴘᴛɪᴏɴ"));
               player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.5F, 2.0F);
               this.absorbingPlayers.add(uuid);
               this.absorbedDamage.put(uuid, 0.0);
               int durationSec = this.plugin.getWeaponsConfig().getInt("paladins-battle-axe.stalwart-absorption.duration-seconds", 5);
               final int durationTicks = durationSec * 20;
               player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 0, false, true, true));
               (new BukkitRunnable() {
                     int a = 0;

                     @Override
                     public void run() {
                        if (this.a < durationTicks && player.isOnline() && !player.isDead() && PaladinsBattleAxeManager.this.absorbingPlayers.contains(uuid)) {
                           Location pLoc = player.getLocation();

                           for (int i = 0; i < 4; i++) {
                              double ox = (Math.random() - 0.5) * 0.85;
                              double oy = Math.random() * 1.85;
                              double oz = (Math.random() - 0.5) * 0.85;
                              Location auraLoc = pLoc.clone().add(ox, oy, oz);
                              player.getWorld().spawnParticle(Particle.DUST, auraLoc, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(Color.fromRGB(255, 215, 0), 1.2F));
                           }

                           double absorbed = PaladinsBattleAxeManager.this.absorbedDamage.getOrDefault(uuid, 0.0);
                           String valStr = absorbed == Math.floor(absorbed) ? String.valueOf((int)absorbed) : String.format(Locale.US, "%.1f", absorbed);
                           TextUtils.sendActionBar(
                              player,
                              TextUtils.shadow(
                                 TextUtils.legacy(PaladinsBattleAxeManager.this.plugin.tr("&6поглощᴇно уронᴀ &e" + valStr, "&6ᴅᴀᴍᴀɢᴇ ᴀʙsᴏʀʙᴇᴅ &e" + valStr))
                              )
                           );
                           this.a++;
                        } else {
                           PaladinsBattleAxeManager.this.releaseAbsorbedDamage(player);
                           this.cancel();
                        }
                     }
                  })
                  .runTaskTimer(this.plugin, 0L, 1L);
            }
         }
      }
   }

   public double getShockwaveDamage(double absorbed) {
      double minimum=this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.min-shockwave-damage",2.0);
      double maximum=this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.max-shockwave-damage",5.0);
      double ratio=this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.release-multiplier",0.5);
      return Math.max(minimum,Math.min(maximum,Math.max(0,absorbed)*ratio));
   }

   public void releaseAbsorbedDamage(Player player) {
      UUID uuid = player.getUniqueId();
      if (this.absorbingPlayers.remove(uuid)) {
         double cap = this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.damage-cap", 15.0);
         double absorbed = Math.min(cap, this.absorbedDamage.getOrDefault(uuid, 0.0));
         this.absorbedDamage.remove(uuid);
         if (player.isOnline()) {
            String valStr = absorbed == Math.floor(absorbed) ? String.valueOf((int)absorbed) : String.format(Locale.US, "%.1f", absorbed);
            TextUtils.sendActionBar(
               player, TextUtils.shadow(TextUtils.legacy(this.plugin.tr("&6&nпоглощᴇно уронᴀ &e&n" + valStr, "&6&nᴅᴀᴍᴀɢᴇ ᴀʙsᴏʀʙᴇᴅ &e&n" + valStr)))
            );
            double knockback = this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.knockback",1.1);
            double releaseDmg = this.getShockwaveDamage(absorbed);
            Location loc = player.getLocation();
            World world = player.getWorld();
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 1.0F);
            world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0F, 0.8F);
            world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0.0, 1.0, 0.0), 2, 0.8, 0.5, 0.8, 0.1);
            world.spawnParticle(Particle.DUST, loc.clone().add(0.0, 0.5, 0.0), 70, 2.0, 0.5, 2.0, 0.1, new DustOptions(Color.fromRGB(255, 220, 50), 2.0F));
            double radius = this.plugin.getWeaponsConfig().getDouble("paladins-battle-axe.stalwart-absorption.release-radius", 6.0);

            for (Entity ent : world.getNearbyEntities(loc, radius, radius, radius)) {
               if (ent instanceof LivingEntity) {
                  LivingEntity target = (LivingEntity)ent;
                  if (!target.getUniqueId().equals(player.getUniqueId())
                     && !this.plugin.getFriendManager().isFriend(player.getUniqueId(), target.getUniqueId())
                     && (this.plugin.getWorldGuardManager() == null || this.plugin.getWorldGuardManager().canAffectTarget(player, target))) {
                     if (!CombatUtils.clearPath(loc.clone().add(0,1,0),target)) continue;
                     this.plugin.getCleanDamageManager().apply(target, player, releaseDmg);
                     Vector kb = target.getLocation().toVector().subtract(loc.toVector());
                     if (kb.lengthSquared() > 0.001) {
                        kb.normalize().multiply(knockback).setY(0.45);
                     } else {
                        kb = new Vector(0.0, 0.5, 0.0);
                     }

                     target.setVelocity(kb);
                  }
               }
            }
         }
      }
   }
}

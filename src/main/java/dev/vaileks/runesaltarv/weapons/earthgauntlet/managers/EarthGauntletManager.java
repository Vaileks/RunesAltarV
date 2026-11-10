package dev.vaileks.runesaltarv.weapons.earthgauntlet.managers;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.ParticleUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class EarthGauntletManager implements Listener {
   private final RunesAltarV plugin;
   private final EarthBossBarManager bossBarManager;
   private final Random random = new Random();
   private final Map<UUID, BukkitTask> activeMeteorStrikes = new HashMap<>();
   private final Map<UUID, UUID> pullableTargets = new HashMap<>();
   private final Map<UUID, LinkedList<BlockState>> activeMudTrails = new HashMap<>();
   private final Set<UUID> stunnedTargets = new HashSet<>();
   private final BlockData mudBlockData = Bukkit.createBlockData(Material.MUD);
   private final BlockData packedMudBlockData = Bukkit.createBlockData(Material.PACKED_MUD);

   public EarthGauntletManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.bossBarManager = new EarthBossBarManager(plugin);
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   public void playPassiveHitSound(Player p) {
      p.getWorld().playSound(p.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.item-shield-block.sound",Sound.ITEM_SHIELD_BLOCK), SoundCategory.MASTER,AltarTuning.volume("earth_gauntlet","visuals.sounds.item-shield-block",1.0F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.item-shield-block",0.6F));
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent e) {
      if (this.stunnedTargets.contains(e.getPlayer().getUniqueId())) {
         Location from = e.getFrom();
         Location to = e.getTo();
         if (to != null) {
            if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() < to.getY()) {
               Location newTo = from.clone();
               newTo.setYaw(to.getYaw());
               newTo.setPitch(to.getPitch());
               e.setTo(newTo);
            }
         }
      }
   }

   public void activateMeteorStrike(final Player p) {
      if (!this.bossBarManager.isOnCooldown(p, "MeteorStrike")) {
         if (!this.activeMeteorStrikes.containsKey(p.getUniqueId())) {
            p.getWorld().playSound(p.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.entity-evoker-prepare-attack.sound",Sound.ENTITY_EVOKER_PREPARE_ATTACK),AltarTuning.volume("earth_gauntlet","visuals.sounds.entity-evoker-prepare-attack",1.0F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.entity-evoker-prepare-attack",1.0F));
            p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("earth_gauntlet",10), 0.5, 0.5, 0.5, 0.1);
            p.getWorld().spawnParticle(Particle.BLOCK, p.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("earth_gauntlet",20), 0.5, 1.0, 0.5, 1.0, this.packedMudBlockData);
            p.getWorld().spawnParticle(Particle.DRAGON_BREATH, p.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("earth_gauntlet",50), 1.0, 1.0, 1.0, 0.05, 1.0F);
            final int activeWindow = this.plugin.getWeaponsConfig().getInt("earth-gauntlet.meteor_strike.active_window", 30);
            this.bossBarManager.setActiveBar(p, "MeteorStrikeActive", AltarTuning.text("earth_gauntlet","messages.earth-gauntlet-manager.message-1","§a§lмᴇᴛᴇориᴛный удᴀр","§a§lᴍᴇᴛᴇᴏʀ sᴛʀɪᴋᴇ"), activeWindow);
            BukkitTask task = (new BukkitRunnable() {
                  int ticks = 0;

                  @Override
                  public void run() {
                     if (p.isOnline() && this.ticks < activeWindow * 20) {
                        p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("earth_gauntlet",1), 0.3, 0.5, 0.3, 0.0);
                        p.getWorld()
                           .spawnParticle(
                              Particle.BLOCK, p.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("earth_gauntlet",3), 0.3, 0.5, 0.3, 0.5, EarthGauntletManager.this.packedMudBlockData
                           );
                        p.getWorld().spawnParticle(Particle.DRAGON_BREATH, p.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("earth_gauntlet",5), 0.5, 0.5, 0.5, 0.01, 1.0F);
                        this.ticks++;
                     } else {
                        EarthGauntletManager.this.cleanupMeteor(p);
                        EarthGauntletManager.this.bossBarManager.removeActiveBar(p, "MeteorStrikeActive");
                        EarthGauntletManager.this.bossBarManager
                           .setCooldown(
                              p,
                              "MeteorStrike",
                              AltarTuning.text("earth_gauntlet","messages.earth-gauntlet-manager.message-2","§e§lмᴇᴛᴇориᴛный удᴀр","§e§lᴍᴇᴛᴇᴏʀ sᴛʀɪᴋᴇ"),
                              EarthGauntletManager.this.plugin.getWeaponsConfig().getInt("earth-gauntlet.meteor_strike.cooldown", 35)
                           );
                     }
                  }
               })
               .runTaskTimer(this.plugin, 0L, 1L);
            this.activeMeteorStrikes.put(p.getUniqueId(), task);
         }
      }
   }

   public void triggerMeteorStrikeHit(Player attacker, LivingEntity victim) {
      if (this.activeMeteorStrikes.containsKey(attacker.getUniqueId())) {
         this.activeMeteorStrikes.get(attacker.getUniqueId()).cancel();
         this.activeMeteorStrikes.remove(attacker.getUniqueId());
         this.bossBarManager.removeActiveBar(attacker, "MeteorStrikeActive");
         this.bossBarManager
            .setCooldown(
               attacker,
               "MeteorStrike",
               AltarTuning.text("earth_gauntlet","messages.earth-gauntlet-manager.message-3","§e§lмᴇᴛᴇориᴛный удᴀр","§e§lᴍᴇᴛᴇᴏʀ sᴛʀɪᴋᴇ"),
               this.plugin.getWeaponsConfig().getInt("earth-gauntlet.meteor_strike.cooldown", 35)
            );
         attacker.getWorld().playSound(victim.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.block-beacon-deactivate.sound",Sound.BLOCK_BEACON_DEACTIVATE),AltarTuning.volume("earth_gauntlet","visuals.sounds.block-beacon-deactivate",1.0F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.block-beacon-deactivate",1.0F));
         attacker.getWorld().playSound(victim.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.entity-wither-break-block.sound",Sound.ENTITY_WITHER_BREAK_BLOCK),AltarTuning.volume("earth_gauntlet","visuals.sounds.entity-wither-break-block",1.0F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.entity-wither-break-block",1.0F));

         try {
            attacker.getWorld().spawnParticle(Particle.valueOf("GUST_EMITTER_LARGE"), victim.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("earth_gauntlet",1), 0.0, 0.0, 0.0, 0.0);
         } catch (Exception var9) {
            attacker.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, victim.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("earth_gauntlet",1), 0.0, 0.0, 0.0, 0.0);
         }

         attacker.getWorld().spawnParticle(Particle.DRAGON_BREATH, victim.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("earth_gauntlet",80), 1.0, 1.0, 1.0, 0.1, 1.0F);
         double dmg = this.plugin.getWeaponsConfig().getDouble("earth-gauntlet.meteor_strike.damage", 4.0);
         this.plugin.getCleanDamageManager().apply(victim, attacker, dmg);
         victim.playHurtAnimation(0.0F);
         double kbMultiplier = this.plugin.getWeaponsConfig().getDouble("earth-gauntlet.meteor_strike.knockback", 2.5);
         Vector kb = attacker.getLocation().getDirection().setY(0).normalize().multiply(kbMultiplier).setY(0.35);
         victim.setVelocity(kb);
         int stunDurationTicks = this.plugin.getWeaponsConfig().getInt("earth-gauntlet.meteor_strike.stun_duration_ticks", 100);
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!victim.isDead() && victim.isValid()) {
               this.stunnedTargets.add(victim.getUniqueId());
               if (victim instanceof Mob mob) {
                  mob.setAware(false);
               }

               (new BukkitRunnable() {
                  int elapsedTicks = 0;

                  @Override
                  public void run() {
                     if (this.elapsedTicks < stunDurationTicks && victim.isValid() && !victim.isDead()) {
                        if (this.elapsedTicks % 20 == 0) {
                           victim.getWorld().playSound(victim.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.entity-zombie-attack-iron-door.sound",Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR),AltarTuning.volume("earth_gauntlet","visuals.sounds.entity-zombie-attack-iron-door",1.0F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.entity-zombie-attack-iron-door",0.8F));
                        }

                        this.elapsedTicks++;
                     } else {
                        EarthGauntletManager.this.stunnedTargets.remove(victim.getUniqueId());
                        if (victim instanceof Mob mob) {
                           mob.setAware(true);
                        }

                        this.cancel();
                     }
                  }
               }).runTaskTimer(this.plugin, 0L, 1L);
            }
         }, 10L);
      }
   }

   public void castMudslide(Player p) {
      if (!this.bossBarManager.isOnCooldown(p, "Mudslide")) {
         int cooldown = this.plugin.getWeaponsConfig().getInt("earth-gauntlet.mudslide.cooldown", 40);
         this.bossBarManager.setCooldown(p, "Mudslide", AltarTuning.text("earth_gauntlet","messages.earth-gauntlet-manager.message-4","§e§lоползᴇнь","§e§lᴍᴜᴅsʟɪᴅᴇ"), cooldown);
         p.getWorld().playSound(p.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.entity-breeze-inhale.sound",Sound.ENTITY_BREEZE_INHALE),AltarTuning.volume("earth_gauntlet","visuals.sounds.entity-breeze-inhale",1.0F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.entity-breeze-inhale",1.0F));
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (p.isOnline()) {
               this.launchMudBall(p);
            }
         }, 13L);
      }
   }

   private void launchMudBall(final Player p) {
      p.swingMainHand();
      Vector initialDirection = p.getEyeLocation().getDirection().normalize();
      final Location startLoc = p.getEyeLocation();
      final BlockDisplay display = p.getWorld().spawn(startLoc, BlockDisplay.class, ent -> {
         ent.setBlock(this.mudBlockData);
         ent.setInterpolationDuration(1);
         ent.setTeleportDuration(1);
      });
      this.plugin.getVisualCleanupManager().track(display);
      final Vector right = initialDirection.clone().setY(0).normalize().crossProduct(new Vector(0, 1, 0)).normalize();
      final LinkedList<BlockState> trail = new LinkedList<>();
      final Vector flatDirection = initialDirection.clone().setY(0).normalize();
      final double maxDist = this.plugin.getWeaponsConfig().getDouble("earth-gauntlet.mudslide.range", 50.0);
      final double speed = this.plugin.getWeaponsConfig().getDouble("earth-gauntlet.mudslide.speed", 1.2);
      final double curveThreshold = 0.55 + this.random.nextDouble() * 0.2;
      (new BukkitRunnable() {
            double distanceTraveled = 0.0;
            float currentAngle = 0.0F;
            Location currentLoc = startLoc.clone();
            boolean isCurving = false;
            int curveSign = 0;
            double curveIntensity = 0.0;
            final Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
               if (p.isOnline() && display.isValid()) {
                  this.currentAngle += 0.35F;
                  double stepSize = AltarTuning.number("earth_gauntlet","tuning.earth-gauntlet-manager.step-size",0.25,0.01,128);
                  int steps = (int)Math.ceil(speed / stepSize);
                  double actualStep = speed / (double)steps;

                  for (int s = 0; s < steps; s++) {
                     double nextX = this.currentLoc.getX() + flatDirection.getX() * actualStep;
                     double nextZ = this.currentLoc.getZ() + flatDirection.getZ() * actualStep;
                     int nX = (int)Math.floor(nextX);
                     int nZ = (int)Math.floor(nextZ);
                     int startY = (int)Math.floor(this.currentLoc.getY() + 3.0);
                     int endY = (int)Math.floor(this.currentLoc.getY() - 4.0);
                     double highestSolidY = Double.NEGATIVE_INFINITY;

                     for (int y = startY; y >= endY; y--) {
                        Block b = this.currentLoc.getWorld().getBlockAt(nX, y, nZ);
                        if (b.getType().isSolid()) {
                           highestSolidY = (double)y;
                           break;
                        }
                     }

                     boolean isClimbing = false;
                     if (highestSolidY != Double.NEGATIVE_INFINITY) {
                        double targetY = highestSolidY + 2.15;
                        if (targetY > this.currentLoc.getY() + 0.2) {
                           isClimbing = true;
                           double climbStep = actualStep * 0.45;
                           this.currentLoc.add(0.0, climbStep, 0.0);
                           this.distanceTraveled += climbStep;
                           Block wallBlock1 = this.currentLoc.getWorld().getBlockAt(nX, (int)Math.floor(this.currentLoc.getY()), nZ);
                           Block wallBlock2 = this.currentLoc.getWorld().getBlockAt(nX, (int)Math.floor(this.currentLoc.getY() - 1.0), nZ);
                           EarthGauntletManager.this.infectBlock(wallBlock1, trail);
                           EarthGauntletManager.this.infectBlock(wallBlock2, trail);
                           EarthGauntletManager.this.infectBlock(wallBlock1.getLocation().add(right).getBlock(), trail);
                           EarthGauntletManager.this.infectBlock(wallBlock2.getLocation().add(right).getBlock(), trail);
                        } else {
                           this.currentLoc.setX(nextX);
                           this.currentLoc.setZ(nextZ);
                           this.currentLoc.setY(targetY);
                           this.distanceTraveled += actualStep;
                           Block centerBlock = this.currentLoc.getWorld().getBlockAt(nX, (int)highestSolidY, nZ);
                           Block sideBlock = centerBlock.getLocation().add(right).getBlock();
                           EarthGauntletManager.this.infectBlock(centerBlock, trail);
                           EarthGauntletManager.this.infectBlock(sideBlock, trail);
                        }
                     } else {
                        this.currentLoc.setX(nextX);
                        this.currentLoc.setZ(nextZ);
                        this.currentLoc.subtract(0.0, actualStep, 0.0);
                        this.distanceTraveled += actualStep;
                     }

                     EarthGauntletManager.this.trampleVegetation(this.currentLoc, right);
                     double pct = this.distanceTraveled / maxDist;
                     if (pct >= curveThreshold && !isClimbing) {
                        if (!this.isCurving) {
                           this.isCurving = true;
                           this.curveSign = EarthGauntletManager.this.random.nextBoolean() ? 1 : -1;
                           this.curveIntensity = 0.015 + EarthGauntletManager.this.random.nextDouble() * 0.015;
                        }

                        flatDirection.rotateAroundY((double)this.curveSign * this.curveIntensity);
                     }

                     if (this.distanceTraveled >= maxDist) {
                        EarthGauntletManager.this.playImpactBurst(this.currentLoc, flatDirection);
                        display.remove();
                        if (!trail.isEmpty()) {
                           EarthGauntletManager.this.clearTrailGradually(trail);
                        }

                        this.cancel();
                        return;
                     }

                     boolean hit = false;

                     for (LivingEntity victim : this.currentLoc.getWorld().getLivingEntities()) {
                        if (!victim.equals(p)
                           && !victim.isDead()
                           && !this.hitTargets.contains(victim.getUniqueId())
                           && victim.getLocation().distanceSquared(this.currentLoc) < 5.0
                           && (
                              !(victim instanceof Player targetPlayer)
                                 || targetPlayer.getGameMode() != GameMode.SPECTATOR
                                    && (
                                       EarthGauntletManager.this.plugin.getFriendManager() == null
                                          || !EarthGauntletManager.this.plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())
                                    )
                           )) {
                           this.hitTargets.add(victim.getUniqueId());
                           EarthGauntletManager.this.hitEntity(p, victim, trail);
                           EarthGauntletManager.this.playImpactBurst(victim.getLocation().add(0.0, 1.0, 0.0), flatDirection);
                           display.remove();
                           hit = true;
                           break;
                        }
                     }

                     if (hit) {
                        this.cancel();
                        return;
                     }
                  }

                  if (display.isValid()) {
                     Location dirLoc = this.currentLoc.clone();
                     dirLoc.setDirection(flatDirection);
                     EarthGauntletManager.this.updateDisplayRotation(display, this.currentLoc, dirLoc.getYaw(), this.currentAngle);
                  }
               } else {
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void trampleVegetation(Location loc, Vector right) {
      Block[] blocksToCheck = new Block[]{
         loc.getBlock(),
         loc.getBlock().getRelative(BlockFace.UP),
         loc.getBlock().getRelative(BlockFace.DOWN),
         loc.clone().add(right).getBlock(),
         loc.clone().subtract(right).getBlock()
      };

      for (Block b : blocksToCheck) {
         Material m = b.getType();
         if (this.isPlantOrWeb(m)) {
            b.breakNaturally();
         }
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

   private void updateDisplayRotation(BlockDisplay display, Location target, float dynamicYaw, float currentAngle) {
      Location loc = target.clone();
      loc.setYaw(dynamicYaw);
      loc.setPitch(0.0F);
      display.setInterpolationDelay(0);
      display.setInterpolationDuration(1);
      Vector3f rotAxis = new Vector3f(1.0F, 0.0F, 0.0F);
      Quaternionf q = new Quaternionf().rotationAxis(currentAngle, rotAxis.x, rotAxis.y, rotAxis.z);
      Transformation t = new Transformation(new Vector3f(-0.9F, -0.75F, -0.9F).rotate(q), q, new Vector3f(1.8F, 1.8F, 1.8F), new Quaternionf());
      display.setTransformation(t);
      display.teleport(loc);
   }

   public void playImpactBurst(Location center, Vector dir) {
      center.getWorld().playSound(center,AltarTuning.sound("earth_gauntlet","visuals.sounds.block-mud-break.sound",Sound.BLOCK_MUD_BREAK),AltarTuning.volume("earth_gauntlet","visuals.sounds.block-mud-break",1.65F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.block-mud-break",0.4F));
      center.getWorld().playSound(center,AltarTuning.sound("earth_gauntlet","visuals.sounds.block-mud-break.sound",Sound.BLOCK_MUD_BREAK),AltarTuning.volume("earth_gauntlet","visuals.sounds.block-mud-break",1.5F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.block-mud-break",0.01F));
      center.getWorld().playSound(center,AltarTuning.sound("earth_gauntlet","visuals.sounds.block-packed-mud-break.sound",Sound.BLOCK_PACKED_MUD_BREAK),AltarTuning.volume("earth_gauntlet","visuals.sounds.block-packed-mud-break",1.2F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.block-packed-mud-break",0.4F));
      Location baseLoc = center.clone().add(0.0, -0.2, 0.0);
      ParticleUtils.spawnBlockDispersion(this.plugin, baseLoc, this.mudBlockData, 4.8);
   }

   private void infectBlock(Block toInfect, LinkedList<BlockState> trail) {
      if (toInfect.getType().isSolid() && toInfect.getType() != Material.MUD && toInfect.getType() != Material.BEDROCK) {
         trail.add(toInfect.getState());
         toInfect.setType(Material.MUD);
         toInfect.getWorld().playSound(toInfect.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.block-mud-place.sound",Sound.BLOCK_MUD_PLACE),AltarTuning.volume("earth_gauntlet","visuals.sounds.block-mud-place",1.3F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.block-mud-place",0.4F));
         ParticleUtils.spawnBlockDispersion(this.plugin, toInfect.getLocation().add(0.5, 0.5, 0.5), this.mudBlockData, 1.2, 16);
      }
   }

   private void hitEntity(final Player p, final LivingEntity victim, LinkedList<BlockState> trail) {
      if (this.plugin.getWorldGuardManager() == null || this.plugin.getWorldGuardManager().canAffectTarget(p, victim)) {
         double mudDmg = this.plugin.getWeaponsConfig().getDouble("earth-gauntlet.mudslide.damage", 4.0);
         this.plugin.getCleanDamageManager().apply(victim, p, mudDmg);
         victim.playHurtAnimation(0.0F);
         p.getWorld().playSound(victim.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.block-sponge-absorb.sound",Sound.BLOCK_SPONGE_ABSORB),AltarTuning.volume("earth_gauntlet","visuals.sounds.block-sponge-absorb",1.0F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.block-sponge-absorb",0.3F));
         p.playSound(p.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.entity-arrow-hit-player.sound",Sound.ENTITY_ARROW_HIT_PLAYER), SoundCategory.MASTER,AltarTuning.volume("earth_gauntlet","visuals.sounds.entity-arrow-hit-player",1.0F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.entity-arrow-hit-player",0.7F));
         victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, AltarTuning.integer("earth_gauntlet","tuning.earth-gauntlet-manager.effects.slowness.duration-ticks",80,1,72000), AltarTuning.integer("earth_gauntlet","tuning.earth-gauntlet-manager.effects.slowness.amplifier",2,0,10), false, false, true));
         ParticleUtils.spawnBlockDispersion(this.plugin, victim.getLocation().add(0.0, 0.5, 0.0), this.mudBlockData, 9.0, 170);
         int debuffTime = this.plugin.getWeaponsConfig().getInt("earth-gauntlet.mudslide.debuff_time_seconds", 4);
         if (victim instanceof Player pVictim) {
            this.bossBarManager.setDebuffBar(pVictim, AltarTuning.text("earth_gauntlet","messages.earth-gauntlet-manager.message-5","§4§l!! §c§lзᴀляпᴀнный грязью §4§l!!","§4§l!! §c§lᴍᴜᴅᴅᴇᴅ §4§l!!"), debuffTime);
         }

         this.bossBarManager.setActiveBar(p, "PullPrompt", AltarTuning.text("earth_gauntlet","messages.earth-gauntlet-manager.message-6","§2§l!! §a§lприᴛянуᴛь цᴇль §2§l!!","§2§l!! §a§lᴩᴜʟʟ ᴛᴀʀɢᴇᴛ §2§l!!"), debuffTime);
         this.pullableTargets.put(p.getUniqueId(), victim.getUniqueId());
         this.activeMudTrails.put(p.getUniqueId(), trail);
         (new BukkitRunnable() {
               @Override
               public void run() {
                  if (EarthGauntletManager.this.pullableTargets.containsKey(p.getUniqueId())
                     && EarthGauntletManager.this.pullableTargets.get(p.getUniqueId()).equals(victim.getUniqueId())) {
                     EarthGauntletManager.this.pullableTargets.remove(p.getUniqueId());
                     EarthGauntletManager.this.bossBarManager.removeActiveBar(p, "PullPrompt");
                     if (EarthGauntletManager.this.activeMudTrails.containsKey(p.getUniqueId())) {
                        EarthGauntletManager.this.clearTrailGradually(EarthGauntletManager.this.activeMudTrails.remove(p.getUniqueId()));
                     }
                  }
               }
            })
            .runTaskLater(this.plugin, (long)debuffTime * 20L);
      }
   }

   public boolean hasMuddiedTarget(Player p) {
      return this.pullableTargets.containsKey(p.getUniqueId());
   }

   public void pullTarget(final Player p) {
      UUID victimId = this.pullableTargets.remove(p.getUniqueId());
      this.bossBarManager.removeActiveBar(p, "PullPrompt");
      if (this.activeMudTrails.containsKey(p.getUniqueId())) {
         this.clearTrailGradually(this.activeMudTrails.remove(p.getUniqueId()));
      }

      if (!(Bukkit.getEntity(victimId) instanceof LivingEntity victim) || victim.isDead()) {
         return;
      }

      if (this.plugin.getWorldGuardManager() == null || this.plugin.getWorldGuardManager().canAffectTarget(p, victim)) {
         if (victim instanceof Player pVictim) {
            this.bossBarManager.removeDebuffBar(pVictim);
         }

         victim.removePotionEffect(PotionEffectType.SLOWNESS);
         Location pLoc = p.getLocation();
         Location vLoc = victim.getLocation();
         Vector dirAway = vLoc.toVector().subtract(pLoc.toVector()).normalize();
         vLoc.setDirection(dirAway);
         victim.teleport(vLoc);
         (new BukkitRunnable() {
               int ticks = 0;
               final double pullSpeed = EarthGauntletManager.this.plugin.getWeaponsConfig().getDouble("earth-gauntlet.mudslide.pull_speed", 1.5);

               @Override
               public void run() {
                  if (p.isOnline() && victim.isValid() && !victim.isDead()) {
                     if (EarthGauntletManager.this.plugin.getWorldGuardManager() != null
                        && !EarthGauntletManager.this.plugin.getWorldGuardManager().canAffectTarget(p, victim)) {
                        this.cancel();
                     } else {
                        Location currentVLoc = victim.getLocation();
                        Location targetPLoc = p.getLocation().add(0.0, 1.0, 0.0);
                        if (currentVLoc.getBlock().getRelative(BlockFace.UP).getType().isSolid()) {
                           victim.setVelocity(new Vector(0.0, -1.0, 0.0));
                        }

                        Vector diff = targetPLoc.toVector().subtract(currentVLoc.toVector().add(new Vector(0, 1, 0)));
                        double beamDist = diff.length();
                        if (beamDist > 0.5) {
                           Vector directionStep = diff.normalize();

                           for (double d = 0.0; d < beamDist; d += 0.4) {
                              Location point = currentVLoc.clone().add(0.0, 1.0, 0.0).add(directionStep.clone().multiply(d));
                              point.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, point,AltarTuning.particles("earth_gauntlet",1), 0.0, 0.0, 0.0, 0.0, EarthGauntletManager.this.mudBlockData);
                           }
                        }

                        double hDistSquared = Math.pow(currentVLoc.getX() - targetPLoc.getX(), 2.0) + Math.pow(currentVLoc.getZ() - targetPLoc.getZ(), 2.0);
                        if (!(hDistSquared < 1.0) && !(currentVLoc.distanceSquared(targetPLoc) < 2.0) && this.ticks <= 60) {
                           if (this.ticks % 2 == 0) {
                              victim.getWorld().playSound(currentVLoc,AltarTuning.sound("earth_gauntlet","visuals.sounds.block-packed-mud-break.sound",Sound.BLOCK_PACKED_MUD_BREAK),AltarTuning.volume("earth_gauntlet","visuals.sounds.block-packed-mud-break",1.0F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.block-packed-mud-break",0.4F));
                           }

                           Vector pullVec = targetPLoc.toVector().subtract(currentVLoc.toVector());
                           double dist = pullVec.length();
                           if (dist > this.pullSpeed) {
                              pullVec.normalize().multiply(this.pullSpeed);
                           }

                           if (currentVLoc.getBlock().getRelative(BlockFace.DOWN).getType().isSolid() && pullVec.getY() < 0.3) {
                              pullVec.setY(0.3);
                           }

                           victim.setVelocity(pullVec);
                           this.ticks++;
                        } else {
                           victim.setVelocity(new Vector(0.0, -0.1, 0.0));
                           victim.setFallDistance(0.0F);
                           victim.getWorld().playSound(victim.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.entity-generic-explode.sound",Sound.ENTITY_GENERIC_EXPLODE),AltarTuning.volume("earth_gauntlet","visuals.sounds.entity-generic-explode",0.5F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.entity-generic-explode",1.2F));
                           this.cancel();
                        }
                     }
                  } else {
                     this.cancel();
                  }
               }
            })
            .runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void clearTrailGradually(final LinkedList<BlockState> trail) {
      (new BukkitRunnable() {
            @Override
            public void run() {
               if (trail.isEmpty()) {
                  this.cancel();
               } else {
                  BlockState state = trail.poll();
                  state.update(true, false);
                  state.getBlock().getWorld().playSound(state.getLocation(),AltarTuning.sound("earth_gauntlet","visuals.sounds.block-mud-break.sound",Sound.BLOCK_MUD_BREAK),AltarTuning.volume("earth_gauntlet","visuals.sounds.block-mud-break",1.0F),AltarTuning.pitch("earth_gauntlet","visuals.sounds.block-mud-break",0.4F));
                  ParticleUtils.spawnBlockDispersion(
                     EarthGauntletManager.this.plugin, state.getLocation().add(0.5, 0.5, 0.5), EarthGauntletManager.this.mudBlockData, 2.5
                  );
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 2L);
   }

   private void cleanupMeteor(Player p) {
      if (this.activeMeteorStrikes.containsKey(p.getUniqueId())) {
         this.activeMeteorStrikes.get(p.getUniqueId()).cancel();
         this.activeMeteorStrikes.remove(p.getUniqueId());
      }
   }

   public void resetCooldowns(Player player) {
      this.cleanupMeteor(player);
      this.bossBarManager.resetPlayer(player);
      this.pullableTargets.remove(player.getUniqueId());
      LinkedList<BlockState> trail = this.activeMudTrails.remove(player.getUniqueId());
      if (trail != null) {
         this.clearTrailGradually(trail);
      }
   }

   public void cleanupAll() {
      for (BukkitTask task : this.activeMeteorStrikes.values()) {
         if (task != null) {
            task.cancel();
         }
      }

      this.activeMeteorStrikes.clear();
      this.pullableTargets.clear();
      this.stunnedTargets.clear();

      for (LinkedList<BlockState> trail : this.activeMudTrails.values()) {
         if (trail != null) {
            while (!trail.isEmpty()) {
               BlockState s = trail.poll();
               if (s != null) {
                  s.update(true, false);
               }
            }
         }
      }

      this.activeMudTrails.clear();
      this.bossBarManager.clearAll();
   }
}

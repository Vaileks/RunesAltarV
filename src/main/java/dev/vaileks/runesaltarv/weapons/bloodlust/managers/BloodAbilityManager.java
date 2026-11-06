package dev.vaileks.runesaltarv.weapons.bloodlust.managers;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.ParticleUtils;
import dev.vaileks.runesaltarv.utils.SpeedBuffUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.weapons.bloodlust.listeners.BloodLustListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.Particle.DustOptions;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BloodAbilityManager {
   private final RunesAltarV plugin;
   private final BloodBossBarManager bossBarManager;
   private final Set<UUID> activePuddles = new HashSet<>();
   private final Map<UUID, List<PotionEffect>> savedBloodTrailEffects = new HashMap<>();
   private final Map<UUID, Long> bloodTrailStartTime = new HashMap<>();
   private final Random random = new Random();
   private final BlockData bloodBlockData = Bukkit.createBlockData(Material.REDSTONE_BLOCK);

   public BloodAbilityManager(RunesAltarV plugin, BloodBossBarManager bossBarManager) {
      this.plugin = plugin;
      this.bossBarManager = bossBarManager;
   }

   public void updateHandCheck(Player p) {
      if (this.isInBloodTrail(p)) {
         ItemStack item = p.getInventory().getItemInMainHand();
         if (!BloodLustListener.isBloodLust(item)) {
            this.cancelBloodTrail(p, true);
         }
      }
   }

   public void launchTrackerProjectile(final Player source, final Player target) {
      if (source != null && target != null) {
         final Location current = source.getLocation().add(0.0, 1.5, 0.0);
         final DustOptions redDust = new DustOptions(Color.RED, 1.2F);
         (new BukkitRunnable() {
            int life = 0;

            @Override
            public void run() {
               if (target.isOnline() && this.life <= 60 && source.isOnline() && target.getWorld().equals(current.getWorld())) {
                  Location targetLoc = target.getLocation().add(0.0, 1.0, 0.0);
                  Vector dir = targetLoc.toVector().subtract(current.toVector());
                  if (dir.length() < 0.6) {
                     source.spawnParticle(Particle.ENTITY_EFFECT, current, 1, 0.0, 0.0, 0.0, 0.0, Color.RED);
                     this.cancel();
                  } else {
                     dir.normalize().multiply(0.5);
                     current.add(dir);
                     source.spawnParticle(Particle.ELECTRIC_SPARK, current, 1, 0.0, 0.0, 0.0, 0.0);
                     source.spawnParticle(Particle.DUST, current, 1, 0.0, 0.0, 0.0, 0.0, redDust);
                     this.life++;
                  }
               } else {
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   public void applyInfection(final Player attacker, final Entity target) {
      if (target instanceof LivingEntity victim) {
         if (victim instanceof Player targetPlayer && this.plugin.getFriendManager().isFriend(attacker.getUniqueId(), targetPlayer.getUniqueId())) {
            return;
         }

         if (!(Math.random() > 0.15)) {
            if (!this.bossBarManager.isOnCooldown(attacker, "Infection")) {
               this.bossBarManager
                  .setCooldown(
                     attacker,
                     "Infection",
                     this.plugin.tr("§4§lИнфекция", "§4§lɪɴꜰᴇᴄᴛɪᴏɴ"),
                     this.plugin.getWeaponsConfig().getInt("bloodlust.infection.cooldown")
                  );
               final int maxHits = this.plugin.getWeaponsConfig().getInt("bloodlust.infection.hits", 6);
               final double bleedDamage = this.plugin.getWeaponsConfig().getDouble("bloodlust.infection.damage", 2.0);
               final String barId = "BloodInfection";
               if (victim instanceof Player pVictim) {
                  this.bossBarManager.setDebuffBar(pVictim, barId, this.plugin.tr("§4§l! §c§lᴋроʙоᴛᴇчᴇниᴇ §4§l!", "§4§l! §c§lʙʟᴇᴇᴅɪɴɢ §4§l!"), maxHits);
               }

               final DustOptions redDust = new DustOptions(Color.RED, 1.5F);
               (new BukkitRunnable() {
                  int currentHit = 0;

                  @Override
                  public void run() {
                     if (this.currentHit < maxHits && !target.isDead() && target.isValid()) {
                        CombatUtils.runSyntheticDamage(() -> victim.damage(bleedDamage, attacker));
                        victim.getWorld().playSound(victim.getLocation(), "bloodlust.hit", 1.0F, 1.0F);
                        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 0.5F);
                        if (victim.isDead()) {
                           BloodAbilityManager.this.playKillVisual(attacker);
                           if (victim instanceof Player pVictim) {
                              BloodAbilityManager.this.bossBarManager.removeDebuffBar(pVictim, barId);
                           }

                           this.cancel();
                        } else {
                           Location effectLoc = victim.getLocation().add(0.0, 1.0, 0.0);

                           for (int i = 0; i < 20; i++) {
                              double angle = (Math.PI / 10) * (double)i;
                              double yOff = 0.2 + (double)i / 20.0 * 1.6;
                              Location swirl = victim.getLocation().add(Math.cos(angle) * 0.65, yOff, Math.sin(angle) * 0.65);
                              victim.getWorld().spawnParticle(Particle.DUST, swirl, 2, 0.05, 0.05, 0.05, 0.0, redDust);
                           }

                           victim.getWorld().spawnParticle(Particle.DUST, effectLoc, 35, 0.35, 0.45, 0.35, 0.0, redDust);
                           BloodAbilityManager.this.launchBloodSteal(effectLoc, attacker);
                           this.currentHit++;
                           if (victim instanceof Player pVictim && !victim.isDead()) {
                              double progress = (double)(maxHits - this.currentHit) / (double)maxHits;
                              BloodAbilityManager.this.bossBarManager.updateProgress(pVictim, barId, progress);
                              if (this.currentHit >= maxHits) {
                                 BloodAbilityManager.this.bossBarManager.removeDebuffBar(pVictim, barId);
                              }
                           }
                        }
                     } else {
                        if (victim instanceof Player pVictimx) {
                           BloodAbilityManager.this.bossBarManager.removeDebuffBar(pVictimx, barId);
                        }

                        this.cancel();
                     }
                  }
               }).runTaskTimer(this.plugin, 0L, 20L);
            }
         }
      }
   }

   private void launchBloodSteal(final Location startLoc, final Player attacker) {
      (new BukkitRunnable() {
         Location current = startLoc.clone();
         int life = 0;

         @Override
         public void run() {
            if (attacker.isOnline() && !attacker.isDead() && this.life <= 60) {
               Location targetLoc = attacker.getLocation().add(0.0, 1.2, 0.0);
               Vector dir = targetLoc.toVector().subtract(this.current.toVector());
               if (dir.lengthSquared() < 1.0) {
                  attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0F, 1.2F);
                  attacker.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 2, false, false, true));
                  this.cancel();
               } else {
                  Location prev = this.current.clone();
                  dir.normalize().multiply(0.7);
                  this.current.add(dir);
                  Vector stepVec = this.current.toVector().subtract(prev.toVector());
                  double dist = stepVec.length();
                  int steps = Math.max(1, (int)Math.ceil(dist / 0.22));
                  Vector subStep = stepVec.clone().multiply(1.0 / (double)steps);

                  for (int s = 0; s <= steps; s++) {
                     Location pt = prev.clone().add(subStep.clone().multiply(s));
                     pt.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, pt, 1, 0.0, 0.0, 0.0, 0.0, BloodAbilityManager.this.bloodBlockData);
                  }

                  this.life++;
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   public void castBloodTrail(final Player p) {
      if (!this.bossBarManager.isOnCooldown(p, "BloodTrail")) {
         this.bossBarManager
            .setCooldown(
               p, "BloodTrail", this.plugin.tr("§4§lᴋроʙᴀʙый ᴄлᴇд", "§4§lʙʟᴏᴏᴅ ᴛʀᴀɪʟ"), this.plugin.getWeaponsConfig().getInt("bloodlust.blood-trail.cooldown")
            );
         final int maxTicks = this.plugin.getWeaponsConfig().getInt("bloodlust.blood-trail.duration", 20) * 20;
         this.activePuddles.add(p.getUniqueId());
         this.bloodTrailStartTime.put(p.getUniqueId(), System.currentTimeMillis());
         List<PotionEffect> toSave = new ArrayList<>();

         for (PotionEffect pe : p.getActivePotionEffects()) {
            {
               toSave.add(pe);
            }
         }

         this.savedBloodTrailEffects.put(p.getUniqueId(), toSave);
         p.getWorld().playSound(p.getLocation(), "bloodlust.dive", 1.0F, 1.0F);
         p.getAttribute(Attribute.SCALE).setBaseValue(0.3);
         p.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(1.0);
         WorldBorder border = Bukkit.createWorldBorder();
         border.setCenter(p.getLocation());
         border.setSize(10000.0);
         border.setWarningDistance(10000);
         p.setWorldBorder(border);

         for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(p)) {
               online.hidePlayer(this.plugin, p);
            }
         }

         (new BukkitRunnable() {
               int ticks = 0;

               @Override
               public void run() {
                  if (BloodAbilityManager.this.activePuddles.contains(p.getUniqueId()) && p.isOnline() && this.ticks < maxTicks) {
                     if (this.ticks % 20 == 0) {
                        TextUtils.sendActionBar(
                           p, TextUtils.legacy(BloodAbilityManager.this.plugin.tr("§cНажмите §e[ПРЫЖОК] §cдля выхода", "§cPress §e[JUMP] §cto exit"))
                        );
                        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 30, 0, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 3, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 30, 3, false, false));
                     }

                     Location loc = p.getLocation();

                     for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * 2.0 * Math.PI;
                        double radius = Math.random() * 0.8;
                        Location partLoc = loc.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
                        loc.getWorld().spawnParticle(Particle.BLOCK, partLoc, 1, 0.0, 0.0, 0.0, 0.0, BloodAbilityManager.this.bloodBlockData);
                     }

                     this.ticks++;
                  } else {
                     if (BloodAbilityManager.this.activePuddles.contains(p.getUniqueId())) {
                        BloodAbilityManager.this.cancelBloodTrail(p, true);
                     }

                     this.cancel();
                  }
               }
            })
            .runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   public void cancelBloodTrail(Player p, boolean forceJump) {
      if (this.activePuddles.contains(p.getUniqueId())) {
         this.activePuddles.remove(p.getUniqueId());
         p.getWorld().playSound(p.getLocation(), "bloodlust.resurface", 1.0F, 1.0F);
         p.getAttribute(Attribute.SCALE).setBaseValue(1.0);
         p.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(0.6);
         p.setWorldBorder(null);

         for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(this.plugin, p);
         }

         p.removePotionEffect(PotionEffectType.INVISIBILITY);
         p.removePotionEffect(PotionEffectType.MINING_FATIGUE);
         PotionEffect speed = p.getPotionEffect(PotionEffectType.SPEED);
         if (speed != null && speed.getAmplifier() == 3 && speed.getDuration() <= 40) {
            p.removePotionEffect(PotionEffectType.SPEED);
         }

         Long startTime = this.bloodTrailStartTime.remove(p.getUniqueId());
         List<PotionEffect> saved = this.savedBloodTrailEffects.remove(p.getUniqueId());
         if (saved != null && startTime != null) {
            long elapsedTicks = (System.currentTimeMillis() - startTime) / 50L;

            for (PotionEffect old : saved) {
               if (old.getDuration() == -1) {
                  p.addPotionEffect(old);
               } else {
                  int rem = old.getDuration() - (int)elapsedTicks;
                  if (rem > 0) {
                     p.addPotionEffect(new PotionEffect(old.getType(), rem, old.getAmplifier(), old.isAmbient(), old.hasParticles(), old.hasIcon()));
                  }
               }
            }
         }

         if (forceJump) {
            p.setVelocity(p.getVelocity().setY(0.55));
            p.getWorld().spawnParticle(Particle.BLOCK, p.getLocation().add(0.0, 0.2, 0.0), 20, 0.3, 0.1, 0.3, 0.0, this.bloodBlockData);
         }
      }
   }

   public boolean isInBloodTrail(Player p) {
      return this.activePuddles.contains(p.getUniqueId());
   }

   public void playKillVisual(Player attacker) {
      this.playKillVisual(attacker, null);
   }

   public void playKillVisual(final Player attacker, Location victimLoc) {
      if (attacker != null && attacker.isOnline()) {
         attacker.getWorld().playSound(attacker.getLocation(), "bloodlust.kill", 1.0F, 1.0F);
         Location origin = victimLoc != null
            ? victimLoc.clone().add(0.0, 1.0, 0.0)
            : attacker.getLocation().add(attacker.getLocation().getDirection().multiply(2.5)).add(0.0, 1.0, 0.0);
         final DustOptions crimsonDust = new DustOptions(Color.fromRGB(220, 20, 20), 1.5F);
         final DustOptions tailDust = new DustOptions(Color.fromRGB(160, 10, 10), 0.9F);
         int numComets = 6;
         final List<Location> cometLocs = new ArrayList<>();
         final List<Double> phases = new ArrayList<>();
         final List<Double> spiralSpeeds = new ArrayList<>();

         for (int i = 0; i < numComets; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 0.8;
            double oy = (this.random.nextDouble() - 0.2) * 0.8;
            double oz = (this.random.nextDouble() - 0.5) * 0.8;
            cometLocs.add(origin.clone().add(ox, oy, oz));
            phases.add(this.random.nextDouble() * Math.PI * 2.0);
            spiralSpeeds.add(0.35 + this.random.nextDouble() * 0.3);
         }

         (new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
               if (attacker.isOnline() && this.ticks <= 60) {
                  Location target = attacker.getLocation().add(0.0, 1.2, 0.0);
                  boolean allReached = true;

                  for (int i = 0; i < cometLocs.size(); i++) {
                     Location comet = cometLocs.get(i);
                     Vector toTarget = target.toVector().subtract(comet.toVector());
                     double distSq = toTarget.lengthSquared();
                     if (distSq > 0.8) {
                        allReached = false;
                        double speed = Math.min(0.85, 0.35 + (double)this.ticks * 0.035);
                        Vector step = toTarget.normalize().multiply(speed);
                        double phase = phases.get(i) + (double)this.ticks * spiralSpeeds.get(i);
                        Vector right = step.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                        if (right.lengthSquared() < 0.001) {
                           right = new Vector(1, 0, 0);
                        }

                        Vector up = right.clone().crossProduct(step).normalize();
                        double swirlR = Math.max(0.08, 0.45 * Math.sin(Math.min(1.0, (double)this.ticks / 20.0) * Math.PI));
                        Vector swirlOffset = right.multiply(Math.cos(phase) * swirlR).add(up.multiply(Math.sin(phase) * swirlR));
                        comet.add(step).add(swirlOffset.multiply(0.35));
                        comet.getWorld().spawnParticle(Particle.DUST, comet, 3, 0.05, 0.05, 0.05, 0.0, crimsonDust);
                        comet.getWorld().spawnParticle(Particle.BLOCK, comet, 1, 0.02, 0.02, 0.02, 0.0, BloodAbilityManager.this.bloodBlockData);
                        Location tailLoc = comet.clone().subtract(step.clone().multiply(0.4));
                        comet.getWorld().spawnParticle(Particle.DUST, tailLoc, 2, 0.04, 0.04, 0.04, 0.0, tailDust);
                     }
                  }

                  if (allReached) {
                     attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8F, 1.2F);
                     attacker.getWorld().spawnParticle(Particle.DUST, target, 35, 0.4, 0.4, 0.4, 0.0, crimsonDust);
                     this.cancel();
                  }

                  this.ticks++;
               } else {
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   public void castBloodHook(final Player p) {
      if (!this.bossBarManager.isOnCooldown(p, "BloodHook")) {
         this.bossBarManager
            .setCooldown(
               p, "BloodHook", this.plugin.tr("§4§lᴋроʙᴀʙый ᴋрюᴋ", "§4§lʙʟᴏᴏᴅ ʜᴏᴏᴋ"), this.plugin.getWeaponsConfig().getInt("bloodlust.blood-hook.cooldown")
            );
         p.swingMainHand();
         p.getWorld().playSound(p.getLocation(), "bloodlust.resurface", 1.0F, 1.5F);
         final Location startLoc = p.getLocation().add(0.0, 1.0, 0.0);
         final Vector direction = p.getEyeLocation().getDirection().normalize();
         startLoc.setDirection(direction);
         final BlockDisplay display = p.getWorld().spawn(startLoc, BlockDisplay.class, ent -> {
            ent.setBlock(this.bloodBlockData);
            ent.setInterpolationDuration(1);
            ent.setTeleportDuration(1);
         });
         this.plugin.getVisualCleanupManager().track(display);
         (new BukkitRunnable() {
               double distanceTraveled = 0.0;
               float currentAngle = 0.0F;
               final double maxDist = BloodAbilityManager.this.plugin.getWeaponsConfig().getDouble("bloodlust.blood-hook.range");
               final double speed = BloodAbilityManager.this.plugin.getWeaponsConfig().getDouble("bloodlust.blood-hook.speed", 1.3);
               final float flightYaw = startLoc.getYaw();

               @Override
               public void run() {
                  if (p.isOnline() && display.isValid()) {
                     Location nextLoc = display.getLocation().add(direction.clone().multiply(this.speed));
                     nextLoc.setYaw(this.flightYaw);
                     nextLoc.setPitch(0.0F);
                     if (!(this.distanceTraveled >= this.maxDist) && !nextLoc.getBlock().getType().isSolid()) {
                        for (Entity e : nextLoc.getWorld().getNearbyEntities(nextLoc, 1.2, 1.2, 1.2)) {
                           if (e instanceof LivingEntity victim
                              && !victim.equals(p)
                              && (
                                 !(victim instanceof Player targetPlayer)
                                    || targetPlayer.getGameMode() != GameMode.SPECTATOR
                                       && !BloodAbilityManager.this.plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())
                              )) {
                              p.getWorld().playSound(nextLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 0.5F);
                              BloodAbilityManager.this.plugin.getVisualCleanupManager().untrack(display);
                              display.remove();
                              BloodAbilityManager.this.playImpactBurst(victim.getLocation().add(0.0, 1.0, 0.0));
                              double damage = BloodAbilityManager.this.plugin.getWeaponsConfig().getDouble("bloodlust.blood-hook.damage", 8.0);
                              BloodAbilityManager.this.plugin.getCleanDamageManager().apply(victim, p, damage);
                              BloodAbilityManager.this.returnHook(p, nextLoc, victim);
                              this.cancel();
                              return;
                           }
                        }

                        display.setInterpolationDelay(0);
                        display.setInterpolationDuration(2);
                        Vector3f rotAxis = new Vector3f(1.0F, 0.0F, 0.0F);
                        Quaternionf q = new Quaternionf().rotationAxis(this.currentAngle, rotAxis.x, rotAxis.y, rotAxis.z);
                        Transformation t = new Transformation(new Vector3f(-0.5F, -0.5F, -0.5F).rotate(q), q, new Vector3f(1.0F, 1.0F, 1.0F), new Quaternionf());
                        display.setTransformation(t);
                        display.teleport(nextLoc);
                        Location tailStart = p.getLocation().add(0.0, 1.0, 0.0);
                        BloodAbilityManager.this.renderChain(tailStart, display.getLocation());
                        this.distanceTraveled = this.distanceTraveled + this.speed;
                        this.currentAngle += 0.5F;
                     } else {
                        p.getWorld().playSound(nextLoc, Sound.BLOCK_NETHER_GOLD_ORE_BREAK, 1.0F, 0.5F);
                        p.getWorld().playSound(nextLoc, Sound.BLOCK_CHAIN_BREAK, 1.0F, 1.2F);
                        BloodAbilityManager.this.playImpactBurst(nextLoc);
                        BloodAbilityManager.this.plugin.getVisualCleanupManager().untrack(display);
                        display.remove();
                        this.cancel();
                     }
                  } else {
                     if (display.isValid()) {
                        BloodAbilityManager.this.plugin.getVisualCleanupManager().untrack(display);
                        display.remove();
                     }

                     this.cancel();
                  }
               }
            })
            .runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void returnHook(final Player p, final Location hitLoc, final LivingEntity caught) {
      (new BukkitRunnable() {
         Location currentLoc = hitLoc.clone();
         LivingEntity target = caught;

         @Override
         public void run() {
            if (p.isOnline() && this.target != null && !this.target.isDead()) {
               Location targetLoc = p.getLocation().add(0.0, 1.0, 0.0);
               Vector toPlayer = targetLoc.toVector().subtract(this.currentLoc.toVector());
               if (toPlayer.length() < 1.5) {
                  this.cancel();
               } else {
                  Vector dir = toPlayer.normalize();
                  this.currentLoc.add(dir.clone().multiply(0.8));
                  BloodAbilityManager.this.renderChain(p.getLocation().add(0.0, 1.0, 0.0), this.currentLoc);

                  for (int i = 0; i < 3; i++) {
                     p.getWorld().playSound(p.getLocation(), "bloodlust.resurface", 1.0F, 1.2F + BloodAbilityManager.this.random.nextFloat() * 0.5F);
                  }

                  if (this.target.getLocation().getBlock().getType() == Material.COBWEB) {
                     this.target = null;
                  } else {
                     this.target.teleport(this.currentLoc.clone().subtract(0.0, 0.5, 0.0));
                     this.target.setFallDistance(0.0F);
                  }
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void renderChain(Location start, Location end) {
      Vector bridge = end.toVector().subtract(start.toVector());
      double dist = bridge.length();
      Vector step = bridge.normalize().multiply(0.35);
      Location pLoc = start.clone();

      for (double i = 0.0; i < dist; i += 0.35) {
         pLoc.add(step);
         pLoc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, pLoc, 1, 0.0, 0.0, 0.0, 0.0, this.bloodBlockData);
      }
   }

   public void playImpactBurst(Location center) {
      Location baseLoc = center.clone().add(0.0, 0.2, 0.0);
      DustOptions crimson = new DustOptions(Color.fromRGB(220, 20, 20), 1.5F);
      center.getWorld().spawnParticle(Particle.DUST, baseLoc, 25, 0.4, 0.4, 0.4, 0.05, crimson);
      ParticleUtils.spawnBlockDispersion(this.plugin, baseLoc, this.bloodBlockData, 4.8);
   }

   public void cleanup() {
      for (UUID uuid : new ArrayList<>(this.activePuddles)) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            this.cancelBloodTrail(p, false);
         }
      }

      this.activePuddles.clear();
      this.savedBloodTrailEffects.clear();
      this.bloodTrailStartTime.clear();
   }
}

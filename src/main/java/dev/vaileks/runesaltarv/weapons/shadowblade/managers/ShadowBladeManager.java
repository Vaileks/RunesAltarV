package dev.vaileks.runesaltarv.weapons.shadowblade.managers;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.TextUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.Particle.DustOptions;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

public class ShadowBladeManager {
   private final RunesAltarV plugin;
   private final Map<UUID, BossBar> leapCooldownBars = new HashMap<>();
   private final Map<UUID, Long> leapCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("shadow_blade","leap");
   private final Map<UUID, BossBar> daggersCooldownBars = new HashMap<>();
   private final Map<UUID, Long> daggersCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("shadow_blade","daggers");
   private final Map<UUID, Long> backstabCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("shadow_blade","backstab");
   public final Set<UUID> activeLeaps = new HashSet<>();
   public final Map<UUID, Location> currentLeapLocs = new HashMap<>();
   public final Map<UUID, Long> leapStartTimes = new HashMap<>();

   public ShadowBladeManager(RunesAltarV plugin) {
      this.plugin = plugin;
   }

   public boolean isOnLeapCooldown(Player p) {
      return this.leapCooldowns.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
   }

   public boolean isOnDaggersCooldown(Player p) {
      return this.daggersCooldowns.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
   }

   public long getLeapStartTime(Player p) {
      return this.leapStartTimes.getOrDefault(p.getUniqueId(), 0L);
   }

   public void castShadowLeap(final Player p) {
      if (!this.isOnLeapCooldown(p) && !this.activeLeaps.contains(p.getUniqueId())) {
         p.getWorld().playSound(p.getLocation(),AltarTuning.sound("shadow_blade","visuals.sounds.entity-enderman-teleport.sound",Sound.ENTITY_ENDERMAN_TELEPORT),AltarTuning.volume("shadow_blade","visuals.sounds.entity-enderman-teleport",1.0F),AltarTuning.pitch("shadow_blade","visuals.sounds.entity-enderman-teleport",0.8F));
         this.spawnBlackDust(p.getLocation());
         WorldBorder border = Bukkit.createWorldBorder();
         border.setCenter(p.getLocation());
         border.setSize(10000.0);
         border.setWarningDistance(10000);
         p.setWorldBorder(border);
         p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, AltarTuning.integer("shadow_blade","tuning.shadow-blade-manager.effects.invisibility.duration-ticks",40,1,72000), AltarTuning.integer("shadow_blade","tuning.shadow-blade-manager.effects.invisibility.amplifier",0,0,10), false, false, false));

         for (Player online : Bukkit.getOnlinePlayers()) {
            online.hidePlayer(this.plugin, p);
         }

         this.activeLeaps.add(p.getUniqueId());
         this.leapStartTimes.put(p.getUniqueId(), System.currentTimeMillis());
         final BossBar activeBar = TextUtils.bossBar(
            AltarTuning.text("shadow_blade","messages.shadow-blade-manager.message-1","§7§l! §f§lнᴇʙидимоᴄᴛь §7§l!","§7§l! §f§lɪɴᴠɪsɪʙɪʟɪᴛʏ §7§l!"), BarColor.WHITE, BarStyle.SOLID
         );
         activeBar.addPlayer(p);
         (new BukkitRunnable() {
               int ticks = 0;
               final int maxTicks = AltarTuning.integer("shadow_blade","tuning.shadow-blade-manager.max-ticks",30,1,512);
               Location actualTargetLoc = p.getEyeLocation().clone();

               @Override
               public void run() {
                  if (p.isOnline() && !p.isDead() && this.ticks < 30 && ShadowBladeManager.this.activeLeaps.contains(p.getUniqueId())) {
                     double progress = (double)(30 - this.ticks) / 30.0;
                     activeBar.setProgress(Math.max(0.0, progress));
                     double trailSpeed = ShadowBladeManager.this.plugin.getWeaponsConfig().getDouble("shadow-blade.leap.trail-speed", 0.6);
                     double distTraveled = (double)this.ticks * trailSpeed;
                     Vector dir = p.getLocation().getDirection().normalize();
                     RayTraceResult rt = p.getWorld().rayTraceBlocks(p.getEyeLocation(), dir, distTraveled, FluidCollisionMode.NEVER, true);
                     if (rt != null && rt.getHitBlock() != null) {
                        this.actualTargetLoc = rt.getHitPosition().toLocation(p.getWorld());
                        this.actualTargetLoc.subtract(dir.clone().multiply(0.3));
                     } else {
                        this.actualTargetLoc = p.getEyeLocation().add(dir.clone().multiply(distTraveled));
                     }

                     ShadowBladeManager.this.currentLeapLocs.put(p.getUniqueId(), this.actualTargetLoc);
                     World world = this.actualTargetLoc.getWorld();
                     if (world != null) {
                        DustOptions blackDustHead = new DustOptions(Color.fromRGB(0, 0, 0), 2.2F);
                        DustOptions blackDustTail = new DustOptions(Color.fromRGB(15, 15, 15), 0.9F);
                        int headParticleCount = ShadowBladeManager.this.plugin.getWeaponsConfig().getInt("shadow-blade.leap.head-particle-count", 8);

                        for (int i = 0; i < headParticleCount; i++) {
                           double rx = (Math.random() - 0.5) * 0.35;
                           double ry = (Math.random() - 0.5) * 0.35;
                           double rz = (Math.random() - 0.5) * 0.35;
                           Location headLoc = this.actualTargetLoc.clone().add(rx, ry, rz);
                           world.spawnParticle(Particle.DUST, headLoc,AltarTuning.particles("shadow_blade",1), 0.0, 0.0, 0.0, 0.0, blackDustHead);
                           Vector sparkVel = dir.clone()
                              .multiply(-0.2)
                              .add(new Vector((Math.random() - 0.5) * 0.03, (Math.random() - 0.5) * 0.03, (Math.random() - 0.5) * 0.03));
                           world.spawnParticle(Particle.ELECTRIC_SPARK, headLoc,AltarTuning.particles("shadow_blade",0), sparkVel.getX(), sparkVel.getY(), sparkVel.getZ(), 0.12);
                        }

                        double tailLength = ShadowBladeManager.this.plugin.getWeaponsConfig().getDouble("shadow-blade.leap.tail-length", 2.5);

                        for (double d = AltarTuning.number("shadow_blade","tuning.shadow-blade-manager.d",0.05,0.01,128); d <= tailLength; d += 0.04) {
                           double factor = 1.0 - d / tailLength;
                           double spread = 0.18 * Math.pow(factor, 1.5);
                           Location tailLoc = this.actualTargetLoc.clone().subtract(dir.clone().multiply(d));

                           for (int k = 0; k < 2; k++) {
                              double ox = (Math.random() - 0.5) * spread;
                              double oy = (Math.random() - 0.5) * spread;
                              double oz = (Math.random() - 0.5) * spread;
                              Location pLoc = tailLoc.clone().add(ox, oy, oz);
                              Vector dustVel = dir.clone().multiply(-0.25 * factor);
                              world.spawnParticle(Particle.DUST, pLoc,AltarTuning.particles("shadow_blade",0), dustVel.getX(), dustVel.getY(), dustVel.getZ(), 1.0, blackDustTail);
                           }

                           if (Math.random() < 0.35) {
                              world.spawnParticle(Particle.ELECTRIC_SPARK, tailLoc,AltarTuning.particles("shadow_blade",1), 0.02, 0.02, 0.02, 0.01);
                           }
                        }
                     }

                     this.ticks++;
                  } else {
                     activeBar.removeAll();
                     p.setWorldBorder(null);
                     p.removePotionEffect(PotionEffectType.INVISIBILITY);

                     for (Player online : Bukkit.getOnlinePlayers()) {
                        online.showPlayer(ShadowBladeManager.this.plugin, p);
                     }

                     Location tpLoc = this.actualTargetLoc.clone();
                     tpLoc.setYaw(p.getLocation().getYaw());
                     tpLoc.setPitch(p.getLocation().getPitch());
                     p.teleport(tpLoc);
                     ShadowBladeManager.this.spawnBlackDust(p.getLocation());
                     p.getWorld().playSound(p.getLocation(),AltarTuning.sound("shadow_blade","visuals.sounds.item-trident-throw.sound",Sound.ITEM_TRIDENT_THROW),AltarTuning.volume("shadow_blade","visuals.sounds.item-trident-throw",1.0F),AltarTuning.pitch("shadow_blade","visuals.sounds.item-trident-throw",1.3F));
                     p.swingMainHand();
                     double endImpulse = ShadowBladeManager.this.plugin.getWeaponsConfig().getDouble("shadow-blade.leap.end-impulse", 0.85);
                     Vector dashImpulse = p.getLocation().getDirection().normalize().multiply(endImpulse).setY(0.2);
                     p.setVelocity(dashImpulse);
                     ShadowBladeManager.this.activeLeaps.remove(p.getUniqueId());
                     ShadowBladeManager.this.currentLeapLocs.remove(p.getUniqueId());
                     int cooldown = ShadowBladeManager.this.plugin.getWeaponsConfig().getInt("shadow-blade.leap.cooldown", 30);
                     ShadowBladeManager.this.leapCooldowns.put(p.getUniqueId(), System.currentTimeMillis() + (long)cooldown * 1000L);
                     ShadowBladeManager.this.startCooldownBar(
                        p,
                        cooldown,
                        AltarTuning.text("shadow_blade","messages.shadow-blade-manager.message-2","§f§lᴛᴇнᴇʙой прыжоᴋ","§f§lsʜᴀᴅᴏᴡ ʟᴇᴀᴘ"),
                        ShadowBladeManager.this.leapCooldownBars,
                        ShadowBladeManager.this.leapCooldowns
                     );
                     (new BukkitRunnable() {
                        @Override
                        public void run() {
                           if (p.isOnline()) {
                              p.getWorld().playSound(p.getLocation(),AltarTuning.sound("shadow_blade","visuals.sounds.entity-enderman-teleport.sound",Sound.ENTITY_ENDERMAN_TELEPORT),AltarTuning.volume("shadow_blade","visuals.sounds.entity-enderman-teleport",1.0F),AltarTuning.pitch("shadow_blade","visuals.sounds.entity-enderman-teleport",0.8F));
                           }
                        }
                     }).runTaskLater(ShadowBladeManager.this.plugin, 8L);
                     this.cancel();
                  }
               }
            })
            .runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   public void castShadowDaggers(final Player p, ItemStack bladeItem) {
      if (!this.isOnDaggersCooldown(p)) {
         int cooldown = this.plugin.getWeaponsConfig().getInt("shadow-blade.daggers.cooldown", 45);
         this.daggersCooldowns.put(p.getUniqueId(), System.currentTimeMillis() + (long)cooldown * 1000L);
         this.startCooldownBar(p, cooldown, AltarTuning.text("shadow_blade","messages.shadow-blade-manager.message-3","§f§lᴛᴇнᴇʙыᴇ ᴋинжᴀлы","§f§lsʜᴀᴅᴏᴡ ᴅᴀɢɢᴇʀs"), this.daggersCooldownBars, this.daggersCooldowns);

         for (long delay = 0L; delay <= 4L; delay += 2L) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               if (p.isOnline()) {
                  p.getWorld().playSound(p.getLocation(),AltarTuning.sound("shadow_blade","visuals.sounds.item-trident-throw.sound",Sound.ITEM_TRIDENT_THROW),AltarTuning.volume("shadow_blade","visuals.sounds.item-trident-throw",1.0F),AltarTuning.pitch("shadow_blade","visuals.sounds.item-trident-throw",1.2F));
               }
            }, delay);
         }

         p.swingMainHand();
         Vector dir = p.getLocation().getDirection().normalize();
         this.playCrossSweep(p.getEyeLocation().add(dir.clone().multiply(1.2)), dir);
         Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
         Location center = p.getEyeLocation();
         Vector[] dirs = new Vector[]{
            dir.clone(), dir.clone().add(right.clone().multiply(-0.25)).normalize(), dir.clone().add(right.clone().multiply(0.25)).normalize()
         };
         final boolean[] soundPlayed = new boolean[]{false};
         int interpTicks = this.plugin.getWeaponsConfig().getInt("shadow-blade.daggers.interpolation-ticks", 3);

         for (final Vector d : dirs) {
            final Location forwardCenter = center.clone().add(d.clone().multiply(1.2));
            final ItemDisplay dagger = p.getWorld().spawn(forwardCenter, ItemDisplay.class, ent -> {
               ent.setItemStack(bladeItem);
               ent.setInterpolationDuration(interpTicks);
               ent.setTeleportDuration(interpTicks);
               Transformation t = ent.getTransformation();
               t.getLeftRotation().set(new Quaternionf().rotateX((float)Math.toRadians(90.0)));
               t.getScale().set(0.6F, 0.6F, 0.6F);
               ent.setTransformation(t);
            });
            this.plugin.getVisualCleanupManager().track(dagger);
            (new BukkitRunnable() {
                  final Location curr = forwardCenter.clone();
                  double daggerSpeed = ShadowBladeManager.this.plugin.getWeaponsConfig().getDouble("shadow-blade.daggers.speed", 1.0);
                  Vector currentDir = d.clone().multiply(this.daggerSpeed * 1.25);
                  int ticks = 0;
                  float roll = 0.0F;

                  @Override
                  public void run() {
                     if (this.ticks <= 50 && dagger.isValid()) {
                        if (this.ticks >= 6) {
                           double grav = ShadowBladeManager.this.plugin.getWeaponsConfig().getDouble("shadow-blade.daggers.gravity", 0.035);
                           this.currentDir.add(new Vector(0.0, -grav, 0.0));
                        }

                        this.curr.add(this.currentDir);
                        dagger.teleport(this.curr);
                        float spinSpeed = (float)ShadowBladeManager.this.plugin.getWeaponsConfig().getDouble("shadow-blade.daggers.spin-speed", 0.1);
                        this.roll -= spinSpeed * 4.5F;
                        Transformation t = dagger.getTransformation();
                        t.getLeftRotation().set(new Quaternionf().rotateX((float) (Math.PI / 2)).rotateY(this.roll));
                        dagger.setTransformation(t);
                        Vector flyVec = this.currentDir.clone().normalize();
                        double backOffset = ShadowBladeManager.this.plugin.getWeaponsConfig().getDouble("shadow-blade.daggers.trail-offset-back", 0.85);
                        Location handleLoc = this.curr.clone().subtract(flyVec.clone().multiply(backOffset));
                        Vector backStep = flyVec.clone().multiply(-0.18);
                        int sparkCount = ShadowBladeManager.this.plugin.getWeaponsConfig().getInt("shadow-blade.daggers.spark-count", 8);

                        for (int i = 0; i < sparkCount; i++) {
                           Location sparkLoc = handleLoc.clone().add(backStep.clone().multiply(i));
                           sparkLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc,AltarTuning.particles("shadow_blade",0), flyVec.getX(), flyVec.getY(), flyVec.getZ(), 0.08);
                        }

                        if (this.curr.getBlock().getType().isSolid()) {
                           DustOptions hitBlockDust = new DustOptions(Color.fromRGB(20, 20, 20), 0.8F);
                           this.curr.getWorld().spawnParticle(Particle.DUST, this.curr,AltarTuning.particles("shadow_blade",15), 0.1, 0.1, 0.1, 0.0, hitBlockDust);
                           this.curr.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, this.curr,AltarTuning.particles("shadow_blade",6), 0.05, 0.05, 0.05, 0.05);
                           dagger.remove();
                           this.cancel();
                        } else {
                           for (Entity e : this.curr.getWorld().getNearbyEntities(this.curr,AltarTuning.number("shadow_blade","tuning.shadow-blade-manager.hit-radius-x",0.8,0,128),AltarTuning.number("shadow_blade","tuning.shadow-blade-manager.hit-radius-y",0.8,0,128),AltarTuning.number("shadow_blade","tuning.shadow-blade-manager.hit-radius-z",0.8,0,128))) {
                              if (e instanceof LivingEntity victim
                                 && !victim.equals(p)
                                 && (
                                    !(victim instanceof Player targetPlayer)
                                       || !ShadowBladeManager.this.plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())
                                 )) {
                                 double dmg = ShadowBladeManager.this.plugin.getWeaponsConfig().getDouble("shadow-blade.daggers.damage", 2.0);
                                 ShadowBladeManager.this.plugin.getCleanDamageManager().apply(victim, p, dmg);
                                 victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, AltarTuning.integer("shadow_blade","tuning.shadow-blade-manager.effects.blindness.duration-ticks",60,1,72000), AltarTuning.integer("shadow_blade","tuning.shadow-blade-manager.effects.blindness.amplifier",0,0,10), false, false));
                                 victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, AltarTuning.integer("shadow_blade","tuning.shadow-blade-manager.effects.slowness.duration-ticks",60,1,72000), AltarTuning.integer("shadow_blade","tuning.shadow-blade-manager.effects.slowness.amplifier",1,0,10), false, false));
                                 Location hitPoint = this.curr.clone();
                                 DustOptions hitDust = new DustOptions(Color.fromRGB(0, 0, 0), 1.0F);
                                 hitPoint.getWorld().spawnParticle(Particle.DUST, hitPoint,AltarTuning.particles("shadow_blade",35), 0.2, 0.2, 0.2, 0.0, hitDust);
                                 hitPoint.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitPoint,AltarTuning.particles("shadow_blade",10), 0.1, 0.1, 0.1, 0.05);
                                 if (!soundPlayed[0]) {
                                    p.getWorld().playSound(victim.getLocation(),AltarTuning.sound("shadow_blade","visuals.sounds.block-sponge-absorb.sound",Sound.BLOCK_SPONGE_ABSORB),AltarTuning.volume("shadow_blade","visuals.sounds.block-sponge-absorb",1.0F),AltarTuning.pitch("shadow_blade","visuals.sounds.block-sponge-absorb",0.8F));
                                    p.getWorld().playSound(victim.getLocation(),AltarTuning.sound("shadow_blade","visuals.sounds.entity-experience-orb-pickup.sound",Sound.ENTITY_EXPERIENCE_ORB_PICKUP),AltarTuning.volume("shadow_blade","visuals.sounds.entity-experience-orb-pickup",3.0F),AltarTuning.pitch("shadow_blade","visuals.sounds.entity-experience-orb-pickup",0.7F));
                                    soundPlayed[0] = true;
                                 }

                                 dagger.remove();
                                 this.cancel();
                                 return;
                              }
                           }

                           this.ticks++;
                        }
                     } else {
                        dagger.remove();
                        this.cancel();
                     }
                  }
               })
               .runTaskTimer(this.plugin, 0L, 1L);
         }
      }
   }

   public void triggerBackstab(final Player attacker, LivingEntity mainVictim) {
      if (this.backstabCooldowns.getOrDefault(attacker.getUniqueId(), 0L) <= System.currentTimeMillis()) {
         if (mainVictim instanceof Player targetPlayer && this.plugin.getFriendManager().isFriend(attacker.getUniqueId(), targetPlayer.getUniqueId())) {
            return;
         }

         double chance = this.plugin.getWeaponsConfig().getDouble("shadow-blade.passive.backstab-chance", 0.25);
         if (!(Math.random() > chance)) {
            long backstabCd = this.plugin.getWeaponsConfig().getLong("shadow-blade.passive.backstab-cooldown", 8L);
            this.backstabCooldowns.put(attacker.getUniqueId(), System.currentTimeMillis() + backstabCd * 1000L);
            attacker.getWorld().playSound(mainVictim.getLocation(),AltarTuning.sound("shadow_blade","visuals.sounds.entity-enderman-teleport.sound",Sound.ENTITY_ENDERMAN_TELEPORT),AltarTuning.volume("shadow_blade","visuals.sounds.entity-enderman-teleport",1.0F),AltarTuning.pitch("shadow_blade","visuals.sounds.entity-enderman-teleport",0.5F));
            final List<LivingEntity> targets = new ArrayList<>();
            targets.add(mainVictim);

            for (Entity e : mainVictim.getWorld().getNearbyEntities(mainVictim.getLocation(),AltarTuning.number("shadow_blade","tuning.shadow-blade-manager.hit-radius-x-2",5.0,0,128),AltarTuning.number("shadow_blade","tuning.shadow-blade-manager.hit-radius-y-2",5.0,0,128),AltarTuning.number("shadow_blade","tuning.shadow-blade-manager.hit-radius-z-2",5.0,0,128))) {
               if (e instanceof LivingEntity) {
                  LivingEntity le = (LivingEntity)e;
                  if (!le.equals(attacker) && !le.equals(mainVictim)) {
                     if (le instanceof Player) {
                        Player p = (Player)le;
                        if (p.getGameMode() == GameMode.SPECTATOR || this.plugin.getFriendManager().isFriend(attacker.getUniqueId(), p.getUniqueId())) {
                           continue;
                        }
                     }

                     targets.add(le);
                  }
               }
            }

            (new BukkitRunnable() {
               int ticks = 0;

               @Override
               public void run() {
                  if (attacker.isOnline() && !attacker.isDead() && this.ticks <= 10) {
                     Location aLoc = attacker.getLocation().add(0.0, 0.5, 0.0);
                     DustOptions black = new DustOptions(Color.BLACK, 1.2F);

                     for (LivingEntity victim : targets) {
                        if (!victim.isDead() && victim.isValid()) {
                           if (this.ticks == 0) {
                              Vector pullVec = attacker.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize().multiply(0.55).setY(0.2);
                              victim.setVelocity(pullVec);
                           }

                           Location vLoc = victim.getLocation().add(0.0, 0.5, 0.0);
                           Vector bridge = aLoc.toVector().subtract(vLoc.toVector());
                           double dist = bridge.length();
                           if (dist > 0.5) {
                              Vector step = bridge.normalize().multiply(0.3);

                              for (int j = 0; j < 3; j++) {
                                 Location pLoc = vLoc.clone().add(0.0, (double)j * 0.5, 0.0);

                                 for (double i = 0.0; i < dist; i += 0.3) {
                                    pLoc.add(step);
                                    pLoc.getWorld().spawnParticle(Particle.DUST, pLoc,AltarTuning.particles("shadow_blade",1), 0.0, 0.0, 0.0, 0.0, black);
                                 }
                              }
                           }
                        }
                     }

                     this.ticks++;
                  } else {
                     this.cancel();
                  }
               }
            }).runTaskTimer(this.plugin, 0L, 1L);
         }
      }
   }

   private void playCrossSweep(Location center, Vector direction) {
      World world = center.getWorld();
      if (world != null) {
         Vector side;
         if (Math.abs(direction.getY()) > 0.99) {
            side = new Vector(1, 0, 0);
         } else {
            side = new Vector(-direction.getZ(), 0.0, direction.getX()).normalize();
         }

         Vector up = side.clone().crossProduct(direction).normalize();
         Vector diagonalOne = up.clone().add(side).normalize();
         Vector diagonalTwo = up.clone().subtract(side).normalize();
         DustOptions blackDust = new DustOptions(Color.fromRGB(0, 0, 0), 2.0F);
         double size = AltarTuning.number("shadow_blade","tuning.shadow-blade-manager.size",1.6,0.01,128);

         for (double i = -size; i <= size; i += 0.2) {
            double bulge = Math.cos(i / size * (Math.PI / 2)) * 2.0;
            Location pointOne = center.clone().add(diagonalOne.clone().multiply(i)).add(direction.clone().multiply(bulge));
            Location pointTwo = center.clone().add(diagonalTwo.clone().multiply(i)).add(direction.clone().multiply(bulge));
            world.spawnParticle(Particle.DUST, pointOne,AltarTuning.particles("shadow_blade",2), 0.05, 0.05, 0.05, 0.0, blackDust);
            world.spawnParticle(Particle.DUST, pointTwo,AltarTuning.particles("shadow_blade",2), 0.05, 0.05, 0.05, 0.0, blackDust);
         }
      }
   }

   private void spawnBlackDust(Location loc) {
      DustOptions blackDust = new DustOptions(Color.fromRGB(0, 0, 0), 2.5F);
      loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.0, 1.0, 0.0),AltarTuning.particles("shadow_blade",60), 0.5, 1.0, 0.5, blackDust);
   }

   private void startCooldownBar(final Player p, final int seconds, String title, final Map<UUID, BossBar> barMap, Map<UUID, Long> cdMap) {
      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(this.yellowTitle(title), BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(p);
      barMap.put(p.getUniqueId(), bar);
      (new BukkitRunnable() {
         double progress = 1.0;
         final double step = 1.0 / ((double)seconds * 20.0);

         @Override
         public void run() {
            if (p.isOnline() && barMap.containsKey(p.getUniqueId()) && barMap.get(p.getUniqueId()) == bar) {
               this.progress = this.progress - this.step;
               if (this.progress <= 0.0) {
                  bar.removeAll();
                  barMap.remove(p.getUniqueId());
                  this.cancel();
               } else {
                  bar.setProgress(Math.max(0.0, this.progress));
               }
            } else {
               bar.removeAll();
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private String yellowTitle(String title) {
      return ChatColor.YELLOW + ChatColor.stripColor(title);
   }

   public void resetCooldowns(Player player) {
      UUID uuid = player.getUniqueId();
      this.leapCooldowns.remove(uuid);
      this.daggersCooldowns.remove(uuid);
      this.backstabCooldowns.remove(uuid);
      this.activeLeaps.remove(uuid);
      this.currentLeapLocs.remove(uuid);
      this.leapStartTimes.remove(uuid);
      BossBar leapBar = this.leapCooldownBars.remove(uuid);
      BossBar daggersBar = this.daggersCooldownBars.remove(uuid);
      if (leapBar != null) {
         leapBar.removeAll();
      }

      if (daggersBar != null) {
         daggersBar.removeAll();
      }
   }

   public void clearAllBars() {
      this.leapCooldownBars.values().forEach(BossBar::removeAll);
      this.daggersCooldownBars.values().forEach(BossBar::removeAll);
      this.leapCooldownBars.clear();
      this.daggersCooldownBars.clear();
      this.leapCooldowns.clear();
      this.daggersCooldowns.clear();
      this.backstabCooldowns.clear();
      this.activeLeaps.clear();
      this.currentLeapLocs.clear();
      this.leapStartTimes.clear();
   }
}

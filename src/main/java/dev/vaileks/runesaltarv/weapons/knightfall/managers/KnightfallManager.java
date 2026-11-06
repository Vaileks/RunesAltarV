package dev.vaileks.runesaltarv.weapons.knightfall.managers;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class KnightfallManager {
   private static final int THROW_INTERPOLATION_TICKS = 2;
   private final RunesAltarV plugin;
   private final Map<UUID, Integer> hookCharges = new HashMap<>();
   private final Map<UUID, Long> hookNextCharge = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("knightfall","hookNextCharge");
   private final Map<UUID, BossBar> hookBars = new HashMap<>();
   private final Map<UUID, BossBar> cloakCooldownBars = new HashMap<>();
   private final Map<UUID, Long> cloakCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("knightfall","cloak");
   private final Map<UUID, BossBar> throwCooldownBars = new HashMap<>();
   private final Map<UUID, Long> throwCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("knightfall","throw");

   public KnightfallManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.startChargeTask();
   }

   private boolean useParticleChains() {
      return this.plugin.getWeaponsConfig().getBoolean("knightfall.chain.use-particles", true);
   }

   public boolean canUseCloak(Player player) {
      return this.cloakCooldowns.getOrDefault(player.getUniqueId(), 0L) <= System.currentTimeMillis();
   }

   public void triggerCloak(final Player player) {
      final int duration = this.plugin.getWeaponsConfig().getInt("knightfall.cloak.duration", 3) * 20;
      int cooldown = this.plugin.getWeaponsConfig().getInt("knightfall.cloak.cooldown", 8);
      this.cloakCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (long)cooldown * 1000L);
      this.startCloakCooldownBar(player, cooldown);
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0F, 1.2F);
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0F, 1.2F);
      this.spawnBlackDust(player.getLocation());
      WorldBorder border = Bukkit.createWorldBorder();
      border.setCenter(player.getLocation());
      border.setSize(10000.0);
      border.setWarningDistance(10000);
      player.setWorldBorder(border);
      player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, false, false));

      for (Player online : Bukkit.getOnlinePlayers()) {
         online.hidePlayer(this.plugin, player);
      }

      (new BukkitRunnable() {
         int ticks = 0;

         @Override
         public void run() {
            if (player.isOnline() && !player.isDead() && this.ticks < duration) {
               TextUtils.sendActionBar(player, TextUtils.legacy(KnightfallManager.this.plugin.tr("&c[ &c&lнᴇʙидимоᴄᴛь &c]", "&c[ &c&lɪɴᴠɪsɪʙɪʟɪᴛʏ &c]")));
               this.ticks++;
            } else {
               player.setWorldBorder(null);
               player.removePotionEffect(PotionEffectType.INVISIBILITY);

               for (Player online : Bukkit.getOnlinePlayers()) {
                  online.showPlayer(KnightfallManager.this.plugin, player);
               }

               if (player.isOnline()) {
                  player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0F, 1.2F);
                  player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0F, 1.2F);
                  KnightfallManager.this.spawnBlackDust(player.getLocation());
               }

               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void startCloakCooldownBar(final Player player, final int seconds) {
      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(this.yellowTitle(this.plugin.tr("плᴀщ", "ᴄʟᴏᴀᴋ")), BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(player);
      this.cloakCooldownBars.put(player.getUniqueId(), bar);
      (new BukkitRunnable() {
            double progress = 1.0;
            final double step = 1.0 / ((double)seconds * 20.0);

            @Override
            public void run() {
               if (player.isOnline()
                  && KnightfallManager.this.cloakCooldownBars.containsKey(player.getUniqueId())
                  && KnightfallManager.this.cloakCooldownBars.get(player.getUniqueId()) == bar) {
                  this.progress = this.progress - this.step;
                  if (this.progress <= 0.0) {
                     bar.removeAll();
                     KnightfallManager.this.cloakCooldownBars.remove(player.getUniqueId());
                     this.cancel();
                  } else {
                     bar.setProgress(Math.max(0.0, this.progress));
                  }
               } else {
                  bar.removeAll();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private void spawnBlackDust(Location loc) {
      DustOptions blackDust = new DustOptions(Color.fromRGB(0, 0, 0), 2.5F);
      loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.0, 1.0, 0.0), 60, 0.5, 1.0, 0.5, blackDust);
   }

   public int getCharges(Player player) {
      return this.hookCharges.getOrDefault(player.getUniqueId(), this.plugin.getWeaponsConfig().getInt("knightfall.grapple.max-charges", 3));
   }

   private void startChargeTask() {
      final int chargeCooldown = this.plugin.getWeaponsConfig().getInt("knightfall.grapple.cooldown", 10) * 1000;
      final int maxCharges = this.plugin.getWeaponsConfig().getInt("knightfall.grapple.max-charges", 3);
      (new BukkitRunnable() {
         @Override
         public void run() {
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
               UUID id = player.getUniqueId();
               int charges = KnightfallManager.this.getCharges(player);
               int usedCharges = maxCharges - charges;
               if (charges < maxCharges) {
                  long next = KnightfallManager.this.hookNextCharge.getOrDefault(id, now);
                  if (now < next) {
                     KnightfallManager.this.updateHookBar(player, usedCharges, (double)(next - now) / (double)chargeCooldown);
                     continue;
                  }

                  KnightfallManager.this.hookCharges.put(id, charges + 1);
                  KnightfallManager.this.hookNextCharge.put(id, now + (long)chargeCooldown);
               }

               if (KnightfallManager.this.getCharges(player) == maxCharges && KnightfallManager.this.hookBars.containsKey(id)) {
                  KnightfallManager.this.hookBars.remove(id).removeAll();
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 2L);
   }

   private void updateHookBar(Player player, int usedCharges, double timeRemainingRaw) {
      if (!this.hookBars.containsKey(player.getUniqueId())) {
         BossBar bar = TextUtils.cooldownBar("", BarColor.YELLOW, BarStyle.SOLID);
         bar.addPlayer(player);
         this.hookBars.put(player.getUniqueId(), bar);
      }

      BossBar bar = this.hookBars.get(player.getUniqueId());
      bar.setTitle(this.yellowTitle(this.plugin.tr("ᴀбордᴀжный ᴋрюᴋ x", "ɢʀᴀᴘᴘʟɪɴɢ ʜᴏᴏᴋ x") + usedCharges));
      bar.setProgress(Math.max(0.0, Math.min(1.0, 1.0 - timeRemainingRaw)));
   }

   private void updateDynamicChain(Player player, Location target, List<BlockDisplay> chainLinks) {
      Location startLoc = player.getLocation().add(0.0, 1.0, 0.0);
      if (this.useParticleChains()) {
         for (BlockDisplay display : chainLinks) {
            display.remove();
         }

         chainLinks.clear();
         this.spawnParticleChain(startLoc, target);
      } else {
         double step = Math.max(0.22, this.plugin.getWeaponsConfig().getDouble("knightfall.chain.segment-length", 0.22));
         double dist = startLoc.distance(target);
         Vector tempDir = target.toVector().subtract(startLoc.toVector());
         Vector dir = tempDir.lengthSquared() > 0.0 ? tempDir.normalize() : new Vector(0, 1, 0);
         int linksNeeded = (int)Math.ceil(dist / step);

         for (int i = 0; i < linksNeeded; i++) {
            Location linkLoc = startLoc.clone().add(dir.clone().multiply((double)i * step));
            linkLoc.setDirection(dir);
            if (i >= chainLinks.size()) {
               BlockDisplay display = linkLoc.getWorld().spawn(linkLoc, BlockDisplay.class, ent -> {
                  ent.setBlock(Material.IRON_CHAIN.createBlockData());
                  ent.setTeleportDuration(0);
                  this.updateChainSegmentRotation(ent);
               });
               this.plugin.getVisualCleanupManager().track(display);
               chainLinks.add(display);
            } else {
               BlockDisplay display = chainLinks.get(i);
               display.setTeleportDuration(0);
               display.teleport(linkLoc);
               this.updateChainSegmentRotation(display);
            }
         }

         while (chainLinks.size() > linksNeeded) {
            BlockDisplay display = chainLinks.removeLast();
            display.remove();
         }
      }
   }

   private void spawnParticleChain(Location startLoc, Location target) {
      double step = Math.max(0.22, this.plugin.getWeaponsConfig().getDouble("knightfall.chain.segment-length", 0.22));
      Vector delta = target.toVector().subtract(startLoc.toVector());
      double length = Math.max(0.001, delta.length());
      Vector dir = delta.clone().normalize();
      BlockData chainData = Material.IRON_CHAIN.createBlockData();
      int points = Math.max(1, (int)Math.ceil(length / step));

      for (int i = 0; i <= points; i++) {
         Location point = startLoc.clone().add(dir.clone().multiply((double)i * step));
         point.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, point, 1, 0.0, 0.0, 0.0, 0.0, chainData);
         if (i % 14 == 0) {
            point.getWorld().spawnParticle(Particle.CRIT, point, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private void updateChainSegmentRotation(BlockDisplay ent) {
      Quaternionf q = new Quaternionf().rotationX((float) (Math.PI / 2));
      Vector3f translation = new Vector3f(-0.5F, 0.0F, -0.5F).rotate(q);
      Transformation t = new Transformation(translation, q, new Vector3f(1.0F, 1.0F, 1.0F), new Quaternionf());
      ent.setInterpolationDelay(0);
      ent.setInterpolationDuration(1);
      ent.setTransformation(t);
   }

   public void playKillEffect(Location loc) {
      if (loc != null && loc.getWorld() != null) {
         final World world = loc.getWorld();
         final Location baseLoc = loc.clone().add(0.0, 0.8, 0.0);
         world.playSound(baseLoc, Sound.ENTITY_WITHER_SHOOT, 0.9F, 0.6F);
         world.playSound(baseLoc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2F, 1.4F);
         final DustOptions coreBlackDust = new DustOptions(Color.fromRGB(15, 15, 20), 1.8F);
         final DustOptions voidPurpleDust = new DustOptions(Color.fromRGB(75, 10, 110), 1.5F);
         final DustOptions tailDust = new DustOptions(Color.fromRGB(30, 20, 40), 0.9F);
         world.spawnParticle(Particle.DUST, baseLoc, 35, 0.4, 0.6, 0.4, 0.05, coreBlackDust);
         world.spawnParticle(Particle.ELECTRIC_SPARK, baseLoc, 20, 0.3, 0.5, 0.3, 0.1);
         (new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 35;
            final int cometCount = 7;
            final List<Location> heads = new ArrayList<>();
            final List<Double> angles = new ArrayList<>();
            final List<Double> swirlSpeeds = new ArrayList<>();
            final List<Double> vYs = new ArrayList<>();
            final List<Double> radii = new ArrayList<>();
            final List<Vector> drift = new ArrayList<>();

            {
               for (int i = 0; i < 7; i++) {
                  double startAngle = (double)i * (Math.PI * 2.0 / 7.0) + (Math.random() - 0.5) * 0.4;
                  double r = 0.35 + Math.random() * 0.3;
                  this.angles.add(startAngle);
                  this.radii.add(r);
                  this.swirlSpeeds.add((Math.random() > 0.5 ? 1.0 : -1.0) * (0.18 + Math.random() * 0.16));
                  this.vYs.add(0.32 + Math.random() * 0.22);
                  this.drift.add(new Vector((Math.random() - 0.5) * 0.08, 0.0, (Math.random() - 0.5) * 0.08));
                  this.heads.add(baseLoc.clone().add(Math.cos(startAngle) * r, (Math.random() - 0.5) * 0.3, Math.sin(startAngle) * r));
               }
            }

            @Override
            public void run() {
               if (this.ticks++ >= 35) {
                  this.cancel();
               } else {
                  for (int i = 0; i < 7; i++) {
                     Location head = this.heads.get(i);
                     double angle = this.angles.get(i) + this.swirlSpeeds.get(i);
                     this.angles.set(i, angle);
                     double r = this.radii.get(i) + 0.015;
                     this.radii.set(i, r);
                     double vy = Math.max(0.12, this.vYs.get(i) * 0.98);
                     this.vYs.set(i, vy);
                     Vector d = this.drift.get(i);
                     head.add(d.getX(), vy, d.getZ());
                     Location currentLoc = head.clone().add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
                     DustOptions headDust = i % 2 == 0 ? coreBlackDust : voidPurpleDust;
                     world.spawnParticle(Particle.DUST, currentLoc, 2, 0.04, 0.04, 0.04, headDust);
                     world.spawnParticle(Particle.ELECTRIC_SPARK, currentLoc, 1, 0.02, 0.02, 0.02, 0.01);
                     Location tailLoc = currentLoc.clone().subtract(0.0, vy * 0.5, 0.0);
                     world.spawnParticle(Particle.DUST, tailLoc, 1, 0.02, 0.02, 0.02, tailDust);
                  }
               }
            }
         }).runTaskTimer(this.plugin, 1L, 1L);
      }
   }

   private boolean isEntityInsidePlayer(Player player, Entity entity, Location startDirLoc) {
      if (entity != null && player != null) {
         try {
            if (player.getBoundingBox().expand(0.35).overlaps(entity.getBoundingBox())) {
               return true;
            }

            if (entity.getBoundingBox().contains(startDirLoc.toVector()) || entity.getBoundingBox().contains(player.getLocation().toVector())) {
               return true;
            }
         } catch (Throwable var5) {
         }

         return entity.getLocation().distanceSquared(player.getLocation()) < 1.44 || entity.getLocation().distanceSquared(startDirLoc) < 1.44;
      } else {
         return false;
      }
   }

   private boolean isValidHookTarget(Player player, Entity entity, Location startDirLoc) {
      if (entity != null && !entity.equals(player) && entity.isValid() && !entity.isDead()) {
         if (entity instanceof Player pTarget && (pTarget.getGameMode() == GameMode.SPECTATOR || !player.canSee(pTarget))) {
            return false;
         }

         if (entity instanceof ArmorStand as && (as.isMarker() || !as.isVisible())) {
            return false;
         }

         return !this.isEntityInsidePlayer(player, entity, startDirLoc);
      } else {
         return false;
      }
   }

   public void useHook(Player player) {
      int charges = this.getCharges(player);
      if (charges > 0) {
         Location startDirLoc = player.getEyeLocation();
         Vector dir = startDirLoc.getDirection().normalize();
         double maxDist = this.plugin.getWeaponsConfig().getDouble("knightfall.grapple.range", 25.0);
         RayTraceResult narrowTrace = player.getWorld()
            .rayTrace(
               startDirLoc,
               dir,
               maxDist,
               FluidCollisionMode.NEVER,
               true,
               0.3,
               entity -> this.isValidHookTarget(player, entity, startDirLoc) && !(entity instanceof Projectile)
            );
         RayTraceResult wideTrace = player.getWorld()
            .rayTrace(
               startDirLoc,
               dir,
               maxDist,
               FluidCollisionMode.NEVER,
               true,
               2.0,
               entity -> entity instanceof Projectile && this.isValidHookTarget(player, entity, startDirLoc)
            );
         RayTraceResult rayTrace = null;
         if (narrowTrace != null && wideTrace != null) {
            double d1 = narrowTrace.getHitPosition().distanceSquared(startDirLoc.toVector());
            double d2 = wideTrace.getHitPosition().distanceSquared(startDirLoc.toVector());
            rayTrace = d1 < d2 ? narrowTrace : wideTrace;
         } else if (narrowTrace != null) {
            rayTrace = narrowTrace;
         } else {
            rayTrace = wideTrace;
         }

         if (rayTrace != null) {
            Location targetLoc = null;
            boolean isFloor = false;
            if (rayTrace.getHitBlock() != null) {
               targetLoc = rayTrace.getHitPosition().toLocation(player.getWorld());
               if (rayTrace.getHitBlockFace() == BlockFace.UP) {
                  isFloor = true;
               }
            } else if (rayTrace.getHitEntity() != null) {
               Entity hitEnt = rayTrace.getHitEntity();
               if (!this.isValidHookTarget(player, hitEnt, startDirLoc)) {
                  return;
               }

               if (hitEnt instanceof LivingEntity || hitEnt instanceof Projectile) {
                  targetLoc = hitEnt.getLocation().add(0.0, hitEnt.getHeight() / 2.0, 0.0);
                  isFloor = true;
                  if (hitEnt instanceof LivingEntity victim) {
                     if (victim instanceof Player && victim.getHealth() <= 2.0) {
                        this.playKillEffect(victim.getLocation());
                     }

                     CombatUtils.runSyntheticDamage(() -> victim.damage(2.0, player));
                  }
               }
            }

            if (targetLoc != null) {
               this.hookCharges.put(player.getUniqueId(), charges - 1);
               if (charges == this.plugin.getWeaponsConfig().getInt("knightfall.grapple.max-charges", 3)) {
                  this.hookNextCharge
                     .put(
                        player.getUniqueId(),
                        System.currentTimeMillis() + (long)this.plugin.getWeaponsConfig().getInt("knightfall.grapple.cooldown", 10) * 1000L
                     );
               }

               player.swingMainHand();
               player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0F, 2.0F);
               player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1.0F, 2.0F);
               List<BlockDisplay> chainLinks = new ArrayList<>();
               this.updateDynamicChain(player, targetLoc, chainLinks);
               this.pullPlayer(player, targetLoc, chainLinks, isFloor);
            }
         }
      }
   }

   private void pullPlayer(final Player player, final Location target, final List<BlockDisplay> chainLinks, final boolean isFloor) {
      target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target, 25, 0.25, 0.25, 0.25, 0.05, Material.IRON_CHAIN.createBlockData());
      player.getWorld().playSound(target, Sound.BLOCK_CHAIN_BREAK, 1.0F, 1.5F);
      (new BukkitRunnable() {
         int ticks = 0;
         Location lastLoc = player.getLocation();
         int stuckTicks = 0;

         @Override
         public void run() {
            if (player.isOnline() && !player.isDead()) {
               KnightfallManager.this.updateDynamicChain(player, target, chainLinks);
               if (this.ticks % 1 == 0) {
                  player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_STEP, 2.0F, 0.9F);
               }

               if (!(player.getLocation().distanceSquared(target) < 2.5) && !(player.getEyeLocation().distanceSquared(target) < 2.5)) {
                  if (this.ticks > 0 && this.ticks % 5 == 0) {
                     if (player.getLocation().distanceSquared(this.lastLoc) < 0.2) {
                        this.stuckTicks++;
                        if (this.stuckTicks >= 2) {
                           if (isFloor) {
                              Vector boost = player.getLocation().getDirection().setY(0).normalize().multiply(0.5).setY(1.1);
                              player.setVelocity(boost);
                           } else {
                              player.setVelocity(new Vector(0.0, 1.2, 0.0));
                           }

                           chainLinks.forEach(Entity::remove);
                           this.cancel();
                           return;
                        }
                     } else {
                        this.stuckTicks = 0;
                     }

                     this.lastLoc = player.getLocation();
                  }

                  Vector pullVec = target.toVector().subtract(player.getLocation().toVector());
                  double distance = pullVec.length();
                  if (distance > 0.001) {
                     pullVec.normalize().multiply(Math.min(1.6, 0.5 + distance * 0.12));
                  }

                  player.setVelocity(pullVec);
                  if (this.ticks > 60) {
                     chainLinks.forEach(Entity::remove);
                     this.cancel();
                  }

                  this.ticks++;
               } else {
                  player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 2.0F, 1.0F);
                  if (isFloor) {
                     Vector boost = player.getLocation().getDirection().setY(0).normalize().multiply(0.5).setY(1.1);
                     player.setVelocity(boost);
                  }

                  chainLinks.forEach(Entity::remove);
                  this.cancel();
               }
            } else {
               chainLinks.forEach(Entity::remove);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   public void throwHammer(final Player player, final ItemStack item) {
      if (this.throwCooldowns.getOrDefault(player.getUniqueId(), 0L) <= System.currentTimeMillis()) {
         int cooldown = this.plugin.getWeaponsConfig().getInt("knightfall.throw.cooldown", 20);
         this.throwCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (long)cooldown * 1000L);
         this.startThrowCooldownBar(player, cooldown);
         if (this.plugin.getCrazySlotsManager() != null && this.plugin.getCrazySlotsManager().isTransformedItem(item)) {
            UUID instId = this.plugin.getCrazySlotsManager().getTransformedInstanceId(item);
            this.plugin.getCrazySlotsManager().markInFlight(instId);
         }

         player.getInventory().setItemInMainHand(null);
         player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0F, 0.5F);
         player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1.0F, 2.0F);
         final Location start = player.getEyeLocation();
         final Vector dir = start.getDirection().normalize();
         final double maxDist = this.plugin.getWeaponsConfig().getDouble("knightfall.throw.range", 30.0);
         final double configDmg = this.plugin.getWeaponsConfig().getDouble("knightfall.throw.damage", 15.0);
         final double flightSpeed = this.plugin.getWeaponsConfig().getDouble("knightfall.throw.flight-speed", 1.5);
         final double returnSpeed = this.plugin.getWeaponsConfig().getDouble("knightfall.throw.return-speed", 1.5);
         final int turningDelayTicks = this.plugin.getWeaponsConfig().getInt("knightfall.throw.turning-delay-ticks", 15);
         final ItemDisplay display = player.getWorld().spawn(start, ItemDisplay.class, ent -> {
            ent.setItemStack(item);
            ent.setInterpolationDuration(2);
            ent.setTeleportDuration(2);
            ent.setInterpolationDelay(0);
            Transformation t = ent.getTransformation();
            t.getLeftRotation().set(new Quaternionf().rotateX((float)Math.toRadians(90.0)));
            ent.setTransformation(t);
         });
         this.plugin.getVisualCleanupManager().track(display);
         (new BukkitRunnable() {
               final int flyingState = 0;
               final int turningState = 1;
               final int returningState = 2;
               final int waitingState = 3;
               double traveled = 0.0;
               Location curr = start.clone();
               Entity caughtEntity = null;
               int state = 0;
               int stateTimer = 0;
               float roll = 0.0F;
               final List<BlockDisplay> throwChain = new ArrayList<>();

               @Override
               public void run() {
                  if (player.isOnline() && !player.isDead()) {
                     Location playerHandLoc = player.getEyeLocation().subtract(0.0, 0.2, 0.0);
                     if (this.state != 3) {
                        KnightfallManager.this.updateDynamicChain(player, this.curr, this.throwChain);
                     }

                     if (this.state == 0) {
                        LivingEntity targetVictim = null;

                        for (Entity target : this.curr.getWorld().getNearbyEntities(this.curr, 1.4, 1.4, 1.4)) {
                           if (!target.equals(player)
                              && !target.equals(display)
                              && !target.isDead()
                              && target.isValid()
                              && (
                                 !(target instanceof Player targetPlayer)
                                    || targetPlayer.getGameMode() != GameMode.SPECTATOR
                                       && player.canSee(targetPlayer)
                                       && !KnightfallManager.this.plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())
                              )
                              && (!(target instanceof ArmorStand as) || !as.isMarker() && as.isVisible())
                              && (!(this.traveled < 1.5) || !KnightfallManager.this.isEntityInsidePlayer(player, target, start))
                              && target instanceof LivingEntity entity
                              && (
                                 KnightfallManager.this.plugin.getWorldGuardManager() == null
                                    || KnightfallManager.this.plugin.getWorldGuardManager().canAffectTarget(player, entity)
                              )) {
                              targetVictim = entity;
                              break;
                           }
                        }

                        if (targetVictim == null) {
                           RayTraceResult result = this.curr
                              .getWorld()
                              .rayTrace(this.curr, dir.clone(), flightSpeed + 0.2, FluidCollisionMode.NEVER, true, 0.0, entity -> false);
                           if (result != null && result.getHitBlock() != null) {
                              this.curr = result.getHitPosition().toLocation(this.curr.getWorld()).subtract(dir.clone().multiply(0.65));
                              display.teleport(this.curr);
                              this.state = 1;
                              this.stateTimer = turningDelayTicks;
                              display.setInterpolationDelay(0);
                              display.setInterpolationDuration(turningDelayTicks);
                           }
                        } else {
                           if (targetVictim instanceof Player targetPlayer) {
                              boolean hasTotem = targetPlayer.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                                 || targetPlayer.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
                              if (targetPlayer.getHealth() - configDmg <= 0.0 && !hasTotem) {
                                 KnightfallManager.this.playKillEffect(targetVictim.getLocation());
                              }
                           }

                           KnightfallManager.this.plugin.getCleanDamageManager().apply(targetVictim, player, configDmg);
                           player.getWorld().playSound(this.curr, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 0.5F);
                           this.caughtEntity = targetVictim;
                           this.state = 1;
                           this.stateTimer = turningDelayTicks;
                           display.setInterpolationDelay(0);
                           display.setInterpolationDuration(turningDelayTicks);
                        }

                        if (this.state == 0) {
                           this.traveled = this.traveled + flightSpeed;
                           this.curr.add(dir.clone().multiply(flightSpeed));
                           display.setInterpolationDelay(0);
                           display.setInterpolationDuration(2);
                           display.setTeleportDuration(2);
                           this.roll -= 0.56F;
                           Transformation transformation = display.getTransformation();
                           transformation.getLeftRotation().set(new Quaternionf().rotateX((float) (Math.PI / 2)).rotateY((float) Math.PI + this.roll));
                           display.setTransformation(transformation);
                           this.curr.setDirection(dir);
                           display.teleport(this.curr);
                        }

                        this.curr.getWorld().spawnParticle(Particle.CRIT, this.curr, 2, 0.05, 0.05, 0.05, 0.0);
                        if (this.traveled >= maxDist && this.state == 0) {
                           this.state = 1;
                           this.stateTimer = turningDelayTicks;
                           display.setInterpolationDelay(0);
                           display.setInterpolationDuration(turningDelayTicks);
                        }
                     } else if (this.state != 1 && this.state != 2) {
                        if (this.state == 3) {
                           if (player.getInventory().firstEmpty() != -1) {
                              ItemStack returnItem = item;
                              if (KnightfallManager.this.plugin.getCrazySlotsManager() != null
                                 && KnightfallManager.this.plugin.getCrazySlotsManager().isTransformedItem(returnItem)) {
                                 UUID instId = KnightfallManager.this.plugin.getCrazySlotsManager().getTransformedInstanceId(returnItem);
                                 KnightfallManager.this.plugin.getCrazySlotsManager().unmarkInFlight(instId);
                                 if (!KnightfallManager.this.plugin.getCrazySlotsManager().hasActiveTransformation(instId)) {
                                    returnItem = KnightfallManager.this.plugin.getCrazySlotsManager().getCleanCrazySlots(instId);
                                    KnightfallManager.this.plugin.getCrazySlotsManager().endTransformationAndApplyCooldown(player, instId);
                                 }
                              }

                              player.getInventory().addItem(new ItemStack[]{returnItem});
                              player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
                              display.remove();
                              this.cancel();
                              return;
                           }

                           this.stateTimer--;
                           if (this.stateTimer <= 0) {
                              ItemStack toDrop = item;
                              if (KnightfallManager.this.plugin.getCrazySlotsManager() != null
                                 && KnightfallManager.this.plugin.getCrazySlotsManager().isTransformedItem(toDrop)) {
                                 UUID instId = KnightfallManager.this.plugin.getCrazySlotsManager().getTransformedInstanceId(toDrop);
                                 KnightfallManager.this.plugin.getCrazySlotsManager().unmarkInFlight(instId);
                                 toDrop = KnightfallManager.this.plugin.getCrazySlotsManager().getCleanCrazySlots(instId);
                                 KnightfallManager.this.plugin.getCrazySlotsManager().endTransformationAndApplyCooldown(player, instId);
                              }

                              player.getWorld().dropItem(player.getLocation(), toDrop);
                              player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 0.6F);
                              display.remove();
                              this.cancel();
                              return;
                           }

                           double orbitRadius = 0.8;
                           double orbitSpeed = 0.4;
                           double currentAngle = (double)(300 - this.stateTimer) * orbitSpeed;
                           this.curr = player.getLocation().add(Math.cos(currentAngle) * orbitRadius, 2.3, Math.sin(currentAngle) * orbitRadius);
                           display.setInterpolationDelay(0);
                           display.setInterpolationDuration(1);
                           display.setTeleportDuration(1);
                           this.roll -= 0.56F;
                           Transformation transformation = display.getTransformation();
                           transformation.getLeftRotation().set(new Quaternionf().rotateX((float) (Math.PI / 2)).rotateY((float) Math.PI + this.roll));
                           display.setTransformation(transformation);
                           display.teleport(this.curr);
                        }
                     } else {
                        if (this.caughtEntity != null && this.caughtEntity.getLocation().getBlock().getType() == Material.COBWEB) {
                           player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0F, 1.0F);
                           this.caughtEntity.setFallDistance(0.0F);
                           this.caughtEntity = null;
                        }

                        Vector toPlayer = playerHandLoc.toVector().subtract(this.curr.toVector());
                        double currentReturnSpeed = this.state == 1 ? returnSpeed / 3.0 : returnSpeed;
                        if (this.state == 1 && --this.stateTimer <= 0) {
                           this.state = 2;
                           display.setInterpolationDuration(2);
                        }

                        if (toPlayer.length() < 2.0) {
                           if (this.caughtEntity != null) {
                              this.caughtEntity.setFallDistance(0.0F);
                              this.caughtEntity.setVelocity(new Vector(0, 0, 0));
                              this.caughtEntity = null;
                           }

                           if (player.getInventory().firstEmpty() != -1) {
                              ItemStack returnItem = item;
                              if (KnightfallManager.this.plugin.getCrazySlotsManager() != null
                                 && KnightfallManager.this.plugin.getCrazySlotsManager().isTransformedItem(returnItem)) {
                                 UUID instId = KnightfallManager.this.plugin.getCrazySlotsManager().getTransformedInstanceId(returnItem);
                                 KnightfallManager.this.plugin.getCrazySlotsManager().unmarkInFlight(instId);
                                 if (!KnightfallManager.this.plugin.getCrazySlotsManager().hasActiveTransformation(instId)) {
                                    returnItem = KnightfallManager.this.plugin.getCrazySlotsManager().getCleanCrazySlots(instId);
                                    KnightfallManager.this.plugin.getCrazySlotsManager().endTransformationAndApplyCooldown(player, instId);
                                 }
                              }

                              player.getInventory().addItem(new ItemStack[]{returnItem});
                              display.remove();
                              this.throwChain.forEach(Entity::remove);
                              this.cancel();
                           } else {
                              this.state = 3;
                              this.stateTimer = 300;
                              this.throwChain.forEach(Entity::remove);
                              this.throwChain.clear();
                           }
                        } else {
                           Vector returnStep = toPlayer.normalize().multiply(currentReturnSpeed);
                           this.curr.add(returnStep);
                           this.curr.setDirection(dir);
                           display.setInterpolationDelay(0);
                           display.setInterpolationDuration(2);
                           display.setTeleportDuration(2);
                           this.roll -= 0.56F;
                           Transformation transformation = display.getTransformation();
                           transformation.getLeftRotation().set(new Quaternionf().rotateX((float) (Math.PI / 2)).rotateY((float) Math.PI + this.roll));
                           display.setTransformation(transformation);
                           display.teleport(this.curr);
                           this.curr.getWorld().spawnParticle(Particle.CRIT, this.curr, 2, 0.05, 0.05, 0.05, 0.0);
                           if (this.caughtEntity != null && !this.caughtEntity.isDead()) {
                              Vector pullVector = this.curr.toVector().subtract(this.caughtEntity.getLocation().toVector());
                              this.caughtEntity.setVelocity(pullVector);
                              this.caughtEntity.setFallDistance(0.0F);
                           }
                        }
                     }
                  } else {
                     if (player.isDead() && this.state == 3) {
                        this.curr.getWorld().playSound(this.curr, Sound.ENTITY_ITEM_PICKUP, 1.0F, 0.6F);
                     }

                     ItemStack toDrop = item;
                     if (KnightfallManager.this.plugin.getCrazySlotsManager() != null
                        && KnightfallManager.this.plugin.getCrazySlotsManager().isTransformedItem(toDrop)) {
                        UUID instId = KnightfallManager.this.plugin.getCrazySlotsManager().getTransformedInstanceId(toDrop);
                        KnightfallManager.this.plugin.getCrazySlotsManager().unmarkInFlight(instId);
                        toDrop = KnightfallManager.this.plugin.getCrazySlotsManager().getCleanCrazySlots(instId);
                        KnightfallManager.this.plugin.getCrazySlotsManager().endTransformationAndApplyCooldown(player, instId);
                     }

                     this.curr.getWorld().dropItem(this.curr, toDrop);
                     display.remove();
                     this.throwChain.forEach(Entity::remove);
                     if (this.caughtEntity != null) {
                        this.caughtEntity.setFallDistance(0.0F);
                     }

                     this.cancel();
                  }
               }
            })
            .runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void startThrowCooldownBar(final Player player, final int seconds) {
      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(this.yellowTitle(this.plugin.tr("броᴄоᴋ молоᴛᴀ", "ʜᴀᴍᴍᴇʀ ᴛʜʀᴏᴡ")), BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(player);
      this.throwCooldownBars.put(player.getUniqueId(), bar);
      (new BukkitRunnable() {
            double progress = 1.0;
            final double step = 1.0 / ((double)seconds * 20.0);

            @Override
            public void run() {
               if (player.isOnline()
                  && KnightfallManager.this.throwCooldownBars.containsKey(player.getUniqueId())
                  && KnightfallManager.this.throwCooldownBars.get(player.getUniqueId()) == bar) {
                  this.progress = this.progress - this.step;
                  if (this.progress <= 0.0) {
                     bar.removeAll();
                     KnightfallManager.this.throwCooldownBars.remove(player.getUniqueId());
                     this.cancel();
                  } else {
                     bar.setProgress(Math.max(0.0, this.progress));
                  }
               } else {
                  bar.removeAll();
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   private String yellowTitle(String title) {
      return title == null ? "" : "§e" + title.replaceAll("(?i)[§&][0-9A-FK-OR]", "");
   }

   public void resetCooldowns(Player player) {
      UUID uuid = player.getUniqueId();
      this.hookCharges.remove(uuid);
      this.hookNextCharge.remove(uuid);
      this.cloakCooldowns.remove(uuid);
      this.throwCooldowns.remove(uuid);
      BossBar hookBar = this.hookBars.remove(uuid);
      BossBar cloakBar = this.cloakCooldownBars.remove(uuid);
      BossBar throwBar = this.throwCooldownBars.remove(uuid);
      if (hookBar != null) {
         hookBar.removeAll();
      }

      if (cloakBar != null) {
         cloakBar.removeAll();
      }

      if (throwBar != null) {
         throwBar.removeAll();
      }
   }

   public void clearAllBars() {
      this.hookBars.values().forEach(BossBar::removeAll);
      this.cloakCooldownBars.values().forEach(BossBar::removeAll);
      this.throwCooldownBars.values().forEach(BossBar::removeAll);
      this.hookBars.clear();
      this.cloakCooldownBars.clear();
      this.throwCooldownBars.clear();
      this.hookCharges.clear();
      this.hookNextCharge.clear();
      this.cloakCooldowns.clear();
      this.throwCooldowns.clear();
   }
}

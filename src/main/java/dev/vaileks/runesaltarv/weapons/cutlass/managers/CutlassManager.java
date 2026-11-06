package dev.vaileks.runesaltarv.weapons.cutlass.managers;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class CutlassManager {
   private final RunesAltarV plugin;
   private final NamespacedKey cutlassKey;
   private final Map<String, Long> cooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("cutlass","");
   private final Map<String, BossBar> activeBars = new HashMap<>();
   private final Map<UUID, CutlassManager.ParryData> activeParries = new HashMap<>();
   private final Map<UUID, BukkitTask> activeThousandCuts = new HashMap<>();

   public CutlassManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.cutlassKey = new NamespacedKey("runesaltarv", "cutlass");
   }

   public boolean isCutlass(ItemStack item) {
      return item != null && !item.getType().isAir() && item.hasItemMeta()
         ? item.getItemMeta().getPersistentDataContainer().has(this.cutlassKey, PersistentDataType.BYTE)
         : false;
   }

   public void setCutlassAttributes(ItemStack item, double damage, double attackSpeed) {
      if (item != null && !item.getType().isAir() && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
         meta.removeAttributeModifier(Attribute.ATTACK_SPEED);
         NamespacedKey damageKey = new NamespacedKey("runesaltarv", "cutlass_damage");
         NamespacedKey speedKey = new NamespacedKey("runesaltarv", "cutlass_speed");
         meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(damageKey, damage - 1.0, Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
         meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(speedKey, attackSpeed - 4.0, Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
         meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
         item.setItemMeta(meta);
      }
   }

   public boolean checkCooldown(Player player, String ability) {
      String key = player.getUniqueId() + ":" + ability;
      return this.cooldowns.getOrDefault(key, 0L) > System.currentTimeMillis();
   }

   public void applyCooldown(final Player player, String ability, final int seconds, String title) {
      String key = player.getUniqueId() + ":" + ability;
      this.cooldowns.put(key, System.currentTimeMillis() + (long)seconds * 1000L);
      final String barKey = player.getUniqueId() + ":cd_" + ability;
      this.removeOldBar(barKey);
      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(this.yellowTitle(title), BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(player);
      this.activeBars.put(barKey, bar);
      (new BukkitRunnable() {
            int ticksLeft = seconds * 20;

            @Override
            public void run() {
               if (this.ticksLeft-- > 0
                  && player.isOnline()
                  && CutlassManager.this.activeBars.containsKey(barKey)
                  && CutlassManager.this.activeBars.get(barKey) == bar) {
                  bar.setProgress((double)this.ticksLeft / (double)(seconds * 20));
               } else {
                  CutlassManager.this.removeOldBar(barKey);
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   public boolean hasActiveParry(UUID uuid) {
      return this.activeParries.containsKey(uuid);
   }

   public CutlassManager.ParryData getParryData(UUID uuid) {
      return this.activeParries.get(uuid);
   }

   public void startParry(final Player player) {
      int maxCharges = this.plugin.getWeaponsConfig().getInt("cutlass.parry.max-charges", 3);
      int durationSeconds = this.plugin.getWeaponsConfig().getInt("cutlass.parry.max-duration-seconds", 7);
      int slownessAmplifier = Math.max(0, this.plugin.getWeaponsConfig().getInt("cutlass.parry.slowness-amplifier", 0));
      long durationMillis = (long)durationSeconds * 1000L;
      final String barKey = player.getUniqueId() + ":parry_active";
      this.removeOldBar(barKey);
      BossBar bar = TextUtils.bossBar(this.parryTitle(maxCharges), BarColor.RED, BarStyle.SOLID);
      bar.addPlayer(player);
      this.activeBars.put(barKey, bar);
      CutlassManager.ParryData data = new CutlassManager.ParryData(maxCharges, System.currentTimeMillis(), durationMillis, bar);
      this.activeParries.put(player.getUniqueId(), data);
      if (slownessAmplifier > 0) {
         player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationSeconds * 20 + 10, slownessAmplifier - 1, false, false, true));
      }

      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0F, 1.2F);
      final DustOptions blueDust = new DustOptions(Color.fromRGB(0, 191, 255), 0.6F);
      (new BukkitRunnable() {
         @Override
         public void run() {
            CutlassManager.ParryData current = CutlassManager.this.activeParries.get(player.getUniqueId());
            if (current != null && player.isOnline()) {
               Location waistLoc = player.getLocation().add(0.0, 0.9, 0.0);
               double radius = 0.85;
               int points = 24;

               for (int i = 0; i < points; i++) {
                  double angle = (Math.PI * 2) * (double)i / (double)points;
                  double x = radius * Math.cos(angle);
                  double z = radius * Math.sin(angle);
                  waistLoc.add(x, 0.0, z);
                  player.getWorld().spawnParticle(Particle.DUST, waistLoc, 1, 0.0, 0.0, 0.0, 0.0, blueDust);
                  waistLoc.subtract(x, 0.0, z);
               }

               long elapsed = System.currentTimeMillis() - current.lastHitTime;
               double remaining = (double)(current.durationMillis - elapsed);
               current.bar.setProgress(Math.max(0.0, remaining / (double)current.durationMillis));
               if (elapsed >= current.durationMillis) {
                  current.charges--;
                  current.lastHitTime = System.currentTimeMillis();
                  if (current.charges <= 0) {
                     CutlassManager.this.endParry(player);
                     this.cancel();
                  } else {
                     current.bar.setTitle(CutlassManager.this.parryTitle(current.charges));
                     player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 1.2F);
                  }
               }
            } else {
               CutlassManager.this.removeOldBar(barKey);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   public void endParry(Player player) {
      this.activeParries.remove(player.getUniqueId());
      this.removeOldBar(player.getUniqueId() + ":parry_active");
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0F, 1.0F);
      this.applyCooldown(
         player, "parry", this.plugin.getWeaponsConfig().getInt("cutlass.parry.cooldown", 30), this.plugin.tr("Пиратское отражение", "Pɪʀᴀᴛᴇ's ᴘᴀʀʀʏ")
      );
   }

   public void startThousandCuts(final Player player) {
      int cooldown = this.plugin.getWeaponsConfig().getInt("cutlass.thousand-cuts.cooldown", 45);
      final int durationTicks = this.plugin.getWeaponsConfig().getInt("cutlass.thousand-cuts.duration-ticks", 42);
      final int slownessAmplifier = Math.max(0, this.plugin.getWeaponsConfig().getInt("cutlass.thousand-cuts.slowness-amplifier", 1));
      final int slownessDurationTicks = this.plugin.getWeaponsConfig().getInt("cutlass.thousand-cuts.slowness-duration", 3) * 20;
      this.applyCooldown(player, "thousand_cuts", cooldown, this.plugin.tr("Сто царапин", "Hᴜɴᴅʀᴇᴅ ᴄᴜᴛs"));
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.5F);
      BukkitTask task = (new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
               if (this.ticks < durationTicks && player.isOnline()) {
                  ItemStack itemInHand = player.getInventory().getItemInMainHand();
                  if (!CutlassManager.this.isCutlass(itemInHand)) {
                     CutlassManager.this.cancelThousandCuts(player);
                  } else {
                     if (this.ticks <= 10) {
                        for (Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5)) {
                           if (entity instanceof LivingEntity) {
                              LivingEntity target = (LivingEntity)entity;
                              if (!target.equals(player) && CombatUtils.clearPath(player.getEyeLocation(),target)) {
                                 if (target instanceof Player) {
                                    Player targetPlayer = (Player)target;
                                    if (CutlassManager.this.plugin.getFriendManager().isFriend(player.getUniqueId(), targetPlayer.getUniqueId())) {
                                       continue;
                                    }
                                 }

                                 target.addPotionEffect(
                                    new PotionEffect(PotionEffectType.SLOWNESS, slownessDurationTicks, slownessAmplifier, false, false, true)
                                 );
                              }
                           }
                        }
                     }

                     if (this.ticks % 3 == 0) {
                        player.swingMainHand();
                        CutlassManager.this.playCutlassSwingFX(player);
                        RayTraceResult result = player.getWorld()
                           .rayTraceEntities(
                              player.getEyeLocation(),
                              player.getEyeLocation().getDirection(),
                              3.0,
                              entityx -> entityx instanceof LivingEntity
                                    && !entityx.equals(player)
                                    && (
                                       !(entityx instanceof Player)
                                          || !CutlassManager.this.plugin.getFriendManager().isFriend(player.getUniqueId(), entityx.getUniqueId())
                                    )
                           );
                        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                           CutlassManager.this.dealThousandCutsHit(player, target);
                           Vector knockback = player.getLocation().getDirection().setY(0).normalize().multiply(0.2).setY(0.1);
                           target.setVelocity(target.getVelocity().add(knockback));
                        }
                     }

                     this.ticks++;
                  }
               } else {
                  if (player.isOnline() && this.ticks >= durationTicks) {
                     Vector dash = player.getLocation().getDirection().normalize().multiply(1.5).setY(0.2);
                     player.setVelocity(dash);
                     player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0F, 1.0F);
                  }

                  CutlassManager.this.cancelThousandCuts(player);
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
      this.activeThousandCuts.put(player.getUniqueId(), task);
   }

   public void cancelThousandCuts(Player player) {
      BukkitTask task = this.activeThousandCuts.remove(player.getUniqueId());
      if (task != null) {
         task.cancel();
      }
   }

   public boolean isDoingThousandCuts(Player player) {
      return this.activeThousandCuts.containsKey(player.getUniqueId());
   }

   public void cleanupAll() {
      for (BossBar bar : this.activeBars.values()) {
         bar.removeAll();
      }

      this.activeBars.clear();

      for (BukkitTask task : this.activeThousandCuts.values()) {
         task.cancel();
      }

      this.activeThousandCuts.clear();
      this.activeParries.clear();
   }

   public void playCutlassSwingFX(Player player) {
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
      Location eyeLoc = player.getEyeLocation();
      Vector look = eyeLoc.getDirection().normalize();
      Vector right = look.clone().crossProduct(new Vector(0, 1, 0)).normalize();
      Vector up = look.clone().crossProduct(right).normalize();
      double tiltRad = Math.toRadians((Math.random() * 60.0 + 15.0) * (double)(Math.random() > 0.5 ? 1 : -1));
      Vector diagonalAxis = right.clone().multiply(Math.cos(tiltRad)).add(up.clone().multiply(Math.sin(tiltRad)));
      double radius = 2.0;
      double arcHalfWidth = 65.0;
      int particleCount = 35;

      for (int i = 0; i < particleCount; i++) {
         double angle = Math.toRadians(-arcHalfWidth + (double)i * (arcHalfWidth * 2.0 / (double)(particleCount - 1)));
         Vector offset = look.clone().multiply(Math.cos(angle)).add(diagonalAxis.clone().multiply(Math.sin(angle))).multiply(radius);
         Location particleLoc = eyeLoc.clone().add(offset);
         player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0.02, 0.02, 0.02, 0.0);
         player.getWorld().spawnParticle(Particle.CRIT, particleLoc, 1, 0.03, 0.03, 0.03, 0.0);
      }
   }

   private void removeOldBar(String key) {
      BossBar old = this.activeBars.remove(key);
      if (old != null) {
         old.removeAll();
      }
   }

   private void dealThousandCutsHit(Player player, LivingEntity target) {
      if (!CombatUtils.clearPath(player.getEyeLocation(),target)) return;
      double damage = this.plugin.getWeaponsConfig().getDouble("cutlass.thousand-cuts.hit-damage", 3.5);
      target.setNoDamageTicks(0);
      this.plugin.getCleanDamageManager().apply(target, player, damage);
      ItemStack weapon = player.getInventory().getItemInMainHand();
      if (weapon.hasItemMeta()) {
         int fireAspect = weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
         if (fireAspect > 0) {
            target.setFireTicks(Math.max(target.getFireTicks(), fireAspect * 4 * 20));
         }
      }
   }

   public void resetCooldowns(Player player) {
      String prefix = player.getUniqueId() + ":";
      this.cooldowns.keySet().removeIf(key -> key.startsWith(prefix));
      this.activeBars.entrySet().removeIf(entry -> {
         if (!entry.getKey().startsWith(prefix)) {
            return false;
         } else {
            entry.getValue().removeAll();
            return true;
         }
      });
      this.activeParries.remove(player.getUniqueId());
      this.cancelThousandCuts(player);
   }

   public String parryTitle(int charges) {
      return "§6§l!! §c§l" + this.plugin.tr("ОТРАЖЕНИЕ", "ᴘᴀʀʀʏ") + " §6x" + charges + " §6§l!!";
   }

   public String yellowTitle(String title) {
      return ChatColor.YELLOW + ChatColor.stripColor(title);
   }

   public static class ParryData {
      public int charges;
      public long lastHitTime;
      public final long durationMillis;
      public final BossBar bar;

      public ParryData(int charges, long lastHitTime, long durationMillis, BossBar bar) {
         this.charges = charges;
         this.lastHitTime = lastHitTime;
         this.durationMillis = durationMillis;
         this.bar = bar;
      }
   }
}

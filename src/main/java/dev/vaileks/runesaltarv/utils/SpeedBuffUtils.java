package dev.vaileks.runesaltarv.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class SpeedBuffUtils implements Listener {
   private static final Map<UUID, SpeedBuffUtils.SavedSpeed> savedSpeeds = new ConcurrentHashMap<>();
   private static long tokenCounter = 0L;

   public static void cleanup(UUID uuid) {
      if (uuid != null) {
         savedSpeeds.remove(uuid);
      }
   }

   public static void cleanupAll() {
      savedSpeeds.clear();
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onQuit(PlayerQuitEvent event) {
      cleanup(event.getPlayer().getUniqueId());
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onDeath(PlayerDeathEvent event) {
      cleanup(event.getEntity().getUniqueId());
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onConsume(PlayerItemConsumeEvent event) {
      if (event.getItem() != null && event.getItem().getType() == Material.MILK_BUCKET) {
         cleanup(event.getPlayer().getUniqueId());
      }
   }

   public static void applyBurstSpeed(Player player, int durationTicks, int targetAmplifier, Plugin plugin) {
      if (player != null && player.isOnline()) {
         UUID uuid = player.getUniqueId();
         PotionEffect cur = player.getPotionEffect(PotionEffectType.SPEED);
         boolean isTrackingBurst = savedSpeeds.containsKey(uuid);
         if (isTrackingBurst || cur == null || cur.getAmplifier() <= targetAmplifier) {
            if (isTrackingBurst || cur == null || cur.getAmplifier() != targetAmplifier || cur.getDuration() < durationTicks) {
               boolean wearingBoots = false;
               if (!wearingBoots && !isTrackingBurst && cur != null && cur.getAmplifier() < targetAmplifier) {
                  long token = ++tokenCounter;
                  savedSpeeds.put(uuid, new SpeedBuffUtils.SavedSpeed(cur, System.currentTimeMillis(), token));
               }

               SpeedBuffUtils.SavedSpeed saved = savedSpeeds.get(uuid);
               long currentToken;
               if (saved != null) {
                  currentToken = ++tokenCounter;
                  saved.token = currentToken;
               } else {
                  currentToken = 0L;
               }

               player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, targetAmplifier, false, false), true);
               if (saved != null) {
                  Bukkit.getScheduler()
                     .runTaskLater(
                        plugin,
                        () -> {
                           if (!player.isOnline() || player.isDead()) {
                              savedSpeeds.remove(uuid);
                           } else {
                              SpeedBuffUtils.SavedSpeed currentSaved = savedSpeeds.get(uuid);
                              if (currentSaved != null && currentSaved.token == currentToken) {
                                 savedSpeeds.remove(uuid);
                                 int remTicks;
                                 if (currentSaved.wasInfinite) {
                                    remTicks = -1;
                                 } else {
                                    long elapsedTicks = (System.currentTimeMillis() - currentSaved.startTime) / 50L;
                                    remTicks = (int)((long)currentSaved.originalDuration - elapsedTicks);
                                 }

                                 if (remTicks > 0) {
                                    PotionEffect activeNow = player.getPotionEffect(PotionEffectType.SPEED);
                                    if (activeNow == null || activeNow.getAmplifier() != currentSaved.amplifier || activeNow.getDuration() <= remTicks + 20) {
                                       if (activeNow == null
                                          || activeNow.getAmplifier() <= currentSaved.amplifier
                                          || activeNow.getAmplifier() == targetAmplifier) {
                                          boolean isExpiringBurst = activeNow != null
                                             && activeNow.getAmplifier() == targetAmplifier
                                             && activeNow.getDuration() <= 20;
                                          boolean isWeakerOrNull = activeNow == null || activeNow.getAmplifier() <= currentSaved.amplifier;
                                          if (isWeakerOrNull || isExpiringBurst) {
                                             player.removePotionEffect(PotionEffectType.SPEED);
                                             player.addPotionEffect(
                                                new PotionEffect(
                                                   PotionEffectType.SPEED,
                                                   remTicks,
                                                   currentSaved.amplifier,
                                                   currentSaved.ambient,
                                                   currentSaved.particles,
                                                   currentSaved.icon
                                                )
                                             );
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        },
                        (long)durationTicks
                     );
               }
            }
         }
      }
   }

   private static class SavedSpeed {
      final int amplifier;
      final int originalDuration;
      final boolean wasInfinite;
      final boolean ambient;
      final boolean particles;
      final boolean icon;
      final long startTime;
      long token;

      SavedSpeed(PotionEffect effect, long startTime, long token) {
         this.amplifier = effect.getAmplifier();
         this.originalDuration = effect.getDuration();
         this.wasInfinite = effect.getDuration() == -1;
         this.ambient = effect.isAmbient();
         this.particles = effect.hasParticles();
         this.icon = effect.hasIcon();
         this.startTime = startTime;
         this.token = token;
      }
   }
}

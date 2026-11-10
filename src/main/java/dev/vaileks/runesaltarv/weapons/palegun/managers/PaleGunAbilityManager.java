package dev.vaileks.runesaltarv.weapons.palegun.managers;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.weapons.palegun.tasks.PaleProjectileTask;
import dev.vaileks.runesaltarv.weapons.palegun.tasks.PaleRootsTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PaleGunAbilityManager {
   private final RunesAltarV plugin;
   private final Map<UUID, Long> rootsCooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("pale_gun","roots");
   private final Map<UUID, BossBar> rootsBars = new HashMap<>();
   private final Set<PaleRootsTask> activeRoots = new HashSet<>();
   private final Set<PaleProjectileTask> activeProjectiles = new HashSet<>();
   private final Map<UUID, PaleRootsTask> rootDisplays = new HashMap<>();

   public PaleGunAbilityManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.startMarkedUpdater();
   }

   public boolean activatePaleRoots(Player player) {
      if (this.isRootsOnCooldown(player)) {
         return false;
      } else {
         int durationSeconds = this.plugin.getWeaponsConfig().getInt("pale-gun.pale-roots.duration-seconds", 18);
         double damage = this.plugin.getWeaponsConfig().getDouble("pale-gun.pale-roots.damage", 6.0);
         int cooldownSeconds = this.plugin.getWeaponsConfig().getInt("pale-gun.pale-roots.cooldown", 25);
         PaleRootsTask task = new PaleRootsTask(player, durationSeconds, damage, this.plugin, this);
         this.activeRoots.add(task);
         task.runTaskTimer(this.plugin, 0L, 1L);
         this.startRootsCooldown(player, cooldownSeconds);
         player.getWorld().playSound(player.getLocation(),AltarTuning.sound("pale_gun","visuals.sounds.entity-creaking-attack.sound",Sound.ENTITY_CREAKING_ATTACK),AltarTuning.volume("pale_gun","visuals.sounds.entity-creaking-attack",1.0F),AltarTuning.pitch("pale_gun","visuals.sounds.entity-creaking-attack",0.8F));
         return true;
      }
   }

   public boolean isPaleGun(ItemStack item) {
      return item != null
         && item.getType() == Material.CROSSBOW
         && item.hasItemMeta()
         && item.getItemMeta().getPersistentDataContainer().has(this.plugin.getPaleGunKey(), PersistentDataType.BYTE);
   }

   public void trackProjectile(PaleProjectileTask task) {
      this.activeProjectiles.add(task);
   }

   public void untrackProjectile(PaleProjectileTask task) {
      this.activeProjectiles.remove(task);
   }

   public void untrackRootTask(PaleRootsTask task) {
      this.activeRoots.remove(task);
      this.rootDisplays.values().removeIf(rootTask -> rootTask == task);
   }

   public void trackRootDisplay(BlockDisplay display, PaleRootsTask task) {
      if (display != null && task != null) {
         this.rootDisplays.put(display.getUniqueId(), task);
      }
   }

   public void untrackRootDisplay(Entity display) {
      if (display != null) {
         this.rootDisplays.remove(display.getUniqueId());
      }
   }

   public boolean isPaleRootDisplay(Entity entity) {
      return entity != null && this.rootDisplays.containsKey(entity.getUniqueId());
   }

   public boolean handleRootDisplayHit(Entity entity, Location hitLocation) {
      if (entity instanceof BlockDisplay display) {
         PaleRootsTask task = this.rootDisplays.get(display.getUniqueId());
         if (task == null) {
            return false;
         } else {
            task.breakDisplay(display, hitLocation);
            return true;
         }
      } else {
         return false;
      }
   }

   public void resetPlayer(Player player) {
      UUID uuid = player.getUniqueId();
      this.rootsCooldowns.remove(uuid);
      BossBar bar = this.rootsBars.remove(uuid);
      if (bar != null) {
         bar.removeAll();
      }

      for (ItemStack item : player.getInventory().getContents()) {
         if (this.isPaleGun(item)) {
            player.setCooldown(item, 0);
         }
      }
   }

   public void clearAll() {
      for (PaleProjectileTask task : new ArrayList<>(this.activeProjectiles)) {
         task.forceCleanup();
      }

      this.activeProjectiles.clear();

      for (PaleRootsTask task : new ArrayList<>(this.activeRoots)) {
         task.forceCleanup();
      }

      this.activeRoots.clear();
      this.rootDisplays.clear();

      for (BossBar bar : this.rootsBars.values()) {
         bar.removeAll();
      }

      this.rootsBars.clear();
      this.rootsCooldowns.clear();
   }

   private boolean isRootsOnCooldown(Player player) {
      Long expireAt = this.rootsCooldowns.get(player.getUniqueId());
      return expireAt != null && System.currentTimeMillis() < expireAt;
   }

   private void startRootsCooldown(final Player player, final int seconds) {
      final UUID uuid = player.getUniqueId();
      final long expireAt = System.currentTimeMillis() + (long)seconds * 1000L;
      this.rootsCooldowns.put(uuid, expireAt);
      BossBar oldBar = this.rootsBars.remove(uuid);
      if (oldBar != null) {
         oldBar.removeAll();
      }

      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(AltarTuning.text("pale_gun","messages.pale-gun-ability-manager.message-1","§e§kБледные §r§eКорни","§e§kᴘᴀʟᴇ §r§eʀᴏᴏᴛs"), BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(player);
      this.rootsBars.put(uuid, bar);
      (new BukkitRunnable() {
         @Override
         public void run() {
            if (player.isOnline() && PaleGunAbilityManager.this.rootsBars.containsKey(uuid) && PaleGunAbilityManager.this.rootsBars.get(uuid) == bar) {
               long timeLeft = expireAt - System.currentTimeMillis();
               if (timeLeft <= 0L) {
                  PaleGunAbilityManager.this.removeRootsCooldown(uuid);
                  this.cancel();
               } else {
                  double progress = Math.max(0.0, Math.min(1.0, (double)timeLeft / (double)((long)seconds * 1000L)));
                  bar.setProgress(progress);
               }
            } else {
               bar.removeAll();
               PaleGunAbilityManager.this.removeRootsCooldown(uuid);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 2L);
   }

   private void removeRootsCooldown(UUID uuid) {
      this.rootsCooldowns.remove(uuid);
      BossBar bar = this.rootsBars.remove(uuid);
      if (bar != null) {
         bar.removeAll();
      }
   }

   private void startMarkedUpdater() {
      (new BukkitRunnable() {
         @Override
         public void run() {
            for (World world : Bukkit.getWorlds()) {
               for (LivingEntity entity : world.getLivingEntities()) {
                  if (entity.hasPotionEffect(PotionEffectType.LUCK)) {
                     entity.getWorld().spawnParticle(Particle.INFESTED, entity.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("pale_gun",2), 0.3, 0.45, 0.3, 0.0);
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 10L);
   }
}

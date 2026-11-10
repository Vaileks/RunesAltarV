package dev.vaileks.runesaltarv.weapons.crazyslots.managers;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class CrazySlotsManager {
   private final RunesAltarV plugin;
   private final NamespacedKey uniqueKey;
   private final NamespacedKey crazySlotsKey;
   private final Map<UUID, ItemStack> activeTransformations = new ConcurrentHashMap<>();
   private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
   private final Map<UUID, UUID> playerToInstance = new ConcurrentHashMap<>();
   private final Map<UUID, UUID> instanceToPlayer = new ConcurrentHashMap<>();
   private final Map<UUID, Long> cooldowns = new dev.vaileks.runesaltarv.integration.ObservedCooldownMap<>("crazyslots","transform");
   private final Map<UUID, BossBar> activeBars = new ConcurrentHashMap<>();
   private final Set<UUID> inFlightInstances = ConcurrentHashMap.newKeySet();
   private final Random random = new Random();

   public CrazySlotsManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.uniqueKey = new NamespacedKey("runesaltarv", "crazyslots_unique_id");
      this.crazySlotsKey = new NamespacedKey("runesaltarv", "crazyslots");
   }

   public NamespacedKey getUniqueKey() {
      return this.uniqueKey;
   }

   public NamespacedKey getCrazySlotsKey() {
      return this.crazySlotsKey;
   }

   public void markInFlight(UUID instanceId) {
      if (instanceId != null) {
         this.inFlightInstances.add(instanceId);
      }
   }

   public void unmarkInFlight(UUID instanceId) {
      if (instanceId != null) {
         this.inFlightInstances.remove(instanceId);
      }
   }

   public boolean isInFlight(UUID instanceId) {
      return instanceId != null && this.inFlightInstances.contains(instanceId);
   }

   public boolean isCrazySlots(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "crazyslots");
   }

   public boolean isTransformedItem(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         return meta == null ? false : meta.getPersistentDataContainer().has(this.uniqueKey, PersistentDataType.STRING);
      } else {
         return false;
      }
   }

   public UUID getTransformedInstanceId(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return null;
         } else {
            String idStr = meta.getPersistentDataContainer().get(this.uniqueKey, PersistentDataType.STRING);
            if (idStr == null) {
               return null;
            } else {
               try {
                  return UUID.fromString(idStr);
               } catch (IllegalArgumentException var5) {
                  return null;
               }
            }
         }
      } else {
         return null;
      }
   }

   public UUID getInstanceIdForPlayer(UUID playerUUID) {
      return this.playerToInstance.get(playerUUID);
   }

   public boolean hasActiveTransformation(UUID instanceId) {
      return instanceId != null && this.activeTransformations.containsKey(instanceId);
   }

   public ItemStack getOriginalCrazySlots(UUID instanceId) {
      return instanceId != null ? this.activeTransformations.get(instanceId) : null;
   }

   public ItemStack getCleanCrazySlots(UUID instanceId) {
      ItemStack original = instanceId != null ? this.activeTransformations.get(instanceId) : null;
      if (original == null) {
         original = WeaponFactory.getCrazySlots();
      }

      ItemStack clean = original.clone();
      ItemMeta meta = clean.getItemMeta();
      if (meta != null) {
         meta.getPersistentDataContainer().remove(this.uniqueKey);
         clean.setItemMeta(meta);
      }

      return clean;
   }

   public boolean hasTransformedItem(Player player, UUID instanceId) {
      if (this.isInFlight(instanceId)) {
         return true;
      } else if (player != null && player.isOnline()) {
         Inventory inv = player.getInventory();

         for (int i = 0; i < inv.getSize(); i++) {
            if (this.hasTargetId(inv.getItem(i), instanceId)) {
               return true;
            }
         }

         if (this.hasTargetId(player.getInventory().getItemInOffHand(), instanceId)) {
            return true;
         } else if (this.hasTargetId(player.getItemOnCursor(), instanceId)) {
            return true;
         } else {
            if (player.getOpenInventory() != null) {
               Inventory top = player.getOpenInventory().getTopInventory();
               if (top != null && (top.getHolder() instanceof Player || top.getType() == InventoryType.CRAFTING)) {
                  for (int ix = 0; ix < top.getSize(); ix++) {
                     if (this.hasTargetId(top.getItem(ix), instanceId)) {
                        return true;
                     }
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public void endTransformationAndApplyCooldown(Player player, UUID instanceId) {
      UUID pUuid = null;
      if (instanceId != null) {
         this.inFlightInstances.remove(instanceId);
         this.activeTransformations.remove(instanceId);
         BukkitTask task = this.activeTasks.remove(instanceId);
         if (task != null) {
            task.cancel();
         }

         pUuid = this.instanceToPlayer.remove(instanceId);
         if (pUuid != null) {
            this.playerToInstance.remove(pUuid);
         }
      }

      if (player == null && pUuid != null) {
         player = Bukkit.getPlayer(pUuid);
      }

      if (player != null && player.isOnline()) {
         this.playerToInstance.remove(player.getUniqueId());
         player.getWorld().playSound(player.getLocation(),AltarTuning.sound("crazyslots","visuals.sounds.entity-enderman-teleport.sound",Sound.ENTITY_ENDERMAN_TELEPORT),AltarTuning.volume("crazyslots","visuals.sounds.entity-enderman-teleport",1.0F),AltarTuning.pitch("crazyslots","visuals.sounds.entity-enderman-teleport",1.2F));
         player.getWorld().playSound(player.getLocation(),AltarTuning.sound("crazyslots","visuals.sounds.item-armor-equip-generic.sound",Sound.ITEM_ARMOR_EQUIP_GENERIC),AltarTuning.volume("crazyslots","visuals.sounds.item-armor-equip-generic",1.0F),AltarTuning.pitch("crazyslots","visuals.sounds.item-armor-equip-generic",1.0F));
         player.updateInventory();
         int cooldownSec = this.plugin.getWeaponsConfig().getInt("crazy-slots.cooldown", 60);
         this.setCooldown(player, cooldownSec);
      }
   }

   public boolean isOnCooldown(Player player) {
      Long exp = this.cooldowns.get(player.getUniqueId());
      return exp != null && exp > System.currentTimeMillis();
   }

   public long getCooldownRemainingSeconds(Player player) {
      Long exp = this.cooldowns.get(player.getUniqueId());
      if (exp == null) {
         return 0L;
      } else {
         long rem = exp - System.currentTimeMillis();
         return rem > 0L ? (rem + 999L) / 1000L : 0L;
      }
   }

   public void setCooldown(final Player player, final int seconds) {
      long expireTime = System.currentTimeMillis() + (long)seconds * 1000L;
      this.cooldowns.put(player.getUniqueId(), expireTime);
      BossBar old = this.activeBars.remove(player.getUniqueId());
      if (old != null) {
         old.removeAll();
      }

      String title = ChatColor.YELLOW + AltarTuning.text("crazyslots","messages.crazy-slots-manager.message-1","бᴇзумныᴇ ᴄлоᴛы","ᴄʀᴀᴢʏ sʟᴏᴛs");
      final BossBar bar = dev.vaileks.runesaltarv.integration.AltarCooldowns.cooldownBar(title, BarColor.YELLOW, BarStyle.SOLID);
      bar.addPlayer(player);
      bar.setProgress(1.0);
      this.activeBars.put(player.getUniqueId(), bar);
      (new BukkitRunnable() {
            int ticksLeft = seconds * 20;
            final int totalTicks = seconds * 20;

            @Override
            public void run() {
               if (player.isOnline()
                  && this.ticksLeft > 0
                  && CrazySlotsManager.this.activeBars.containsKey(player.getUniqueId())
                  && CrazySlotsManager.this.activeBars.get(player.getUniqueId()).equals(bar)) {
                  bar.setProgress(Math.max(0.0, Math.min(1.0, (double)this.ticksLeft / (double)this.totalTicks)));
                  this.ticksLeft--;
               } else {
                  bar.removeAll();
                  CrazySlotsManager.this.activeBars.remove(player.getUniqueId());
                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 1L);
   }

   public void startTransformation(final Player player, ItemStack originalItem) {
      if (!this.isOnCooldown(player)) {
         if (!this.playerToInstance.containsKey(player.getUniqueId())) {
            if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().isLocationAllowed(player.getLocation(), player)) {
               this.plugin.getWorldGuardManager().notifyDeniedChat(player);
            } else {
               Location loc = player.getLocation();
               World world = player.getWorld();
               world.playSound(loc,AltarTuning.sound("crazyslots","visuals.sounds.block-note-block-pling.sound",Sound.BLOCK_NOTE_BLOCK_PLING),AltarTuning.volume("crazyslots","visuals.sounds.block-note-block-pling",1.0F),AltarTuning.pitch("crazyslots","visuals.sounds.block-note-block-pling",0.8F));
               world.playSound(loc,AltarTuning.sound("crazyslots","visuals.sounds.ui-button-click.sound",Sound.UI_BUTTON_CLICK),AltarTuning.volume("crazyslots","visuals.sounds.ui-button-click",1.0F),AltarTuning.pitch("crazyslots","visuals.sounds.ui-button-click",1.2F));
               world.spawnParticle(Particle.PORTAL, loc.clone().add(0.0, 1.0, 0.0),AltarTuning.particles("crazyslots",30), 0.4, 0.4, 0.4, 0.2);
               ItemStack rolledWeapon = this.rollRandomWeapon();
               if (rolledWeapon == null || rolledWeapon.getType().isAir()) {
                  rolledWeapon = WeaponFactory.getBloodLust(0);
               }

               final UUID instanceId = UUID.randomUUID();
               ItemMeta meta = rolledWeapon.getItemMeta();
               if (meta != null) {
                  meta.getPersistentDataContainer().set(this.uniqueKey, PersistentDataType.STRING, instanceId.toString());
                  rolledWeapon.setItemMeta(meta);
               }

               this.activeTransformations.put(instanceId, originalItem.clone());
               this.playerToInstance.put(player.getUniqueId(), instanceId);
               this.instanceToPlayer.put(instanceId, player.getUniqueId());
               player.getInventory().setItemInMainHand(rolledWeapon);
               player.updateInventory();
               world.playSound(loc,AltarTuning.sound("crazyslots","visuals.sounds.entity-player-levelup.sound",Sound.ENTITY_PLAYER_LEVELUP),AltarTuning.volume("crazyslots","visuals.sounds.entity-player-levelup",1.0F),AltarTuning.pitch("crazyslots","visuals.sounds.entity-player-levelup",1.3F));
               world.playSound(loc,AltarTuning.sound("crazyslots","visuals.sounds.entity-enderman-teleport.sound",Sound.ENTITY_ENDERMAN_TELEPORT),AltarTuning.volume("crazyslots","visuals.sounds.entity-enderman-teleport",1.0F),AltarTuning.pitch("crazyslots","visuals.sounds.entity-enderman-teleport",1.0F));
               world.spawnParticle(Particle.FIREWORK, loc.clone().add(0.0, 1.2, 0.0),AltarTuning.particles("crazyslots",20), 0.3, 0.4, 0.3, 0.05);
               final int duration = this.plugin.getWeaponsConfig().getInt("crazy-slots.transform-duration", 30);
               BukkitTask task = (new BukkitRunnable() {
                  int elapsed = 0;
                  final int maxDurationTicks = duration * 20;

                  @Override
                  public void run() {
                     if (!CrazySlotsManager.this.activeTransformations.containsKey(instanceId)) {
                        this.cancel();
                     } else if (player.isOnline()) {
                        if (!player.isDead()) {
                           this.elapsed += 10;
                           boolean hasItem = CrazySlotsManager.this.hasTransformedItem(player, instanceId);
                           if (this.elapsed >= this.maxDurationTicks) {
                              this.cancel();
                              CrazySlotsManager.this.revertItem(instanceId);
                           } else {
                              if (!hasItem) {
                                 this.cancel();
                                 CrazySlotsManager.this.revertItem(instanceId, true);
                              }
                           }
                        }
                     }
                  }
               }).runTaskTimer(this.plugin, 10L, 10L);
               this.activeTasks.put(instanceId, task);
            }
         }
      }
   }

   private ItemStack rollRandomWeapon() {
      List<String> poolConfig = this.plugin.getWeaponsConfig().getStringList("crazy-slots.weapons-pool");
      List<ItemStack> pool = new ArrayList<>();
      if (poolConfig != null && !poolConfig.isEmpty()) {
         for (String id : poolConfig) {
            ItemStack item = this.resolveWeaponById(id.trim().toLowerCase());
            if (item != null) {
               pool.add(item);
            }
         }
      }

      if (pool.isEmpty()) {
         pool.add(WeaponFactory.getBloodLust(0));
         pool.add(WeaponFactory.getBoneBlade());
         pool.add(WeaponFactory.getCutlass());
         pool.add(WeaponFactory.getEarthGauntlet());
         pool.add(WeaponFactory.getFrostScythe());
         pool.add(WeaponFactory.getHyperion());
         pool.add(WeaponFactory.getKnightfall(0));
         pool.add(WeaponFactory.getNightpiercer());
         pool.add(WeaponFactory.getPaladinsBattleAxe());
         pool.add(WeaponFactory.getPaleGun());
         pool.add(WeaponFactory.getPureBlade());
         pool.add(WeaponFactory.getShadowBlade());
         pool.add(WeaponFactory.getVulcanCrossbow());
         pool.add(WeaponFactory.getWindWeaver());
         pool.add(WeaponFactory.getWitherBlade());
      }

      return pool.get(this.random.nextInt(pool.size()));
   }

   private ItemStack resolveWeaponById(String id) {
      int bloodlustKills = this.plugin.getWeaponsConfig().getInt("crazy-slots.bloodlust-kills", 5);
      int knightfallKills = this.plugin.getWeaponsConfig().getInt("crazy-slots.knightfall-kills", 10);

      return switch (id) {
         case "bloodlust" -> WeaponFactory.getBloodLust(bloodlustKills);
         case "boneblade", "bone_blade" -> WeaponFactory.getBoneBlade();
         case "cutlass" -> WeaponFactory.getCutlass();
         case "earth_gauntlet", "earthgauntlet" -> WeaponFactory.getEarthGauntlet();
         case "frost_scythe", "frostscythe" -> WeaponFactory.getFrostScythe();
         case "hyperion" -> WeaponFactory.getHyperion();
         case "knightfall" -> WeaponFactory.getKnightfall(knightfallKills);
         case "nightpiercer" -> WeaponFactory.getNightpiercer();
         case "paladins_battle_axe", "paladin_axe", "paladin", "battle_axe" -> WeaponFactory.getPaladinsBattleAxe();
         case "pale_gun", "palegun" -> WeaponFactory.getPaleGun();
         case "pure_blade", "pureblade" -> WeaponFactory.getPureBlade();
         case "shadow_blade", "shadowblade" -> WeaponFactory.getShadowBlade();
         case "vulcans_crossbow", "vulcan", "vulcan_crossbow" -> WeaponFactory.getVulcanCrossbow();
         case "windweaver", "wind_weaver" -> WeaponFactory.getWindWeaver();
         case "wither_blade", "witherblade" -> WeaponFactory.getWitherBlade();
         default -> null;
      };
   }

   public void revertItem(UUID instanceId) {
      this.revertItem(instanceId, true);
   }

   public void revertItem(UUID instanceId, boolean applyCooldown) {
      ItemStack original = this.activeTransformations.remove(instanceId);
      if (original != null) {
         BukkitTask task = this.activeTasks.remove(instanceId);
         if (task != null) {
            task.cancel();
         }

         UUID playerUUID = this.instanceToPlayer.remove(instanceId);
         if (playerUUID != null) {
            this.playerToInstance.remove(playerUUID);
         }

         ItemStack cleanOriginal = original.clone();
         ItemMeta meta = cleanOriginal.getItemMeta();
         if (meta != null) {
            meta.getPersistentDataContainer().remove(this.uniqueKey);
            cleanOriginal.setItemMeta(meta);
         }

         boolean restored = false;
         Player targetPlayer = null;

         for (Player p : Bukkit.getOnlinePlayers()) {
            Inventory inv = p.getInventory();

            for (int i = 0; i < inv.getSize(); i++) {
               ItemStack cur = inv.getItem(i);
               if (this.hasTargetId(cur, instanceId)) {
                  inv.setItem(i, cleanOriginal.clone());
                  restored = true;
                  targetPlayer = p;
                  break;
               }
            }

            if (restored) {
               break;
            }

            if (this.hasTargetId(p.getInventory().getItemInOffHand(), instanceId)) {
               p.getInventory().setItemInOffHand(cleanOriginal.clone());
               restored = true;
               targetPlayer = p;
               break;
            }

            if (this.hasTargetId(p.getItemOnCursor(), instanceId)) {
               p.setItemOnCursor(cleanOriginal.clone());
               restored = true;
               targetPlayer = p;
               break;
            }
         }

         if (!restored) {
            for (World w : Bukkit.getWorlds()) {
               for (Entity ent : w.getEntities()) {
                  if (ent instanceof Item itemEnt && this.hasTargetId(itemEnt.getItemStack(), instanceId)) {
                     itemEnt.setItemStack(cleanOriginal.clone());
                     restored = true;
                     break;
                  }
               }

               if (restored) {
                  break;
               }
            }
         }

         if (!restored) {
            for (World w : Bukkit.getWorlds()) {
               for (Chunk chunk : w.getLoadedChunks()) {
                  for (BlockState state : chunk.getTileEntities()) {
                     if (state instanceof Container container) {
                        Inventory inv = container.getInventory();

                        for (int ix = 0; ix < inv.getSize(); ix++) {
                           if (this.hasTargetId(inv.getItem(ix), instanceId)) {
                              inv.setItem(ix, cleanOriginal.clone());
                              restored = true;
                              break;
                           }
                        }
                     } else if (state instanceof InventoryHolder holder) {
                        Inventory inv = holder.getInventory();

                        for (int ixx = 0; ixx < inv.getSize(); ixx++) {
                           if (this.hasTargetId(inv.getItem(ixx), instanceId)) {
                              inv.setItem(ixx, cleanOriginal.clone());
                              restored = true;
                              break;
                           }
                        }
                     }

                     if (restored) {
                        break;
                     }
                  }

                  if (restored) {
                     break;
                  }
               }

               if (restored) {
                  break;
               }
            }
         }

         if (!restored) {
            for (World w : Bukkit.getWorlds()) {
               for (Entity entx : w.getEntities()) {
                  if (entx instanceof ItemFrame frame && this.hasTargetId(frame.getItem(), instanceId)) {
                     frame.setItem(cleanOriginal.clone());
                     restored = true;
                     break;
                  }
               }

               if (restored) {
                  break;
               }
            }
         }

         if (!restored && playerUUID != null) {
            Player owner = Bukkit.getPlayer(playerUUID);
            if (owner != null && owner.isOnline()) {
               Map<Integer, ItemStack> leftover = owner.getInventory().addItem(new ItemStack[]{cleanOriginal.clone()});
               if (!leftover.isEmpty()) {
                  for (ItemStack rem : leftover.values()) {
                     owner.getWorld().dropItemNaturally(owner.getLocation(), rem);
                  }
               }

               targetPlayer = owner;
               restored = true;
            }
         }

         Player playerToCooldown = targetPlayer;
         if (targetPlayer == null && playerUUID != null) {
            playerToCooldown = Bukkit.getPlayer(playerUUID);
         }

         if (playerToCooldown != null && playerToCooldown.isOnline()) {
            playerToCooldown.getWorld().playSound(playerToCooldown.getLocation(),AltarTuning.sound("crazyslots","visuals.sounds.entity-enderman-teleport.sound",Sound.ENTITY_ENDERMAN_TELEPORT),AltarTuning.volume("crazyslots","visuals.sounds.entity-enderman-teleport",1.0F),AltarTuning.pitch("crazyslots","visuals.sounds.entity-enderman-teleport",1.2F));
            playerToCooldown.getWorld().playSound(playerToCooldown.getLocation(),AltarTuning.sound("crazyslots","visuals.sounds.item-armor-equip-generic.sound",Sound.ITEM_ARMOR_EQUIP_GENERIC),AltarTuning.volume("crazyslots","visuals.sounds.item-armor-equip-generic",1.0F),AltarTuning.pitch("crazyslots","visuals.sounds.item-armor-equip-generic",1.0F));
            playerToCooldown.updateInventory();
            if (applyCooldown) {
               int cooldownSec = this.plugin.getWeaponsConfig().getInt("crazy-slots.cooldown", 60);
               this.setCooldown(playerToCooldown, cooldownSec);
            }
         }
      }
   }

   private void handlePlayerReverted(Player player) {
      player.getWorld().playSound(player.getLocation(),AltarTuning.sound("crazyslots","visuals.sounds.entity-enderman-teleport.sound",Sound.ENTITY_ENDERMAN_TELEPORT),AltarTuning.volume("crazyslots","visuals.sounds.entity-enderman-teleport",1.0F),AltarTuning.pitch("crazyslots","visuals.sounds.entity-enderman-teleport",1.2F));
      player.getWorld().playSound(player.getLocation(),AltarTuning.sound("crazyslots","visuals.sounds.item-armor-equip-generic.sound",Sound.ITEM_ARMOR_EQUIP_GENERIC),AltarTuning.volume("crazyslots","visuals.sounds.item-armor-equip-generic",1.0F),AltarTuning.pitch("crazyslots","visuals.sounds.item-armor-equip-generic",1.0F));
      player.updateInventory();
      int cooldownSec = this.plugin.getWeaponsConfig().getInt("crazy-slots.cooldown", 60);
      this.setCooldown(player, cooldownSec);
   }

   private boolean hasTargetId(ItemStack item, UUID targetId) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         } else {
            String val = meta.getPersistentDataContainer().get(this.uniqueKey, PersistentDataType.STRING);
            return val != null && val.equals(targetId.toString());
         }
      } else {
         return false;
      }
   }

   public void handleQuit(Player player) {
      UUID instanceId = this.playerToInstance.remove(player.getUniqueId());
      if (instanceId != null) {
         this.revertItem(instanceId);
      }
   }

   public void handleDeath(Player player) {
      UUID instanceId = this.playerToInstance.remove(player.getUniqueId());
      if (instanceId != null) {
         this.revertItem(instanceId);
      }
   }

   public void forceRevertAll() {
      for (UUID instanceId : new ArrayList<>(this.activeTransformations.keySet())) {
         this.revertItem(instanceId, false);
      }

      this.activeTransformations.clear();
      this.activeTasks.clear();
      this.playerToInstance.clear();
      this.instanceToPlayer.clear();
      this.clearAllBars();
   }

   public void resetPlayer(Player player) {
      UUID instanceId = this.playerToInstance.get(player.getUniqueId());
      if (instanceId != null) {
         this.revertItem(instanceId, false);
      }

      this.cooldowns.remove(player.getUniqueId());
      BossBar bar = this.activeBars.remove(player.getUniqueId());
      if (bar != null) {
         bar.removeAll();
      }
   }

   public void clearAllBars() {
      for (BossBar bar : this.activeBars.values()) {
         bar.removeAll();
      }

      this.activeBars.clear();
   }
}

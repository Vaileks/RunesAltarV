package dev.vaileks.runesaltarv.weapons.frostscythe.listeners;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import dev.vaileks.runesaltarv.weapons.frostscythe.managers.FrostAbilityManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FrostListener implements Listener {
   private final FrostAbilityManager abilityManager;
   private final RunesAltarV plugin;
   private final Set<UUID> checkingPlayers = new HashSet<>();

   public FrostListener(FrostAbilityManager abilityManager) {
      this.abilityManager = abilityManager;
      this.plugin = abilityManager.getPlugin();
   }

   private boolean isFrostScythe(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "frost_scythe");
   }

   public static void setScytheModel(ItemStack item, boolean isDrawing) {
      if (item != null && item.hasItemMeta()) {
         try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
               return;
            }

            RunesAltarV plugin = RunesAltarV.getInstance();
            String keyStr = "minecraft:netherite_sword";
            int cmd = 4;
            if (plugin != null && plugin.getModelsConfig() != null) {
               String configured = plugin.getModelsConfig().getString("weapons.frost_scythe.item-model");
               if (configured != null && !configured.isBlank()) {
                  keyStr = configured.trim();
               }

               cmd = plugin.getModelsConfig().getInt("weapons.frost_scythe.custom-model-data", 4);
            }

            if (cmd > 0) {
               meta.setCustomModelData(cmd);
            }

            if (keyStr != null && !keyStr.isBlank()) {
               NamespacedKey key = NamespacedKey.fromString(keyStr);
               if (key != null) {
                  meta.setItemModel(key);
               }
            }

            item.setItemMeta(meta);
         } catch (Throwable var7) {
         }
      }
   }

   private void morphScythe(ItemStack item, Material targetMaterial) {
      if (item.getType() == targetMaterial) {
         setScytheModel(item, false);
      } else {
         item.setType(targetMaterial);
         setScytheModel(item, false);
      }
   }

   private void forceResetHands(Player p) {
      ItemStack main = p.getInventory().getItemInMainHand();
      ItemStack off = p.getInventory().getItemInOffHand();
      boolean changed = false;
      if (this.isFrostScythe(main)) {
         setScytheModel(main, false);
         changed = true;
      }

      if (this.isFrostScythe(off)) {
         setScytheModel(off, false);
         changed = true;
      }

      if (changed && p.getGameMode() != GameMode.CREATIVE) {
         p.updateInventory();
      }
   }

   private void startCooldownCheck(final Player p) {
      if (!this.checkingPlayers.contains(p.getUniqueId())) {
         this.checkingPlayers.add(p.getUniqueId());
         (new BukkitRunnable() {
            @Override
            public void run() {
               if (!p.isOnline() || !FrostListener.this.abilityManager.isOnCooldown(p, "ScytheThrow")) {
                  if (p.isOnline()) {
                     for (int i = 0; i < p.getInventory().getSize(); i++) {
                        ItemStack item = p.getInventory().getItem(i);
                        if (FrostListener.this.isFrostScythe(item) && item.getType() == Material.NETHERITE_SWORD) {
                           FrostListener.this.morphScythe(item, Material.TRIDENT);
                        }
                     }

                     p.updateInventory();
                  }

                  FrostListener.this.checkingPlayers.remove(p.getUniqueId());
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 10L, 10L);
      }
   }

   @EventHandler
   public void onInteract(PlayerInteractEvent e) {
      final Player p = e.getPlayer();
      ItemStack item = e.getItem();
      if (item == null) {
         item = e.getHand() == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
      }

      if (this.isFrostScythe(item)) {
         if (e.getHand() != EquipmentSlot.OFF_HAND || !this.isFrostScythe(p.getInventory().getItemInMainHand())) {
            if (item.getType() != Material.TRIDENT) {
               this.morphScythe(item, Material.TRIDENT);
               p.updateInventory();
            }

            if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().canUseAbilities(p)) {
               e.setCancelled(true);
               this.plugin.getWorldGuardManager().notifyDenied(p);
            } else {
               if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                  if (p.isSneaking()) {
                     e.setCancelled(true);
                     e.setUseItemInHand(Result.DENY);
                     this.abilityManager.castCommandOfIce(p);
                     return;
                  }

                  if (this.abilityManager.isOnCooldown(p, "ScytheThrow")) {
                     e.setCancelled(true);
                     e.setUseItemInHand(Result.DENY);
                  } else if (item.getType() == Material.TRIDENT) {
                     setScytheModel(item, true);
                     p.updateInventory();
                     final EquipmentSlot hand = e.getHand();
                     (new BukkitRunnable() {
                           @Override
                           public void run() {
                              if (!p.isOnline()) {
                                 this.cancel();
                              } else if (FrostListener.this.plugin.getWorldGuardManager() != null
                                 && !FrostListener.this.plugin.getWorldGuardManager().canUseAbilities(p)) {
                                 FrostListener.this.forceResetHands(p);
                                 this.cancel();
                              } else {
                                 ItemStack currentItem = hand == EquipmentSlot.HAND
                                    ? p.getInventory().getItemInMainHand()
                                    : p.getInventory().getItemInOffHand();
                                 if (!p.isHandRaised() || !FrostListener.this.isFrostScythe(currentItem) || currentItem.getType() != Material.TRIDENT) {
                                    FrostListener.this.forceResetHands(p);
                                    this.cancel();
                                 }
                              }
                           }
                        })
                        .runTaskTimer(this.plugin, 1L, 1L);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onInteractEntity(PlayerInteractEntityEvent e) {
      Player p = e.getPlayer();
      EquipmentSlot hand = e.getHand();
      ItemStack item = hand == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
      if (this.isFrostScythe(item)) {
         if (hand != EquipmentSlot.OFF_HAND || !this.isFrostScythe(p.getInventory().getItemInMainHand())) {
            if (item.getType() != Material.TRIDENT) {
               this.morphScythe(item, Material.TRIDENT);
               p.updateInventory();
            }

            if (p.isSneaking()) {
               e.setCancelled(true);
               if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().canUseAbilities(p)) {
                  this.plugin.getWorldGuardManager().notifyDenied(p);
                  return;
               }

               this.abilityManager.castCommandOfIce(p);
            }
         }
      }
   }

   @EventHandler
   public void onSlotChange(PlayerItemHeldEvent e) {
      Player p = e.getPlayer();
      ItemStack oldItem = p.getInventory().getItem(e.getPreviousSlot());
      if (this.isFrostScythe(oldItem)) {
         setScytheModel(oldItem, false);
      }

      this.forceResetHands(p);
   }

   @EventHandler
   public void onHandSwap(PlayerSwapHandItemsEvent e) {
      Player p = e.getPlayer();
      ItemStack main = e.getMainHandItem();
      ItemStack off = e.getOffHandItem();
      boolean isMain = this.isFrostScythe(main);
      boolean isOff = this.isFrostScythe(off);
      if (isMain || isOff) {
         if (isMain) {
            setScytheModel(main, false);
         }

         if (isOff) {
            setScytheModel(off, false);
         }

         if (p.isSneaking()) {
            e.setCancelled(true);
            if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().canUseAbilities(p)) {
               this.plugin.getWorldGuardManager().notifyDenied(p);
            } else {
               this.abilityManager.castCommandOfIce(p);
               p.updateInventory();
            }
         } else {
            p.updateInventory();
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onLaunch(ProjectileLaunchEvent e) {
      if (e.getEntity() instanceof Trident trident && trident.getShooter() instanceof Player p) {
         ItemStack main = p.getInventory().getItemInMainHand();
         ItemStack off = p.getInventory().getItemInOffHand();
         EquipmentSlot hand = null;
         ItemStack item = null;
         if (this.isFrostScythe(main) && main.getType() == Material.TRIDENT) {
            item = main;
            hand = EquipmentSlot.HAND;
         } else if (this.isFrostScythe(off) && off.getType() == Material.TRIDENT) {
            item = off;
            hand = EquipmentSlot.OFF_HAND;
         }

         if (item != null) {
            e.setCancelled(true);
            if (this.plugin.getWorldGuardManager() != null && !this.plugin.getWorldGuardManager().canUseAbilities(p)) {
               this.plugin.getWorldGuardManager().notifyDenied(p);
               p.updateInventory();
               return;
            }

            p.updateInventory();
            if (!this.abilityManager.isOnCooldown(p, "ScytheThrow")) {
               this.abilityManager.executeScytheThrow(p, item, hand);
               this.startCooldownCheck(p);
            }
         }
      }
   }

   @EventHandler
   public void onHit(EntityDamageByEntityEvent e) {
      if (e.getCause() == DamageCause.ENTITY_ATTACK || e.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
         if (e.getDamager() instanceof Player p) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (this.isFrostScythe(item) && e.getEntity() instanceof LivingEntity victim) {
               if (victim instanceof Player targetPlayer && targetPlayer.getGameMode() == GameMode.SPECTATOR) {
                  return;
               }

               Vector dir = p.getLocation().getDirection();
               Location sweepLoc = victim.getLocation().clone().add(0.0, 1.2, 0.0).subtract(dir.clone().normalize().multiply(1.2));
               this.abilityManager.playSweepEffect(sweepLoc, dir);
            }
         }
      }
   }

   @EventHandler
   public void onDrop(PlayerDropItemEvent e) {
      ItemStack item = e.getItemDrop().getItemStack();
      if (this.isFrostScythe(item)) {
         setScytheModel(item, false);
         this.forceResetHands(e.getPlayer());
      }
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent e) {
      for (ItemStack item : e.getDrops()) {
         if (this.isFrostScythe(item)) {
            setScytheModel(item, false);
         }
      }
   }

   @EventHandler
   public void onPickup(EntityPickupItemEvent e) {
      if (e.getEntity() instanceof Player p) {
         Item itemEntity = e.getItem();
         ItemStack itemStack = itemEntity.getItemStack();
         if (this.isFrostScythe(itemStack)) {
            if (itemStack.getType() != Material.TRIDENT) {
               this.morphScythe(itemStack, Material.TRIDENT);
               itemEntity.setItemStack(itemStack);
            }

            this.startCooldownCheck(p);
         }
      }
   }
}

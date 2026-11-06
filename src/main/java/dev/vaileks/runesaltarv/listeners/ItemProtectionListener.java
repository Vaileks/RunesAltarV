package dev.vaileks.runesaltarv.listeners;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemProtectionListener implements Listener {
   private final RunesAltarV plugin;
   private final Map<UUID, Long> weaponLimitTimers = new HashMap<>();

   public ItemProtectionListener(RunesAltarV plugin) {
      this.plugin = plugin;
      this.startInventoryCheckTask();
   }

   private void startInventoryCheckTask() {
      (new BukkitRunnable() {
            @Override
            public void run() {
               if (!ItemProtectionListener.this.plugin.isLimitsEnabled()) {
                  ItemProtectionListener.this.weaponLimitTimers.clear();
               } else {
                  int maxWeapons = ItemProtectionListener.this.plugin.getConfig().getInt("limits.max-legendary-weapons", 4);

                  for (Player player : Bukkit.getOnlinePlayers()) {
                     PlayerInventory inventory = player.getInventory();
                     UUID uuid = player.getUniqueId();
                     int weaponCount = ItemProtectionListener.this.countLegendaryWeapons(inventory);
                     if (weaponCount > maxWeapons) {
                        if (!ItemProtectionListener.this.weaponLimitTimers.containsKey(uuid)) {
                           ItemProtectionListener.this.weaponLimitTimers.put(uuid, System.currentTimeMillis() + 30000L);
                           player.sendMessage(
                              TextUtils.legacy(
                                 "§c§l[!] §f"
                                    + ItemProtectionListener.this.plugin
                                       .tr(
                                          "Внимание! У вас слишком много легендарного оружия (" + weaponCount + "/" + maxWeapons + ").",
                                          "Warning! You have too many legendary weapons (" + weaponCount + "/" + maxWeapons + ")."
                                       )
                              )
                           );
                           player.sendMessage(
                              TextUtils.legacy(
                                 "§c"
                                    + ItemProtectionListener.this.plugin
                                       .tr(
                                          "У вас есть 30 секунд, чтобы убрать лишнее, иначе оно будет выброшено!",
                                          "You have 30 seconds to remove the extra items, or they will be dropped!"
                                       )
                              )
                           );
                           player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
                        } else if (System.currentTimeMillis() >= ItemProtectionListener.this.weaponLimitTimers.get(uuid)) {
                           int kept = 0;

                           for (int ix = 0; ix < inventory.getSize(); ix++) {
                              ItemStack item = inventory.getItem(ix);
                              if (ItemProtectionListener.this.isLegendaryWeapon(item)) {
                                 if (++kept > maxWeapons) {
                                    inventory.setItem(ix, null);
                                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                                 }
                              }
                           }

                           player.sendMessage(
                              TextUtils.legacy(
                                 "§4§l[!] §c"
                                    + ItemProtectionListener.this.plugin
                                       .tr(
                                          "Время вышло! Лишнее легендарное оружие было выброшено из инвентаря.",
                                          "Time is up! Extra legendary weapons were dropped from your inventory."
                                       )
                              )
                           );
                           player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.5F);
                           ItemProtectionListener.this.weaponLimitTimers.remove(uuid);
                        }
                     } else if (ItemProtectionListener.this.weaponLimitTimers.containsKey(uuid)) {
                        ItemProtectionListener.this.weaponLimitTimers.remove(uuid);
                        player.sendMessage(
                           TextUtils.legacy(
                              "§a§l[!] §f"
                                 + ItemProtectionListener.this.plugin
                                    .tr(
                                       "Количество легендарного оружия снова в норме. Спасибо!",
                                       "Your legendary weapon count is back within the limit. Thank you!"
                                    )
                           )
                        );
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                     }
                  }
               }
            }
         })
         .runTaskTimer(this.plugin, 0L, 10L);
   }

   @EventHandler
   public void onItemDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Item droppedItem) {
         ItemStack var5 = droppedItem.getItemStack();
         if (this.isLegendary(var5)) {
            DamageCause cause = event.getCause();
            if (cause == DamageCause.FIRE
               || cause == DamageCause.FIRE_TICK
               || cause == DamageCause.LAVA
               || cause == DamageCause.ENTITY_EXPLOSION
               || cause == DamageCause.BLOCK_EXPLOSION
               || cause == DamageCause.CONTACT
               || cause == DamageCause.LIGHTNING) {
               event.setCancelled(true);
               droppedItem.setFireTicks(0);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onEnderChestClick(InventoryClickEvent event) {
      if (event.getView() != null) {
         if (event.getWhoClicked() instanceof Player player) {
            Inventory var9 = event.getView().getTopInventory();
            Inventory clickedInv = event.getClickedInventory();
            ItemStack curItem = event.getCurrentItem();
            ItemStack cursor = event.getCursor();
            if (this.isBundle(curItem) && this.isLegendary(cursor)) {
               event.setCancelled(true);
               player.sendMessage(
                  TextUtils.legacy(
                     "§c§l[!] §f" + this.plugin.tr("Легендарные предметы нельзя помещать в мешочек!", "Legendary items cannot be put into a bundle!")
                  )
               );
               player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            } else if (this.isBundle(cursor) && this.isLegendary(curItem)) {
               event.setCancelled(true);
               player.sendMessage(
                  TextUtils.legacy(
                     "§c§l[!] §f" + this.plugin.tr("Легендарные предметы нельзя помещать в мешочек!", "Legendary items cannot be put into a bundle!")
                  )
               );
               player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            } else {
               if (var9.getType() == InventoryType.ENDER_CHEST) {
                  if (event.isShiftClick() && this.isLegendary(curItem)) {
                     event.setCancelled(true);
                     player.sendMessage(
                        TextUtils.legacy(
                           "§c§l[!] §f"
                              + this.plugin.tr("Легендарные предметы нельзя помещать в Эндер-сундук!", "Legendary items cannot be put into an Ender Chest!")
                        )
                     );
                     player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                     return;
                  }

                  if (clickedInv != null && clickedInv.equals(var9)) {
                     if (this.isLegendary(cursor)) {
                        event.setCancelled(true);
                        player.sendMessage(
                           TextUtils.legacy(
                              "§c§l[!] §f"
                                 + this.plugin.tr("Легендарные предметы нельзя помещать в Эндер-сундук!", "Legendary items cannot be put into an Ender Chest!")
                           )
                        );
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                        return;
                     }

                     if (event.getClick() == ClickType.NUMBER_KEY) {
                        int hotbarSlot = event.getHotbarButton();
                        if (hotbarSlot >= 0 && hotbarSlot < 9) {
                           ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                           if (this.isLegendary(hotbarItem)) {
                              event.setCancelled(true);
                              player.sendMessage(
                                 TextUtils.legacy(
                                    "§c§l[!] §f"
                                       + this.plugin
                                          .tr("Легендарные предметы нельзя помещать в Эндер-сундук!", "Legendary items cannot be put into an Ender Chest!")
                                 )
                              );
                              player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                              return;
                           }
                        }
                     }

                     if (event.getClick() == ClickType.SWAP_OFFHAND) {
                        ItemStack offhand = player.getInventory().getItemInOffHand();
                        if (this.isLegendary(offhand)) {
                           event.setCancelled(true);
                           player.sendMessage(
                              TextUtils.legacy(
                                 "§c§l[!] §f"
                                    + this.plugin
                                       .tr("Легендарные предметы нельзя помещать в Эндер-сундук!", "Legendary items cannot be put into an Ender Chest!")
                              )
                           );
                           player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onEnderChestDrag(InventoryDragEvent event) {
      if (event.getView() != null) {
         Inventory topInv = event.getView().getTopInventory();
         if (topInv.getType() == InventoryType.ENDER_CHEST) {
            ItemStack oldCursor = event.getOldCursor();
            if (this.isLegendary(oldCursor)) {
               int topSize = topInv.getSize();

               for (int rawSlot : event.getRawSlots()) {
                  if (rawSlot < topSize) {
                     event.setCancelled(true);
                     if (event.getWhoClicked() instanceof Player player) {
                        player.sendMessage(
                           TextUtils.legacy(
                              "§c§l[!] §f"
                                 + this.plugin.tr("Легендарные предметы нельзя помещать в Эндер-сундук!", "Legendary items cannot be put into an Ender Chest!")
                           )
                        );
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                     }

                     return;
                  }
               }
            }
         }
      }
   }

   private boolean isBundle(ItemStack item) {
      return item == null ? false : item.getType().name().endsWith("BUNDLE");
   }

   private int countLegendaryWeapons(PlayerInventory inventory) {
      int count = 0;

      for (ItemStack item : inventory.getContents()) {
         if (this.isLegendaryWeapon(item)) {
            count++;
         }
      }

      return count;
   }



   private boolean isLegendaryWeapon(ItemStack item) {
      return WeaponFactory.isAnyAltarWeapon(item)
         ? true
         : this.plugin.getCrazySlotsManager() != null && this.plugin.getCrazySlotsManager().isTransformedItem(item);
   }

   private boolean isLegendary(ItemStack item) { return isLegendaryWeapon(item); }
}

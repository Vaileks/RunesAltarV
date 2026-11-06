package dev.vaileks.runesaltarv.weapons.crazyslots.listeners;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.weapons.crazyslots.managers.CrazySlotsManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CrazySlotsListener implements Listener {
   private final RunesAltarV plugin;
   private final CrazySlotsManager manager;

   public CrazySlotsListener(RunesAltarV plugin, CrazySlotsManager manager) {
      this.plugin = plugin;
      this.manager = manager;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onInteract(PlayerInteractEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         Action action = event.getAction();
         if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            if (this.manager.isCrazySlots(item)) {
               event.setCancelled(true);
               this.manager.startTransformation(player, item);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.manager.handleQuit(event.getPlayer());
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player player = event.getPlayer();
      UUID instanceId = this.manager.getInstanceIdForPlayer(player.getUniqueId());
      List<ItemStack> drops = event.getDrops();

      for (int i = 0; i < drops.size(); i++) {
         ItemStack drop = drops.get(i);
         if (drop != null && this.manager.isTransformedItem(drop)) {
            UUID id = this.manager.getTransformedInstanceId(drop);
            if (id != null) {
               drops.set(i, this.manager.getCleanCrazySlots(id));
               if (instanceId == null) {
                  instanceId = id;
               }
            }
         }
      }

      Inventory inv = player.getInventory();

      for (int ix = 0; ix < inv.getSize(); ix++) {
         ItemStack cur = inv.getItem(ix);
         if (cur != null && this.manager.isTransformedItem(cur)) {
            UUID id = this.manager.getTransformedInstanceId(cur);
            if (id != null) {
               inv.setItem(ix, this.manager.getCleanCrazySlots(id));
               if (instanceId == null) {
                  instanceId = id;
               }
            }
         }
      }

      ItemStack offhand = player.getInventory().getItemInOffHand();
      if (offhand != null && this.manager.isTransformedItem(offhand)) {
         UUID id = this.manager.getTransformedInstanceId(offhand);
         if (id != null) {
            player.getInventory().setItemInOffHand(this.manager.getCleanCrazySlots(id));
            if (instanceId == null) {
               instanceId = id;
            }
         }
      }

      if (instanceId != null) {
         this.manager.endTransformationAndApplyCooldown(player, instanceId);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onDropItem(PlayerDropItemEvent event) {
      ItemStack item = event.getItemDrop().getItemStack();
      if (this.manager.isTransformedItem(item)) {
         UUID instanceId = this.manager.getTransformedInstanceId(item);
         if (instanceId != null) {
            ItemStack cleanCrazySlots = this.manager.getCleanCrazySlots(instanceId);
            event.getItemDrop().setItemStack(cleanCrazySlots);
            Player player = event.getPlayer();
            this.manager.endTransformationAndApplyCooldown(player, instanceId);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getView() != null) {
         if (event.getWhoClicked() instanceof Player player) {
            Inventory var16 = event.getView().getTopInventory();
            Inventory bottomInv = event.getView().getBottomInventory();
            Inventory clickedInv = event.getClickedInventory();
            boolean isTopContainer = this.isExternalContainer(var16);
            ItemStack curItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            if (this.isBundle(curItem) && this.manager.isTransformedItem(cursorItem)) {
               event.setCancelled(true);
            } else if (this.isBundle(cursorItem) && this.manager.isTransformedItem(curItem)) {
               event.setCancelled(true);
            } else {
               if (this.isModificationStation(var16)) {
                  if (clickedInv != null && clickedInv.equals(var16)) {
                     ItemStack cursor = event.getCursor();
                     if (cursor != null && this.manager.isTransformedItem(cursor)) {
                        event.setCancelled(true);
                        return;
                     }

                     if (event.getClick() == ClickType.NUMBER_KEY) {
                        int hb = event.getHotbarButton();
                        if (hb >= 0 && hb < 9) {
                           ItemStack hbItem = player.getInventory().getItem(hb);
                           if (hbItem != null && this.manager.isTransformedItem(hbItem)) {
                              event.setCancelled(true);
                              return;
                           }
                        }
                     }
                  }

                  if (clickedInv != null && clickedInv.equals(bottomInv) && event.isShiftClick()) {
                     ItemStack cur = event.getCurrentItem();
                     if (cur != null && this.manager.isTransformedItem(cur)) {
                        event.setCancelled(true);
                        return;
                     }
                  }
               }

               if (isTopContainer && clickedInv != null && clickedInv.equals(var16)) {
                  ItemStack cursorx = event.getCursor();
                  if (cursorx != null && this.manager.isTransformedItem(cursorx)) {
                     UUID instanceId = this.manager.getTransformedInstanceId(cursorx);
                     if (instanceId != null) {
                        event.setCancelled(true);
                        ItemStack cleanCrazySlots = this.manager.getCleanCrazySlots(instanceId);
                        int slot = event.getSlot();
                        ItemStack currentInSlot = var16.getItem(slot);
                        var16.setItem(slot, cleanCrazySlots);
                        if (currentInSlot != null && !currentInSlot.getType().isAir()) {
                           player.setItemOnCursor(currentInSlot);
                        } else {
                           player.setItemOnCursor(null);
                        }

                        player.updateInventory();
                        this.manager.endTransformationAndApplyCooldown(player, instanceId);
                        return;
                     }
                  }

                  if (event.getClick() == ClickType.NUMBER_KEY) {
                     int hotbarButton = event.getHotbarButton();
                     if (hotbarButton >= 0 && hotbarButton < 9) {
                        ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                        if (hotbarItem != null && this.manager.isTransformedItem(hotbarItem)) {
                           UUID instanceId = this.manager.getTransformedInstanceId(hotbarItem);
                           if (instanceId != null) {
                              event.setCancelled(true);
                              ItemStack cleanCrazySlots = this.manager.getCleanCrazySlots(instanceId);
                              int slot = event.getSlot();
                              ItemStack currentInSlot = var16.getItem(slot);
                              var16.setItem(slot, cleanCrazySlots);
                              player.getInventory().setItem(hotbarButton, currentInSlot);
                              player.updateInventory();
                              this.manager.endTransformationAndApplyCooldown(player, instanceId);
                              return;
                           }
                        }
                     }
                  }

                  if (event.getClick() == ClickType.SWAP_OFFHAND) {
                     ItemStack offhandItem = player.getInventory().getItemInOffHand();
                     if (offhandItem != null && this.manager.isTransformedItem(offhandItem)) {
                        UUID instanceId = this.manager.getTransformedInstanceId(offhandItem);
                        if (instanceId != null) {
                           event.setCancelled(true);
                           ItemStack cleanCrazySlots = this.manager.getCleanCrazySlots(instanceId);
                           int slot = event.getSlot();
                           ItemStack currentInSlot = var16.getItem(slot);
                           var16.setItem(slot, cleanCrazySlots);
                           player.getInventory().setItemInOffHand(currentInSlot);
                           player.updateInventory();
                           this.manager.endTransformationAndApplyCooldown(player, instanceId);
                           return;
                        }
                     }
                  }
               }

               if (isTopContainer && clickedInv != null && clickedInv.equals(bottomInv) && event.isShiftClick()) {
                  ItemStack clickedItem = event.getCurrentItem();
                  if (clickedItem != null && this.manager.isTransformedItem(clickedItem)) {
                     UUID instanceId = this.manager.getTransformedInstanceId(clickedItem);
                     if (instanceId != null) {
                        event.setCancelled(true);
                        ItemStack cleanCrazySlots = this.manager.getCleanCrazySlots(instanceId);
                        Map<Integer, ItemStack> leftover = var16.addItem(cleanCrazySlots);
                        if (leftover.isEmpty()) {
                           event.setCurrentItem(null);
                           player.updateInventory();
                           this.manager.endTransformationAndApplyCooldown(player, instanceId);
                        } else {
                           player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
                        }

                        return;
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
   public void onInventoryDrag(InventoryDragEvent event) {
      if (event.getView() != null) {
         if (event.getWhoClicked() instanceof Player player) {
            Inventory var10 = event.getView().getTopInventory();
            if (this.isExternalContainer(var10)) {
               ItemStack oldCursor = event.getOldCursor();
               if (oldCursor != null && this.manager.isTransformedItem(oldCursor)) {
                  UUID instanceId = this.manager.getTransformedInstanceId(oldCursor);
                  if (instanceId != null) {
                     int topSize = var10.getSize();
                     int targetSlot = -1;

                     for (int rawSlot : event.getRawSlots()) {
                        if (rawSlot < topSize) {
                           targetSlot = rawSlot;
                           break;
                        }
                     }

                     if (targetSlot >= 0) {
                        event.setCancelled(true);
                        ItemStack cleanCrazySlots = this.manager.getCleanCrazySlots(instanceId);
                        var10.setItem(targetSlot, cleanCrazySlots);
                        player.setItemOnCursor(null);
                        player.updateInventory();
                        this.manager.endTransformationAndApplyCooldown(player, instanceId);
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
   public void onInventoryClose(InventoryCloseEvent event) {
      if (event.getView() != null) {
         Inventory topInv = event.getView().getTopInventory();
         if (this.isExternalContainer(topInv)) {
            Player player = event.getPlayer() instanceof Player p ? p : null;

            for (int i = 0; i < topInv.getSize(); i++) {
               ItemStack item = topInv.getItem(i);
               if (item != null && this.manager.isTransformedItem(item)) {
                  UUID instanceId = this.manager.getTransformedInstanceId(item);
                  if (instanceId != null) {
                     topInv.setItem(i, this.manager.getCleanCrazySlots(instanceId));
                     this.manager.endTransformationAndApplyCooldown(player, instanceId);
                  }
               }
            }
         }
      }
   }

   private boolean isExternalContainer(Inventory inventory) {
      if (inventory == null) {
         return false;
      } else {
         InventoryHolder holder = inventory.getHolder();
         if (holder instanceof Player) {
            return false;
         } else {
            InventoryType type = inventory.getType();

            return switch (type) {
               case CHEST, ENDER_CHEST, BARREL, SHULKER_BOX, DISPENSER, DROPPER, HOPPER, FURNACE, BLAST_FURNACE, SMOKER, BREWING, CHISELED_BOOKSHELF, JUKEBOX, CRAFTER -> true;
               default -> holder instanceof Container || holder instanceof DoubleChest || holder instanceof Entity;
            };
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onInteractEntity(PlayerInteractEntityEvent event) {
      if (event.getRightClicked() instanceof ItemFrame frame) {
         Player player = event.getPlayer();
         ItemStack item = player.getInventory().getItem(event.getHand());
         if (item != null && this.manager.isTransformedItem(item)) {
            UUID instanceId = this.manager.getTransformedInstanceId(item);
            if (instanceId != null) {
               event.setCancelled(true);
               if (frame.getItem().getType().isAir()) {
                  ItemStack cleanCrazySlots = this.manager.getCleanCrazySlots(instanceId);
                  frame.setItem(cleanCrazySlots);
                  player.getInventory().setItem(event.getHand(), null);
                  player.updateInventory();
                  this.manager.endTransformationAndApplyCooldown(player, instanceId);
               }
            }
         }
      }
   }

   private boolean isBundle(ItemStack item) {
      return item == null ? false : item.getType().name().endsWith("BUNDLE");
   }

   private boolean isModificationStation(Inventory inventory) {
      if (inventory == null) {
         return false;
      } else {
         InventoryType type = inventory.getType();

         return switch (type) {
            case ANVIL, SMITHING, ENCHANTING, GRINDSTONE, MERCHANT, LOOM, CARTOGRAPHY, STONECUTTER -> true;
            default -> false;
         };
      }
   }
}

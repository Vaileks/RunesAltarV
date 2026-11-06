package dev.vaileks.runesaltarv.integrations.worldguard;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class WorldGuardAbilityGate implements Listener {
   private final RunesAltarV plugin;
   private final WorldGuardManager worldGuardManager;

   public WorldGuardAbilityGate(RunesAltarV plugin, WorldGuardManager worldGuardManager) {
      this.plugin = plugin;
      this.worldGuardManager = worldGuardManager;
   }

   private boolean isLegendaryWeapon(ItemStack item) {
      return WeaponFactory.isAnyAltarWeapon(item);
   }

   private boolean isPaladinsBattleAxe(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "paladins_battle_axe");
   }

   private void notifyDenied(Player player, ItemStack item) {
      if (this.isPaladinsBattleAxe(item)) {
         this.worldGuardManager.notifyDeniedChat(player);
      } else {
         this.worldGuardManager.notifyDenied(player);
      }
   }

   @EventHandler(
      priority = EventPriority.LOW,
      ignoreCancelled = true
   )
   public void onSwapHand(PlayerSwapHandItemsEvent event) {
      Player player = event.getPlayer();
      if (this.isLegendaryWeapon(event.getMainHandItem()) || this.isLegendaryWeapon(event.getOffHandItem())) {
         if (!this.worldGuardManager.canUseAbilities(player)) {
            event.setCancelled(true);
            ItemStack item = this.isLegendaryWeapon(event.getMainHandItem()) ? event.getMainHandItem() : event.getOffHandItem();
            this.notifyDenied(player, item);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOW,
      ignoreCancelled = true
   )
   public void onInteract(PlayerInteractEvent event) {
      Action action = event.getAction();
      if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
         ItemStack item = event.getItem();
         if (this.isLegendaryWeapon(item)) {
            Player player = event.getPlayer();
            if (!this.worldGuardManager.canUseAbilities(player)) {
               event.setCancelled(true);
               this.notifyDenied(player, item);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOW,
      ignoreCancelled = true
   )
   public void onShootBow(EntityShootBowEvent event) {
      if (event.getEntity() instanceof Player player) {
         if (this.isLegendaryWeapon(event.getBow())) {
            if (!this.worldGuardManager.canUseAbilities(player)) {
               event.setCancelled(true);
               this.worldGuardManager.notifyDenied(player);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOW,
      ignoreCancelled = true
   )
   public void onProjectileLaunch(ProjectileLaunchEvent event) {
      if (event.getEntity().getShooter() instanceof Player player && !this.worldGuardManager.canUseAbilities(player)) {
         ItemStack main = player.getInventory().getItemInMainHand();
         ItemStack off = player.getInventory().getItemInOffHand();
         if (this.isLegendaryWeapon(main) || this.isLegendaryWeapon(off)) {
            event.setCancelled(true);
            this.notifyDenied(player, this.isLegendaryWeapon(main) ? main : off);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onTemporaryDisplayTeleport(EntityTeleportEvent event) {
      if (event.getEntity() instanceof Display) {
         if (event.getTo() != null && !this.worldGuardManager.isLocationAllowed(event.getTo(), null)) {
            event.setCancelled(true);
         }
      }
   }
}

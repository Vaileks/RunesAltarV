package dev.vaileks.runesaltarv.weapons.pureblade.listeners;

import dev.vaileks.runesaltarv.utils.WeaponFactory;
import dev.vaileks.runesaltarv.weapons.pureblade.managers.PureBladeManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class PureBladeListener implements Listener {
   private final PureBladeManager abilityManager;

   public PureBladeListener(PureBladeManager abilityManager) {
      this.abilityManager = abilityManager;
   }

   private boolean isPureBlade(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "pure_blade");
   }

   @EventHandler
   public void onSwapHand(PlayerSwapHandItemsEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         ItemStack item = player.getInventory().getItemInMainHand();
         if (this.isPureBlade(item)) {
            event.setCancelled(true);
            if (player.isSneaking()) {
               this.abilityManager.castCycloneSlash(player);
            } else {
               this.abilityManager.castShadeSoul(player);
            }
         }
      }
   }

   @EventHandler
   public void onEntityHit(EntityDamageByEntityEvent event) {
      if (event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
         if (event.getDamager() instanceof Player player
            && event.getEntity() instanceof LivingEntity victim
            && this.isPureBlade(player.getInventory().getItemInMainHand())) {
            this.abilityManager.playMassiveSweep(player, victim);
         }
      }
   }
}

package dev.vaileks.runesaltarv.weapons.earthgauntlet.listeners;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.weapons.earthgauntlet.managers.EarthGauntletManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class EarthGauntletListener implements Listener {
   private final RunesAltarV plugin;
   private final EarthGauntletManager manager;
   private final NamespacedKey gauntletKey;

   public EarthGauntletListener(RunesAltarV plugin, EarthGauntletManager manager) {
      this.plugin = plugin;
      this.manager = manager;
      this.gauntletKey = new NamespacedKey("runesaltarv", "earth_gauntlet");
   }

   public boolean isEarthGauntlet(ItemStack item) {
      return item != null && item.hasItemMeta() ? item.getItemMeta().getPersistentDataContainer().has(this.gauntletKey, PersistentDataType.BYTE) : false;
   }

   @EventHandler
   public void onSwapHand(PlayerSwapHandItemsEvent e) {
      if (!e.isCancelled()) {
         Player p = e.getPlayer();
         ItemStack item = p.getInventory().getItemInMainHand();
         if (this.isEarthGauntlet(item)) {
            e.setCancelled(true);
            if (p.isSneaking()) {
               this.manager.castMudslide(p);
            } else if (this.manager.hasMuddiedTarget(p)) {
               this.manager.pullTarget(p);
            } else {
               this.manager.activateMeteorStrike(p);
            }
         }
      }
   }

   @EventHandler
   public void onEntityHit(EntityDamageByEntityEvent e) {
      if (e.getDamager() instanceof Player p) {
         if (e.getEntity() instanceof LivingEntity victim) {
            ItemStack var6 = p.getInventory().getItemInMainHand();
            if (this.isEarthGauntlet(var6) && (e.getCause() == DamageCause.ENTITY_ATTACK || e.getCause() == DamageCause.ENTITY_SWEEP_ATTACK)) {
               this.manager.playPassiveHitSound(p);
               this.manager.triggerMeteorStrikeHit(p, victim);
            }
         }
      }
   }
}

package dev.vaileks.runesaltarv.weapons.bloodlust.listeners;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import dev.vaileks.runesaltarv.weapons.bloodlust.managers.BloodAbilityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class BloodLustListener implements Listener {
   private final RunesAltarV plugin;
   private final BloodAbilityManager abilityManager;

   public BloodLustListener(RunesAltarV plugin, BloodAbilityManager abilityManager) {
      this.plugin = plugin;
      this.abilityManager = abilityManager;
   }

   public static int getKills(ItemStack item) {
      return item != null && item.hasItemMeta()
         ? item.getItemMeta()
            .getPersistentDataContainer()
            .getOrDefault(RunesAltarV.getInstance().getKillsKey(), PersistentDataType.INTEGER, Integer.valueOf(0))
         : 0;
   }

   public static boolean isBloodLust(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "bloodlust");
   }

   @EventHandler
   public void onKill(EntityDeathEvent e) {
      if (e.getEntity() instanceof Player && e.getEntity().getKiller() != null) {
         Player p = e.getEntity().getKiller();
         ItemStack item = p.getInventory().getItemInMainHand();
         if (isBloodLust(item)) {
            int kills = getKills(item) + 1;
            this.updateItemLore(item, kills);
            TextUtils.sendActionBar(p, TextUtils.legacy(AltarTuning.text("bloodlust","messages.blood-lust-listener.message-1","&cЖажда крови пополняется новыми жертвами","&cʙʟᴏᴏᴅʟᴜsᴛ feeds on new victims")));
            this.abilityManager.playKillVisual(p, e.getEntity().getLocation());
         }
      }
   }

   private void updateItemLore(ItemStack item, int kills) {
      ItemMeta meta = item.getItemMeta();
      meta.getPersistentDataContainer().set(this.plugin.getKillsKey(), PersistentDataType.INTEGER, kills);
      ItemStack dummyItem = WeaponFactory.getBloodLust(kills);
      if (dummyItem.getItemMeta() != null && dummyItem.getItemMeta().hasLore()) {
         meta.lore(dummyItem.getItemMeta().lore());
      }

      item.setItemMeta(meta);
   }

   @EventHandler
   public void onInteract(PlayerInteractEvent e) {
      Player p = e.getPlayer();
      ItemStack item = p.getInventory().getItemInMainHand();
      if (isBloodLust(item)) {
         if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && p.isSneaking()) {
            TextUtils.sendActionBar(
               p,
               TextUtils.legacy(
                  this.plugin.tr("&cЖажда крови имеет &4&l" + getKills(item) + " &cубийств", "&cʙʟᴏᴏᴅʟᴜsᴛ has &4&l" + getKills(item) + " &ckills")
               )
            );
         }
      }
   }

   @EventHandler
   public void onSwapHand(PlayerSwapHandItemsEvent e) {
      if (!e.isCancelled()) {
         Player p = e.getPlayer();
         ItemStack item = p.getInventory().getItemInMainHand();
         if (isBloodLust(item)) {
            e.setCancelled(true);
            if (p.isSneaking()) {
               if (getKills(item) >= AltarTuning.integer("bloodlust","abilities.progression.blood-trail-kills",3,0,1000)) {
                  this.abilityManager.castBloodTrail(p);
               }
            } else if (getKills(item) >= AltarTuning.integer("bloodlust","abilities.progression.blood-hook-kills",5,0,1000)) {
               this.abilityManager.castBloodHook(p);
            }
         }
      }
   }

   @EventHandler
   public void onHit(EntityDamageByEntityEvent e) {
      if (CombatUtils.isDirectMeleeHit(e)) {
         if (e.getDamager() instanceof Player p) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (isBloodLust(item)) {
               this.abilityManager.applyInfection(p, e.getEntity());
            }
         }
      }
   }

   @EventHandler
   public void onJump(PlayerMoveEvent event) {
      if (this.abilityManager.isInBloodTrail(event.getPlayer())) {
         if (event.getTo() != null) {
            if (event.getTo().getY() > event.getFrom().getY() + 0.2 && event.getPlayer().getVelocity().getY() > 0.0) {
               this.abilityManager.cancelBloodTrail(event.getPlayer(), true);
            }
         }
      }
   }

   @EventHandler
   public void onItemHeld(PlayerItemHeldEvent event) {
      Player p = event.getPlayer();
      if (this.abilityManager.isInBloodTrail(p)) {
         ItemStack newItem = p.getInventory().getItem(event.getNewSlot());
         if (!isBloodLust(newItem)) {
            this.abilityManager.cancelBloodTrail(p, true);
         }
      }
   }

   @EventHandler
   public void onDrop(PlayerDropItemEvent event) {
      Player p = event.getPlayer();
      if (this.abilityManager.isInBloodTrail(p) && isBloodLust(event.getItemDrop().getItemStack())) {
         this.abilityManager.cancelBloodTrail(p, true);
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent e) {
      Player p = e.getPlayer();
      if (this.abilityManager.isInBloodTrail(p)) {
         this.abilityManager.cancelBloodTrail(p, false);
      }
   }
}

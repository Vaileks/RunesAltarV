package dev.vaileks.runesaltarv.weapons.cutlass.listeners;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.weapons.cutlass.managers.CutlassManager;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class CutlassListener implements Listener {
   private final CutlassManager manager;

   public CutlassListener(CutlassManager manager) {
      this.manager = manager;
   }

   @EventHandler
   public void onCutlassAbility(PlayerSwapHandItemsEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         ItemStack item = player.getInventory().getItemInMainHand();
         if (this.manager.isCutlass(item)) {
            event.setCancelled(true);
            if (player.isSneaking()) {
               if (!this.manager.checkCooldown(player, "parry")) {
                  if (!this.manager.hasActiveParry(player.getUniqueId())) {
                     this.manager.startParry(player);
                  }
               }
            } else if (!this.manager.checkCooldown(player, "thousand_cuts")) {
               if (!this.manager.isDoingThousandCuts(player)) {
                  this.manager.startThousandCuts(player);
               }
            }
         }
      }
   }

   @EventHandler
   public void onDropWeapon(PlayerDropItemEvent event) {
      if (this.manager.isDoingThousandCuts(event.getPlayer())) {
         this.manager.cancelThousandCuts(event.getPlayer());
      }
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      if (this.manager.isDoingThousandCuts(event.getEntity())) {
         this.manager.cancelThousandCuts(event.getEntity());
      }
   }

   @EventHandler
   public void onSlotChange(PlayerItemHeldEvent event) {
      if (this.manager.isDoingThousandCuts(event.getPlayer())) {
         this.manager.cancelThousandCuts(event.getPlayer());
      }
   }

   @EventHandler
   public void onMeleeHit(EntityDamageByEntityEvent event) {
      if (CombatUtils.isDirectMeleeHit(event)) {
         if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
         }

         if (this.manager.isCutlass(player.getInventory().getItemInMainHand())) {
            if (!this.manager.isDoingThousandCuts(player)) {
               if (!(event.getFinalDamage() <= 0.0)) {
                  this.manager.playCutlassSwingFX(player);
                  boolean fullyCharged = player.getCooledAttackStrength(0.0F) >= 0.85F;
                  if (fullyCharged) {
                     if (ThreadLocalRandom.current().nextInt(100) < 20) {
                        Vector pull = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.55).setY(0.2);
                        target.setVelocity(pull);
                        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.8F, 1.5F);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onParryHitReceive(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player victim) {
         if (this.manager.hasActiveParry(victim.getUniqueId())) {
            event.setCancelled(true);
            event.setDamage(0.0);

            for (DamageModifier modifier : DamageModifier.values()) {
               if (event.isApplicable(modifier)) {
                  try {
                     event.setDamage(modifier, 0.0);
                  } catch (UnsupportedOperationException var9) {
                  }
               }
            }

            if (event.getCause() == DamageCause.ENTITY_ATTACK
               && event instanceof EntityDamageByEntityEvent damageEvent
               && damageEvent.getDamager() instanceof Player attacker
               && attacker.getCooledAttackStrength(0.0F) < 0.8F) {
               return;
            }

            CutlassManager.ParryData data = this.manager.getParryData(victim.getUniqueId());
            if (data != null) {
               data.lastHitTime = System.currentTimeMillis();
               data.charges--;
               victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 0.8F);
               if (event instanceof EntityDamageByEntityEvent damageEvent && damageEvent.getDamager() instanceof LivingEntity attacker) {
                  double knockbackPower = RunesAltarV.getInstance().getWeaponsConfig().getDouble("cutlass.parry.knockback-power", 0.8);
                  Vector knockback = attacker.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize().multiply(knockbackPower).setY(0.3);
                  attacker.setVelocity(knockback);
               }

               if (data.charges <= 0) {
                  this.manager.endParry(victim);
               } else {
                  data.bar.setTitle(this.manager.parryTitle(data.charges));
               }
            }
         }
      }
   }
}

package dev.vaileks.runesaltarv.weapons.palegun.listeners;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.weapons.palegun.managers.PaleGunAbilityManager;
import dev.vaileks.runesaltarv.weapons.palegun.tasks.PaleProjectileTask;
import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class PaleGunListener implements Listener {
   private final RunesAltarV plugin;
   private final PaleGunAbilityManager abilityManager;

   public PaleGunListener(RunesAltarV plugin, PaleGunAbilityManager abilityManager) {
      this.plugin = plugin;
      this.abilityManager = abilityManager;
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onInteract(PlayerInteractEvent event) {
      ItemStack item = event.getItem();
      if (item != null && this.abilityManager.isPaleGun(item)) {
         Action action = event.getAction();
         if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            if (player.hasCooldown(item)) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onShoot(EntityShootBowEvent event) {
      if (event.getEntity() instanceof Player player) {
         ItemStack bow = event.getBow();
         if (bow != null && this.abilityManager.isPaleGun(bow)) {
            if (player.hasCooldown(bow)) {
               event.setCancelled(true);
               return;
            }
            {
               int cdSec = this.plugin.getWeaponsConfig().getInt("pale-gun.shoot.cooldown", 10);
               player.setCooldown(bow, cdSec * 20);
               dev.vaileks.runesaltarv.integration.AltarCooldowns.observe(player,"pale_gun","shoot",()->player.getCooldown(bow)*50L,cdSec*1000L);
            }

            if (event.getProjectile() instanceof AbstractArrow arrow) {
               arrow.remove();
            }

            this.clearCharge(player, bow);
            this.shoot(player, bow);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onSwapHand(PlayerSwapHandItemsEvent event) {
      if (this.abilityManager.isPaleGun(event.getMainHandItem()) || this.abilityManager.isPaleGun(event.getOffHandItem())) {
         Player player = event.getPlayer();
         event.setCancelled(true);
         if (!this.abilityManager.activatePaleRoots(player)) {
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBackstab(EntityDamageByEntityEvent event) {
      if (CombatUtils.isDirectMeleeHit(event)) {
         if (event.getEntity() instanceof LivingEntity victim) {
            if (event.getDamager() instanceof Player attacker) {
               if (victim instanceof Player playerVictim && this.plugin.getFriendManager().isFriend(attacker.getUniqueId(), playerVictim.getUniqueId())) {
                  return;
               }

               if (victim.hasPotionEffect(PotionEffectType.LUCK)) {
                  Vector victimLook = victim.getEyeLocation().getDirection().setY(0);
                  Vector toAttacker = attacker.getLocation().toVector().subtract(victim.getLocation().toVector()).setY(0);
                  if (!(victimLook.lengthSquared() < 1.0E-4) && !(toAttacker.lengthSquared() < 1.0E-4)) {
                     double dot = victimLook.normalize().dot(toAttacker.normalize());
                     if (dot < -0.3) {
                        double multiplier = this.plugin.getWeaponsConfig().getDouble("pale-gun.backstab.multiplier", 1.5);
                        event.setDamage(event.getDamage() * multiplier);
                        victim.getWorld().playSound(victim.getLocation(),AltarTuning.sound("pale_gun","visuals.sounds.entity-creaking-attack.sound",Sound.ENTITY_CREAKING_ATTACK),AltarTuning.volume("pale_gun","visuals.sounds.entity-creaking-attack",0.8F),AltarTuning.pitch("pale_gun","visuals.sounds.entity-creaking-attack",1.05F));
                        victim.getWorld().spawnParticle(Particle.INFESTED, victim.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("pale_gun",20), 0.3, 0.5, 0.3, 0.01);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.abilityManager.resetPlayer(event.getPlayer());
   }

   private void shoot(Player player, ItemStack item) {
      Location eyeLoc = player.getEyeLocation();
      Vector direction = eyeLoc.getDirection().normalize();
      player.getWorld().playSound(eyeLoc,AltarTuning.sound("pale_gun","visuals.sounds.entity-generic-explode.sound",Sound.ENTITY_GENERIC_EXPLODE),AltarTuning.volume("pale_gun","visuals.sounds.entity-generic-explode",1.0F),AltarTuning.pitch("pale_gun","visuals.sounds.entity-generic-explode",2.0F));
      Vector recoil = direction.clone().multiply(-1).normalize().multiply(0.8).setY(0.25);
      player.setVelocity(recoil);
      double speed = this.plugin.getWeaponsConfig().getDouble("pale-gun.shoot.projectile-speed", 1.8);
      PaleProjectileTask projectileTask = new PaleProjectileTask(player, eyeLoc, direction.multiply(speed), this.plugin, this.abilityManager);
      this.abilityManager.trackProjectile(projectileTask);
      projectileTask.runTaskTimer(this.plugin, 0L, 1L);
   }

   private void clearCharge(Player player, ItemStack item) {
      if (item.getItemMeta() instanceof CrossbowMeta meta && meta.hasChargedProjectiles()) {
         meta.setChargedProjectiles(new ArrayList<>());
         item.setItemMeta(meta);
         player.updateInventory();
         return;
      }
   }
}

package dev.vaileks.runesaltarv.weapons.witherblade.listeners;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import dev.vaileks.runesaltarv.weapons.witherblade.managers.WitherManager;
import dev.vaileks.runesaltarv.weapons.witherblade.tasks.WitherTasks;
import java.util.Random;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class WitherBladeListener implements Listener {
   private final RunesAltarV plugin;
   private final WitherManager manager;
   private final Random random = new Random();

   public WitherBladeListener(RunesAltarV plugin, WitherManager manager) {
      this.plugin = plugin;
      this.manager = manager;
   }

   private boolean isWitherBlade(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "wither_blade");
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onSwapHand(PlayerSwapHandItemsEvent e) {
      if (!e.isCancelled()) {
         Player p = e.getPlayer();
         ItemStack item = p.getInventory().getItemInMainHand();
         if (this.isWitherBlade(item)) {
            e.setCancelled(true);
            if (p.isSneaking()) {
               if (this.manager.isOnAuraCooldown(p)) {
                  return;
               }

               WitherTasks.castAura(this.plugin, p, this.manager);
            } else {
               WitherTasks.castDash(this.plugin, p, this.manager);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onHit(EntityDamageByEntityEvent e) {
      if (e.getCause() == DamageCause.ENTITY_ATTACK || e.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
         if (e.getDamager() instanceof Player p && e.getEntity() instanceof LivingEntity victim) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (this.isWitherBlade(item)) {
               if (victim instanceof Player targetPlayer && this.plugin.getFriendManager().isFriend(p.getUniqueId(), targetPlayer.getUniqueId())) {
                  e.setCancelled(true);
                  return;
               }

               this.manager.addAttackCharge(p);
            }
         }
      }
   }

   @EventHandler
   public void onPlayerDeath(PlayerDeathEvent e) {
      Player victim = e.getEntity();
      if (victim.getKiller() != null) {
         Player killer = victim.getKiller();
         ItemStack item = killer.getInventory().getItemInMainHand();
         if (this.isWitherBlade(item)) {
            this.manager.fillMaxCharge(killer);
            Location loc = victim.getLocation().add(0.0, 1.0, 0.0);
            victim.getWorld().playSound(loc,AltarTuning.sound("wither_blade","visuals.sounds.entity-wither-skeleton-death.sound",Sound.ENTITY_WITHER_SKELETON_DEATH),AltarTuning.volume("wither_blade","visuals.sounds.entity-wither-skeleton-death",1.5F),AltarTuning.pitch("wither_blade","visuals.sounds.entity-wither-skeleton-death",0.8F));
            DustOptions blackDust = new DustOptions(Color.fromRGB(0, 0, 0), 2.5F);
            victim.getWorld().spawnParticle(Particle.DUST, loc,AltarTuning.particles("wither_blade",50), 0.5, 0.5, 0.5, 0.1, blackDust);

            for (int i = 0; i < 6; i++) {
               double angle = this.random.nextDouble() * 2.0 * Math.PI;
               double ySpread = (this.random.nextDouble() - 0.2) * 0.8;
               Vector dir = new Vector(Math.cos(angle), ySpread, Math.sin(angle)).normalize();
               WitherTasks.launchSlimeProjectile(this.plugin, killer, victim.getEyeLocation(), dir, 5.0, false);
            }
         }
      }
   }
}

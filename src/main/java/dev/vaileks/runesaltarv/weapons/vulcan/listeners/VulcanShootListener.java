package dev.vaileks.runesaltarv.weapons.vulcan.listeners;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.weapons.vulcan.managers.VulcanCooldownManager;
import dev.vaileks.runesaltarv.weapons.vulcan.tasks.ArrowParticleTask;
import dev.vaileks.runesaltarv.weapons.vulcan.tasks.MagmaProjectile;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class VulcanShootListener implements Listener {
   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onShoot(EntityShootBowEvent event) {
      if (event.getEntity() instanceof Player player) {
         ItemStack bow = event.getBow();
         if (bow != null && bow.hasItemMeta()) {
            RunesAltarV plugin = RunesAltarV.getInstance();
            if (bow.getItemMeta().getPersistentDataContainer().has(plugin.getVulcanKey(), PersistentDataType.BYTE)) {
               Location eyeLoc = player.getEyeLocation();
               Vector dir = eyeLoc.getDirection().normalize();
               if (player.isSneaking() && !VulcanCooldownManager.isOnCooldown(player)) {
                  event.setCancelled(true);
                  player.getWorld().playSound(eyeLoc,AltarTuning.sound("vulcan_crossbow","visuals.sounds.entity-warden-sonic-boom.sound",Sound.ENTITY_WARDEN_SONIC_BOOM),AltarTuning.volume("vulcan_crossbow","visuals.sounds.entity-warden-sonic-boom",2.0F),AltarTuning.pitch("vulcan_crossbow","visuals.sounds.entity-warden-sonic-boom",1.0F));
                  new MagmaProjectile(player, eyeLoc, dir.clone().multiply(1.5)).runTaskTimer(plugin, 0L, 1L);
                  Vector recoil = dir.clone().multiply(-1).normalize().multiply(0.8).setY(0.25);
                  player.setVelocity(recoil);
                  int cooldownTime = plugin.getWeaponsConfig().getInt("vulcan_crossbow.magma-attack.cooldown", 25);
                  VulcanCooldownManager.startCooldown(player, cooldownTime);
               } else if (event.getProjectile() instanceof Arrow arrow) {
                  arrow.getPersistentDataContainer().set(plugin.getArrowKey(), PersistentDataType.BYTE, (byte)1);
                  player.getWorld().playSound(eyeLoc,AltarTuning.sound("vulcan_crossbow","visuals.sounds.entity-warden-sonic-boom.sound",Sound.ENTITY_WARDEN_SONIC_BOOM),AltarTuning.volume("vulcan_crossbow","visuals.sounds.entity-warden-sonic-boom",0.6F),AltarTuning.pitch("vulcan_crossbow","visuals.sounds.entity-warden-sonic-boom",1.5F));
                  player.getWorld().playSound(eyeLoc,AltarTuning.sound("vulcan_crossbow","visuals.sounds.block-fire-extinguish.sound",Sound.BLOCK_FIRE_EXTINGUISH),AltarTuning.volume("vulcan_crossbow","visuals.sounds.block-fire-extinguish",1.0F),AltarTuning.pitch("vulcan_crossbow","visuals.sounds.block-fire-extinguish",1.0F));
                  Vector centerVec = player.getEyeLocation().getDirection().normalize().multiply(arrow.getVelocity().length());
                  Vector originalVelocity = arrow.getVelocity();
                  Vector newVec = originalVelocity.clone().multiply(0.25).add(centerVec.clone().multiply(0.75));
                  arrow.setVelocity(newVec);
                  double angle = (double)originalVelocity.normalize().angle(player.getEyeLocation().getDirection().normalize());
                  boolean isCentral = angle < 0.05;
                  if (isCentral) {
                     arrow.setFireTicks(1000);
                  }

                  new ArrowParticleTask(arrow, isCentral).runTaskTimer(plugin, 1L, 1L);
               }
            }
         }
      }
   }
}

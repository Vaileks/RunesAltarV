package dev.vaileks.runesaltarv.weapons.paladinsbattleaxe.listeners;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.weapons.paladinsbattleaxe.managers.PaladinsBattleAxeManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PaladinsBattleAxeListener implements Listener {
   private final RunesAltarV plugin;
   private final PaladinsBattleAxeManager manager;
   private final Map<UUID, Long> debounceMap = new ConcurrentHashMap<>();

   public PaladinsBattleAxeListener(RunesAltarV plugin, PaladinsBattleAxeManager manager) {
      this.plugin = plugin;
      this.manager = manager;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onSwapHand(PlayerSwapHandItemsEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         ItemStack mainItem = player.getInventory().getItemInMainHand();
         if (this.manager.isPaladinsBattleAxe(mainItem)) {
            event.setCancelled(true);
            player.updateInventory();
            if (System.currentTimeMillis() - this.debounceMap.getOrDefault(player.getUniqueId(), 0L) >= 250L) {
               this.debounceMap.put(player.getUniqueId(), System.currentTimeMillis());
               if (player.isSneaking()) {
                  this.manager.useStalwartAbsorption(player);
               } else {
                  this.manager.useEarthShatter(player);
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onMeleeHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker) {
         ItemStack var7 = attacker.getInventory().getItemInMainHand();
         if (this.manager.isPaladinsBattleAxe(var7)) {
            Vector dir = attacker.getEyeLocation().getDirection().normalize();
            Location center = attacker.getLocation().add(0.0, 1.0, 0.0).add(dir.clone().multiply(1.0));
            center.setDirection(dir);
            Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            this.spawnSlashArc(attacker.getWorld(), center, right, true, Color.fromRGB(255, 200, 50));
            attacker.getWorld().playSound(attacker.getLocation(),AltarTuning.sound("paladins_battle_axe","visuals.sounds.item-trident-throw.sound",Sound.ITEM_TRIDENT_THROW),AltarTuning.volume("paladins_battle_axe","visuals.sounds.item-trident-throw",1.0F),AltarTuning.pitch("paladins_battle_axe","visuals.sounds.item-trident-throw",0.6F));
            if (this.manager.isAbsorbing(attacker.getUniqueId())) {
               attacker.getWorld().playSound(attacker.getLocation(),AltarTuning.sound("paladins_battle_axe","visuals.sounds.block-anvil-land.sound",Sound.BLOCK_ANVIL_LAND),AltarTuning.volume("paladins_battle_axe","visuals.sounds.block-anvil-land",0.6F),AltarTuning.pitch("paladins_battle_axe","visuals.sounds.block-anvil-land",1.8F));
            }
         }
      }
   }

   private void spawnSlashArc(World world, Location center, Vector right, boolean flip, Color color) {
      int points = AltarTuning.integer("paladins_battle_axe","tuning.paladins-battle-axe-listener.points",22,1,512);
      double width = AltarTuning.number("paladins_battle_axe","tuning.paladins-battle-axe-listener.width",2.2,0.01,128);
      double height = AltarTuning.number("paladins_battle_axe","tuning.paladins-battle-axe-listener.height",1.5,0.01,128);
      Vector forward = center.getDirection();

      for (int i = 0; i < points; i++) {
         double progress = (double)i / (double)(points - 1);
         double xOffset = (progress - 0.5) * width * (flip ? 1.0 : -1.0);
         double yBase = (0.5 - progress) * height;
         double yCurve = Math.sin(progress * Math.PI) * 0.3;
         double yOffset = yBase + yCurve;
         double zOffset = Math.sin(progress * Math.PI) * 0.4;
         Location pLoc = center.clone().add(right.clone().multiply(xOffset)).add(forward.clone().multiply(zOffset)).add(0.0, yOffset, 0.0);
         float particleSize = i % 2 == 0 ? 0.6F : 0.8F;
         world.spawnParticle(Particle.DUST, pLoc,AltarTuning.particles("paladins_battle_axe",1), 0.02, 0.02, 0.02, 0.0, new DustOptions(color, particleSize));
         if (i % 2 == 0) {
            world.spawnParticle(Particle.CRIT, pLoc,AltarTuning.particles("paladins_battle_axe",1), 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOW,
      ignoreCancelled = true
   )
   public void onPlayerDamaged(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player victim) {
         if (this.manager.isAbsorbing(victim.getUniqueId())) {
            double damage = event.getDamage();
            this.manager.recordAbsorbedDamage(victim.getUniqueId(), damage);
            event.setDamage(0.0);
            victim.getWorld().playSound(victim.getLocation(),AltarTuning.sound("paladins_battle_axe","visuals.sounds.item-shield-block.sound",Sound.ITEM_SHIELD_BLOCK),AltarTuning.volume("paladins_battle_axe","visuals.sounds.item-shield-block",0.8F),AltarTuning.pitch("paladins_battle_axe","visuals.sounds.item-shield-block",1.2F));
            victim.getWorld()
               .spawnParticle(Particle.DUST, victim.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("paladins_battle_axe",10), 0.3, 0.5, 0.3, 0.0, new DustOptions(Color.YELLOW, 1.5F));
         }
      }
   }
}

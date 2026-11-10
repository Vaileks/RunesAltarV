package dev.vaileks.runesaltarv.weapons.knightfall.listeners;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import dev.vaileks.runesaltarv.weapons.knightfall.managers.KnightfallManager;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class KnightfallListener implements Listener {
   private final RunesAltarV plugin;
   private final KnightfallManager manager;
   private final NamespacedKey huntKey;
   private static final double HEALTH_POINTS_PER_HEART = 2.0;
   private static final int DAMAGE_SEARCH_ITERATIONS = 40;

   public KnightfallListener(RunesAltarV plugin, KnightfallManager manager) {
      this.plugin = plugin;
      this.manager = manager;
      this.huntKey = new NamespacedKey("ffashopwh", "hunt_item");
   }

   private boolean isKnightfall(ItemStack item) {
      return WeaponFactory.isAltarWeapon(item, "knightfall");
   }

   private int getKills(ItemStack item) {
      return item.getItemMeta().getPersistentDataContainer().getOrDefault(this.plugin.getKillsKey(), PersistentDataType.INTEGER, Integer.valueOf(0));
   }

   private ItemStack applyHuntTagIfNecessary(ItemStack oldItem, ItemStack newItem) {
      if (oldItem.hasItemMeta() && oldItem.getItemMeta().getPersistentDataContainer().has(this.huntKey, PersistentDataType.BYTE)) {
         ItemMeta newMeta = newItem.getItemMeta();
         if (newMeta != null) {
            newMeta.getPersistentDataContainer().set(this.huntKey, PersistentDataType.BYTE, (byte)1);
            newItem.setItemMeta(newMeta);
         }
      }

      return newItem;
   }

   @EventHandler
   public void onAnvil(PrepareAnvilEvent e) {
      if (this.isKnightfall(e.getInventory().getItem(0)) || this.isKnightfall(e.getInventory().getItem(1))) {
         e.setResult(null);
      }
   }

   @EventHandler
   public void onEnchant(PrepareItemEnchantEvent e) {
      if (this.isKnightfall(e.getItem())) {
         e.setCancelled(true);
      }
   }

   private int getTier(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         Integer tier = item.getItemMeta().getPersistentDataContainer().get(this.plugin.getKnightfallTierKey(), PersistentDataType.INTEGER);
         if (tier != null) {
            return tier;
         } else {
            int cmd = item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : 1;
            boolean hasDensity = item.getItemMeta().hasEnchant(Enchantment.DENSITY);
            if (cmd >= 3003 || cmd == 3) {
               return 3;
            } else if (hasDensity) {
               return 2;
            } else {
               return cmd < 3002 && cmd != 2 ? 0 : 1;
            }
         }
      } else {
         return 0;
      }
   }

   @EventHandler
   public void onKill(EntityDeathEvent e) {
      if (e.getEntity().getKiller() != null) {
         Player p = e.getEntity().getKiller();
         ItemStack item = p.getInventory().getItemInMainHand();
         if (this.isKnightfall(item) && e.getEntity() instanceof Player) {
            this.manager.playKillEffect(e.getEntity().getLocation());
            int newKills = this.getKills(item) + 1;
            int currentTier = this.getTier(item);
            int currentCmd = currentTier >= 3 ? 3 : (currentTier >= 1 ? 2 : 1);
            boolean currentDensity = currentTier >= 2;
            ItemStack newWeapon = WeaponFactory.getKnightfallCustom(newKills, currentCmd, currentDensity);
            newWeapon = this.applyHuntTagIfNecessary(item, newWeapon);
            if (this.plugin.getCrazySlotsManager() != null && this.plugin.getCrazySlotsManager().isTransformedItem(item)) {
               UUID csId = this.plugin.getCrazySlotsManager().getTransformedInstanceId(item);
               if (csId != null) {
                  ItemMeta nm = newWeapon.getItemMeta();
                  if (nm != null) {
                     nm.getPersistentDataContainer().set(this.plugin.getCrazySlotsManager().getUniqueKey(), PersistentDataType.STRING, csId.toString());
                     newWeapon.setItemMeta(nm);
                  }
               }
            }

            p.getInventory().setItemInMainHand(newWeapon);
            p.sendMessage(
               TextUtils.legacy(this.plugin.tr("§cПадший Рыцарь теперь имеет " + newKills + " убийств(а/ов).", "§cknightfall now has " + newKills + " kills."))
            );
            if (newKills == 2) {
               p.sendMessage(TextUtils.legacy(AltarTuning.text("knightfall","messages.knightfall-listener.message-1","§eПадший рыцарь теперь имеет чары Порыв Ветра I.","§eknightfall now has Wind Burst I.")));
            }

            boolean needsUpgrade = newKills >= AltarTuning.integer("knightfall","abilities.progression.tier-1-kills",4,1,1000) && currentTier < 1 || newKills >= AltarTuning.integer("knightfall","abilities.progression.tier-2-kills",8,1,1000) && currentTier < 2 || newKills >= AltarTuning.integer("knightfall","abilities.progression.tier-3-kills",10,1,1000) && currentTier < 3;
            if (needsUpgrade) {
               p.sendMessage(
                  TextUtils.legacy(AltarTuning.text("knightfall","messages.knightfall-listener.message-2","§aНажмите §2[Смена руки] §aчтобы улучшить оружие.","§aPress §2[OffHand] §ato upgrade the weapon."))
               );
            }
         }
      }
   }

   @EventHandler
   public void onSwapHand(PlayerSwapHandItemsEvent e) {
      if (!e.isCancelled()) {
         Player p = e.getPlayer();
         ItemStack item = p.getInventory().getItemInMainHand();
         if (this.isKnightfall(item)) {
            e.setCancelled(true);
            int kills = this.getKills(item);
            int currentTier = this.getTier(item);
            if (p.isSneaking()) {
               if (kills >= AltarTuning.integer("knightfall","abilities.progression.tier-3-kills",10,1,1000) && currentTier >= 3) {
                  this.manager.throwHammer(p, item);
               }
            } else {
               int targetTier = currentTier;
               if (kills >= AltarTuning.integer("knightfall","abilities.progression.tier-3-kills",10,1,1000) && currentTier < 3) {
                  targetTier = 3;
               } else if (kills >= AltarTuning.integer("knightfall","abilities.progression.tier-2-kills",8,1,1000) && currentTier < 2) {
                  targetTier = 2;
               } else if (kills >= AltarTuning.integer("knightfall","abilities.progression.tier-1-kills",4,1,1000) && currentTier < 1) {
                  targetTier = 1;
               }

               if (targetTier > currentTier) {
                  int newCmd = targetTier >= 3 ? 3 : (targetTier >= 1 ? 2 : 1);
                  boolean newDensity = targetTier >= 2;
                  ItemStack newWeapon = WeaponFactory.getKnightfallCustom(kills, newCmd, newDensity);
                  newWeapon = this.applyHuntTagIfNecessary(item, newWeapon);
                  if (this.plugin.getCrazySlotsManager() != null && this.plugin.getCrazySlotsManager().isTransformedItem(item)) {
                     UUID csId = this.plugin.getCrazySlotsManager().getTransformedInstanceId(item);
                     if (csId != null) {
                        ItemMeta nm = newWeapon.getItemMeta();
                        if (nm != null) {
                           nm.getPersistentDataContainer().set(this.plugin.getCrazySlotsManager().getUniqueKey(), PersistentDataType.STRING, csId.toString());
                           newWeapon.setItemMeta(nm);
                        }
                     }
                  }

                  p.getInventory().setItemInMainHand(newWeapon);
                  p.updateInventory();
                  p.getWorld().playSound(p.getLocation(),AltarTuning.sound("knightfall","visuals.sounds.block-enchantment-table-use.sound",Sound.BLOCK_ENCHANTMENT_TABLE_USE),AltarTuning.volume("knightfall","visuals.sounds.block-enchantment-table-use",1.0F),AltarTuning.pitch("knightfall","visuals.sounds.block-enchantment-table-use",1.0F));
                  p.getWorld().playSound(p.getLocation(),AltarTuning.sound("knightfall","visuals.sounds.block-end-portal-spawn.sound",Sound.BLOCK_END_PORTAL_SPAWN),AltarTuning.volume("knightfall","visuals.sounds.block-end-portal-spawn",1.0F),AltarTuning.pitch("knightfall","visuals.sounds.block-end-portal-spawn",0.8F));
                  p.getWorld().playSound(p.getLocation(),AltarTuning.sound("knightfall","visuals.sounds.entity-zombie-attack-iron-door.sound",Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR),AltarTuning.volume("knightfall","visuals.sounds.entity-zombie-attack-iron-door",1.0F),AltarTuning.pitch("knightfall","visuals.sounds.entity-zombie-attack-iron-door",0.0F));
                  p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation().add(0.0, 1.0, 0.0),AltarTuning.particles("knightfall",100), 0.5, 1.0, 0.5);
                  TextUtils.sendActionBar(p, TextUtils.legacy(AltarTuning.text("knightfall","messages.knightfall-listener.message-3","&6Оружие успешно улучшено.","&6Weapon upgraded successfully.")));
                  TextUtils.sendMessage(p, TextUtils.legacy(AltarTuning.text("knightfall","messages.knightfall-listener.message-4","§6Оружие успешно улучшено.","§6Weapon upgraded successfully.")));
                  if (targetTier == 1) {
                     TextUtils.sendMessage(
                        p,
                        TextUtils.legacy(
                           AltarTuning.text("knightfall","messages.knightfall-listener.message-5","§eТеперь вы можете использовать Абордажный Крюк Падшего Рыцаря.","§eYou can now use knightfall's grappling hook.")
                        )
                     );
                  }

                  if (targetTier == 2) {
                     TextUtils.sendMessage(
                        p, TextUtils.legacy(AltarTuning.text("knightfall","messages.knightfall-listener.message-6","§eПадший рыцарь теперь имеет чары Плотность II.","§eknightfall now has Density II."))
                     );
                  }

                  if (targetTier == 3) {
                     TextUtils.sendMessage(
                        p,
                        TextUtils.legacy(
                           AltarTuning.text("knightfall","messages.knightfall-listener.message-7","§eТеперь вы можете использовать Бросок Молота Падшего Рыцаря.","§eYou can now use knightfall's hammer throw.")
                        )
                     );
                  }

                  return;
               }

               if (kills >= AltarTuning.integer("knightfall","abilities.progression.tier-1-kills",4,1,1000) && currentTier >= 1) {
                  this.manager.useHook(p);
               }
            }
         }
      }
   }

   @EventHandler
   public void onSmashAttack(EntityDamageByEntityEvent e) {
      if (CombatUtils.isDirectMeleeHit(e)) {
         if (e.getDamager() instanceof Player p) {
            ItemStack var4 = p.getInventory().getItemInMainHand();
            if (this.isKnightfall(var4)
               && (double)p.getFallDistance() > 0.0
               && !p.isOnGround()
               && Math.random() <= this.plugin.getWeaponsConfig().getDouble("knightfall.cloak.chance", 0.1)
               && this.manager.canUseCloak(p)) {
               this.manager.triggerCloak(p);
            }
         }
      }
   }

   private boolean isDamageCapEnabled() {
      FileConfiguration cfg = this.plugin.getWeaponsConfig();
      if (cfg.contains("knightfall.knightfall-damage-cap.enabled")) {
         return cfg.getBoolean("knightfall.knightfall-damage-cap.enabled", true);
      } else if (cfg.contains("knightfall.damage-cap.enabled")) {
         return cfg.getBoolean("knightfall.damage-cap.enabled", true);
      } else {
         return cfg.contains("knightfall-damage-cap.enabled") ? cfg.getBoolean("knightfall-damage-cap.enabled", true) : true;
      }
   }

   private double getDamageCapHearts() {
      FileConfiguration cfg = this.plugin.getWeaponsConfig();
      if (cfg.contains("knightfall.knightfall-damage-cap.max-damage-hearts")) {
         return cfg.getDouble("knightfall.knightfall-damage-cap.max-damage-hearts", 6.0);
      } else if (cfg.contains("knightfall.knightfall-damage-cap.damage-cap")) {
         return cfg.getDouble("knightfall.knightfall-damage-cap.damage-cap", 6.0);
      } else if (cfg.contains("knightfall.damage-cap.max-damage-hearts")) {
         return cfg.getDouble("knightfall.damage-cap.max-damage-hearts", 6.0);
      } else if (cfg.contains("knightfall.damage-cap.damage-cap")) {
         return cfg.getDouble("knightfall.damage-cap.damage-cap", 6.0);
      } else if (cfg.contains("knightfall-damage-cap.max-damage-hearts")) {
         return cfg.getDouble("knightfall-damage-cap.max-damage-hearts", 6.0);
      } else {
         return cfg.contains("knightfall-damage-cap.damage-cap") ? cfg.getDouble("knightfall-damage-cap.damage-cap", 6.0) : 6.0;
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onKnightfallDamageCap(EntityDamageByEntityEvent event) {
      if (this.isDamageCapEnabled()) {
         if (event.getEntity() instanceof Player && event.getDamager() instanceof Player attacker) {
            ItemStack var8 = attacker.getInventory().getItemInMainHand();
            if (this.isKnightfall(var8)) {
               double configuredHearts = this.getDamageCapHearts();
               if (!Double.isFinite(configuredHearts) || configuredHearts < 0.0) {
                  configuredHearts = 6.0;
               }

               double maxDamage = configuredHearts * 2.0;
               if (event.getFinalDamage() > maxDamage) {
                  this.capFinalDamage(event, maxDamage);
               }
            }
         }
      }
   }

   private void capFinalDamage(EntityDamageByEntityEvent event, double limit) {
      if (limit <= 0.0) {
         event.setDamage(0.0);
      } else {
         double low = 0.0;
         double high = event.getDamage();

         for (int i = 0; i < 40; i++) {
            double candidate = (low + high) / 2.0;
            event.setDamage(candidate);
            if (event.getFinalDamage() > limit) {
               high = candidate;
            } else {
               low = candidate;
            }
         }

         event.setDamage(low);
      }
   }
}

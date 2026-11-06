package dev.vaileks.runesaltarv;

import dev.vaileks.runesaltarv.commands.AltarLegendaryCommand;
import dev.vaileks.runesaltarv.commands.FriendListCommand;
import dev.vaileks.runesaltarv.commands.FriendListTabCompleter;
import dev.vaileks.runesaltarv.integrations.worldguard.WorldGuardAbilityGate;
import dev.vaileks.runesaltarv.integrations.worldguard.WorldGuardManager;
import dev.vaileks.runesaltarv.listeners.ItemProtectionListener;
import dev.vaileks.runesaltarv.managers.FriendManager;
import dev.vaileks.runesaltarv.managers.VisualCleanupManager;
import dev.vaileks.runesaltarv.utils.CleanDamageManager;
import dev.vaileks.runesaltarv.utils.SpeedBuffUtils;
import dev.vaileks.runesaltarv.utils.TextUtils;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
import dev.vaileks.runesaltarv.weapons.bloodlust.listeners.BloodLustListener;
import dev.vaileks.runesaltarv.weapons.bloodlust.managers.BloodAbilityManager;
import dev.vaileks.runesaltarv.weapons.bloodlust.managers.BloodBossBarManager;
import dev.vaileks.runesaltarv.weapons.bloodlust.tasks.PassiveAuraTask;
import dev.vaileks.runesaltarv.weapons.boneblade.listeners.BladeListener;
import dev.vaileks.runesaltarv.weapons.boneblade.managers.BoneCooldownManager;
import dev.vaileks.runesaltarv.weapons.crazyslots.listeners.CrazySlotsListener;
import dev.vaileks.runesaltarv.weapons.crazyslots.managers.CrazySlotsManager;
import dev.vaileks.runesaltarv.weapons.cutlass.listeners.CutlassListener;
import dev.vaileks.runesaltarv.weapons.cutlass.managers.CutlassManager;
import dev.vaileks.runesaltarv.weapons.earthgauntlet.listeners.EarthGauntletListener;
import dev.vaileks.runesaltarv.weapons.earthgauntlet.managers.EarthGauntletManager;
import dev.vaileks.runesaltarv.weapons.frostscythe.listeners.FrostListener;
import dev.vaileks.runesaltarv.weapons.frostscythe.managers.FrostAbilityManager;
import dev.vaileks.runesaltarv.weapons.frostscythe.managers.FrostBossBarManager;
import dev.vaileks.runesaltarv.weapons.hyperion.listeners.HyperionListener;
import dev.vaileks.runesaltarv.weapons.hyperion.managers.HyperionCooldownManager;
import dev.vaileks.runesaltarv.weapons.knightfall.listeners.KnightfallListener;
import dev.vaileks.runesaltarv.weapons.knightfall.managers.KnightfallManager;
import dev.vaileks.runesaltarv.weapons.knightfall.tasks.KnightfallPassiveTask;
import dev.vaileks.runesaltarv.weapons.nightpiercer.listeners.NightAbilityListener;
import dev.vaileks.runesaltarv.weapons.nightpiercer.managers.NightBossBarCooldown;
import dev.vaileks.runesaltarv.weapons.nightpiercer.managers.NightHealthStealManager;
import dev.vaileks.runesaltarv.weapons.nightpiercer.tasks.NightPassiveTask;
import dev.vaileks.runesaltarv.weapons.paladinsbattleaxe.listeners.PaladinsBattleAxeListener;
import dev.vaileks.runesaltarv.weapons.paladinsbattleaxe.managers.PaladinsBattleAxeManager;
import dev.vaileks.runesaltarv.weapons.palegun.listeners.PaleGunListener;
import dev.vaileks.runesaltarv.weapons.palegun.managers.PaleGunAbilityManager;
import dev.vaileks.runesaltarv.weapons.pureblade.listeners.PureBladeListener;
import dev.vaileks.runesaltarv.weapons.pureblade.managers.PureBladeManager;
import dev.vaileks.runesaltarv.weapons.pureblade.managers.PureBossBarManager;
import dev.vaileks.runesaltarv.weapons.shadowblade.listeners.ShadowBladeListener;
import dev.vaileks.runesaltarv.weapons.shadowblade.managers.ShadowBladeManager;
import dev.vaileks.runesaltarv.weapons.shadowblade.tasks.ShadowBladePassiveTask;
import dev.vaileks.runesaltarv.weapons.vulcan.listeners.VulcanHitListener;
import dev.vaileks.runesaltarv.weapons.vulcan.listeners.VulcanShootListener;
import dev.vaileks.runesaltarv.weapons.vulcan.managers.VulcanChargeManager;
import dev.vaileks.runesaltarv.weapons.vulcan.managers.VulcanCooldownManager;
import dev.vaileks.runesaltarv.weapons.windweaver.listeners.WindWeaverListener;
import dev.vaileks.runesaltarv.weapons.witherblade.listeners.WitherBladeListener;
import dev.vaileks.runesaltarv.weapons.witherblade.managers.WitherManager;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class RunesAltarV extends JavaPlugin {
   private static RunesAltarV instance;
   private NamespacedKey killsKey;
   private NamespacedKey vulcanKey;
   private NamespacedKey arrowKey;
   private NamespacedKey paleGunKey;
   private NamespacedKey hyperionKey;
   private NamespacedKey witherKey;
   private NamespacedKey paladinsBattleAxeKey;
   private NamespacedKey knightfallTierKey;
   private PaladinsBattleAxeManager paladinsBattleAxeManager;
   private PaladinsBattleAxeListener paladinsBattleAxeListener;
   private CrazySlotsManager crazySlotsManager;
   private CrazySlotsListener crazySlotsListener;
   private WorldGuardManager worldGuardManager;
   private VisualCleanupManager visualCleanupManager;
   private FriendManager friendManager;
   private CleanDamageManager cleanDamageManager;
   private EarthGauntletManager earthGauntletManager;
   private CutlassManager cutlassManager;
   private BoneCooldownManager boneCooldownManager;
   private BloodAbilityManager bloodAbilityManager;
   private BloodBossBarManager bloodBossBarManager;
   private NightAbilityListener nightAbilityListener;
   private NightHealthStealManager nightHealthManager;
   private FrostBossBarManager frostBossBarManager;
   private FrostAbilityManager frostAbilityManager;
   private PaleGunAbilityManager paleGunAbilityManager;
   private KnightfallManager knightfallManager;
   private PureBossBarManager pureBossBarManager;
   private PureBladeManager pureBladeManager;
   private ShadowBladeManager shadowBladeManager;
   private WindWeaverListener windWeaverListener;
   private WitherManager witherManager;
   private FileConfiguration weaponsConfig;
   private FileConfiguration modelsConfig;

   @Override
   public void onLoad() {
      WorldGuardManager.registerFlag();
   }

   @Override
   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      dev.vaileks.runesaltarv.integration.AltarPresets.load(this);
      this.saveDefaultWeaponsConfig();
      this.saveDefaultModelsConfig();
      this.killsKey = new NamespacedKey("runesaltarv", "bloodlust_kills");
      this.vulcanKey = new NamespacedKey("runesaltarv", "vulcan_crossbow");
      this.arrowKey = new NamespacedKey("runesaltarv", "vulcan_arrow");
      this.paleGunKey = new NamespacedKey("runesaltarv", "pale_gun");
      this.hyperionKey = new NamespacedKey("runesaltarv", "hyperion_sword");
      this.witherKey = new NamespacedKey("runesaltarv", "wither_blade");
      this.knightfallTierKey = new NamespacedKey("runesaltarv", "knightfall_tier");
      this.visualCleanupManager = new VisualCleanupManager(this);
      this.worldGuardManager = new WorldGuardManager(this);
      this.getServer().getPluginManager().registerEvents(new WorldGuardAbilityGate(this, this.worldGuardManager), this);
      this.paladinsBattleAxeKey = new NamespacedKey("runesaltarv", "paladins_battle_axe");
      this.paladinsBattleAxeManager = new PaladinsBattleAxeManager(this);
      this.paladinsBattleAxeListener = new PaladinsBattleAxeListener(this, this.paladinsBattleAxeManager);
      this.getServer().getPluginManager().registerEvents(this.paladinsBattleAxeListener, this);
      this.crazySlotsManager = new CrazySlotsManager(this);
      this.crazySlotsListener = new CrazySlotsListener(this, this.crazySlotsManager);
      this.getServer().getPluginManager().registerEvents(this.crazySlotsListener, this);
      this.friendManager = new FriendManager(this);
      this.cleanDamageManager = new CleanDamageManager(this);
      this.earthGauntletManager = new EarthGauntletManager(this);
      this.cutlassManager = new CutlassManager(this);
      this.boneCooldownManager = new BoneCooldownManager(this);
      this.bloodBossBarManager = new BloodBossBarManager(this);
      this.bloodAbilityManager = new BloodAbilityManager(this, this.bloodBossBarManager);
      this.nightAbilityListener = new NightAbilityListener(this);
      this.nightHealthManager = new NightHealthStealManager(this);
      this.knightfallManager = new KnightfallManager(this);
      this.shadowBladeManager = new ShadowBladeManager(this);
      this.witherManager = new WitherManager(this);
      this.windWeaverListener = new WindWeaverListener(this);
      this.frostBossBarManager = new FrostBossBarManager(this);
      this.frostAbilityManager = new FrostAbilityManager(this, this.frostBossBarManager);
      this.paleGunAbilityManager = new PaleGunAbilityManager(this);
      this.pureBossBarManager = new PureBossBarManager(this);
      this.pureBladeManager = new PureBladeManager(this, this.pureBossBarManager);
      PluginCommand cmd = this.getCommand("runesaltarv");
      if (cmd != null) {
         var commands = new AltarLegendaryCommand(this);
         cmd.setExecutor(commands);
         cmd.setTabCompleter(commands);
         this.getServer().getPluginManager().registerEvents(commands, this);
      }

      PluginCommand friendCmd = this.getCommand("friendlist");
      if (friendCmd != null) {
         friendCmd.setExecutor(new FriendListCommand(this));
         friendCmd.setTabCompleter(new FriendListTabCompleter(this));
      }

      this.getServer().getPluginManager().registerEvents(new BladeListener(this), this);
      this.getServer().getPluginManager().registerEvents(new BloodLustListener(this, this.bloodAbilityManager), this);
      new PassiveAuraTask(this).runTaskTimer(this, 0L, 1L);
      this.getServer().getPluginManager().registerEvents(this.nightAbilityListener, this);
      new NightPassiveTask().runTaskTimer(this, 0L, 20L);
      this.getServer().getPluginManager().registerEvents(new VulcanShootListener(), this);
      this.getServer().getPluginManager().registerEvents(new VulcanHitListener(), this);
      this.getServer().getPluginManager().registerEvents(new PaleGunListener(this, this.paleGunAbilityManager), this);
      this.getServer().getPluginManager().registerEvents(new ItemProtectionListener(this), this);
      this.getServer().getPluginManager().registerEvents(new FrostListener(this.frostAbilityManager), this);
      this.getServer().getPluginManager().registerEvents(new PureBladeListener(this.pureBladeManager), this);
      this.getServer().getPluginManager().registerEvents(new KnightfallListener(this, this.knightfallManager), this);
      new KnightfallPassiveTask(this).runTaskTimer(this, 0L, 1L);
      this.getServer().getPluginManager().registerEvents(new ShadowBladeListener(this, this.shadowBladeManager), this);
      new ShadowBladePassiveTask().runTaskTimer(this, 0L, 1L);
      this.getServer().getPluginManager().registerEvents(this.windWeaverListener, this);
      this.getServer().getPluginManager().registerEvents(new HyperionListener(), this);
      this.getServer().getPluginManager().registerEvents(new WitherBladeListener(this, this.witherManager), this);
      this.getServer().getPluginManager().registerEvents(new EarthGauntletListener(this, this.earthGauntletManager), this);
      this.getServer().getPluginManager().registerEvents(new CutlassListener(this.cutlassManager), this);
      this.getServer().getPluginManager().registerEvents(new SpeedBuffUtils(), this);
      (new BukkitRunnable() {
            @Override
            public void run() {
               for (Player p : RunesAltarV.this.getServer().getOnlinePlayers()) {
                  ItemStack item = p.getInventory().getItemInMainHand();
                  if (WeaponFactory.isAltarWeapon(item, "knightfall")) {
                     int kills = item.getItemMeta()
                        .getPersistentDataContainer()
                        .getOrDefault(RunesAltarV.this.killsKey, PersistentDataType.INTEGER, Integer.valueOf(0));
                     int cmd = item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : 0;
                     boolean hasDensity = item.getItemMeta().hasEnchant(Enchantment.DENSITY);
                     int tier = item.getItemMeta()
                        .getPersistentDataContainer()
                        .getOrDefault(
                           RunesAltarV.this.knightfallTierKey,
                           PersistentDataType.INTEGER,
                           Integer.valueOf(cmd >= 3003 || cmd == 3 ? 3 : (hasDensity ? 2 : (cmd < 3002 && cmd != 2 ? 0 : 1)))
                        );
                     if (kills >= 4 && tier < 1 || kills >= 8 && tier < 2 || kills >= 10 && tier < 3) {
                        TextUtils.sendActionBar(
                           p,
                           TextUtils.legacy(
                              RunesAltarV.this.tr("&aНажмите &2[Смена руки] &aчтобы улучшить оружие.", "&aPress &2[OffHand] &ato upgrade the weapon.")
                           )
                        );
                     }
                  }
               }
            }
         })
         .runTaskTimer(this, 0L, 20L);
      dev.vaileks.runesaltarv.integration.AltarIntegration.enable(this);
      this.getLogger().info("RunesAltarV enabled");
   }







   public void saveDefaultWeaponsConfig() {
      this.weaponsConfig=dev.vaileks.runesaltarv.integration.AltarPresets.mechanics();
   }

   public void reloadWeaponsConfig() {
      this.weaponsConfig=dev.vaileks.runesaltarv.integration.AltarPresets.mechanics();
   }

   public FileConfiguration getWeaponsConfig() {
      if (this.weaponsConfig == null) {
         this.reloadWeaponsConfig();
      }

      return this.weaponsConfig;
   }









   @Override
   public void onDisable() {
      dev.vaileks.runesaltarv.integration.AltarIntegration.disable();
      if (this.boneCooldownManager != null) {
         this.boneCooldownManager.removeAllBars();
      }

      if (this.bloodBossBarManager != null) {
         this.bloodBossBarManager.clearAll();
      }

      if (this.bloodAbilityManager != null) {
         this.bloodAbilityManager.cleanup();
      }

      if (this.nightHealthManager != null) {
         this.nightHealthManager.revertAll();
      }

      NightBossBarCooldown.clearAll();
      if (this.pureBossBarManager != null) {
         this.pureBossBarManager.clearAll();
      }

      if (this.frostBossBarManager != null) {
         this.frostBossBarManager.clearAll();
      }

      if (this.paleGunAbilityManager != null) {
         this.paleGunAbilityManager.clearAll();
      }

      if (this.windWeaverListener != null) {
         this.windWeaverListener.removeAllBars();
      }

      if (this.knightfallManager != null) {
         this.knightfallManager.clearAllBars();
      }

      if (this.shadowBladeManager != null) {
         this.shadowBladeManager.clearAllBars();
      }

      if (this.witherManager != null) {
         this.witherManager.clearAllBars();
      }

      if (this.earthGauntletManager != null) {
         this.earthGauntletManager.cleanupAll();
      }

      if (this.friendManager != null) {
         this.friendManager.saveFriends();
      }

      if (this.cutlassManager != null) {
         this.cutlassManager.cleanupAll();
      }

      if (this.visualCleanupManager != null) {
         this.visualCleanupManager.cleanupAll();
      }

      if (this.paladinsBattleAxeManager != null) {
         this.paladinsBattleAxeManager.clearAllBars();
      }

      if (this.crazySlotsManager != null) {
         this.crazySlotsManager.forceRevertAll();
      }

      if (this.cleanDamageManager != null) {
         this.cleanDamageManager.clear();
      }


      SpeedBuffUtils.cleanupAll();
      TextUtils.clearBossBars();
      VulcanCooldownManager.clearAllBars();
      VulcanChargeManager.cancelAll();
      HyperionCooldownManager.clearAllBars();

      this.getLogger().info("RunesAltarV выключен!");
   }

   public static RunesAltarV getInstance() {
      return instance;
   }

   public NamespacedKey getKillsKey() {
      return this.killsKey;
   }

   public NamespacedKey getVulcanKey() {
      return this.vulcanKey;
   }

   public NamespacedKey getArrowKey() {
      return this.arrowKey;
   }

   public NamespacedKey getPaleGunKey() {
      return this.paleGunKey;
   }

   public NamespacedKey getHyperionKey() {
      return this.hyperionKey;
   }

   public NamespacedKey getWitherKey() {
      return this.witherKey;
   }

   public NamespacedKey getKnightfallTierKey() {
      return this.knightfallTierKey;
   }

   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   public CleanDamageManager getCleanDamageManager() {
      return this.cleanDamageManager;
   }

   public EarthGauntletManager getEarthGauntletManager() {
      return this.earthGauntletManager;
   }

   public CutlassManager getCutlassManager() {
      return this.cutlassManager;
   }

   public BoneCooldownManager getBoneCooldownManager() {
      return this.boneCooldownManager;
   }

   public BloodAbilityManager getBloodAbilityManager() {
      return this.bloodAbilityManager;
   }

   public BloodBossBarManager getBloodBossBarManager() {
      return this.bloodBossBarManager;
   }

   public NightAbilityListener getNightAbilityListener() {
      return this.nightAbilityListener;
   }

   public NightHealthStealManager getNightHealthManager() {
      return this.nightHealthManager;
   }

   public PureBladeManager getPureBladeManager() {
      return this.pureBladeManager;
   }

   public WindWeaverListener getWindWeaverListener() {
      return this.windWeaverListener;
   }

   public boolean isEnglish() {
      return "en_US".equalsIgnoreCase(this.getConfig().getString("lang", "en_US"));
   }

   public String tr(String ru, String en) {
      return this.isEnglish() ? en : ru;
   }

   public boolean isLimitsEnabled() {
      return this.getConfig().getBoolean("limits.enabled", false);
   }

   public double getLegendaryMaxY() {
      return this.getConfig().getDouble("limits.max-y-height", 200.0);
   }

   public boolean isAboveLegendaryHeight(Player player) {
      return this.isLimitsEnabled() && player.getLocation().getY() > this.getLegendaryMaxY();
   }

   public void resetAllCooldowns(Player player) {
      dev.vaileks.runesaltarv.integration.AltarCooldowns.clear(player);
      if (this.boneCooldownManager != null) {
         this.boneCooldownManager.resetPlayer(player);
      }

      if (this.bloodBossBarManager != null) {
         this.bloodBossBarManager.resetPlayer(player);
      }

      if (this.nightAbilityListener != null) {
         this.nightAbilityListener.resetCooldowns(player);
      }

      NightBossBarCooldown.resetPlayer(player);
      if (this.frostBossBarManager != null) {
         this.frostBossBarManager.resetPlayer(player);
      }

      if (this.paleGunAbilityManager != null) {
         this.paleGunAbilityManager.resetPlayer(player);
      }

      if (this.pureBladeManager != null) {
         this.pureBladeManager.resetCooldowns(player);
      }

      if (this.knightfallManager != null) {
         this.knightfallManager.resetCooldowns(player);
      }

      if (this.shadowBladeManager != null) {
         this.shadowBladeManager.resetCooldowns(player);
      }

      if (this.witherManager != null) {
         this.witherManager.resetCooldowns(player);
      }

      if (this.earthGauntletManager != null) {
         this.earthGauntletManager.resetCooldowns(player);
      }

      if (this.cutlassManager != null) {
         this.cutlassManager.resetCooldowns(player);
      }

      if (this.windWeaverListener != null) {
         this.windWeaverListener.resetPlayer(player);
      }

      VulcanCooldownManager.resetPlayer(player);
      HyperionCooldownManager.resetPlayer(player);
      if (this.paladinsBattleAxeManager != null) {
         this.paladinsBattleAxeManager.resetCooldowns(player);
      }

      if (this.crazySlotsManager != null) {
         this.crazySlotsManager.resetPlayer(player);
      }

      TextUtils.removePlayerFromAllBossBars(player);
   }

















   public void saveDefaultModelsConfig() { reloadModelsConfig(); }
   public void reloadModelsConfig() {
      this.modelsConfig = new YamlConfiguration();
      this.modelsConfig.set("symbols.crouch-rmb", "Shift + RMB");
      this.modelsConfig.set("symbols.charges-icon", "●");
      dev.vaileks.runesaltarv.integration.AltarPresets.models(this.modelsConfig);
   }
   public FileConfiguration getModelsConfig() {
      if (this.modelsConfig == null) {
         this.reloadModelsConfig();
      }

      return this.modelsConfig;
   }

   public NamespacedKey getPaladinsBattleAxeKey() {
      return this.paladinsBattleAxeKey;
   }

   public PaladinsBattleAxeManager getPaladinsBattleAxeManager() {
      return this.paladinsBattleAxeManager;
   }

   public CrazySlotsManager getCrazySlotsManager() {
      return this.crazySlotsManager;
   }

   public int getKnightfallCharges(Player player){return this.knightfallManager.getCharges(player);}

   public WorldGuardManager getWorldGuardManager() {
      return this.worldGuardManager;
   }

   public VisualCleanupManager getVisualCleanupManager() {
      return this.visualCleanupManager;
   }

   public PaleGunAbilityManager getPaleGunAbilityManager() {
      return this.paleGunAbilityManager;
   }






}

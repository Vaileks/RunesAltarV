package dev.vaileks.runesaltarv.integrations.worldguard;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.TextUtils;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldGuardManager {
   private final RunesAltarV plugin;
   private static Object altarWeaponsFlag = null;
   private boolean available = false;

   public WorldGuardManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.init();
   }

   public static void registerFlag() {
      try {
         Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
         Method getInstanceMethod = wgClass.getMethod("getInstance");
         Object wgInstance = getInstanceMethod.invoke(null);
         Method getFlagRegistry = wgClass.getMethod("getFlagRegistry");
         Object registry = getFlagRegistry.invoke(wgInstance);
         Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
         Object newFlag = stateFlagClass.getConstructor(String.class, boolean.class).newInstance("altar-weapons", true);
         Method registerMethod = registry.getClass().getMethod("register", Class.forName("com.sk89q.worldguard.protection.flags.Flag"));

         try {
            registerMethod.invoke(registry, newFlag);
            altarWeaponsFlag = newFlag;
         } catch (Exception var10) {
            Method getMethod = registry.getClass().getMethod("get", String.class);
            altarWeaponsFlag = getMethod.invoke(registry, "altar-weapons");
         }
      } catch (Throwable var11) {
      }
   }

   private void init() {
      Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
      this.available = wgPlugin != null && wgPlugin.isEnabled() && altarWeaponsFlag != null;
   }

   public boolean isAvailable() {
      return this.available;
   }

   public boolean canUseAbilities(Player player) {
      return this.available && player != null ? this.isLocationAllowed(player.getLocation(), player) : true;
   }

   public boolean isLocationAllowed(Location location, Player player) {
      if (this.available && location != null) {
         try {
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptLocMethod = bukkitAdapter.getMethod("adapt", Location.class);
            Object weLoc = adaptLocMethod.invoke(null, location);
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = wgClass.getMethod("getInstance").invoke(null);
            Object platform = wgClass.getMethod("getPlatform").invoke(wgInstance);
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Object query = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);
            Object associable = null;
            if (player != null) {
               Class<?> wgPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
               Object wgPluginInst = wgPluginClass.getMethod("inst").invoke(null);
               associable = wgPluginClass.getMethod("wrapPlayer", Player.class).invoke(wgPluginInst, player);
            }

            Class<?> locationClass = Class.forName("com.sk89q.worldedit.util.Location");
            Class<?> associableClass = Class.forName("com.sk89q.worldguard.protection.association.RegionAssociable");
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            Method testStateMethod = query.getClass().getMethod("testState", locationClass, associableClass, stateFlagClass.arrayType());
            Object flagsArray = Array.newInstance(stateFlagClass, 1);
            Array.set(flagsArray, 0, altarWeaponsFlag);
            return testStateMethod.invoke(query, weLoc, associable, flagsArray) instanceof Boolean b ? b : true;
         } catch (Throwable var19) {
            return true;
         }
      } else {
         return true;
      }
   }

   public boolean canAffectTarget(Player source, Entity target) {
      if (!this.available || target == null) {
         return true;
      } else {
         return source != null && !this.canUseAbilities(source) ? false : this.isLocationAllowed(target.getLocation(), source);
      }
   }

   public void notifyDenied(Player player) {
      if (player != null) {
         Component msg = TextUtils.shadow(
            Component.text(
               this.plugin.tr("Способности легендарного оружия отключены в этой зоне!", "Legendary weapon abilities are disabled in this safe zone!"),
               NamedTextColor.RED
            )
         );
         TextUtils.sendActionBar(player, msg);
      }
   }

   public void notifyDeniedChat(Player player) {
      if (player != null) {
         TextUtils.sendMessage(
            player,
            TextUtils.legacy(
               "§c" + this.plugin.tr("Способности легендарного оружия отключены в этой зоне.", "Legendary weapon abilities are disabled in this zone.")
            )
         );
      }
   }
}

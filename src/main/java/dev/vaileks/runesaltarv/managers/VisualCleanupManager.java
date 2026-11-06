package dev.vaileks.runesaltarv.managers;

import dev.vaileks.runesaltarv.RunesAltarV;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

public class VisualCleanupManager {
   private final RunesAltarV plugin;
   private final NamespacedKey tagKey;
   private final Set<Entity> trackedEntities = Collections.newSetFromMap(new ConcurrentHashMap<>());

   public VisualCleanupManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.tagKey = new NamespacedKey("runesaltarv", "altar_temp_display");
      this.cleanupWorldOrphans();
   }

   public void track(Entity entity) {
      if (entity != null && entity.isValid()) {
         try {
            entity.getPersistentDataContainer().set(this.tagKey, PersistentDataType.BYTE, (byte)1);
         } catch (Throwable var3) {
         }

         this.trackedEntities.add(entity);
      }
   }

   public void untrack(Entity entity) {
      if (entity != null) {
         this.trackedEntities.remove(entity);
      }
   }

   public boolean isTrackedEntity(Entity entity) {
      return entity == null ? false : this.trackedEntities.contains(entity) || entity.getPersistentDataContainer().has(this.tagKey, PersistentDataType.BYTE);
   }

   public void cleanupAll() {
      for (Entity entity : this.trackedEntities) {
         try {
            if (entity != null && entity.isValid()) {
               entity.remove();
            }
         } catch (Throwable var4) {
         }
      }

      this.trackedEntities.clear();
      this.cleanupWorldOrphans();
   }

   private void cleanupWorldOrphans() {
      for (World world : Bukkit.getWorlds()) {
         for (Entity entity : world.getEntities()) {
            try {
               if (entity.getPersistentDataContainer().has(this.tagKey, PersistentDataType.BYTE)) {
                  entity.remove();
               }
            } catch (Throwable var6) {
            }
         }
      }
   }
}

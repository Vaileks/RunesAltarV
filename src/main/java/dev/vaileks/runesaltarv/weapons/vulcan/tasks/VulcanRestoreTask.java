package dev.vaileks.runesaltarv.weapons.vulcan.tasks;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.TextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

public class VulcanRestoreTask {
   private final RunesAltarV plugin;
   private final Map<Location, Material> blocksToRestore;
   private final Location holoLoc;
   private final int totalSeconds;
   private TextDisplay hologram;

   public VulcanRestoreTask(RunesAltarV plugin, Map<Location, Material> blocksToRestore, Location holoLoc, int totalSeconds) {
      this.plugin = plugin;
      this.blocksToRestore = blocksToRestore;
      this.holoLoc = holoLoc;
      this.totalSeconds = totalSeconds;
   }

   public void start() {
      Location spawnLoc = this.holoLoc.clone().subtract(0.0, 2.0, 0.0);
      this.hologram = (TextDisplay)spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);
      RunesAltarV.getInstance().getVisualCleanupManager().track(this.hologram);
      this.hologram.setBillboard(Billboard.CENTER);
      this.hologram.setShadowed(true);
      this.hologram.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      Transformation transformation = this.hologram.getTransformation();
      transformation.getScale().set(2.0F, 2.0F, 2.0F);
      this.hologram.setTransformation(transformation);
      this.updateHolo(this.totalSeconds);
      (new BukkitRunnable() {
         int timeLeft = VulcanRestoreTask.this.totalSeconds;

         @Override
         public void run() {
            if (this.timeLeft <= 0) {
               VulcanRestoreTask.this.hologram.remove();
               VulcanRestoreTask.this.startRestoringBlocks();
               this.cancel();
            } else {
               VulcanRestoreTask.this.updateHolo(this.timeLeft);
               this.timeLeft--;
            }
         }
      }).runTaskTimer(this.plugin, 20L, 20L);
   }

   private void updateHolo(int seconds) {
      if (this.hologram != null && this.hologram.isValid()) {
         Component text = TextUtils.legacy(this.plugin.tr("&6⚠&f " + seconds + " секунд", "&6⚠&f " + seconds + " seconds"));
         this.hologram.text(text);
      }
   }

   private void startRestoringBlocks() {
      final List<Location> locations = new ArrayList<>(this.blocksToRestore.keySet());
      locations.sort((first, second) -> {
         boolean airFirst = this.blocksToRestore.get(first).isAir();
         boolean airSecond = this.blocksToRestore.get(second).isAir();
         if (airFirst && !airSecond) {
            return -1;
         } else if (!airFirst && airSecond) {
            return 1;
         } else {
            return airFirst ? Double.compare(second.getY(), first.getY()) : Double.compare(first.getY(), second.getY());
         }
      });
      (new BukkitRunnable() {
         int index = 0;
         final int blocksPerTick = 5;

         @Override
         public void run() {
            if (this.index >= locations.size()) {
               this.cancel();
            } else {
               for (int i = 0; i < 5 && this.index < locations.size(); i++) {
                  Location location = locations.get(this.index);
                  Material oldMaterial = VulcanRestoreTask.this.blocksToRestore.get(location);
                  Material currentType = location.getBlock().getType();
                  if (currentType != oldMaterial) {
                     location.getBlock().setType(oldMaterial, false);
                     location.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, location.clone().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.05);
                     if (Math.random() < 0.2) {
                        location.getWorld().playSound(location, Sound.BLOCK_LAVA_EXTINGUISH, 0.3F, 1.5F);
                     }
                  }

                  this.index++;
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }
}

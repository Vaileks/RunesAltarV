package dev.vaileks.runesaltarv.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ParticleUtils {
   public static void spawnBlockDispersion(Plugin plugin, Location center, BlockData blockData) {
      spawnBlockDispersion(plugin, center, blockData, 4.5, 74);
   }

   public static void spawnBlockDispersion(Plugin plugin, Location center, BlockData blockData, double maxRadius) {
      spawnBlockDispersion(plugin, center, blockData, maxRadius, 74);
   }

   public static void spawnBlockDispersion(Plugin plugin, Location center, BlockData blockData, double maxRadius, int totalCount) {
      if (center != null && center.getWorld() != null && blockData != null) {
         Material mat = blockData.getMaterial();
         if (!mat.isItem() || mat.isAir()) {
            mat = Material.COBBLESTONE;
         }

         World world = center.getWorld();
         Location origin = center.clone().add(0.0, 0.8, 0.0);
         ItemStack stack = new ItemStack(mat);
         double baseH = Math.max(0.32, maxRadius * 0.14);
         int mainCount = Math.max(8, (int)((double)totalCount * 0.45));
         int fountainCount = Math.max(4, (int)((double)totalCount * 0.2));
         int lowCount = Math.max(4, (int)((double)totalCount * 0.15));
         int downCount = Math.max(4, totalCount - mainCount - fountainCount - lowCount);

         for (int i = 0; i < mainCount; i++) {
            double angle = (Math.PI * 2) * (double)i / (double)mainCount + (Math.random() - 0.5) * 0.35;
            double h = baseH * (0.8 + Math.random() * 0.7);
            double vx = Math.cos(angle) * h;
            double vz = Math.sin(angle) * h;
            double vy = 0.42 + Math.random() * 0.38;
            world.spawnParticle(Particle.ITEM, origin, 0, vx, vy, vz, 1.0, stack, true);
         }

         for (int i = 0; i < fountainCount; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double h = 0.08 + Math.random() * baseH * 0.5;
            double vy = 0.65 + Math.random() * 0.45;
            world.spawnParticle(Particle.ITEM, origin, 0, Math.cos(angle) * h, vy, Math.sin(angle) * h, 1.0, stack, true);
         }

         for (int i = 0; i < lowCount; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double h = baseH * (1.0 + Math.random() * 0.45);
            double vy = 0.22 + Math.random() * 0.2;
            world.spawnParticle(Particle.ITEM, origin, 0, Math.cos(angle) * h, vy, Math.sin(angle) * h, 1.0, stack, true);
         }

         for (int i = 0; i < downCount; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double h = baseH * (0.7 + Math.random() * 0.5);
            double vy = -0.15 - Math.random() * 0.35;
            world.spawnParticle(Particle.ITEM, origin, 0, Math.cos(angle) * h, vy, Math.sin(angle) * h, 1.0, stack, true);
         }
      }
   }
}

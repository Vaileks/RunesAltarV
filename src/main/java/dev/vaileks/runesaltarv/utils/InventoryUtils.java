package dev.vaileks.runesaltarv.utils;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class InventoryUtils {
   private InventoryUtils() {
   }

   public static void giveOrDrop(Player player, ItemStack item) {
      Map<Integer, ItemStack> leftovers = player.getInventory().addItem(new ItemStack[]{item});

      for (ItemStack leftover : leftovers.values()) {
         player.getWorld().dropItemNaturally(player.getLocation(), leftover);
      }
   }
}

package dev.vaileks.runesaltarv.utils;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemUtils {
   private static final boolean HAS_COMPONENT_META;

   private ItemUtils() {
   }

   public static void setDisplayName(ItemMeta meta, Component component) {
      if (meta != null && component != null) {
         if (HAS_COMPONENT_META) {
            try {
               meta.displayName(component);
               return;
            } catch (Throwable var3) {
            }
         }

         meta.setDisplayName(TextUtils.toLegacy(component));
      }
   }

   public static void setLore(ItemMeta meta, List<Component> lore) {
      if (meta != null) {
         if (HAS_COMPONENT_META) {
            try {
               meta.lore(lore);
               return;
            } catch (Throwable var5) {
            }
         }

         if (lore == null) {
            meta.setLore(null);
         } else {
            List<String> list = new ArrayList<>(lore.size());

            for (Component c : lore) {
               list.add(TextUtils.toLegacy(c));
            }

            meta.setLore(list);
         }
      }
   }

   static {
      boolean has = false;

      try {
         ItemMeta.class.getMethod("displayName", Component.class);
         has = true;
      } catch (Throwable var2) {
      }

      HAS_COMPONENT_META = has;
   }
}

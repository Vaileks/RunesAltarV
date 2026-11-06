package dev.vaileks.runesaltarv.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TextUtils {
   private static final ShadowColor TEXT_SHADOW = ShadowColor.shadowColor(-16777216);
   private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
   private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
   private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
   private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
   private static final boolean IS_PAPER_ADVENTURE;

   private TextUtils() {
   }

   public static Component shadow(Component component) {
      return component.shadowColor(TEXT_SHADOW);
   }

   public static Component legacy(String text) {
      return shadow(LEGACY.deserialize(text.replace('§', '&')));
   }

   public static Component mini(String text) {
      return shadow(MINI_MESSAGE.deserialize(text));
   }

   public static Component empty() {
      return shadow(Component.empty());
   }

   public static String toLegacy(Component component) {
      return component == null ? "" : LEGACY_SECTION.serialize(component);
   }

   public static String toPlain(Component component) {
      return component == null ? "" : PLAIN.serialize(component);
   }

   public static void sendMessage(CommandSender sender, Component component) {
      if (sender != null && component != null) {
         if (IS_PAPER_ADVENTURE) {
            try {
               sender.sendMessage(component);
               return;
            } catch (Throwable var3) {
            }
         }

         sender.sendMessage(toLegacy(component));
      }
   }

   public static void sendMessage(CommandSender sender, String message) {
      if (sender != null && message != null) {
         if (IS_PAPER_ADVENTURE) {
            try {
               sender.sendMessage(legacy(message));
               return;
            } catch (Throwable var3) {
            }
         }

         sender.sendMessage(message);
      }
   }

   public static void sendActionBar(Player player, Component component) {
      ShadowBossBar.message(player, component);
   }

   static void sendActionBarNow(Player player, Component component) {
      if (player != null && player.isOnline() && component != null) {
         if (IS_PAPER_ADVENTURE) {
            try {
               player.sendActionBar(component);
               return;
            } catch (Throwable var4) {
            }
         }

         try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(toLegacy(component)));
         } catch (Throwable var3) {
         }
      }
   }

   public static void showTitle(Player player, Title title) {
      if (player != null && player.isOnline() && title != null) {
         if (IS_PAPER_ADVENTURE) {
            try {
               player.showTitle(title);
               return;
            } catch (Throwable var8) {
            }
         }

         try {
            String main = title.title() != null ? toLegacy(title.title()) : "";
            String sub = title.subtitle() != null ? toLegacy(title.subtitle()) : "";
            int fadeIn = title.times() != null ? (int)(title.times().fadeIn().toMillis() / 50L) : 10;
            int stay = title.times() != null ? (int)(title.times().stay().toMillis() / 50L) : 40;
            int fadeOut = title.times() != null ? (int)(title.times().fadeOut().toMillis() / 50L) : 10;
            player.sendTitle(main, sub, fadeIn, stay, fadeOut);
         } catch (Throwable var7) {
         }
      }
   }

   public static BossBar bossBar(String title, BarColor color, BarStyle style) {
      return new ShadowBossBar(title, color, style);
   }

   public static BossBar cooldownBar(String title, BarColor color, BarStyle style) {
      BossBar bar = new ShadowBossBar(title, color, style, true);
      bar.setVisible(false);
      return bar;
   }

   public static void removePlayerFromAllBossBars(Player player) {
      ShadowBossBar.removePlayerFromAll(player);
   }

   public static void clearBossBars() {
      ShadowBossBar.clearAll();
   }

   static {
      boolean paper = false;

      try {
         CommandSender.class.getMethod("sendMessage", Component.class);
         paper = true;
      } catch (Throwable var2) {
      }

      IS_PAPER_ADVENTURE = paper;
   }
}

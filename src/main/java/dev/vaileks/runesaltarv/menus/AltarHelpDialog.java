package dev.vaileks.runesaltarv.menus;
import org.bukkit.entity.Player;import dev.vaileks.runesaltarv.RunesAltarV;import dev.vaileks.runesaltarv.utils.TextUtils;
public final class AltarHelpDialog {
 public static void open(Player player,RunesAltarV plugin){sendChatFallback(player,!plugin.isEnglish());}
 public static void sendChatFallback(Player player,boolean ru){if(player==null)return;
  for(String line:new String[]{"§6RunesAltarV","§7/lega — меню легендарного оружия","§7/csi give <игрок> Altar_<id> — выдать легу","§7/al reload — перечитать пресеты","§7/al cooldown reset <игрок> — сбросить КД","§7/al altar help — команды старых алтарей","§7Настройки: LegendaryDopPlugin/presets/<пресет>/RunesAltarV"})player.sendMessage(TextUtils.legacy(line));
 }
 private AltarHelpDialog(){}
}

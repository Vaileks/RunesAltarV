package dev.vaileks.runesaltarv.integration;

import dev.vaileks.runesaltarv.RunesAltarV;
import java.util.*;
import java.util.function.LongSupplier;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class CooldownFeedback {
 private static final Map<UUID,Map<String,BukkitTask>> tasks=new HashMap<>();
 private static final Map<UUID,Long> lastWait=new HashMap<>();
 static void watch(Player player,String key,long millis,ItemStack label,LongSupplier remaining) {
  CooldownFeedback.cancel(player,key);
  Component name=label.hasItemMeta()&&label.getItemMeta().hasDisplayName()?label.getItemMeta().displayName():Component.text(key);
  BukkitTask task=new BukkitRunnable(){public void run(){
   if(!player.isOnline()){CooldownFeedback.cancel(player,key);return;}
   if(remaining.getAsLong()>0)return;
   CooldownFeedback.cancel(player,key);
   player.sendMessage(Component.text(RunesAltarV.getInstance().tr("Способность готова к использованию: ","Ability ready: "),NamedTextColor.GREEN).append(name));
   player.playSound(player.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,.6f,1.5f);
  }}.runTaskTimer(RunesAltarV.getInstance(), Math.max(1, Math.min(20,millis/50)),10);
  tasks.computeIfAbsent(player.getUniqueId(),id->new HashMap<>()).put(key,task);
 }
 static void cancel(Player player,String key){var all=tasks.get(player.getUniqueId());if(all==null)return;var old=all.remove(key);if(old!=null)old.cancel();if(all.isEmpty()){tasks.remove(player.getUniqueId());lastWait.remove(player.getUniqueId());}}
 static void waiting(Player player,long millis){long now=System.currentTimeMillis();UUID id=player.getUniqueId();if(now-lastWait.getOrDefault(id,0L)<1000)return;lastWait.put(id,now);long seconds=(millis+999)/1000;
  player.sendMessage(Component.text(RunesAltarV.getInstance().tr("Способность перезаряжается: "+seconds+" сек.","Ability recharging: "+seconds+"s."),NamedTextColor.RED));
 }
 private CooldownFeedback(){}
}

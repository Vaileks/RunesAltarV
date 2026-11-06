package dev.vaileks.runesaltarv.integration;
import java.util.*;import java.util.function.LongSupplier;import org.bukkit.*;import org.bukkit.entity.Player;import org.bukkit.inventory.ItemStack;import org.bukkit.boss.*;
public final class AltarCooldowns {
 private record Timer(String weapon,String ability,String key,LongSupplier remaining){}
 private static final Map<UUID,Map<String,Timer>> timers=new HashMap<>();
 public static void observe(Player player,String weapon,String ability,LongSupplier remaining,long millis){
  if(millis<=0||java.util.Set.of("infection","backstab","cloak").contains(ability.toLowerCase(java.util.Locale.ROOT)))return;String key="altar:"+weapon+":"+ability;
  timers.computeIfAbsent(player.getUniqueId(),id->new HashMap<>()).put(key,new Timer(weapon,ability,key,remaining));
  var held=player.getInventory().getItemInMainHand();var match=AltarCatalog.find(held);
  ItemStack label=match!=null&&match.id().equals(weapon)?held:AltarCatalog.WEAPONS.stream().filter(e->e.id().equals(weapon)).findFirst().orElseThrow().factory().get();
  CooldownFeedback.watch(player,key,millis,label,remaining);
 }
 public static void clear(Player player){var removed=timers.remove(player.getUniqueId());if(removed!=null)removed.values().forEach(t->CooldownFeedback.cancel(player,t.key));}
 public static void disable(){for(UUID id:new ArrayList<>(timers.keySet())){var p=Bukkit.getPlayer(id);if(p!=null)clear(p);}timers.clear();}
 public static BossBar cooldownBar(String title,BarColor color,BarStyle style){return dev.vaileks.runesaltarv.utils.TextUtils.cooldownBar(title,color,style);}
 public static void attempt(Player player,String trigger){
  var weapon=AltarCatalog.find(player.getInventory().getItemInMainHand());if(weapon==null)return;
  var all=timers.get(player.getUniqueId());if(all==null)return;
  if(weapon.id().equals("knightfall")&&trigger.equals("f")&&dev.vaileks.runesaltarv.RunesAltarV.getInstance().getKnightfallCharges(player)>0)return;
  String wanted=ability(weapon.id(),trigger);if(wanted==null)return;
  for(var timer:all.values())if(timer.weapon.equals(weapon.id())&&normalize(timer.ability).equals(normalize(wanted))){long left=timer.remaining.getAsLong();if(left>0){CooldownFeedback.waiting(player,left);return;}}
 }
 private static String normalize(String value){return value.toLowerCase(Locale.ROOT).replace("cooldowns","").replace("cooldown","").replace("_","").replace("-","");}
 static String ability(String weapon,String trigger){return switch(weapon){
  case "bone_blade"->trigger.equals("f")?"dash":trigger.equals("shift-f")?"cage":null;
  case "bloodlust"->trigger.equals("f")?"BloodHook":trigger.equals("shift-f")?"BloodTrail":null;
  case "nightpiercer"->trigger.equals("f")?"bite":trigger.equals("shift-f")?"transformation":null;
  case "vulcan_crossbow"->trigger.equals("shift-rmb")?"magma":null;
  case "pale_gun"->trigger.endsWith("rmb")?"shoot":trigger.endsWith("f")?"roots":null;
  case "frost_scythe"->trigger.equals("shift-rmb")||trigger.equals("shift-f")?"CommandOfIce":trigger.equals("rmb")?"ScytheThrow":null;
  case "pure_blade"->trigger.equals("f")?"ShadeSoul":trigger.equals("shift-f")?"CycloneSlash":null;
  case "knightfall"->trigger.equals("shift-f")?"throw":trigger.equals("f")?"hookNextCharge":null;
  case "shadow_blade"->trigger.equals("shift-rmb")?"leap":trigger.endsWith("f")?"daggers":null;
  case "windweaver"->trigger.equals("f")?"leap":trigger.equals("shift-f")?"burst":null;
  case "hyperion"->trigger.equals("f")?"holyLance":trigger.equals("shift-f")?"scorching":null;
  case "wither_blade"->trigger.equals("f")?"dash":trigger.equals("shift-f")?"aura":null;
  case "earth_gauntlet"->trigger.equals("f")?"MeteorStrike":trigger.equals("shift-f")?"Mudslide":null;
  case "cutlass"->trigger.equals("f")?"thousand_cuts":trigger.equals("shift-f")?"parry":null;
  case "paladins_battle_axe"->trigger.equals("f")?"earth_shatter":trigger.equals("shift-f")?"stalwart_absorption":null;
  case "crazyslots"->trigger.endsWith("rmb")?"transform":null;
  default->null;
 };}
 private AltarCooldowns(){}
}

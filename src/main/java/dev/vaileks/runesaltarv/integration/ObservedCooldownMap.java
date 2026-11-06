package dev.vaileks.runesaltarv.integration;
import java.util.*;import java.util.concurrent.ConcurrentHashMap;import org.bukkit.Bukkit;
public final class ObservedCooldownMap<K> extends ConcurrentHashMap<K,Long> {
 private final String weapon,ability;
 public ObservedCooldownMap(String weapon,String ability){this.weapon=weapon;this.ability=ability;}
 @Override public Long put(K key,Long until){Long old=super.put(key,until);var ref=parse(key);if(ref!=null){var p=Bukkit.getPlayer(ref.owner);if(p!=null)AltarCooldowns.observe(p,weapon,ref.ability,()->getOrDefault(key,0L)-System.currentTimeMillis(),until-System.currentTimeMillis());}return old;}
 private record Ref(UUID owner,String ability){}
 private Ref parse(Object key){if(key instanceof UUID id)return new Ref(id,ability);String text=String.valueOf(key);int colon=text.indexOf(':');if(colon<0)return null;try{return new Ref(UUID.fromString(text.substring(0,colon)),text.substring(colon+1));}catch(IllegalArgumentException e){return null;}}
}

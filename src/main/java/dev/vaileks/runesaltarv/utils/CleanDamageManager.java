package dev.vaileks.runesaltarv.utils;
import java.util.*;import org.bukkit.entity.*;import org.bukkit.event.*;import org.bukkit.event.entity.EntityDamageEvent;import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;import dev.vaileks.runesaltarv.RunesAltarV;
public final class CleanDamageManager implements Listener {
 private final Map<UUID,Double> pendingDamage=new HashMap<>();
 private final Map<UUID,Integer> resurrected=new HashMap<>();
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 public void resurrect(org.bukkit.event.entity.EntityResurrectEvent event){
  UUID id=event.getEntity().getUniqueId(); int tick=org.bukkit.Bukkit.getCurrentTick();resurrected.put(id,tick);
  org.bukkit.Bukkit.getScheduler().runTaskLater(RunesAltarV.getInstance(),()->resurrected.remove(id,tick),3L);
 }
 public CleanDamageManager(RunesAltarV plugin){plugin.getServer().getPluginManager().registerEvents(this,plugin);}
 public void apply(LivingEntity target,double amount){apply(target,null,amount);}
 public void apply(LivingEntity target,Entity attacker,double amount){
  if(target==null||!Double.isFinite(amount)||amount<=0||target.isDead()||!target.isValid())return;
  if(target instanceof Player p&&(p.getGameMode()==org.bukkit.GameMode.CREATIVE||p.getGameMode()==org.bukkit.GameMode.SPECTATOR))return;
  if(resurrected.getOrDefault(target.getUniqueId(), -100) >= org.bukkit.Bukkit.getCurrentTick() - 1)return;
  pendingDamage.put(target.getUniqueId(),amount);target.setNoDamageTicks(0);
  try{CombatUtils.runSyntheticDamage(()->{if(attacker==null)target.damage(amount);else target.damage(amount,attacker);});}
  finally{pendingDamage.remove(target.getUniqueId());}
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 public void onCleanDamage(EntityDamageEvent event){
  Double amount=pendingDamage.remove(event.getEntity().getUniqueId());if(amount==null)return;
  event.setDamage(amount);
  for(var modifier:new DamageModifier[]{DamageModifier.ARMOR,DamageModifier.RESISTANCE,DamageModifier.MAGIC,DamageModifier.HARD_HAT,DamageModifier.BLOCKING})if(event.isApplicable(modifier))event.setDamage(modifier,0);
  if(event.isApplicable(DamageModifier.ABSORPTION)&&event.getEntity() instanceof LivingEntity target)event.setDamage(DamageModifier.ABSORPTION,-Math.min(amount,Math.max(0,target.getAbsorptionAmount())));
 }
 public void clear(){pendingDamage.clear();resurrected.clear();}
}

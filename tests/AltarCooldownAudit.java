package test;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.integration.*;
import dev.vaileks.runesaltarv.weapons.vulcan.managers.VulcanCooldownManager;
import dev.vaileks.runesaltarv.weapons.vulcan.listeners.VulcanShootListener;
import dev.vaileks.runesaltarv.weapons.pureblade.managers.PureBossBarManager;
import dev.vaileks.runesaltarv.weapons.palegun.listeners.PaleGunListener;
import java.lang.reflect.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

/** Runs against a disposable Paper server, using real managers and event handlers. */
public final class AltarCooldownAudit {
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static Object field(Object owner,String name)throws Exception{
  Class<?> type=owner instanceof Class<?> c?c:owner.getClass();
  Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner instanceof Class<?>?null:owner);
 }
 static Object call(Object owner,String name,Class<?>[] types,Object... args)throws Exception{
  Class<?> type=owner instanceof Class<?> c?c:owner.getClass();
  Method m=type.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(owner instanceof Class<?>?null:owner,args);
 }
 static Object defaults(Class<?> type){if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;if(type==float.class)return 0f;if(type==double.class)return 0d;return null;}
 @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler h){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},h);}
 @SuppressWarnings("unchecked") static void flat(Object owner,String map,String guard,Player p)throws Exception{
  Map<UUID,Long> values=(Map<UUID,Long>)field(owner,map);Class<?>[] types={Player.class};
  values.put(p.getUniqueId(),System.currentTimeMillis()+25000);
  check((boolean)call(owner,guard,types,p),map+" blocks repeat activation");
  values.put(p.getUniqueId(),System.currentTimeMillis()-1);
  check(!(boolean)call(owner,guard,types,p),map+" expires");values.remove(p.getUniqueId());
 }
 @SuppressWarnings("unchecked") static void nested(Object owner,String map,String guard,String ability,Player p)throws Exception{
  Map<UUID,Map<String,Long>> values=(Map<UUID,Map<String,Long>>)field(owner,map);var timer=new HashMap<String,Long>();values.put(p.getUniqueId(),timer);
  timer.put(ability,System.currentTimeMillis()+25000);
  check((boolean)call(owner,guard,new Class[]{Player.class,String.class},p,ability),ability+" blocks repeat activation");
  timer.put(ability,System.currentTimeMillis()-1);
  check(!(boolean)call(owner,guard,new Class[]{Player.class,String.class},p,ability),ability+" expires");values.remove(p.getUniqueId());
 }
 public static void run(RunesAltarV plugin)throws Exception{
  UUID id=UUID.randomUUID();ItemStack[] held={AltarCatalog.WEAPONS.stream().filter(e->e.id().equals("vulcan_crossbow")).findFirst().orElseThrow().factory().get()};
  PlayerInventory inv=proxy(PlayerInventory.class,(o,m,a)->switch(m.getName()){case "getItemInMainHand"->held[0];case "getItemInOffHand"->new ItemStack(Material.AIR);default->defaults(m.getReturnType());});
  int[] cooldown={0},cleared={0};
  Player p=proxy(Player.class,(o,m,a)->switch(m.getName()){
   case "getUniqueId"->id;case "getInventory"->inv;case "isOnline","isSneaking"->true;
   case "getEyeLocation","getLocation"->Bukkit.getWorlds().getFirst().getSpawnLocation();
   case "hasCooldown"->cooldown[0]>0;case "getCooldown"->cooldown[0];case "setCooldown"->{if((int)a[1]==0)cleared[0]++;cooldown[0]=(int)a[1];yield null;}
   case "hashCode"->id.hashCode();case "equals"->o==a[0];default->defaults(m.getReturnType());});
  var bar=AltarCooldowns.cooldownBar("Test",org.bukkit.boss.BarColor.YELLOW,org.bukkit.boss.BarStyle.SOLID);
  check(!bar.isVisible(),"cooldown stays in chat while gameplay timer remains enabled");bar.removeAll();
  VulcanCooldownManager.startCooldown(p,25);
  check(VulcanCooldownManager.isOnCooldown(p),"Vulcan cooldown starts");
  check(VulcanCooldownManager.remainingMillis(p)>24000,"Vulcan reports real 25 seconds, not 50 ms");
  var projectile=proxy(Entity.class,(o,m,a)->defaults(m.getReturnType()));
  var shot=new EntityShootBowEvent(p,held[0],new ItemStack(Material.ARROW),projectile,EquipmentSlot.HAND,1f,true);
  new VulcanShootListener().onShoot(shot);
  check(!shot.isCancelled(),"Vulcan blocks repeated magma but retains ordinary shot");
  ((Map<?,?>)fieldStatic(VulcanCooldownManager.class,"activeBars")).clear();
  check(VulcanCooldownManager.isOnCooldown(p),"removing Vulcan display cannot reset cooldown");
  ((Map<UUID,Long>)fieldStatic(VulcanCooldownManager.class,"cooldownEnds")).put(id,System.currentTimeMillis()-1);
  check(!VulcanCooldownManager.isOnCooldown(p),"Vulcan expires by deadline");VulcanCooldownManager.resetPlayer(p);
  var pure=(PureBossBarManager)field(plugin,"pureBossBarManager");
  pure.setCooldown(p,"ShadeSoul","Test",45);pure.setCooldown(p,"CycloneSlash","Test",30);
  check(pure.remainingMillis(p,"ShadeSoul")>44000&&pure.remainingMillis(p,"CycloneSlash")>29000,"Pure Blade has independent accurate cooldowns");
  ((Map<?,?>)field(pure,"activeBars")).clear();
  check(pure.isOnCooldown(p,"ShadeSoul")&&pure.isOnCooldown(p,"CycloneSlash"),"Pure Blade cooldowns independent of display");
  pure.resetPlayer(p);check(!pure.isOnCooldown(p,"ShadeSoul")&&!pure.isOnCooldown(p,"CycloneSlash"),"explicit reset clears Pure Blade cooldowns");
  flat(field(plugin,"boneCooldownManager"),"dashCooldowns","isOnDashCooldown",p);flat(field(plugin,"boneCooldownManager"),"cageCooldowns","isOnCageCooldown",p);
  for(String a:List.of("BloodHook","BloodTrail","Infection"))nested(field(plugin,"bloodBossBarManager"),"cooldownEnds","isOnCooldown",a,p);
  for(String a:List.of("ScytheThrow","CommandOfIce"))nested(field(plugin,"frostBossBarManager"),"cooldownEnds","isOnCooldown",a,p);
  Object earthBars=field(field(plugin,"earthGauntletManager"),"bossBarManager");for(String a:List.of("MeteorStrike","Mudslide"))nested(earthBars,"cooldowns","isOnCooldown",a,p);
  flat(field(plugin,"paleGunAbilityManager"),"rootsCooldowns","isRootsOnCooldown",p);
  flat(field(plugin,"shadowBladeManager"),"leapCooldowns","isOnLeapCooldown",p);flat(field(plugin,"shadowBladeManager"),"daggersCooldowns","isOnDaggersCooldown",p);
  flat(field(plugin,"windWeaverListener"),"leapCooldowns","onLeapCooldown",p);flat(field(plugin,"windWeaverListener"),"burstCooldowns","onBurstCooldown",p);
  flat(field(plugin,"witherManager"),"auraCooldowns","isOnAuraCooldown",p);flat(field(plugin,"crazySlotsManager"),"cooldowns","isOnCooldown",p);
  var hyperion=dev.vaileks.runesaltarv.weapons.hyperion.managers.HyperionCooldownManager.class;
  flat(hyperion,"scorchingCooldowns","isOnScorchingCooldown",p);flat(hyperion,"holyLanceCooldowns","isOnHolyLanceCooldown",p);
  for(String ability:List.of("parry","thousand_cuts"))keyed(field(plugin,"cutlassManager"),"checkCooldown",ability,p);
  for(String ability:List.of("earth_shatter","stalwart_absorption"))keyed(field(plugin,"paladinsBattleAxeManager"),"isOnCooldown",ability,p);
  Object night=field(plugin,"nightAbilityListener");
  for(String name:List.of("biteCooldowns","transformationCooldowns")){
   Map<UUID,Long> values=(Map<UUID,Long>)field(night,name);values.put(id,System.currentTimeMillis()+25000);
   check((boolean)call(night,"isOnCooldown",new Class[]{Player.class,Map.class},p,values),"Nightpiercer "+name+" blocks repeat");
   values.put(id,System.currentTimeMillis()-1);check(!(boolean)call(night,"isOnCooldown",new Class[]{Player.class,Map.class},p,values),"Nightpiercer "+name+" expires");values.remove(id);
  }
  Object knight=field(plugin,"knightfallManager");
  Map<UUID,Long> throwsMap=(Map<UUID,Long>)field(knight,"throwCooldowns");throwsMap.put(id,System.currentTimeMillis()+25000);
  call(knight,"throwHammer",new Class[]{Player.class,ItemStack.class},p,null);
  check(throwsMap.get(id)>System.currentTimeMillis(),"Knightfall rejects throw before touching target/item");throwsMap.remove(id);
  held[0]=AltarCatalog.WEAPONS.stream().filter(e->e.id().equals("pale_gun")).findFirst().orElseThrow().factory().get();cooldown[0]=10;
  var pale=new PaleGunListener(plugin,plugin.getPaleGunAbilityManager());
  var click=new PlayerInteractEvent(p,Action.RIGHT_CLICK_AIR,held[0],null,org.bukkit.block.BlockFace.SELF,EquipmentSlot.HAND);
  pale.onInteract(click);check(click.isCancelled()&&cleared[0]==0&&cooldown[0]==10,"Pale Gun retains final second of cooldown");
  shot=new EntityShootBowEvent(p,held[0],new ItemStack(Material.ARROW),projectile,EquipmentSlot.HAND,1f,true);pale.onShoot(shot);
  check(shot.isCancelled()&&cooldown[0]==10,"Pale Gun rejects shoot-event cooldown bypass");
  for(var entry:AltarCatalog.WEAPONS){var cfg=AltarPresets.get(entry.id());boolean found=false;for(String path:cfg.getKeys(true))if(path.endsWith("cooldown")){check(cfg.getInt(path)>0,"positive configured cooldown: "+entry.id()+"/"+path);found=true;}check(found,"cooldown configured for "+entry.id());}
  AltarCooldowns.clear(p);plugin.getLogger().info("ALTAR_COOLDOWN_PASS checks="+checks+" weapons=16");
 }
 @SuppressWarnings("unchecked") static void keyed(Object owner,String guard,String ability,Player p)throws Exception{
  Map<String,Long> values=(Map<String,Long>)field(owner,"cooldowns");String key=p.getUniqueId()+":"+ability;
  values.put(key,System.currentTimeMillis()+25000);check((boolean)call(owner,guard,new Class[]{Player.class,String.class},p,ability),ability+" blocks repeat");
  values.put(key,System.currentTimeMillis()-1);check(!(boolean)call(owner,guard,new Class[]{Player.class,String.class},p,ability),ability+" expires");values.remove(key);
 }
 static Object fieldStatic(Class<?> owner,String name)throws Exception{Field f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
}

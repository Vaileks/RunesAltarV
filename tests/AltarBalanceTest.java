package test;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.integration.*;
import dev.vaileks.runesaltarv.utils.CombatUtils;
import dev.vaileks.runesaltarv.weapons.hyperion.tasks.HyperionTasks;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;
import org.bukkit.*;import org.bukkit.block.Block;import org.bukkit.block.data.BlockData;import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Zombie;import org.bukkit.inventory.ItemStack;import org.bukkit.enchantments.Enchantment;

public final class AltarBalanceTest {
 static int checks;static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 public static void run(RunesAltarV plugin)throws Exception{
  for(var entry:AltarCatalog.WEAPONS){
   var active=AltarPresets.get(entry.id());
   var file=plugin.getDataFolder().toPath().resolve("items/"+entry.id()+".yml");
   check(Files.isRegularFile(file),"own editable weapon file: "+entry.id());
   byte[] original=Files.readAllBytes(file);
   active.set("abilities.test-user-value",123);active.save(file.toFile());
   AltarPresets.load(plugin);check(AltarPresets.get(entry.id()).getInt("abilities.test-user-value")==123,"administrator edits survive reload");
   Files.write(file,original);
  }
  AltarPresets.load(plugin);plugin.reloadWeaponsConfig();
  originalValues(plugin);
  var cfg=plugin.getWeaponsConfig();check(cfg.getInt("vulcan_crossbow.magma-attack.cooldown")==25,"Vulcan cooldown restored to 25s");check(cfg.getDouble("vulcan_crossbow.damage")==18,"Vulcan ordinary bonus restored");
  check(cfg.getInt("frost-scythe.throw.cooldown")==45&&cfg.getDouble("frost-scythe.throw.damage")==20,"frost throw restored to 45s / 20 damage");
  check(cfg.getDouble("windweaver.wind-burst.radius")==6&&cfg.getDouble("wither-blade.aura.radius")==4,"room-scale aura radii");
  check(cfg.getInt("pale-gun.pale-roots.max-hits-per-target")==3&&cfg.getInt("pale-gun.pale-roots.contact-hit-cooldown-ticks")==10,"root contact budget and interval configured");
  check(!cfg.getBoolean("hyperion.scorching-blade.place-real-lava"),"wave retains display lava without placing damaging lava blocks");
  var paladin=plugin.getPaladinsBattleAxeManager();
  for(double damage:new double[]{0,1,5,10,30,1000})check(paladin.getShockwaveDamage(damage)>=4&&paladin.getShockwaveDamage(damage)<=10,"original shockwave range 4-10HP");
  check(paladin.getShockwaveDamage(5)==4&&paladin.getShockwaveDamage(1000)==10,"shockwave scaling and ceiling");
  var world=Bukkit.getWorlds().getFirst();Location base=world.getSpawnLocation().clone().add(0,7,8);base.setX(base.getBlockX()+.5);base.setZ(base.getBlockZ()+.5);
  Map<Block,BlockData> restored=new LinkedHashMap<>();Zombie target=null;
  try{
   for(int x=0;x<=4;x++)for(int y=0;y<=2;y++){
    Block b=base.clone().add(x,y,0).getBlock();restored.put(b,b.getBlockData().clone());b.setType(Material.AIR,false);
   }
   target=world.spawn(base.clone().add(4,0,0),Zombie.class,z->{z.setAI(false);z.setSilent(true);});
   Location from=base.clone().add(0,1,0);
   check(CombatUtils.clearPath(from,target),"open space permits area hit");
   check(CombatUtils.clearPath(from,from.clone()),"zero-length ray is safe");
   for(int y=0;y<=2;y++)base.clone().add(2,y,0).getBlock().setType(Material.STONE,false);
   check(!CombatUtils.clearPath(from,target),"solid wall blocks room-to-room area hit");
   for(int y=0;y<=2;y++)base.clone().add(2,y,0).getBlock().setType(Material.AIR,false);
   check(CombatUtils.clearPath(from,target),"opening cover permits hit again");
   ItemStack[] armor=new ItemStack[]{new ItemStack(Material.DIAMOND_BOOTS),new ItemStack(Material.DIAMOND_LEGGINGS),new ItemStack(Material.DIAMOND_CHESTPLATE),new ItemStack(Material.DIAMOND_HELMET)};
   for(ItemStack piece:armor)piece.addEnchantment(Enchantment.PROTECTION,3);target.getEquipment().setArmorContents(armor);
   double health=target.getHealth();plugin.getCleanDamageManager().apply(target,paladin.getShockwaveDamage(1000));
   check(Math.abs(target.getHealth()-(health-10))<.001,"original pure shockwave deals 10HP to diamond Protection III");
   Set<Integer> before=new HashSet<>();Bukkit.getScheduler().getPendingTasks().forEach(t->before.add(t.getTaskId()));
   HyperionTasks.ignite(target,null);long once=Bukkit.getScheduler().getPendingTasks().stream().filter(t->!before.contains(t.getTaskId())).count();
   HyperionTasks.ignite(target,null);long twice=Bukkit.getScheduler().getPendingTasks().stream().filter(t->!before.contains(t.getTaskId())).count();
   check(once==1&&twice==1,"holy flames cannot stack concurrent burn tasks");
   Bukkit.getScheduler().getPendingTasks().stream().filter(t->!before.contains(t.getTaskId())).forEach(t->t.cancel());HyperionTasks.hallowedVictims.remove(target.getUniqueId());
  }finally{if(target!=null)target.remove();restored.forEach((b,data)->b.setBlockData(data,false));}
  plugin.getLogger().info("ALTAR_BALANCE_PASS checks="+checks);
 }
 static void originalValues(RunesAltarV plugin) {
  check(AltarPresets.get("bloodlust").getDouble("abilities.infection.cooldown") == 15, "restored bloodlust abilities.infection.cooldown");
  check(AltarPresets.get("bloodlust").getDouble("abilities.infection.damage") == 2.0, "restored bloodlust abilities.infection.damage");
  check(AltarPresets.get("bloodlust").getDouble("abilities.blood-trail.cooldown") == 60, "restored bloodlust abilities.blood-trail.cooldown");
  check(AltarPresets.get("bloodlust").getDouble("abilities.blood-hook.damage") == 8, "restored bloodlust abilities.blood-hook.damage");
  check(AltarPresets.get("bloodlust").getDouble("abilities.blood-hook.cooldown") == 30, "restored bloodlust abilities.blood-hook.cooldown");
  check(AltarPresets.get("bone_blade").getDouble("abilities.dash.cooldown") == 15, "restored bone_blade abilities.dash.cooldown");
  check(AltarPresets.get("bone_blade").getDouble("abilities.dash.damage") == 4.0, "restored bone_blade abilities.dash.damage");
  check(AltarPresets.get("bone_blade").getDouble("abilities.cage.cooldown") == 45, "restored bone_blade abilities.cage.cooldown");
  check(AltarPresets.get("crazyslots").getDouble("abilities.cooldown") == 60, "restored crazyslots abilities.cooldown");
  check(AltarPresets.get("cutlass").getDouble("abilities.thousand-cuts.cooldown") == 45, "restored cutlass abilities.thousand-cuts.cooldown");
  check(AltarPresets.get("cutlass").getDouble("abilities.thousand-cuts.hit-damage") == 1.0, "restored cutlass abilities.thousand-cuts.hit-damage");
  check(AltarPresets.get("cutlass").getDouble("abilities.parry.cooldown") == 30, "restored cutlass abilities.parry.cooldown");
  check(AltarPresets.get("earth_gauntlet").getDouble("abilities.meteor_strike.cooldown") == 35, "restored earth_gauntlet abilities.meteor_strike.cooldown");
  check(AltarPresets.get("earth_gauntlet").getDouble("abilities.meteor_strike.damage") == 4.0, "restored earth_gauntlet abilities.meteor_strike.damage");
  check(AltarPresets.get("earth_gauntlet").getDouble("abilities.mudslide.cooldown") == 40, "restored earth_gauntlet abilities.mudslide.cooldown");
  check(AltarPresets.get("earth_gauntlet").getDouble("abilities.mudslide.damage") == 4.0, "restored earth_gauntlet abilities.mudslide.damage");
  check(AltarPresets.get("frost_scythe").getDouble("abilities.throw.cooldown") == 45, "restored frost_scythe abilities.throw.cooldown");
  check(AltarPresets.get("frost_scythe").getDouble("abilities.throw.damage") == 20.0, "restored frost_scythe abilities.throw.damage");
  check(AltarPresets.get("frost_scythe").getDouble("abilities.ice-command.cooldown") == 65, "restored frost_scythe abilities.ice-command.cooldown");
  check(AltarPresets.get("frost_scythe").getDouble("abilities.ice-command.damage") == 15.0, "restored frost_scythe abilities.ice-command.damage");
  check(AltarPresets.get("frost_scythe").getDouble("abilities.freeze.damage") == 3.5, "restored frost_scythe abilities.freeze.damage");
  check(AltarPresets.get("hyperion").getDouble("abilities.scorching-blade.cooldown") == 30, "restored hyperion abilities.scorching-blade.cooldown");
  check(AltarPresets.get("hyperion").getDouble("abilities.scorching-blade.pure-damage") == 2.0, "restored hyperion abilities.scorching-blade.pure-damage");
  check(AltarPresets.get("hyperion").getDouble("abilities.holy-lance.cooldown") == 60, "restored hyperion abilities.holy-lance.cooldown");
  check(AltarPresets.get("hyperion").getDouble("abilities.holy-lance.radius-damage") == 2.5, "restored hyperion abilities.holy-lance.radius-damage");
  check(AltarPresets.get("hyperion").getDouble("abilities.holy-lance.center-damage") == 6.0, "restored hyperion abilities.holy-lance.center-damage");
  check(AltarPresets.get("knightfall").getDouble("abilities.cloak.cooldown") == 8, "restored knightfall abilities.cloak.cooldown");
  check(AltarPresets.get("knightfall").getDouble("abilities.grapple.cooldown") == 10, "restored knightfall abilities.grapple.cooldown");
  check(AltarPresets.get("knightfall").getDouble("abilities.throw.cooldown") == 20, "restored knightfall abilities.throw.cooldown");
  check(AltarPresets.get("knightfall").getDouble("abilities.throw.damage") == 7.0, "restored knightfall abilities.throw.damage");
  check(AltarPresets.get("knightfall").getDouble("abilities.knightfall-damage-cap.max-damage-hearts") == 6.0, "restored knightfall abilities.knightfall-damage-cap.max-damage-hearts");
  check(AltarPresets.get("nightpiercer").getDouble("abilities.bite.cooldown") == 45, "restored nightpiercer abilities.bite.cooldown");
  check(AltarPresets.get("nightpiercer").getDouble("abilities.bite.damage") == 4.0, "restored nightpiercer abilities.bite.damage");
  check(AltarPresets.get("nightpiercer").getDouble("abilities.transformation.cooldown") == 20, "restored nightpiercer abilities.transformation.cooldown");
  check(AltarPresets.get("nightpiercer").getDouble("abilities.transformation.damage") == 4.0, "restored nightpiercer abilities.transformation.damage");
  check(AltarPresets.get("paladins_battle_axe").getDouble("abilities.base.damage") == 10.0, "restored paladins_battle_axe abilities.base.damage");
  check(AltarPresets.get("paladins_battle_axe").getDouble("abilities.earth-shatter.cooldown") == 45, "restored paladins_battle_axe abilities.earth-shatter.cooldown");
  check(AltarPresets.get("paladins_battle_axe").getDouble("abilities.earth-shatter.damage") == 7.0, "restored paladins_battle_axe abilities.earth-shatter.damage");
  check(AltarPresets.get("paladins_battle_axe").getDouble("abilities.stalwart-absorption.cooldown") == 45, "restored paladins_battle_axe abilities.stalwart-absorption.cooldown");
  check(AltarPresets.get("paladins_battle_axe").getDouble("abilities.stalwart-absorption.damage-cap") == 30.0, "restored paladins_battle_axe abilities.stalwart-absorption.damage-cap");
  check(AltarPresets.get("paladins_battle_axe").getDouble("abilities.stalwart-absorption.min-shockwave-damage") == 4.0, "restored paladins_battle_axe abilities.stalwart-absorption.min-shockwave-damage");
  check(AltarPresets.get("pale_gun").getDouble("abilities.shoot.cooldown") == 10, "restored pale_gun abilities.shoot.cooldown");
  check(AltarPresets.get("pale_gun").getDouble("abilities.shoot.explosion-power") == 3.2, "restored pale_gun abilities.shoot.explosion-power");
  check(AltarPresets.get("pale_gun").getDouble("abilities.pale-roots.cooldown") == 25, "restored pale_gun abilities.pale-roots.cooldown");
  check(AltarPresets.get("pale_gun").getDouble("abilities.pale-roots.damage") == 2.0, "restored pale_gun abilities.pale-roots.damage");
  check(AltarPresets.get("pale_gun").getDouble("abilities.pale-roots.contact-hit-cooldown-ticks") == 10, "restored pale_gun abilities.pale-roots.contact-hit-cooldown-ticks");
  check(AltarPresets.get("pure_blade").getDouble("abilities.shade-soul.cooldown") == 45, "restored pure_blade abilities.shade-soul.cooldown");
  check(AltarPresets.get("pure_blade").getDouble("abilities.shade-soul.damage") == 4.0, "restored pure_blade abilities.shade-soul.damage");
  check(AltarPresets.get("pure_blade").getDouble("abilities.cyclone-slash.cooldown") == 30, "restored pure_blade abilities.cyclone-slash.cooldown");
  check(AltarPresets.get("pure_blade").getDouble("abilities.cyclone-slash.damage") == 12.0, "restored pure_blade abilities.cyclone-slash.damage");
  check(AltarPresets.get("shadow_blade").getDouble("abilities.leap.cooldown") == 30, "restored shadow_blade abilities.leap.cooldown");
  check(AltarPresets.get("shadow_blade").getDouble("abilities.daggers.cooldown") == 45, "restored shadow_blade abilities.daggers.cooldown");
  check(AltarPresets.get("shadow_blade").getDouble("abilities.daggers.damage") == 2.0, "restored shadow_blade abilities.daggers.damage");
  check(AltarPresets.get("shadow_blade").getDouble("abilities.passive.backstab-cooldown") == 4, "restored shadow_blade abilities.passive.backstab-cooldown");
  check(AltarPresets.get("vulcan_crossbow").getDouble("abilities.damage") == 18.0, "restored vulcan_crossbow abilities.damage");
  check(AltarPresets.get("vulcan_crossbow").getDouble("abilities.magma-attack.cooldown") == 25, "restored vulcan_crossbow abilities.magma-attack.cooldown");
  check(AltarPresets.get("vulcan_crossbow").getDouble("abilities.magma-attack.explosion-power") == 2.5, "restored vulcan_crossbow abilities.magma-attack.explosion-power");
  check(AltarPresets.get("windweaver").getDouble("abilities.wind-leap.cooldown") == 15, "restored windweaver abilities.wind-leap.cooldown");
  check(AltarPresets.get("windweaver").getDouble("abilities.wind-burst.cooldown") == 15, "restored windweaver abilities.wind-burst.cooldown");
  check(AltarPresets.get("wither_blade").getDouble("abilities.dash.cooldown") == 30, "restored wither_blade abilities.dash.cooldown");
  check(AltarPresets.get("wither_blade").getDouble("abilities.aura.cooldown") == 50, "restored wither_blade abilities.aura.cooldown");
  check(AltarPresets.get("wither_blade").getDouble("abilities.aura.max-damage") == 10.0, "restored wither_blade abilities.aura.max-damage");
 }

}

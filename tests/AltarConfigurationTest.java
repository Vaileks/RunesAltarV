package test;
import dev.vaileks.runesaltarv.RunesAltarV;import dev.vaileks.runesaltarv.integration.*;import java.nio.file.*;import java.util.*;import org.bukkit.*;import org.bukkit.attribute.Attribute;import org.bukkit.configuration.file.YamlConfiguration;
public final class AltarConfigurationTest {
 static int checks;static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 static double stat(org.bukkit.inventory.ItemStack item,Attribute attribute,double base){var meta=item.getItemMeta();var list=meta.getAttributeModifiers(attribute);if(list==null)list=item.getType().getDefaultAttributeModifiers(org.bukkit.inventory.EquipmentSlot.HAND).get(attribute);double add=0,scalar=0,mul=1;for(var m:list)if(m.getSlotGroup().test(org.bukkit.inventory.EquipmentSlot.HAND))switch(m.getOperation()){case ADD_NUMBER->add+=m.getAmount();case ADD_SCALAR->scalar+=m.getAmount();case MULTIPLY_SCALAR_1->mul*=1+m.getAmount();}return (base+add)*(1+scalar)*mul;}
 public static void run(RunesAltarV plugin)throws Exception{
  for(var entry:AltarCatalog.WEAPONS){var cfg=AltarPresets.get(entry.id());check(cfg.contains("visuals.particle-density"),"particle settings "+entry.id());check(cfg.contains("item.attack-damage-bonus"),"attribute settings "+entry.id());
   try(var stream=plugin.getResource("defaults/"+entry.id()+".yml")){var defaults=YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));for(String path:defaults.getKeys(true))if(!defaults.isConfigurationSection(path))check(cfg.contains(path),"complete editable settings "+entry.id()+"/"+path);}
  }
  var bone=AltarCatalog.WEAPONS.stream().filter(e->e.id().equals("bone_blade")).findFirst().orElseThrow();var cfg=AltarPresets.get("bone_blade");
  var before=bone.factory().get();double base=stat(before,Attribute.ATTACK_DAMAGE,1);cfg.set("item.attack-damage-bonus",2.5);cfg.set("item.attack-speed-bonus",.2);
  var tuned=bone.factory().get();check(Math.abs(stat(tuned,Attribute.ATTACK_DAMAGE,1)-base-2.5)<.001,"configured damage modifier applied");AltarPresentation.decorate(tuned);AltarPresentation.decorate(tuned);check(Math.abs(stat(tuned,Attribute.ATTACK_DAMAGE,1)-base-2.5)<.001,"attribute refresh does not stack bonuses");
  cfg.set("item.attack-damage-bonus",0);cfg.set("item.attack-speed-bonus",0);AltarPresentation.decorate(tuned);check(Math.abs(stat(tuned,Attribute.ATTACK_DAMAGE,1)-base)<.001,"bonus can be removed on reload");
  cfg.set("visuals.particle-density",.5);check(AltarTuning.particles("bone_blade",10)==5,"particle density used");cfg.set("visuals.particle-density",1);
  cfg.set("tuning.test",Double.NaN);check(AltarTuning.number("bone_blade","tuning.test",2,0,5)==2,"non-finite config falls back");cfg.set("tuning.test",9999);check(AltarTuning.number("bone_blade","tuning.test",2,0,5)==5,"extreme tuning clamped");cfg.set("tuning.test",null);
  cfg.set("visuals.sounds.test.sound","NOT_A_SOUND");check(AltarTuning.sound("bone_blade","visuals.sounds.test.sound",Sound.ENTITY_SKELETON_DEATH).equals(Sound.ENTITY_SKELETON_DEATH.getKey().asString()),"invalid sound safe fallback");cfg.set("visuals.sounds.test.volume-multiplier",.5);check(AltarTuning.volume("bone_blade","visuals.sounds.test",1f)==.5f,"sound volume editable");cfg.set("visuals.sounds.test",null);
  cfg.set("messages.test.ru","Тест");cfg.set("messages.test.en","Test");boolean english=plugin.isEnglish();check(AltarTuning.text("bone_blade","messages.test","fallback","fallback").equals(english?"Test":"Тест"),"messages configurable");cfg.set("messages.test",null);
  // Re-read a deliberately old/sparse file; additions must appear without changing its custom balance.
  Path path=plugin.getDataFolder().toPath().resolve("items/bone_blade.yml");
  byte[] saved=Files.readAllBytes(path);
  try{var sparse=new YamlConfiguration();sparse.set("name","Custom keeper");sparse.set("abilities.dash.cooldown",77);sparse.set("balance-revision",2);sparse.save(path.toFile());AltarPresets.load(plugin);
   var updated=YamlConfiguration.loadConfiguration(path.toFile());check(updated.getInt("abilities.dash.cooldown")==77&&updated.getString("name").equals("Custom keeper"),"administrator settings preserved");check(updated.contains("visuals.particle-density")&&updated.contains("abilities.dash.velocity"),"missing settings written to disk");
  }finally{Files.write(path,saved);AltarPresets.load(plugin);plugin.reloadWeaponsConfig();plugin.reloadModelsConfig();}
  plugin.getLogger().info("ALTAR_CONFIGURATION_PASS checks="+checks);
 }
}

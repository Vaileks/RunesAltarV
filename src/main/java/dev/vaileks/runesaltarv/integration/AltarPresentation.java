package dev.vaileks.runesaltarv.integration;

import java.util.*;
import dev.vaileks.runesaltarv.RunesAltarV;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.*;
import org.bukkit.enchantments.Enchantment;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.*;

public final class AltarPresentation {
 private static Component line(String value,NamedTextColor color){return Component.text(value,color).decoration(TextDecoration.ITALIC,false);}
 private static void wrap(List<Component> result,String value,NamedTextColor color){StringBuilder row=new StringBuilder();for(String word:value.split(" +")){if(row.length()+word.length()+1>38 && !row.isEmpty()){result.add(line(row.toString(),color));row.setLength(0);}if(!row.isEmpty())row.append(' ');row.append(word);}if(!row.isEmpty())result.add(line(row.toString(),color));}
 public static ItemStack decorate(ItemStack item){
  if(item==null||item.getType().isAir()||!item.hasItemMeta())return item;
  var entry=AltarCatalog.find(item);if(entry==null)return item;
  var config=AltarPresets.get(entry.id());if(config==null)return item;
  var plugin=RunesAltarV.getInstance();boolean english=plugin.isEnglish();String suffix=english?"-en":"";
  var meta=item.getItemMeta();
  applyAttributes(item,meta,entry.id());meta.displayName(line(config.getString("name"+suffix,config.getString("name",entry.id())),NamedTextColor.GOLD));
  List<Component> lore=new ArrayList<>();
  for(String value:config.getStringList("lore-description"+suffix))wrap(lore,value,NamedTextColor.GRAY);
  lore.add(line("",NamedTextColor.GRAY));
  for(String value:config.getStringList("lore-abilities"+suffix)) {
   int split=value.indexOf(" — ");if(split<0){wrap(lore,value,NamedTextColor.GRAY);continue;}
   List<Component> rows=new ArrayList<>();wrap(rows,value.substring(split+3),NamedTextColor.GRAY);
   if(rows.isEmpty())rows.add(Component.empty());lore.add(line(value.substring(0,split)+" — ",NamedTextColor.WHITE).append(rows.getFirst()));lore.addAll(rows.subList(1,rows.size()));
  }
  lore.add(line("",NamedTextColor.GRAY));lore.add(line(plugin.tr("Когда в ведущей руке:","When in Main Hand:"),NamedTextColor.GRAY));
  lore.add(line(plugin.tr(" Урон при атаке "," Attack Damage ")+number(value(item,meta,Attribute.ATTACK_DAMAGE,1)),NamedTextColor.DARK_GREEN));
  lore.add(line(plugin.tr(" Скорость атаки "," Attack Speed ")+number(value(item,meta,Attribute.ATTACK_SPEED,4)),NamedTextColor.DARK_GREEN));
  meta.lore(lore);meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,ItemFlag.HIDE_UNBREAKABLE,ItemFlag.HIDE_ENCHANTS);meta.setUnbreakable(true);item.setItemMeta(meta);return item;
 }
 private static String number(double value){return java.math.BigDecimal.valueOf(value).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
 static double value(ItemStack item,ItemMeta meta,Attribute attribute,double base){
  var modifiers=meta.getAttributeModifiers(attribute);
  if(modifiers==null)modifiers=item.getType().getDefaultAttributeModifiers(EquipmentSlot.HAND).get(attribute);
  double add=0,scalar=0,multiplier=1;
  if(modifiers!=null)for(var m:modifiers){if(!m.getSlotGroup().test(EquipmentSlot.HAND))continue;switch(m.getOperation()){case ADD_NUMBER->add+=m.getAmount();case ADD_SCALAR->scalar+=m.getAmount();case MULTIPLY_SCALAR_1->multiplier*=1+m.getAmount();}}
  double result=(base+add)*(1+scalar)*multiplier;
  int sharp=meta.getEnchantLevel(Enchantment.SHARPNESS);if(attribute.equals(Attribute.ATTACK_DAMAGE)&&sharp>0)result+=.5*sharp+.5;
  return result;
 }
 private static void applyAttributes(ItemStack item,org.bukkit.inventory.meta.ItemMeta meta,String id){
  for(var attribute:new org.bukkit.attribute.Attribute[]{org.bukkit.attribute.Attribute.ATTACK_DAMAGE,org.bukkit.attribute.Attribute.ATTACK_SPEED}){
   String suffix=attribute.equals(org.bukkit.attribute.Attribute.ATTACK_DAMAGE)?"damage":"speed";
   var key=new org.bukkit.NamespacedKey("runesaltarv","configured_"+suffix);
   var modifiers=meta.getAttributeModifiers(attribute);
   if(modifiers!=null)for(var modifier:new java.util.ArrayList<>(modifiers))if(modifier.getKey().equals(key))meta.removeAttributeModifier(attribute,modifier);
   double amount=AltarTuning.number(id,"item.attack-"+suffix+"-bonus",0,-20,100);
   if(amount!=0){
    if(meta.getAttributeModifiers(attribute)==null)for(var modifier:item.getType().getDefaultAttributeModifiers(org.bukkit.inventory.EquipmentSlot.HAND).get(attribute))meta.addAttributeModifier(attribute,modifier);
    meta.addAttributeModifier(attribute,new org.bukkit.attribute.AttributeModifier(key,amount,org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,org.bukkit.inventory.EquipmentSlotGroup.MAINHAND));
   }
  }
 }
 private AltarPresentation(){}
}

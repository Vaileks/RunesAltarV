package dev.vaileks.runesaltarv.integration;
import org.bukkit.Sound;
public final class AltarTuning {
 public static double number(String id,String path,double fallback,double min,double max){var c=AltarPresets.get(id);double n=c==null?fallback:c.getDouble(path,fallback);return Double.isFinite(n)?Math.max(min,Math.min(max,n)):fallback;}
 public static int integer(String id,String path,int fallback,int min,int max){return (int)number(id,path,fallback,min,max);}
 public static String sound(String id,String path,Sound fallback){
  var c=AltarPresets.get(id);String configured=c==null?fallback.name():c.getString(path,fallback.name());
  if(configured.indexOf(':')>=0)return org.bukkit.NamespacedKey.fromString(configured)!=null?configured:fallback.getKey().asString();
  try{return Sound.valueOf(configured.toUpperCase(java.util.Locale.ROOT)).getKey().asString();}catch(IllegalArgumentException ex){return fallback.getKey().asString();}
 }
 public static float volume(String id,String path,float original){return original*(float)number(id,path+".volume-multiplier",1,0,4);}
 public static float pitch(String id,String path,float original){return Math.max(0f,Math.min(2f,original*(float)number(id,path+".pitch-multiplier",1,.01,2)));}
 public static int particles(String id,int original){return Math.max(0,Math.min(4096,(int)Math.round(original*number(id,"visuals.particle-density",1,0,4))));}
 public static String text(String id,String path,String ru,String en){var c=AltarPresets.get(id);boolean english=dev.vaileks.runesaltarv.RunesAltarV.getInstance().isEnglish();return c==null?(english?en:ru):c.getString(path+(english?".en":".ru"),english?en:ru);}
 private AltarTuning(){}
}

package dev.vaileks.runesaltarv.utils;

import java.lang.reflect.*;
import java.util.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

/** Exercises the real shared renderer on Paper; no client-side boss bars are permitted. */
public final class AltarActionBarTest {
   static int checks;
   static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
   private static void invoke(String method){
      try { var m=Class.forName("dev.vaileks.runesaltarv.utils.ShadowBossBar").getDeclaredMethod(method);m.setAccessible(true);m.invoke(null); }
      catch(ReflectiveOperationException e){throw new AssertionError(e);}
   }
   public static void run(){
      invoke("clearAll");
      List<Component> packets=new ArrayList<>();int[] nativeBars={0};boolean[] online={true};UUID id=UUID.randomUUID();
      Player player=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class[]{Player.class},(o,m,a)->{
         return switch(m.getName()){
            case "getUniqueId" -> id;
            case "isOnline" -> online[0];
            case "hashCode" -> id.hashCode();
            case "equals" -> o==a[0];
            case "showBossBar","hideBossBar" -> {nativeBars[0]++;yield null;}
            case "sendActionBar" -> {packets.add((Component)a[0]);yield null;}
            default -> m.getReturnType()==boolean.class?false:m.getReturnType()==int.class?0:null;
         };
      });
      BossBar cooldown=TextUtils.cooldownBar("§e§lОползень",BarColor.YELLOW,BarStyle.SOLID);
      cooldown.addPlayer(player);cooldown.setProgress(.8);
      check(!cooldown.isVisible(),"cooldown action bar is disabled");
      BossBar active=TextUtils.bossBar("§a§lМетеоритный удар",BarColor.GREEN,BarStyle.SOLID);
      active.addPlayer(player);active.setProgress(.5);
      check(packets.isEmpty(),"progress changes are coalesced, not sent every tick");
      invoke("render");
      check(packets.size()==1,"one action bar packet combines simultaneous indicators");
      String text=TextUtils.toPlain(packets.getLast());
      check(text.contains("Метеоритный удар")&&text.contains("50%")&&!text.contains("Оползень"),"active ability and cooldown have distinct readable labels");
      check(!text.contains("КД"),"cooldowns never enter the action bar");
      check(active.getProgress()==.5&&cooldown.getProgress()==.8,"renderer preserves original progress state");
      active.hide();invoke("render");text=TextUtils.toPlain(packets.getLast());
      check(text.isEmpty(),"hidden effect does not reveal a cooldown HUD");
      active.show();cooldown.removeAll();invoke("render");text=TextUtils.toPlain(packets.getLast());
      check(text.contains("Метеоритный")&&!text.contains("Оползень"),"removing cooldown does not clear active effect");
      BossBar debuff=TextUtils.bossBar("§c!! Связан !!",BarColor.RED,BarStyle.SOLID);debuff.addPlayer(player);
      BossBar extra=TextUtils.cooldownBar("Third",BarColor.YELLOW,BarStyle.SOLID);extra.addPlayer(player);
      invoke("render");text=TextUtils.toPlain(packets.getLast());
      check(text.contains("Связан")&&!text.contains("Third")&&!text.contains("!"),"two active effects take precedence and decorative exclamation marks are removed");
      TextUtils.sendActionBar(player,Component.text("Заряд: 3"));invoke("render");text=TextUtils.toPlain(packets.getLast());
      check(text.contains("Заряд: 3")&&text.contains("Метеоритный"),"existing action bar messages share the renderer");
      TextUtils.sendActionBar(player,Component.empty());invoke("render");
      check(TextUtils.toPlain(packets.getLast()).contains("Метеоритный"),"empty legacy message cannot wipe active ability");
      active.removeAll();debuff.removeAll();extra.removeAll();invoke("render");
      check(TextUtils.toPlain(packets.getLast()).isEmpty(),"last indicator clears once after completion");
      int before=packets.size();invoke("render");check(packets.size()==before,"idle renderer sends no stale or empty spam");
      active.addPlayer(player);active.setTitle("Updated");active.setProgress(.25);invoke("render");
      check(TextUtils.toPlain(packets.getLast()).contains("Updated")&&TextUtils.toPlain(packets.getLast()).contains("25%"),"reused indicator resumes and uses current title/progress");
      online[0]=false;before=packets.size();invoke("render");
      check(packets.size()==before&&active.getPlayers().isEmpty(),"offline player is removed without sending packets");
      check(nativeBars[0]==0,"no native boss bar is ever shown or hidden");
      invoke("clearAll");
      System.out.println("ALTAR_ACTIONBAR_PASS checks="+checks);
   }
}

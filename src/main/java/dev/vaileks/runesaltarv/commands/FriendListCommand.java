package dev.vaileks.runesaltarv.commands;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.utils.TextUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.command.*;
import java.util.Locale;

public final class FriendListCommand implements CommandExecutor {
 private final RunesAltarV plugin;
 public FriendListCommand(RunesAltarV plugin){this.plugin=plugin;}
 private void message(CommandSender sender,String ru,String en){sender.sendMessage(TextUtils.legacy(plugin.tr(ru,en)));}
 public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
  if(!(sender instanceof Player p)){message(sender,"Только для игроков.","Only players can use this command.");return true;}
  var fm=plugin.getFriendManager();
  if(args.length==0){sender.sendMessage("/"+label+" <add|remove|list> <player>");return true;}
  String action=args[0].toLowerCase(Locale.ROOT);
  if(action.equals("list")){message(p,"§aВаши друзья:","§aYour friends:");for(var id:fm.getFriends(p.getUniqueId()))p.sendMessage("§e"+Bukkit.getOfflinePlayer(id).getName());return true;}
  boolean toggle=args.length==1&&!action.equals("add")&&!action.equals("remove");
  if(!toggle&&(!action.equals("add")&&!action.equals("remove")||args.length!=2)){p.sendMessage("/"+label+" <add|remove|list> <player>");return true;}
  String name=toggle?args[0]:args[1];
  OfflinePlayer target=Bukkit.getOfflinePlayer(name);
  boolean friend=fm.isFriend(p.getUniqueId(),target.getUniqueId());
  if(action.equals("remove")||toggle&&friend){
   if(!friend){message(p,"§cЭтого игрока нет в списке.","§cThis player is not in your friend list.");return true;}
   fm.removeFriend(p.getUniqueId(),target.getUniqueId());message(p,"§aИгрок удалён из списка друзей.","§aPlayer removed from your friend list.");return true;
  }
  Player online=Bukkit.getPlayerExact(name);
  if(online==null){message(p,"§cИгрок должен быть онлайн.","§cThe player must be online.");return true;}
  if(online.equals(p)){message(p,"§cНельзя добавить самого себя.","§cYou cannot add yourself.");return true;}
  if(friend){message(p,"§cИгрок уже в списке друзей.","§cThis player is already in your friend list.");return true;}
  if(fm.addFriend(p.getUniqueId(),online.getUniqueId()))message(p,"§aДруг добавлен. Ваши способности его не заденут.","§aFriend added. Your abilities will not hurt them.");
  else message(p,"§cВ списке может быть не более двух друзей.","§cYour friend list can contain at most two players.");return true;
 }
}

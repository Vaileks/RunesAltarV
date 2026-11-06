package dev.vaileks.runesaltarv.commands;

import dev.vaileks.runesaltarv.RunesAltarV;
import dev.vaileks.runesaltarv.integration.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class AltarLegendaryCommand implements CommandExecutor, TabCompleter, Listener {
 private final RunesAltarV plugin;
 public AltarLegendaryCommand(RunesAltarV plugin) { this.plugin = plugin; }
 private String tr(String ru,String en) { return plugin.tr(ru,en); }
 private void message(CommandSender sender,String ru,String en) { sender.sendMessage(Component.text(tr(ru,en),NamedTextColor.GRAY)); }
 public static final class Menu implements InventoryHolder {
  private final Inventory inventory;
  public Menu(RunesAltarV plugin) {
   inventory = Bukkit.createInventory(this, 27, Component.text(plugin.tr("Легендарные предметы", "Legendary weapons")));
   for (int i=0;i<AltarCatalog.WEAPONS.size();i++) inventory.setItem(i, AltarCatalog.WEAPONS.get(i).factory().get());
  }
  public Inventory getInventory() { return inventory; }
 }
 public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
  if(args.length==0 || args[0].equalsIgnoreCase("menu")) {
   if(!(sender instanceof Player p)) { message(sender,"Команда доступна игроку.","Only players can open the menu."); return true; }
   if(!sender.hasPermission("runesaltarv.menu")) { message(sender,"Недостаточно прав.","You do not have permission."); return true; }
   p.openInventory(new Menu(plugin).getInventory()); return true;
  }
  if(!sender.hasPermission("runesaltarv.admin")) { message(sender,"Недостаточно прав.","You do not have permission."); return true; }
  if(args.length==1 && args[0].equalsIgnoreCase("reload")) {
   try { plugin.reloadConfig(); AltarPresets.load(plugin); plugin.reloadWeaponsConfig(); plugin.reloadModelsConfig();
    Bukkit.getOnlinePlayers().forEach(p->{var inv=p.getInventory();for(int i=0;i<inv.getSize();i++){var item=inv.getItem(i);if(AltarCatalog.find(item)!=null)inv.setItem(i,AltarPresentation.decorate(item));}});
    message(sender,"Настройки перезагружены.","Configuration reloaded.");
   } catch(Exception e) { plugin.getLogger().log(java.util.logging.Level.SEVERE,"Reload failed",e); message(sender,"Не удалось прочитать настройки. Проверьте консоль.","Could not read the configuration. Check the console."); }
   return true;
  }
  if(args.length==3 && args[0].equalsIgnoreCase("give")) {
   Player target=Bukkit.getPlayerExact(args[1]); var entry=AltarCatalog.WEAPONS.stream().filter(e->e.id().equalsIgnoreCase(args[2])).findFirst().orElse(null);
   if(target==null){message(sender,"Игрок не найден.","Player not found.");return true;}
   if(entry==null){message(sender,"Неизвестное оружие.","Unknown weapon.");return true;}
   give(target,entry);message(sender,"Предмет выдан: "+entry.id(),"Item given: "+entry.id());return true;
  }
  if(args.length==3 && args[0].equalsIgnoreCase("cooldown") && args[1].equalsIgnoreCase("reset")) {
   Player target=Bukkit.getPlayerExact(args[2]);
   if(target==null){message(sender,"Игрок не найден.","Player not found.");return true;}
   plugin.resetAllCooldowns(target);message(sender,"Кулдауны сброшены.","Cooldowns reset.");return true;
  }
  sender.sendMessage("/"+label+" [menu|give <player> <weapon>|reload|cooldown reset <player>]");return true;
 }
 private void give(Player player,AltarCatalog.Entry entry) { player.getInventory().addItem(entry.factory().get()).values().forEach(item->player.getWorld().dropItemNaturally(player.getLocation(),item)); }
 @EventHandler public void click(InventoryClickEvent event) {
  if(!(event.getView().getTopInventory().getHolder() instanceof Menu))return;
  event.setCancelled(true);
  if(event.getWhoClicked() instanceof Player p && event.getRawSlot()>=0 && event.getRawSlot()<AltarCatalog.WEAPONS.size() && p.hasPermission("runesaltarv.admin")) give(p,AltarCatalog.WEAPONS.get(event.getRawSlot()));
 }
 @EventHandler public void drag(InventoryDragEvent event) { if(event.getView().getTopInventory().getHolder() instanceof Menu) event.setCancelled(true); }
 public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
  List<String> values=List.of();
  if(args.length==1)values=sender.hasPermission("runesaltarv.admin")?List.of("menu","give","reload","cooldown"):List.of("menu");
  else if(sender.hasPermission("runesaltarv.admin")) {
   if(args.length==2 && args[0].equalsIgnoreCase("give") || args.length==3 && args[0].equalsIgnoreCase("cooldown") && args[1].equalsIgnoreCase("reset"))values=Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
   else if(args.length==3 && args[0].equalsIgnoreCase("give"))values=AltarCatalog.WEAPONS.stream().map(AltarCatalog.Entry::id).toList();
   else if(args.length==2 && args[0].equalsIgnoreCase("cooldown"))values=List.of("reset");
  }
  String prefix=args[args.length-1].toLowerCase(Locale.ROOT); return values.stream().filter(v->v.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
 }
}

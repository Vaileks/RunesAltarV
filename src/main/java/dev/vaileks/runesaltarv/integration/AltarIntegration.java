package dev.vaileks.runesaltarv.integration;
import java.util.*;import org.bukkit.*;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.player.*;import org.bukkit.event.inventory.InventoryOpenEvent;import org.bukkit.scheduler.BukkitTask;
import dev.vaileks.runesaltarv.RunesAltarV;
public final class AltarIntegration implements Listener {
 public static void enable(RunesAltarV plugin){
  Bukkit.getPluginManager().registerEvents(new AltarIntegration(),plugin);
  for(Player player:Bukkit.getOnlinePlayers())refresh(player);
 }
 public static void disable(){
  AltarCooldowns.disable();

 }
 private static void refresh(Player player){var inv=player.getInventory();for(int slot=0;slot<inv.getSize();slot++){var item=inv.getItem(slot);if(item!=null&&AltarCatalog.find(item)!=null){AltarPresentation.decorate(item);inv.setItem(slot,item);}}}
 @EventHandler public void join(PlayerJoinEvent e){refresh(e.getPlayer());}
 @EventHandler public void held(PlayerItemHeldEvent e){refresh(e.getPlayer());}
 @EventHandler public void open(InventoryOpenEvent e){if(e.getPlayer() instanceof Player p)refresh(p);}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void pickup(org.bukkit.event.entity.EntityPickupItemEvent e){if(AltarCatalog.find(e.getItem().getItemStack())!=null)e.getItem().setItemStack(AltarPresentation.decorate(e.getItem().getItemStack()));}
 @EventHandler public void quit(PlayerQuitEvent e){AltarCooldowns.clear(e.getPlayer());}
 @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void swap(PlayerSwapHandItemsEvent e){AltarCooldowns.attempt(e.getPlayer(),e.getPlayer().isSneaking()?"shift-f":"f");}
 @EventHandler(priority=EventPriority.LOWEST) public void interact(PlayerInteractEvent e){
  if(e.getHand()!=org.bukkit.inventory.EquipmentSlot.HAND||!e.getAction().isRightClick())return;
  AltarCooldowns.attempt(e.getPlayer(),e.getPlayer().isSneaking()?"shift-rmb":"rmb");
 }
}

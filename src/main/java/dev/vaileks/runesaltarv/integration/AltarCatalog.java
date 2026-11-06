package dev.vaileks.runesaltarv.integration;
import java.util.*;import java.util.function.Supplier;
import org.bukkit.inventory.ItemStack;
import dev.vaileks.runesaltarv.utils.WeaponFactory;
public final class AltarCatalog {
 public record Entry(String id,String mechanics,Supplier<ItemStack> factory) {}
 public static final List<Entry> WEAPONS=List.of(
  new Entry("bone_blade","bone-blade",()->WeaponFactory.getBoneBlade()),
  new Entry("bloodlust","bloodlust",()->WeaponFactory.getBloodLust(0)),
  new Entry("nightpiercer","nightpiercer",()->WeaponFactory.getNightpiercer()),
  new Entry("vulcan_crossbow","vulcan_crossbow",()->WeaponFactory.getVulcanCrossbow()),
  new Entry("pale_gun","pale-gun",()->WeaponFactory.getPaleGun()),
  new Entry("frost_scythe","frost-scythe",()->WeaponFactory.getFrostScythe()),
  new Entry("pure_blade","pure-blade",()->WeaponFactory.getPureBlade()),
  new Entry("knightfall","knightfall",()->WeaponFactory.getKnightfall(0)),
  new Entry("shadow_blade","shadow-blade",()->WeaponFactory.getShadowBlade()),
  new Entry("windweaver","windweaver",()->WeaponFactory.getWindWeaver()),
  new Entry("hyperion","hyperion",()->WeaponFactory.getHyperion()),
  new Entry("wither_blade","wither-blade",()->WeaponFactory.getWitherBlade()),
  new Entry("earth_gauntlet","earth-gauntlet",()->WeaponFactory.getEarthGauntlet()),
  new Entry("cutlass","cutlass",()->WeaponFactory.getCutlass()),
  new Entry("paladins_battle_axe","paladins-battle-axe",()->WeaponFactory.getPaladinsBattleAxe()),
  new Entry("crazyslots","crazy-slots",()->WeaponFactory.getCrazySlots()));
 public static Entry find(ItemStack item){for(Entry entry:WEAPONS)if(WeaponFactory.isAltarWeapon(item,entry.id()))return entry;return null;}
 private AltarCatalog(){}
}

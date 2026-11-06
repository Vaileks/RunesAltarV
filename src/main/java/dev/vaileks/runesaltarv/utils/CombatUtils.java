package dev.vaileks.runesaltarv.utils;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public final class CombatUtils {
   private static final ThreadLocal<Integer> SYNTHETIC_DAMAGE_DEPTH = ThreadLocal.withInitial(() -> 0);

   private CombatUtils() {
   }

   public static boolean isDirectMeleeHit(EntityDamageByEntityEvent event) {
      return !isSyntheticDamage() && (event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK);
   }

   public static void runSyntheticDamage(Runnable action) {
      SYNTHETIC_DAMAGE_DEPTH.set(SYNTHETIC_DAMAGE_DEPTH.get() + 1);

      try {
         action.run();
      } finally {
         int next = SYNTHETIC_DAMAGE_DEPTH.get() - 1;
         if (next <= 0) {
            SYNTHETIC_DAMAGE_DEPTH.remove();
         } else {
            SYNTHETIC_DAMAGE_DEPTH.set(next);
         }
      }
   }

   public static boolean clearPath(org.bukkit.Location from, org.bukkit.Location to) {
      if (from.getWorld() == null || from.getWorld() != to.getWorld()) return false;
      org.bukkit.util.Vector delta = to.toVector().subtract(from.toVector());
      double distance = delta.length();
      return distance < 0.05 || from.getWorld().rayTraceBlocks(from, delta.multiply(1.0 / distance), distance,
         org.bukkit.FluidCollisionMode.NEVER, true) == null;
   }

   public static boolean clearPath(org.bukkit.Location from, org.bukkit.entity.LivingEntity target) {
      return clearPath(from, target.getLocation().add(0, target.getHeight() * 0.5, 0));
   }

   public static boolean isSyntheticDamage() {
      return SYNTHETIC_DAMAGE_DEPTH.get() > 0;
   }
}

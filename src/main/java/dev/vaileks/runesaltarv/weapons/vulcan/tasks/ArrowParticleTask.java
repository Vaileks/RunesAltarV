package dev.vaileks.runesaltarv.weapons.vulcan.tasks;
import dev.vaileks.runesaltarv.integration.AltarTuning;

import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.scheduler.BukkitRunnable;

public class ArrowParticleTask extends BukkitRunnable {
   private final Arrow arrow;
   private final boolean isCentral;

   public ArrowParticleTask(Arrow arrow, boolean isCentral) {
      this.arrow = arrow;
      this.isCentral = isCentral;
   }

   @Override
   public void run() {
      if (!this.arrow.isDead() && !this.arrow.isInBlock() && !this.arrow.isOnGround()) {
         if (this.isCentral) {
            this.arrow.getWorld().spawnParticle(Particle.FLAME, this.arrow.getLocation(),AltarTuning.particles("vulcan_crossbow",2), 0.05, 0.05, 0.05, 0.01);
            if (Math.random() > 0.6) {
               this.arrow.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, this.arrow.getLocation(),AltarTuning.particles("vulcan_crossbow",1), 0.05, 0.05, 0.05, 0.01);
            }
         } else {
            this.arrow.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, this.arrow.getLocation(),AltarTuning.particles("vulcan_crossbow",2), 0.05, 0.05, 0.05, 0.01);
            if (Math.random() > 0.6) {
               this.arrow.getWorld().spawnParticle(Particle.FLAME, this.arrow.getLocation(),AltarTuning.particles("vulcan_crossbow",1), 0.05, 0.05, 0.05, 0.01);
            }
         }
      } else {
         this.cancel();
      }
   }
}

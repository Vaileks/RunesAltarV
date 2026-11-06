package dev.vaileks.runesaltarv.utils;

import dev.vaileks.runesaltarv.RunesAltarV;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Adapts the existing progress API to one shared action bar, without owning gameplay timers. */
final class ShadowBossBar implements BossBar {
   private static final Set<ShadowBossBar> ACTIVE_BARS = new LinkedHashSet<>();
   private record Message(Player player, Component text, long until) {}
   private static final Map<UUID, Message> MESSAGES = new LinkedHashMap<>();
   private static final Map<UUID, Player> DISPLAYED = new LinkedHashMap<>();
   private static BukkitTask task;
   private final Set<Player> players = new LinkedHashSet<>();
   private final Set<BarFlag> flags = EnumSet.noneOf(BarFlag.class);
   private final boolean cooldown;
   private String title;
   private BarColor color;
   private BarStyle style;
   private double progress = 1;
   private boolean visible = true;

   ShadowBossBar(String title, BarColor color, BarStyle style) {
      this(title, color, style, false);
   }

   ShadowBossBar(String title, BarColor color, BarStyle style, boolean cooldown) {
      this.title = Objects.requireNonNull(title);
      this.color = Objects.requireNonNull(color);
      this.style = Objects.requireNonNull(style);
      this.cooldown = cooldown;
   }

   @Override public String getTitle() { return title; }
   @Override public void setTitle(String title) { this.title = Objects.requireNonNull(title); }
   @Override public BarColor getColor() { return color; }
   @Override public void setColor(BarColor color) { this.color = Objects.requireNonNull(color); }
   @Override public BarStyle getStyle() { return style; }
   @Override public void setStyle(BarStyle style) { this.style = Objects.requireNonNull(style); }
   @Override public void removeFlag(BarFlag flag) { flags.remove(flag); }
   @Override public void addFlag(BarFlag flag) { flags.add(flag); }
   @Override public boolean hasFlag(BarFlag flag) { return flags.contains(flag); }
   @Override public double getProgress() { return progress; }
   @Override public void setProgress(double progress) {
      if (!Double.isFinite(progress) || progress < 0 || progress > 1) throw new IllegalArgumentException("Progress must be between 0 and 1");
      this.progress = progress;
   }
   @Override public List<Player> getPlayers() { return List.copyOf(players); }
   @Override public void addPlayer(Player player) {
      if (players.add(Objects.requireNonNull(player))) { ACTIVE_BARS.add(this); start(); }
   }
   @Override public void removePlayer(Player player) {
      players.remove(player);
      if (players.isEmpty()) ACTIVE_BARS.remove(this);
   }
   @Override public void removeAll() { players.clear(); ACTIVE_BARS.remove(this); }
   @Override public void setVisible(boolean visible) { this.visible = visible; if (visible && !players.isEmpty()) start(); }
   @Override public boolean isVisible() { return visible; }
   @Override public void show() { setVisible(true); }
   @Override public void hide() { setVisible(false); }

   private Component text() {
      NamedTextColor accent = cooldown ? NamedTextColor.GOLD : switch (color) {
         case RED -> NamedTextColor.RED;
         case GREEN -> NamedTextColor.GREEN;
         case BLUE -> NamedTextColor.AQUA;
         case PURPLE, PINK -> NamedTextColor.LIGHT_PURPLE;
         case YELLOW -> NamedTextColor.YELLOW;
         default -> NamedTextColor.WHITE;
      };
      String label = TextUtils.toPlain(TextUtils.legacy(title)).replace("!", "").strip().replaceAll("\\s+", " ");
      int filled = (int)Math.round(progress * 8);
      Component result = Component.empty().decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, false);
      if (cooldown) result = result.append(Component.text("КД · ", NamedTextColor.GOLD));
      return result.append(Component.text(label + "  ", NamedTextColor.WHITE))
         .append(Component.text("▰".repeat(filled), accent))
         .append(Component.text("▰".repeat(8 - filled), NamedTextColor.DARK_GRAY))
         .append(Component.text("  " + (int)Math.ceil(progress * 100) + "%", accent));
   }

   static void message(Player player, Component text) {
      if (player == null || !player.isOnline() || text == null) return;
      UUID id = player.getUniqueId();
      if (TextUtils.toPlain(text).isBlank()) MESSAGES.remove(id);
      else MESSAGES.put(id, new Message(player, text, System.currentTimeMillis() + 1200));
      start();
   }

   private static void start() {
      var plugin = RunesAltarV.getInstance();
      if (task == null && plugin != null && plugin.isEnabled())
         task = plugin.getServer().getScheduler().runTaskTimer(plugin, ShadowBossBar::render, 0L, 4L);
   }

   /** One packet per player/update; active effects take precedence over cooldowns. */
   static void render() {
      long now = System.currentTimeMillis();
      MESSAGES.values().removeIf(message -> message.until <= now || !message.player.isOnline());
      Map<UUID, Player> viewers = new LinkedHashMap<>();
      Map<UUID, List<ShadowBossBar>> bars = new LinkedHashMap<>();
      for (ShadowBossBar bar : List.copyOf(ACTIVE_BARS)) {
         bar.players.removeIf(player -> !player.isOnline());
         if (bar.players.isEmpty()) { ACTIVE_BARS.remove(bar); continue; }
         if (!bar.visible) continue;
         for (Player player : bar.players) {
            UUID id = player.getUniqueId();
            viewers.put(id, player);
            bars.computeIfAbsent(id, key -> new ArrayList<>()).add(bar);
         }
      }
      MESSAGES.forEach((id, message) -> viewers.put(id, message.player));
      for (var entry : viewers.entrySet()) {
         UUID id = entry.getKey();
         List<ShadowBossBar> list = bars.getOrDefault(id, List.of());
         list = new ArrayList<>(list);
         list.sort(Comparator.comparing(bar -> bar.cooldown));
         Component result = null;
         int count = 0;
         Message message = MESSAGES.get(id);
         if (message != null) { result = message.text; count++; }
         for (ShadowBossBar bar : list) {
            if (count >= 2) break;
            if (result == null) result = bar.text();
            else result = result.append(Component.text("   │   ", NamedTextColor.DARK_GRAY)).append(bar.text());
            count++;
         }
         if (result != null) {
            TextUtils.sendActionBarNow(entry.getValue(), result);
            DISPLAYED.put(id, entry.getValue());
         }
      }
      for (var entry : new ArrayList<>(DISPLAYED.entrySet())) {
         if (!viewers.containsKey(entry.getKey())) {
            TextUtils.sendActionBarNow(entry.getValue(), Component.empty());
            DISPLAYED.remove(entry.getKey());
         }
      }
      if (viewers.isEmpty() && task != null) { task.cancel(); task = null; }
   }

   static void removePlayerFromAll(Player player) {
      if (player == null) return;
      for (ShadowBossBar bar : List.copyOf(ACTIVE_BARS)) bar.removePlayer(player);
      MESSAGES.remove(player.getUniqueId());
      if (DISPLAYED.remove(player.getUniqueId()) != null) TextUtils.sendActionBarNow(player, Component.empty());
   }

   static void clearAll() {
      if (task != null) { task.cancel(); task = null; }
      for (ShadowBossBar bar : List.copyOf(ACTIVE_BARS)) bar.players.clear();
      ACTIVE_BARS.clear(); MESSAGES.clear();
      for (Player player : DISPLAYED.values()) TextUtils.sendActionBarNow(player, Component.empty());
      DISPLAYED.clear();
   }
}

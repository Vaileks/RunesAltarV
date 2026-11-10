package dev.vaileks.runesaltarv.integration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.bukkit.configuration.file.*;
import dev.vaileks.runesaltarv.RunesAltarV;

/** Each weapon has one configuration file in this plugin's items directory. */
public final class AltarPresets {
 private static final Map<String,FileConfiguration> entries = new LinkedHashMap<>();
 public static FileConfiguration get(String id) { return entries.get(id); }
 public static void load(RunesAltarV plugin) {
  Map<String,FileConfiguration> loaded = new LinkedHashMap<>();
  for (var entry : AltarCatalog.WEAPONS) {
   Path file = plugin.getDataFolder().toPath().resolve("items/" + entry.id() + ".yml");
   try (InputStream in = plugin.getResource("defaults/" + entry.id() + ".yml")) {
    if (in == null) throw new IOException("Missing defaults: " + entry.id());
    var defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in,StandardCharsets.UTF_8));
    if (!Files.exists(file)) { Files.createDirectories(file.getParent()); defaults.save(file.toFile()); }
    var yaml = new YamlConfiguration(); yaml.load(file.toFile());
    boolean changed=false;
    for(String key:defaults.getKeys(true))if(!defaults.isConfigurationSection(key)&&!yaml.contains(key)){yaml.set(key,defaults.get(key));changed=true;}
    if(changed)yaml.save(file.toFile());
    yaml.setDefaults(defaults);
    loaded.put(entry.id(), yaml);
   } catch (Exception e) { throw new IllegalStateException("Cannot load " + file, e); }
  }
  entries.clear(); entries.putAll(loaded);
 }
 public static FileConfiguration mechanics() {
  var result = new YamlConfiguration();
  for (var entry : AltarCatalog.WEAPONS) result.set(entry.mechanics(), get(entry.id()).getConfigurationSection("abilities"));
  return result;
 }
 public static void models(FileConfiguration models) {
  for (var entry : AltarCatalog.WEAPONS) models.set("weapons." + entry.id(), get(entry.id()).getConfigurationSection("item"));
 }
 private AltarPresets() {}
}

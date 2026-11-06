package dev.vaileks.runesaltarv.managers;

import dev.vaileks.runesaltarv.RunesAltarV;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class FriendManager {
   private final RunesAltarV plugin;
   private final File file;
   private FileConfiguration config;
   private final Map<UUID, Set<UUID>> friendsCache = new HashMap<>();

   public FriendManager(RunesAltarV plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "friends.yml");
      this.loadFriends();
   }

   private void loadFriends() {
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException var8) {
            var8.printStackTrace();
         }
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);

      for (String key : this.config.getKeys(false)) {
         UUID playerUUID = UUID.fromString(key);
         Set<UUID> playerFriends = new HashSet<>();

         for (String friendStr : this.config.getStringList(key)) {
            playerFriends.add(UUID.fromString(friendStr));
         }

         this.friendsCache.put(playerUUID, playerFriends);
      }
   }

   public void saveFriends() {
      for (Entry<UUID, Set<UUID>> entry : this.friendsCache.entrySet()) {
         List<String> list = new ArrayList<>();

         for (UUID f : entry.getValue()) {
            list.add(f.toString());
         }

         this.config.set(entry.getKey().toString(), list);
      }

      try {
         this.config.save(this.file);
      } catch (IOException var6) {
         var6.printStackTrace();
      }
   }

   public boolean isFriend(UUID player, UUID target) {
      return this.friendsCache.containsKey(player) && this.friendsCache.get(player).contains(target);
   }

   public Set<UUID> getFriends(UUID player) {
      return this.friendsCache.getOrDefault(player, new HashSet<>());
   }

   public boolean addFriend(UUID player, UUID target) {
      Set<UUID> friends = this.getFriends(player);
      if (friends.size() >= 2) {
         return false;
      } else {
         friends.add(target);
         this.friendsCache.put(player, friends);
         this.saveFriends();
         return true;
      }
   }

   public void removeFriend(UUID player, UUID target) {
      if (this.friendsCache.containsKey(player)) {
         this.friendsCache.get(player).remove(target);
         this.saveFriends();
      }
   }
}

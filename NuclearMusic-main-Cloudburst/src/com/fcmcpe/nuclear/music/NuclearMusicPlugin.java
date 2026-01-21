package com.fcmcpe.nuclear.music;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.entity.EntityLevelChangeEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import com.xxmicloxx.NoteBlockAPI.NoteBlockAPI;
import com.xxmicloxx.NoteBlockAPI.NBS.NBSDecoder;
import com.xxmicloxx.NoteBlockAPI.Song;
import com.xxmicloxx.NoteBlockAPI.event.SongEndEvent;
import com.xxmicloxx.NoteBlockAPI.player.NoteBlockSongPlayer;
import com.xxmicloxx.NoteBlockAPI.player.RadioStereoSongPlayer;
import com.xxmicloxx.NoteBlockAPI.player.SongPlayer;

import java.io.File;
import java.util.*;

public class NuclearMusicPlugin extends PluginBase {
    public static NuclearMusicPlugin instance;
    private final LinkedList<Song> songs = new LinkedList<>();
    private final Map<NodeIntegerPosition, SongPlayer> songPlayers = new HashMap<>();
    private NodeIntegerPosition controlBlockPosition;
    private RadioStereoSongPlayer radioPlayer;
    public static boolean playEverywhere;
    public static boolean allowNonOpControl;
    public static boolean singleCycle;
    public static List<String> enabledWorlds;
    private DynamicTicker ticker;
    private static final int INITIAL_DELAY_TICKS = 1;
    private static final int INITIAL_DELAY_TICKS_AUTO = 20; // 1秒延迟

    static List<File> getAllNBSFiles(File path) {
        List<File> result = new ArrayList<>();
        File[] subFile = path.listFiles();
        if (subFile == null) return result;
        for (File aSubFile : subFile) {
            if (aSubFile.isDirectory()) continue;
            if (!aSubFile.getName().trim().toLowerCase().endsWith(".nbs")) continue;
            result.add(aSubFile);
        }
        return result;
    }

    private boolean isWorldEnabled(String worldName) {
        if (enabledWorlds.isEmpty()) return true;
        return enabledWorlds.contains(worldName);
    }

    private void addPlayerToRadio(Player player) {
        if (radioPlayer == null) return;
        if (isWorldEnabled(player.getLevel().getName())) {
            radioPlayer.addPlayer(player);
        }
    }

    private void removePlayerFromRadio(Player player) {
        if (radioPlayer != null) {
            radioPlayer.removePlayer(player);
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        playEverywhere = getConfig().getBoolean("playEverywhere", true);
        allowNonOpControl = getConfig().getBoolean("allowNonOpControl", false);
        singleCycle = getConfig().getBoolean("singleCycle", false);
        enabledWorlds = getConfig().getStringList("enabledWorlds");
        loadAllSongs();
        
        if (playEverywhere) {
            Config controlBlocks = new Config(getDataFolder() + "/controlblock.yml", Config.YAML);
            String position = controlBlocks.getString("position");
            if (position != null && !position.isEmpty()) {
                String[] data = position.split(":");
                if (data.length == 4) {
                    Level level = getServer().getLevelByName(data[3]);
                    if (level != null) {
                        try {
                            int x = Integer.parseInt(data[0]);
                            int y = Integer.parseInt(data[1]);
                            int z = Integer.parseInt(data[2]);
                            Block block = level.getBlock(x, y, z, true);
                            if (block.getId() == Item.NOTEBLOCK) {
                                controlBlockPosition = new NodeIntegerPosition(block);
                                startRadioPlayer();
                            }
                        } catch (NumberFormatException ignore) {}
                    }
                }
            }
        } else {
            Config noteblocks = new Config(getDataFolder() + "/noteblocks.yml", Config.YAML);
            List<String> positions = noteblocks.getStringList("positions");
            for (String position : positions) {
                String[] data = position.split(":");
                if (data.length != 4) {
                    getLogger().warning("Corrupted save data found: " + position);
                    continue;
                }
                Level level = getServer().getLevelByName(data[3]);
                if (level == null) {
                    getLogger().warning("Unknown level: " + data[3]);
                    continue;
                }
                int x, y, z;
                try {
                    x = Integer.parseInt(data[0]);
                    y = Integer.parseInt(data[1]);
                    z = Integer.parseInt(data[2]);
                } catch (NumberFormatException ignore) {
                    getLogger().warning("Corrupted save data found: " + position);
                    continue;
                }
                Block block = level.getBlock(x, y, z, true);
                if (block.getId() != Item.NOTEBLOCK) {
                    getLogger().warning("Noteblock does not exist at " + x + ' ' + y + ' ' + z);
                    continue;
                }
                Song song = songs.getFirst();
                NoteBlockSongPlayer songPlayer = new NoteBlockSongPlayer(song);
                songPlayer.setNoteBlock(block);
                songPlayer.setAutoCycle(singleCycle);
                songPlayer.setAutoDestroy(false);
                getServer().getOnlinePlayers().forEach((s, p) -> songPlayer.addPlayer(p));
                songPlayer.setPlaying(true);
                NodeIntegerPosition node = new NodeIntegerPosition(block);
                songPlayers.put(node, songPlayer);
            }
        }

        getServer().getPluginManager().registerEvents(new NuclearMusicListener(), this);
        
        // 启动动态调度线程
        ticker = new DynamicTicker();
        ticker.start();
    }

    @Override
    public void onDisable() {
        // 安全关闭调度线程
        if (ticker != null) {
            ticker.shutdown();
            try {
                ticker.join(1000);
            } catch (InterruptedException ignored) {}
        }
        
        if (playEverywhere) {
            Config controlBlocks = new Config(getDataFolder() + "/controlblock.yml", Config.YAML);
            if (controlBlockPosition != null) {
                controlBlocks.set("position", controlBlockPosition.x + ":" + controlBlockPosition.y + ':' + controlBlockPosition.z + ':' + controlBlockPosition.level.getName());
            } else {
                controlBlocks.set("position", "");
            }
            controlBlocks.save();
            
            if (radioPlayer != null) {
                radioPlayer.destroy();
            }
        } else {
            Config noteblocks = new Config(getDataFolder() + "/noteblocks.yml", Config.YAML);
            List<String> positions = new ArrayList<>();
            songPlayers.forEach((pos, sp) -> positions.add(pos.x + ":" + pos.y + ':' + pos.z  + ':' + pos.level.getName()));
            noteblocks.set("positions", positions);
            noteblocks.save();
            
            songPlayers.values().forEach(SongPlayer::destroy);
        }
    }

    private void loadAllSongs() {
        new File(getDataFolder() + "/tracks").mkdirs();
        List<File> files = getAllNBSFiles(new File(getDataFolder(), "tracks"));
        files.forEach(file -> {
            Song song = NBSDecoder.parse(file);
            if (song == null) {
                getLogger().warning("Failed to load song: " + file.getName());
                return;
            }
            songs.add(song);
        });
        Collections.shuffle(songs);
        getLogger().info("Loaded " + songs.size() + " songs");
    }

    public Song nextSong(Song now) {
        if (songs.isEmpty()) return null;
        if (!songs.contains(now)) return songs.getFirst();
        int nextIndex = (songs.indexOf(now) + 1) % songs.size();
        return songs.get(nextIndex);
    }
    
    private void startRadioPlayer() {
        if (songs.isEmpty()) return;
        Song song = songs.getFirst();
        radioPlayer = new RadioStereoSongPlayer(song);
        radioPlayer.setAutoCycle(singleCycle);
        radioPlayer.setAutoDestroy(false);
        
        // 从全局队列移除，避免内存泄漏
        NoteBlockAPI.getInstance().playing.remove(radioPlayer);
        
        getServer().getOnlinePlayers().forEach((s, p) -> addPlayerToRadio(p));
        radioPlayer.setPlaying(true);
    }

    class NodeIntegerPosition {
        int x;
        int y;
        int z;
        Level level;

        NodeIntegerPosition(int x, int y, int z, Level level) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
        }

        NodeIntegerPosition(Position position) {
            this(position.getFloorX(), position.getFloorY(), position.getFloorZ(), position.getLevel());
        }
        
        NodeIntegerPosition(Block block) {
            this(block.getFloorX(), block.getFloorY(), block.getFloorZ(), block.getLevel());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NodeIntegerPosition)) return false;
            NodeIntegerPosition node = (NodeIntegerPosition) obj;
            return (x == node.x) && (y == node.y) && (z == node.z) && (level == node.level);
        }

        @Override
        public int hashCode() {
            return (x + ":" + y + ':' + z + ':' + level.getName()).hashCode();
        }
    }

    class NuclearMusicListener implements Listener {

        @EventHandler
        public void onBlockTouch(PlayerInteractEvent event) {
            if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
            Block block = event.getBlock();
            if (block.getId() != Item.NOTEBLOCK) return;
            
            NodeIntegerPosition node = new NodeIntegerPosition(block);
            boolean isControlBlock = node.equals(controlBlockPosition);
            boolean isMusicBlock = songPlayers.containsKey(node);
            
            boolean isControlItem = event.getItem() != null && event.getItem().getId() == Item.DIAMOND_HOE && event.getItem().getDamage() == 9999;
            
            boolean hasPermission = event.getPlayer().hasPermission("nuclearmusic.setup") || allowNonOpControl;
            
            if (isControlItem && hasPermission) {
                if (playEverywhere) {
                    if (isControlBlock) {
                        if (songs.isEmpty()) {
                            event.getPlayer().sendActionBar("§cError! No songs loaded!");
                            event.setCancelled(true);
                            return;
                        }
                        
                        Song now = radioPlayer.getSong();
                        Song next = nextSong(now);
                        if (next == null) {
                            event.getPlayer().sendActionBar("§cError! Failed to get next song!");
                            return;
                        }
                        radioPlayer.resetSong(next, INITIAL_DELAY_TICKS); // 添加1tick延迟
                        event.getPlayer().sendActionBar("§aNow playing: §7" + next.getTitle());
                        event.setCancelled(true);
                    } else if (controlBlockPosition == null) {
                        if (songs.isEmpty()) {
                            event.getPlayer().sendActionBar("§cError! No songs loaded!");
                            event.setCancelled(true);
                            return;
                        }
                        
                        controlBlockPosition = node;
                        if (radioPlayer != null) {
                            radioPlayer.destroy();
                        }
                        startRadioPlayer();
                        event.getPlayer().sendActionBar("§aNow playing: §7" + songs.getFirst().getTitle());
                        event.setCancelled(true);
                    }
                } else {
                    if (isMusicBlock) {
                        SongPlayer sp = songPlayers.get(node);
                        Song now = sp.getSong();
                        Song next = nextSong(now);
                        if (next == null) {
                            event.getPlayer().sendActionBar("§cError! Failed to get next song!");
                            return;
                        }
                        sp.resetSong(next, INITIAL_DELAY_TICKS); // 添加1tick延迟
                        event.getPlayer().sendActionBar("§aNow playing: §7" + next.getTitle());
                        event.setCancelled(true);
                    } else {
                        try {
                            Song song = songs.getFirst();
                            NoteBlockSongPlayer songPlayer = new NoteBlockSongPlayer(song);
                            
                            // 从全局队列移除，避免内存泄漏
                            NoteBlockAPI.getInstance().playing.remove(songPlayer);
                            
                            songPlayer.setNoteBlock(block);
                            songPlayer.setAutoCycle(singleCycle);
                            songPlayer.setAutoDestroy(false);
                            getServer().getOnlinePlayers().forEach((s, p) -> songPlayer.addPlayer(p));
                            songPlayer.setPlaying(true);
                            NodeIntegerPosition nodePos = new NodeIntegerPosition(block);
                            songPlayers.put(nodePos, songPlayer);
                            event.getPlayer().sendActionBar("§aNow playing: §7" + song.getTitle());
                            event.setCancelled(true);
                        } catch (NoSuchElementException ignore) {
                            event.getPlayer().sendActionBar("§cError! No songs loaded!");
                            event.setCancelled(true);
                        }
                    }
                }
            } else {
                if (playEverywhere && isControlBlock) {
                    event.getPlayer().sendActionBar("§aNow playing: §7" + radioPlayer.getSong().getTitle());
                    event.setCancelled(true);
                } else if (!playEverywhere && isMusicBlock) {
                    event.getPlayer().sendActionBar("§aNow playing: §7" + songPlayers.get(node).getSong().getTitle());
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            if (playEverywhere) {
                if (radioPlayer != null) {
                    addPlayerToRadio(event.getPlayer());
                }
            } else {
                songPlayers.values().forEach(sp -> sp.addPlayer(event.getPlayer()));
            }
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            NoteBlockAPI.getInstance().stopPlaying(event.getPlayer());
        }

        @EventHandler
        public void onWorldChange(EntityLevelChangeEvent event) {
            if (!playEverywhere || radioPlayer == null) return;
            if (!(event.getEntity() instanceof Player)) return;
            
            Player player = (Player) event.getEntity();
            Level targetLevel = event.getTarget();
            
            removePlayerFromRadio(player);

            if (isWorldEnabled(targetLevel.getName())) {
                radioPlayer.addPlayer(player);
            }
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.getBlock().getId() == Item.NOTEBLOCK) {
                NodeIntegerPosition node = new NodeIntegerPosition(event.getBlock());
                
                if (playEverywhere) {
                    if (node.equals(controlBlockPosition)) {
                        if (radioPlayer != null) {
                            radioPlayer.destroy();
                            radioPlayer = null;
                        }
                        controlBlockPosition = null;
                    }
                } else {
                    SongPlayer sp = songPlayers.get(node);
                    if (sp != null) {
                        sp.destroy();
                        songPlayers.remove(node);
                    }
                }
            }
        }

        @EventHandler
        public void onSongEnd(SongEndEvent event) {
            SongPlayer songPlayer = event.getSongPlayer();
            
            // 处理全局收音机
            if (playEverywhere && songPlayer == radioPlayer) {
                if (!singleCycle) {
                    Song next = nextSong(songPlayer.getSong());
                    if (next != null) {
                        radioPlayer.resetSong(next, INITIAL_DELAY_TICKS_AUTO); // 添加1秒延迟
                    }
                }
                return;
            }
            
            // 处理音符块播放器
            if (!playEverywhere) {
                for (Map.Entry<NodeIntegerPosition, SongPlayer> entry : songPlayers.entrySet()) {
                    if (entry.getValue() == songPlayer) {
                        if (!singleCycle) {
                            Song next = nextSong(songPlayer.getSong());
                            if (next != null) {
                                songPlayer.resetSong(next, INITIAL_DELAY_TICKS_AUTO); // 添加1秒延迟
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    class DynamicTicker extends Thread {
        private static final long MIN_SLEEP = 1;
        private volatile boolean running = true;

        DynamicTicker() {
            setName("NuclearMusic-DynamicTicker");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (running && instance.isEnabled()) {
                try {
                    // 调用内置播放逻辑处理（自动管理tick和结束检测）
                    if (playEverywhere && radioPlayer != null) {
                        radioPlayer.tryPlay();
                    }

                    if (!playEverywhere) {
                        songPlayers.values().forEach(SongPlayer::tryPlay);
                    }

                    Thread.sleep(MIN_SLEEP);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    getLogger().error("Error in DynamicTicker: " + e.getMessage(), e);
                }
            }
        }

        public void shutdown() {
            running = false;
            interrupt();
        }
    }
}
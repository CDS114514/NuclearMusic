package com.fcmcpe.nuclear.music;

import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
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
    private boolean isRadioWaiting = false;
    private final Map<NodeIntegerPosition, Boolean> noteBlockWaiting = new HashMap<>();
    private final Map<NodeIntegerPosition, Long> noteBlockLastProcessTime = new HashMap<>();
    private long radioLastProcessTime = 0;

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

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        playEverywhere = getConfig().getBoolean("playEverywhere", true);
        allowNonOpControl = getConfig().getBoolean("allowNonOpControl", false);
        singleCycle = getConfig().getBoolean("singleCycle", false);
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
                                radioLastProcessTime = System.currentTimeMillis();
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
                noteBlockLastProcessTime.put(node, System.currentTimeMillis());
            }
        }

        getServer().getPluginManager().registerEvents(new NuclearMusicListener(), this);
        new DynamicTicker().start();
    }

    @Override
    public void onDisable() {
        if (playEverywhere) {
            Config controlBlocks = new Config(getDataFolder() + "/controlblock.yml", Config.YAML);
            if (controlBlockPosition != null) {
                controlBlocks.set("position", controlBlockPosition.x + ":" + controlBlockPosition.y + ':' + controlBlockPosition.z + ':' + controlBlockPosition.level.getName());
            } else {
                controlBlocks.set("position", "");
            }
            controlBlocks.save();
            
            if (radioPlayer != null) {
                radioPlayer.setPlaying(false);
                radioPlayer.destroy();
            }
        } else {
            Config noteblocks = new Config(getDataFolder() + "/noteblocks.yml", Config.YAML);
            List<String> positions = new ArrayList<>();
            songPlayers.forEach((pos, sp) -> positions.add(pos.x + ":" + pos.y + ':' + pos.z  + ':' + pos.level.getName()));
            noteblocks.set("positions", positions);
            noteblocks.save();
            
            songPlayers.values().forEach(sp -> {
                sp.setPlaying(false);
                sp.destroy();
            });
        }
    }

    private void loadAllSongs() {
        new File(getDataFolder() + "/tracks").mkdirs();
        List<File> files = getAllNBSFiles(new File(getDataFolder(), "tracks"));
        files.forEach(file -> {
            Song song = NBSDecoder.parse(file);
            if (song == null) return;
            songs.add(song);
        });
        Collections.shuffle(songs);
        getLogger().info("Loaded " + songs.size() + " songs");
    }

    public Song nextSong(Song now) {
        if (!songs.contains(now)) return songs.getFirst();
        if (songs.indexOf(now) >= songs.size() - 1) return songs.getFirst();
        return songs.get(songs.indexOf(now) + 1);
    }
    
    private void startRadioPlayer() {
        if (songs.isEmpty()) return;
        Song song = songs.getFirst();
        radioPlayer = new RadioStereoSongPlayer(song);
        radioPlayer.setAutoCycle(singleCycle);
        radioPlayer.setAutoDestroy(false);
        getServer().getOnlinePlayers().forEach((s, p) -> radioPlayer.addPlayer(p));
        radioPlayer.setPlaying(true);
        radioLastProcessTime = System.currentTimeMillis();
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
                        Song now = radioPlayer.getSong();
                        radioPlayer.setPlaying(false);
                        Song next = nextSong(now);
                        radioPlayer = new RadioStereoSongPlayer(next);
                        radioPlayer.setAutoCycle(singleCycle);
                        radioPlayer.setAutoDestroy(false);
                        getServer().getOnlinePlayers().forEach((s, p) -> radioPlayer.addPlayer(p));
                        radioPlayer.setPlaying(true);
                        radioLastProcessTime = System.currentTimeMillis();
                        event.getPlayer().sendActionBar("§aNow playing: §7" + next.getTitle());
                        event.setCancelled(true);
                    } else if (controlBlockPosition == null) {
                        controlBlockPosition = node;
                        if (radioPlayer != null) {
                            radioPlayer.setPlaying(false);
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
                        sp.setPlaying(false);
                        songPlayers.remove(node);
                        noteBlockLastProcessTime.remove(node);
                        
                        Song next = nextSong(now);
                        NoteBlockSongPlayer songPlayer = new NoteBlockSongPlayer(next);
                        songPlayer.setNoteBlock(block);
                        songPlayer.setAutoCycle(singleCycle);
                        songPlayer.setAutoDestroy(false);
                        getServer().getOnlinePlayers().forEach((s, p) -> songPlayer.addPlayer(p));
                        songPlayer.setPlaying(true);
                        songPlayers.put(node, songPlayer);
                        noteBlockLastProcessTime.put(node, System.currentTimeMillis());
                        event.getPlayer().sendActionBar("§aNow playing: §7" + next.getTitle());
                        event.setCancelled(true);
                    } else {
                        try {
                            Song song = songs.getFirst();
                            NoteBlockSongPlayer songPlayer = new NoteBlockSongPlayer(song);
                            songPlayer.setNoteBlock(block);
                            songPlayer.setAutoCycle(singleCycle);
                            songPlayer.setAutoDestroy(false);
                            getServer().getOnlinePlayers().forEach((s, p) -> songPlayer.addPlayer(p));
                            songPlayer.setPlaying(true);
                            NodeIntegerPosition nodePos = new NodeIntegerPosition(block);
                            songPlayers.put(nodePos, songPlayer);
                            noteBlockLastProcessTime.put(nodePos, System.currentTimeMillis());
                            event.getPlayer().sendActionBar("§aNow playing: §7" + song.getTitle());
                            event.setCancelled(true);
                        } catch (NoSuchElementException ignore) {
                            event.getPlayer().sendMessage("§cError! No songs loaded!");
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
                    radioPlayer.addPlayer(event.getPlayer());
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
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.getBlock().getId() == Item.NOTEBLOCK) {
                NodeIntegerPosition node = new NodeIntegerPosition(event.getBlock());
                
                if (playEverywhere) {
                    if (node.equals(controlBlockPosition)) {
                        if (radioPlayer != null) {
                            radioPlayer.setPlaying(false);
                            radioPlayer.destroy();
                            radioPlayer = null;
                        }
                        controlBlockPosition = null;
                    }
                } else {
                    SongPlayer sp = songPlayers.get(node);
                    if (sp != null) {
                        sp.setPlaying(false);
                        sp.destroy();
                        songPlayers.remove(node);
                        noteBlockLastProcessTime.remove(node);
                    }
                }
            }
        }
    }

    class DynamicTicker extends Thread {
        private static final long MIN_SLEEP = 5;

        DynamicTicker() {
            setName("NuclearMusic-DynamicTicker");
        }

        @Override
        public void run() {
            while (isEnabled()) {
                try {
                    long currentTime = System.currentTimeMillis();
                    
                    if (playEverywhere && radioPlayer != null && radioPlayer.isPlaying() && !isRadioWaiting) {
                        Song currentSong = radioPlayer.getSong();
                        double tickInterval = 1000.0 / currentSong.getSpeed();
                        long elapsed = currentTime - radioLastProcessTime;

                        if (elapsed >= tickInterval) {
                            radioPlayer.tryPlay();
                            radioLastProcessTime = currentTime;
                        }

                        if (radioPlayer.getTick() >= currentSong.getLength()) {
                            isRadioWaiting = true;
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                if (!isEnabled()) return;

                                if (singleCycle) {
                                    radioPlayer.setTick((short) 0);
                                } else {
                                    Song nextSong = nextSong(currentSong);
                                    radioPlayer.setPlaying(false);
                                    radioPlayer.destroy();
                                    radioPlayer = new RadioStereoSongPlayer(nextSong);
                                    radioPlayer.setAutoCycle(singleCycle);
                                    radioPlayer.setAutoDestroy(false);
                                    getServer().getOnlinePlayers().forEach((uuid, player) -> radioPlayer.addPlayer(player));
                                    radioPlayer.setPlaying(true);
                                }
                                radioLastProcessTime = System.currentTimeMillis();
                                isRadioWaiting = false;
                            }).start();
                        }
                    }

                    if (!playEverywhere) {
                        Iterator<Map.Entry<NodeIntegerPosition, SongPlayer>> iterator = songPlayers.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<NodeIntegerPosition, SongPlayer> entry = iterator.next();
                            NodeIntegerPosition pos = entry.getKey();
                            SongPlayer player = entry.getValue();

                            boolean isWaiting = noteBlockWaiting.getOrDefault(pos, false);
                            if (player.isPlaying() && player instanceof NoteBlockSongPlayer && !isWaiting) {
                                Song currentSong = player.getSong();
                                double tickInterval = 1000.0 / currentSong.getSpeed();
                                long lastTime = noteBlockLastProcessTime.getOrDefault(pos, currentTime);
                                long elapsed = currentTime - lastTime;

                                if (elapsed >= tickInterval) {
                                    player.tryPlay();
                                    noteBlockLastProcessTime.put(pos, currentTime);
                                }

                                if (player.getTick() >= currentSong.getLength()) {
                                    noteBlockWaiting.put(pos, true);
                                    new Thread(() -> {
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                        if (!isEnabled()) return;

                                        if (singleCycle) {
                                            player.setTick((short) 0);
                                        } else {
                                            Song nextSong = nextSong(currentSong);
                                            player.setPlaying(false);
                                            player.destroy();

                                            NoteBlockSongPlayer newPlayer = new NoteBlockSongPlayer(nextSong);
                                            newPlayer.setNoteBlock(((NoteBlockSongPlayer) player).getNoteBlock());
                                            newPlayer.setAutoCycle(singleCycle);
                                            newPlayer.setAutoDestroy(false);
                                            getServer().getOnlinePlayers().forEach((uuid, p) -> newPlayer.addPlayer(p));
                                            newPlayer.setPlaying(true);
                                            entry.setValue(newPlayer);
                                        }
                                        noteBlockLastProcessTime.put(pos, System.currentTimeMillis());
                                        noteBlockWaiting.put(pos, false);
                                    }).start();
                                }
                            }
                        }
                    }

                    Thread.sleep(MIN_SLEEP);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ignore) {}
            }
        }
    }
}

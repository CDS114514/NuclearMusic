package com.xxmicloxx.NoteBlockAPI;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import com.xxmicloxx.NoteBlockAPI.player.SongPlayer;
import com.xxmicloxx.NoteBlockAPI.runnable.TickerRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NoteBlockAPI extends PluginBase {

    private static NoteBlockAPI instance;
    public Queue<SongPlayer> playing = new ConcurrentLinkedQueue<>();
    public Map<String, List<SongPlayer>> playingSongs = new ConcurrentHashMap<>();

    public Map<String, Byte> playerVolume = new ConcurrentHashMap<>();

    public static NoteBlockAPI getInstance() {
        if (instance == null) instance = new NoteBlockAPI();
        return instance;
    }

    public void onLoad() {
        instance = this;
    }

    public void onEnable() {
        getLogger().info("! NoteBlockAPI !");
        Server.getInstance().getScheduler().scheduleDelayedTask(this, () -> new Thread(new TickerRunnable(), "NoteBlock Ticker").start(), 100);
    }

    public void onDisable() {
        getServer().getScheduler().cancelTask(this);
    }

    public boolean isReceivingSong(Player p) {
        List<SongPlayer> songs = playingSongs.get(p.getName());
        return songs != null && !songs.isEmpty();
    }

    public void stopPlaying(Player p) {
        List<SongPlayer> songs = playingSongs.get(p.getName());
        if (songs == null) {
            return;
        }
        for (SongPlayer s : songs) {
            s.removePlayer(p);
        }
    }

    public void setPlayerVolume(Player p, byte volume) {
        playerVolume.put(p.getName(), volume);
    }

    public byte getPlayerVolume(Player p) {
        return playerVolume.computeIfAbsent(p.getName(), k -> (byte) 100);
    }
}
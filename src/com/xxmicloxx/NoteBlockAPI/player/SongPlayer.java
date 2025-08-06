package com.xxmicloxx.NoteBlockAPI.player;

import cn.nukkit.Player;
import cn.nukkit.Server;
import com.xxmicloxx.NoteBlockAPI.event.SongEndEvent;
import com.xxmicloxx.NoteBlockAPI.note.FadeType;
import com.xxmicloxx.NoteBlockAPI.note.Interpolator;
import com.xxmicloxx.NoteBlockAPI.NoteBlockAPI;
import com.xxmicloxx.NoteBlockAPI.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SongPlayer {

    protected Song song;
    protected boolean playing = false;
    protected short tick = -1;
    protected Set<Player> playerList = Collections.newSetFromMap(new ConcurrentHashMap<>());
    protected boolean autoDestroy = false;
    protected boolean destroyed = false;
    protected boolean autoCycle = true;
    protected byte fadeTarget = 100;
    protected byte volume = 100;
    protected byte fadeStart = volume;
    protected int fadeDuration = 60;
    protected int fadeDone = 0;
    protected FadeType fadeType = FadeType.FADE_LINEAR;
    private long lastPlayed = 0;
    protected float speedUp = 1;

    public SongPlayer(Song song) {
        this.song = song;
        NoteBlockAPI.getInstance().playing.offer(this);
    }

    public boolean getAutoCycle() {
        return autoCycle;
    }

    public void setAutoCycle(boolean b) {
        autoCycle = b;
    }

    public FadeType getFadeType() {
        return fadeType;
    }

    public void setFadeType(FadeType fadeType) {
        this.fadeType = fadeType;
    }

    public byte getFadeTarget() {
        return fadeTarget;
    }

    public void setFadeTarget(byte fadeTarget) {
        this.fadeTarget = fadeTarget;
    }

    public byte getFadeStart() {
        return fadeStart;
    }

    public void setFadeStart(byte fadeStart) {
        this.fadeStart = fadeStart;
    }

    public int getFadeDuration() {
        return fadeDuration;
    }

    public void setFadeDuration(int fadeDuration) {
        this.fadeDuration = fadeDuration;
    }

    public int getFadeDone() {
        return fadeDone;
    }

    public void setFadeDone(int fadeDone) {
        this.fadeDone = fadeDone;
    }

    public void resetSong(Song song) {
        resetSong(song, -1);
    }

    public void resetSong(Song song, int waitTick) {
        tick = (short) -waitTick;
        this.song = song;
        this.fadeDone = 0;
        this.fadeDuration = 0;
        this.volume = 100;
        setPlaying(true);
    }

    protected void calculateFade() {
        if (fadeDone >= fadeDuration) return; // no fade today
        double targetVolume = Interpolator.interpLinear(new double[]{0, fadeStart, fadeDuration, fadeTarget}, fadeDone);
        setVolume((byte) targetVolume);
        fadeDone++;
    }

    public Set<Player> getPlayerList() {
        return Collections.unmodifiableSet(playerList);
    }

    public void addPlayer(Player p) {
        if (playerList.add(p)) {
            List<SongPlayer> songs = NoteBlockAPI.getInstance().playingSongs.computeIfAbsent(p.getName(), k -> new ArrayList<>());
            songs.add(this);
        }
    }

    public boolean getAutoDestroy() {
        return autoDestroy;
    }

    public void setAutoDestroy(boolean value) {
        autoDestroy = value;
    }

    public abstract void playTick(Player p, int tick);

    public void destroy() {
        destroyed = true;
        playerList.forEach(this::removePlayer);
        playing = false;
        setTick((short) -1);
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public short getTick() {
        return tick;
    }

    public void setTick(short tick) {
        this.tick = tick;
    }

    public void removePlayer(Player p) {
        playerList.remove(p);
        List<SongPlayer> songs = NoteBlockAPI.getInstance().playingSongs.get(p.getName());
        if (songs == null) {
            return;
        }
        songs = new ArrayList<>(songs);
        songs.remove(this);
        NoteBlockAPI.getInstance().playingSongs.put(p.getName(), songs);
        if (autoDestroy && playerList.isEmpty()) {
            destroy();
        }
    }

    public byte getVolume() {
        return volume;
    }

    public void setVolume(byte volume) {
        this.volume = volume;
    }

    public float getSpeedUp() {
        return speedUp;
    }

    public void setSpeedUp(float speedUp) {
        this.speedUp = speedUp;
    }

    public Song getSong() {
        return song;
    }

    public final void tryPlay() {
        if (!playing) return;
        calculateFade();
        if (System.currentTimeMillis() - lastPlayed < 50 * getSong().getDelay() / this.speedUp) return;
        tick++;
        //Server.getInstance().getLogger().notice("delay: " + getSong().getDelay());
        //Server.getInstance().getLogger().notice(String.valueOf(tick));
        if (tick > song.getLength()) {
            playing = false;
            tick = -1;
            if (autoDestroy) {
                destroy();
                return;
            }
            if (autoCycle) playing = true;
            Server.getInstance().getScheduler().scheduleTask(NoteBlockAPI.getInstance(), () -> new SongEndEvent(this).call());
            return;
        }
        for (Player p : playerList) {
            try {
                if (!p.isConnected()) playerList.remove(p); //offline
                if (p.spawned) playTick(p, tick);
            } catch (Exception ignore) {
            }
        }
        lastPlayed = System.currentTimeMillis();
    }
}

package com.xxmicloxx.NoteBlockAPI.event;

import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import com.xxmicloxx.NoteBlockAPI.player.SongPlayer;

public class SongEndEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final SongPlayer song;

    public SongEndEvent(SongPlayer song) {
        this.song = song;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public SongPlayer getSongPlayer() {
        return song;
    }
}

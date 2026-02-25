package com.xxmicloxx.NoteBlockAPI.note;

import java.util.HashMap;

public class Layer {

    private HashMap<Integer, Note> hashMap = new HashMap<>();
    private byte volume = 100;
    private byte stereo = 0;
    private String name = "";

    public HashMap<Integer, Note> getHashMap() {
        return hashMap;
    }

    public void setHashMap(HashMap<Integer, Note> hashMap) {
        this.hashMap = hashMap;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Note getNote(int tick) {
        return hashMap.get(tick);
    }

    public void setNote(int tick, Note note) {
        hashMap.put(tick, note);
    }

    public byte getVolume() {
        return volume;
    }

    public void setVolume(byte volume) {
        this.volume = volume;
    }

    public byte getStereo() {
        return stereo;
    }

    public void setStereo(byte stereo) {
        this.stereo = stereo;
    }
}

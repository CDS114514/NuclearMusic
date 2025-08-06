package com.xxmicloxx.NoteBlockAPI;

import cn.nukkit.Server;
import com.xxmicloxx.NoteBlockAPI.note.Layer;
import com.xxmicloxx.NoteBlockAPI.note.Note;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Song {

    private final Map<Integer, Layer> layerHashMap;
    private final short songHeight;
    private final short length;
    private final String title;
    private final File path;
    private final String author;
    private final String description;
    private final float speed;
    private final float delay;
    private final CustomInstrument[] customInstruments;
    private final int firstCustomInstrumentIndex;

    /**
     * Create Song instance by copying other Song parameters
     * @param other song
     */
    public Song(Song other) {
        this(other.getSpeed(), other.getLayerHashMap(), other.getSongHeight(),
                other.getLength(), other.getTitle(), other.getAuthor(),
                other.getDescription(), other.getPath(), other.getFirstCustomInstrumentIndex(), other.getCustomInstruments());
    }

    /**
     * @deprecated Use {@link #Song(float, Map, short, short, String, String, String, File, int)}
     * @param speed
     * @param layerHashMap
     * @param songHeight
     * @param length
     * @param title
     * @param author
     * @param description
     * @param path
     */
    @Deprecated
    public Song(float speed, Map<Integer, Layer> layerHashMap,
                short songHeight, final short length, String title, String author,
                String description, File path) {
        this(speed, layerHashMap, songHeight, length, title, author, description, path, 16, new CustomInstrument[0]);
    }

    /**
     * @deprecated Use {@link #Song(float, Map, short, short, String, String, String, File, int, CustomInstrument[])}
     * @param speed
     * @param layerHashMap
     * @param songHeight
     * @param length
     * @param title
     * @param author
     * @param description
     * @param path
     * @param customInstruments
     */
    @Deprecated
    public Song(float speed, Map<Integer, Layer> layerHashMap,
                short songHeight, final short length, String title, String author,
                String description, File path, CustomInstrument[] customInstruments) {
        this(speed, layerHashMap, songHeight, length, title, author, description, path, 16, customInstruments);
    }

    public Song(float speed, Map<Integer, Layer> layerHashMap,
                short songHeight, final short length, String title, String author,
                String description, File path, int firstCustomInstrumentIndex) {
        this(speed, layerHashMap, songHeight, length, title, author, description, path, firstCustomInstrumentIndex, new CustomInstrument[0]);
    }

    public Song(float speed, Map<Integer, Layer> layerHashMap,
                short songHeight, final short length, String title, String author,
                String description, File path, int firstCustomInstrumentIndex, CustomInstrument[] customInstruments) {
        this.speed = speed;
        delay = 20 / speed;
        this.layerHashMap = layerHashMap;
        this.songHeight = songHeight;
        this.length = length;
        this.title = title;
        this.author = author;
        this.description = description;
        this.path = path;
        this.firstCustomInstrumentIndex = firstCustomInstrumentIndex;
        this.customInstruments = customInstruments;
        //this.clearMultiNote();
    }

    /**
     * Gets all Layers in this Song and their index
     * @return HashMap of Layers and their index
     */
    public Map<Integer, Layer> getLayerHashMap() {
        return layerHashMap;
    }

    /**
     * Gets the Song's height
     * @return Song height
     */
    public short getSongHeight() {
        return songHeight;
    }

    /**
     * Gets the length in ticks of this Song
     * @return length of this Song
     */
    public short getLength() {
        return length;
    }

    /**
     * Gets the title / name of this Song
     * @return title of the Song
     */
    public String getTitle() {
        return "".equals(title) ? path.getName().replaceAll(".nbs", "") : title;
    }

    /**
     * Gets the author of the Song
     * @return author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Returns the File from which this Song is sourced
     * @return file of this Song
     */
    public File getPath() {
        return path;
    }

    /**
     * Gets the description of this Song
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the speed (ticks per second) of this Song
     * @return
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Gets the delay of this Song
     * @return delay
     */
    public float getDelay() {
        return delay;
    }

    /**
     * Gets the CustomInstruments made for this Song
     * @see CustomInstrument
     * @return array of CustomInstruments
     */
    public CustomInstrument[] getCustomInstruments() {
        return customInstruments;
    }

    @Override
    public Song clone() {
        return new Song(this);
    }

    public int getFirstCustomInstrumentIndex() {
        return firstCustomInstrumentIndex;
    }

    public void clearMultiNote() {
        Map<Byte, List<Byte>> noteMap = new HashMap<>();
        for (int tick = 0; tick < length; tick++) {
            noteMap.clear();
            for (Layer layer : this.layerHashMap.values()) {
                Note note = layer.getNote(tick);
                if (note != null) {
                    noteMap.putIfAbsent(note.getInstrument(false), new ArrayList<>());
                    if (noteMap.get(note.getInstrument(false)).stream().anyMatch(key -> note.getKey() == key)) {
                        layer.getHashMap().remove(tick);
                        Server.getInstance().getLogger().info("[" + getTitle() + "] 已移除重复Note: " + tick + " - " + note.getSoundEnum(false) + " - " + note.getKey());
                    } else {
                        noteMap.get(note.getInstrument(false)).add(note.getKey());
                    }
                }
            }
        }
    }

}

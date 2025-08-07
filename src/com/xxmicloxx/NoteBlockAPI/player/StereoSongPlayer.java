package com.xxmicloxx.NoteBlockAPI.player;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.network.protocol.BlockEventPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacketV2;
import cn.nukkit.network.protocol.PlaySoundPacket;
import com.xxmicloxx.NoteBlockAPI.Song;
import com.xxmicloxx.NoteBlockAPI.note.Layer;
import com.xxmicloxx.NoteBlockAPI.note.Note;
import com.xxmicloxx.NoteBlockAPI.nukkit.ClientTypeDetector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StereoSongPlayer extends SongPlayer {
    private Block[] l4, l3, l2, l1, m0, r1, r2, r3, r4;

    private static final Map<Class<?>, Field> EVENT_TYPE_CACHE = new HashMap<>();
    private static final Map<Class<?>, Field> EVENT_DATA_CACHE = new HashMap<>();
    
    static {
        cacheFields(BlockEventPacket.class);
    }
    
    private static void cacheFields(Class<?> clazz) {
        try {
            Field eventType = clazz.getDeclaredField("eventType");
            eventType.setAccessible(true);
            EVENT_TYPE_CACHE.put(clazz, eventType);
        } catch (NoSuchFieldException e1) {
            try {
                Field eventType = clazz.getDeclaredField("case1");
                eventType.setAccessible(true);
                EVENT_TYPE_CACHE.put(clazz, eventType);
            } catch (NoSuchFieldException e2) {
                System.err.println("NoteBlockAPI: eventType field not found in BlockEventPacket");
            }
        }

        try {
            Field eventData = clazz.getDeclaredField("eventData");
            eventData.setAccessible(true);
            EVENT_DATA_CACHE.put(clazz, eventData);
        } catch (NoSuchFieldException e1) {
            try {
                Field eventData = clazz.getDeclaredField("case2");
                eventData.setAccessible(true);
                EVENT_DATA_CACHE.put(clazz, eventData);
            } catch (NoSuchFieldException e2) {
                System.err.println("NoteBlockAPI: eventData field not found in BlockEventPacket");
            }
        }
    }

    public StereoSongPlayer(Song song) {
        super(song);
    }

    public Block[][] getNoteBlock() {
        return new Block[][]{l4, l3, l2, l1, m0, r1, r2, r3, r4};
    }

    public void setNoteBlock(Block l4, Block l3, Block l2, Block l1, Block m0, Block r1, Block r2, Block r3, Block r4) {
        this.setNoteBlock(
                new Block[]{l4},
                new Block[]{l3},
                new Block[]{l2},
                new Block[]{l1},
                new Block[]{m0},
                new Block[]{r1},
                new Block[]{r2},
                new Block[]{r3},
                new Block[]{r4}
        );
    }

    public void setNoteBlock2(Block l4, Block l3, Block l2, Block l1, Block m0, Block r1, Block r2, Block r3, Block r4) {
        this.setNoteBlock(
                new Block[]{l4, l4},
                new Block[]{l3, l3},
                new Block[]{l2, l2},
                new Block[]{l1, l1},
                new Block[]{m0, m0},
                new Block[]{r1, r1},
                new Block[]{r2, r2},
                new Block[]{r3, r3},
                new Block[]{r4, r4}
        );
    }

    public void setNoteBlock(Block[] l4, Block[] l3, Block[] l2, Block[] l1, Block[] m0, Block[] r1, Block[] r2, Block[] r3, Block[] r4) {
        this.l1 = l1;
        this.l2 = l2;
        this.l3 = l3;
        this.l4 = l4;
        this.m0 =  m0;
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.r4 = r4;
    }

    public Block[] getNoteBlock(int side) {
        if (side == 0) return this.m0;
        if (side == 1) return this.r1;
        if (side == 2) return this.r2;
        if (side == 3) return this.r3;
        if (side == 4) return this.r4;
        if (side > 4) return this.r4;
        if (side == -1) return this.l1;
        if (side == -2) return this.l2;
        if (side == -3) return this.l3;
        if (side == -4) return this.l4;
        if (side < -4) return this.l4;
        return null;
    }

    private void setFieldValue(Object obj, String[] fieldNames, Object value) {
        Class<?> clazz = obj.getClass();
        
        if ("eventType".equals(fieldNames[0]) || "case1".equals(fieldNames[0])) {
            Field field = EVENT_TYPE_CACHE.get(clazz);
            if (field != null) {
                try {
                    field.set(obj, value);
                    return;
                } catch (IllegalAccessException e) {
                }
            }
        } 
        else if ("eventData".equals(fieldNames[0]) || "case2".equals(fieldNames[0])) {
            Field field = EVENT_DATA_CACHE.get(clazz);
            if (field != null) {
                try {
                    field.set(obj, value);
                    return;
                } catch (IllegalAccessException e) {
                }
            }
        }
        
        for (String fieldName : fieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
                
                if ("eventType".equals(fieldName) || "case1".equals(fieldName)) {
                    EVENT_TYPE_CACHE.put(clazz, field);
                } 
                else if ("eventData".equals(fieldName) || "case2".equals(fieldName)) {
                    EVENT_DATA_CACHE.put(clazz, field);
                }
                return;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                continue;
            }
        }
    }

    @Override
    public void playTick(Player p, int tick) {
        if (!p.getLevel().getFolderName().equals(m0[0].getLevel().getFolderName())) {
            return;
        }
        
        int clientType = ClientTypeDetector.getClientType(p);
        boolean limit = clientType < 2;
        List<DataPacket> batchedPackets = new ArrayList<>();
        
        if (p.distance(m0[0]) < 24) {
            for (Layer l : song.getLayerHashMap().values()) {
                Note note = l.getNote(tick);
                if (note == null) {
                    continue;
                }
                
                int side = (note.getKey() - 43) / 3;
                Block[] noteBlocks = this.getNoteBlock(side);
                if (noteBlocks != null) {
                    for (Block noteBlock: noteBlocks) {
                        int pitch = note.getKey() - 33;

                        BlockEventPacket pk = new BlockEventPacket();
                        pk.x = (int) noteBlock.x;
                        pk.y = (int) noteBlock.y;
                        pk.z = (int) noteBlock.z;
                        
                        setFieldValue(pk, new String[]{"eventType", "case1"}, note.getInstrument(limit));
                        setFieldValue(pk, new String[]{"eventData", "case2"}, pitch);
                        pk.tryEncode();

                        if (note.getInstrument(false) >= song.getFirstCustomInstrumentIndex()) {
                            PlaySoundPacket psk = new PlaySoundPacket();
                            psk.name = song.getCustomInstruments()[note.getInstrument(false) - song.getFirstCustomInstrumentIndex()].getName();
                            psk.x = (int) ((float) p.x);
                            psk.y = (int) ((float) p.y + p.getEyeHeight());
                            psk.z = (int) ((float) p.z);
                            psk.pitch = note.getNoteSoundPitch();
                            psk.volume = (float) l.getVolume() / 100 * ((float) this.getVolume() / 100);
                            psk.tryEncode();
                            batchedPackets.add(psk);
                        } else if (clientType >= 1 && pitch < 0) {
                            PlaySoundPacket psk = new PlaySoundPacket();
                            psk.name = note.getSoundEnum(limit).getSound();
                            psk.x = (int) noteBlock.x;
                            psk.y = (int) noteBlock.y;
                            psk.z = (int) noteBlock.z;
                            psk.pitch = note.getNoteSoundPitch();
                            psk.volume = (float) l.getVolume() / 100 * ((float) this.getVolume() / 100);
                            psk.tryEncode();
                            batchedPackets.add(psk);
                        } else {
                            if (clientType > 3) {
                                int instrument = note.getInstrument(limit);
                                switch (instrument) {
                                    case 5: instrument = 6; break;
                                    case 6: instrument = 5; break;
                                    case 7: instrument = 8; break;
                                    case 8: instrument = 7; break;
                                }
                                
                                LevelSoundEventPacket pk1 = new LevelSoundEventPacket();
                                pk1.x = (float) noteBlock.x + 0.5f;
                                pk1.y = (float) noteBlock.y + 0.5f;
                                pk1.z = (float) noteBlock.z + 0.5f;
                                pk1.sound = LevelSoundEventPacket.SOUND_NOTE;
                                pk1.extraData = instrument * 256 + pitch;
                                pk1.entityIdentifier = ":";
                                pk1.tryEncode();
                                batchedPackets.add(pk1);
                            } else {
                                LevelSoundEventPacketV2 pk1 = new LevelSoundEventPacketV2();
                                pk1.x = (float) noteBlock.x + 0.5f;
                                pk1.y = (float) noteBlock.y + 0.5f;
                                pk1.z = (float) noteBlock.z + 0.5f;
                                pk1.sound = LevelSoundEventPacketV2.SOUND_NOTE;
                                pk1.extraData = (int) (note.getInstrument(limit) * 256) + pitch;
                                pk1.entityIdentifier = ":";
                                pk1.tryEncode();
                                batchedPackets.add(pk1);
                            }
                        }

                        batchedPackets.add(pk);
                    }
                }
            }
        }
        
        for (DataPacket pk: batchedPackets) {
            p.dataPacket(pk);
        }
    }
}
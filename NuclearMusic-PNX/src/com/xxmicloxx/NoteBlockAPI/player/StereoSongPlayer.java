package com.xxmicloxx.NoteBlockAPI.player;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.network.protocol.*;
import cn.nukkit.network.protocol.types.LevelSoundEvent;
import com.xxmicloxx.NoteBlockAPI.Song;
import com.xxmicloxx.NoteBlockAPI.note.Layer;
import com.xxmicloxx.NoteBlockAPI.note.Note;

import java.util.ArrayList;
import java.util.List;

public class StereoSongPlayer extends SongPlayer {
    private Block[] l4, l3, l2, l1, m0, r1, r2, r3, r4;

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

    @Override
    public void playTick(Player p, int tick) {
        if (!p.getLevel().getFolderName().equals(m0[0].getLevel().getFolderName())) {
            return;
        }
        boolean limit = ProtocolInfo.CURRENT_PROTOCOL <= 361;
        boolean trumpet_limit = ProtocolInfo.CURRENT_PROTOCOL <= 924;
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
                        pk.type = note.getInstrument(limit, trumpet_limit);
                        pk.value = pitch;

                        if (note.getInstrument(false, false) >= song.getFirstCustomInstrumentIndex())
                        {
                            PlaySoundPacket psk = new PlaySoundPacket();
                            psk.name = song.getCustomInstruments()[note.getInstrument(false, false) - song.getFirstCustomInstrumentIndex()].getName();
                            psk.x = (int) ((float) p.x);
                            psk.y = (int) ((float) p.y + p.getEyeHeight());
                            psk.z = (int) ((float) p.z);
                            psk.pitch = note.getNoteSoundPitch();
                            psk.volume = (float) l.getVolume() / 100 * ((float) this.getVolume() / 100);
                            batchedPackets.add(psk);
                        }
                        else if (pitch < 0 || (ProtocolInfo.CURRENT_PROTOCOL <= 361 && note.getInstrument(limit, trumpet_limit) == 15) || ProtocolInfo.CURRENT_PROTOCOL < 361)
                        {
                            PlaySoundPacket psk = new PlaySoundPacket();
                            psk.name = note.getSoundEnum(limit, trumpet_limit).getSound();
                            psk.x = (int) noteBlock.x;
                            psk.y = (int) noteBlock.y;
                            psk.z = (int) noteBlock.z;
                            psk.pitch = note.getNoteSoundPitch();
                            psk.volume = (float) l.getVolume() / 100 * ((float) this.getVolume() / 100);
                            batchedPackets.add(psk);
                        }
                        else
                        {
                            int instrument = note.getInstrument(limit, trumpet_limit);
                            if (ProtocolInfo.CURRENT_PROTOCOL >= 766)
                            {
                                instrument = note.NewVersionInstrument(instrument);
                            }
                            LevelSoundEventPacket pk1 = new LevelSoundEventPacket();
                            pk1.x = (float) noteBlock.x + 0.5f;
                            pk1.y = (float) noteBlock.y + 0.5f;
                            pk1.z = (float) noteBlock.z + 0.5f;
                            pk1.sound = LevelSoundEvent.NOTE;
                            pk1.extraData = instrument * 256 + pitch;
                            pk1.entityIdentifier = ":";
                            batchedPackets.add(pk1);
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
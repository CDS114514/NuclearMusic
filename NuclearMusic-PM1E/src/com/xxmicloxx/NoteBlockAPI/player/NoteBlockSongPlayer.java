package com.xxmicloxx.NoteBlockAPI.player;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.network.protocol.BlockEventPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.PlaySoundPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import com.xxmicloxx.NoteBlockAPI.*;
import com.xxmicloxx.NoteBlockAPI.note.Layer;
import com.xxmicloxx.NoteBlockAPI.note.Note;

import java.util.ArrayList;
import java.util.List;

public class NoteBlockSongPlayer extends SongPlayer {
    private Block[] noteBlock;
    public int distance = 24;

    public NoteBlockSongPlayer(Song song) {
        super(song);
    }

    public Block[] getNoteBlock() {
        return noteBlock;
    }

    public void setNoteBlock(Block noteBlock) {
        this.setNoteBlock(new Block[]{noteBlock});
    }

    public void setNoteBlock(Block[] noteBlock) {
        this.noteBlock = noteBlock;
    }

    @Override
    public void playTick(Player p, int tick) {
        if (noteBlock.length == 0) {
            return;
        }
        if (!p.getLevel().getFolderName().equals(noteBlock[0].getLevel().getFolderName())) {
            return;
        }

        boolean limit = p.protocol <= 361;
        List<DataPacket> batchedPackets = new ArrayList<>();
        int distanceSquared = distance * distance;
        
        for (Block noteBlock: this.noteBlock.clone()) {
            if (p.distanceSquared(noteBlock) < distanceSquared) {
                for (Layer l : song.getLayerHashMap().values()) {
                    Note note = l.getNote(tick);
                    if (note == null) {
                        continue;
                    }

                    int pitch = note.getKey() - 33;

                    BlockEventPacket pk = new BlockEventPacket();
                    pk.x = (int) noteBlock.x;
                    pk.y = (int) noteBlock.y;
                    pk.z = (int) noteBlock.z;
                    pk.case1 = note.getInstrument(limit);
                    pk.case2 = pitch;

                    float subtractY = (float)(100 - l.getVolume()) / 25F;
                    
                    if (note.getInstrument(false) >= song.getFirstCustomInstrumentIndex())
                    {
                        PlaySoundPacket psk = new PlaySoundPacket();
                        psk.name = song.getCustomInstruments()[note.getInstrument(false) - song.getFirstCustomInstrumentIndex()].getName();
                        psk.x = (int) ((float) p.x);
                        psk.y = (int) ((float) p.y + p.getEyeHeight());
                        psk.z = (int) ((float) p.z);
                        psk.pitch = note.getNoteSoundPitch();
                        psk.volume = (float) l.getVolume() / 100;
                        batchedPackets.add(psk);
                    }
                    else if (pitch < 0 || (p.protocol <= 361 && note.getInstrument(limit) == 15) || p.protocol < 361)
                    {
                        PlaySoundPacket psk = new PlaySoundPacket();
                        psk.name = note.getSoundEnum(limit).getSound();
                        psk.x = (int) noteBlock.x;
                        psk.y = (int) noteBlock.y;
                        psk.z = (int) noteBlock.z;
                        psk.pitch = note.getNoteSoundPitch();
                        psk.volume = (float) l.getVolume() / 100;
                        batchedPackets.add(psk);
                    }
                    else
                    {
                        int instrument = note.NewVersionInstrument(note.getInstrument(limit));
                        //if (p.protocol >= 766)
                        //{
                        //    instrument = note.NewVersionInstrument(instrument);
                        //}
                        LevelSoundEventPacket pk1 = new LevelSoundEventPacket();
                        pk1.x = (float) noteBlock.x + 0.5f;
                        pk1.y = (float) noteBlock.y - subtractY + 0.5f;
                        pk1.z = (float) noteBlock.z + 0.5f;
                        pk1.sound = LevelSoundEventPacket.SOUND_NOTE;
                        pk1.extraData = instrument * 256 + pitch;
                        pk1.entityIdentifier = ":";
                        batchedPackets.add(pk1);

                    }

                    batchedPackets.add(pk);
                }
            }
        }
        
        for (DataPacket pk: batchedPackets) {
            p.dataPacket(pk);
        }
    }
}
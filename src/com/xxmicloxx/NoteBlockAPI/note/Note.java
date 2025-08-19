package com.xxmicloxx.NoteBlockAPI.note;

import com.xxmicloxx.NoteBlockAPI.nukkit.SoundEnum;

public class Note {

    private byte instrument;
    private byte key;

    public Note(byte instrument, byte key) {
        switch (instrument) {
            case 1:
                instrument = 4;
                break;
            case 2:
                instrument = 1;
                break;
            case 3:
                instrument = 2;
                break;
            case 4:
                instrument = 3;
                break;
            case 5:
                instrument = 8;
                break;
            case 7:
                instrument = 5;
                break;
            case 8:
                instrument = 7;
                break;
        }
        this.instrument = instrument;
        this.key = key;
    }

    public byte getInstrument(boolean limit) {
        if (limit && instrument > 4 && instrument != 15) return 0;
        return instrument;
    }

    public SoundEnum getSoundEnum(boolean limit) {
        switch (getInstrument(limit)) {
            case 0:
                return SoundEnum.NOTE_HARP;
            case 1:
                return SoundEnum.NOTE_BD;
            case 2:
                return SoundEnum.NOTE_SNARE;
            case 3:
                return SoundEnum.NOTE_HAT;
            case 4:
                return SoundEnum.NOTE_BASS;
            case 5:
                return SoundEnum.NOTE_BELL;
            case 6:
                return SoundEnum.NOTE_FLUTE;
            case 7:
                return SoundEnum.NOTE_CHIME;
            case 8:
                return SoundEnum.NOTE_GUITAR;
            case 9:
                return SoundEnum.NOTE_XYLOPHONE;
            case 10:
                return SoundEnum.NOTE_IRON_XYLOPHONE;
            case 11:
                return SoundEnum.NOTE_COW_BELL;
            case 12:
                return SoundEnum.NOTE_DIDGERIDOO;
            case 13:
                return SoundEnum.NOTE_BIT;
            case 14:
                return SoundEnum.NOTE_BANJO;
            case 15:
                return SoundEnum.NOTE_PLING;
            default:
                return SoundEnum.NOTE_HARP;
        }
    }

    public void setInstrument(byte instrument) {
        this.instrument = instrument;
    }

    public byte getKey() {
        return key;
    }

    public void setKey(byte key) {
        this.key = key;
    }

    public float getNoteSoundPitch() {
        return (float) Math.pow(2d, ((double) key - 33d - 12d) / 12d);
    }

}

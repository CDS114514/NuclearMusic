package com.xxmicloxx.NoteBlockAPI.note;

import com.xxmicloxx.NoteBlockAPI.SoundEnum;

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

    public byte getInstrument(boolean limit, boolean trumpet_limit) {
        if (trumpet_limit && instrument > 15 && instrument < 20) return 0;
        if (limit && instrument > 4 && instrument != 15) return 0;
        return instrument;
    }

    public SoundEnum getSoundEnum(boolean limit, boolean trumpet_limit) {
        switch (getInstrument(limit, trumpet_limit)) {
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
            case 16:
                return SoundEnum.NOTE_TRUMPET;
            case 17:
                return SoundEnum.NOTE_TRUMPET_EXPOSED;
            case 18:
                return SoundEnum.NOTE_TRUMPET_WEATHERED;
            case 19:
                return SoundEnum.NOTE_TRUMPET_OXIDIZED;
            default:
                return SoundEnum.NOTE_HARP;
        }
    }

    public int NewVersionInstrument(int instrument)
    {
        switch (instrument) {
            case 5:
                return 6;
            case 6:
                return 5;
            case 7:
                return 8;
            case 8:
                return 7;
            default:
                return instrument;
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

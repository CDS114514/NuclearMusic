package com.xxmicloxx.NoteBlockAPI;

import java.util.Arrays;

/**
 * @author CreeperFace
 */
public enum SoundEnum {
    NOTE_BANJO("note.banjo"),
    NOTE_BASS("note.bass"),
    NOTE_BASSATTACK("note.bassattack"),
    NOTE_BD("note.bd"),
    NOTE_BELL("note.bell"),
    NOTE_BIT("note.bit"),
    NOTE_CHIME("note.chime"),
    NOTE_COW_BELL("note.cow_bell"),
    NOTE_CREEPER("note.creeper"),
    NOTE_DIDGERIDOO("note.didgeridoo"),
    NOTE_ENDERDRAGON("note.enderdragon"),
    NOTE_FLUTE("note.flute"),
    NOTE_GUITAR("note.guitar"),
    NOTE_HARP("note.harp"),
    NOTE_HAT("note.hat"),
    NOTE_IRON_XYLOPHONE("note.iron_xylophone"),
    NOTE_PIGLIN("note.piglin"),
    NOTE_PLING("note.pling"),
    NOTE_SKELETON("note.skeleton"),
    NOTE_SNARE("note.snare"),
    NOTE_TRUMPET("note.trumpet"),
    NOTE_TRUMPET_EXPOSED("note.trumpet_exposed"),
    NOTE_TRUMPET_OXIDIZED("note.trumpet_oxidized"),
    NOTE_TRUMPET_WEATHERED("note.trumpet_weathered"),
    NOTE_WITHERSKELETON("note.witherskeleton"),
    NOTE_XYLOPHONE("note.xylophone"),
    NOTE_ZOMBIE("note.zombie");

    private final String sound;

    SoundEnum(String sound) {
        this.sound = sound;
    }

    public String getSound() {
        return this.sound;
    }

    public static SoundEnum fromName(String name) {
        return Arrays.stream(values()).filter(e -> e.sound.equals(name)).findFirst().orElse(null);
    }
}

package dev.chililisoup.bigsignwriter.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContinuousWritingConfigTest {
    @Test
    void oldSettingsDefaultOffAndCopiesAndJsonPreserveTheExplicitChoice() {
        Gson gson = new Gson();
        var old = gson.fromJson("{\"buttonsWidth\":240}", BigSignWriterConfig.PersistentConfig.class);
        assertFalse(old.continuousWriting);
        assertFalse(new BigSignWriterConfig.MainConfig().continuousWriting);
        old.continuousWriting = true;
        var copy = new BigSignWriterConfig.PersistentConfig().copyFrom(old);
        assertTrue(copy.continuousWriting);
        assertEquals(240, copy.buttonsWidth);
        assertTrue(gson.fromJson(gson.toJson(copy), BigSignWriterConfig.PersistentConfig.class).continuousWriting);
    }
}

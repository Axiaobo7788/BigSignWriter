package dev.chililisoup.bigsignwriter.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TranslationsTest {
    private static final Path LANG = Path.of("src/main/resources/assets/bigsignwriter/lang");
    private static final Pattern ARGUMENT = Pattern.compile("%(?:\\d+\\$)?[sd]");

    @Test
    void allSupportedLanguagesHaveEveryKeyAndMatchingFormatArguments() throws Exception {
        JsonObject english = read("en_us");
        for (String locale : new String[]{"zh_cn", "zh_tw", "ja_jp", "ko_kr"}) {
            JsonObject translated = read(locale);
            assertEquals(english.keySet(), translated.keySet(), locale);
            for (String key : english.keySet()) {
                String value = translated.get(key).getAsString();
                assertFalse(value.isBlank(), locale + ": " + key);
                assertEquals(ARGUMENT.matcher(english.get(key).getAsString()).results().map(m -> m.group()).toList(),
                        ARGUMENT.matcher(value).results().map(m -> m.group()).toList(), locale + ": " + key);
            }
        }
    }

    private static JsonObject read(String locale) throws Exception {
        return JsonParser.parseString(Files.readString(LANG.resolve(locale + ".json"))).getAsJsonObject();
    }
}

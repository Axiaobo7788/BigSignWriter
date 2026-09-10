package dev.chililisoup.bigsignwriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.chililisoup.bigsignwriter.config.BigSignWriterConfig;
import dev.chililisoup.bigsignwriter.config.ConfigInterface;
import dev.chililisoup.bigsignwriter.font.FamilyCharacterProvider;
import dev.chililisoup.bigsignwriter.font.FontFile;
import dev.chililisoup.bigsignwriter.font.FontInfo;
import dev.chililisoup.bigsignwriter.font.GlyphLookup;
import dev.chililisoup.bigsignwriter.font.SymbolGroup;
import dev.chililisoup.bigsignwriter.resources.BigFontManager;
import dev.chililisoup.bigsignwriter.resources.BigFontResourceProvider;
import dev.chililisoup.bigsignwriter.input.PendingSignText;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class BigSignWriter {
    private static final BigFontResourceProvider BIG_FONT_RESOURCE_PROVIDER = new BigFontResourceProvider();
    private static final BigFontManager BIG_FONT_MANAGER = new BigFontManager();
    public static final String MOD_ID = "bigsignwriter";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final String LOGGER_PREFIX = "[BSW] ";
    public static final Identifier ICON = id("icon.png");
    public static String VERSION;
    public static Path CONFIG_DIR;
    public static final PendingSignText PENDING_TEXT = new PendingSignText();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier userFontId(String path) {
        return id("user/" + path.toLowerCase().replaceAll("[^a-z0-9_.-]", ""));
    }

    public static void initialize(String version, Path configDir) {
        VERSION = version;
        CONFIG_DIR = configDir;

        BigSignWriterConfig.init();
    }

    public static boolean isVanillaTyping() {
        return selectedFont() == null;
    }

    public static void reloadUserFonts() {
        BIG_FONT_MANAGER.reloadUserFonts();
    }

    public static List<FontInfo> availableFonts() {
        return BIG_FONT_MANAGER.availableFonts();
    }

    public static List<SymbolGroup> availableSymbolGroups() {
        return BIG_FONT_MANAGER.availableSymbolGroups();
    }

    public static @Nullable FontInfo getFont(Identifier id) {
        for (FontInfo fontInfo : availableFonts())
            if (fontInfo.id.equals(id)) return fontInfo;
        return null;
    }

    public static @Nullable FontInfo selectedFont() {
        return BIG_FONT_MANAGER.selectedFont();
    }

    public static String characterSeparator() {
        return BIG_FONT_MANAGER.characterSeparator();
    }

    public static int height() {
        FontInfo selected = selectedFont();
        return selected != null ? selected.height() : 1;
    }

    public static BigFontResourceProvider getBigFontResourceProvider() {
        return BIG_FONT_RESOURCE_PROVIDER;
    }

    public static BigFontManager getBigFontManager() {
        return BIG_FONT_MANAGER;
    }

    public static void selectFont(@Nullable FontInfo fontInfo) {
        BIG_FONT_MANAGER.selectFont(fontInfo);
    }

    public static void reselectFont() {
        BIG_FONT_MANAGER.reselectFont();
    }

    public static Optional<String[]> getBigChar(int codePoint, @Nullable FamilyCharacterProvider fontInfo) {
        return GlyphLookup.find(codePoint, fontInfo);
    }

    public static Optional<String[]> getBigChar(int codePoint) {
        return getBigChar(codePoint, selectedFont());
    }

    public static @NotNull Path getFontsDir() {
        Path fontsDir = BigSignWriterConfig.getConfigDir().resolve("fonts");
        try {
            Files.createDirectories(fontsDir);
        } catch (IOException e) {
            LOGGER.error(LOGGER_PREFIX + "Failed to create fonts directory: {}", fontsDir, e);
        }
        return fontsDir;
    }

    public static void copyFontToFile(FontInfo fontInfo) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path configFonts = getFontsDir();

        try {
            String path = fontInfo.id.getPath() + "_copy";
            String fontName = fontInfo.name() + " Copy";
            Path target = configFonts.resolve(path + ".json");
            int i = 1;
            while (Files.exists(target)) {
                fontName = fontInfo.name() + " Copy (" + i + ")";
                target = configFonts.resolve(path + "_" + i++ + ".json");
            }

            FontFile copy = fontInfo.fontFile.copyWithUnsafeCharacters();
            copy.name = fontName;
            getFontFileInterface(gson, target).save(copy);
            reloadUserFonts();
        } catch (Exception e) {
            LOGGER.error(LOGGER_PREFIX + "Error saving font", e);
        }
    }

    public static ConfigInterface<FontFile> getFontFileInterface(Gson gson, Path path) {
        Gson fontGson = gson.newBuilder().registerTypeAdapter(FontFile.class, new FontFile.GsonAdapter()).create();
        return new ConfigInterface<>(fontGson, new TypeToken<>() {}, path, new FontFile());
    }
}

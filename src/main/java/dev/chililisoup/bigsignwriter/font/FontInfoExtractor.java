package dev.chililisoup.bigsignwriter.font;

import dev.chililisoup.bigsignwriter.resources.BigFontManager;
import dev.chililisoup.bigsignwriter.resources.BitmapFontLoader;
import dev.chililisoup.bigsignwriter.BigSignWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static dev.chililisoup.bigsignwriter.config.BigSignWriterConfig.MAIN_CONFIG;

public final class FontInfoExtractor {
    public static Map<Identifier, FontInfoExtraction> prepareFonts(Map<Identifier, FontFile> fontSources) {
        Map<Identifier, FontInfoExtraction> preparedFonts = fontSources.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new FontInfoExtraction(entry.getValue(), entry.getKey())
                ));
        preparedFonts.values().forEach(extraction -> extraction.preparedFonts = preparedFonts);
        preparedFonts.values().forEach(FontInfoExtraction::checkParentCycle);
        return preparedFonts;
    }

    public static List<FontInfo> extractAll(Map<Identifier, FontInfoExtraction> preparedFonts) {
        preparedFonts.values().forEach(FontInfoExtraction::ensureInfoChecked);
        return preparedFonts.values().stream().map(FontInfoExtraction::get).toList();
    }

    private static String createRangeInfo(ArrayList<Integer> samples) {
        if (samples.isEmpty()) return "0";
        int minSample = Collections.min(samples);
        int maxSample = Collections.max(samples);
        return minSample == maxSample ?
                String.valueOf(minSample) :
                String.format(
                        "%d-%d ~%.2f",
                        minSample,
                        maxSample,
                        (float) samples.stream().mapToInt(Integer::intValue).sum() / samples.size()
                );
    }

    public static class FontInfoExtraction implements FamilyCharacterProvider {
        final FontFile fontFile;
        final Identifier id;

        @Nullable FontInfoExtraction parentFont = null;
        private @Nullable FontInfoExtraction rootAncestorFont = null;
        @Nullable Component error = null;
        @Nullable BitmapFontLoader.Loaded bitmapFont = null;
        private boolean cyclicParent = false;
        private @Nullable TreeSet<Integer> cumulativeCharacters = null;
        String widthInfo = "0";
        @Nullable String cumulativeWidthInfo = null;
        @Nullable Component symbolError = null;
        String symbolWidthInfo = "0";
        String symbolHeightInfo = "0";

        private Map<Identifier, FontInfoExtraction> preparedFonts = Map.of();
        private boolean relationsChecked = false;
        private boolean infoChecked = false;
        private @Nullable FontInfo result = null;

        private FontInfoExtraction(FontFile fontFile, Identifier id) {
            this.fontFile = fontFile;
            this.id = id;
        }

        FontInfo get() {
            if (this.result == null)
                this.result = new FontInfo(this);
            return this.result;
        }

        private int height() {
            return this.fontFile.getHeight();
        }

        @Override
        public Map<Integer, String[]> characters() {
            return this.fontFile.getCharacters();
        }

        @Override
        public @Nullable BigGlyphProvider glyphProvider() {
            this.ensureInfoChecked();
            return this.bitmapFont != null ? this.bitmapFont.provider() : null;
        }

        private void checkParentCycle() {
            Set<Identifier> seen = new HashSet<>();
            FontInfoExtraction next = this;
            while (next != null) {
                if (!seen.add(next.id)) {
                    this.cyclicParent = true;
                    return;
                }
                Identifier parent = next.fontFile.parentFont().orElse(null);
                next = parent != null ? this.preparedFonts.get(parent) : null;
            }
        }

        private void ensureRelationsChecked() {
            if (this.relationsChecked) return;
            this.relationsChecked = true;

            this.parentFont = this.findParent();
            this.rootAncestorFont = this.findRootAncestor();
        }

        @Override
        public boolean parentIsImplicit() {
            return this.fontFile.parentFont().isEmpty();
        }

        private boolean hasExplicitParent() {
            return !this.parentIsImplicit() && this.parentFont() != null;
        }

        @Override
        public @Nullable FontInfoExtraction parentFont() {
            this.ensureRelationsChecked();
            return this.parentFont;
        }

        public @Nullable FontInfo parentFontInfo() {
            FontInfoExtraction parentFont = this.parentFont();
            return parentFont != null ? parentFont.get() : null;
        }

        public @Nullable FontInfo rootAncestorFont() {
            return this.rootAncestorFont != null ? this.rootAncestorFont.get() : null;
        }

        private @Nullable FontInfoExtraction findRootAncestor() {
            if (this.parentIsImplicit()) return null;
            return this.parentFont != null && this.parentFont.hasExplicitParent() ?
                    this.parentFont.findRootAncestor() :
                    this.parentFont;
        }

        public Set<Integer> cumulativeCharacters() {
            if (this.cumulativeCharacters != null) return this.cumulativeCharacters;
            if (!this.hasExplicitParent()) return this.characters().keySet();

            this.cumulativeCharacters = new TreeSet<>(FontFile::compareChars);
            FontInfoExtraction nextFont = this;
            while (nextFont != null) {
                this.cumulativeCharacters.addAll(nextFont.characters().keySet());
                nextFont = nextFont.hasExplicitParent() ? nextFont.parentFont() : null;
            }
            return this.cumulativeCharacters;
        }

        public Map<String, String[]> symbols() {
            Map<Integer, String[]> characters = this.characters();
            LinkedHashMap<String, String[]> symbols = new LinkedHashMap<>();

            if (this.fontFile.symbols != null) symbols.putAll(this.fontFile.symbols);

            if (MAIN_CONFIG.nonUSCharactersInSymbols)
                characters.entrySet().stream()
                        .filter(entry -> entry.getKey() < 32 || entry.getKey() > 126)
                        .forEach(entry -> symbols.put(UnicodeCodePoints.toKey(entry.getKey()), entry.getValue()));

            return symbols.isEmpty() ? Map.of() : symbols;
        }

        private String widthInfo() {
            this.ensureInfoChecked();
            return this.widthInfo;
        }

        private void ensureInfoChecked() {
            if (this.infoChecked) return;
            this.infoChecked = true;
            this.error = this.extractInfo();
            this.symbolError = this.extractSymbolInfo();
        }

        private @Nullable Component extractInfo() {
            if (this.cyclicParent) return Component.translatable("bigsignwriter.font.error.cyclicParent");
            if (this.fontFile.height != null && this.fontFile.height <= 0) return Component.translatable(
                    "bigsignwriter.font.error.invalidHeight",
                    fontFile.height
            );

            this.parentFont = this.findParent();
            this.rootAncestorFont = this.findRootAncestor();
            if (this.fontFile.bitmapFont().isPresent()) {
                try {
                    this.bitmapFont = BitmapFontLoader.load(this.fontFile.bitmapFont().get(), this.height(),
                            Minecraft.getInstance().getResourceManager(), Minecraft.getInstance().font);
                    this.widthInfo = "≤" + this.bitmapFont.maxWidth();
                } catch (IOException e) {
                    BigSignWriter.LOGGER.warn(BigSignWriter.LOGGER_PREFIX + "Cannot load bitmap font {}", this.id, e);
                    return Component.translatable("bigsignwriter.font.error.bitmap", e.getMessage());
                }
            }
            if (this.fontFile.getCharacters().isEmpty()) {
                if (!this.parentIsImplicit() && this.parentFont != null)
                    this.cumulativeWidthInfo = this.parentFont.widthInfo();
                return null;
            }

            Font font = Minecraft.getInstance().font;
            Set<Integer> cumulativeCharacters = this.cumulativeCharacters();
            ArrayList<Integer> ownWidths = new ArrayList<>(this.fontFile.getCharacters().size());
            ArrayList<Integer> cumulativeWidths = new ArrayList<>(cumulativeCharacters.size());

            for (int chr : cumulativeCharacters) {
                String[] bigChar = this.fontFile.getCharacters().get(chr);
                if (bigChar == null && this.parentFont != null)
                    bigChar = BigSignWriter.getBigChar(chr, this.parentFont).orElse(null);
                if (bigChar == null) continue;

                if (bigChar.length != this.fontFile.getHeight()) return Component.translatable(
                        "bigsignwriter.font.error.wrongLineCount",
                        UnicodeCodePoints.toKey(chr),
                        bigChar.length,
                        this.fontFile.getHeight()
                );

                int[] widths = new int[bigChar.length];
                int topWidth = font.width(bigChar[0]);
                widths[0] = topWidth;

                if (this.fontFile.getCharacters().containsKey(chr)) {
                    boolean unfixed = false;
                    for (int i = 1; i < bigChar.length; i++) {
                        widths[i] = font.width(bigChar[i]);
                        if (widths[i] != widths[0]) unfixed = true;
                    }
                    if (unfixed) return Component.translatable(
                            "bigsignwriter.font.error.unfixedWidth",
                            UnicodeCodePoints.toKey(chr),
                            Arrays.toString(widths)
                    );

                    ownWidths.add(topWidth);
                }

                cumulativeWidths.add(topWidth);
            }

            this.widthInfo = this.bitmapFont == null ? createRangeInfo(ownWidths) : "≤" + this.bitmapFont.maxWidth();
            if (!this.parentIsImplicit()) {
                String cumulativeWidthInfo = createRangeInfo(cumulativeWidths);
                if (!this.widthInfo.equals(cumulativeWidthInfo))
                    this.cumulativeWidthInfo = cumulativeWidthInfo;
            }

            return null;
        }

        private @Nullable Component extractSymbolInfo() {
            if (this.fontFile.symbols == null || this.fontFile.symbols.isEmpty())
                return null;

            Font font = Minecraft.getInstance().font;
            ArrayList<Integer> allWidths = new ArrayList<>(this.fontFile.symbols.size());
            ArrayList<Integer> allHeights = new ArrayList<>(this.fontFile.symbols.size());

            for (Map.Entry<String, String[]> entry : this.fontFile.symbols.entrySet()) {
                String key = entry.getKey();
                String[] symbol = entry.getValue();

                if (symbol.length == 0)
                    return Component.translatable("bigsignwriter.font.error.emptySymbol", key);

                int[] widths = new int[symbol.length];
                int topWidth = font.width(symbol[0]);
                widths[0] = topWidth;

                boolean unfixed = false;
                for (int i = 1; i < symbol.length; i++) {
                    widths[i] = font.width(symbol[i]);
                    if (widths[i] != widths[0]) unfixed = true;
                }
                if (unfixed) return Component.translatable(
                        "bigsignwriter.font.error.unfixedSymbolWidth",
                        key,
                        Arrays.toString(widths)
                );

                allWidths.add(topWidth);
                allHeights.add(symbol.length);
            }

            this.symbolWidthInfo = createRangeInfo(allWidths);
            this.symbolHeightInfo = createRangeInfo(allHeights);

            return null;
        }

        private @Nullable FontInfoExtraction findParent() {
            if (this.cyclicParent) return null;
            if (this.id.equals(BigFontManager.DEFAULT_FONT_ID)) return null;

            FontInfoExtraction parentFont = null;
            if (this.fontFile.parentFont().isEmpty()) {
                if (this.height() == 4) {
                    parentFont = this.preparedFonts.get(BigFontManager.DEFAULT_FONT_ID);
                } else return null;
            } else for (FontInfoExtraction extraction : this.preparedFonts.values()) {
                if (this == extraction) continue;
                if (extraction.id.equals(this.fontFile.parentFont().get())) {
                    parentFont = extraction;
                    break;
                }
            }

            if (parentFont == null || parentFont.cyclicParent) return null;
            if (parentFont.height() != this.height()) return null;

            // An explicit parent may inherit its own glyphs through an empty alias.
            // Inspecting only its local character map would silently sever that chain.
            if (!this.parentIsImplicit() || parentFont.fontFile.bitmapFont().isPresent()) return parentFont;

            for (int chr : parentFont.characters().keySet()) {
                if (!this.characters().containsKey(chr)
                        && !this.characters().containsKey(Character.toUpperCase(chr))
                ) return parentFont;
            }

            return null;
        }
    }
}

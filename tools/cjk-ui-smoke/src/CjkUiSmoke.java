import dev.chililisoup.bigsignwriter.BigSignWriter;
import dev.chililisoup.bigsignwriter.font.FontInfo;
import dev.chililisoup.bigsignwriter.gui.sign.FontSelectionWidget;
import dev.chililisoup.bigsignwriter.input.SignEditContext;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/** Opt-in Fabric 1.21.11 integration fixture. Never packaged in the mod JAR. */
public final class CjkUiSmoke implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext test) {
        try (TestSingleplayerContext world = test.worldBuilder().create()) {
            world.getClientWorld().waitForChunksDownload();
            test.runOnClient(mc -> {
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                dev.chililisoup.bigsignwriter.config.BigSignWriterConfig.MAIN_CONFIG.continuousWriting = false;
                dev.chililisoup.bigsignwriter.config.BigSignWriterConfig.saveConfig();
                FontInfo root = BigSignWriter.availableFonts().stream()
                        .filter(f -> f.id.equals(BigSignWriter.id("cjk_pixel_12_zh_hans"))).findFirst().orElseThrow();
                require(root.name().equals("CJK Pixel 12"), "Unified font name");
                require(root.children().isEmpty(), "Unified CJK must have no language submenu");
                BigSignWriter.selectFont(root);
                BigSignWriter.PENDING_TEXT.clear();
            });
            test.getInput().resizeWindow(1280, 854);
            BlockPos pos = test.computeOnClient(mc -> mc.player.blockPosition().offset(2, 0, 0));
            CjkOptionsSmoke.verifyBehavior(test, world, pos);
            openSign(test, world, pos, false);
            test.getInput().typeChars("你好世界");
            checkToolbar(test, false, 3);
            test.takeScreenshot("cjk-ui-normal-full");
            test.getInput().resizeWindow(640, 480);
            checkToolbar(test, false, 3);
            test.takeScreenshot("cjk-ui-compact-full");
            test.getInput().resizeWindow(1280, 854);
            test.clickScreenButton("gui.done");
            test.waitTick();
            world.getServer().runOnServer(server -> {
                SignBlockEntity sign = (SignBlockEntity) server.overworld().getBlockEntity(pos);
                require(sign.getFrontText().getMessage(0, false).getString().equals(
                        BigSignWriter.getBigChar('你').orElseThrow()[0]), "Vanilla commit did not reach test server");
            });
            openSign(test, world, pos.east(), true);
            checkToolbar(test, true, 3);
            test.takeScreenshot("cjk-ui-next-sign");
            click(test, "bigsignwriter.input.continueQueue");
            checkToolbar(test, false, 2);
            test.takeScreenshot("cjk-ui-hanging-full");
            test.runOnClient(mc -> selector(mc.screen).setOpen(true));
            test.runOnClient(mc -> {
                FontSelectionWidget selector = selector(mc.screen);
                long visible = selector.children().stream().filter(e -> e.getHeight() > 0)
                        .filter(e -> e.getNarration().getString().contains("CJK Pixel 12")).count();
                require(visible == 1, "Unified CJK menu has " + visible + " visible entries");
                require(!button(mc.screen, "bigsignwriter.input.continueQueue").visible, "Queue visible over dropdown");
                require(!button(mc.screen, "bigsignwriter.input.clearQueue").visible, "Clear visible over dropdown");
            });
            test.takeScreenshot("cjk-ui-single-font");
            test.runOnClient(mc -> selector(mc.screen).setOpen(false));
            click(test, "bigsignwriter.input.clearQueue");
            test.runOnClient(mc -> require(!button(mc.screen, "bigsignwriter.input.continueQueue").visible,
                    "Empty queue toolbar is still visible"));
            test.clickScreenButton("gui.done");
            // One selected font, one mixed phrase: simplified/traditional, both
            // Kana scripts, Korean and a supplementary Han. Never switch fonts.
            int[] mixed = "汉漢あア한𠁨".codePoints().toArray();
            for (int i = 0; i < mixed.length; i++) {
                BlockPos mixedPos = pos.offset(i, 0, 2);
                int cp = mixed[i];
                int pending = mixed.length - i - 1;
                openSign(test, world, mixedPos, false);
                if (i == 0) {
                    // Fabric 4.3.5 typeChars uses String.chars(), splitting
                    // supplementary scalars. typeChar(int) models GLFW correctly.
                    for (int scalar : mixed) test.getInput().typeChar(scalar);
                } else click(test, "bigsignwriter.input.continueQueue");
                test.runOnClient(mc -> {
                    require(BigSignWriter.selectedFont().name().equals("CJK Pixel 12"), "Font changed during mixed input");
                    require(BigSignWriter.PENDING_TEXT.size() == pending, "Mixed phrase queue count: expected "
                            + pending + ", got " + BigSignWriter.PENDING_TEXT.size());
                    require(java.util.Arrays.equals(context(mc.screen).messages, BigSignWriter.getBigChar(cp).orElseThrow()),
                            "Mixed-script preview mismatch U+" + Integer.toHexString(cp));
                });
                test.takeScreenshot("cjk-ui-mixed-" + Integer.toHexString(cp));
                test.clickScreenButton("gui.done");
                test.waitTick();
                world.getServer().runOnServer(server -> {
                    var sign = (SignBlockEntity) server.overworld().getBlockEntity(mixedPos);
                    String[] expected = BigSignWriter.getBigChar(cp).orElseThrow();
                    for (int row = 0; row < 4; row++)
                        require(sign.getFrontText().getMessage(row, false).getString().equals(expected[row]),
                                "Mixed-script vanilla commit mismatch U+" + Integer.toHexString(cp));
                });
            }
            for (boolean hanging : new boolean[]{false, true}) {
                BlockPos daoPos = pos.offset(hanging ? 1 : 0, 0, 4);
                openSign(test, world, daoPos, hanging);
                test.getInput().typeChar('道');
                test.runOnClient(mc -> require(java.util.Arrays.equals(context(mc.screen).messages,
                        BigSignWriter.getBigChar('道').orElseThrow()), "Dao preview rows changed"));
                test.takeScreenshot("cjk-ui-dao-" + (hanging ? "hanging" : "normal"));
                test.clickScreenButton("gui.done");
                test.waitTick();
                world.getServer().runOnServer(server -> {
                    var sign = (SignBlockEntity) server.overworld().getBlockEntity(daoPos);
                    String[] expected = BigSignWriter.getBigChar('道').orElseThrow();
                    for (int row = 0; row < 4; row++)
                        require(sign.getFrontText().getMessage(row, false).getString().equals(expected[row]),
                                "Dao vanilla commit row changed: " + row);
                });
            }
            CjkOptionsSmoke.verifyLanguages(test, world, pos);
            System.out.println("CJK_UI_SMOKE_PASS: Dao normal/hanging previews and commits, relocated input option, optional configuration, silent missing glyphs, font switching, five languages, normal/compact/hanging layout, capacity probe, pending continuation, vanilla commit, single multilingual entry, simplified/traditional/Kana/Hangul/supplementary mixed input and commits, clear");
        }
    }

    static void openSign(ClientGameTestContext test, TestSingleplayerContext world, BlockPos pos, boolean hanging) {
        world.getServer().runOnServer(server -> server.overworld().removeBlock(pos, false));
        test.waitFor(mc -> mc.level.getBlockEntity(pos) == null);
        world.getServer().runOnServer(server -> {
            var level = server.overworld();
            level.setBlock(pos, (hanging ? Blocks.OAK_HANGING_SIGN : Blocks.OAK_SIGN).defaultBlockState(), 3);
            SignBlockEntity sign = (SignBlockEntity) level.getBlockEntity(pos);
            sign.setAllowedPlayerEditor(server.getPlayerList().getPlayers().getFirst().getUUID());
        });
        test.waitFor(mc -> mc.level.getBlockEntity(pos) instanceof SignBlockEntity entity
                && entity.getFrontText().getMessage(0, false).getString().isEmpty());
        test.runOnClient(mc -> mc.player.openTextEdit((SignBlockEntity) mc.level.getBlockEntity(pos), true));
        test.waitTick();
    }

    static void checkToolbar(ClientGameTestContext test, boolean active, int pending) {
        test.runOnClient(mc -> {
            Screen screen = mc.screen;
            Button done = button(screen, "gui.done");
            Button next = button(screen, "bigsignwriter.input.continueQueue");
            Button clear = button(screen, "bigsignwriter.input.clearQueue");
            require(done.visible && next.visible && clear.visible, "Toolbar or Done is hidden");
            require(next.active == active, "Wrong continuation enabled state");
            require(!overlaps(done, next) && !overlaps(done, clear) && !overlaps(next, clear), "Buttons overlap");
            require(next.getY() >= 0 && next.getBottom() <= screen.height, "Toolbar outside screen");
            require(mc.font.width(net.minecraft.network.chat.Component.translatable("bigsignwriter.input.signFull", net.minecraft.network.chat.Component.translatable("gui.done")))
                    <= screen.width - 8, "Sign-full instruction is clipped");
            SignEditContext context = context(screen);
            String[] before = context.messages.clone();
            require(context.fontTyper.canContinuePendingText() == active, "Capacity probe result");
            require(java.util.Arrays.equals(before, context.messages) && BigSignWriter.PENDING_TEXT.size() == pending,
                    "Capacity probe changed rows or queue count differs: expected " + pending
                            + ", got " + BigSignWriter.PENDING_TEXT.size());
        });
    }

    static SignEditContext context(Screen screen) {
        for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField("bigSignWriter$context");
                field.setAccessible(true);
                return (SignEditContext) field.get(screen);
            } catch (NoSuchFieldException ignored) {
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
        throw new AssertionError("Missing sign edit context");
    }

    static Button button(Screen screen, String key) {
        String text = net.minecraft.network.chat.Component.translatable(key,
                BigSignWriter.PENDING_TEXT.size()).getString();
        return screen.children().stream().filter(e -> e instanceof Button).map(e -> (Button) e)
                .filter(b -> b.getMessage().getString().equals(text)).findFirst().orElseThrow(() -> new AssertionError(key));
    }

    static void click(ClientGameTestContext test, String key) {
        test.runOnClient(mc -> {
            Button button = button(mc.screen, key);
            require(button.active && button.visible, "Cannot click " + key);
            require(mc.screen.mouseClicked(new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                    button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(0, 0)), false),
                    "Screen did not handle " + key);
        });
        test.waitTick();
    }

    static FontSelectionWidget selector(Screen screen) {
        return screen.children().stream().filter(e -> e instanceof FontSelectionWidget)
                .map(e -> (FontSelectionWidget) e).findFirst().orElseThrow();
    }

    static boolean overlaps(Button a, Button b) {
        return a.getX() < b.getRight() && a.getRight() > b.getX() && a.getY() < b.getBottom() && a.getBottom() > b.getY();
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

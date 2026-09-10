import dev.chililisoup.bigsignwriter.BigSignWriter;
import dev.chililisoup.bigsignwriter.config.BigSignWriterConfig;
import dev.chililisoup.bigsignwriter.gui.TickBox;
import dev.chililisoup.bigsignwriter.gui.config.BigSignWriterConfigScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.stream.Stream;

/** Optional writing and localization regressions in the disposable test client. */
final class CjkOptionsSmoke {
    private static final String OPTION = "bigsignwriter.config.continuousWriting";

    static void verifyBehavior(ClientGameTestContext test, TestSingleplayerContext world, BlockPos pos) {
        test.runOnClient(mc -> {
            CjkUiSmoke.require(!BigSignWriterConfig.MAIN_CONFIG.continuousWriting, "Fresh/default setting must be off");
            mc.gui.setOverlayMessage(Component.literal("unchanged"), false);
        });
        CjkUiSmoke.openSign(test, world, pos, false);
        test.getInput().typeChars("你好");
        test.runOnClient(mc -> {
            CjkUiSmoke.require(BigSignWriter.PENDING_TEXT.isEmpty(), "Disabled mode queued text");
            CjkUiSmoke.require(!CjkUiSmoke.button(mc.screen, "bigsignwriter.input.continueQueue").visible,
                    "Disabled toolbar visible");
            CjkUiSmoke.require(overlay(mc).equals("unchanged"), "Disabled overflow emitted a HUD message");
        });
        test.getInput().pressKey(GLFW.GLFW_KEY_BACKSPACE);
        test.runOnClient(mc -> CjkUiSmoke.require(Arrays.stream(CjkUiSmoke.context(mc.screen).messages).allMatch(String::isEmpty),
                "Disabled Backspace did not edit the current sign"));
        select(test, "Default");
        test.getInput().typeChars("马A");
        test.runOnClient(mc -> {
            CjkUiSmoke.require(Arrays.equals(CjkUiSmoke.context(mc.screen).messages, BigSignWriter.getBigChar('A').orElseThrow()),
                    "Unsupported Chinese blocked the following English character");
            CjkUiSmoke.require(overlay(mc).equals("unchanged"), "Missing glyph emitted a HUD message");
            mc.gui.setOverlayMessage(Component.empty(), false);
        });
        test.takeScreenshot("cjk-options-default-off");
        setOption(test, true);
        test.runOnClient(mc -> {
            BigSignWriterConfig.MAIN_CONFIG.continuousWriting = false;
            BigSignWriterConfig.loadConfig();
            CjkUiSmoke.require(BigSignWriterConfig.MAIN_CONFIG.continuousWriting, "Setting was not persisted");
        });
        test.clickScreenButton("gui.done");
        CjkUiSmoke.openSign(test, world, pos, false);
        select(test, "CJK Pixel 12");
        test.getInput().typeChar(0x20000); // Absent in the real source; must not block the phrase.
        test.getInput().typeChars("你好");
        CjkUiSmoke.checkToolbar(test, false, 1);
        select(test, "Default");
        test.runOnClient(mc -> CjkUiSmoke.require(BigSignWriter.PENDING_TEXT.isEmpty(), "Font change kept stale pending text"));
        test.getInput().pressKey(GLFW.GLFW_KEY_BACKSPACE);
        test.runOnClient(mc -> CjkUiSmoke.require(Arrays.stream(CjkUiSmoke.context(mc.screen).messages).allMatch(String::isEmpty),
                "Backspace after font change was intercepted by the old queue"));
        test.getInput().typeChars("马ABCDEFGHIJKLMNO");
        test.runOnClient(mc -> CjkUiSmoke.require(!BigSignWriter.PENDING_TEXT.isEmpty(), "Enabled English font did not retain overflow"));
        setOption(test, false);
        test.runOnClient(mc -> {
            CjkUiSmoke.require(BigSignWriter.PENDING_TEXT.isEmpty(), "Disabling did not end the queue");
            CjkUiSmoke.require(!CjkUiSmoke.button(mc.screen, "bigsignwriter.input.continueQueue").visible, "Disabled toolbar visible");
        });
        test.clickScreenButton("gui.done");
        CjkUiSmoke.openSign(test, world, pos, false);
        select(test, "CJK Pixel 12");
        setOption(test, true);
        // Revert an unsaved choice through the option's existing Reset button.
        clickWidget(test, "☰");
        revealOption(test);
        test.runOnClient(mc -> {
            TickBox option = option(mc);
            clickAt(mc, option.getX() + 5, option.getY() + 10); // off
            clickAt(mc, option.getX() + 5, option.getY() + 10); // on
            clickAt(mc, option.getRight() + 12, option.getY() + 10); // reset to default off
            CjkUiSmoke.require(!option.value, "Reset did not restore default off");
            CjkUiSmoke.require(BigSignWriterConfig.MAIN_CONFIG.continuousWriting, "Working setting changed before save");
        });
        test.clickScreenButton("gui.done");
        setOption(test, true);
        test.clickScreenButton("gui.done");
    }

    static void verifyLanguages(ClientGameTestContext test, TestSingleplayerContext world, BlockPos pos) {
        for (String locale : new String[]{"en_us", "zh_cn", "zh_tw", "ja_jp", "ko_kr"}) {
            var reload = test.computeOnClient(mc -> {
                mc.getLanguageManager().setSelected(locale);
                mc.options.languageCode = locale;
                return mc.reloadResourcePacks();
            });
            // Let the gametest scheduler advance render-thread tasks during reload.
            test.waitFor(mc -> reload.isDone());
            reload.join();
            CjkUiSmoke.openSign(test, world, pos, false);
            select(test, "CJK Pixel 12");
            test.getInput().typeChars("你好");
            CjkUiSmoke.checkToolbar(test, false, 1);
            test.runOnClient(mc -> mc.gui.setOverlayMessage(Component.empty(), false));
            test.getInput().resizeWindow(1280, 854);
            test.takeScreenshot("cjk-locale-" + locale + "-sign");
            clickWidget(test, "☰");
            test.runOnClient(mc -> CjkUiSmoke.require(mc.screen instanceof BigSignWriterConfigScreen, "Settings screen missing"));
            hoverOption(test);
            test.takeScreenshot("cjk-locale-" + locale + "-settings");
            test.getInput().resizeWindow(640, 480);
            hoverOption(test);
            test.takeScreenshot("cjk-locale-" + locale + "-compact-settings");
            test.runOnClient(mc -> {
                AbstractWidget panel = widgets(mc.screen).filter(w -> w.getClass().getSimpleName().equals("OptionsSidePanel"))
                        .findFirst().orElseThrow();
                int needed = 25 + mc.font.lineHeight * mc.font.split(Component.translatable(OPTION + ".desc"), panel.getWidth()).size();
                CjkUiSmoke.require(panel.getHeight() >= needed, "Localized explanation has no scrollable space");
            });
            test.getInput().setCursorPos(480, 260);
            test.getInput().scroll(-20);
            test.waitTick();
            test.takeScreenshot("cjk-locale-" + locale + "-compact-settings-scrolled");
            test.getInput().resizeWindow(1280, 854);
            test.clickScreenButton("gui.done");
            CjkUiSmoke.click(test, "bigsignwriter.input.clearQueue");
            test.clickScreenButton("gui.done");
        }
        // Leave only the isolated fixture's saved preferences in their default state.
        CjkUiSmoke.openSign(test, world, pos, false);
        setOption(test, false);
        test.clickScreenButton("gui.done");
        test.runOnClient(mc -> {
            mc.getLanguageManager().setSelected("zh_cn");
            mc.options.languageCode = "zh_cn";
            mc.options.save();
        });
    }

    static void setOption(ClientGameTestContext test, boolean value) {
        clickWidget(test, "☰");
        revealOption(test);
        test.runOnClient(mc -> {
            TickBox option = option(mc);
            if (option.value != value) clickAt(mc, option.getX() + 5, option.getY() + 10);
            CjkUiSmoke.require(option.value == value, "Checkbox did not change");
        });
        test.clickScreenButton("gui.done");
        test.runOnClient(mc -> CjkUiSmoke.require(BigSignWriterConfig.MAIN_CONFIG.continuousWriting == value, "Setting was not applied"));
    }

    private static void hoverOption(ClientGameTestContext test) {
        revealOption(test);
        double[] point = test.computeOnClient(mc -> {
            TickBox option = option(mc);
            CjkUiSmoke.require(option.getY() >= 0 && option.getBottom() <= mc.screen.height, "Option outside viewport");
            return new double[]{(option.getX() + 5) * mc.getWindow().getGuiScale(), (option.getY() + 10) * mc.getWindow().getGuiScale()};
        });
        test.getInput().setCursorPos(point[0], point[1]);
        test.waitTick();
    }

    private static void revealOption(ClientGameTestContext test) {
        // Input options are grouped at the bottom, above character separators.
        double[] point = test.computeOnClient(mc -> new double[]{
                mc.screen.width * 0.25 * mc.getWindow().getGuiScale(),
                mc.screen.height * 0.45 * mc.getWindow().getGuiScale()});
        test.getInput().setCursorPos(point[0], point[1]);
        test.getInput().scroll(-100);
        test.waitTick();
    }

    private static TickBox option(Minecraft mc) {
        return widgets(mc.screen).filter(w -> w instanceof TickBox)
                .filter(w -> w.getMessage().getString().equals(Component.translatable(OPTION).getString()))
                .map(w -> (TickBox) w).findFirst().orElseThrow();
    }

    private static Stream<AbstractWidget> widgets(GuiEventListener listener) {
        Stream<AbstractWidget> own = listener instanceof AbstractWidget widget ? Stream.of(widget) : Stream.empty();
        return listener instanceof ContainerEventHandler container
                ? Stream.concat(own, container.children().stream().flatMap(CjkOptionsSmoke::widgets)) : own;
    }

    private static void clickWidget(ClientGameTestContext test, String text) {
        test.runOnClient(mc -> {
            AbstractWidget widget = widgets(mc.screen).filter(w -> w.getMessage().getString().equals(text)).findFirst().orElseThrow();
            clickAt(mc, widget.getX() + 5, widget.getY() + 5);
        });
        test.waitTick();
    }

    private static void clickAt(Minecraft mc, double x, double y) {
        CjkUiSmoke.require(mc.screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false), "Click was not handled");
        mc.screen.mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)));
    }

    private static void select(ClientGameTestContext test, String name) {
        test.runOnClient(mc -> {
            var selector = CjkUiSmoke.selector(mc.screen);
            String narration = Component.translatable("narrator.select", Component.literal(name)).getString();
            selector.setSelected(selector.children().stream().filter(e -> e.getNarration().getString().equals(narration)).findFirst().orElseThrow());
        });
    }

    private static String overlay(Minecraft mc) {
        try {
            var field = mc.gui.getClass().getDeclaredField("overlayMessageString");
            field.setAccessible(true);
            Component value = (Component) field.get(mc.gui);
            return value == null ? "" : value.getString();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}

package modules;

import com.kyozm.nanoutils.NanoUtils;
import com.kyozm.nanoutils.events.DrawSlotEvent;
import com.kyozm.nanoutils.events.TooltipRenderEvent;
import com.kyozm.nanoutils.gui.widgets.Draggable;
import com.kyozm.nanoutils.modules.Module;
import com.kyozm.nanoutils.modules.ModuleCategory;
import com.kyozm.nanoutils.modules.ModuleManager;
import com.kyozm.nanoutils.settings.NestedSetting;
import com.kyozm.nanoutils.settings.Setting;
import com.kyozm.nanoutils.utils.ChromaSync;
import com.kyozm.nanoutils.utils.Clipboard;
import com.kyozm.nanoutils.utils.FontDrawer;
import com.kyozm.nanoutils.utils.InputUtils;
import com.kyozm.nanoutils.utils.MapUtils;
import com.kyozm.nanoutils.utils.NanoColor;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Keyboard;

import java.awt.*;


public class MapPreview extends Module {

    public MapPreview() {
        name = "Map Preview";
        category = ModuleCategory.RENDER;
        bind = Keyboard.KEY_NONE;
        desc = "Renders map data on tooltip and screen.";

        MapPreviewWidget widget = new MapPreviewWidget( 100, 100, 65, 65 );
        saveablePositions.put("MapDisplay", widget);
        NanoUtils.gui.queue.add(widget);

        tooltipDisplay.register(tooltipBorder);
        tooltipDisplay.register(tooltipScale);
        tooltipDisplay.register(drawStackName);
        tooltipDisplay.register(copyTooltip);
        tooltipDisplay.register(clipboardScale);
        registerSetting(MapPreview.class, tooltipDisplay);

        itemStackDisplay.register(drawStackQuantity);
        itemStackDisplay.register(revealItemOnHover);
        itemStackDisplay.register(hideStackDisplay);
        registerSetting(MapPreview.class, itemStackDisplay);

        mapDisplay.register(mapDisplayBorderColor);
        mapDisplay.register(mapDisplayThirdPerson);
        mapDisplay.register(mapDisplayScale);
        mapDisplay.register(mapDisplayBorderSize);
        mapDisplay.register(mapDisplayAutoOffhand);
        mapDisplay.register(mapDisplaySetMap);
        registerSetting(MapPreview.class, mapDisplay);

        registerSetting(MapPreview.class, cache);

        ChromaSync.updateables.add(mapDisplayBorderColor);
        ChromaSync.updateables.add(tooltipBorder);
    }

    public static NestedSetting tooltipDisplay = new NestedSetting()
            .setName("Tooltip Display")
            .setConfigName("MapPreviewTooltipDisplay")
            .setDefaultVal(false)
            .withType(NestedSetting.class);

    public static NestedSetting itemStackDisplay = new NestedSetting()
            .setName("Item Stack Display")
            .setConfigName("MapPreviewItemStackDisplay")
            .setDefaultVal(false)
            .withType(NestedSetting.class);

    public static Setting<Boolean> cache = new Setting<Boolean>()
            .setName("Cache Maps")
            .setConfigName("MapPreviewCache")
            .setDefaultVal(false)
            .withType(Boolean.class);

    public static NestedSetting mapDisplay = new NestedSetting()
            .setName("Map Display")
            .setConfigName("MapPreviewDisplay")
            .setExtraWidth(10)
            .setDefaultVal(false)
            .withType(NestedSetting.class);

    public static Setting<Boolean> mapDisplayThirdPerson = new Setting<Boolean>()
            .setName("Third Person Only")
            .setConfigName("MapPreviewMapDisplayThirdPerson")
            .setDefaultVal(false)
            .withType(Boolean.class);

    public static Setting<Boolean> mapDisplayAutoOffhand = new Setting<Boolean>()
            .setName("AutoPull from OffHand")
            .setConfigName("MapPreviewMapDisplayAutoPullOffhand")
            .setDefaultVal(false)
            .withType(Boolean.class);

    public static Setting<NanoColor> mapDisplayBorderColor = new Setting<NanoColor>()
            .setName("Border")
            .setConfigName("MapPreviewMapDisplayBorderColor")
            .setDefaultVal(NanoColor.fromColor(Color.WHITE))
            .withType(NanoColor.class);

    public static Setting<Float> mapDisplayScale = new Setting<Float>()
            .setName("Scale")
            .setConfigName("MapPreviewMapDisplayScale")
            .setDefaultVal(1f)
            .setMinVal(0.1f)
            .setMaxVal(6f)
            .withStep(0.05f)
            .setExtraWidth(80)
            .withType(Float.class);

    public static Setting<Integer> mapDisplayBorderSize = new Setting<Integer>()
            .setName("Border Size")
            .setConfigName("MapPreviewMapDisplayBorderSize")
            .setDefaultVal(2)
            .setMinVal(0)
            .setMaxVal(10)
            .withStep(1)
            .withType(Integer.class);

    public static Setting<Boolean> drawStackQuantity = new Setting<Boolean>()
            .setName("Draw Stack Quantity")
            .setConfigName("MapPreviewStackQuantity")
            .setDefaultVal(false)
            .withType(Boolean.class);

    public static Setting<Boolean> revealItemOnHover = new Setting<Boolean>()
            .setName("Reveal on Hover")
            .setConfigName("MapPreviewStackHover")
            .setDefaultVal(false)
            .withType(Boolean.class);

    public static Setting<NanoColor> tooltipBorder = new Setting<NanoColor>()
            .setName("Border")
            .setConfigName("MapPreviewTooltipBorderColor")
            .setDefaultVal(NanoColor.fromColor(Color.WHITE))
            .withType(NanoColor.class);

    public static Setting<Boolean> drawStackName = new Setting<Boolean>()
            .setName("Draw Stack Name")
            .setConfigName("MapPreviewTooltipName")
            .setDefaultVal(true)
            .withType(Boolean.class);

    public static Setting<Float> tooltipScale = new Setting<Float>()
            .setName("Scale")
            .setConfigName("MapPreviewTooltipScale")
            .setDefaultVal(1f)
            .setMinVal(0.1f)
            .setMaxVal(6f)
            .withStep(0.05f)
            .setExtraWidth(80)
            .withType(Float.class);

    public static Setting<KeyBinding> hideStackDisplay = new Setting<KeyBinding>()
            .setName("Hide")
            .setConfigName("MapPreviewStackHide")
            .setDefaultVal(new KeyBinding("Hide Stack Display", Keyboard.KEY_NONE, "NanoUtils"))
            .setExtraWidth(50)
            .withType(KeyBinding.class);

    public static Setting<KeyBinding> copyTooltip = new Setting<KeyBinding>()
            .setName("Copy to Clipboard")
            .setConfigName("MapPreviewCopy")
            .setDefaultVal(new KeyBinding("Copy Map to Clip", Keyboard.KEY_NONE, "NanoUtils"))
            .setExtraWidth(50)
            .withType(KeyBinding.class);

    public static Setting<KeyBinding> mapDisplaySetMap = new Setting<KeyBinding>()
            .setName("Set Map")
            .setConfigName("MapPreviewMapDisplaySetMap")
            .setDefaultVal(new KeyBinding("Set Display Map", Keyboard.KEY_NONE, "NanoUtils"))
            .setExtraWidth(50)
            .withType(KeyBinding.class);

    public static Setting<Float> clipboardScale = new Setting<Float>()
            .setName("Clipboard Scale")
            .setConfigName("MapPreviewTooltipClipboard")
            .setDefaultVal(1f)
            .setMinVal(0.1f)
            .setMaxVal(6f)
            .withStep(0.05f)
            .setExtraWidth(60)
            .withType(Float.class);

    public static int tooltipX;
    public static int tooltipY;
    public static boolean activeTooltip = false;
    public static ItemStack tooltipStack;
    public static ItemStack displayStack;

    @EventHandler
    private Listener<GuiScreenEvent.DrawScreenEvent.Post> drawListener = new Listener<>(event -> {
        if (MapPreview.activeTooltip) {
            int x = MapPreview.tooltipX + 4;
            if (MapPreview.drawStackName.getVal())
                NanoUtils.gui.drawTooltip(String.format("x%s §o%s",  MapPreview.tooltipStack.getCount(), MapPreview.tooltipStack.getDisplayName()), x - 3, MapPreview.tooltipY - 2);
            int w = (int) (64 * MapPreview.tooltipScale.getVal());
            int h = (int) (64 * MapPreview.tooltipScale.getVal());
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            Gui.drawRect(x + 3, MapPreview.tooltipY + 3, x + 3 + w + 4, MapPreview.tooltipY + 5 + h + 2, MapPreview.tooltipBorder.getVal().getRGB());
            GlStateManager.disableDepth();
            GlStateManager.enableLighting();
            MapUtils.renderMapFromStack(MapPreview.tooltipStack, x + 5, MapPreview.tooltipY + 5, MapPreview.tooltipScale.getVal(), MapPreview.cache.getVal());
            MapPreview.activeTooltip = false;

            if (InputUtils.wasKeybindJustPressed(MapPreview.copyTooltip.getVal())) {
                MapData md = MapUtils.mapDataFromStack(MapPreview.tooltipStack);
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("[NanoUtils] Copied map data to Clipboard"));
                Clipboard.writeImageToClipboard(MapUtils.mapToImage(md.colors, MapPreview.clipboardScale.getVal()));
            }
        }
    });

    @EventHandler
    private Listener<TooltipRenderEvent> tooltipListener = new Listener<>(e -> {
        if (MapPreview.tooltipDisplay.getVal() && ModuleManager.isActive(MapPreview.class) && e.stack.getItem() instanceof ItemMap) {
            MapData mapData = MapUtils.mapDataFromStack(e.stack);
            if (mapData == null && MapUtils.tryMapCache("map_" + e.stack.getMetadata()) == null) return;
            e.cancel();
            MapPreview.activeTooltip = true;
            MapPreview.tooltipX = e.mouseX;
            MapPreview.tooltipY = e.mouseY;
            MapPreview.tooltipStack = e.stack;
        }
    });

    @EventHandler
    private Listener<DrawSlotEvent> slotEvent = new Listener<>(event -> {
        Slot slot = event.s;
        if (!slot.getHasStack())
            return;
        ItemStack itemStack = slot.getStack();
        if (MapPreview.itemStackDisplay.getVal() && itemStack.getItem() instanceof ItemMap) {
            int x = slot.xPos - 1;
            int y = slot.yPos - 1;

            if (MapPreview.tooltipStack != null && itemStack == MapPreview.tooltipStack && MapPreview.revealItemOnHover.getVal()) {
                MapPreview.tooltipStack = null;
                return;
            }

            if (InputUtils.isKeybindHeld(MapPreview.hideStackDisplay.getVal()))
                return;

            if (MapUtils.renderMapFromStack(itemStack, x, y, 0.29f, MapPreview.cache.getVal())) { // do not question the float magick
                event.cancel();
                if (MapPreview.drawStackQuantity.getVal()) {
                    if (itemStack.getCount() > 1) {
                        String count = String.valueOf(itemStack.getCount());
                        FontDrawer.drawString(count, x + 19 - FontDrawer.getStringWidth(count), y + 22 - FontDrawer.getFontHeight(), Color.DARK_GRAY);
                        FontDrawer.drawString(count, x + 18 - FontDrawer.getStringWidth(count), y + 21 - FontDrawer.getFontHeight(), Color.WHITE);
                    }
                }
            }
        }
    });

    @Override
    public void onEnable() {
        NanoUtils.EVENT_BUS.subscribe(this);
    }

    @Override
    public void onDisable() {
        NanoUtils.EVENT_BUS.unsubscribe(this);
    }

    class MapPreviewWidget extends Draggable {

        public MapPreviewWidget(int x, int y, int xx, int yy) {
            super(x, y, xx, yy);
            clearable = false;
        }

        @Override
        public void render() {
            super.render();
            ItemStack selectedMap = mc.player.inventory.offHandInventory.get(0);

            if (InputUtils.wasKeybindJustPressed(MapPreview.mapDisplaySetMap.getVal())) {
                MapPreview.displayStack = mc.player.inventory.getCurrentItem().getItem() instanceof ItemMap ? mc.player.inventory.getCurrentItem() : null;
            }

            if (!(MapPreview.mapDisplayAutoOffhand.getVal() && selectedMap.getItem() instanceof ItemMap)) {
                selectedMap = MapPreview.displayStack;
                if (selectedMap == null)
                    return;
            }

            if (!ModuleManager.isActive(MapPreview.class) || !MapPreview.mapDisplay.getVal()) {
                canDrag = false;
                return;
            } else {
                canDrag = true;
            }

            if (MapPreview.mapDisplay.getVal() && selectedMap.getItem() instanceof ItemMap) {
                int borderWidth = MapPreview.mapDisplayBorderSize.getVal();
                if (mc.gameSettings.thirdPersonView == 0 && MapPreview.mapDisplayThirdPerson.getVal())
                    return;
                int borderColor = MapPreview.mapDisplayBorderColor.getVal().getRGB();
                this.width = (int) (64 * MapPreview.mapDisplayScale.getVal());
                this.height = (int) (64 * MapPreview.mapDisplayScale.getVal());
                Gui.drawRect(
                        screenX - borderWidth,
                        screenY - borderWidth,
                        (int) (screenX + (64 * MapPreview.mapDisplayScale.getVal()) + borderWidth),
                        (int) (screenY + (64 * MapPreview.mapDisplayScale.getVal())) + borderWidth, borderColor);
                MapUtils.renderMapFromStack(selectedMap, screenX, screenY, MapPreview.mapDisplayScale.getVal(), MapPreview.cache.getVal());
                canDrag = true;
            }
        }
    }
}

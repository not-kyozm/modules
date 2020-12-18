package modules;

import com.kyozm.nanoutils.NanoUtils;
import com.kyozm.nanoutils.events.TooltipRenderEvent;
import com.kyozm.nanoutils.modules.Module;
import com.kyozm.nanoutils.modules.ModuleCategory;
import com.kyozm.nanoutils.modules.ModuleManager;
import com.kyozm.nanoutils.modules.gui.Theme;
import com.kyozm.nanoutils.settings.Setting;
import com.kyozm.nanoutils.utils.FontDrawer;
import com.kyozm.nanoutils.utils.NBTUtils;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Method;

import static org.lwjgl.opengl.GL11.glDepthRange;

public class ShulkerPreview extends Module {

    public ShulkerPreview() {
        name = "Shulker Preview";
        category = ModuleCategory.RENDER;
        bind = Keyboard.KEY_NONE;
        desc = "Shows the contents of Shulker boxes.";

        registerSetting(ShulkerPreview.class, darkMode);
    }

    public static Setting<Boolean> darkMode = new Setting<Boolean>()
            .setName("Dark Mode")
            .setConfigName("ShulkPreviewDarkMode")
            .setDefaultVal(true)
            .withType(Boolean.class);

    public static boolean visible;
    public static int previewX;
    public static int previewY;
    public static ItemStack shulkerStack;

    @EventHandler
    private Listener<TooltipRenderEvent> tooltipListener = new Listener<TooltipRenderEvent>(e -> {
        NonNullList<ItemStack> items = NBTUtils.getShulkerItems(e.stack);
        if (items == null || !ModuleManager.isActive(ShulkerPreview.class)) return;

        previewX = e.mouseX;
        previewY = e.mouseY;
        visible = true;
        shulkerStack = e.stack;
        e.cancel();
    });

    @EventHandler
    private Listener<GuiScreenEvent.DrawScreenEvent.Post> drawListener = new Listener<>(event -> {
        if (!visible) return;
        NonNullList<ItemStack> items = NBTUtils.getShulkerItems(shulkerStack);

        int x = previewX + 8;
        int y = previewY - 8;

        IInventory dummyInv = new InventoryBasic(new TextComponentString("Cummy"), 1);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        Gui.drawRect(x - 2, y - 12, x + 162 + 3, y + 54 + 3, Theme.shulkPreviewBG.getVal().getRGB());
        FontDrawer.drawString(shulkerStack.getDisplayName(), x, y - 8, Theme.shulkPreviewFG.getVal());
        if (darkMode.getVal()) GlStateManager.enableLighting();
        mc.renderEngine.bindTexture(new ResourceLocation("textures/gui/container/shulker_box.png"));
        GlStateManager.color(1, 1, 1, 1);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 7, 17, 162, 54, 256, 256);
        RenderHelper.enableGUIStandardItemLighting();


        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                try {
                    if (mc.currentScreen != null && GuiContainer.class.isAssignableFrom(mc.currentScreen.getClass())) {
                        Slot slotTest = new Slot(dummyInv, 0, (x + 1) + j * 18, (y + 1) + i * 18);
                        slotTest.putStack(items.get(i * 9 + j));
                        glDepthRange(0, 0.01);

                        Method m = ReflectionHelper.findMethod(GuiContainer.class, "drawSlot", "func_146977_a", Slot.class);
                        m.setAccessible(true);
                        m.invoke(mc.currentScreen, slotTest);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        glDepthRange(0, 1.0);
        GlStateManager.enableDepth();
        visible = false;
    });

    @Override
    public void onEnable() {
        NanoUtils.EVENT_BUS.subscribe(this);
    }

    @Override
    public void onDisable() {
        NanoUtils.EVENT_BUS.unsubscribe(this);
    }
}

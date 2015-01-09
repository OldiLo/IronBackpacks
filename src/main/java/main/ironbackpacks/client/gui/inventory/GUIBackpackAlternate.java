package main.ironbackpacks.client.gui.inventory;

import main.ironbackpacks.ModInformation;
import main.ironbackpacks.client.gui.buttons.RenameButton;
import main.ironbackpacks.container.alternateGui.ContainerAlternateGui;
import main.ironbackpacks.container.alternateGui.InventoryAlternateGui;
import main.ironbackpacks.items.backpacks.IronBackpackType;
import main.ironbackpacks.items.upgrades.UpgradeMethods;
import main.ironbackpacks.items.upgrades.UpgradeTypes;
import main.ironbackpacks.network.ButtonUpgradeMessage;
import main.ironbackpacks.network.NetworkingHandler;
import main.ironbackpacks.network.RenameMessage;
import main.ironbackpacks.util.IronBackpacksHelper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class GUIBackpackAlternate extends GuiContainer { //extend GuiScreen?

    public enum ResourceList { //TODO - move to constants?

        NONE(new ResourceLocation(ModInformation.ID, "textures/guis/BASIC_alternateGui.png")), //yes, it is supposed to be the same
        BASIC(new ResourceLocation(ModInformation.ID, "textures/guis/BASIC_alternateGui.png")),
        ONE(new ResourceLocation(ModInformation.ID, "textures/guis/ONE_alternateGui.png")),
        TWO(new ResourceLocation(ModInformation.ID, "textures/guis/TWO_alternateGui.png")),
        THREE(new ResourceLocation(ModInformation.ID, "textures/guis/THREE_alternateGui.png")),
        FOUR(new ResourceLocation(ModInformation.ID, "textures/guis/FOUR_alternateGui.png"));

        public final ResourceLocation location;

        private ResourceList(ResourceLocation loc) {
            this.location = loc;
        }

    }

    public enum GUI {

        NONE( 200, 137, ResourceList.NONE),
        BASIC(200, 137, ResourceList.BASIC),
        ONE(  200, 168, ResourceList.ONE),
        TWO(  200, 200, ResourceList.TWO),
        THREE(200, 200, ResourceList.THREE),
        FOUR( 200, 200, ResourceList.FOUR);

        private int xSize;
        private int ySize;
        private ResourceList guiResourceList;

        private GUI(int xSize, int ySize, ResourceList guiResourceList) {
            this.xSize = xSize;
            this.ySize = ySize;
            this.guiResourceList = guiResourceList;
        }

        public Container makeContainer(EntityPlayer player, InventoryAlternateGui inv) {
            return new ContainerAlternateGui(player, inv, xSize, ySize);
        }

        public static GUIBackpackAlternate buildGUIAlternate(EntityPlayer player, InventoryAlternateGui inv, int[] upgrades, IronBackpackType backpackType) {
            return new GUIBackpackAlternate(values()[UpgradeMethods.getAlternateGuiUpgradesCount(upgrades)], player, inv, upgrades, backpackType);
        }
    }

    private GUI type;
    private ContainerAlternateGui container;
    private IronBackpackType backpackType;
    private EntityPlayer player;

    private GuiTextField textField;
    private RenameButton renameButton;

    private boolean hasNoUpgrades;
    private boolean hasRenamingUpgrade;
    private boolean hasFilterUpgrade;
    private boolean hasHopperUpgrade;
    private boolean hasCondenserUpgrade;

    private GUIBackpackAlternate(GUI type, EntityPlayer player, InventoryAlternateGui inv, int[] upgrades, IronBackpackType backpackType) {
        super(type.makeContainer(player, inv));
        this.player = player;
        this.container = (ContainerAlternateGui) type.makeContainer(player, inv);
        this.type = type;
        this.backpackType = backpackType;

        this.xSize = type.xSize;
        this.ySize = type.ySize;

        this.hasNoUpgrades = type.equals(GUI.NONE);
        this.hasRenamingUpgrade = UpgradeMethods.hasRenamingUpgrade(upgrades);
        this.hasFilterUpgrade = UpgradeMethods.hasFilterUpgrade(upgrades);
        this.hasHopperUpgrade = UpgradeMethods.hasHopperUpgrade(upgrades);
        this.hasCondenserUpgrade = UpgradeMethods.hasCondenserUpgrade(upgrades);
    }

    @Override
    public void initGui(){
        super.initGui();

        int xStart = ((width - xSize) / 2);
        int yStart = ((height - ySize) / 2);

        if (this.hasRenamingUpgrade){
            this.allowUserInput = true;

            this.textField = new GuiTextField(this.fontRendererObj, xStart + 20, yStart + 23, 103, 12);  //x,y,width,height

            this.textField.setTextColor(-1); //TODO - play around with colors? - set background color
            this.textField.setDisabledTextColour(-1);
            this.textField.setMaxStringLength(30);

            Keyboard.enableRepeatEvents(true);

            this.buttonList.add(this.renameButton = new RenameButton(1, xStart + xSize - 57, yStart + 24, 25, 10, RenameButton.RENAME_BUTTON));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        this.mc.getTextureManager().bindTexture(type.guiResourceList.location);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect(x+12, y, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        ItemStack itemStack = IronBackpacksHelper.getBackpack(this.player);
        this.fontRendererObj.drawString(StatCollector.translateToLocal(itemStack.getDisplayName()), 20, 6, 4210752);
        this.fontRendererObj.drawString(StatCollector.translateToLocal("player.inventory"), 20, this.ySize - 96 + 2, 4210752);

        if (this.hasNoUpgrades){
            this.fontRendererObj.drawString(StatCollector.translateToLocal("noValidUpgradesFound"), 20, 23, 4210752);
        }
        int yStart = this.hasRenamingUpgrade ? 45 : 15;
        if (this.hasHopperUpgrade){
            this.fontRendererObj.drawString(StatCollector.translateToLocal("item.ironbackpacks:hopperUpgrade.name"),20, yStart, 4210752);
            yStart += 20;
        }
        if (this.hasFilterUpgrade){
            this.fontRendererObj.drawString(StatCollector.translateToLocal("item.ironbackpacks:filterUpgrade.name"),20, yStart, 4210752);
            yStart += 20;
        }
        if (this.hasCondenserUpgrade){
            this.fontRendererObj.drawString(StatCollector.translateToLocal("item.ironbackpacks:condenserUpgrade.name"),20, yStart, 4210752);
//            yStart += 20; //last one so unnecessary
        }
    }

    @Override
    protected void keyTyped(char char1, int int1)
    {
        if (this.hasRenamingUpgrade) {
            if (this.textField.textboxKeyTyped(char1, int1)){
                //
            }else {
                super.keyTyped(char1, int1);
            }
        }else{
            super.keyTyped(char1, int1);
        }
    }

//    private void updateItemNaming()
//    {
//        String s = this.textField.getText();
//        Slot slot = this.container.getSlot(0);
//
//        if (slot != null && slot.getHasStack() && !slot.getStack().hasDisplayName() && s.equals(slot.getStack().getDisplayName()))
//        {
//            s = "";
//        }
//
//        this.container.updateItemName(s); //get player's held item + rename that
//        this.mc.thePlayer.sendQueue.addToSendQueue(new C17PacketCustomPayload("MC|ItemName", s.getBytes(Charsets.UTF_8)));
//    }

    @Override
    protected void mouseClicked(int int1, int int2, int int3)
    {
        super.mouseClicked(int1, int2, int3);
        if (this.hasRenamingUpgrade) {
            this.textField.mouseClicked(int1, int2, int3);
        }
    }

    @Override
    public void drawScreen(int int1, int int2, float float1)
    {
        super.drawScreen(int1, int2, float1);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        if (this.hasRenamingUpgrade) {
            this.textField.drawTextBox();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == renameButton) {
            this.container.renameBackpack(this.textField.getText());
            NetworkingHandler.network.sendToServer(new RenameMessage(this.textField.getText()));
        }
    }

}

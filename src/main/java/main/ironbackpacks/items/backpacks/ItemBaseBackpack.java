package main.ironbackpacks.items.backpacks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import main.ironbackpacks.IronBackpacks;
import main.ironbackpacks.container.backpack.ContainerBackpack;
import main.ironbackpacks.container.backpack.InventoryBackpack;
import main.ironbackpacks.items.ItemBase;
import main.ironbackpacks.items.upgrades.UpgradeMethods;
import main.ironbackpacks.items.upgrades.UpgradeTypes;
import main.ironbackpacks.util.ConfigHandler;
import main.ironbackpacks.util.IronBackpacksConstants;
import main.ironbackpacks.util.NBTHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.List;
import java.util.UUID;

public class ItemBaseBackpack extends ItemBase {

    private int guiId;
    private int typeID;
    private int upgradeSlots;
    private int upgradeIndex;

    public ItemBaseBackpack(String unlocName, String textureName, int id, int upgradeSlots, int typeID) {
        super(unlocName, textureName);
        setMaxStackSize(1);
        this.guiId = id;
        this.typeID = typeID;
        this.upgradeSlots = upgradeSlots;
        this.upgradeIndex = upgradeSlots-1;
    }

    public int getTypeId() { return typeID;}

    public int getUpgradeSlots(){
        return upgradeSlots;
    }

    public int getUpgradeIndex(){
        return upgradeIndex;
    }

//    @Override
//    public int getMaxDamage(){
//        return IronBackpackType.values()[this.typeID].getSize();
//    }

    @Override
    public boolean showDurabilityBar(ItemStack stack)
    {
        int[] upgrades = getUpgradesFromNBT(stack);
        return UpgradeMethods.hasDamageBarUpgrade(upgrades);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return getFullness(stack);
    }

    public double getFullness(ItemStack stack){
        ItemStack[] inventory;
        int total = 0;
        int full = 0;
        if (stack != null) {
            NBTTagCompound nbtTagCompound = stack.getTagCompound();

            if (nbtTagCompound != null){
                if (nbtTagCompound.hasKey("Items")) {
                    NBTTagList tagList = nbtTagCompound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
                    inventory = new ItemStack[IronBackpackType.values()[this.guiId].getSize()];

                    for (int i = 0; i < tagList.tagCount(); i++) {
                        NBTTagCompound stackTag = tagList.getCompoundTagAt(i);
                        int j = stackTag.getByte("Slot");
                        if (i >= 0 && i <= inventory.length) {
                            inventory[j] = ItemStack.loadItemStackFromNBT(stackTag);
                        }
                    }

                    for (ItemStack tempStack: inventory) {
                        if (tempStack != null) {
                            full += tempStack.stackSize;
                            total += tempStack.getMaxStackSize();
                        } else {
                            total += 64;
                        }
                    }
                }
            }
        }
        return 1 - ((double) full / total);
    }


    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (world.isRemote){
            return itemStack;
        }else {
            NBTHelper.setUUID(itemStack);
            if (!player.isSneaking()) {
                player.openGui(IronBackpacks.instance, guiId, world, (int) player.posX, (int) player.posY, (int) player.posZ);
                return itemStack;
            }else{
                player.openGui(IronBackpacks.instance, (guiId * -1) - 1, world, (int) player.posX, (int) player.posY, (int) player.posZ);
                return itemStack;
            }
        }
    }

    public int[] getUpgradesFromNBT(EntityPlayer player, ItemStack parent) {
        int[] upgrades = new int[3]; //default [0,0,0]
        ItemStack stack = findParentItemStack(player, parent);
        if (stack != null) {
            NBTTagCompound nbtTagCompound = stack.getTagCompound();
            if (nbtTagCompound != null) {
                if(nbtTagCompound.hasKey("Upgrades")) {
                    NBTTagList tagList = nbtTagCompound.getTagList("Upgrades", Constants.NBT.TAG_COMPOUND);
                    for (int i = 0; i < tagList.tagCount(); i++) {
                        NBTTagCompound stackTag = tagList.getCompoundTagAt(i);
                        int hasUpgrade = stackTag.getByte("Upgrade");
                        if (hasUpgrade == 1){ //true
                            upgrades[i] = hasUpgrade;
                        }
                    }
                }
            }
        }
        return upgrades;
    }

    public int[] getUpgradesFromNBT(ItemStack stack) { //TODO - Unckecked
        int[] upgrades = new int[3]; //default [0,0,0]
        if (stack != null) {
            NBTTagCompound nbtTagCompound = stack.getTagCompound();
            if (nbtTagCompound != null) {
                if(nbtTagCompound.hasKey("Upgrades")) {
                    NBTTagList tagList = nbtTagCompound.getTagList("Upgrades", Constants.NBT.TAG_COMPOUND); //TODO - should be TAG_LIST?
                    for (int i = 0; i < tagList.tagCount(); i++) {
                        NBTTagCompound stackTag = tagList.getCompoundTagAt(i);
                        int hasUpgrade = stackTag.getByte("Upgrade");
                        if (hasUpgrade != 0){ //true
                            upgrades[i] = hasUpgrade;
                        }
                    }
                }
            }
        }
        return upgrades;
    }

    public ItemStack findParentItemStack(EntityPlayer entityPlayer, ItemStack stack){
        if (NBTHelper.hasUUID(stack)){
            UUID parentUUID = new UUID(stack.getTagCompound().getLong(IronBackpacksConstants.Miscellaneous.MOST_SIG_UUID), stack.getTagCompound().getLong(IronBackpacksConstants.Miscellaneous.LEAST_SIG_UUID));
            for (int i = 0; i < entityPlayer.inventory.getSizeInventory(); i++){
                ItemStack itemStack = entityPlayer.inventory.getStackInSlot(i);

                if (itemStack != null && itemStack.getItem() instanceof ItemBaseBackpack && NBTHelper.hasUUID(itemStack)){
                    if (itemStack.getTagCompound().getLong(IronBackpacksConstants.Miscellaneous.MOST_SIG_UUID) == parentUUID.getMostSignificantBits() && itemStack.getTagCompound().getLong(IronBackpacksConstants.Miscellaneous.LEAST_SIG_UUID) == parentUUID.getLeastSignificantBits()){
                        return itemStack;
                    }
                }
            }
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemStack, EntityPlayer player, List list, boolean par4) {
        int[] upgrades = this.getUpgradesFromNBT(itemStack);
        for (int i = 0; i < this.upgradeSlots; i++){
            list.add(UpgradeTypes.values()[upgrades[i]].getFancyName());
        }
    }

}

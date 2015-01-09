package main.ironbackpacks.util;

import cpw.mods.fml.common.network.IGuiHandler;
import main.ironbackpacks.client.gui.inventory.GUIBackpack;
import main.ironbackpacks.client.gui.inventory.GUIBackpackAlternate;
import main.ironbackpacks.container.alternateGui.ContainerAlternateGui;
import main.ironbackpacks.container.alternateGui.InventoryAlternateGui;
import main.ironbackpacks.container.backpack.ContainerBackpack;
import main.ironbackpacks.items.backpacks.IronBackpackType;
import main.ironbackpacks.container.backpack.InventoryBackpack;
import main.ironbackpacks.items.backpacks.ItemBaseBackpack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class GuiHandler implements IGuiHandler {

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID >=0){
            IronBackpackType type = IronBackpackType.values()[ID];
            return new ContainerBackpack(player, new InventoryBackpack(player, IronBackpacksHelper.getBackpack(player), type), type);
        }else if (ID < 0){
            IronBackpackType type = IronBackpackType.values()[Math.abs(ID + 1)];
            return new ContainerAlternateGui(player, new InventoryAlternateGui(player, IronBackpacksHelper.getBackpack(player), type));//, type);
        }
        return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID >= 0) {
            ItemStack backpack = IronBackpacksHelper.getBackpack(player);
            int[] upgrades = ((ItemBaseBackpack)backpack.getItem()).getUpgradesFromNBT(backpack);
            return GUIBackpack.GUI.buildGUI(player, new InventoryBackpack(player, IronBackpacksHelper.getBackpack(player), IronBackpackType.values()[ID]), upgrades);
        }else if (ID < 0){
            ItemStack backpack = IronBackpacksHelper.getBackpack(player);
            int[] upgrades = ((ItemBaseBackpack)backpack.getItem()).getUpgradesFromNBT(backpack);
            return GUIBackpackAlternate.GUI.buildGUIAlternate(player, new InventoryAlternateGui(player, IronBackpacksHelper.getBackpack(player), IronBackpackType.values()[Math.abs((ID + 1))]), upgrades, IronBackpackType.values()[Math.abs((ID + 1))]);
        }
		return null;
	}
}

package gr8pefish.ironbackpacks.events;


import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;

import gr8pefish.ironbackpacks.api.items.backpacks.interfaces.IBackpack;
import gr8pefish.ironbackpacks.capabilities.player.PlayerWearingBackpackCapabilities;
import gr8pefish.ironbackpacks.config.ConfigHandler;
import gr8pefish.ironbackpacks.container.backpack.ContainerBackpack;
import gr8pefish.ironbackpacks.container.backpack.InventoryBackpack;
import gr8pefish.ironbackpacks.items.backpacks.ItemBackpack;
import gr8pefish.ironbackpacks.items.upgrades.UpgradeMethods;
import gr8pefish.ironbackpacks.util.Logger;
import gr8pefish.ironbackpacks.util.helpers.IronBackpacksHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.oredict.OreDictionary;

public class IronBackpacksEventHelper {

    //============================================================================Helper Methods===============================================================================

    /**
     * Gets all the backpacks that have filter, condenser, or restocker upgrades in them for the EntityItemPickupEvent event.
     * @param player - the player to check
     * @return - a nested array list of the array lists of each type of backpack that has each filter type
     */
    protected static NonNullList<NonNullList<ItemStack>> getFilterCrafterAndRestockerBackpacks(EntityPlayer player){
    	NonNullList<ItemStack> filterBackpacks = NonNullList.create();
        NonNullList<ItemStack> crafterTinyBackpacks = NonNullList.create();;
        NonNullList<ItemStack> crafterSmallBackpacks = NonNullList.create();
        NonNullList<ItemStack> crafterBackpacks = NonNullList.create();
        NonNullList<ItemStack> restockerBackpacks = NonNullList.create();
        NonNullList<NonNullList<ItemStack>> returnArray = NonNullList.create();

        //get the equipped pack
        getEventBackpacks(PlayerWearingBackpackCapabilities.getEquippedBackpack(player), filterBackpacks, crafterTinyBackpacks, crafterSmallBackpacks, crafterBackpacks, restockerBackpacks, player);


        //get the packs in the inventory
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            getEventBackpacks(stack, filterBackpacks, crafterTinyBackpacks, crafterSmallBackpacks, crafterBackpacks, restockerBackpacks, player);
        }

        returnArray.add(filterBackpacks);
        returnArray.add(crafterTinyBackpacks);
        returnArray.add(crafterSmallBackpacks);
        returnArray.add(crafterBackpacks);
        returnArray.add(restockerBackpacks);
        return returnArray;
    }

    protected static void getEventBackpacks(ItemStack backpack, NonNullList<ItemStack> filterBackpacks, NonNullList<ItemStack> crafterTinyBackpacks, NonNullList<ItemStack> crafterSmallBackpacks, NonNullList<ItemStack> crafterBackpacks, NonNullList<ItemStack> restockerBackpacks, EntityPlayer player){
        if (!backpack.isEmpty() && backpack.getItem() instanceof IBackpack) {

            NonNullList<ItemStack> upgrades = IronBackpacksHelper.getUpgradesAppliedFromNBT(backpack);
            addToLists(backpack, filterBackpacks, crafterTinyBackpacks, crafterSmallBackpacks, crafterBackpacks, restockerBackpacks, upgrades);

            if (UpgradeMethods.hasDepthUpgrade(upgrades)) {
                ContainerBackpack container = new ContainerBackpack(new InventoryBackpack(player, backpack));
                for (int j = 0; j < container.getInventoryBackpack().getSizeInventory(); j++) {
                    ItemStack nestedBackpack = container.getInventoryBackpack().getStackInSlot(j);
                    if (!nestedBackpack.isEmpty() && nestedBackpack.getItem() instanceof IBackpack) {
                        addToLists(nestedBackpack, filterBackpacks, crafterTinyBackpacks, crafterSmallBackpacks, crafterBackpacks, restockerBackpacks, IronBackpacksHelper.getUpgradesAppliedFromNBT(nestedBackpack));
                    }
                }
            }
        }
    }

    protected static void addToLists(ItemStack stack, NonNullList<ItemStack> filterBackpacks, NonNullList<ItemStack> crafterTinyBackpacks, NonNullList<ItemStack> crafterSmallBackpacks, NonNullList<ItemStack> crafterBackpacks, NonNullList<ItemStack> restockerBackpacks, NonNullList<ItemStack> upgrades){
        if (UpgradeMethods.hasFilterBasicUpgrade(upgrades) || UpgradeMethods.hasFilterModSpecificUpgrade(upgrades) ||
                UpgradeMethods.hasFilterFuzzyUpgrade(upgrades) || UpgradeMethods.hasFilterOreDictUpgrade(upgrades) ||
                UpgradeMethods.hasFilterVoidUpgrade(upgrades) || UpgradeMethods.hasFilterAdvancedUpgrade(upgrades) ||
                UpgradeMethods.hasFilterMiningUpgrade(upgrades)) {
            filterBackpacks.add(stack);
        }
        if (UpgradeMethods.hasCraftingTinyUpgrade(upgrades)) {
            crafterTinyBackpacks.add(stack);
        }
        if (UpgradeMethods.hasCraftingSmallUpgrade(upgrades)) {
            crafterSmallBackpacks.add(stack);
        }
        if (UpgradeMethods.hasCraftingUpgrade(upgrades)) {
            crafterBackpacks.add(stack);
        }
        if (UpgradeMethods.hasRestockingUpgrade(upgrades)) {
            restockerBackpacks.add(stack);
        }
    }


    //TODO: cleanup the following two methods

    /**
     * Checks the hopper/restocking upgrade to try and refill items.
     * @param event - EntityItemPickupEvent
     * @param backpackStacks - the backpacks with this upgrade
     * @return - boolean successful
     */
    protected static boolean checkRestockingUpgradeItemPickup(EntityItemPickupEvent event, NonNullList<ItemStack> backpackStacks){
        boolean doFilter = true;
        boolean shouldSave;
        if (!backpackStacks.isEmpty()){
            for (ItemStack backpack : backpackStacks) {
                shouldSave = false;

                ItemBackpack itemBackpack = ((ItemBackpack)backpack.getItem()); //TODO: hardcoded
                ContainerBackpack container = new ContainerBackpack(new InventoryBackpack(event.getEntityPlayer(), backpack)); //TODO: remove additional itemstack parameter
                if (!(event.getEntityPlayer().openContainer instanceof ContainerBackpack)) { //can't have the backpack open

                    NonNullList<ItemStack> restockerItems = UpgradeMethods.getRestockingItems(backpack);
                    for (ItemStack restockerItem : restockerItems) {
                        if (!restockerItem.isEmpty()) {

                            boolean foundSlot = false;
                            ItemStack stackToResupply = ItemStack.EMPTY;
                            Slot slotToResupply = null;

                            for (int i = itemBackpack.getSize(backpack); i < itemBackpack.getSize(backpack) + 36; i++){ //check player's inv for items
                                Slot tempSlot = container.getSlot(i);
                                if (tempSlot != null && tempSlot.getHasStack()){
                                    ItemStack tempItem = tempSlot.getStack();
                                    if (IronBackpacksHelper.areItemsEqualAndStackable(tempItem, restockerItem)){ //found and less than max stack size
                                        foundSlot = true;
                                        slotToResupply = tempSlot;
                                        stackToResupply = tempItem;
                                        break;
                                    }
                                }
                            }

                            if (foundSlot){ //try to resupply with the itemEntity first
                                boolean done = false;
                                if (IronBackpacksHelper.areItemsEqualForStacking(event.getItem().getItem(), stackToResupply)){
                                    int amountToResupply = stackToResupply.getMaxStackSize() - stackToResupply.getCount();
                                    if (event.getItem().getItem().getCount() >= amountToResupply) { //if larger size of stack on the ground than needed to resupply

                                        //TODO: not updating event entity correctly, make this work

//                                        System.out.println("setting to "+(event.getItem()s.getItem().stackSize - amountToResupply));
//
//                                        event.getItem()s.setEntityItemStack(new ItemStack(event.getItem()s.getItem().getItem(), event.getItem()s.getItem().stackSize - amountToResupply, event.getItem()s.getItem().getItemDamage()));
//
//                                        event.getEntityPlayer().inventory.setInventorySlotContents(slotToResupply.getSlotIndex(), new ItemStack(stackToResupply.getItem(), stackToResupply.getMaxStackSize(), stackToResupply.getItemDamage()));
                                        done = true;

                                        shouldSave = true;
                                    } else { //just resupply what you can, it will automatically go into the player's slot needed

                                        doFilter = false;
                                        done = false;
                                    }
                                }
                                if (!done) { //then resupply from the backpack (if necessary)
                                    for (int i = 0; i < itemBackpack.getSize(backpack); i++) {
                                        Slot tempSlot = container.getSlot(i);
                                        if (tempSlot != null && tempSlot.getHasStack()) {
                                            ItemStack tempItem = tempSlot.getStack();
                                            if (IronBackpacksHelper.areItemsEqualForStacking(tempItem, stackToResupply)) {
                                                int amountToResupply;
                                                if (IronBackpacksHelper.areItemsEqualForStacking(event.getItem().getItem(), stackToResupply)) { //if resupplied already from the items picked up

                                                    ItemStack stackUpdated = event.getEntityPlayer().inventory.getStackInSlot(slotToResupply.getSlotIndex());
                                                    amountToResupply = stackToResupply.getMaxStackSize() - stackUpdated.getCount() - event.getItem().getItem().getCount();

                                                    if (tempItem.getCount() >= amountToResupply) {
                                                        tempSlot.decrStackSize(amountToResupply);
                                                        event.getEntityPlayer().inventory.setInventorySlotContents(slotToResupply.getSlotIndex(), new ItemStack(stackToResupply.getItem(), stackToResupply.getMaxStackSize() - event.getItem().getItem().getCount(), stackToResupply.getItemDamage()));
                                                        container.onContainerClosed(event.getEntityPlayer());
                                                        break;
                                                    } else {
                                                        tempSlot.decrStackSize(tempItem.getCount());
                                                        event.getEntityPlayer().inventory.setInventorySlotContents(slotToResupply.getSlotIndex(), new ItemStack(stackToResupply.getItem(), stackUpdated.getCount() + tempItem.getCount(), stackToResupply.getItemDamage()));
                                                    }
                                                } else { //normal resupply, no items picked up contribution

                                                    ItemStack stackUpdated = event.getEntityPlayer().inventory.getStackInSlot(slotToResupply.getSlotIndex());
                                                    amountToResupply = stackToResupply.getMaxStackSize() - stackUpdated.getCount();

                                                    if (tempItem.getCount() >= amountToResupply) {
                                                        tempSlot.decrStackSize(amountToResupply);
                                                        slotToResupply.putStack(new ItemStack(stackToResupply.getItem(), stackToResupply.getMaxStackSize(), stackToResupply.getItemDamage()));
                                                        container.onContainerClosed(event.getEntityPlayer());
                                                        break;
                                                    } else {
                                                        tempSlot.decrStackSize(tempItem.getCount());
                                                        event.getEntityPlayer().inventory.setInventorySlotContents(slotToResupply.getSlotIndex(), new ItemStack(stackToResupply.getItem(), stackUpdated.getCount() + tempItem.getCount(), stackToResupply.getItemDamage()));
                                                    }
                                                }
                                                shouldSave = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (shouldSave) {
                    container.onContainerClosed(event.getEntityPlayer());
                }
            }
        }
        return doFilter;
    }

    /**
     * Checks the hopper/restocking upgrade to try and refill items. Decrements from the backpack's stacks and updates the appropriate slot/stack in the player's inventory.
     * for each backpack
     *  if backpack has itemUsed in filter
     *      if backpack has itemUsed in inv
     *          resupply itemUsed
     *              get rid of backpackStack
     *              return new size of itemUsed stack
     * @param backpackStacks - the backpacks with this upgrade
     */
    protected static ItemStack checkRestockerUpgradeItemUse(EntityPlayer player, ItemStack stack, NonNullList<ItemStack> backpackStacks){
        if (!backpackStacks.isEmpty()){
            for (ItemStack backpack : backpackStacks) {
//                BackpackTypes type = BackpackTypes.values()[((ItemBackpackSubItems) backpack.getItem()).getGuiId()];
                ItemBackpack itemBackpack = (ItemBackpack)backpack.getItem(); //TODO: hardcoded
                ContainerBackpack container = new ContainerBackpack(new InventoryBackpack(player, backpack));
                if (!(player.openContainer instanceof ContainerBackpack)) { //can't have the backpack open
                    container.sort(); //TODO: test with this removed
                    NonNullList<ItemStack> restockerItems = UpgradeMethods.getRestockingItems(backpack);
                    for (ItemStack restockerItem : restockerItems) {
                        if (!restockerItem.isEmpty()) {

                            boolean foundSlot = false;
                            ItemStack stackToResupply = ItemStack.EMPTY;

                            for (int i = itemBackpack.getSize(backpack); i < itemBackpack.getSize(backpack) + 36; i++){ //check player's inv for items (backpack size + 36 for player inv)
                                Slot tempSlot = (Slot) container.getSlot(i);
                                if (tempSlot != null && tempSlot.getHasStack()){
                                    ItemStack tempItem = tempSlot.getStack();
                                    if (IronBackpacksHelper.areItemsEqualForStacking(stack, restockerItem) //has to be same items as what was used in the event
                                            && IronBackpacksHelper.areItemsEqualAndStackable(tempItem, restockerItem)){ //found and less than max stack size
                                        foundSlot = true;
                                        stackToResupply = tempItem;
                                        break;
                                    }
                                }
                            }

                            if (foundSlot){ // resupply from the backpack
                                for (int i = 0; i < itemBackpack.getSize(backpack); i++) {
                                    Slot backpackSlot = (Slot) container.getSlot(i);
                                    if (backpackSlot != null && backpackSlot.getHasStack()) {
                                        ItemStack backpackItemStack = backpackSlot.getStack();

                                        if (IronBackpacksHelper.areItemsEqualForStacking(stackToResupply, backpackItemStack)) {
                                            int amountToResupply = stackToResupply.getMaxStackSize() - stackToResupply.getCount();

                                            if (backpackItemStack.getCount() >= amountToResupply) {
                                                backpackSlot.decrStackSize(amountToResupply);
                                                container.sort();
                                                container.onContainerClosed(player);
                                                return (new ItemStack(stackToResupply.getItem(), stackToResupply.getMaxStackSize(), stackToResupply.getItemDamage()));

                                            } else {
                                                backpackSlot.decrStackSize(backpackItemStack.getCount());
                                                container.sort();
                                                container.onContainerClosed(player);
                                                return (new ItemStack(stackToResupply.getItem(), stackToResupply.getCount() + backpackItemStack.getCount(), stackToResupply.getItemDamage()));
                                                //don't have to iterate
                                                //b/c once sorted you have as big of a stack as you will ever have so it can only refill that much
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } //no save b/c returns and saves if it does anything
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Checks the hopper/restocking upgrade to try and refill items. Decrements from the backpack's stacks and updates the appropriate slot/stack in the player's inventory.
     * for each backpack
     *  if backpack has itemUsed in filter
     *      if backpack has itemUsed in inv
     *          resupply itemUsed
     *              get rid of backpackStack
     *              return new size of itemUsed stack
     * @param backpackStacks - the backpacks with this upgrade
     */
    protected static ItemStack checkRestockerUpgradeItemPlace(EntityPlayer player, EnumHand hand, ItemStack toResupply, NonNullList<ItemStack> backpackStacks){
        if (!backpackStacks.isEmpty()){

            for (ItemStack backpack : backpackStacks) {
//                BackpackTypes type = BackpackTypes.values()[((ItemBackpackSubItems) backpack.getItem()).getGuiId()];
                ItemBackpack itemBackpack = (ItemBackpack)backpack.getItem(); //TODO: hardcoded
                ContainerBackpack container = new ContainerBackpack(new InventoryBackpack(player, backpack));
                if (!(player.openContainer instanceof ContainerBackpack)) { //can't have the backpack open
                    container.sort(); //TODO: test with this removed
                    NonNullList<ItemStack> restockerItems = UpgradeMethods.getRestockingItems(backpack);
                    for (ItemStack restockerItem : restockerItems) {
                        if (!restockerItem.isEmpty()) {

                            boolean foundSlot = false;
                            ItemStack stackToResupply = ItemStack.EMPTY;

                            if (hand == EnumHand.OFF_HAND) {
                                if (IronBackpacksHelper.areItemsEqualForStacking(toResupply, restockerItem) && IronBackpacksHelper.areItemsEqualAndStackable(toResupply, restockerItem) ) {
                                    stackToResupply = toResupply;
                                    foundSlot = true;
                                }

                            } else {

                                for (int i = itemBackpack.getSize(backpack); i < itemBackpack.getSize(backpack) + 36; i++) { //check player's inv for items (backpack size + 36 for player inv)
                                    Slot tempSlot = (Slot) container.getSlot(i);
                                    if (tempSlot != null && tempSlot.getHasStack()) {
                                        ItemStack tempItem = tempSlot.getStack();
                                        if (IronBackpacksHelper.areItemsEqualForStacking(toResupply, restockerItem) //has to be same items as what was used in the event
                                                && IronBackpacksHelper.areItemsEqualAndStackable(tempItem, restockerItem)) { //found and less than max stack size
                                            foundSlot = true;
                                            stackToResupply = tempItem;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (foundSlot){ // resupply from the backpack
                                for (int i = 0; i < itemBackpack.getSize(backpack); i++) {
                                    Slot backpackSlot = (Slot) container.getSlot(i);
                                    if (backpackSlot != null && backpackSlot.getHasStack()) {
                                        ItemStack backpackItemStack = backpackSlot.getStack();

                                        if (IronBackpacksHelper.areItemsEqualForStacking(stackToResupply, backpackItemStack)) {
                                            int amountToResupply = stackToResupply.getMaxStackSize() - (stackToResupply.getCount() - 1);

                                            if (backpackItemStack.getCount() >= amountToResupply) {
                                                backpackSlot.decrStackSize(amountToResupply);
                                                container.sort();
                                                container.onContainerClosed(player);
                                                return (new ItemStack(stackToResupply.getItem(), stackToResupply.getMaxStackSize(), stackToResupply.getItemDamage()));

                                            } else {
                                                backpackSlot.decrStackSize(backpackItemStack.getCount());
                                                container.sort();
                                                container.onContainerClosed(player);
                                                return (new ItemStack(stackToResupply.getItem(), stackToResupply.getCount() + backpackItemStack.getCount(), stackToResupply.getItemDamage()));
                                                //don't have to iterate
                                                //b/c once sorted you have as big of a stack as you will ever have so it can only refill that much
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } //no save b/c returns and saves if it does anything
            }
        }
        return ItemStack.EMPTY;
    }


    /**
     * Checks the hopper/restocking upgrade to try and refill items. Decrements from the backpack's stacks and updates the appropriate slot/stack in the player's inventory.
     * for each backpack
     *  if backpack has itemUsed in filter
     *      if backpack has itemUsed in inv
     *          resupply itemUsed
     *              get rid of backpackStack
     *              return new size of itemUsed stack
     * @param backpackStacks - the backpacks with this upgrade
     */
    //ToDo: Need to fix it for derivative arrows, works for normal arrows only.
    protected static ImmutablePair<ItemStack, Slot> checkRestockerUpgradeArrowLoose(EntityPlayer player, NonNullList<ItemStack> backpackStacks){
        if (!backpackStacks.isEmpty()){

            for (ItemStack backpack : backpackStacks) {
//                BackpackTypes type = BackpackTypes.values()[((ItemBackpackSubItems) backpack.getItem()).getGuiId()];
                ItemBackpack itemBackpack = (ItemBackpack)backpack.getItem(); //TODO: hardcoded
                ContainerBackpack container = new ContainerBackpack(new InventoryBackpack(player, backpack));
                if (!(player.openContainer instanceof ContainerBackpack)) { //can't have the backpack open
                    container.sort(); //TODO: test with this removed
                    NonNullList<ItemStack> restockerItems = UpgradeMethods.getRestockingItems(backpack);
                    for (ItemStack restockerItem : restockerItems) {
                        if ((!restockerItem.isEmpty()) && (restockerItem.getItem() instanceof ItemArrow || restockerItem.getItem().getClass().isAssignableFrom(ItemArrow.class))) { //only restock arrows
                            boolean foundSlot = false;
                            ItemStack stackToResupply = ItemStack.EMPTY;
                            Slot slotToResupply = null;

                            for (int i = itemBackpack.getSize(backpack); i < itemBackpack.getSize(backpack) + 36; i++){ //check player's inv for items (backpack size + 36 for player inv)
                                Slot tempSlot = (Slot) container.getSlot(i);
                                if (tempSlot!= null && tempSlot.getHasStack()){
                                    ItemStack tempItem = tempSlot.getStack();
                                    if (tempItem.getItem() instanceof ItemArrow) {
                                        if (IronBackpacksHelper.areItemsEqualForStacking(tempItem, restockerItem) //has to be same items as what was used in the event
                                                && IronBackpacksHelper.areItemsEqualAndStackable(tempItem, restockerItem)) { //found and less than max stack size
                                            foundSlot = true;
                                            slotToResupply = tempSlot;
                                            stackToResupply = tempItem;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (foundSlot){ // resupply from the backpack
                                for (int i = 0; i < itemBackpack.getSize(backpack); i++) {
                                    Slot backpackSlot = (Slot) container.getSlot(i);
                                    if (backpackSlot != null && backpackSlot.getHasStack()) {
                                        ItemStack backpackItemStack = backpackSlot.getStack();

                                        if (IronBackpacksHelper.areItemsEqualForStacking(stackToResupply, backpackItemStack)) {
                                            int amountToResupply = stackToResupply.getMaxStackSize() - (stackToResupply.getCount() - 1);

                                            if (backpackItemStack.getCount() >= amountToResupply) {
                                                backpackSlot.decrStackSize(amountToResupply);
                                                container.sort();
                                                container.onContainerClosed(player);
                                                ItemStack stackToReturn = stackToResupply.copy();
                                                stackToReturn.setCount(stackToResupply.getMaxStackSize());
                                                return new ImmutablePair<>(stackToReturn, slotToResupply);

                                            } else {
                                                backpackSlot.decrStackSize(backpackItemStack.getCount());
                                                container.sort();
                                                container.onContainerClosed(player);
                                                ItemStack stackToReturn = stackToResupply.copy();
                                                stackToReturn.setCount(stackToResupply.getCount() + backpackItemStack.getCount());
                                                return new ImmutablePair<>(stackToReturn, slotToResupply);
                                                //don't have to iterate
                                                //b/c once sorted you have as big of a stack as you will ever have so it can only refill that much
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } //no save b/c returns and saves if it does anything
            }
        }
        return null;
    }

    /**
     * Checks the backpacks with the crafter/recipes upgrade to craft the specified items
     * @param event - EntityItemPickupEvent
     * @param backpackStacks - the backpacks with the crafter upgrade
     * @param craftingGridDiameterToFill - The size of the recipes grid to try filling with (1x1 or 2x2 or 3x3)
     */
    protected static void checkCrafterUpgrade(EntityItemPickupEvent event, NonNullList<ItemStack> backpackStacks, int craftingGridDiameterToFill){
        boolean shouldSave = false;
        if (!backpackStacks.isEmpty()){
            for (ItemStack backpack : backpackStacks) {
                shouldSave = false;
                if (!(event.getEntityPlayer().openContainer instanceof ContainerBackpack)) { //can't have the backpack open

//                    BackpackTypes type = BackpackTypes.values()[((ItemBackpackSubItems) backpack.getItem()).getGuiId()];
                    ItemBackpack itemBackpack = (ItemBackpack)backpack.getItem();
                    ContainerBackpack container = new ContainerBackpack(new InventoryBackpack(event.getEntityPlayer(), backpack));

                    container.sort(); //sort to make sure all items are in their smallest slot numbers possible
                    if (!container.getInventoryBackpack().getStackInSlot( //if the last slot has an items
                            container.getInventoryBackpack().getSizeInventory()).isEmpty()){ //assume the backpack is full and stop trying to craft
                        break; //TODO: test
                    }

                    ContainerWorkbench containerWorkbench = new ContainerWorkbench(event.getEntityPlayer().inventory, event.getItem().world, new BlockPos(0, 0, 0));
                    InventoryCrafting inventoryCrafting = new InventoryCrafting(containerWorkbench, 3, 3); //fake workbench/inventory for checking matching recipe

                    NonNullList<ItemStack> crafterItems;
                    switch (craftingGridDiameterToFill){
                        case 1:
                            crafterItems = UpgradeMethods.getCrafterTinyItems(backpack);
                            break;
                        case 2:
                            crafterItems = UpgradeMethods.getCrafterSmallItems(backpack);
                            break;
                        case 3:
                            crafterItems = UpgradeMethods.getCrafterItems(backpack);
                            break;
                        default: //should be unreachable
                            crafterItems = UpgradeMethods.getCrafterItems(backpack);
                            Logger.error("IronBackpacks CraftingUpgrade Error, will probably give the wrong output");
                    }

                    for (ItemStack crafterItem : crafterItems) {
                        if (!crafterItem.isEmpty()) {
                            for (int index = 0; index < itemBackpack.getSize(backpack); index++) {
                                final Slot theSlot = (Slot) container.getSlot(index);
                                if (theSlot!=null && theSlot.getHasStack()) {
                                    final ItemStack theStack = theSlot.getStack();
                                    if (!theStack.isEmpty() && theStack.getCount() >= (craftingGridDiameterToFill*craftingGridDiameterToFill) && IronBackpacksHelper.areItemsEqualForStacking(theStack, crafterItem)) {
                                        ItemStack myStack = new ItemStack(theStack.getItem(), 1, theStack.getItemDamage()); //stackSize of 1
                                        if (craftingGridDiameterToFill == 2){//special handling needed to make it a square
                                            inventoryCrafting.setInventorySlotContents(0, myStack);
                                            inventoryCrafting.setInventorySlotContents(1, myStack);
                                            inventoryCrafting.setInventorySlotContents(3, myStack);
                                            inventoryCrafting.setInventorySlotContents(4, myStack);
                                        }else {
                                            for (int i = 0; i < (craftingGridDiameterToFill*craftingGridDiameterToFill); i++) {
                                                inventoryCrafting.setInventorySlotContents(i, myStack); //recipes grid with a 1x1 (single items) or 3x3 square of the items
                                            }
                                        }
                                        ItemStack recipeOutput = CraftingManager.findMatchingRecipe(inventoryCrafting, event.getItem().world).getRecipeOutput().copy();
                                        if (!recipeOutput.isEmpty()) { //TODO: test math is correct here

                                            shouldSave = true;

                                            int numberOfIterations = Math.floorDiv(theStack.getCount(), (craftingGridDiameterToFill * craftingGridDiameterToFill));
                                            int numberOfItems = recipeOutput.getCount() * numberOfIterations;
                                            if (numberOfItems > 64){ //multiple stacks, need to make sure there is room

                                                //More efficient code [that doesn't work]
//                                                int tempNumberOfItems = numberOfItems;
//                                                int totalStacks = ((int)Math.ceil(numberOfItems / 64d));
//                                                for (int numOfStacks = 0; numOfStacks < totalStacks; numOfStacks++) {
//                                                    Logger.info("temp number of items: "+tempNumberOfItems);
//                                                    ItemStack myRecipeOutput = new ItemStack(recipeOutput.getItem(), tempNumberOfItems, recipeOutput.getItemDamage());
//                                                    if (container.transferStackInSlot(myRecipeOutput) != null) { //check if there is room to put them
//                                                        int decrementAmount = tempNumberOfItems >= 64 ? 64 : tempNumberOfItems;
//                                                        theSlot.decrStackSize(theStack.stackSize - ((int) Math.ceil(decrementAmount / recipeOutput.stackSize)));
//                                                    }
//                                                    tempNumberOfItems -= 64;
//                                                }

                                                //TODO: iterates an excessive amount, make it more efficient by using the basis of the code above
                                                for (int i = 0; i < numberOfIterations; i++){ //for every possible recipes operation
                                                    ItemStack myRecipeOutput = recipeOutput.copy(); //get the output
                                                    ItemStack stack = container.transferStackInSlot(myRecipeOutput); //try to put that output into the backpack
                                                    if (recipeOutput.getCount() == stack.getCount()){ //can't put it anywhere
                                                        break;
                                                    }else if (!stack.isEmpty()) { //remainder present, stack couldn't be fully transferred, undo the last operation
                                                        Slot slot = container.getSlot(itemBackpack.getSize(backpack)-1); //last slot in pack
                                                        slot.putStack(new ItemStack(recipeOutput.getItem(), recipeOutput.getMaxStackSize()-(recipeOutput.getCount() - stack.getCount()), recipeOutput.getItemDamage()));
                                                        break;
                                                    } else { //normal condition, stack was fully transferred
                                                    	System.out.println(theSlot.getStack());
                                                    	theSlot.decrStackSize(1);
                                                    }
                                                }
                                            }else {
                                                ItemStack myRecipeOutput = new ItemStack(recipeOutput.getItem(), numberOfItems, recipeOutput.getItemDamage());
                                                if (container.transferStackInSlot(myRecipeOutput).isEmpty()) 
                                                    theSlot.decrStackSize(numberOfItems * craftingGridDiameterToFill * craftingGridDiameterToFill);
                                                container.save(event.getEntityPlayer());
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (shouldSave) {
                        container.sort(); //sort items
                        container.onContainerClosed(event.getEntityPlayer());
                    }
                }
            }
        }
    }


    //===================================================================Filter Upgrade======================================================================

    /**
     * Checks the filters to see what items should be picked up and put in the backpack(s).
     * @param event - EntityItemPickupEvent
     * @param backpackStacks - the backpacks with a filter
     */
    protected static void checkFilterUpgrade(EntityItemPickupEvent event, NonNullList<ItemStack> backpackStacks){
        if (!backpackStacks.isEmpty()){
            for (ItemStack backpack : backpackStacks) {
                if(!backpack.isEmpty()){
                ContainerBackpack container = new ContainerBackpack(new InventoryBackpack(event.getEntityPlayer(), backpack));
                if (!(event.getEntityPlayer().openContainer instanceof ContainerBackpack)) { //can't have the backpack open
                    NonNullList<ItemStack> upgrades = IronBackpacksHelper.getUpgradesAppliedFromNBT(backpack);

                    if (UpgradeMethods.hasFilterBasicUpgrade(upgrades))
                        transferWithBasicFilter(UpgradeMethods.getBasicFilterItems(backpack), event, container);

                    if (UpgradeMethods.hasFilterModSpecificUpgrade(upgrades))
                        transferWithModSpecificFilter(UpgradeMethods.getModSpecificFilterItems(backpack), event, container);

                    if (UpgradeMethods.hasFilterFuzzyUpgrade(upgrades))
                        transferWithFuzzyFilter(UpgradeMethods.getFuzzyFilterItems(backpack), event, container);

                    if (UpgradeMethods.hasFilterOreDictUpgrade(upgrades))
                        transferWithOreDictFilter(UpgradeMethods.getOreDictFilterItems(backpack), getOreDict(event.getItem().getItem()), event, container);

                    if (UpgradeMethods.hasFilterVoidUpgrade(upgrades))
                        deleteWithVoidFilter(UpgradeMethods.getVoidFilterItems(backpack), event);

                    if (UpgradeMethods.hasFilterAdvancedUpgrade(upgrades)) {
                        NonNullList<ItemStack> advFilterItems = UpgradeMethods.getAdvFilterAllItems(backpack);
                        byte[] advFilterButtonStates = UpgradeMethods.getAdvFilterButtonStates(backpack);

                        transferWithBasicFilter(UpgradeMethods.getAdvFilterBasicItems(advFilterItems, advFilterButtonStates), event, container);
                        transferWithModSpecificFilter(UpgradeMethods.getAdvFilterModSpecificItems(advFilterItems, advFilterButtonStates), event, container);
                        transferWithFuzzyFilter(UpgradeMethods.getAdvFilterFuzzyItems(advFilterItems, advFilterButtonStates), event, container);
                        transferWithOreDictFilter(UpgradeMethods.getAdvFilterOreDictItems(advFilterItems, advFilterButtonStates), getOreDict(event.getItem().getItem()), event, container);
                        deleteWithVoidFilter(UpgradeMethods.getAdvFilterVoidItems(advFilterItems, advFilterButtonStates), event);
                    }

                    if (UpgradeMethods.hasFilterMiningUpgrade(upgrades))
                        transferWithMiningFilter(UpgradeMethods.getMiningFilterItems(backpack), getOreDict(event.getItem().getItem()), event, container);
                }
                }
            }
        }
    }

    /**
     * Transfers items with respect to exact matching.
     * @param filterItems - the itemstacks to check
     * @param event - EntityItemPickupEvent
     * @param container - the backpack to transfer items into
     */
    private static void transferWithBasicFilter(NonNullList<ItemStack> filterItems, EntityItemPickupEvent event, ContainerBackpack container){
        boolean shouldSave = false;
        for (ItemStack filterItem : filterItems) {
            if (!filterItem.isEmpty()) {
                if (IronBackpacksHelper.areItemsEqualForStacking(event.getItem().getItem(), filterItem)) {
                    ItemStack returned = container.transferStackInSlot(event.getItem().getItem()); //custom method to put itemEntity's itemStack into the backpack
                    if (returned.isEmpty()) shouldSave = true;
                }
            }
        }
        if (shouldSave) {
            playItemPickupSound(event);
            container.onContainerClosed(event.getEntityPlayer());
        }
    }

    /**
     * Transfers items ignoring damage values.
     * @param filterItems - the items to check
     * @param event - EntityItemPickupEvent
     * @param container - the backpack to transfer items into
     */
    private static void transferWithFuzzyFilter(NonNullList<ItemStack> filterItems, EntityItemPickupEvent event, ContainerBackpack container){
        boolean shouldSave = false;
        for (ItemStack filterItem : filterItems) {
            if (!filterItem.isEmpty()) {
                if (event.getItem().getItem().getItem() == filterItem.getItem()) {
                    ItemStack returned = container.transferStackInSlot(event.getItem().getItem()); //custom method to put itemEntity's itemStack into the backpack
                    if (!returned.isEmpty()) shouldSave = true;
                }
            }
        }
        if (shouldSave) {
            playItemPickupSound(event);
            container.onContainerClosed(event.getEntityPlayer());
        }
    }

    /**
     * Transfers items with respect to the ore dictionary
     * @param filterItems - the items to check
     * @param itemEntityOre - the ore dictionary entry of the items
     * @param event - EntityItemPickupEvent
     * @param container - the backpack to move items into
     */
    private static void transferWithOreDictFilter(NonNullList<ItemStack> filterItems, ArrayList<String> itemEntityOre, EntityItemPickupEvent event, ContainerBackpack container){
        boolean shouldSave = false;
        for (ItemStack filterItem : filterItems) {
            if (!filterItem.isEmpty()) {
                ArrayList<String> filterItemOre = getOreDict(filterItem);
                if (itemEntityOre != null && filterItemOre != null) {
                    for (String oreName : itemEntityOre) {
                        if (oreName != null && filterItemOre.contains(oreName)) {
                            ItemStack returned = container.transferStackInSlot(event.getItem().getItem()); //custom method to put itemEntity's itemStack into the backpack
                            if (returned.isEmpty()) shouldSave = true;
                        }
                    }
                }
            }
        }
        if (shouldSave) {
            playItemPickupSound(event);
            container.onContainerClosed(event.getEntityPlayer());
        }
    }

    /**
     * Transfers items with respect to the category of the same mod
     * @param filterItems - the items to check
     * @param event - EntityItemPickupEvent
     * @param container - the backpack to move the items into
     */
    private static void transferWithModSpecificFilter(NonNullList<ItemStack> filterItems, EntityItemPickupEvent event, ContainerBackpack container){
        boolean shouldSave = false;
        for (ItemStack filterItem : filterItems) {
            if (!filterItem.isEmpty()) {
                //if modId1 == modId2 same mod so transfer
                if ((event.getItem().getItem().getItem()).getRegistryName().getResourceDomain().equals(((filterItem.getItem()).getRegistryName().getResourceDomain()))){
                    ItemStack returned = container.transferStackInSlot(event.getItem().getItem()); //custom method to put itemEntity's itemStack into the backpack
                    if (!returned.isEmpty()) shouldSave = true;
                }
            }
        }
        if (shouldSave) {
            playItemPickupSound(event);
            container.onContainerClosed(event.getEntityPlayer());
        }
    }

    /**
     * Transfers items with ore in the name
     * @param filterItems - the items to check
     * @param event - EntityItemPickupEvent
     * @param container - the backpack to move the items into
     */
    private static void transferWithMiningFilter(NonNullList<ItemStack> filterItems, ArrayList<String> itemEntityOre, EntityItemPickupEvent event, ContainerBackpack container){
        boolean shouldSave = false;
        filterItems.add(new ItemStack(Items.COAL, 1, 0)); //add coal to filter
        addWhitelistEntriesToFilterItems(filterItems); //adds whitelisted items to filter
        transferWithBasicFilter(filterItems, event, container);
        if (itemEntityOre != null) {
            for (String oreName : itemEntityOre) {
                if (oreName != null && (oreName.startsWith("ore") || oreName.startsWith("gem") || oreName.startsWith("dust"))) {
                    ItemStack returned = container.transferStackInSlot(event.getItem().getItem()); //custom method to put itemEntity's itemStack into the backpack
                    if (!returned.isEmpty()) shouldSave = true;
                }
            }
        }
        if (shouldSave) {
            playItemPickupSound(event);
            container.onContainerClosed(event.getEntityPlayer());
        }
    }

    /**
     * Deletes items in the void filter by destroying the entityItem picked up intead of moving it into the backpack or elsewhere
     * @param filterItems - the items to delete
     * @param event - EntityItemPickupEvent
     */
    private static void deleteWithVoidFilter(NonNullList<ItemStack> filterItems, EntityItemPickupEvent event){
        for (ItemStack stack : filterItems) {
            if (!stack.isEmpty()) {
                if (event.getItem().getItem().getItem() == stack.getItem()){ //if same items (but different damage value)
                    event.getItem().setDead(); //delete it
                    event.getItem().onUpdate(); //update to make sure it's gone
                    event.setCanceled(true); //make sure it can't be picked up by other mods/vanilla
                }
            }
        }
    }

    /**
     * Gets the ore dictionary entries from an items
     * @param itemStack - the items to check
     * @return - OreDict entries in string form, null if no entries
     */
    private static ArrayList<String> getOreDict(ItemStack itemStack){
        int[] ids = OreDictionary.getOreIDs(itemStack);
        ArrayList<String> retList = new ArrayList<String>();
        if (ids.length > 0){
            for (int i = 0; i < ids.length; i++) {
                if (i > 0 && !retList.contains(OreDictionary.getOreName(ids[i]))) { //no duplicates
                    retList.add(OreDictionary.getOreName(ids[i]));
                }else{
                    retList.add(OreDictionary.getOreName(ids[i]));
                }
            }
        }
        return retList.isEmpty() ? null : retList;
    }

    //ToDo: Not at runtime
    private static NonNullList<ItemStack> addWhitelistEntriesToFilterItems(NonNullList<ItemStack> filterItems) {
        String[] whitelistEntries = ConfigHandler.filterMiningUpgradeWhitelist;
        for (String entry : whitelistEntries) {
            ItemStack stack = getItemStackFromString(entry);
            if (!stack.isEmpty()) filterItems.add(stack);
        }
        return filterItems;
    }

    private static Pattern pattern = Pattern.compile("((?<modid>.*?):)?(?<item>[^@]*)(@(?<damage>\\d+|[*])(x(?<size>\\d+))?)?");

    /**
     * Construct an {@link ItemStack} from a string in the format modid:itemName@damagexstackSize.
     * Thanks to Ordinaste for the code.
     *
     * @param str the str
     * @return the item
     */
    private static ItemStack getItemStackFromString(String str) {
        Matcher matcher = pattern.matcher(str);

        if (!matcher.find())
            return ItemStack.EMPTY;

        String itemString = matcher.group("item");
        if (itemString == null)
            return ItemStack.EMPTY;

        String modid = matcher.group("modid");
        if (modid == null)
            modid = "minecraft";

        int damage = 0;
        String strDamage = matcher.group("damage");
        if (strDamage != null)
            damage = strDamage.equals("*") ? OreDictionary.WILDCARD_VALUE : Integer.parseInt(matcher.group("damage")); //work on wildcard returns multiple items?
        int size = matcher.group("size") == null ? 1 : Integer.parseInt(matcher.group("size"));
        if (size == 0)
            size = 1;

        Item item = Item.getByNameOrId(modid + ":" + itemString);
        if (item == null)
            return ItemStack.EMPTY;

        return new ItemStack(item, size, damage);
    }

    /**
     * Play the item pickup sound
     * @param event - the item pick up event
     */
    private static void playItemPickupSound(EntityItemPickupEvent event){
        Random random = new Random();
        EntityPlayerMP playerMP = ((EntityPlayerMP)event.getEntityPlayer());
        playerMP.connection.sendPacket(new SPacketCustomSound("minecraft:entity.item.pickup", SoundCategory.PLAYERS, playerMP.getPositionVector().x, playerMP.getPositionVector().y, playerMP.getPositionVector().z, 0.3F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F));
    }

    /**
     * Handles any indirect restocking by scanning the inventory of the player for the first matching item rather than restocking the hand directly.
     * @param player
     * @param backpackStacks
     * @param toResupply
     */
    public static void handleIndirectRestock(EntityPlayer player, NonNullList<ItemStack> backpackStacks, ItemStack toResupply) {

        //if restockingItem matches toResupply
            //for each slot in player's inventory (starting with offhand)
                //if item in slot is equal to toResupply
                    //save that slot
            //if have a slot
                //iterate through backpack's inventory
                    //if found a slot with same item as toResupply
                        //join slot items together, update backpack
                            //if slot is now equal to maxStackSize stop iterating, otherwise continue

        boolean useOffhand;
        boolean foundSlot;
        int playerSlotIndexToRestockTo = 0;

        if (!backpackStacks.isEmpty()) {
            for (ItemStack backpack : backpackStacks) {
                ItemBackpack itemBackpack = (ItemBackpack) backpack.getItem(); //TODO: hardcoded
                ContainerBackpack container = new ContainerBackpack(new InventoryBackpack(player, backpack));
                if (!(player.openContainer instanceof ContainerBackpack)) { //can't have the backpack open
                    NonNullList<ItemStack> restockerItems = UpgradeMethods.getRestockingItems(backpack);
                    for (ItemStack restockerItem : restockerItems) {
                        foundSlot = false;
                        useOffhand = false;
                        if (!restockerItem.isEmpty() && IronBackpacksHelper.areItemsEqualAndStackable(toResupply, restockerItem)) {

                            //for each slot in player's inventory (starting with offhand)
                            if (sameItemForRestocking(player.inventory.offHandInventory.get(0), restockerItem)) {
                                foundSlot = true;
                                useOffhand = true;
                            } else {
                                for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                                    if (sameItemForRestocking(player.inventory.getStackInSlot(i), restockerItem)) {
                                        foundSlot = true;
                                        playerSlotIndexToRestockTo = i;
                                        break;
                                    }
                                }
                            }

                            //if slot exists to resupply to
                            if (foundSlot) {
                                for (int i = 0; i < itemBackpack.getSize(backpack); i++) { //check backpack's inv for items
                                    Slot backpackSlot = (Slot) container.getSlot(i);
                                    if (backpackSlot != null && backpackSlot.getHasStack()) {
                                        ItemStack backpackItemStack = backpackSlot.getStack();
                                        if (IronBackpacksHelper.areItemsEqualAndStackable(useOffhand ? player.inventory.offHandInventory.get(0) : player.inventory.mainInventory.get(playerSlotIndexToRestockTo), backpackItemStack)) { //found resupply slot (accounts for stack size at maximum)
                                            ItemStack stackToResupply = useOffhand ? player.inventory.offHandInventory.get(0) : player.inventory.mainInventory.get(playerSlotIndexToRestockTo);

                                            int amountToResupply = stackToResupply.getMaxStackSize() - stackToResupply.getCount();

                                            if (backpackItemStack.getCount() >= amountToResupply) {
                                                backpackSlot.decrStackSize(amountToResupply);
                                                container.onContainerClosed(player);

                                                if (useOffhand)
                                                    player.inventory.offHandInventory.get(0).setCount(stackToResupply.getMaxStackSize());
                                                else
                                                    player.inventory.mainInventory.get(playerSlotIndexToRestockTo).setCount(stackToResupply.getMaxStackSize());

                                                player.inventory.markDirty();
                                                return; //full stack size, no point in continuing to iterate

                                            } else {
                                                backpackSlot.decrStackSize(backpackItemStack.getCount());
                                                container.onContainerClosed(player);

                                                if (useOffhand)
                                                    player.inventory.offHandInventory.get(0).grow(backpackItemStack.getCount());
                                                else
                                                    player.inventory.mainInventory.get(playerSlotIndexToRestockTo).grow(backpackItemStack.getCount());

                                                player.inventory.markDirty();

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle restocking an item directly (off or mainhand only)
     * @param player
     * @param backpackStacks
     * @param toResupply
     */
    public static void handleDirectRestock(EntityPlayer player, NonNullList<ItemStack> backpackStacks, ItemStack toResupply, boolean preEvent) {

        boolean useOffhand;
        boolean foundSlot;
        boolean firstRestock;
        int extraCost = preEvent ? 1 : 0; //to deal with BlockPlaceEvent giving the itemStack.stackSize *before* it is placed

        if (!backpackStacks.isEmpty()) {
            for (ItemStack backpack : backpackStacks) {
                ItemBackpack itemBackpack = (ItemBackpack) backpack.getItem(); //TODO: hardcoded
                ContainerBackpack container = new ContainerBackpack(new InventoryBackpack(player, backpack));
                if (!(player.openContainer instanceof ContainerBackpack)) { //can't have the backpack open
                    NonNullList<ItemStack> restockerItems = UpgradeMethods.getRestockingItems(backpack);
                    for (ItemStack restockerItem : restockerItems) {
                        foundSlot = false;
                        useOffhand = false;
                        if (!restockerItem.isEmpty() && IronBackpacksHelper.areItemsEqualAndStackable(toResupply, restockerItem)) {

                            //for each slot in player's inventory (starting with offhand)
                            if (sameItemForRestocking(player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND), restockerItem)) {
                                foundSlot = true;
                                useOffhand = true;
                            } else if (sameItemForRestocking(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), restockerItem)) {
                                foundSlot = true;
                                useOffhand = false;
                            } else {
                                Logger.warn("Error with restocking. Please create a bug report detailing your actions on Github.");
                            }

                            //if slot exists to resupply to
                            if (foundSlot) {
                                firstRestock = true;
                                for (int i = 0; i < itemBackpack.getSize(backpack); i++) { //check backpack's inv for items
                                    Slot backpackSlot = (Slot) container.getSlot(i);
                                    if (backpackSlot != null && backpackSlot.getHasStack()) {
                                        ItemStack backpackItemStack = backpackSlot.getStack();
                                        if (IronBackpacksHelper.areItemsEqualAndStackable(useOffhand ? player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND) : player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), backpackItemStack)) { //found resupply slot (accounts for stack size at maximum)

                                            ItemStack stackToResupply = useOffhand ? player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND) : player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);

                                            if (stackToResupply.getCount() + backpackItemStack.getCount() >= stackToResupply.getMaxStackSize()) { //if can refill stack completely

                                                backpackSlot.decrStackSize(stackToResupply.getMaxStackSize() - stackToResupply.getCount() + extraCost);
                                                container.onContainerClosed(player);

                                                ItemStack copy = stackToResupply.copy();
                                                copy.setCount(copy.getMaxStackSize()); //max stack size
                                                if (useOffhand)
                                                    player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, copy);
                                                else
                                                    player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, copy);

                                                player.inventory.markDirty();
                                                return; //full stack size, no point in continuing to iterate

                                            } else { //can't fully refill with this stack
                                                backpackSlot.decrStackSize(backpackItemStack.getCount());
                                                container.onContainerClosed(player);

                                                ItemStack copy = stackToResupply.copy();
                                                copy.grow(backpackItemStack.getCount());
                                                if (firstRestock) copy.shrink(extraCost); //deal with placing block

                                                if (useOffhand)
                                                    player.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, copy);
                                                else
                                                    player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, copy);

                                                player.inventory.markDirty();
                                                firstRestock = false;

                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean sameItemForRestocking(ItemStack toFill, ItemStack toSupply){
        if (!toFill.isEmpty() && !toSupply.isEmpty()) {
            return IronBackpacksHelper.areItemsEqualAndStackable(toFill, toSupply);
        }
        return false;
    }
}

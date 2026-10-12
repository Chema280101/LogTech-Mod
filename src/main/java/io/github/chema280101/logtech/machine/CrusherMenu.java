package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CrusherMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ContainerData data;

    // Constructor cliente
    public CrusherMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new ItemStackHandler(2), new SimpleContainerData(7));
    }

    // Constructor servidor
    public CrusherMenu(int containerId, Inventory playerInventory, BlockPos pos, IItemHandler itemHandler, ContainerData data) {
        super(ModMenuTypes.CRUSHER.get(), containerId);
        this.pos = pos;
        this.data = data;

        checkContainerDataCount(data, 7);
        addDataSlots(data);

        // Ranura 0: Entrada
        this.addSlot(new SlotItemHandler(itemHandler, 0, 56, 35));

        // Ranura 1: Salida (el jugador solo puede retirar)
        this.addSlot(new SlotItemHandler(itemHandler, 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Inventario del jugador
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar del jugador
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public int getEnergy() {
        int low = this.data.get(2) & 0xFFFF;
        int high = this.data.get(3) & 0xFFFF;
        return (high << 16) | low;
    }

    public int getMaxEnergy() {
        int low = this.data.get(4) & 0xFFFF;
        int high = this.data.get(5) & 0xFFFF;
        return (high << 16) | low;
    }

    public MachineStatus getStatus() {
        int ord = this.data.get(6);
        if (ord >= 0 && ord < MachineStatus.values().length) {
            return MachineStatus.values()[ord];
        }
        return MachineStatus.NO_INPUT;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, ModBlocks.CRUSHER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index == 1) { // Salida: mover al inventario del jugador
                if (!this.moveItemStackTo(stackInSlot, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else if (index == 0) { // Entrada: mover al inventario del jugador
                if (!this.moveItemStackTo(stackInSlot, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else { // Desde el inventario del jugador
                if (CrusherRecipes.hasRecipe(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 2 && index < 29) {
                    if (!this.moveItemStackTo(stackInSlot, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 29 && index < 38) {
                    if (!this.moveItemStackTo(stackInSlot, 2, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }
}

package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CoalGeneratorMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ContainerData data;

    // Constructor cliente
    public CoalGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new ItemStackHandler(1), new SimpleContainerData(9));
    }

    // Constructor servidor
    public CoalGeneratorMenu(int containerId, Inventory playerInventory, BlockPos pos, IItemHandler itemHandler, ContainerData data) {
        super(ModMenuTypes.COAL_GENERATOR.get(), containerId);
        this.pos = pos;
        this.data = data;

        checkContainerDataCount(data, 9);
        addDataSlots(data);

        // Ranura 0: Combustible en (80, 53)
        this.addSlot(new SlotItemHandler(itemHandler, 0, 80, 53));

        // Inventario del jugador (3 filas)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar del jugador (1 fila)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getBurnTime() {
        return (this.data.get(1) << 16) | (this.data.get(0) & 0xFFFF);
    }

    public int getMaxBurnTime() {
        return (this.data.get(3) << 16) | (this.data.get(2) & 0xFFFF);
    }

    public int getEnergy() {
        return (this.data.get(5) << 16) | (this.data.get(4) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.data.get(7) << 16) | (this.data.get(6) & 0xFFFF);
    }

    public MachineStatus getStatus() {
        int ord = this.data.get(8);
        if (ord >= 0 && ord < MachineStatus.values().length) {
            return MachineStatus.values()[ord];
        }
        return MachineStatus.NO_INPUT;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        // Si el clic es en la ranura del generador (ranura 0)
        if (index == 0) {
            if (!this.moveItemStackTo(sourceStack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
            sourceSlot.onQuickCraft(sourceStack, copyStack);
        } else {
            // El clic es en el inventario del jugador
            if (CoalGeneratorBlockEntity.getBurnDuration(sourceStack) > 0) {
                if (!this.moveItemStackTo(sourceStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < 28) {
                // De inventario a hotbar
                if (!this.moveItemStackTo(sourceStack, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 28 && index < 37) {
                // De hotbar a inventario
                if (!this.moveItemStackTo(sourceStack, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        if (sourceStack.getCount() == copyStack.getCount()) {
            return ItemStack.EMPTY;
        }

        sourceSlot.onTake(player, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(player.level(), this.pos), player, io.github.chema280101.logtech.registry.ModBlocks.COAL_GENERATOR.get());
    }
}

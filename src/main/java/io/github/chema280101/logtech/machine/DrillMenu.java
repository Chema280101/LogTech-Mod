package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.LogTechConfig;
import io.github.chema280101.logtech.network.EnergyPriority;
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

public class DrillMenu extends AbstractContainerMenu implements PriorityConfigurableMenu {
    private final BlockPos pos;
    private final ContainerData data;

    // Constructor cliente
    public DrillMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new ItemStackHandler(1), new ItemStackHandler(1), new SimpleContainerData(10));
    }

    // Constructor servidor
    public DrillMenu(int containerId, Inventory playerInventory, BlockPos pos, IItemHandler bufferHandler, IItemHandler firmwareHandler, ContainerData data) {
        super(ModMenuTypes.DRILL.get(), containerId);
        this.pos = pos;
        this.data = data;

        checkContainerDataCount(data, 10);
        addDataSlots(data);

        // Ranura 0: Búfer de salida (el jugador solo puede retirar)
        this.addSlot(new SlotItemHandler(bufferHandler, 0, 80, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Ranura 1: Firmware
        this.addSlot(new SlotItemHandler(firmwareHandler, 0, 80, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof io.github.chema280101.logtech.item.FirmwareItem;
            }
        });

        // Inventario del jugador (2..28)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar del jugador (29..37)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        int max = this.data.get(1);
        return max > 0 ? max : LogTechConfig.DRILL_PROCESS_TIME.get();
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
        return MachineStatus.NO_RESOURCE;
    }

    @Override
    public EnergyPriority getEnergyPriority() {
        return EnergyPriority.fromOrdinal(this.data.get(7));
    }

    @Override
    public void setEnergyPriority(EnergyPriority priority) {
        this.data.set(7, priority != null ? priority.ordinal() : EnergyPriority.NORMAL.ordinal());
    }

    public int getHeat() {
        return this.data.get(8);
    }

    public double getCooling() {
        return this.data.get(9) / 10.0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, ModBlocks.DRILL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            // Índices: 0=Búfer salida, 1=Firmware, 2..28=Inventario jugador, 29..37=Hotbar
            if (index == 0) {
                // Desde salida hacia el inventario del jugador
                if (!this.moveItemStackTo(stackInSlot, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else if (index == 1) {
                // Desde firmware hacia el inventario del jugador
                if (!this.moveItemStackTo(stackInSlot, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Desde inventario del jugador hacia firmware si es chip
                if (stackInSlot.getItem() instanceof io.github.chema280101.logtech.item.FirmwareItem) {
                    if (!this.moveItemStackTo(stackInSlot, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (index >= 2 && index < 29) {
                        // De inventario principal a hotbar
                        if (!this.moveItemStackTo(stackInSlot, 29, 38, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 29 && index < 38) {
                        // De hotbar a inventario principal
                        if (!this.moveItemStackTo(stackInSlot, 2, 29, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                    return ItemStack.EMPTY;
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

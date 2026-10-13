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
import io.github.chema280101.logtech.network.EnergyPriority;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class WasherMenu extends AbstractContainerMenu implements PriorityConfigurableMenu {
    public static final int DATA_COUNT = 12;

    private final BlockPos pos;
    private final ContainerData data;

    // Constructor cliente
    public WasherMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new ItemStackHandler(WasherBlockEntity.TOTAL_SLOTS), new SimpleContainerData(DATA_COUNT));
    }

    // Constructor servidor
    public WasherMenu(int containerId, Inventory playerInventory, BlockPos pos, IItemHandler itemHandler, ContainerData data) {
        super(ModMenuTypes.WASHER.get(), containerId);
        this.pos = pos;
        this.data = data;

        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);

        // Ranura 0: Entrada
        this.addSlot(new SlotItemHandler(itemHandler, WasherBlockEntity.SLOT_INPUT, 56, 35));

        // Ranura 1: Salida de polvo (el jugador solo puede retirar)
        this.addSlot(new SlotItemHandler(itemHandler, WasherBlockEntity.SLOT_OUTPUT_DUST, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Ranura 2: Salida de lodo (el jugador solo puede retirar)
        this.addSlot(new SlotItemHandler(itemHandler, WasherBlockEntity.SLOT_OUTPUT_SLUDGE, 136, 35) {
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

    public int getFluidAmount() {
        int low = this.data.get(7) & 0xFFFF;
        int high = this.data.get(8) & 0xFFFF;
        return (high << 16) | low;
    }

    public int getFluidCapacity() {
        int low = this.data.get(9) & 0xFFFF;
        int high = this.data.get(10) & 0xFFFF;
        return (high << 16) | low;
    }

    @Override
    public EnergyPriority getEnergyPriority() {
        return EnergyPriority.fromOrdinal(this.data.get(11));
    }

    @Override
    public void setEnergyPriority(EnergyPriority priority) {
        this.data.set(11, priority != null ? priority.ordinal() : EnergyPriority.NORMAL.ordinal());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, ModBlocks.WASHER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            // Índices: 0=Entrada, 1..2=Salidas, 3..29=Inventario jugador, 30..38=Hotbar
            if (index == WasherBlockEntity.SLOT_OUTPUT_DUST || index == WasherBlockEntity.SLOT_OUTPUT_SLUDGE) {
                // Desde salidas hacia el inventario del jugador
                if (!this.moveItemStackTo(stackInSlot, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else if (index == WasherBlockEntity.SLOT_INPUT) {
                // Desde entrada hacia el inventario del jugador
                if (!this.moveItemStackTo(stackInSlot, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Desde inventario del jugador hacia la máquina (ranura de entrada 0)
                if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                    if (index >= 3 && index < 30) {
                        // De inventario principal a hotbar
                        if (!this.moveItemStackTo(stackInSlot, 30, 39, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 30 && index < 39) {
                        // De hotbar a inventario principal
                        if (!this.moveItemStackTo(stackInSlot, 3, 30, false)) {
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

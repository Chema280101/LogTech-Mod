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

public class CentrifugeMenu extends AbstractContainerMenu implements PriorityConfigurableMenu {
    private final BlockPos pos;
    private final ContainerData data;

    // Constructor cliente
    public CentrifugeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new ItemStackHandler(CentrifugeBlockEntity.TOTAL_SLOTS), new ItemStackHandler(1), new SimpleContainerData(10));
    }

    // Constructor servidor
    public CentrifugeMenu(int containerId, Inventory playerInventory, BlockPos pos, IItemHandler itemHandler, IItemHandler firmwareHandler, ContainerData data) {
        super(ModMenuTypes.CENTRIFUGE.get(), containerId);
        this.pos = pos;
        this.data = data;

        checkContainerDataCount(data, 10);
        addDataSlots(data);

        // Ranura 0: Entrada
        this.addSlot(new SlotItemHandler(itemHandler, CentrifugeBlockEntity.INPUT_SLOT, 48, 35));

        // Ranuras 1, 2, 3: Salidas (el jugador solo puede retirar)
        this.addSlot(new SlotItemHandler(itemHandler, CentrifugeBlockEntity.OUTPUT_SLOT_1, 102, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, CentrifugeBlockEntity.OUTPUT_SLOT_2, 122, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(itemHandler, CentrifugeBlockEntity.OUTPUT_SLOT_3, 142, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Ranura 4: Firmware
        this.addSlot(new SlotItemHandler(firmwareHandler, 0, 48, 15) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof io.github.chema280101.logtech.item.FirmwareItem;
            }
        });

        // Inventario del jugador (5..31)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar del jugador (32..40)
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
        return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, ModBlocks.CENTRIFUGE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            // Índices: 0=Entrada, 1..3=Salidas, 4=Firmware, 5..31=Inventario jugador, 32..40=Hotbar
            if (index >= 1 && index <= 3) {
                // Desde salidas hacia el inventario del jugador
                if (!this.moveItemStackTo(stackInSlot, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else if (index == 0 || index == 4) {
                // Desde entrada o firmware hacia el inventario del jugador
                if (!this.moveItemStackTo(stackInSlot, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Desde inventario del jugador hacia la máquina
                if (stackInSlot.getItem() instanceof io.github.chema280101.logtech.item.FirmwareItem) {
                    if (!this.moveItemStackTo(stackInSlot, 4, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                    if (index >= 5 && index < 32) {
                        // De inventario principal a hotbar
                        if (!this.moveItemStackTo(stackInSlot, 32, 41, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 32 && index < 41) {
                        // De hotbar a inventario principal
                        if (!this.moveItemStackTo(stackInSlot, 5, 32, false)) {
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

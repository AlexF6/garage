package io.github.alexf6.garage;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Vehículo con baúl de 1 a 3 filas (patrón de {@code AbstractChestBoat}). Reúne todo el contrato de
 * {@link ContainerEntity} para que la clase del vehículo motorizado no cargue con él: guardado del contenido,
 * menú, tabla de botín y vaciado al destruirse.
 */
public abstract class ChestVehicleEntity extends VehicleEntity implements ContainerEntity, HasCustomInventoryScreen {

    private final int containerRows;
    private NonNullList<ItemStack> itemStacks;
    private ResourceKey<LootTable> lootTable;
    private long lootTableSeed;

    /** @param containerRows filas de 9 casillas (1 a 3); se pasa por constructor porque el de {@link Entity} ya pide el tamaño */
    protected ChestVehicleEntity(EntityType<? extends ChestVehicleEntity> type, Level level, int containerRows) {
        super(type, level);
        this.containerRows = containerRows;
        this.itemStacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        addChestVehicleSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        readChestVehicleSaveData(input);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide() && reason.shouldDestroy()) {
            Containers.dropContents(level(), this, this);
        }
        super.remove(reason);
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        player.openMenu(this);
        if (!player.level().isClientSide()) {
            gameEvent(GameEvent.CONTAINER_OPEN, player);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        unpackChestVehicleLootTable(inventory.player);
        MenuType<ChestMenu> menuType = switch (containerRows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            default -> throw new IllegalStateException("filas de baúl no soportadas: " + containerRows);
        };
        return new ChestMenu(menuType, containerId, inventory, this, containerRows);
    }

    @Override
    public void clearContent() {
        clearChestVehicleContent();
    }

    @Override
    public int getContainerSize() {
        return containerRows * 9;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getChestVehicleItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return removeChestVehicleItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeChestVehicleItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setChestVehicleItem(slot, stack);
    }

    @Override
    public SlotAccess getSlot(int slot) {
        return getChestVehicleSlot(slot);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return isChestVehicleStillValid(player);
    }

    @Override
    public ResourceKey<LootTable> getContainerLootTable() {
        return this.lootTable;
    }

    @Override
    public void setContainerLootTable(ResourceKey<LootTable> lootTable) {
        this.lootTable = lootTable;
    }

    @Override
    public long getContainerLootTableSeed() {
        return this.lootTableSeed;
    }

    @Override
    public void setContainerLootTableSeed(long seed) {
        this.lootTableSeed = seed;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return this.itemStacks;
    }

    @Override
    public void clearItemStacks() {
        this.itemStacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
    }
}

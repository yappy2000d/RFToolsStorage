package mcjty.rftoolsstorage.craftinggrid;

import mcjty.lib.McJtyLib;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.varia.Logging;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

public class PacketGridSync {

    protected BlockPos pos;
    protected DimensionType type;
    private List<ItemStack[]> recipes;

    public void convertFromBytes(PacketBuffer buf) {
        if (buf.readBoolean()) {
            pos = buf.readBlockPos();
        } else {
            pos = null;
        }
        type = DimensionType.getById(buf.readInt());
        int s = buf.readInt();
        recipes = new ArrayList<>(s);
        for (int i = 0 ; i < s ; i++) {
            int ss = buf.readInt();
            ItemStack[] stacks = new ItemStack[ss];
            for (int j = 0 ; j < ss ; j++) {
                stacks[j] = buf.readItemStack();
            }
            recipes.add(stacks);
        }
    }

    public void convertToBytes(PacketBuffer buf) {
        if (pos != null) {
            buf.writeBoolean(true);
            buf.writeBlockPos(pos);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeInt(type.getId());
        buf.writeInt(recipes.size());
        for (ItemStack[] recipe : recipes) {
            buf.writeInt(recipe.length);
            for (ItemStack stack : recipe) {
                buf.writeItemStack(stack);
            }
        }
    }

    protected void init(BlockPos pos, DimensionType type, CraftingGrid grid) {
        this.pos = pos;
        this.type = type;
        recipes = new ArrayList<>();
        for (int i = 0 ; i < 6 ; i++) {
            CraftingRecipe recipe = grid.getRecipe(i);
            CraftingInventory inventory = recipe.getInventory();
            ItemStack[] stacks = new ItemStack[10];
            stacks[0] = recipe.getResult();
            for (int j = 0 ; j < 9 ; j++) {
                stacks[j+1] = inventory.getStackInSlot(j);
            }
            recipes.add(stacks);
        }
    }

    protected CraftingGridProvider handleMessage(World world, PlayerEntity player) {
        CraftingGridProvider provider = null;

        TileEntity te;
        if (pos == null) {
            // We are working from a tablet. Find the tile entity through the open container
            GenericContainer container = getOpenContainer();
            if (container == null) {
                Logging.log("Container is missing!");
                return null;
            }
            te = container.getTe();
        } else {
            te = world.getTileEntity(pos);
        }

        if (te instanceof CraftingGridProvider) {
            provider = ((CraftingGridProvider) te);
        }

        if (provider != null) {
            for (int i = 0; i < recipes.size(); i++) {
                provider.setRecipe(i, recipes.get(i));
            }
        }
        return provider;
    }

    private static GenericContainer getOpenContainer() {
        Container container = McJtyLib.proxy.getClientPlayer().openContainer;
        if (container instanceof GenericContainer) {
            return (GenericContainer) container;
        } else {
            return null;
        }
    }

}

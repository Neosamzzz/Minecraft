package newearth;

import galaxyspace.GalaxySpace;
import galaxyspace.core.configs.GSConfigCore;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/** Opens the existing GS information screen. The item is not consumed. */
public final class ControlPanelItem extends Item {
    public ControlPanelItem() {
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        player.openGui(GalaxySpace.MODID, GSConfigCore.guiIDGuideBook, world, 0, 0, 0);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
}

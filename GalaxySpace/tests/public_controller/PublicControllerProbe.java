package diagnostic;
import java.lang.reflect.*;
import java.util.UUID;
import sun.misc.Unsafe;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController;
import micdoodle8.mods.galacticraft.core.client.gui.element.GuiElementTextBox;
import micdoodle8.mods.galacticraft.core.client.gui.element.GuiElementCheckbox;

public class PublicControllerProbe {
    public static void main(String[] args) throws Exception {
        net.minecraft.init.Bootstrap.func_151354_b();
        Field uf=Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);Unsafe u=(Unsafe)uf.get(null);
        TileEntityLaunchController tile=(TileEntityLaunchController)u.allocateInstance(TileEntityLaunchController.class);
        tile.ownerName="OriginalOwner";
        EntityPlayer player=(EntityPlayer)u.allocateInstance(EntityPlayerMP.class);
        for(Class<?> c=player.getClass();c!=null;c=c.getSuperclass())
            for(Field f:c.getDeclaredFields()) if(f.getType()==GameProfile.class){f.setAccessible(true);f.set(player,new GameProfile(UUID.randomUUID(),"OtherPlayer"));}
        boolean expected=Boolean.getBoolean("probe.mixins");
        String[] names={"GuiLaunchController","GuiLaunchControllerAdvanced"};
        Class<?>[] widgets={GuiElementTextBox.class,GuiElementCheckbox.class};
        for(int i=0;i<names.length;i++) {
            Class<?> c=Class.forName("micdoodle8.mods.galacticraft.planets.mars.client.gui."+names[i]);
            Object gui=u.allocateInstance(c);
            Field f=c.getDeclaredField("launchController");f.setAccessible(true);f.set(gui,tile);
            boolean allowed=(Boolean)c.getMethod("canPlayerEdit",widgets[i],EntityPlayer.class).invoke(gui,null,player);
            if(allowed!=expected) throw new AssertionError("Unexpected editing permission: "+names[i]);
            if(!"OriginalOwner".equals(tile.getOwnerName()))throw new AssertionError("Owner changed");
            if(i==1) {
                Class<?> widget=Class.forName("micdoodle8.mods.galacticraft.core.client.gui.element.GuiElementDropdown");
                Object dropdown=u.allocateInstance(widget);
                Field df=c.getDeclaredField("dropdownTest");df.setAccessible(true);df.set(gui,dropdown);
                boolean clickable=(Boolean)c.getMethod("canBeClickedBy",widget,EntityPlayer.class).invoke(gui,dropdown,player);
                if(clickable!=expected)throw new AssertionError("Unexpected dropdown permission");
            }
            System.out.println("PASS: non-owner editing="+allowed+", stored owner preserved: "+names[i]);
        }
    }
}

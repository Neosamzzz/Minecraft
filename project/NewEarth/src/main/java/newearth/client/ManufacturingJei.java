package newearth.client;

import java.util.*;
import newearth.Manufacturing;
import mezz.jei.api.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Recipe lookup for the two stations; GS owns assembler recipe displays. */
@JEIPlugin
public final class ManufacturingJei implements IModPlugin {
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        for(String id:new String[]{"material_station","engine_station"})registration.addRecipeCategories(new Category(id,registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void register(IModRegistry registry) {
        for(String id:new String[]{"material_station","engine_station"}) {
            List<Wrapper> recipes=new ArrayList<>();
            for(Manufacturing.Batch batch:Manufacturing.batches())if(batch.machine.equals(id))recipes.add(new Wrapper(batch));
            registry.addRecipes(recipes,"newearth."+id);
            registry.addRecipeCatalyst(Manufacturing.own(id),"newearth."+id);
        }
    }
    private static final class Wrapper implements IRecipeWrapper {
        final Manufacturing.Batch batch;
        Wrapper(Manufacturing.Batch batch){this.batch=batch;}
        @Override public void getIngredients(IIngredients ingredients) {
            ingredients.setInputs(ItemStack.class,batch.inputs);
            ingredients.setOutput(ItemStack.class,batch.output);
        }
    }
    private static final class Category implements IRecipeCategory<Wrapper> {
        final String id;final IDrawable background,slot;
        Category(String id,IGuiHelper helper){this.id=id;this.background=helper.createBlankDrawable(150,66);this.slot=helper.getSlotDrawable();}
        @Override public String getUid(){return "newearth."+id;}
        @Override public String getTitle(){return Manufacturing.own(id).getDisplayName();}
        @Override public String getModName(){return "NewEarth";}
        @Override public IDrawable getBackground(){return background;}
        @Override public void drawExtras(Minecraft mc){for(int i=0;i<6;i++)slot.draw(mc,(i%3)*20,(i/3)*20+10);slot.draw(mc,124,20);mc.fontRenderer.drawString("→",90,25,0x555555);}
        @Override public void setRecipe(IRecipeLayout layout,Wrapper wrapper,IIngredients ingredients){
            for(int i=0;i<wrapper.batch.inputs.size();i++)layout.getItemStacks().init(i,true,(i%3)*20,(i/3)*20+10);
            layout.getItemStacks().init(wrapper.batch.inputs.size(),false,124,20);layout.getItemStacks().set(ingredients);
        }
    }
}

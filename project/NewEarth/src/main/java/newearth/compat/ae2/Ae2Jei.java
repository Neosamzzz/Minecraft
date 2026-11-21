package newearth.compat.ae2;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.*;
import mezz.jei.api.ingredients.VanillaTypes;
import net.minecraft.item.ItemStack;

@JEIPlugin
public final class Ae2Jei implements IModPlugin {
    private mezz.jei.api.ingredients.IIngredientRegistry ingredients;
    @Override public void register(IModRegistry registry) { ingredients = registry.getIngredientRegistry(); }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = new ArrayList<>();
        for (ItemStack stack : ingredients.getAllIngredients(VanillaTypes.ITEM))
            if (Ae2Integration.retired(stack)) hidden.add(stack);
        ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM, hidden);
    }
}

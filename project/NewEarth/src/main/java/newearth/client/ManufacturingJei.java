package newearth.client;

import java.util.*;
import newearth.Manufacturing;
import mezz.jei.api.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Recipe lookup for the stations; GS owns assembler recipe displays. */
@JEIPlugin
public final class ManufacturingJei implements IModPlugin {
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        for (String id : new String[]{"material_station", "engine_station"}) {
            registration.addRecipeCategories(new Category(id, helper));
        }
        registration.addRecipeCategories(new AssemblyCategory(helper));
        registration.addRecipeCategories(new DisassemblyCategory(helper));
    }

    @Override public void register(IModRegistry registry) {
        for (String id : new String[]{"material_station", "engine_station"}) {
            List<Wrapper> recipes = new ArrayList<>();
            for (Manufacturing.Batch batch : Manufacturing.batches())
                if (batch.machine.equals(id)) recipes.add(new Wrapper(batch));
            registry.addRecipes(recipes, "newearth." + id);
            registry.addRecipeCatalyst(Manufacturing.own(id), "newearth." + id);
        }
        List<AssemblyWrapper> assemblyRecipes = new ArrayList<>();
        List<DisassemblyWrapper> disassemblyRecipes = new ArrayList<>();
        for (int tier : new int[] {7, 9, 10}) {
            ItemStack rocket = newearth.AssemblyStationTile.output(tier);
            assemblyRecipes.add(new AssemblyWrapper(newearth.AssemblyStationTile.inputs(tier), rocket, "T" + tier));
            disassemblyRecipes.add(new DisassemblyWrapper(rocket, newearth.DisassemblyStationTile.parts(rocket)));
        }
        registry.addRecipes(assemblyRecipes, "newearth.assembly_station");
        registry.addRecipeCatalyst(Manufacturing.own("assembly_station"), "newearth.assembly_station");
        registry.addRecipes(disassemblyRecipes, "newearth.disassembly_station");
        registry.addRecipeCatalyst(Manufacturing.own("disassembly_station"), "newearth.disassembly_station");
    }

    private static final class Wrapper implements IRecipeWrapper {
        final Manufacturing.Batch batch;
        Wrapper(Manufacturing.Batch batch) { this.batch = batch; }
        @Override public void getIngredients(IIngredients ingredients) {
            ingredients.setInputs(ItemStack.class, batch.inputs);
            ingredients.setOutput(ItemStack.class, batch.output);
        }
    }

    private static final class Category implements IRecipeCategory<Wrapper> {
        final String id; final IDrawable background, slot;
        Category(String id, IGuiHelper helper) {
            this.id = id;
            this.background = helper.createBlankDrawable(150, 66);
            this.slot = helper.getSlotDrawable();
        }
        @Override public String getUid() { return "newearth." + id; }
        @Override public String getTitle() { return Manufacturing.own(id).getDisplayName(); }
        @Override public String getModName() { return "NewEarth"; }
        @Override public IDrawable getBackground() { return background; }
        @Override public void drawExtras(Minecraft mc) {
            for (int i = 0; i < 6; i++) slot.draw(mc, (i % 3) * 20, (i / 3) * 20 + 10);
            slot.draw(mc, 124, 20);
            mc.fontRenderer.drawString("→", 90, 25, 0x555555);
        }
        @Override public void setRecipe(IRecipeLayout layout, Wrapper wrapper, IIngredients ingredients) {
            for (int i = 0; i < wrapper.batch.inputs.size(); i++)
                layout.getItemStacks().init(i, true, (i % 3) * 20, (i / 3) * 20 + 10);
            layout.getItemStacks().init(wrapper.batch.inputs.size(), false, 124, 20);
            layout.getItemStacks().set(ingredients);
        }
    }

    /** Wrapper for assembly station recipes (T7/T9 rockets). */
    private static final class AssemblyWrapper implements IRecipeWrapper {
        final List<ItemStack> inputs;
        final ItemStack output;
        final String tier;
        AssemblyWrapper(List<ItemStack> inputs, ItemStack output, String tier) {
            this.inputs = inputs; this.output = output; this.tier = tier;
        }
        @Override public void getIngredients(IIngredients ingredients) {
            ingredients.setInputs(ItemStack.class, inputs);
            ingredients.setOutput(ItemStack.class, output);
        }
    }

    /** JEI category for the assembly station (9 inputs + 1 output). */
    private static final class AssemblyCategory implements IRecipeCategory<AssemblyWrapper> {
        final IDrawable background, slot;
        AssemblyCategory(IGuiHelper helper) {
            this.background = helper.createBlankDrawable(180, 80);
            this.slot = helper.getSlotDrawable();
        }
        @Override public String getUid() { return "newearth.assembly_station"; }
        @Override public String getTitle() { return Manufacturing.own("assembly_station").getDisplayName(); }
        @Override public String getModName() { return "NewEarth"; }
        @Override public IDrawable getBackground() { return background; }
        @Override public void drawExtras(Minecraft mc) {
            // 3x3 input grid
            for (int i = 0; i < 10; i++) {
                slot.draw(mc, (i % 5) * 18 + 2, (i / 5) * 18 + 10);
            }
            // Output slot
            slot.draw(mc, 152, 20);
            mc.fontRenderer.drawString("→", 120, 25, 0x555555);
        }
        @Override public void setRecipe(IRecipeLayout layout, AssemblyWrapper wrapper, IIngredients ingredients) {
            for (int i = 0; i < 10; i++) {
                layout.getItemStacks().init(i, true, (i % 5) * 18 + 2, (i / 5) * 18 + 10);
            }
            layout.getItemStacks().init(10, false, 152, 20);
            layout.getItemStacks().set(ingredients);
        }
    }

    /** Wrapper for disassembly station recipes (1 rocket -> 9 parts). */
    private static final class DisassemblyWrapper implements IRecipeWrapper {
        final ItemStack input;
        final List<ItemStack> outputs;
        DisassemblyWrapper(ItemStack input, List<ItemStack> outputs) {
            this.input = input; this.outputs = outputs;
        }
        @Override public void getIngredients(IIngredients ingredients) {
            ingredients.setInput(ItemStack.class, input);
            ingredients.setOutputs(ItemStack.class, outputs);
        }
    }

    /** JEI category for the disassembly station (1 input + 9 outputs). */
    private static final class DisassemblyCategory implements IRecipeCategory<DisassemblyWrapper> {
        final IDrawable background, slot;
        DisassemblyCategory(IGuiHelper helper) {
            this.background = helper.createBlankDrawable(180, 80);
            this.slot = helper.getSlotDrawable();
        }
        @Override public String getUid() { return "newearth.disassembly_station"; }
        @Override public String getTitle() { return Manufacturing.own("disassembly_station").getDisplayName(); }
        @Override public String getModName() { return "NewEarth"; }
        @Override public IDrawable getBackground() { return background; }
        @Override public void drawExtras(Minecraft mc) {
            slot.draw(mc, 10, 40); // input
            for (int i = 0; i < 10; i++) {
                slot.draw(mc, (i % 5) * 18 + 80, (i / 5) * 18 + 10);
            }
            mc.fontRenderer.drawString("→", 55, 45, 0x555555);
        }
        @Override public void setRecipe(IRecipeLayout layout, DisassemblyWrapper wrapper, IIngredients ingredients) {
            layout.getItemStacks().init(0, true, 10, 40);
            for (int i = 0; i < 10; i++) {
                layout.getItemStacks().init(1 + i, false, (i % 5) * 18 + 80, (i / 5) * 18 + 10);
            }
            layout.getItemStacks().set(ingredients);
        }
    }
}

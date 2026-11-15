package newearth.client;

import java.util.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import newearth.storage.*;

/** Wrap the selected JEI handler after plugin registration, retaining vanilla fallback away from drawers. */
public final class DrawerTransferJei {
    private static final Map<Container,TransferNetwork.EndpointState> ENDPOINTS=new WeakHashMap<>();
    private static int nextToken;
    /** Kept separate so proxy construction does not load optional JEI types when JEI is absent. */
    public static final class Bridge extends TransferNetwork.ClientBridge {
        @Override public void receive(TransferNetwork.Endpoint message) {
            if(net.minecraftforge.fml.common.Loader.isModLoaded("jei"))DrawerTransferJei.receive(message);
        }
    }
    private static void receive(TransferNetwork.Endpoint message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            EntityPlayer player=Minecraft.getMinecraft().player;
            if(player==null)return;
            TransferNetwork.EndpointState state=ENDPOINTS.get(player.openContainer);
            Boolean previous=state==null?null:state.connected;
            if(state!=null && state.accept(message) && !Objects.equals(previous,state.connected)
                    && Minecraft.getMinecraft().currentScreen instanceof mezz.jei.gui.recipes.RecipesGui)
                ((mezz.jei.gui.recipes.RecipesGui)Minecraft.getMinecraft().currentScreen).onStateChange();
        });
    }
    private static Boolean connected(Container container) {
        TransferNetwork.EndpointState state=ENDPOINTS.get(container);
        if(state==null) {
            state=new TransferNetwork.EndpointState(container.windowId,++nextToken);
            ENDPOINTS.put(container,state);
        }
        long now=System.nanoTime();
        if(state.lastQuery==0 || now-state.lastQuery>1000000000L) {
            state.lastQuery=now;TransferNetwork.query(state.window,state.token);
        }
        return state.connected;
    }
    public static Object wrap(Object original,Object rawContainer,Object rawCategory) {
        Container container=(Container)rawContainer;IRecipeCategory<?> category=(IRecipeCategory<?>)rawCategory;
        TransferProfiles.Profile profile=TransferProfiles.get(container,category.getUid());
        if (profile==null || profile.slots(container).isEmpty()) return original;
        return new Handler((IRecipeTransferHandler<?>)original,profile);
    }
    private static final class Handler implements IRecipeTransferHandler<Container> {
        private final IRecipeTransferHandler fallback;
        private final TransferProfiles.Profile profile;
        Handler(IRecipeTransferHandler<?> fallback,TransferProfiles.Profile profile) { this.fallback=fallback;this.profile=profile; }
        public Class<Container> getContainerClass() { return Container.class; }
        public IRecipeTransferError transferRecipe(Container container,IRecipeLayout layout,EntityPlayer player,boolean max,boolean perform) {
            // Vanilla's client GuiCrafting uses BlockPos.ORIGIN, so only the server knows the real anchor.
            Boolean connected=connected(container);
            if(connected==null)return error("正在确认工作台的抽屉连接，请稍候");
            // Keep vanilla batch transfer behaviour when no drawer network is connected.
            if (!connected && fallback!=null) return fallback.transferRecipe(container,layout,player,max,perform);
            if (!player.inventory.getItemStack().isEmpty()) return error("请先放下鼠标上的物品");
            Map<Integer,? extends IGuiIngredient<ItemStack>> ingredients=layout.getItemStacks().getGuiIngredients();
            List<DrawerTransfer.Request> requests=new ArrayList<>();
            for (Map.Entry<Integer,Integer> pair:profile.slots(container).entrySet()) {
                IGuiIngredient<ItemStack> ingredient=ingredients.get(pair.getKey());
                List<ItemStack> choices=new ArrayList<>();boolean special=false;
                if (ingredient!=null && ingredient.isInput()) for (ItemStack stack:ingredient.getAllIngredients()) {
                    if (stack.isEmpty()) continue;
                    if (TransferProfiles.special(stack)) { special=true;continue; }
                    if (choices.size()>=128 || stack.getCount()>64) return error("该配方的材料选项过多，需手动放置");
                    choices.add(stack.copy());
                }
                // Excluded inputs stay exactly as the player placed them, even when changing recipes.
                if ((choices.isEmpty() && special) || TransferProfiles.special(container.getSlot(pair.getValue()).getStack())) continue;
                requests.add(new DrawerTransfer.Request(pair.getValue(),choices));
            }
            if (requests.isEmpty()) return error("该配方只有特殊输入，请手动放置");
            if (perform) TransferNetwork.fill(container.windowId,profile.category,requests);
            return null;
        }
    }
    private static IRecipeTransferError error(String message) {
        return new IRecipeTransferError() {
            public Type getType() { return Type.USER_FACING; }
            public void showError(Minecraft mc,int mouseX,int mouseY,IRecipeLayout layout,int recipeX,int recipeY) {
                net.minecraft.client.gui.GuiScreen screen=mc.currentScreen;
                if (screen!=null) screen.drawHoveringText(Collections.singletonList(message),mouseX,mouseY);
            }
        };
    }
}

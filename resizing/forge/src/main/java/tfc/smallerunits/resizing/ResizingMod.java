package tfc.smallerunits.resizing;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("smallerunits_resizing") // this is kinda dumb, but it has to be done
public class ResizingMod {
    public ResizingMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onSetup);
    }

    private void onSetup(FMLCommonSetupEvent event) {
        ResizingSetup.init();
    }
}

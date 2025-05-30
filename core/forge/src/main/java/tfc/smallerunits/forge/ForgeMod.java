package tfc.smallerunits.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import tfc.smallerunits.core.SmallerUnits;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod("smallerunits_core")
public class ForgeMod extends SmallerUnits {
	IEventBus forgeBus;
	IEventBus modBus;
	
	@Override
	public void prepare() {
		forgeBus = MinecraftForge.EVENT_BUS;
		modBus = FMLJavaModLoadingContext.get().getModEventBus();
	}
	
	public ForgeMod() {
		super();
	}
	
	@Override
	public void registerSetup(Runnable common) {
		modBus.addListener(
				EventPriority.NORMAL, false,
				FMLCommonSetupEvent.class,
				(ev) -> common.run()
		);
	}
	
	// do not ask.
	@Override
	public void registerTick(TickType type, Phase phase, Runnable tick) {
		switch (phase) {
			case START -> {
				switch (type) {
					case CLIENT -> forgeBus.addListener(
							EventPriority.NORMAL, false, TickEvent.ClientTickEvent.class,
							(event) -> {
								if (event.phase == TickEvent.Phase.START)
									tick.run();
							}
					);
					case SERVER -> forgeBus.addListener(
							EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class,
							(event) -> {
								if (event.phase == TickEvent.Phase.START)
									tick.run();
							}
					);
					case ALL -> forgeBus.addListener(
							EventPriority.NORMAL, false, TickEvent.class,
							(event) -> {
								if (event.phase == TickEvent.Phase.START)
									tick.run();
							}
					);
				}
			}
			case END -> {
				switch (type) {
					case CLIENT -> forgeBus.addListener(
							EventPriority.NORMAL, false, TickEvent.ClientTickEvent.class,
							(event) -> {
								if (event.phase == TickEvent.Phase.END)
									tick.run();
							}
					);
					case SERVER -> forgeBus.addListener(
							EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class,
							(event) -> {
								if (event.phase == TickEvent.Phase.END)
									tick.run();
							}
					);
					case ALL -> forgeBus.addListener(
							EventPriority.NORMAL, false, TickEvent.class,
							(event) -> {
								if (event.phase == TickEvent.Phase.END)
									tick.run();
							}
					);
				}
			}
			case ANY -> {
				switch (type) {
					case CLIENT -> forgeBus.addListener(
							EventPriority.NORMAL, false, TickEvent.ClientTickEvent.class,
							(event) -> tick.run()
					);
					case SERVER -> forgeBus.addListener(
							EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class,
							(event) -> tick.run()
					);
					case ALL -> forgeBus.addListener(
							EventPriority.NORMAL, false, TickEvent.class,
							(event) -> tick.run()
					);
				}
			}
		}
	}
	
	@Override
	public void registerAtlas(BiConsumer<ResourceLocation, Consumer<ResourceLocation>> onTextureStitch) {}
	
	@Override
	public void registerChunkStatus(BiConsumer<LevelAccessor, ChunkAccess> onChunkLoaded, BiConsumer<LevelAccessor, ChunkAccess> onChunkUnloaded) {
		forgeBus.addListener(
				EventPriority.NORMAL, false,
				ChunkEvent.Load.class,
				(ev) -> onChunkLoaded.accept(ev.getLevel(), ev.getChunk())
		);
		forgeBus.addListener(
				EventPriority.NORMAL, false,
				ChunkEvent.Unload.class,
				(ev) -> onChunkUnloaded.accept(ev.getLevel(), ev.getChunk())
		);
	}
	
	@Override
	public void registerAttachment() {
		forgeBus.addGenericListener(LevelChunk.class, CapabilityProvider::onAttachCapabilities);
	}
	
	@Override
	public void registerCapabilities() {
		modBus.addListener(CapabilityProvider::onRegisterCapabilities);
	}

	@Override
	public void onWorldUnload(Consumer<LevelAccessor> consumer) {
		forgeBus.addListener(
				EventPriority.NORMAL, true,
				LevelEvent.Unload.class,
				(evt) -> {
					consumer.accept(evt.getLevel());
				}
		);
	}
}

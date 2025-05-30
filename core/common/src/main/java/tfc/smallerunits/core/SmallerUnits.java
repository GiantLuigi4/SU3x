package tfc.smallerunits.core;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import tfc.smallerunits.core.client.access.tracking.SUVBOEmittingWorld;
import tfc.smallerunits.core.crafting.CraftingRegistry;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.tracking.RegionClosable;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;
import tfc.smallerunits.core.networking.SUNetworkRegistry;
import tfc.smallerunits.core.networking.hackery.InfoRegistry;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;
import tfc.smallerunits.core.networking.sync.SyncPacketS2C;
import tfc.smallerunits.core.simulation.chunk.BasicVerticalChunk;
import tfc.smallerunits.core.utils.config.ClientConfig;
import tfc.smallerunits.core.utils.config.CommonConfig;
import tfc.smallerunits.core.utils.config.ServerConfig;
import tfc.smallerunits.plat.util.PlatformProvider;

import java.util.ArrayDeque;
import java.util.function.Consumer;

public abstract class SmallerUnits extends AbstractMod {
    /**
     * Use {@link SmallerUnits#ABS_MIN} instead, as that won't get inlined by the compiler
     * Reason: this value may change in the future, so ideally mods would be able to react to that without updating
     */
    @Deprecated
	public static final int ABS_MIN_CONST = 256;

	// prevents inlining of constant
	private static int theMin() {
		return ABS_MIN_CONST;
	}

	public static final int ABS_MIN = theMin();

    public static float tesselScale = 0;
	private static boolean isVivecraftPresent;
	private static boolean isOFPresent;
	private static boolean isSodiumPresent =
			PlatformProvider.UTILS.isLoaded("sodium") ||
			PlatformProvider.UTILS.isLoaded("rubidium") ||
			PlatformProvider.UTILS.isLoaded("embeddium") ||
			PlatformProvider.UTILS.isLoaded("magnesium")
			;

	private static final boolean isImmPrtlPresent = PlatformProvider.UTILS.isLoaded("imm_ptl_core");

	public SmallerUnits() {
		prepare();

		SUNetworkRegistry.init();
		/* registries */
		Registry.BLOCK_REGISTER.register();
		Registry.ITEM_REGISTER.register();
		Registry.TAB_REGISTER.register();
		CraftingRegistry.RECIPES.register();
		/* mod loading events */
		registerCapabilities();
		registerAttachment();
		registerSetup(this::setup);
		/* in game events */
		if (PlatformProvider.UTILS.isClient()) registerTick(TickType.CLIENT, Phase.START, SyncPacketS2C::tick);

		registerChunkStatus(SmallerUnits::onChunkLoaded, SmallerUnits::onChunkUnloaded);

		if (PlatformProvider.UTILS.isClient()) {
			ClientConfig.init();
//			ClientCompatConfig.init();
		}
		CommonConfig.init();
		ServerConfig.init();

		InfoRegistry.register("su:world_redir", () -> {
			if (NetworkingHacks.unitPos.get() == null) return null;
			CompoundTag tg = new CompoundTag();
			NetworkingHacks.unitPos.get().write(tg);
			return tg;
		}, (tag, ctx) -> {
			CompoundTag tg = (CompoundTag) tag;
			NetworkingHacks.LevelDescriptor pos = NetworkingHacks.LevelDescriptor.read(tg);
			NetworkingHacks.setPosFor(ctx.pkt, pos);
			return null;
		}, (obj, ctx) -> {
		});

		if (PlatformProvider.UTILS.isClient()) {
			registerAtlas(SmallerUnits::onTextureStitch);
		}

		isVivecraftPresent = PlatformProvider.UTILS.isLoaded("vivecraft");
		try {
			Class<?> clazz = Class.forName("net.optifine.Config");
			if (clazz != null) {
				PlatformProvider.UTILS.startupWarning(ChatFormatting.YELLOW + "Smaller Units" + ChatFormatting.RESET + "\nSU and Optifine are " + ChatFormatting.RED + ChatFormatting.BOLD + "highly incompatible" + ChatFormatting.RESET + " with eachother.");
				isOFPresent = true;
			}
		} catch (Throwable ignored) {
		}

		registerTick(TickType.SERVER, Phase.END, SmallerUnits::onTick);

		onWorldUnload((level) -> {
			if (level instanceof RegionClosable closable) {
				closable.closeSURegions();
			}
			if (level instanceof SUVBOEmittingWorld emittingWorld) {
				emittingWorld.freeSUEmitter();
			}
		});
	}

	private static final ArrayDeque<Runnable> enqueued = new ArrayDeque<>();

	// this ended up being necessary, as without it, furnaces can end up deadlocing world loading
	private static void onTick() {
		synchronized (enqueued) {
			while (!enqueued.isEmpty()) {
				enqueued.poll().run();
			}
		}
	}

	private static void onChunkLoaded(LevelAccessor level, ChunkAccess chunk) {
		if (level instanceof ServerLevel lvl) {
			if (chunk instanceof LevelChunk lvlChk) {
				synchronized (enqueued) {
					enqueued.add(() -> SUCapabilityManager.onChunkLoad(lvlChk));
				}
			}

			if (lvl.getChunkSource().chunkMap instanceof RegionalAttachments attachments) {
				ChunkAccess access = chunk;
				int min = access.getMinBuildHeight();
				int max = access.getMaxBuildHeight();
				ChunkPos pos = access.getPos();
				for (int y = min; y < max; y += 16)
					attachments.SU$findChunk(y, pos, (rp, r) -> r.addRef(rp));
			}
		}
	}

	private static void onChunkUnloaded(LevelAccessor level, ChunkAccess chunk) {
		if (level instanceof ServerLevel lvl) {
			if (chunk instanceof LevelChunk lvlChk) {
				ISUCapability capability = SUCapabilityManager.getCapability(lvlChk);
				for (UnitSpace unit : capability.getUnits()) {
					for (BasicVerticalChunk bvc : unit.getChunks()) {
						bvc.maybeUnload();
					}
				}
			}

			if (lvl.getChunkSource().chunkMap instanceof RegionalAttachments attachments) {
				ChunkAccess access = chunk;
				int min = access.getMinBuildHeight();
				int max = access.getMaxBuildHeight();
				ChunkPos pos = access.getPos();
				for (int y = min; y < max; y += 16)
					attachments.SU$findChunk(y, pos, (rp, r) -> {
						if (r.subtractRef(rp) <= 0) {
							Region region = attachments.SU$getRegionMap().remove(rp);
							if (region != null) region.close();
						}
					});
			}
		}
	}

	public static boolean isImmersivePortalsPresent() {
		return isImmPrtlPresent;
	}

	private void setupCfg() {
		PlatformProvider.UTILS.delayConfigInit(null);
	}

	private void setup() {
		setupCfg();
	}

	public static boolean isVivecraftPresent() {
		return isVivecraftPresent;
	}

	public static boolean isIsOFPresent() {
		return isOFPresent;
	}

	public static boolean isSodiumPresent() {
		return isSodiumPresent;
	}

	private static void onTextureStitch(ResourceLocation atlas, Consumer<ResourceLocation> callback) {
		if (atlas.equals(TextureAtlas.LOCATION_BLOCKS)) {
			callback.accept(new ResourceLocation("smallerunits:block/white_pixel"));
		}
	}
}

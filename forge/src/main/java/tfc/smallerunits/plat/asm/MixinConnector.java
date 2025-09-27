package tfc.smallerunits.plat.asm;

import net.minecraftforge.coremod.api.ASMAPI;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import tfc.smallerunits.common.logging.Loggers;

import java.util.ArrayList;

public class MixinConnector extends BasePlugin {
	TargetParser targets = TargetParser.parse("/META-INF/connector_targets.txt", MixinConnector.class);

    public MixinConnector() {
        ArrayList<String> incompat = new ArrayList<>();
	    incompat.add("me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
	    incompatibilityMap.put("tfc.smallerunits.core.mixin.LevelRendererMixinBlocks", incompat);
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.equals("tfc.smallerunits.core.mixin.LevelRendererMixin")) {
	        // renderChunkLayer
	        TargetReference reference;
	        reference = targets.getReference("renderChunkLayer");
            String target = ASMAPI.mapMethod(reference.getPropertyName());
            String desc = reference.getDescriptor();
	        
	        // getCompiledChunk
	        reference = targets.getReference("getCompiledChunk");
            String refOwner = reference.getClassName();
            String ref = ASMAPI.mapMethod(reference.getPropertyName());
            String refDesc = reference.getDescriptor();
	        
	        boolean located = false;
	        boolean targetLocated = false;
            for (MethodNode method : targetClass.methods) {
                if (method.name.equals(target) && method.desc.equals(desc)) {
	                located = true;
					
                    // TODO: try to find a way to figure out a more specific target
                    ArrayList<AbstractInsnNode> targetNodes = new ArrayList<>();
                    for (AbstractInsnNode instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode methodNode) {
                            if (methodNode.owner.equals(refOwner) && methodNode.name.equals(ref) && methodNode.desc.equals(refDesc)) {
                                targetNodes.add(methodNode);
	                            targetLocated = true;
                            }
                        }
                    }
                    for (AbstractInsnNode targetNode : targetNodes) {
                        InsnList list = new InsnList();
                        list.add(ASMAPI.buildMethodCall(
		                        "tfc/smallerunits/core/utils/RenderChunkSetter",
                                "updateRenderChunk",
                                "(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;)Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;", ASMAPI.MethodType.STATIC)
                        );
                        method.instructions.insertBefore(targetNode, list);
                    }
                }
            }
	        
	        if (!located) {
		        Loggers.SU_LOGGER.error("Could not locate injection target!");
	        } else if (!targetLocated) {
		        Loggers.SU_LOGGER.error("Could not locate point target!");
	        }
        }
    }
}

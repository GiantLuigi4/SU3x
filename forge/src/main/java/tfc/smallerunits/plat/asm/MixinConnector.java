package tfc.smallerunits.plat.asm;

import net.minecraftforge.coremod.api.ASMAPI;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;

public class MixinConnector extends BasePlugin {
    public MixinConnector() {
        ArrayList<String> incompat = new ArrayList<>();
        incompat.add("me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
        incompatibilityMap.put("tfc.smallerunits.core.mixin.LevelRendererMixinBlocks", incompat);
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.equals("tfc.smallerunits.core.mixin.LevelRendererMixin")) {
            String target = ASMAPI.mapMethod("m_172993_"); // renderChunkLayer
            String desc = "(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V"; // TODO: I'd like to not assume Mojmap

            String refOwner = "net/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk";
            String ref = ASMAPI.mapMethod("m_112835_"); // getCompiledChunk
            String refDesc = "()Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$CompiledChunk;";
            for (MethodNode method : targetClass.methods) {
                if (method.name.equals(target) && method.desc.equals(desc)) {
//					AbstractInsnNode targetNode = null;
                    // TODO: try to find a way to figure out a more specific target
                    ArrayList<AbstractInsnNode> targetNodes = new ArrayList<>();
                    for (AbstractInsnNode instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode methodNode) {
                            if (methodNode.owner.equals(refOwner) && methodNode.name.equals(ref) && methodNode.desc.equals(refDesc)) {
                                targetNodes.add(methodNode);
//								targetNode = methodNode;
//								break;
                            }
                        }
                    }
//					if (targetNode != null) {
                    for (AbstractInsnNode targetNode : targetNodes) {
                        InsnList list = new InsnList();
                        list.add(ASMAPI.buildMethodCall(
                                "tfc/smallerunits/core/utils/IHateTheDistCleaner",
                                "updateRenderChunk",
                                "(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;)Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;", ASMAPI.MethodType.STATIC)
                        );
                        method.instructions.insertBefore(targetNode, list);
                    }
//					}
                }
            }
        }
    }
}

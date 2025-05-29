package tfc.smallerunits.plat.asm;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class BasePlugin implements IMixinConfigPlugin {
    protected final ArrayList<String> classLookup = new ArrayList<>();
    protected final ArrayList<String> pkgLookup = new ArrayList<>();
    protected final HashMap<String, ArrayList<String>> incompatibilityMap = new HashMap<>();
    protected final HashMap<String, String> dependencies = new HashMap<>();

    public BasePlugin() {
        System.out.println("Mixin plugin loaded: " + getClass().getName());
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    public boolean doesPkgNeedLookup(String name) {
        for (String s : pkgLookup) {
            if (name.startsWith(s)) return true;
        }
        return false;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (dependencies.containsKey(mixinClassName)) {
            ClassLoader loader = BasePlugin.class.getClassLoader();
            // tests if the classloader contains a .class file for the target
            InputStream stream = loader.getResourceAsStream(dependencies.get(mixinClassName).replace('.', '/') + ".class");
            if (stream != null) {
                try {
                    stream.close();
                    return true;
                } catch (Throwable ignored) {
                }
            } else {
                System.out.println("Skipping " + targetClassName + ": missing dependency.");
                return false;
            }
        }

        if (incompatibilityMap.containsKey(mixinClassName)) {
            ClassLoader loader = BasePlugin.class.getClassLoader();
            // tests if the classloader contains a .class file for the target
            for (String name : incompatibilityMap.get(mixinClassName)) {
                InputStream stream = loader.getResourceAsStream(name.replace('.', '/') + ".class");
                if (stream == null) {
                } else {
                    try {
                        stream.close();
                        System.out.println("Skipping " + targetClassName + ": has incompatibility.");
                        return false;
                    } catch (Throwable ignored) {
                        System.out.println("Skipping " + targetClassName + ": has incompatibility.");
                        return false;
                    }
                }
            }
        }

        if (classLookup.contains(mixinClassName) || doesPkgNeedLookup(mixinClassName)) {
            ClassLoader loader = BasePlugin.class.getClassLoader();
            // tests if the classloader contains a .class file for the target
            InputStream stream = loader.getResourceAsStream(targetClassName.replace('.', '/') + ".class");
            if (stream != null) {
                try {
                    stream.close();
                    return true;
                } catch (Throwable ignored) {
                    return true;
                }
            }
            System.out.println("Skipping " + targetClassName + ": missed lookup.");
            return false;
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
//		if (
//				mixinClassName.equals("tfc.smallerunits.core.mixin.LevelRendererMixinBlocks") ||
//						mixinClassName.equals("tfc.smallerunits.core.mixin.core.gui.client.expansion.DebugScreenOverlayMixin")
//		) {
//			try {
//				FileOutputStream outputStream = new FileOutputStream(targetClass.name.substring(targetClass.name.lastIndexOf("/") + 1) + "-pre.class");
//				ClassWriter writer = new ClassWriter(0);
//				targetClass.accept(writer);
//				outputStream.write(writer.toByteArray());
//				outputStream.flush();
//				outputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
//		if (
//				mixinClassName.equals("tfc.smallerunits.core.mixin.LevelRendererMixin") ||
//						mixinClassName.equals("tfc.smallerunits.core.mixin.core.gui.client.expansion.DebugScreenOverlayMixin")
//		) {
//			try {
//				FileOutputStream outputStream = new FileOutputStream(targetClass.name.substring(targetClass.name.lastIndexOf("/") + 1) + "-post.class");
//				ClassWriter writer = new ClassWriter(0);
//				targetClass.accept(writer);
//				outputStream.write(writer.toByteArray());
//				outputStream.flush();
//				outputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
    }
}

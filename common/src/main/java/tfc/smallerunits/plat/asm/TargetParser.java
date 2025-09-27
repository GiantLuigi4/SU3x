package tfc.smallerunits.plat.asm;

import tfc.smallerunits.common.logging.Loggers;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TargetParser {
	Map<String, TargetReference> references = new HashMap<>();
	
	public TargetParser(String text) {
		for (String s : text.split("\n")) {
			String[] lr = s.split(":", 2);
			String[] mr = lr[1].split("->", 2);
			switch (mr[0].trim().charAt(0)) {
				case 'm' -> {
					String[] ct = mr[1].trim().split(" ", 2);
					String[] td = ct[1].trim().split("\\(", 2);
					references.put(
							lr[0].trim(),
							new TargetReference().method(
									ct[0],
									td[0],
									"(" + td[1]
							)
					);
				}
				case 'f' -> {
					String[] ct = mr[1].trim().split(" ", 2);
					String[] td = ct[1].trim().split("#", 2);
					references.put(
							lr[0].trim(),
							new TargetReference().field(
									ct[0],
									td[0],
									td[1]
							)
					);
				}
				case 'c' -> {
					references.put(
							lr[0].trim(),
							new TargetReference().clazz(mr[1].trim())
					);
				}
				default -> throw new RuntimeException("Unrecognized property type: " + mr[0].trim());
			}
		}
	}
	
	public static TargetParser parse(String pth, Class<?> clz) {
		try {
			InputStream is = clz.getResourceAsStream(pth);
			if (is == null) {
				Loggers.SU_LOGGER.warn("Failed to find " + pth + " in jar, falling back to class loader.");
				is = clz.getClassLoader().getResourceAsStream(pth);
			}
			String str = new String(is.readAllBytes());
			try {
				is.close();
			} catch (Throwable ignored) {
			}
			return new TargetParser(str);
		} catch (Throwable err) {
			throw new RuntimeException(err);
		}
	}
	
	public TargetReference getReference(String name) {
		return references.get(name);
	}
}

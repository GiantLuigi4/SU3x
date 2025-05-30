package tfc.smallerunits.plat.config.annoconfg.annotation.value;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface IntRange {
	int minV();
	int maxV();
}

package tfc.smallerunits.plat.config.annoconfg.annotation.format;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CFGSegment {
	String value();
}

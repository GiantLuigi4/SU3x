package tfc.smallerunits.plat.asm;

public class TargetReference {
	TargetType type;
	String className;
	String propertyName;
	String descriptor;
	
	public TargetReference method(String className, String propertyName, String descriptor) {
		type = TargetType.METHOD;
		
		this.className = className;
		this.propertyName = propertyName;
		this.descriptor = descriptor;
		
		return this;
	}
	
	public TargetReference field(String className, String propertyName, String descriptor) {
		type = TargetType.FIELD;
		
		this.className = className;
		this.propertyName = propertyName;
		this.descriptor = descriptor;
		
		return this;
	}
	
	public TargetReference clazz(String className) {
		type = TargetType.CLASS;
		
		this.className = className;
		
		return this;
	}
	
	public TargetType getType() {
		return type;
	}
	
	public String getClassName() {
		return className;
	}
	
	public String getPropertyName() {
		return propertyName;
	}
	
	public String getDescriptor() {
		return descriptor;
	}
	
	public enum TargetType {
		CLASS,
		METHOD,
		FIELD
	}
}

package myau.module;

public class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(String name, boolean defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public boolean getValue() { return value; }
    public void setValue(boolean v) { value = v; }
    public void toggle() { value = !value; }
}

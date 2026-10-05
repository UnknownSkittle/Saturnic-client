package namidevelopment.kiriyaga.api.model.setting;

import com.google.gson.JsonElement;
import namidevelopment.kiriyaga.api.model.feature.Feature;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String identifier;
    private String name;
    protected T value;
    private Runnable onChanged = null;
    private boolean show = true;
    private Feature parentFeature;

    private BooleanSupplier showCondition = null;

    public Setting(String identifier, String name, T defaultValue) {
        this.identifier = identifier;
        this.name = name;
        this.value = defaultValue;
    }

    public Setting(String name, T defaultValue) {
        this(name, name, defaultValue);
    }

    public String getName() {
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
        notifyChanged();
    }

    protected final void notifyChanged() {
        if (onChanged != null) onChanged.run();
    }

    public void setOnChanged(Runnable callback) {
        this.onChanged = callback;
    }

    public boolean isShow() {
        if (showCondition != null) {
            return showCondition.getAsBoolean();
        }
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public void setShowCondition(BooleanSupplier condition) {
        this.showCondition = condition;
    }

    public void setParentFeature(Feature Feature) {
        this.parentFeature = Feature;
    }

    public Feature getParentFeature() {
        return parentFeature;
    }

    public String getIdentifier() {
        return identifier;
    }

    public abstract void fromJson(JsonElement json);
    public abstract JsonElement toJson();
}
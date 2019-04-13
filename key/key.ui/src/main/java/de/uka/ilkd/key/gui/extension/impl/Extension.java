package de.uka.ilkd.key.gui.extension.impl;

import de.uka.ilkd.key.gui.extension.api.KeYGuiExtension;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * @author Alexander Weigl
 * @version 1 (07.04.19)
 */
public class Extension<T> implements Comparable<Extension> {
    private final Class<T> clazz;
    private final KeYGuiExtension.Info info;
    private T instance = null;

    public Extension(Class<T> clazz) {
        this.clazz = clazz;
        this.info = clazz.getAnnotation(KeYGuiExtension.Info.class);
    }

    public T getInstance() {
        if (instance == null) {
            try {
                instance = clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public String getName() {
        return info == null ? getType().getName() : info.name();
    }

    public boolean isOptional() {
        return info != null && info.optional();
    }

    public int getPriority() {
        return info == null ? 0 : info.priority();
    }

    public boolean isDisabled() {
        return info != null && info.disabled();
    }

    public Class<T> getType() {
        return clazz;
    }

    @Override
    public int compareTo(Extension o) {
        return Integer.compare(getPriority(), o.getPriority());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Extension)) return false;
        Extension<?> extension = (Extension<?>) o;
        return clazz.equals(extension.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz);
    }

    public String getDescription() {
        return info == null ? "" : info.description();
    }

    public boolean supports(Class<?> c) {
        return c.isAssignableFrom(getType());
    }

    public boolean supportsSettings() {
        return supports(KeYGuiExtension.Settings.class);
    }

    public boolean supportsLeftPanel() {
        return supports(KeYGuiExtension.LeftPanel.class);
    }

    public boolean supportsContextMenu() {
        return supports(KeYGuiExtension.ContextMenu.class);
    }

    public boolean supportsMainMenu() {
        return supports(KeYGuiExtension.MainMenu.class);
    }

    public boolean supportsStatusLine() {
        return supports(KeYGuiExtension.StatusLine.class);
    }


    public boolean supportsToolbar() {
        return supports(KeYGuiExtension.Toolbar.class);
    }
}

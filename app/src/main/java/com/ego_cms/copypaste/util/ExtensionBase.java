package com.ego_cms.copypaste.util;


import org.jetbrains.annotations.NotNull;

public class ExtensionBase<T> {

    private final T instanceExtended;


    public ExtensionBase(@NotNull T instanceExtended) {
        this.instanceExtended = instanceExtended;
    }


	@NotNull
    public T getInstanceExtended() {
        return this.instanceExtended;
    }

}

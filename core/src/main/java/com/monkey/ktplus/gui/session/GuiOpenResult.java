package com.monkey.ktplus.gui.session;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import com.monkey.ktplus.gui.window.GuiWindowHandle;

public final class GuiOpenResult {
    private final boolean opened;
    private final @Nullable Throwable failure;
    private final @Nullable GuiWindowHandle windowHandle;

    private GuiOpenResult(boolean opened, @Nullable Throwable failure, @Nullable GuiWindowHandle windowHandle) {
        this.opened = opened;
        this.failure = failure;
        this.windowHandle = windowHandle;
    }

    public static GuiOpenResult success() {
        return new GuiOpenResult(true, null, null);
    }

    public static GuiOpenResult success(@Nullable GuiWindowHandle windowHandle) {
        return new GuiOpenResult(true, null, windowHandle);
    }

    public static GuiOpenResult failure(Throwable failure) {
        return new GuiOpenResult(false, Objects.requireNonNull(failure, "failure"), null);
    }

    public boolean opened() {
        return opened;
    }

    public @Nullable Throwable failure() {
        return failure;
    }

    public Optional<GuiWindowHandle> windowHandle() {
        return Optional.ofNullable(windowHandle);
    }
}

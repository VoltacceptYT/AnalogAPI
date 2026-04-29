package com.voltaccept.AnalogAPI.controller;

public final class ButtonBinding {
    public final ControllerButton button;
    public final ControllerAction action;
    public final float threshold;
    public final boolean isAnalog;

    public ButtonBinding(ControllerButton button, ControllerAction action, float threshold, boolean isAnalog) {
        this.button = button;
        this.action = action;
        this.threshold = threshold;
        this.isAnalog = isAnalog;
    }

    public ButtonBinding(ControllerButton button, ControllerAction action) {
        this(button, action, 0.5f, false);
    }

    public ButtonBinding(ControllerButton button, ControllerAction action, float threshold) {
        this(button, action, threshold, true);
    }

    public String toJson() {
        return "{"
            + "\"button\":\"" + button.getId() + "\""
            + ",\"action\":\"" + action.getId() + "\""
            + ",\"threshold\":" + threshold
            + ",\"analog\":" + isAnalog
            + "}";
    }

    @Override
    public String toString() {
        return button.getDisplayName() + " -> " + action.getDisplayName();
    }
}

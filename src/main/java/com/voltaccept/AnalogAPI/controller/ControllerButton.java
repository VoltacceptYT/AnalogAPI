package com.voltaccept.AnalogAPI.controller;

public enum ControllerButton {
    A_BUTTON("A"),
    B_BUTTON("B"), 
    X_BUTTON("X"),
    Y_BUTTON("Y"),
    LEFT_BUMPER("LB"),
    RIGHT_BUMPER("RB"),
    LEFT_TRIGGER("LT"),
    RIGHT_TRIGGER("RT"),
    BACK_BUTTON("Back"),
    START_BUTTON("Start"),
    LEFT_STICK_CLICK("LS"),
    RIGHT_STICK_CLICK("RS"),
    DPAD_UP("DPad Up"),
    DPAD_DOWN("DPad Down"),
    DPAD_LEFT("DPad Left"),
    DPAD_RIGHT("DPad Right");

    private final String displayName;

    ControllerButton(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return name().toLowerCase();
    }
}

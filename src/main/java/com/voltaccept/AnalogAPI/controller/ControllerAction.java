package com.voltaccept.AnalogAPI.controller;

public enum ControllerAction {
    FORWARD("Forward"),
    BACKWARD("Backward"),
    LEFT("Left"),
    RIGHT("Right"),
    JUMP("Jump"),
    SNEAK("Sneak"),
    SPRINT("Sprint"),
    ATTACK("Attack"),
    USE("Use"),
    INVENTORY("Inventory"),
    DROP_ITEM("Drop Item"),
    CHAT("Chat"),
    PAUSE("Pause");

    private final String displayName;

    ControllerAction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return name().toLowerCase();
    }
}

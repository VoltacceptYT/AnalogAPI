package com.voltaccept.AnalogAPI.controller;

public final class ControllerInput {
    public final float leftStickX;
    public final float leftStickY;
    public final float rightStickX;
    public final float rightStickY;
    public final float leftTrigger;
    public final float rightTrigger;
    public final boolean[] buttonStates;

    public ControllerInput(float leftStickX, float leftStickY, float rightStickX, float rightStickY,
                         float leftTrigger, float rightTrigger, boolean[] buttonStates) {
        this.leftStickX = clamp(leftStickX);
        this.leftStickY = clamp(leftStickY);
        this.rightStickX = clamp(rightStickX);
        this.rightStickY = clamp(rightStickY);
        this.leftTrigger = clamp(leftTrigger);
        this.rightTrigger = clamp(rightTrigger);
        this.buttonStates = buttonStates.clone();
    }

    private static float clamp(float value) {
        return Math.max(-1.0f, Math.min(1.0f, value));
    }

    public boolean isButtonPressed(ControllerButton button) {
        int index = button.ordinal();
        return index < buttonStates.length && buttonStates[index];
    }

    public float getAnalogValue(ControllerButton button) {
        return switch (button) {
            case LEFT_TRIGGER -> leftTrigger;
            case RIGHT_TRIGGER -> rightTrigger;
            default -> isButtonPressed(button) ? 1.0f : 0.0f;
        };
    }

    public boolean isEmpty() {
        for (boolean pressed : buttonStates) {
            if (pressed) return false;
        }
        return Math.abs(leftStickX) < 0.01f && Math.abs(leftStickY) < 0.01f &&
               Math.abs(rightStickX) < 0.01f && Math.abs(rightStickY) < 0.01f &&
               leftTrigger < 0.01f && rightTrigger < 0.01f;
    }

    public static ControllerInput empty() {
        return new ControllerInput(0f, 0f, 0f, 0f, 0f, 0f, new boolean[ControllerButton.values().length]);
    }
}

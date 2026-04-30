package com.voltaccept.AnalogAPI.controller;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ControllerConfig {
    private final Map<ControllerButton, ButtonBinding> buttonBindings;
    private float leftStickDeadzone = 0.15f;
    private float rightStickDeadzone = 0.15f;
    private float triggerThreshold = 0.1f;
    private boolean invertYAxis = false;
    private float stickSensitivity = 1.0f;

    public ControllerConfig() {
        this.buttonBindings = new ConcurrentHashMap<>();
        setupDefaultBindings();
    }

    private void setupDefaultBindings() {
        // Standard Xbox controller layout
        bind(ControllerButton.A_BUTTON, ControllerAction.JUMP);
        bind(ControllerButton.B_BUTTON, ControllerAction.SNEAK);
        bind(ControllerButton.X_BUTTON, ControllerAction.ATTACK);
        bind(ControllerButton.Y_BUTTON, ControllerAction.INVENTORY);
        bind(ControllerButton.LEFT_BUMPER, ControllerAction.DROP_ITEM);
        bind(ControllerButton.RIGHT_BUMPER, ControllerAction.SPRINT);
        bind(ControllerButton.LEFT_TRIGGER, ControllerAction.USE, 0.1f);
        bind(ControllerButton.RIGHT_TRIGGER, ControllerAction.ATTACK, 0.1f);
        bind(ControllerButton.START_BUTTON, ControllerAction.PAUSE);
        bind(ControllerButton.BACK_BUTTON, ControllerAction.INVENTORY);
    }

    public void bind(ControllerButton button, ControllerAction action) {
        buttonBindings.put(button, new ButtonBinding(button, action));
    }

    public void bind(ControllerButton button, ControllerAction action, float threshold) {
        buttonBindings.put(button, new ButtonBinding(button, action, threshold, true));
    }

    public void unbind(ControllerButton button) {
        buttonBindings.remove(button);
    }

    public ButtonBinding getBinding(ControllerButton button) {
        return buttonBindings.get(button);
    }

    public Map<ControllerButton, ButtonBinding> getAllBindings() {
        return new HashMap<>(buttonBindings);
    }

    public void setBindings(Map<ControllerButton, ButtonBinding> bindings) {
        buttonBindings.clear();
        buttonBindings.putAll(bindings);
    }

    // Getters and setters for configuration
    public float getLeftStickDeadzone() { return leftStickDeadzone; }
    public void setLeftStickDeadzone(float leftStickDeadzone) { this.leftStickDeadzone = Math.max(0, Math.min(1, leftStickDeadzone)); }

    public float getRightStickDeadzone() { return rightStickDeadzone; }
    public void setRightStickDeadzone(float rightStickDeadzone) { this.rightStickDeadzone = Math.max(0, Math.min(1, rightStickDeadzone)); }

    public float getTriggerThreshold() { return triggerThreshold; }
    public void setTriggerThreshold(float triggerThreshold) { this.triggerThreshold = Math.max(0, Math.min(1, triggerThreshold)); }

    public boolean isInvertYAxis() { return invertYAxis; }
    public void setInvertYAxis(boolean invertYAxis) { this.invertYAxis = invertYAxis; }

    public float getStickSensitivity() { return stickSensitivity; }
    public void setStickSensitivity(float stickSensitivity) { this.stickSensitivity = Math.max(0.1f, Math.min(3.0f, stickSensitivity)); }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"leftStickDeadzone\":").append(leftStickDeadzone).append(",");
        sb.append("\"rightStickDeadzone\":").append(rightStickDeadzone).append(",");
        sb.append("\"triggerThreshold\":").append(triggerThreshold).append(",");
        sb.append("\"invertYAxis\":").append(invertYAxis).append(",");
        sb.append("\"stickSensitivity\":").append(stickSensitivity).append(",");
        sb.append("\"bindings\":[");
        
        boolean first = true;
        for (ButtonBinding binding : buttonBindings.values()) {
            if (!first) sb.append(",");
            sb.append(binding.toJson());
            first = false;
        }
        
        sb.append("]}");
        return sb.toString();
    }
    
    /**
     * Load configuration from JSON string.
     */
    public static ControllerConfig fromJson(String json) {
        ControllerConfig config = new ControllerConfig();
        
        // Simple JSON parsing for basic settings
        // In a real implementation, you'd use a proper JSON library
        if (json.contains("\"leftStickDeadzone\"")) {
            int start = json.indexOf("\"leftStickDeadzone\":") + 19;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            try {
                config.setLeftStickDeadzone(Float.parseFloat(json.substring(start, end)));
            } catch (Exception ignored) {}
        }
        
        if (json.contains("\"rightStickDeadzone\"")) {
            int start = json.indexOf("\"rightStickDeadzone\":") + 20;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            try {
                config.setRightStickDeadzone(Float.parseFloat(json.substring(start, end)));
            } catch (Exception ignored) {}
        }
        
        if (json.contains("\"triggerThreshold\"")) {
            int start = json.indexOf("\"triggerThreshold\":") + 18;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            try {
                config.setTriggerThreshold(Float.parseFloat(json.substring(start, end)));
            } catch (Exception ignored) {}
        }
        
        if (json.contains("\"invertYAxis\"")) {
            config.setInvertYAxis(json.contains("\"invertYAxis\":true"));
        }
        
        if (json.contains("\"stickSensitivity\"")) {
            int start = json.indexOf("\"stickSensitivity\":") + 19;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            try {
                config.setStickSensitivity(Float.parseFloat(json.substring(start, end)));
            } catch (Exception ignored) {}
        }
        
        return config;
    }
    
    /**
     * Reset to default configuration.
     */
    public void resetToDefaults() {
        buttonBindings.clear();
        setupDefaultBindings();
        leftStickDeadzone = 0.15f;
        rightStickDeadzone = 0.15f;
        triggerThreshold = 0.1f;
        invertYAxis = false;
        stickSensitivity = 1.0f;
    }
    
    /**
     * Validate configuration values and clamp them to acceptable ranges.
     */
    public void validateAndClamp() {
        leftStickDeadzone = Math.max(0f, Math.min(1f, leftStickDeadzone));
        rightStickDeadzone = Math.max(0f, Math.min(1f, rightStickDeadzone));
        triggerThreshold = Math.max(0f, Math.min(1f, triggerThreshold));
        stickSensitivity = Math.max(0.1f, Math.min(3f, stickSensitivity));
    }
    
    /**
     * Get a summary of the current configuration.
     */
    public String getSummary() {
        return String.format("ControllerConfig: %d bindings, deadzones=[%.2f,%.2f], " +
                           "threshold=%.2f, invertY=%b, sensitivity=%.1f",
                           buttonBindings.size(), leftStickDeadzone, rightStickDeadzone,
                           triggerThreshold, invertYAxis, stickSensitivity);
    }
}

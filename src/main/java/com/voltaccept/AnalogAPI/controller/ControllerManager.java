package com.voltaccept.AnalogAPI.controller;

import com.voltaccept.AnalogAPI.api.AnalogAPI;
import com.voltaccept.AnalogAPI.api.MovementState;
import com.voltaccept.AnalogAPI.api.VirtualInput;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class ControllerManager {
    
    public static class MovementResult {
        public final float forward;
        public final float sideways;
        public final boolean jump;
        public final boolean sneak;
        public final boolean sprint;

        public MovementResult(float forward, float sideways, boolean jump, boolean sneak, boolean sprint) {
            this.forward = forward;
            this.sideways = sideways;
            this.jump = jump;
            this.sneak = sneak;
            this.sprint = sprint;
        }
    }
    private static final Logger LOGGER = LoggerFactory.getLogger("AnalogAPI");
    private static final ControllerManager INSTANCE = new ControllerManager();

    private final ControllerConfig config;
    private final AtomicReference<ControllerInput> currentInput = new AtomicReference<>(ControllerInput.empty());
    private final AtomicReference<VirtualInput> controllerVirtualInput = new AtomicReference<>(null);
    private volatile boolean controllerConnected = false;
    private volatile boolean controllerInputEnabled = true;

    private ControllerManager() {
        this.config = new ControllerConfig();
    }

    public static ControllerManager getInstance() {
        return INSTANCE;
    }

    public ControllerConfig getConfig() {
        return config;
    }

    public boolean isControllerConnected() {
        return controllerConnected;
    }

    public void setControllerConnected(boolean connected) {
        this.controllerConnected = connected;
    }

    public boolean isControllerInputEnabled() {
        return controllerInputEnabled;
    }

    public void setControllerInputEnabled(boolean enabled) {
        this.controllerInputEnabled = enabled;
        if (!enabled) {
            clearControllerInput();
        }
    }

    public void updateControllerInput(ControllerInput input) {
        if (!controllerInputEnabled || !controllerConnected) {
            clearControllerInput();
            return;
        }

        currentInput.set(input);
        processControllerInput(input);
    }

    private void processControllerInput(ControllerInput input) {
        float forward = 0f;
        float sideways = 0f;
        boolean jump = false;
        boolean sneak = false;
        boolean sprint = false;

        // Apply deadzones and sensitivity
        float leftX = applyDeadzone(input.leftStickX, config.getLeftStickDeadzone());
        float leftY = applyDeadzone(input.leftStickY, config.getLeftStickDeadzone());
        
        // Map analog stick to movement (invert both axes for standard controller behavior)
        forward = -leftY * config.getStickSensitivity();
        sideways = leftX * config.getStickSensitivity();

        // Process button bindings
        for (ButtonBinding binding : config.getAllBindings().values()) {
            if (binding.isAnalog) {
                float value = input.getAnalogValue(binding.button);
                if (value >= binding.threshold) {
                    MovementResult result = mapActionToMovement(binding.action, value, forward, sideways, jump, sneak, sprint);
                    forward = result.forward;
                    sideways = result.sideways;
                    jump = result.jump;
                    sneak = result.sneak;
                    sprint = result.sprint;
                }
            } else if (input.isButtonPressed(binding.button)) {
                MovementResult result = mapActionToMovement(binding.action, 1.0f, forward, sideways, jump, sneak, sprint);
                forward = result.forward;
                sideways = result.sideways;
                jump = result.jump;
                sneak = result.sneak;
                sprint = result.sprint;
            }
        }

        // Create virtual input from controller
        VirtualInput virtualInput = new VirtualInput(
            clamp(forward), clamp(sideways), jump, sneak, sprint
        );
        
        controllerVirtualInput.set(virtualInput);
        
        // Apply to AnalogAPI system
        AnalogAPI.getInstance().setVirtualInput(virtualInput);
    }

    private MovementResult mapActionToMovement(ControllerAction action, float value,
                                    float forward, float sideways, boolean jump, boolean sneak, boolean sprint) {
        return switch (action) {
            case FORWARD -> new MovementResult(Math.max(forward, value), sideways, jump, sneak, sprint);
            case BACKWARD -> new MovementResult(Math.min(forward, -value), sideways, jump, sneak, sprint);
            case LEFT -> new MovementResult(forward, Math.min(sideways, -value), jump, sneak, sprint);
            case RIGHT -> new MovementResult(forward, Math.max(sideways, value), jump, sneak, sprint);
            case JUMP -> new MovementResult(forward, sideways, true, sneak, sprint);
            case SNEAK -> new MovementResult(forward, sideways, jump, true, sprint);
            case SPRINT -> new MovementResult(forward, sideways, jump, sneak, true);
            case CHANGE_PERSPECTIVE -> new MovementResult(forward, sideways, jump, sneak, sprint); // No movement change
            default -> new MovementResult(forward, sideways, jump, sneak, sprint);
        };
    }

    private float applyDeadzone(float value, float deadzone) {
        if (Math.abs(value) < deadzone) return 0f;
        return (value - Math.signum(value) * deadzone) / (1f - deadzone);
    }

    private float clamp(float value) {
        return Math.max(-1.0f, Math.min(1.0f, value));
    }

    public void clearControllerInput() {
        controllerVirtualInput.set(null);
        currentInput.set(ControllerInput.empty());
        
        // Only clear virtual input if it was from controller
        if (AnalogAPI.getInstance().getVirtualInput() == controllerVirtualInput.get()) {
            AnalogAPI.getInstance().clearVirtualInput();
        }
    }

    public ControllerInput getCurrentInput() {
        return currentInput.get();
    }

    public boolean isControllerDriving() {
        return controllerVirtualInput.get() != null;
    }
    
    /**
     * Get detailed controller status information.
     */
    public String getControllerStatus() {
        if (!controllerConnected) {
            return "No controller connected";
        }
        
        ControllerInput input = currentInput.get();
        String drivingStatus = isControllerDriving() ? "driving" : "idle";
        String enabledStatus = controllerInputEnabled ? "enabled" : "disabled";
        
        return String.format("Controller %s, input %s, currently %s", 
                           enabledStatus, drivingStatus, 
                           input != null && !input.isEmpty() ? "active" : "idle");
    }
    
        
    /**
     * Get controller input statistics.
     */
    public String getInputStats() {
        ControllerInput input = currentInput.get();
        if (input == null) {
            return "No input data";
        }
        
        int activeButtons = 0;
        for (boolean pressed : input.buttonStates) {
            if (pressed) activeButtons++;
        }
        
        return String.format("Buttons: %d active, Sticks: L[%.2f,%.2f] R[%.2f,%.2f], " +
                           "Triggers: L[%.2f] R[%.2f]",
                           activeButtons, input.leftStickX, input.leftStickY,
                           input.rightStickX, input.rightStickY,
                           input.leftTrigger, input.rightTrigger);
    }
}

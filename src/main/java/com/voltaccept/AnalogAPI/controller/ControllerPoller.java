package com.voltaccept.AnalogAPI.controller;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Real controller polling system using GLFW for gamepad support.
 * Detects and reads input from connected controllers.
 */
public final class ControllerPoller {
    private static final Logger LOGGER = LoggerFactory.getLogger("AnalogAPI");
    private static final ControllerPoller INSTANCE = new ControllerPoller();
    
    private static final int GLFW_JOYSTICK_1 = 0;
    private static final int GLFW_JOYSTICK_2 = 1;
    private static final int GLFW_JOYSTICK_3 = 2;
    private static final int GLFW_JOYSTICK_4 = 3;
    
    private volatile boolean running = false;
    private Thread pollThread;
    private int activeController = -1;
    
    private ControllerPoller() {}
    
    public static ControllerPoller getInstance() {
        return INSTANCE;
    }
    
    public void start() {
        if (running) return;
        
        running = true;
        pollThread = new Thread(this::pollLoop, "ControllerPoller");
        pollThread.setDaemon(true);
        pollThread.start();
        
        LOGGER.info("Controller polling started");
    }
    
    public void stop() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
            try {
                pollThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("Controller polling stopped");
    }
    
    private void pollLoop() {
        ControllerManager manager = ControllerManager.getInstance();
        
        while (running) {
            try {
                // Check for connected controllers
                int newActiveController = findConnectedController();
                
                if (newActiveController != activeController) {
                    if (activeController != -1) {
                        LOGGER.info("Controller disconnected from slot {}", activeController);
                    }
                    activeController = newActiveController;
                    if (activeController != -1) {
                        String name = GLFW.glfwGetJoystickName(activeController);
                        LOGGER.info("Controller connected: {} in slot {}", name, activeController);
                    }
                    manager.setControllerConnected(activeController != -1);
                }
                
                if (activeController != -1) {
                    // Read actual controller input
                    ControllerInput input = readControllerInput(activeController);
                    if (input != null) {
                        manager.updateControllerInput(input);
                    }
                }
                
                Thread.sleep(16); // ~60 FPS polling rate
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Only log errors that aren't just temporary controller issues
                if (!(e instanceof IllegalStateException) || !e.getMessage().contains("Joystick")) {
                    LOGGER.error("Error in controller polling loop", e);
                }
            }
        }
    }
    
    /**
     * Find the first connected controller.
     */
    private int findConnectedController() {
        // Check all 4 possible joystick slots
        for (int i = GLFW_JOYSTICK_1; i <= GLFW_JOYSTICK_4; i++) {
            if (GLFW.glfwJoystickPresent(i)) {
                return i;
            }
        }
        return -1; // No controller found
    }
    
    /**
     * Read actual input from the specified controller.
     */
    private ControllerInput readControllerInput(int joystickId) {
        try {
            FloatBuffer axes = GLFW.glfwGetJoystickAxes(joystickId);
            ByteBuffer buttons = GLFW.glfwGetJoystickButtons(joystickId);
            
            if (axes == null || buttons == null) {
                return null;
            }
            
            // Read analog sticks and triggers
            float leftX = clampAxis(axes.get(0));
            float leftY = clampAxis(axes.get(1));
            float rightX = clampAxis(axes.get(2));
            float rightY = clampAxis(axes.get(3));
            
            // Triggers (may be axes 4 and 5, or combined in axis 2 depending on controller)
            float leftTrigger = 0f;
            float rightTrigger = 0f;
            
            if (axes.capacity() > 4) {
                leftTrigger = Math.max(0f, axes.get(4));
            }
            if (axes.capacity() > 5) {
                rightTrigger = Math.max(0f, axes.get(5));
            }
            
            // Map buttons to our enum
            boolean[] buttonStates = new boolean[ControllerButton.values().length];
            
            // Standard Xbox/PlayStation button mapping
            if (buttons.capacity() > 0) {
                buttonStates[ControllerButton.A_BUTTON.ordinal()] = buttons.get(0) != 0;
            }
            if (buttons.capacity() > 1) {
                buttonStates[ControllerButton.B_BUTTON.ordinal()] = buttons.get(1) != 0;
            }
            if (buttons.capacity() > 2) {
                buttonStates[ControllerButton.X_BUTTON.ordinal()] = buttons.get(2) != 0;
            }
            if (buttons.capacity() > 3) {
                buttonStates[ControllerButton.Y_BUTTON.ordinal()] = buttons.get(3) != 0;
            }
            if (buttons.capacity() > 4) {
                buttonStates[ControllerButton.LEFT_BUMPER.ordinal()] = buttons.get(4) != 0;
            }
            if (buttons.capacity() > 5) {
                buttonStates[ControllerButton.RIGHT_BUMPER.ordinal()] = buttons.get(5) != 0;
            }
            if (buttons.capacity() > 6) {
                buttonStates[ControllerButton.BACK_BUTTON.ordinal()] = buttons.get(6) != 0;
            }
            if (buttons.capacity() > 7) {
                buttonStates[ControllerButton.START_BUTTON.ordinal()] = buttons.get(7) != 0;
            }
            if (buttons.capacity() > 8) {
                buttonStates[ControllerButton.LEFT_STICK_CLICK.ordinal()] = buttons.get(8) != 0;
            }
            if (buttons.capacity() > 9) {
                buttonStates[ControllerButton.RIGHT_STICK_CLICK.ordinal()] = buttons.get(9) != 0;
            }
            
            // D-pad (if available as buttons or hat)
            if (buttons.capacity() > 10) {
                buttonStates[ControllerButton.DPAD_UP.ordinal()] = buttons.get(10) != 0;
            }
            if (buttons.capacity() > 11) {
                buttonStates[ControllerButton.DPAD_DOWN.ordinal()] = buttons.get(11) != 0;
            }
            if (buttons.capacity() > 12) {
                buttonStates[ControllerButton.DPAD_LEFT.ordinal()] = buttons.get(12) != 0;
            }
            if (buttons.capacity() > 13) {
                buttonStates[ControllerButton.DPAD_RIGHT.ordinal()] = buttons.get(13) != 0;
            }
            
            return new ControllerInput(leftX, leftY, rightX, rightY, leftTrigger, rightTrigger, buttonStates);
            
        } catch (Exception e) {
            LOGGER.error("Error reading controller input from joystick {}", joystickId, e);
            return null;
        }
    }
    
    /**
     * Clamp and normalize axis values to [-1.0, 1.0] range.
     */
    private float clampAxis(float value) {
        return Math.max(-1.0f, Math.min(1.0f, value));
    }
    
    /**
     * Get information about connected controllers.
     */
    public String getControllerInfo() {
        if (activeController == -1) {
            return "No controller connected";
        }
        
        String name = GLFW.glfwGetJoystickName(activeController);
        FloatBuffer axes = GLFW.glfwGetJoystickAxes(activeController);
        ByteBuffer buttons = GLFW.glfwGetJoystickButtons(activeController);
        
        return String.format("Controller: %s, Axes: %d, Buttons: %d", 
                           name, axes != null ? axes.capacity() : 0, 
                           buttons != null ? buttons.capacity() : 0);
    }
    
    /**
     * Check if a specific controller slot has a connected device.
     */
    public boolean isControllerConnected(int slot) {
        if (slot < GLFW_JOYSTICK_1 || slot > GLFW_JOYSTICK_4) {
            return false;
        }
        return GLFW.glfwJoystickPresent(slot);
    }
    
    /**
     * Get the name of the controller in the specified slot.
     */
    public String getControllerName(int slot) {
        if (!isControllerConnected(slot)) {
            return null;
        }
        return GLFW.glfwGetJoystickName(slot);
    }
    
    /**
     * Get a list of all connected controllers.
     */
    public String[] getConnectedControllers() {
        java.util.List<String> controllers = new java.util.ArrayList<>();
        
        for (int i = GLFW_JOYSTICK_1; i <= GLFW_JOYSTICK_4; i++) {
            if (GLFW.glfwJoystickPresent(i)) {
                String name = GLFW.glfwGetJoystickName(i);
                if (name != null) {
                    controllers.add(String.format("Slot %d: %s", i, name));
                }
            }
        }
        
        return controllers.toArray(new String[0]);
    }
    
    /**
     * Force re-scan for controllers.
     */
    public void rescanControllers() {
        int oldActive = activeController;
        activeController = findConnectedController();
        
        if (oldActive != activeController) {
            ControllerManager manager = ControllerManager.getInstance();
            manager.setControllerConnected(activeController != -1);
            
            if (activeController != -1) {
                String name = GLFW.glfwGetJoystickName(activeController);
                LOGGER.info("Controller rescanned and found: {} in slot {}", name, activeController);
            }
        }
    }
    
    /**
     * Test if a specific button is currently pressed on the active controller.
     */
    public boolean isButtonPressed(ControllerButton button) {
        if (activeController == -1) {
            return false;
        }
        
        try {
            ByteBuffer buttons = GLFW.glfwGetJoystickButtons(activeController);
            if (buttons == null || button.ordinal() >= buttons.capacity()) {
                return false;
            }
            
            return buttons.get(button.ordinal()) != 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the current value of an axis on the active controller.
     */
    public float getAxisValue(int axisIndex) {
        if (activeController == -1) {
            return 0f;
        }
        
        try {
            FloatBuffer axes = GLFW.glfwGetJoystickAxes(activeController);
            if (axes == null || axisIndex >= axes.capacity()) {
                return 0f;
            }
            
            return clampAxis(axes.get(axisIndex));
        } catch (Exception e) {
            return 0f;
        }
    }
}

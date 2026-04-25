package com.voltaccept.AnalogAPI.mixin;

//? if <1.21.3 {
/*import net.minecraft.client.player.Input;
*///? } else {
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
//? }
//? if >=1.21.5 {
import net.minecraft.world.phys.Vec2;
//? }
import com.voltaccept.AnalogAPI.api.AnalogAPI;
import com.voltaccept.AnalogAPI.api.MovementState;
import com.voltaccept.AnalogAPI.api.VirtualInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.verotek.libanalog.interfaces.mixin.IAnalogKeybinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends /*? if >=1.21.3 {*/ ClientInput /*?} else {*/ /*Input *//*?}*/ {
  @Shadow @Final private Options options;

  @Unique
  private float computeSidewaysMovement(KeyMapping positive, KeyMapping negative) {
    IAnalogKeybinding analogPositive = (IAnalogKeybinding) positive;
    IAnalogKeybinding analogNegative = (IAnalogKeybinding) negative;

    return analogPositive.pressedAmount() - analogNegative.pressedAmount();
  }

  @Unique
  private float computeForwardMovement(KeyMapping positive, KeyMapping negative) {
    IAnalogKeybinding analogPositive = (IAnalogKeybinding) positive;
    IAnalogKeybinding analogNegative = (IAnalogKeybinding) negative;

    if (options.keySprint.isDown()
        && positive.isDown()
        && analogPositive.pressedAmount() > analogNegative.pressedAmount()) {
      return 1.0f;
    }

    return analogPositive.pressedAmount() - analogNegative.pressedAmount();
  }

  /**
   * @author lvoegl
   * @reason Computes player movement based on analog inputs and exposes the AnalogAPI hook so that
   *         external applications can read the current movement state or override it with virtual
   *         input over the AnalogAPI HTTP server.
   */
  @Overwrite
  public void tick(/*? if <1.21.4 {*/ /*boolean slowDown, float slowDownFactor *//*?}*/) {
    Screen screen = Minecraft.getInstance().screen;
    LocalPlayer player = Minecraft.getInstance().player;
    if (screen != null || player == null) {
      // reset all movement

      //? if >=1.21.3 {
      keyPresses = new Input(false, false, false, false, false, false, false);
      //?} else {
      /*up = false;
      down = false;
      left = false;
      right = false;
      jumping = false;
      shiftKeyDown = false;
      *///?}

      //? if >=1.21.5 {
      moveVector = new Vec2(0.0f, 0.0f);
      //?} else {
      /*forwardImpulse = 0.0f;
      leftImpulse = 0.0f;
      *///?}
      AnalogAPI.getInstance().publishMovementState(MovementState.idle());
      return;
    }

    AnalogAPI api = AnalogAPI.getInstance();
    VirtualInput virtual = api.getVirtualInput();

    float forwardMovement;
    float sidewaysMovement;
    boolean upPressed;
    boolean downPressed;
    boolean leftPressed;
    boolean rightPressed;
    boolean jumpPressed;
    boolean sneakPressed;
    boolean sprintPressed;

    if (virtual != null) {
      forwardMovement = virtual.forward;
      sidewaysMovement = virtual.sideways;
      upPressed = virtual.forward > 0f;
      downPressed = virtual.forward < 0f;
      rightPressed = virtual.sideways > 0f;
      leftPressed = virtual.sideways < 0f;
      jumpPressed = virtual.jump;
      sneakPressed = virtual.sneak;
      sprintPressed = virtual.sprint;
    } else {
      forwardMovement = computeForwardMovement(options.keyUp, options.keyDown);
      sidewaysMovement = computeSidewaysMovement(options.keyLeft, options.keyRight);
      upPressed = options.keyUp.isDown();
      downPressed = options.keyDown.isDown();
      leftPressed = options.keyLeft.isDown();
      rightPressed = options.keyRight.isDown();
      jumpPressed = options.keyJump.isDown();
      sneakPressed = options.keyShift.isDown();
      sprintPressed = options.keySprint.isDown();
    }

    //? if >=1.21.3 {
    keyPresses = new Input(
        upPressed,
        downPressed,
        leftPressed,
        rightPressed,
        jumpPressed,
        sneakPressed,
        sprintPressed);
    //?} else {
    /*up = upPressed;
    down = downPressed;
    left = leftPressed;
    right = rightPressed;
    jumping = jumpPressed;
    shiftKeyDown = sneakPressed;
    *///?}

    //? if <1.21.4 {
    /*if (slowDown) {
      forwardMovement *= slowDownFactor;
      sidewaysMovement *= slowDownFactor;
    }
    *///?}

    //? if >=1.21.5 {
    moveVector = new Vec2(sidewaysMovement, forwardMovement);
    //?} else {
    /*forwardImpulse = forwardMovement;
    leftImpulse = sidewaysMovement;
    *///?}

    api.publishMovementState(new MovementState(
        forwardMovement, sidewaysMovement, jumpPressed, sneakPressed, sprintPressed));
  }
}

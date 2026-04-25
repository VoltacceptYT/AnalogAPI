package com.voltaccept.AnalogAPI.api;

public final class MovementState {
  public final float forward;
  public final float sideways;
  public final boolean jump;
  public final boolean sneak;
  public final boolean sprint;

  public MovementState(float forward, float sideways, boolean jump, boolean sneak, boolean sprint) {
    this.forward = forward;
    this.sideways = sideways;
    this.jump = jump;
    this.sneak = sneak;
    this.sprint = sprint;
  }

  public static MovementState idle() {
    return new MovementState(0f, 0f, false, false, false);
  }

  public String toJson() {
    return "{"
        + "\"forward\":" + forward
        + ",\"sideways\":" + sideways
        + ",\"jump\":" + jump
        + ",\"sneak\":" + sneak
        + ",\"sprint\":" + sprint
        + "}";
  }
}

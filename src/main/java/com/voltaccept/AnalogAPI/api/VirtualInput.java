package com.voltaccept.AnalogAPI.api;

public final class VirtualInput {
  public final float forward;
  public final float sideways;
  public final boolean jump;
  public final boolean sneak;
  public final boolean sprint;

  public VirtualInput(float forward, float sideways, boolean jump, boolean sneak, boolean sprint) {
    this.forward = clamp(forward);
    this.sideways = clamp(sideways);
    this.jump = jump;
    this.sneak = sneak;
    this.sprint = sprint;
  }

  private static float clamp(float v) {
    if (v < -1f) return -1f;
    if (v > 1f) return 1f;
    return v;
  }
}

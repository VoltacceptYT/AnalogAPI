package com.voltaccept.AnalogAPI.api;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class AnalogAPI {
  private static final AnalogAPI INSTANCE = new AnalogAPI();

  private final AtomicReference<MovementState> currentState = new AtomicReference<>(MovementState.idle());
  private final AtomicReference<VirtualInput> virtualInput = new AtomicReference<>(null);
  private final List<Consumer<MovementState>> listeners = new CopyOnWriteArrayList<>();

  private AnalogAPI() {}

  public static AnalogAPI getInstance() {
    return INSTANCE;
  }

  public MovementState getMovementState() {
    return currentState.get();
  }

  public void publishMovementState(MovementState state) {
    if (state == null) state = MovementState.idle();
    currentState.set(state);
    for (Consumer<MovementState> l : listeners) {
      try {
        l.accept(state);
      } catch (Throwable ignored) {
      }
    }
  }

  public void setVirtualInput(VirtualInput input) {
    virtualInput.set(input);
  }

  public void clearVirtualInput() {
    virtualInput.set(null);
  }

  public VirtualInput getVirtualInput() {
    return virtualInput.get();
  }

  public boolean isVirtualInputActive() {
    return virtualInput.get() != null;
  }

  public void addListener(Consumer<MovementState> listener) {
    if (listener != null) listeners.add(listener);
  }

  public void removeListener(Consumer<MovementState> listener) {
    listeners.remove(listener);
  }
}

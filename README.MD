# AnalogAPI Controller Support

A controller support mod for Minecraft (Fabric) that provides comprehensive gamepad
functionality with configurable button bindings. The mod exposes controller state
and allows **real-time button remapping** through a small HTTP API running on
`localhost`.

Built on top of the original AnalogMovement work and the `libanalog` library,
AnalogAPI Controller Support adds native gamepad integration with customizable
mappings, deadzone settings, and full analog stick support for an enhanced
gaming experience.

* Mod ID: `analog_api`
* Java package: `com.voltaccept.AnalogAPI`
* Default Minecraft version: `1.21.1` (Fabric)
* Environment: client only

---

## Table of contents

1. [Installation](#installation)
2. [Controller Setup](#controller-setup)
3. [Button Bindings](#button-bindings)
4. [Configuration](#configuration)
5. [Java API](#java-api)
6. [HTTP API](#http-api)
   * [`GET /api/health`](#get-apihealth)
   * [`GET /api/state`](#get-apistate)
   * [`GET /api/virtual-input`](#get-apivirtual-input)
   * [`POST /api/virtual-input`](#post-apivirtual-input)
   * [`DELETE /api/virtual-input`](#delete-apivirtual-input)
   * [`GET /api/controller`](#get-apicontroller)
   * [`POST /api/controller`](#post-apicontroller)
   * [`GET /api/controller/bindings`](#get-apicontrollerbindings)
   * [`POST /api/controller/bindings`](#post-apicontrollerbindings)
   * [`DELETE /api/controller/bindings`](#delete-apicontrollerbindings)
   * [`GET /api/controller/config`](#get-apicontrollerconfig)
   * [`POST /api/controller/config`](#post-apicontrollerconfig)
7. [Examples](#examples)
8. [Building from source](#building-from-source)
9. [License](#license)

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft `1.21.1`.
2. Drop the following jars into your `mods/` folder:
   * `fabric-api`
   * `libanalog`
   * `analog_api-fabric-<version>.jar` (built by this project)
3. Launch the game. On startup the log will contain a line similar to:

   ```
   [analog_api] AnalogAPI Controller Support HTTP server started on http://127.0.0.1:27800
   ```

The HTTP server listens on `127.0.0.1` (loopback) by default, so it is **not**
reachable from the network unless you change the host explicitly.

---

## Controller Setup

AnalogAPI Controller Support automatically detects compatible gamepads (Xbox, PlayStation, and generic USB controllers). 

**Supported Controllers:**
- Xbox One/Series X|S controllers
- PlayStation DualShock 4/DualSense controllers  
- Generic USB gamepads following standard layouts
- Most Bluetooth controllers

**Default Button Layout:**
- **A Button** - Jump
- **B Button** - Sneak  
- **X Button** - Attack/Use item
- **Y Button** - Inventory
- **Left Bumper** - Drop item
- **Right Bumper** - Sprint
- **Left Trigger** - Use item (analog)
- **Right Trigger** - Attack (analog)
- **Left Stick** - Movement
- **Start Button** - Pause menu
- **Back/Select Button** - Inventory

---

## Button Bindings

The mod allows complete customization of controller button mappings through the HTTP API. Each button can be bound to any Minecraft action, with support for both digital and analog inputs.

**Available Actions:**
- Movement: `forward`, `backward`, `left`, `right`
- Actions: `jump`, `sneak`, `sprint`, `attack`, `use`
- Interface: `inventory`, `chat`, `drop_item`, `pause`

**Binding Types:**
- **Digital**: Simple on/off button press
- **Analog**: Variable input based on pressure (triggers, some buttons)

See the [HTTP API](#http-api) section for detailed binding configuration.

## Configuration

The HTTP server reads its host and port from system properties or environment
variables, in this order of priority:

| Setting | System property      | Environment variable | Default       |
|---------|----------------------|----------------------|---------------|
| Host    | `analogapi.host`     | `ANALOGAPI_HOST`     | `127.0.0.1`   |
| Port    | `analogapi.port`     | `ANALOGAPI_PORT`     | `27800`       |

Examples:

```bash
# Bind to a different port
java -Danalogapi.port=31415 -jar minecraft.jar

# Or via env var
ANALOGAPI_PORT=31415 ANALOGAPI_HOST=127.0.0.1 ./run-minecraft.sh
```

> Binding to `0.0.0.0` exposes the API to your local network. Only do this on
> a trusted network and behind a firewall — there is no built-in
> authentication.

---

## Java API

For other Fabric mods that want direct in-process access, the API is a
singleton at `com.voltaccept.AnalogAPI.api.AnalogAPI`.

```java
import com.voltaccept.AnalogAPI.api.AnalogAPI;
import com.voltaccept.AnalogAPI.api.MovementState;
import com.voltaccept.AnalogAPI.api.VirtualInput;

AnalogAPI api = AnalogAPI.getInstance();

// Read the latest computed movement state
MovementState state = api.getMovementState();
System.out.println(state.forward + ", " + state.sideways);

// Drive the player from your own code
api.setVirtualInput(new VirtualInput(
    /* forward  */ 1.0f,    // -1.0 .. 1.0
    /* sideways */ 0.0f,    // -1.0 .. 1.0
    /* jump     */ false,
    /* sneak    */ false,
    /* sprint   */ true
));

// Hand control back to the keyboard / libanalog
api.clearVirtualInput();

// Subscribe to every tick of new state
api.addListener(s -> {
    if (s.jump) System.out.println("player just jumped");
});
```

### Types

#### `MovementState`

| Field      | Type      | Description                                  |
|------------|-----------|----------------------------------------------|
| `forward`  | `float`   | Forward (+) / backward (−) impulse, `−1..1` |
| `sideways` | `float`   | Right (+) / left (−) impulse, `−1..1`       |
| `jump`     | `boolean` | Jump key pressed                             |
| `sneak`    | `boolean` | Sneak / shift key pressed                    |
| `sprint`   | `boolean` | Sprint key pressed                           |

#### `VirtualInput`

Same fields as `MovementState`. Float fields are clamped to `[-1.0, 1.0]`.

While a `VirtualInput` is set, the mixin uses **its** values instead of the
real keyboard / analog input. Calling `clearVirtualInput()` (or sending
`DELETE /api/virtual-input`) restores normal keyboard behavior.

---

## HTTP API

All endpoints use JSON bodies and return JSON responses with
`Content-Type: application/json; charset=utf-8`. CORS is enabled
(`Access-Control-Allow-Origin: *`) so browser-based tools can call the API
directly.

Base URL: `http://<host>:<port>` — defaults to `http://127.0.0.1:27800`.

### `GET /api/health`

Liveness probe. Always returns `200`.

```json
{ "ok": true, "name": "AnalogAPI" }
```

### `GET /api/state`

Returns the latest movement state computed by the mod for the current tick.

**Response `200`:**

```json
{
  "forward": 0.85,
  "sideways": -0.32,
  "jump": false,
  "sneak": false,
  "sprint": true
}
```

### `GET /api/virtual-input`

Returns whether a virtual input is currently driving the player and, if so,
its values.

**Response `200` — inactive:**

```json
{ "active": false }
```

**Response `200` — active:**

```json
{
  "active": true,
  "forward": 1.0,
  "sideways": 0.0,
  "jump": false,
  "sneak": false,
  "sprint": true
}
```

### `POST /api/virtual-input`

Set or replace the virtual input. While set, the mixin uses these values
instead of the real keyboard input. `PUT` is also accepted as an alias.

**Request body:** All fields are optional. Missing numeric fields default to
`0`, missing boolean fields default to `false`.

```json
{
  "forward":  1.0,
  "sideways": 0.0,
  "jump":     false,
  "sneak":    false,
  "sprint":   true
}
```

**Response `200`:**

```json
{ "active": true }
```

**Response `400`:** Invalid JSON.

```json
{ "error": "invalid_json", "message": "..." }
```

> Float values are clamped to `[-1.0, 1.0]`.

### `DELETE /api/virtual-input`

Clear any active virtual input and return control to the keyboard.

**Response `200`:**

```json
{ "active": false }
```

### `GET /api/controller`

Get controller status and connection information.

**Response `200`:**

```json
{
  "connected": true,
  "enabled": true,
  "driving": false
}
```

### `POST /api/controller`

Enable or disable controller input processing.

**Request body:**

```json
{
  "enabled": true
}
```

**Response `200`:**

```json
{ "enabled": true }
```

### `GET /api/controller/bindings`

Get all current button bindings.

**Response `200`:**

```json
{
  "bindings": [
    {
      "button": "a_button",
      "action": "jump",
      "threshold": 0.5,
      "analog": false
    },
    {
      "button": "left_trigger",
      "action": "use",
      "threshold": 0.1,
      "analog": true
    }
  ]
}
```

### `POST /api/controller/bindings`

Create or update a button binding.

**Request body:**

```json
{
  "button": "a_button",
  "action": "jump",
  "threshold": 0.5,
  "analog": false
}
```

**Response `200`:**

```json
{ "success": true }
```

**Response `400`:** Invalid button/action or missing fields.

### `DELETE /api/controller/bindings`

Remove a button binding.

**Request body:**

```json
{
  "button": "a_button"
}
```

**Response `200`:**

```json
{ "success": true }
```

### `GET /api/controller/config`

Get controller configuration settings.

**Response `200`:**

```json
{
  "leftStickDeadzone": 0.15,
  "rightStickDeadzone": 0.15,
  "triggerThreshold": 0.1,
  "invertYAxis": false,
  "stickSensitivity": 1.0,
  "bindings": [...]
}
```

### `POST /api/controller/config`

Update controller configuration settings.

**Request body:** All fields are optional.

```json
{
  "leftStickDeadzone": 0.2,
  "rightStickDeadzone": 0.2,
  "triggerThreshold": 0.15,
  "invertYAxis": true,
  "stickSensitivity": 1.2
}
```

**Response `200`:**

```json
{ "success": true }
```

---

## Examples

### `curl`

```bash
# Read current state
curl http://127.0.0.1:27800/api/state

# Walk forward at full speed while sprinting
curl -X POST http://127.0.0.1:27800/api/virtual-input \
     -H "Content-Type: application/json" \
     -d '{"forward":1.0,"sprint":true}'

# Strafe right + jump
curl -X POST http://127.0.0.1:27800/api/virtual-input \
     -H "Content-Type: application/json" \
     -d '{"sideways":1.0,"jump":true}'

# Stop
curl -X DELETE http://127.0.0.1:27800/api/virtual-input

# Check controller status
curl http://127.0.0.1:27800/api/controller

# Get current button bindings
curl http://127.0.0.1:27800/api/controller/bindings

# Bind Xbox A button to jump
curl -X POST http://127.0.0.1:27800/api/controller/bindings \
     -H "Content-Type: application/json" \
     -d '{"button":"a_button","action":"jump"}'

# Configure controller deadzones
curl -X POST http://127.0.0.1:27800/api/controller/config \
     -H "Content-Type: application/json" \
     -d '{"leftStickDeadzone":0.2,"rightStickDeadzone":0.2}'

### Python

```python
import requests

BASE = "http://127.0.0.1:27800"

# Poll the player's current movement
state = requests.get(f"{BASE}/api/state").json()
print(state)

# Drive the player forward for 2 seconds, then stop
import time
requests.post(f"{BASE}/api/virtual-input",
              json={"forward": 1.0, "sprint": True})
time.sleep(2)
requests.delete(f"{BASE}/api/virtual-input")

# Controller management examples
def setup_controller():
    # Check if controller is connected
    status = requests.get(f"{BASE}/api/controller").json()
    print(f"Controller connected: {status['connected']}")
    
    if status['connected']:
        # Enable controller input
        requests.post(f"{BASE}/api/controller", json={"enabled": True})
        
        # Configure deadzones for precision
        requests.post(f"{BASE}/api/controller/config", json={
            "leftStickDeadzone": 0.15,
            "rightStickDeadzone": 0.15,
            "triggerThreshold": 0.1
        })
        
        # Custom button binding - make RB the attack button
        requests.post(f"{BASE}/api/controller/bindings", json={
            "button": "right_bumper",
            "action": "attack"
        })

def remap_buttons():
    # Create a FPS-style layout
    bindings = [
        {"button": "right_trigger", "action": "attack", "analog": True},
        {"button": "left_trigger", "action": "use", "analog": True},
        {"button": "a_button", "action": "jump"},
        {"button": "b_button", "action": "sneak"},
        {"button": "y_button", "action": "inventory"},
        {"button": "x_button", "action": "drop_item"}
    ]
    
    for binding in bindings:
        requests.post(f"{BASE}/api/controller/bindings", json=binding)

# Usage
setup_controller()
remap_buttons()
```

### JavaScript / Node

```js
const BASE = "http://127.0.0.1:27800";

const state = await fetch(`${BASE}/api/state`).then(r => r.json());
console.log(state);

await fetch(`${BASE}/api/virtual-input`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ forward: 1.0, sideways: 0.5, sprint: true })
});

// Controller management examples
class ControllerManager {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
  }

  async getStatus() {
    const response = await fetch(`${this.baseUrl}/api/controller`);
    return await response.json();
  }

  async enableController(enabled = true) {
    await fetch(`${this.baseUrl}/api/controller`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ enabled })
    });
  }

  async getBindings() {
    const response = await fetch(`${this.baseUrl}/api/controller/bindings`);
    return await response.json();
  }

  async setBinding(button, action, options = {}) {
    const payload = { button, action, ...options };
    await fetch(`${this.baseUrl}/api/controller/bindings`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
  }

  async configureSettings(settings) {
    await fetch(`${this.baseUrl}/api/controller/config`, {
      method: "POST", 
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(settings)
    });
  }
}

// Usage example
async function setupFPSLayout() {
  const controller = new ControllerManager(BASE);
  
  // Check connection
  const status = await controller.getStatus();
  console.log(`Controller connected: ${status.connected}`);
  
  if (status.connected) {
    await controller.enableController(true);
    
    // Configure for FPS gaming
    await controller.configureSettings({
      leftStickDeadzone: 0.1,
      rightStickDeadzone: 0.1,
      triggerThreshold: 0.05,
      invertYAxis: true,
      stickSensitivity: 1.2
    });
    
    // Set up FPS controls
    await controller.setBinding("right_trigger", "attack", { analog: true });
    await controller.setBinding("left_trigger", "use", { analog: true });
    await controller.setBinding("a_button", "jump");
    await controller.setBinding("b_button", "sneak");
    await controller.setBinding("right_bumper", "sprint");
    await controller.setBinding("y_button", "inventory");
    
    console.log("FPS controller layout configured!");
  }
}

setupFPSLayout();
```

---

## Building from source

The project uses Gradle (via the wrapper) and
[Stonecutter](https://github.com/kikugie/stonecutter) for multi-version
support. **Gradle and Stonecutter require Java 21+.**

The active version is set in `stonecutter.gradle.kts` (default `1.21.1-fabric`).

```bash
# Build only the active Minecraft version
./gradlew :1.21.1-fabric:build

# Build all configured versions
./gradlew build
```

Produced jars:

```
versions/1.21.1-fabric/build/libs/analog_api-fabric-<version>.jar
```

To switch to another Minecraft version, edit the line:

```kotlin
stonecutter active "1.21.1-fabric" /* [SC] DO NOT EDIT */
```

…in `stonecutter.gradle.kts` to one of the entries declared in
`settings.gradle.kts` (e.g. `1.20.1-fabric`, `1.21.4-fabric`, `1.21.5-fabric`,
…), then rebuild.

---

## License

Released under the **GPL-2.0-or-later** license. See [`LICENSE`](LICENSE) for
the full text.

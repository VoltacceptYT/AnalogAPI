# AnalogAPI

An API mod for Minecraft (Fabric) that exposes player movement state and lets
**any external application** drive the player through a small HTTP API running
on `localhost`.

It is built on top of the original AnalogMovement work and the `libanalog`
library: keyboard analog values are still computed from `libanalog`, but they
are now also published to a public Java API and a JSON HTTP server so other
programs (overlays, AI agents, controller bridges, accessibility tools, custom
launchers, etc.) can both **observe** and **override** what the player is doing.

* Mod ID: `analog_api`
* Java package: `com.voltaccept.AnalogAPI`
* Default Minecraft version: `1.21.1` (Fabric)
* Environment: client only

---

## Table of contents

1. [Installation](#installation)
2. [Configuration](#configuration)
3. [Java API](#java-api)
4. [HTTP API](#http-api)
   * [`GET /api/health`](#get-apihealth)
   * [`GET /api/state`](#get-apistate)
   * [`GET /api/virtual-input`](#get-apivirtual-input)
   * [`POST /api/virtual-input`](#post-apivirtual-input)
   * [`DELETE /api/virtual-input`](#delete-apivirtual-input)
5. [Examples](#examples)
6. [Building from source](#building-from-source)
7. [License](#license)

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft `1.21.1`.
2. Drop the following jars into your `mods/` folder:
   * `fabric-api`
   * `libanalog`
   * `analog_api-fabric-<version>.jar` (built by this project)
3. Launch the game. On startup the log will contain a line similar to:

   ```
   [analog_api] AnalogAPI HTTP server started on http://127.0.0.1:27800
   ```

The HTTP server listens on `127.0.0.1` (loopback) by default, so it is **not**
reachable from the network unless you change the host explicitly.

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
```

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

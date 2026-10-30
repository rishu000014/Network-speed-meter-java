# NetSpeed Meter

Windows desktop network speed monitor for a college hackathon. It is a real JavaFX application: live download and upload Mbps come from Windows adapter byte counters (`Get-NetAdapterStatistics`), not placeholder numbers.

## Stack

- Java 21
- JavaFX 21
- Maven 3.10.0
- CSS for the dark operations dashboard

## Features

- Reads **actual** received/sent bytes from Windows NICs
- Converts successive samples into **download and upload Mbps**
- Live throughput graph of collected measurements
- **Ping** via Windows `ping.exe` (default `8.8.8.8`)
- Active adapter: name, description, MAC, IPv4, link speed, status, byte counters
- **Start / Stop** monitoring without freezing the UI
- Clean shutdown of background threads and child processes

## Installation (from source)

1. Windows 10 or 11
2. Install **JDK 21** and add it to `PATH` (`java -version` should show 21)
3. Install **Apache Maven 3.10.x** and add it to `PATH` (`mvn -version`)
4. Open a terminal in this project folder

Verify the build:

```bat
mvn clean verify
```

## Run from Maven

```bat
mvn javafx:run
```

Click **Start monitoring**. The first tick calibrates counters; later ticks show real Mbps. Generate traffic (browser, download) to see the graph move. **Stop** ends sampling. Closing the window shuts the monitor down.

## Run the Windows package

After `mvn clean verify` (which includes `package`), the portable folder is:

`target\NetSpeedMeter-windows\`

Double-click `NetSpeedMeter.bat`, or from a terminal:

```bat
target\NetSpeedMeter-windows\NetSpeedMeter.bat
```

That folder contains:

- `network-speed-meter-1.0.0.jar` — application
- `javafx\` — JavaFX 21 Windows modules
- `NetSpeedMeter.bat` — launcher (uses JDK 21 `java` on PATH)

If `jpackage` is available, you can also build a native app-image:

```bat
mvn -q package
jpackage --type app-image --name NetSpeedMeter --dest target\dist --input target\NetSpeedMeter-windows --main-jar network-speed-meter-1.0.0.jar --main-class com.netspeedmeter.App --module-path target\NetSpeedMeter-windows\javafx --add-modules javafx.controls,javafx.graphics,javafx.base
```

Then run `target\dist\NetSpeedMeter\NetSpeedMeter.exe`. This needs a JDK that includes `jpackage` (JDK 21). WiX is only required for `.msi` installers, not for `app-image`.

## How speed is calculated

The app records `ReceivedBytes` and `SentBytes` per adapter, then:

`Mbps = (deltaBytes * 8) / elapsedSeconds / 1_000_000`

Download and upload are the sum of those deltas across adapters. The adapter with the largest byte delta is shown as active.

## Notes

- Requires PowerShell (`Get-NetAdapterStatistics`), which ships with Windows.
- Ping uses ICMP via `ping.exe`. Blocked ICMP is shown as timeout, not a fake 0 ms.
- Speeds are **interface utilization**, not a speed-test to a remote server.
- Do not push this project to GitHub until you choose to.

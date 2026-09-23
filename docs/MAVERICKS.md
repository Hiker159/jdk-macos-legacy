# Java 21, 22 and 25 for OS X Mavericks

## Source baselines

| Port | Upstream repository | Exact tag | Source archive SHA-256 |
|---|---|---|---|
| 21 | openjdk/jdk21u | `jdk-21.0.3+9` | `b7a78e596b272d958843eab0c0412fd7ee874a3b6fff577584ebeed39dfef7ee` |
| 25 | openjdk/jdk25u | `jdk-25.0.4.1+1` | `1dd5567b315e19cf4cfb2c32931e535fd65b326960a8770f351d7655439b1eb8` |
| 22 | openjdk/jdk22u | `jdk-22.0.2+9` | `f3bc990ed34dd0c9a1467640a9c654df4375ff948e854038b913abec12be6179` |

## What changes

- Sets the Intel deployment target to 10.9. ARM's deployment target is not lowered; the build script requires an Intel host.
- Removes Metal framework linking, shader compilation and native source compilation. The original Metal source files stay intact. Java2D chooses OpenGL, including when a launcher requests Metal. Missing OpenGL produces a clear error.
- Replaces HotSpot's newer clock calls with `gettimeofday` and Mach time. This includes timed waits and performance monitoring, not just `System.nanoTime`.
- Uses Jazzzny's BSD UDP disconnect path from the Java 17 patches.
- Looks up `clonefile` dynamically. On Mavericks, `ENOTSUP` activates the JDK's existing file-copy fallback rather than failing to load `libnio`.
- Avoids `MAP_JIT` for Intel executable memory; these builds must not be signed with hardened runtime enabled. ARM retains its existing mapping flag.
- Uses the older NSString substring API for keyboard input and the Darwin math header for bundled PNG code.
- Guards automatic window tabbing and full-window content; replaces the post-Mavericks `NSStatusBarButton` superclass with `NSButton` and the newer accessibility superclass with `NSObject` plus the accessibility protocol. Accessibility behavior still needs target-machine testing.
- Restores Java 22's removed guards for Mojave system colors and newer accessibility subroles.
- Java 25 restores guarded OS-version detection and uses LaunchServices on Mavericks for opening links and files, preserving the selected application and upstream open/edit checks.
- Java 25 dynamically resolves newer file timestamp, permission and directory APIs. When absent, it uses Mavericks timestamp and permission APIs with microsecond precision. Secure directory streams remain unavailable rather than being emulated with weaker race guarantees.
- Excludes ZGC through the build configuration. G1 remains available and is the normal choice for Minecraft.

## Build on an Intel Mac

Build on a newer macOS installation that can run the bootstrap JDK. Building *on* Mavericks is not required. Install Xcode command line tools and Autoconf; use a full JDK 21 as the bootstrap for Java 21/22, and JDK 25 for Java 25. Python 3 is only needed for the binary audit on the build host.

Example for Java 21, from this project's root:

```sh
mkdir -p work
curl -fL 'https://github.com/openjdk/jdk21u/archive/refs/tags/jdk-21.0.3+9.tar.gz' -o work/jdk21.tar.gz
echo 'b7a78e596b272d958843eab0c0412fd7ee874a3b6fff577584ebeed39dfef7ee  work/jdk21.tar.gz' | shasum -a 256 -c -
mkdir work/21
tar -xzf work/jdk21.tar.gz -C work/21 --strip-components=1
scripts/apply-patches.sh 21 work/21
scripts/build-mavericks.sh 21 work/21 /path/to/boot-jdk/Contents/Home --with-extra-ldflags=-Wl,-ld_classic
python3 scripts/audit-macos.py work/21/build/mavericks/images/jdk
scripts/smoke-test.sh work/21/build/mavericks/images/jdk --gui
```

For Java 22, use its repository, tag and checksum from the table, `work/22`, and `22` in the script arguments. For Java 25, use `openjdk/jdk25u`, its exact tag and checksum above, `work/25`, and `25` in the script arguments, with a JDK 25 bootstrap. Add `--with-version-build=1` to reproduce the packaged version number. Patches must be applied to a clean source tree; a second application fails without modifying files.

`-Wl,-ld_classic` selects Apple's classic linker on Xcode releases that still include it. Newer linker defaults can prevent old deployment targets. If this linker is unavailable, use an Xcode installation that includes a linker supporting 10.9; do not merely relabel an incompatible binary. SDK/compiler differences can introduce more unavailable APIs, which require inspection and target testing.

Build outputs are under `work/21/build/mavericks/images/` `work/22/build/mavericks/images/`, or `work/25/build/mavericks/images/`. The `.jdk` bundle is in `jdk-bundle`; its runtime lives in `Contents/Home`. The manual GitHub Actions workflow builds each version on an explicit Intel runner and uploads archives only after its checks pass. It does not publish releases or run Mavericks tests.

## Test on Mavericks

Copy the `.jdk` bundle plus this project's `scripts/smoke-test.sh` and `tests/LegacySmoke.java` to the Mavericks machine. The script and test must retain their relative project layout. No Python, compiler or system-wide Java installation is needed there.

```sh
scripts/smoke-test.sh /path/to/jdk-21.0.3.jdk/Contents/Home --gui
scripts/smoke-test.sh /path/to/jdk-22.0.2.jdk/Contents/Home --gui
scripts/smoke-test.sh /path/to/jdk-25.0.4.1.jdk/Contents/Home --gui
```

The test checks clocks, virtual-thread timed parking, file copying and timestamps, ZIP reading, IPv4 and IPv6 UDP disconnect/reconnect, TLS initialization, font rendering, and optionally a Swing window. It uses loopback networking only; TLS initialization does not test a remote HTTPS handshake. The window should appear and close after two seconds.

Also test HTTPS downloads, ordinary TCP connections, window resizing/fullscreen, audio, and sustained game play. Keep the console output and any `hs_err_pid*.log` crash files. The binary audit catches known bad imports, Metal links, build-host dependencies and deployment targets above 10.9, but cannot prove that every Cocoa selector or library ABI works on Mavericks.

## Minecraft

Game-native library patches and launcher settings are maintained in the separate
Minecraft compatibility patches repository. See this repository’s README for
current user-reported JDK results.

# jdk-macos-legacy

Patches for OpenJDK to create builds for Mac OS X 10.9 and later.

## Available builds

| Java | Source baseline | Download | Target |
| --- | --- | --- | --- |
| 21 (LTS) | `jdk-21.0.3+9` | `jdk-21.0.3-mavericks-experimental.tar.gz` | Intel x86_64, OS X 10.9 |
| 22 | `jdk-22.0.2+9` | `jdk-22.0.2-mavericks-experimental.tar.gz` | Intel x86_64, OS X 10.9 |
| 25 (LTS) | `jdk-25.0.4.1+1` | `jdk-25.0.4.1-mavericks-experimental.tar.gz` | Intel x86_64, OS X 10.9 |

These are full JDK bundles, including `java` and `javac`. They are not Apple
Silicon builds, and earlier Java 11 and Java 17 ports are not included here.

## Install a prebuilt JDK

1. Download the archive for the Java version your application requires.
2. Extract it. It contains a `.jdk` folder, such as `jdk-25.0.4.1.jdk`.
3. Put that folder somewhere permanent. The default location is "/Library/Java/JavaVirtualMachines/"
4. Configure your app or game to use the executable inside the bundle. For example, if the Java bundles are in the `JavaVirtualMachines` folder:

```text
/Library/Java/JavaVirtualMachines/jdk-21.0.3.jdk/Contents/Home/bin/java
/Library/Java/JavaVirtualMachines/jdk-22.0.2.jdk/Contents/Home/bin/java
/Library/Java/JavaVirtualMachines/jdk-25.0.4.1.jdk/Contents/Home/bin/java
```

## What the ports change

- Target Intel macOS 10.9 and replace newer dependencies with legacy APIs.
- Remove Java2D's Metal dependency and use its OpenGL backend.
- Adapt file-copy and UDP behavior, and guard newer Cocoa APIs.
- Avoid Intel `MAP_JIT` requirements and exclude ZGC; G1 remains available.
- Restore additional Java 25 file timestamp, permission, OS-version, and desktop
  integration fallbacks needed on Mavericks.

## Minecraft

Minecraft's game libraries changed a lot with the release of Minecraft 26.1, and many of the game libraries require newer MacOS APIs, specifically SDL, FreeType, OpenAL, and shader. Even with the JDK 25 build working, Minecraft specifically cannot run. I have written packages that patch these issues, available separately here: [Minecraft Patches for Legacy OS X](https://github.com/Hiker159/minecraft-jdk-patches)

## Build from source

Use a newer **Intel Mac** that can run the bootstrap JDK, with Xcode command line
tools, Autoconf, and an Apple linker supporting macOS 10.9. Use a full JDK 21 to
bootstrap Java 21 or 22, and JDK 25 to bootstrap Java 25. Python 3 is needed for
the binary audit. Building directly on Mavericks is not required.

Download a clean source tree for the exact baseline in the table above. Upstream
repositories are `openjdk/jdk21u`, `openjdk/jdk22u`, and `openjdk/jdk25u`.
The [detailed build guide](docs/MAVERICKS.md) includes source URLs and checksums.

For example, with Java 25 sources extracted into `work/25`:

```sh
scripts/apply-patches.sh 25 work/25
scripts/build-mavericks.sh 25 work/25 /path/to/boot-jdk/Contents/Home \
  --with-extra-ldflags=-Wl,-ld_classic --with-version-build=1
python3 scripts/audit-macos.py work/25/build/mavericks/images/jdk
scripts/smoke-test.sh work/25/build/mavericks/images/jdk --gui
```

For Java 21 or 22, substitute that version and its source directory; omit the
Java 25-specific `--with-version-build=1`. The scripts reject mismatched source
versions. Apply patches only once to a clean tree. The classic-linker option
requires an Xcode release that includes that linker.

## Validation and troubleshooting

The smoke test exercises clocks, virtual threads, files, ZIP reading, UDP, TLS
initialization, fonts, and an optional Swing window. Run it against the installed
JDK on Mavericks as well, keeping `scripts/` and `tests/` in their project layout:

```sh
scripts/smoke-test.sh /Library/Java/JavaVirtualMachines/jdk-25.0.4.1.jdk/Contents/Home --gui
```

- [Java 21 and 22 validation](docs/VALIDATION.md)
- [Java 25 validation and target-machine status](docs/VALIDATION-25.md)
- [Detailed port design and build instructions](docs/MAVERICKS.md)
- Minecraft compatibility patches repository (published separately)

## License
This project is licensed under the GPL v2 license. For more information, see the [LICENSE](LICENSE) file.

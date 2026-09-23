/* SPDX-License-Identifier: GPL-2.0-only */
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.zip.*;
import javax.net.ssl.SSLContext;
import javax.swing.*;

public class LegacySmoke {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static void udp(StandardProtocolFamily family, String loopback) throws Exception {
        InetAddress address = InetAddress.getByName(loopback);
        try (DatagramChannel first = DatagramChannel.open(family);
             DatagramChannel second = DatagramChannel.open(family)) {
            first.bind(new InetSocketAddress(address, 0));
            second.bind(new InetSocketAddress(address, 0));
            first.connect(second.getLocalAddress());
            first.disconnect();
            check(!first.isConnected(), "UDP disconnect " + family);
            first.connect(second.getLocalAddress());
            first.write(ByteBuffer.wrap(new byte[]{42}));
            second.configureBlocking(false);
            ByteBuffer result = ByteBuffer.allocate(1);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (second.receive(result) == null) {
                check(System.nanoTime() < deadline, "UDP receive timed out " + family);
                Thread.sleep(1);
            }
            check(result.get(0) == 42, "UDP payload " + family);
        }
    }
    public static void main(String[] args) throws Exception {
        System.out.println(System.getProperty("java.runtime.version") + " on " +
                           System.getProperty("os.name") + " " + System.getProperty("os.version"));
        check(Runtime.version().feature() == 21 || Runtime.version().feature() == 22 || Runtime.version().feature() == 25, "Use Java 21, 22 or 25");
        boolean gui = args.length > 0 && args[0].equals("--gui");
        long before = System.nanoTime();
        Thread.sleep(30);
        check(System.nanoTime() > before, "monotonic clock");
        check(Math.abs(Instant.now().toEpochMilli() - System.currentTimeMillis()) < 1000, "wall clock");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = executor.submit(() -> { LockSupport.parkNanos(5_000_000); return 42; });
            check(future.get(5, TimeUnit.SECONDS) == 42, "virtual thread timed park");
        }
        Path dir = Files.createTempDirectory("legacy-java-test-");
        try {
            Path original = Files.writeString(dir.resolve("source"), "Minecraft world data test");
            FileTime timestamp = FileTime.fromMillis(1_600_000_000_000L);
            Files.setLastModifiedTime(original, timestamp);
            Path copied = Files.copy(original, dir.resolve("copy"), StandardCopyOption.COPY_ATTRIBUTES);
            check(Files.readString(copied).equals(Files.readString(original)), "Files.copy contents");
            check(Files.getLastModifiedTime(copied).equals(timestamp), "Files.copy timestamp");
            Path link = Files.createSymbolicLink(dir.resolve("link"), original.getFileName());
            FileTime linkTime = FileTime.fromMillis(1_610_000_000_000L);
            Files.getFileAttributeView(link, java.nio.file.attribute.BasicFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS).setTimes(linkTime, linkTime, null);
            check(Files.getLastModifiedTime(link, LinkOption.NOFOLLOW_LINKS).equals(linkTime), "symlink timestamp");
            check(Files.getLastModifiedTime(original).equals(timestamp), "NOFOLLOW preserves target timestamp");
            Files.setPosixFilePermissions(original, java.nio.file.attribute.PosixFilePermissions.fromString("rw-r-----"));
            check(Files.getPosixFilePermissions(original).equals(
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-r-----")), "file permissions");
            Path zip = dir.resolve("test.zip");
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
                out.putNextEntry(new ZipEntry("world.txt"));
                out.write(new byte[]{1, 2, 3});
                out.closeEntry();
            }
            try (ZipFile in = new ZipFile(zip.toFile())) {
                check(in.getInputStream(in.getEntry("world.txt")).readAllBytes().length == 3, "ZIP read");
            }
        } finally {
            try (var files = Files.walk(dir)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        udp(StandardProtocolFamily.INET, "127.0.0.1");
        udp(StandardProtocolFamily.INET6, "::1");
        check(SSLContext.getDefault().getSupportedSSLParameters().getProtocols().length > 0, "TLS initialization");
        BufferedImage image = new BufferedImage(240, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawString("Java on Mavericks", 10, 30);
        graphics.dispose();
        if (gui) {
            SwingUtilities.invokeAndWait(() -> {
                JFrame frame = new JFrame("Mavericks Java smoke test");
                frame.add(new JLabel(new ImageIcon(image)));
                frame.pack();
                frame.setVisible(true);
                new javax.swing.Timer(2000, event -> frame.dispose()) {{ setRepeats(false); }}.start();
            });
        }
        System.out.println("PASS: clocks, virtual threads, files, ZIP, IPv4/IPv6 UDP, TLS initialization, fonts" +
                           (gui ? ", GUI requested" : ""));
    }
}

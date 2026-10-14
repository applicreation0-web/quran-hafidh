import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Dependency-free JDK 17 tool for the private Quran Haafidh Amorces corpus.
 *
 * The key is a raw 32-byte random secret and is never stored in the container.
 * The fixed header and context string are authenticated as GCM associated data.
 */
public final class AmorceAeadTool {
    private static final byte[] MAGIC = new byte[] {'Q','H','A','E','A','D','0','1'};
    private static final byte[] AAD = (
        "quran-haafidh|ibn-kathir-amorces|m7b-final|1903|1708|195|8|2026-10-13"
    ).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final long MAX_INPUT_BYTES = 16L * 1024L * 1024L;

    private AmorceAeadTool() {}

    public static void main(String[] args) {
        try {
            if (args.length == 2 && "keygen".equals(args[0])) {
                keygen(Path.of(args[1]));
            } else if (args.length == 4 && "encrypt".equals(args[0])) {
                encrypt(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
            } else if (args.length == 4 && "decrypt".equals(args[0])) {
                decrypt(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
            } else {
                throw new IllegalArgumentException(
                    "Usage: keygen KEY | encrypt KEY INPUT OUTPUT | decrypt KEY INPUT OUTPUT"
                );
            }
        } catch (AEADBadTagException badTag) {
            System.err.println("STOP: authentication failed; wrong key or modified container");
            System.exit(3);
        } catch (Exception failure) {
            System.err.println("STOP: " + failure.getMessage());
            System.exit(2);
        }
    }

    private static void keygen(Path target) throws Exception {
        byte[] key = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(key);
        Files.write(target, key, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try {
            Files.setPosixFilePermissions(target, Set.of(
                java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
            ));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX platforms still receive a create-new file; protect it manually there.
        }
        Arrays.fill(key, (byte) 0);
        System.out.println("PASS: generated a new private 256-bit key");
    }

    private static void encrypt(Path keyPath, Path input, Path output) throws Exception {
        byte[] key = readKey(keyPath);
        byte[] plaintext = readBounded(input);
        byte[] nonce = new byte[NONCE_BYTES];
        new SecureRandom().nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(authenticatedHeader(nonce));
        byte[] encrypted = cipher.doFinal(plaintext);
        ByteBuffer container = ByteBuffer.allocate(MAGIC.length + NONCE_BYTES + encrypted.length);
        container.put(MAGIC).put(nonce).put(encrypted);
        writeNewAtomically(output, container.array());
        Arrays.fill(key, (byte) 0);
        Arrays.fill(plaintext, (byte) 0);
        System.out.println("PASS: authenticated-encrypted container created");
    }

    private static void decrypt(Path keyPath, Path input, Path output) throws Exception {
        byte[] key = readKey(keyPath);
        byte[] container = readBounded(input);
        if (container.length < MAGIC.length + NONCE_BYTES + 16) {
            throw new IllegalArgumentException("container is truncated");
        }
        byte[] magic = Arrays.copyOfRange(container, 0, MAGIC.length);
        if (!Arrays.equals(MAGIC, magic)) throw new IllegalArgumentException("wrong container magic");
        byte[] nonce = Arrays.copyOfRange(container, MAGIC.length, MAGIC.length + NONCE_BYTES);
        byte[] encrypted = Arrays.copyOfRange(container, MAGIC.length + NONCE_BYTES, container.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(authenticatedHeader(nonce));
        byte[] plaintext;
        try {
            plaintext = cipher.doFinal(encrypted);
        } catch (AEADBadTagException badTag) {
            throw badTag;
        }
        writeNewAtomically(output, plaintext);
        Arrays.fill(key, (byte) 0);
        Arrays.fill(plaintext, (byte) 0);
        System.out.println("PASS: container authenticated and decrypted");
    }

    private static byte[] authenticatedHeader(byte[] nonce) {
        ByteBuffer data = ByteBuffer.allocate(MAGIC.length + nonce.length + AAD.length);
        return data.put(MAGIC).put(nonce).put(AAD).array();
    }

    private static byte[] readKey(Path path) throws IOException {
        byte[] key = Files.readAllBytes(path);
        if (key.length != KEY_BYTES) throw new IllegalArgumentException("key must contain exactly 32 bytes");
        return key;
    }

    private static byte[] readBounded(Path path) throws IOException {
        long size = Files.size(path);
        if (size < 1 || size > MAX_INPUT_BYTES) {
            throw new IllegalArgumentException("input size outside the 1..16 MiB bound");
        }
        return Files.readAllBytes(path);
    }

    private static void writeNewAtomically(Path output, byte[] content) throws IOException {
        Path absolute = output.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) throw new IllegalArgumentException("output must have a parent directory");
        Files.createDirectories(parent);
        if (Files.exists(absolute)) throw new IllegalArgumentException("output already exists: " + absolute);
        Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            Files.write(temporary, content, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, absolute);
            }
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
    }
}

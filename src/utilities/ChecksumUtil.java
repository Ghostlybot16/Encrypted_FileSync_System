package utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Utility class for calculating file checksum.
 *
 * This class currently supports SHA-256 checksum generation.
 * which is used to verify file integrity after transfer and decryption.
 */
public class ChecksumUtil {
    /**
     * Calculates the SHA-256 checksum of a file.
     *
     * @param file file whose checksum should be calculated.
     * @return SHA-256 checksum represented as a hexadecimal string.
     * @throws Exception if the file cannot be read or the hashing algorithm fails
     */
    public static String calculateChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] checksumBytes = digest.digest();
        StringBuilder checksum = new StringBuilder();

        for (byte b : checksumBytes) {
            checksum.append(String.format("%02x", b));
        }

        return checksum.toString();
    }
}
package com.tadeasfort.steganomessages.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class DCTSteganographyService {

    private static final String MESSAGE_DELIMITER = "###END_OF_MESSAGE###";
    private static final int BLOCK_SIZE = 8;
    private static final double QUANTIZATION_FACTOR = 50.0; // Embedding strength for DCT coefficients

    // Use single AC coefficient position for simplicity and reliability
    private static final int[] EMBED_POSITION = { 2, 1 }; // Second AC coefficient

    // Precomputed DCT coefficient matrices for 8x8 blocks (JPEG standard)
    private static final double[][] DCT_MATRIX = computeDCTMatrix();
    private static final double[][] IDCT_MATRIX = computeIDCTMatrix();

    /**
     * Embeds a secret message into an image using simple spaced-out static
     * positions
     */
    public BufferedImage embedMessage(BufferedImage image, String message) {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        String messageWithDelimiter = message + MESSAGE_DELIMITER;
        byte[] messageBytes = messageWithDelimiter.getBytes(StandardCharsets.UTF_8);

        // Convert message to binary string
        StringBuilder binaryMessage = new StringBuilder();
        for (byte b : messageBytes) {
            binaryMessage.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        // Generate static sequential embedding sequence
        List<EmbedPosition> embedSequence = generateStaticSequence(image, binaryMessage.length());

        log.debug(
                "Embedding message: '{}' + delimiter = {} bytes, {} bits into image {}x{} using {} sequential positions",
                message, messageBytes.length, binaryMessage.length(), image.getWidth(), image.getHeight(),
                embedSequence.size());
        log.debug("Binary message (first 64 bits): {}",
                binaryMessage.length() >= 64 ? binaryMessage.substring(0, 64) : binaryMessage.toString());

        if (binaryMessage.length() > embedSequence.size()) {
            throw new IllegalArgumentException("Message too long for this image. Maximum capacity: " +
                    (embedSequence.size() / 8) + " characters, required: " + (binaryMessage.length() / 8)
                    + " characters");
        }

        BufferedImage stegoImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Copy the entire original image
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                stegoImage.setRGB(x, y, image.getRGB(x, y));
            }
        }

        // Embed bits using the static sequential sequence
        for (int i = 0; i < binaryMessage.length(); i++) {
            EmbedPosition pos = embedSequence.get(i);
            char bit = binaryMessage.charAt(i);
            embedBitInBlock(image, stegoImage, pos.blockX, pos.blockY, pos.coeffX, pos.coeffY, bit);

            // Debug: log first few bits
            if (i < 32) {
                log.debug("Embedded bit {} at position {} (block {},{} coeff {},{})",
                        bit, i, pos.blockX, pos.blockY, pos.coeffX, pos.coeffY);
            }
        }

        log.info("Successfully embedded {} bits using sequential DCT positions", binaryMessage.length());
        return stegoImage;
    }

    /**
     * Extracts a hidden message using the same sequential sequence
     */
    public String extractMessage(BufferedImage image) {
        StringBuilder binaryMessage = new StringBuilder();

        // Generate the same sequential embedding sequence
        // We'll extract up to a reasonable limit to find the delimiter
        int maxBitsToTry = Math.min(10000, getMaxPossibleEmbedPositions(image)); // Lower limit for efficiency
        List<EmbedPosition> embedSequence = generateStaticSequence(image, maxBitsToTry);

        log.debug("Starting message extraction from image {}x{} using {} sequential positions",
                image.getWidth(), image.getHeight(), embedSequence.size());

        // Extract bits using the same sequence
        for (int i = 0; i < embedSequence.size(); i++) {
            EmbedPosition pos = embedSequence.get(i);
            char bit = extractBitFromBlock(image, pos.blockX, pos.blockY, pos.coeffX, pos.coeffY);
            binaryMessage.append(bit);

            // Debug: log first few bits
            if (i < 32) {
                log.debug("Extracted bit {} at position {} (block {},{} coeff {},{})",
                        bit, i, pos.blockX, pos.blockY, pos.coeffX, pos.coeffY);
            }

            // Check for delimiter every 8 bits (complete bytes)
            if (binaryMessage.length() % 8 == 0 && binaryMessage.length() >= MESSAGE_DELIMITER.length() * 8) {
                try {
                    String currentMessage = binaryStringToText(binaryMessage.toString());
                    if (currentMessage.contains(MESSAGE_DELIMITER)) {
                        int delimiterIndex = currentMessage.indexOf(MESSAGE_DELIMITER);
                        String extractedMessage = currentMessage.substring(0, delimiterIndex);
                        log.info("Successfully extracted message of {} characters after {} bits",
                                extractedMessage.length(), binaryMessage.length());
                        return extractedMessage;
                    }
                } catch (Exception e) {
                    log.debug("Error converting binary to text at {} bits: {}", binaryMessage.length(), e.getMessage());
                }
            }
        }

        log.warn("No message delimiter found after extracting {} bits", binaryMessage.length());

        // Debug output
        if (binaryMessage.length() >= 8) {
            try {
                int completeBytes = (binaryMessage.length() / 8) * 8;
                String partialBinary = binaryMessage.substring(0, completeBytes);
                String partialText = binaryStringToText(partialBinary);

                String cleanText = partialText.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "?");
                log.debug("Partial text extracted ({} bytes): '{}'",
                        partialBinary.length() / 8,
                        cleanText.length() > 100 ? cleanText.substring(0, 100) + "..." : cleanText);
                log.debug("Binary (first 64 bits): {}",
                        binaryMessage.length() >= 64 ? binaryMessage.substring(0, 64) : binaryMessage.toString());
            } catch (Exception e) {
                log.debug("Error converting partial binary to text: {}", e.getMessage());
            }
        }

        throw new IllegalArgumentException("No hidden message found in the image");
    }

    /**
     * Generates a static sequential sequence of embedding positions
     * This uses a predictable sequential pattern across the image
     */
    private List<EmbedPosition> generateStaticSequence(BufferedImage image, int bitsNeeded) {
        List<EmbedPosition> allPositions = new ArrayList<>();

        // Generate all possible embedding positions in a predictable sequential order
        for (int blockY = 0; blockY < image.getHeight() - BLOCK_SIZE + 1; blockY += BLOCK_SIZE) {
            for (int blockX = 0; blockX < image.getWidth() - BLOCK_SIZE + 1; blockX += BLOCK_SIZE) {
                allPositions.add(new EmbedPosition(blockX, blockY, EMBED_POSITION[0], EMBED_POSITION[1]));
            }
        }

        // Return the first N positions (sequential, predictable)
        int positionsToReturn = Math.min(bitsNeeded, allPositions.size());
        List<EmbedPosition> result = allPositions.subList(0, positionsToReturn);

        log.debug("Generated {} sequential embed positions from {} total possible positions",
                result.size(), allPositions.size());

        return result;
    }

    /**
     * Calculate maximum possible embedding positions
     */
    private int getMaxPossibleEmbedPositions(BufferedImage image) {
        int blocksX = (image.getWidth() / BLOCK_SIZE);
        int blocksY = (image.getHeight() / BLOCK_SIZE);
        return blocksX * blocksY; // Only one coefficient per block now
    }

    /**
     * Class to hold embedding position information
     */
    private static class EmbedPosition {
        final int blockX, blockY, coeffX, coeffY;

        EmbedPosition(int blockX, int blockY, int coeffX, int coeffY) {
            this.blockX = blockX;
            this.blockY = blockY;
            this.coeffX = coeffX;
            this.coeffY = coeffY;
        }

        @Override
        public String toString() {
            return String.format("Block(%d,%d) Coeff(%d,%d)", blockX, blockY, coeffX, coeffY);
        }
    }

    /**
     * Calculates the maximum message length that can be embedded
     */
    public int getMaxMessageLength(BufferedImage image) {
        int maxPositions = getMaxPossibleEmbedPositions(image);
        return (maxPositions / 8) - MESSAGE_DELIMITER.length();
    }

    /**
     * Embeds a single bit in a specific DCT coefficient
     */
    private void embedBitInBlock(BufferedImage source, BufferedImage dest, int blockX, int blockY, int coeffX,
            int coeffY, char bit) {
        try {
            // Extract 8x8 block (grayscale)
            double[][] block = extractGrayscaleBlock(source, blockX, blockY);

            // Apply DCT
            double[][] dctBlock = applyDCT(block);

            // Get the specific AC coefficient
            double coefficient = dctBlock[coeffX][coeffY];

            // Handle invalid values
            if (Double.isNaN(coefficient) || Double.isInfinite(coefficient)) {
                log.warn("Invalid DCT coefficient at block ({},{}) coeff ({},{}) - skipping",
                        blockX, blockY, coeffX, coeffY);
                copyBlock(source, dest, blockX, blockY);
                return;
            }

            // Robust DCT steganography: force coefficient to specific ranges
            int bitValue = bit - '0';
            double alpha = QUANTIZATION_FACTOR; // Embedding strength

            if (bitValue == 1) {
                // Embed bit 1: force coefficient to be >= alpha (positive range)
                dctBlock[coeffX][coeffY] = Math.max(alpha, Math.abs(coefficient));
            } else {
                // Embed bit 0: force coefficient to be <= -alpha (negative range)
                dctBlock[coeffX][coeffY] = -Math.max(alpha, Math.abs(coefficient));
            }

            // Apply inverse DCT
            double[][] modifiedBlock = applyIDCT(dctBlock);

            // Copy modified block to destination
            copyBlockToImageAsRGB(modifiedBlock, dest, blockX, blockY);

        } catch (Exception e) {
            log.warn("Error embedding bit in block at ({},{}): {} - copying original",
                    blockX, blockY, e.getMessage());
            copyBlock(source, dest, blockX, blockY);
        }
    }

    /**
     * Extracts a single bit from a specific DCT coefficient
     */
    private char extractBitFromBlock(BufferedImage image, int blockX, int blockY, int coeffX, int coeffY) {
        try {
            // Extract 8x8 block (grayscale)
            double[][] block = extractGrayscaleBlock(image, blockX, blockY);

            // Apply DCT
            double[][] dctBlock = applyDCT(block);

            // Get the specific AC coefficient
            double coefficient = dctBlock[coeffX][coeffY];

            // Handle invalid values
            if (Double.isNaN(coefficient) || Double.isInfinite(coefficient)) {
                return '0'; // Default fallback
            }

            // Extract bit based on coefficient sign - simple and robust
            // Since embedding forces coefficients to be either positive (1) or negative (0)
            return (coefficient >= 0) ? '1' : '0';

        } catch (Exception e) {
            log.warn("Error extracting bit from block at ({},{}): {} - returning 0",
                    blockX, blockY, e.getMessage());
            return '0';
        }
    }

    private double[][] extractGrayscaleBlock(BufferedImage image, int startX, int startY) {
        double[][] block = new double[BLOCK_SIZE][BLOCK_SIZE];

        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                int pixelX = Math.min(startX + x, image.getWidth() - 1);
                int pixelY = Math.min(startY + y, image.getHeight() - 1);

                int rgb = image.getRGB(pixelX, pixelY);
                int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) +
                        0.587 * ((rgb >> 8) & 0xFF) +
                        0.114 * (rgb & 0xFF));
                block[y][x] = gray - 128; // Center around 0 for DCT
            }
        }

        return block;
    }

    private void copyBlockToImageAsRGB(double[][] block, BufferedImage image, int startX, int startY) {
        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                int pixelX = Math.min(startX + x, image.getWidth() - 1);
                int pixelY = Math.min(startY + y, image.getHeight() - 1);

                int gray = (int) Math.max(0, Math.min(255, Math.round(block[y][x] + 128)));
                int rgb = (gray << 16) | (gray << 8) | gray;
                image.setRGB(pixelX, pixelY, rgb);
            }
        }
    }

    private void copyBlock(BufferedImage source, BufferedImage dest, int startX, int startY) {
        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                int pixelX = Math.min(startX + x, source.getWidth() - 1);
                int pixelY = Math.min(startY + y, source.getHeight() - 1);
                dest.setRGB(pixelX, pixelY, source.getRGB(pixelX, pixelY));
            }
        }
    }

    private double[][] applyDCT(double[][] block) {
        return matrixMultiply(matrixMultiply(DCT_MATRIX, block), transposeMatrix(DCT_MATRIX));
    }

    private double[][] applyIDCT(double[][] dct) {
        return matrixMultiply(matrixMultiply(IDCT_MATRIX, dct), transposeMatrix(IDCT_MATRIX));
    }

    private static double[][] computeDCTMatrix() {
        double[][] matrix = new double[BLOCK_SIZE][BLOCK_SIZE];

        for (int i = 0; i < BLOCK_SIZE; i++) {
            for (int j = 0; j < BLOCK_SIZE; j++) {
                double ci = (i == 0) ? 1.0 / Math.sqrt(2.0) : 1.0;
                matrix[i][j] = ci * Math.sqrt(2.0 / BLOCK_SIZE) *
                        Math.cos((2 * j + 1) * i * Math.PI / (2 * BLOCK_SIZE));
            }
        }

        return matrix;
    }

    private static double[][] computeIDCTMatrix() {
        return transposeMatrix(computeDCTMatrix());
    }

    private static double[][] matrixMultiply(double[][] a, double[][] b) {
        int rows = a.length;
        int cols = b[0].length;
        int common = b.length;

        double[][] result = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                for (int k = 0; k < common; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }

        return result;
    }

    private static double[][] transposeMatrix(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] transposed = new double[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }

        return transposed;
    }

    private String binaryStringToText(String binary) {
        StringBuilder result = new StringBuilder();

        try {
            for (int i = 0; i < binary.length(); i += 8) {
                if (i + 8 <= binary.length()) {
                    String byteString = binary.substring(i, i + 8);
                    int charCode = Integer.parseInt(byteString, 2);

                    if (charCode >= 0 && charCode <= 255) {
                        result.append((char) charCode);
                    } else {
                        result.append('?');
                    }
                }
            }
        } catch (NumberFormatException e) {
            log.error("Error parsing binary string: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid binary string format", e);
        }

        return result.toString();
    }

    public void saveImage(BufferedImage image, String format, String filePath) throws IOException {
        File outputFile = new File(filePath);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(image, format, outputFile);
        log.info("DCT steganography image saved to: {}", filePath);
    }

    public BufferedImage loadImage(String filePath) throws IOException {
        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            throw new FileNotFoundException("Image file not found: " + filePath);
        }
        return ImageIO.read(imageFile);
    }

    public BufferedImage loadImage(InputStream inputStream) throws IOException {
        return ImageIO.read(inputStream);
    }
}

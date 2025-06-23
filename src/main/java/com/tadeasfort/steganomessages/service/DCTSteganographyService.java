package com.tadeasfort.steganomessages.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class DCTSteganographyService {

    private static final String MESSAGE_DELIMITER = "###END_OF_MESSAGE###";
    private static final int BLOCK_SIZE = 8;
    private static final double ALPHA = 10.0; // Increased embedding strength

    // Use middle-frequency AC coefficients for better robustness
    // These are the positions in the DCT matrix that are less sensitive to
    // compression
    private static final int[][] EMBED_POSITIONS = {
            { 1, 1 }, { 1, 2 }, { 2, 1 }, { 2, 2 }, { 1, 3 }, { 3, 1 }, { 2, 3 }, { 3, 2 }
    };
    // Precomputed DCT coefficient matrices for 8x8 blocks (JPEG standard)
    private static final double[][] DCT_MATRIX = computeDCTMatrix();
    private static final double[][] IDCT_MATRIX = computeIDCTMatrix();

    /**
     * Embeds a secret message into an image using DCT steganography
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

        // Check if image can accommodate the message
        int totalBlocks = (image.getWidth() / BLOCK_SIZE) * (image.getHeight() / BLOCK_SIZE);
        int availablePositions = totalBlocks * EMBED_POSITIONS.length;
        int requiredBits = binaryMessage.length();

        log.debug(
                "Embedding message: '{}' + delimiter = {} bytes, {} bits into image {}x{} with {} blocks, {} available positions",
                message, messageBytes.length, binaryMessage.length(), image.getWidth(), image.getHeight(), totalBlocks,
                availablePositions);

        if (requiredBits > availablePositions) {
            throw new IllegalArgumentException("Message too long for this image. Maximum capacity: " +
                    (availablePositions / 8) + " characters");
        }

        BufferedImage stegoImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Process image in 8x8 blocks
        int messageIndex = 0;
        for (int y = 0; y < image.getHeight() - BLOCK_SIZE + 1; y += BLOCK_SIZE) {
            for (int x = 0; x < image.getWidth() - BLOCK_SIZE + 1; x += BLOCK_SIZE) {
                if (messageIndex >= binaryMessage.length()) {
                    // Copy remaining blocks without modification
                    copyBlock(image, stegoImage, x, y);
                } else {
                    // Embed as many bits as possible in this block
                    StringBuilder blockBits = new StringBuilder();
                    for (int i = 0; i < EMBED_POSITIONS.length && messageIndex < binaryMessage.length(); i++) {
                        blockBits.append(binaryMessage.charAt(messageIndex));
                        messageIndex++;
                    }
                    embedBitsInBlock(image, stegoImage, x, y, blockBits.toString());
                }
            }
        }

        // Copy any remaining pixels that don't fit in complete 8x8 blocks
        copyRemainingPixels(image, stegoImage);

        log.info("Successfully embedded message of {} bits into image using DCT steganography", binaryMessage.length());
        log.debug("Binary message: {}", binaryMessage.substring(0, Math.min(64, binaryMessage.length())));
        return stegoImage;
    }

    /**
     * Extracts a hidden message from an image using DCT steganography
     */
    public String extractMessage(BufferedImage image) {
        StringBuilder binaryMessage = new StringBuilder();
        int bitsExtracted = 0;
        int totalBlocks = ((image.getHeight() / BLOCK_SIZE) * (image.getWidth() / BLOCK_SIZE));
        log.debug("Starting message extraction from image {}x{}, total blocks: {}",
                image.getWidth(), image.getHeight(), totalBlocks);

        // Process image in 8x8 blocks
        for (int y = 0; y < image.getHeight() - BLOCK_SIZE + 1; y += BLOCK_SIZE) {
            for (int x = 0; x < image.getWidth() - BLOCK_SIZE + 1; x += BLOCK_SIZE) {
                String blockBits = extractBitsFromBlock(image, x, y);
                binaryMessage.append(blockBits);
                bitsExtracted += blockBits.length();

                // Check if we have enough bits for characters and if we've reached the
                // delimiter
                if (binaryMessage.length() % 8 == 0 && binaryMessage.length() >= 8) {
                    String currentMessage = binaryStringToText(binaryMessage.toString());
                    if (currentMessage.endsWith(MESSAGE_DELIMITER)) {
                        String extractedMessage = currentMessage.substring(0,
                                currentMessage.length() - MESSAGE_DELIMITER.length());
                        log.info(
                                "Successfully extracted message of {} characters using DCT steganography after {} bits",
                                extractedMessage.length(), bitsExtracted);
                        return extractedMessage;
                    }
                }

                // Safety check - if we've extracted way more bits than reasonable, stop
                if (bitsExtracted > totalBlocks * EMBED_POSITIONS.length) {
                    log.warn("Extracted {} bits from {} total positions without finding delimiter",
                            bitsExtracted, totalBlocks * EMBED_POSITIONS.length);
                    break;
                }
            }
        }

        log.warn("No message delimiter found after extracting {} bits from {} blocks. First 64 bits: {}",
                bitsExtracted, totalBlocks,
                binaryMessage.length() >= 64 ? binaryMessage.substring(0, 64) : binaryMessage.toString());

        // Try to convert what we have to see if there's partial text
        if (binaryMessage.length() >= 8) {
            String partialText = binaryStringToText(binaryMessage.substring(0, (binaryMessage.length() / 8) * 8));
            log.debug("Partial text extracted: '{}'", partialText.replaceAll("[\\p{Cntrl}]", "?"));
        }

        throw new IllegalArgumentException("No hidden message found in the image");
    }

    /**
     * Calculates the maximum message length that can be embedded in an image
     */
    public int getMaxMessageLength(BufferedImage image) {
        int totalBlocks = (image.getWidth() / BLOCK_SIZE) * (image.getHeight() / BLOCK_SIZE);
        int availablePositions = totalBlocks * EMBED_POSITIONS.length;
        return (availablePositions / 8) - MESSAGE_DELIMITER.length();
    }

    private void embedBitsInBlock(BufferedImage source, BufferedImage dest, int startX, int startY, String bits) {
        // Extract 8x8 block
        double[][] block = extractBlock(source, startX, startY);

        // Apply DCT
        double[][] dctBlock = applyDCT(block);

        // Embed bits using multiple AC coefficients
        for (int i = 0; i < bits.length() && i < EMBED_POSITIONS.length; i++) {
            int posX = EMBED_POSITIONS[i][0];
            int posY = EMBED_POSITIONS[i][1];
            char bit = bits.charAt(i);

            double coefficient = dctBlock[posX][posY];
            int bitValue = bit - '0';

            // Quantization-based embedding with stronger modification
            double quantizedCoeff = Math.round(coefficient / ALPHA);

            // Ensure the coefficient has the desired parity
            if (bitValue == 1) {
                // We want odd parity for bit 1
                if ((int) quantizedCoeff % 2 == 0) {
                    quantizedCoeff = quantizedCoeff + (quantizedCoeff >= 0 ? 1 : -1);
                }
            } else {
                // We want even parity for bit 0
                if ((int) quantizedCoeff % 2 == 1) {
                    quantizedCoeff = quantizedCoeff + (quantizedCoeff >= 0 ? 1 : -1);
                }
            }

            // Apply the modified coefficient back
            dctBlock[posX][posY] = quantizedCoeff * ALPHA;
        }

        // Apply inverse DCT
        double[][] modifiedBlock = applyIDCT(dctBlock);

        // Copy modified block to destination
        copyBlockToImage(modifiedBlock, dest, startX, startY);
    }

    private String extractBitsFromBlock(BufferedImage image, int startX, int startY) {
        // Extract 8x8 block
        double[][] block = extractBlock(image, startX, startY);

        // Apply DCT
        double[][] dctBlock = applyDCT(block);

        StringBuilder bits = new StringBuilder();

        // Extract bits from AC coefficients
        for (int i = 0; i < EMBED_POSITIONS.length; i++) {
            int posX = EMBED_POSITIONS[i][0];
            int posY = EMBED_POSITIONS[i][1];

            double coefficient = dctBlock[posX][posY];
            double quantizedCoeff = Math.round(coefficient / ALPHA);

            // Extract bit based on parity
            char extractedBit = (int) quantizedCoeff % 2 == 1 ? '1' : '0';
            bits.append(extractedBit);
        }

        return bits.toString();
    }

    private double[][] extractBlock(BufferedImage image, int startX, int startY) {
        double[][] block = new double[BLOCK_SIZE][BLOCK_SIZE];

        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                int pixelX = Math.min(startX + x, image.getWidth() - 1);
                int pixelY = Math.min(startY + y, image.getHeight() - 1);

                int rgb = image.getRGB(pixelX, pixelY);
                // Use luminance component for better compatibility
                int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) +
                        0.587 * ((rgb >> 8) & 0xFF) +
                        0.114 * (rgb & 0xFF));
                block[y][x] = gray - 128; // Center around 0 for DCT
            }
        }

        return block;
    }

    private void copyBlockToImage(double[][] block, BufferedImage image, int startX, int startY) {
        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                int pixelX = Math.min(startX + x, image.getWidth() - 1);
                int pixelY = Math.min(startY + y, image.getHeight() - 1);

                // Clamp and convert back to RGB
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

    private void copyRemainingPixels(BufferedImage source, BufferedImage dest) {
        // Copy pixels that don't fit in complete 8x8 blocks
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (x >= (source.getWidth() / BLOCK_SIZE) * BLOCK_SIZE ||
                        y >= (source.getHeight() / BLOCK_SIZE) * BLOCK_SIZE) {
                    dest.setRGB(x, y, source.getRGB(x, y));
                }
            }
        }
    }

    private double[][] applyDCT(double[][] block) {
        return matrixMultiply(matrixMultiply(DCT_MATRIX, block), transposeMatrix(DCT_MATRIX));
    }

    private double[][] applyIDCT(double[][] dct) {
        return matrixMultiply(matrixMultiply(IDCT_MATRIX, dct), transposeMatrix(IDCT_MATRIX));
    }

    /**
     * Computes the DCT coefficient matrix using JPEG standard
     */
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

    /**
     * Computes the IDCT coefficient matrix (transpose of DCT matrix)
     */
    private static double[][] computeIDCTMatrix() {
        return transposeMatrix(computeDCTMatrix());
    }

    /**
     * Matrix multiplication for DCT operations
     */
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

    /**
     * Matrix transpose operation
     */
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

        for (int i = 0; i < binary.length(); i += 8) {
            if (i + 8 <= binary.length()) {
                String byteString = binary.substring(i, i + 8);
                int charCode = Integer.parseInt(byteString, 2);
                result.append((char) charCode);
            }
        }

        return result.toString();
    }

    /**
     * Saves a BufferedImage to a file
     */
    public void saveImage(BufferedImage image, String format, String filePath) throws IOException {
        File outputFile = new File(filePath);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(image, format, outputFile);
        log.info("DCT steganography image saved to: {}", filePath);
    }

    /**
     * Loads an image from a file
     */
    public BufferedImage loadImage(String filePath) throws IOException {
        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            throw new FileNotFoundException("Image file not found: " + filePath);
        }
        return ImageIO.read(imageFile);
    }

    /**
     * Loads an image from InputStream
     */
    public BufferedImage loadImage(InputStream inputStream) throws IOException {
        return ImageIO.read(inputStream);
    }
}

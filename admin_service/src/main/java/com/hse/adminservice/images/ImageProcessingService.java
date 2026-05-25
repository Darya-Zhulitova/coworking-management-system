package com.hse.adminservice.images;

import com.hse.adminservice.common.error.ConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
public class ImageProcessingService {
    private static final int FLOOR_MAX_WIDTH = 1920;
    private static final int FLOOR_MAX_HEIGHT = 1080;
    private static final int COWORKING_MAX_WIDTH = 1920;
    private static final int COWORKING_MAX_HEIGHT = 1080;
    private static final int PLACE_PREVIEW_MAX_WIDTH = 960;
    private static final int PLACE_PREVIEW_MAX_HEIGHT = 720;
    private static final double COWORKING_RATIO = 16D / 9D;
    private static final double PLACE_PREVIEW_RATIO = 4D / 3D;

    public ImageProcessingResult processFloorPlan(byte[] sourceBytes) {
        BufferedImage source = readImage(sourceBytes);
        return new ImageProcessingResult(toJpegBytes(fitInside(source, FLOOR_MAX_WIDTH, FLOOR_MAX_HEIGHT)), null);
    }

    public ImageProcessingResult processCoworkingPhoto(byte[] sourceBytes) {
        BufferedImage source = readImage(sourceBytes);
        return new ImageProcessingResult(
                toJpegBytes(resizeThenCrop(
                        source,
                        COWORKING_MAX_WIDTH,
                        COWORKING_MAX_HEIGHT,
                        COWORKING_RATIO
                )), null
        );
    }

    public ImageProcessingResult processPlacePhoto(byte[] sourceBytes) {
        BufferedImage source = readImage(sourceBytes);
        return new ImageProcessingResult(
                toJpegBytes(copyAsRgb(source, source.getWidth(), source.getHeight())),
                toJpegBytes(resizeThenCrop(
                        source,
                        PLACE_PREVIEW_MAX_WIDTH,
                        PLACE_PREVIEW_MAX_HEIGHT,
                        PLACE_PREVIEW_RATIO
                ))
        );
    }

    private BufferedImage readImage(byte[] sourceBytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceBytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new ConflictException("Загрузите изображение JPG, PNG или WEBP.");
            }
            return image;
        } catch (IOException exception) {
            log.error("Failed to read uploaded image", exception);
            throw new ConflictException("Загрузите изображение JPG, PNG или WEBP.");
        }
    }

    private BufferedImage fitInside(BufferedImage source, int maxWidth, int maxHeight) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        double scale = Math.min(1D, Math.min(maxWidth / (double) sourceWidth, maxHeight / (double) sourceHeight));
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        return copyAsRgb(source, targetWidth, targetHeight);
    }

    private BufferedImage resizeThenCrop(BufferedImage source, int maxWidth, int maxHeight, double targetRatio) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        double scale = Math.min(1D, Math.max(maxWidth / (double) sourceWidth, maxHeight / (double) sourceHeight));
        int resizedWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int resizedHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        BufferedImage resized = copyAsRgb(source, resizedWidth, resizedHeight);

        int cropWidth = resizedWidth;
        int cropHeight = resizedHeight;
        if (resizedWidth / (double) resizedHeight > targetRatio) {
            cropWidth = Math.max(1, (int) Math.round(resizedHeight * targetRatio));
        } else {
            cropHeight = Math.max(1, (int) Math.round(resizedWidth / targetRatio));
        }
        cropWidth = Math.min(cropWidth, maxWidth);
        cropHeight = Math.min(cropHeight, maxHeight);
        if (cropWidth / (double) cropHeight > targetRatio) {
            cropWidth = Math.max(1, (int) Math.round(cropHeight * targetRatio));
        } else if (cropWidth / (double) cropHeight < targetRatio) {
            cropHeight = Math.max(1, (int) Math.round(cropWidth / targetRatio));
        }

        int x = Math.max(0, (resizedWidth - cropWidth) / 2);
        int y = Math.max(0, (resizedHeight - cropHeight) / 2);
        BufferedImage cropped = resized.getSubimage(x, y, cropWidth, cropHeight);
        return copyAsRgb(cropped, cropped.getWidth(), cropped.getHeight());
    }

    private BufferedImage copyAsRgb(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
            return target;
        } finally {
            graphics.dispose();
        }
    }

    private byte[] toJpegBytes(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "jpg", outputStream)) {
                throw new ConflictException("Не удалось обработать изображение. Загрузите другой файл.");
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            log.error("Failed to write JPEG image", exception);
            throw new ConflictException("Не удалось обработать изображение. Загрузите другой файл.");
        }
    }
}

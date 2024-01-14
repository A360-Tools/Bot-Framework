package com.automationanywhere.botcommand.utilities.screen;

import com.automationanywhere.botcommand.exception.BotCommandException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CaptureScreen {

    public static void captureDesktop(String filePath, boolean isOverwriteFile) {
        try {
            if (StringUtils.isBlank(filePath)) {
                throw new BotCommandException("Empty screenshot path provided");
            }

            Path path = Paths.get(filePath);
            File file = path.toFile();

            if (!isOverwriteFile && file.exists()) {
                throw new BotCommandException("Screenshot path already exists " + path);
            }


            Robot robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRectangle = new Rectangle(screenSize);
            BufferedImage image = robot.createScreenCapture(screenRectangle);
            String extension = FilenameUtils.getExtension(filePath);
            saveFile(image, extension, file);
        } catch (Exception ex) {
            throw new BotCommandException("Unable to save screenshot " + ex.getMessage());
        }
    }

    private static void saveFile(BufferedImage screenCapture, String extension, File fileToSave) throws IOException {
        if ("wmf".equalsIgnoreCase(extension)) {
            extension = "png";
        }

        ImageIO.write(screenCapture, extension, fileToSave);
    }
}


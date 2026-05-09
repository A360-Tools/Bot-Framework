package screen.process;

import org.testng.SkipException;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

/**
 * Test-only helper that decides whether the current JVM has an interactive
 * desktop available for screen-capture tests. Use {@link #requireDesktop()}
 * in {@code @BeforeClass} of any test that depends on real {@code gdigrab}.
 */
public final class DesktopAvailability {

    private static final boolean AVAILABLE = detect();

    private DesktopAvailability() {
    }

    private static boolean detect() {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        try {
            BufferedImage img = new Robot().createScreenCapture(new Rectangle(1, 1));
            return img != null && img.getWidth() > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /** Skips the calling test (via TestNG's {@link SkipException}) when no desktop is available. */
    public static void requireDesktop() {
        if (!AVAILABLE) {
            throw new SkipException(
                    "skipped: no interactive desktop available for screen capture");
        }
    }
}

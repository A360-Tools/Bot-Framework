package screen;

import com.automationanywhere.botcommand.utilities.logger.HTMLGenerator;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HTMLScreenCellTest {

    @Test
    public void videoTakesPriorityOverScreenshot() {
        String html = HTMLGenerator.getScreenCellHTML(
                "C:\\logs\\screenshots\\warn_uuid.png",
                "C:\\logs\\clips\\error_uuid.mp4",
                "C:\\logs\\screenshots\\error_uuid.png");

        Assert.assertTrue(html.contains("video-link"),
                "video-link class should be applied when video path is present");
        Assert.assertTrue(html.contains("clips/error_uuid.mp4"),
                "video href should reference the clips/ relative path");
        Assert.assertTrue(html.contains("screenshots/error_uuid.png"),
                "poster should reference the screenshots/ relative path");
        Assert.assertTrue(html.contains("&#9654;") || html.contains("▶"),
                "play overlay glyph should be present");
    }

    @Test
    public void videoOnlyWithoutPoster() {
        String html = HTMLGenerator.getScreenCellHTML("", "C:\\logs\\clips\\x.mp4", "");

        Assert.assertTrue(html.contains("video-link"));
        Assert.assertTrue(html.contains("clips/x.mp4"));
        // No img tag if no poster
        Assert.assertFalse(html.contains("<img"),
                "no poster image expected when poster path is empty");
    }

    @Test
    public void screenshotOnlyFallsBackToExistingRendering() {
        String html = HTMLGenerator.getScreenCellHTML(
                "C:\\logs\\screenshots\\info_uuid.png", "", "");

        Assert.assertTrue(html.contains("img-link"),
                "img-link class should be applied for plain screenshots");
        Assert.assertFalse(html.contains("video-link"),
                "video-link class must not appear for screenshot-only rows");
        Assert.assertTrue(html.contains("screenshots/info_uuid.png"));
    }

    @Test
    public void emptyEverythingProducesEmptyCell() {
        Assert.assertEquals(HTMLGenerator.getScreenCellHTML("", "", ""), "");
        Assert.assertEquals(HTMLGenerator.getScreenCellHTML(null, null, null), "");
    }

    @Test
    public void htmlEscapesPaths() {
        // A path with characters that must be escaped in HTML attributes
        String html = HTMLGenerator.getScreenCellHTML(
                "", "C:\\logs\\clips\\evil&<>.mp4", "C:\\logs\\screenshots\\evil&<>.png");

        // FilenameUtils.getName strips the directory; only the filename
        // appears in the URL. The escaping should turn & into &amp; etc.
        Assert.assertFalse(html.contains("&<"),
                "ampersand-followed-by-less-than should be escaped");
        Assert.assertTrue(html.contains("&amp;") || html.contains("&#"),
                "ampersand in filenames should be HTML-escaped");
    }
}

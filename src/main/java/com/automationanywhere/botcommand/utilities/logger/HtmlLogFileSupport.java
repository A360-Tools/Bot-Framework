package com.automationanywhere.botcommand.utilities.logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class HtmlLogFileSupport {
    static final String FOOTER = "</tbody></table></body></html>";

    private static final Pattern DATA_ROW_PATTERN = Pattern.compile("(?i)<tr><td>");
    private static final Pattern FOOTER_PATTERN = Pattern.compile(
            "(?i)</tbody\\s*>\\s*</table\\s*>\\s*</body\\s*>\\s*</html\\s*>"
    );

    private HtmlLogFileSupport() {
    }

    static int prepareForAppend(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path) || Files.size(path) == 0) {
            return 0;
        }

        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        int entryCount = countDataRows(content);
        String contentWithoutFooter = removeFooters(content);

        if (!contentWithoutFooter.equals(content)) {
            Files.write(path, contentWithoutFooter.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }

        return entryCount;
    }

    static void finalizeLogFiles(String filePath) throws IOException {
        Set<Path> paths = new HashSet<>();
        Path activePath = Paths.get(filePath);
        paths.add(activePath);

        Path directory = activePath.getParent();
        if (directory == null) {
            directory = Paths.get(".");
        }

        String fileName = activePath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
        String extension = extensionIndex >= 0 ? fileName.substring(extensionIndex + 1) : "";
        Pattern rotatedPattern = Pattern.compile("\\d+_" + Pattern.quote(baseName)
                + (extension.isEmpty() ? "" : "\\." + Pattern.quote(extension)));

        if (Files.isDirectory(directory)) {
            try (Stream<Path> stream = Files.list(directory)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> rotatedPattern.matcher(path.getFileName().toString()).matches())
                        .forEach(paths::add);
            }
        }

        for (Path path : paths) {
            finalizeLogFile(path);
        }
    }

    private static void finalizeLogFile(Path path) throws IOException {
        if (!Files.exists(path) || Files.size(path) == 0) {
            return;
        }

        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        String finalizedContent = removeFooters(content) + FOOTER;

        Files.write(path, finalizedContent.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String removeFooters(String content) {
        return FOOTER_PATTERN.matcher(content).replaceAll("");
    }

    private static int countDataRows(String content) {
        int count = 0;
        Matcher matcher = DATA_ROW_PATTERN.matcher(content);

        while (matcher.find()) {
            count++;
        }

        return count;
    }
}

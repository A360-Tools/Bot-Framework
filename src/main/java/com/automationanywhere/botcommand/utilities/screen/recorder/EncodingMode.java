package com.automationanywhere.botcommand.utilities.screen.recorder;

/**
 * Stage-2 encoder selection for clip finalization.
 *
 * <ul>
 *   <li>{@link #FAST} - libx264 with {@code -preset ultrafast}. Encodes a 30 s
 *       clip in ~0.5-1 s on a typical bot host. File sizes are ~3-5x larger
 *       than {@link #COMPACT}. Recommended for bots writing to network storage
 *       where space is plentiful but CPU is constrained.</li>
 *   <li>{@link #COMPACT} - libaom-av1 with {@code -cpu-used 8}. Encodes a 30 s
 *       clip in ~5-15 s. Smaller files. Use when storage is the binding
 *       constraint or for archive scenarios.</li>
 * </ul>
 *
 * <p>Both encoders are present in the bundled ffmpeg.exe; the binary is
 * cross-compiled from the recipe at {@code tools/ffmpeg-build/Dockerfile}
 * with libx264 and libaom-av1 enabled.
 *
 * @author Sumit Kumar
 */
public enum EncodingMode {
    FAST,
    COMPACT
}

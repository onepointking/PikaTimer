/*
 * Copyright (C) 2026
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pikatimer.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Detects the character encoding of a text file.
 *
 * Detection order:
 * 1. UTF-8 BOM (EF BB BF)
 * 2. Strict UTF-8 decode of the entire file
 * 3. Platform-specific fallback (currently Windows-1252 to preserve legacy behavior)
 */
public final class CharsetDetector {

    private static final Logger LOGGER = Logger.getLogger(CharsetDetector.class.getName());

    private static final Charset FALLBACK_CHARSET = Charset.forName("Cp1252");

    private CharsetDetector() {
        // utility class
    }

    /**
     * Detects the charset of the supplied file.
     *
     * @param file the file to inspect
     * @return the detected charset; never null
     * @throws IOException if the file cannot be read
     */
    public static Charset detect(File file) throws IOException {
        // 1. Check for UTF-8 BOM
        if (hasUtf8Bom(file)) {
            LOGGER.log(Level.FINE, "Detected UTF-8 BOM in {0}", file.getAbsolutePath());
            return StandardCharsets.UTF_8;
        }

        // 2. Try a strict UTF-8 decode of the entire file
        if (isValidUtf8(file)) {
            LOGGER.log(Level.FINE, "Detected valid UTF-8 in {0}", file.getAbsolutePath());
            return StandardCharsets.UTF_8;
        }

        // 3. Fall back to the legacy platform-specific charset
        LOGGER.log(Level.FINE, "Falling back to {0} for {1}",
                new Object[]{FALLBACK_CHARSET.name(), file.getAbsolutePath()});
        return FALLBACK_CHARSET;
    }

    /**
     * Convenience overload that accepts a file path.
     *
     * @param fileName absolute path to the file
     * @return the detected charset; never null
     * @throws IOException if the file cannot be read
     */
    public static Charset detect(String fileName) throws IOException {
        return detect(new File(fileName));
    }

    private static boolean hasUtf8Bom(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            byte[] bom = new byte[3];
            int n = in.read(bom);
            return n >= 3
                    && bom[0] == (byte) 0xEF
                    && bom[1] == (byte) 0xBB
                    && bom[2] == (byte) 0xBF;
        }
    }

    private static boolean isValidUtf8(File file) throws IOException {
        CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), utf8Decoder))) {
            // Force a full decode by reading the entire file
            reader.lines().collect(Collectors.joining("\n"));
            return true;
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "File is not valid UTF-8: {0}", ex.getMessage());
            return false;
        }
    }
}

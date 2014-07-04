/*
 * Copyright 2004-2014 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.robot.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * @author shinsuke
 * 
 */
public final class StreamUtil {
    private static final int BLOCK_SIZE = 4096;

    private StreamUtil() {
    }

    public static void drain(final InputStream r, final OutputStream w)
            throws IOException {
        byte[] bytes = new byte[BLOCK_SIZE];
        try {
            int length = r.read(bytes);
            while (length != -1) {
                if (length != 0) {
                    w.write(bytes, 0, length);
                }
                length = r.read(bytes);
            }
        } finally {
            bytes = null;
        }
    }

    public static void drain(final InputStream r, final File outputFile)
            throws IOException {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(outputFile));
            drain(r, bos);
            bos.flush();
        } finally {
            IOUtils.closeQuietly(bos);
        }
    }
}

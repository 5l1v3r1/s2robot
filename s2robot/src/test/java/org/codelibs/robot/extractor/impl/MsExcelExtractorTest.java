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
package org.codelibs.robot.extractor.impl;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.codelibs.robot.RobotSystemException;
import org.seasar.extension.unit.S2TestCase;
import org.seasar.framework.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shinsuke
 *
 */
public class MsExcelExtractorTest extends S2TestCase {
    private static final Logger logger = LoggerFactory
            .getLogger(MsExcelExtractorTest.class);

    public MsExcelExtractor msExcelExtractor;

    @Override
    protected String getRootDicon() throws Throwable {
        return "org/codelibs/robot/extractor/extractor.dicon";
    }

    public void test_getText() {
        final InputStream in = ResourceUtil
                .getResourceAsStream("extractor/msoffice/test.xls");
        final String content = msExcelExtractor.getText(in, null).getContent();
        IOUtils.closeQuietly(in);
        logger.info(content);
        assertTrue(content.contains("テスト"));
    }

    public void test_getText_null() {
        try {
            msExcelExtractor.getText(null, null);
            fail();
        } catch (final RobotSystemException e) {
            // NOP
        }
    }
}

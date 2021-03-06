/*
 * Copyright 2012-2015 CodeLibs Project and the Others.
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
import org.codelibs.core.io.ResourceUtil;
import org.codelibs.robot.container.StandardRobotContainer;
import org.codelibs.robot.exception.RobotSystemException;
import org.dbflute.utflute.core.PlainTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shinsuke
 *
 */
public class MsWordExtractorTest extends PlainTestCase {
    private static final Logger logger = LoggerFactory
            .getLogger(MsWordExtractorTest.class);

    public MsWordExtractor msWordExtractor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        StandardRobotContainer container = new StandardRobotContainer()
                .singleton("msWordExtractor", MsWordExtractor.class);
        msWordExtractor = container.getComponent("msWordExtractor");
    }

    public void test_getText() {
        final InputStream in = ResourceUtil
                .getResourceAsStream("extractor/msoffice/test.doc");
        final String content = msWordExtractor.getText(in, null).getContent();
        IOUtils.closeQuietly(in);
        logger.info(content);
        assertTrue(content.contains("テスト"));
    }

    public void test_getText_null() {
        try {
            msWordExtractor.getText(null, null);
            fail();
        } catch (final RobotSystemException e) {
            // NOP
        }
    }
}

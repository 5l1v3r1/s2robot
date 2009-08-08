/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
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
package org.seasar.robot.extractor.impl;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.seasar.framework.util.StringUtil;
import org.seasar.robot.RobotSystemException;
import org.seasar.robot.entity.ExtractData;
import org.seasar.robot.extractor.ExtractException;
import org.seasar.robot.extractor.Extractor;

/**
 * @author shinsuke
 *
 */
public class TikaExtractor implements Extractor {

    /* (non-Javadoc)
     * @see org.seasar.robot.extractor.Extractor#getText(java.io.InputStream, java.util.Map)
     */
    public ExtractData getText(InputStream in, Map<String, String> params) {
        if (in == null) {
            throw new RobotSystemException("The inputstream is null.");
        }

        String resourceName = params != null ? params
                .get(ExtractData.RESOURCE_NAME_KEY) : null;
        String contentType = params != null ? params
                .get(ExtractData.CONTENT_TYPE) : null;

        Metadata metadata = new Metadata();
        if (StringUtil.isNotEmpty(resourceName)) {
            metadata.set(Metadata.RESOURCE_NAME_KEY, resourceName);
        }
        if (StringUtil.isNotBlank(contentType)) {
            metadata.set(Metadata.CONTENT_TYPE, contentType);
        }

        Parser parser = new AutoDetectParser();
        StringWriter writer = new StringWriter();
        try {
            parser.parse(in, new BodyContentHandler(writer), metadata);
        } catch (Exception e) {
            throw new ExtractException("Could not extract a content.", e);
        }

        ExtractData extractData = new ExtractData(writer.toString().replaceAll(
                "\\s+$", " ").trim());

        String[] names = metadata.names();
        Arrays.sort(names);
        for (String name : names) {
            extractData.putValues(name, metadata.getValues(name));
        }

        return extractData;
    }

}

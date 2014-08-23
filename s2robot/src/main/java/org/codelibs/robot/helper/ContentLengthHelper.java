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
package org.codelibs.robot.helper;

import java.util.HashMap;
import java.util.Map;

import org.codelibs.robot.RobotSystemException;
import org.seasar.framework.container.annotation.tiger.Binding;
import org.seasar.framework.container.annotation.tiger.BindingType;
import org.seasar.framework.util.StringUtil;

/**
 * @author shinsuke
 * 
 */
public class ContentLengthHelper {

    @Binding(bindingType = BindingType.MAY)
    protected long defaultMaxLength = 10L * 1024L * 1024L;// 10M

    protected Map<String, Long> maxLengthMap = new HashMap<String, Long>();

    public void addMaxLength(final String mimeType, final long maxLength) {
        if (StringUtil.isBlank(mimeType)) {
            throw new RobotSystemException("MIME type is a blank.");
        }
        if (maxLength < 0) {
            throw new RobotSystemException("The value of maxLength is invalid.");
        }
        maxLengthMap.put(mimeType, maxLength);
    }

    public long getMaxLength(final String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return defaultMaxLength;
        }
        final Long maxLength = maxLengthMap.get(mimeType);
        if (maxLength != null && maxLength >= 0L) {
            return maxLength;
        }
        return defaultMaxLength;
    }

    public long getDefaultMaxLength() {
        return defaultMaxLength;
    }

    public void setDefaultMaxLength(final long defaultMaxLength) {
        this.defaultMaxLength = defaultMaxLength;
    }
}

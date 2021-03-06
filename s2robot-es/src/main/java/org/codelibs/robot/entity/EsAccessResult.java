package org.codelibs.robot.entity;

import java.io.IOException;

import org.codelibs.core.beans.util.BeanUtil;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class EsAccessResult extends AccessResultImpl<String>implements ToXContent {

    public static final String ID = "id";

    public static final String SESSION_ID = "sessionId";

    public static final String RULE_ID = "ruleId";

    public static final String URL = "url";

    public static final String PARENT_URL = "parentUrl";

    public static final String STATUS = "status";

    public static final String HTTP_STATUS_CODE = "httpStatusCode";

    public static final String METHOD = "method";

    public static final String MIME_TYPE = "mimeType";

    public static final String CREATE_TIME = "createTime";

    public static final String EXECUTION_TIME = "executionTime";

    public static final String CONTENT_LENGTH = "contentLength";

    public static final String LAST_MODIFIED = "lastModified";

    public static final String ACCESS_RESULT_DATA = "accessResultData";

    @Override
    public void init(final ResponseData responseData, final ResultData resultData) {

        setCreateTime(System.currentTimeMillis());
        if (responseData != null) {
            BeanUtil.copyBeanToBean(responseData, this);
        }

        final EsAccessResultData accessResultData = new EsAccessResultData();
        if (resultData != null) {
            BeanUtil.copyBeanToBean(resultData, accessResultData);
        }
        setAccessResultData(accessResultData);
    }

    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final Params params) throws IOException {
        builder.startObject();
        if (sessionId != null) {
            builder.field(SESSION_ID, sessionId);
        }
        if (ruleId != null) {
            builder.field(RULE_ID, ruleId);
        }
        if (url != null) {
            builder.field(URL, url);
        }
        if (parentUrl != null) {
            builder.field(PARENT_URL, parentUrl);
        }
        if (status != null) {
            builder.field(STATUS, status);
        }
        if (httpStatusCode != null) {
            builder.field(HTTP_STATUS_CODE, httpStatusCode);
        }
        if (method != null) {
            builder.field(METHOD, method);
        }
        if (mimeType != null) {
            builder.field(MIME_TYPE, mimeType);
        }
        if (createTime != null) {
            builder.field(CREATE_TIME, createTime);
        }
        if (executionTime != null) {
            builder.field(EXECUTION_TIME, executionTime);
        }
        if (contentLength != null) {
            builder.field(CONTENT_LENGTH, contentLength);
        }
        if (lastModified != null) {
            builder.field(LAST_MODIFIED, lastModified);
        }
        if (accessResultData instanceof ToXContent) {
            builder.field(ACCESS_RESULT_DATA);
            ((ToXContent) accessResultData).toXContent(builder, params);
        }
        builder.endObject();
        return builder;
    }
}

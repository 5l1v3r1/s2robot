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
package org.codelibs.robot.processor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.codelibs.core.lang.SystemUtil;
import org.codelibs.robot.Constants;
import org.codelibs.robot.S2RobotContext;
import org.codelibs.robot.container.RobotContainer;
import org.codelibs.robot.entity.AccessResult;
import org.codelibs.robot.entity.RequestData;
import org.codelibs.robot.entity.ResponseData;
import org.codelibs.robot.entity.ResultData;
import org.codelibs.robot.entity.UrlQueue;
import org.codelibs.robot.processor.ResponseProcessor;
import org.codelibs.robot.service.UrlQueueService;
import org.codelibs.robot.transformer.Transformer;
import org.codelibs.robot.util.CrawlingParameterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shinsuke
 *
 */
public class DefaultResponseProcessor implements ResponseProcessor {
    private static final Logger logger = LoggerFactory // NOPMD
            .getLogger(DefaultResponseProcessor.class);

    @Resource
    protected RobotContainer robotContainer;

    protected Transformer transformer;

    protected int[] successfulHttpCodes;

    protected int[] notModifiedHttpCodes;

    @Override
    public void process(final ResponseData responseData) {
        if (isNotModified(responseData)) {
            final UrlQueue<?> urlQueue = CrawlingParameterUtil.getUrlQueue();
            final ResultData resultData = new ResultData();
            resultData.setData(new byte[0]);
            resultData.setEncoding(Constants.UTF_8);
            resultData.setTransformerName(Constants.NO_TRANSFORMER);
            processResult(urlQueue, responseData, resultData);
        } else if (isSuccessful(responseData)) {
            if (transformer == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No Transformer for (" + responseData.getUrl()
                            + "). PLEASE CHECK YOUR CONFIGURATION.");
                }
            } else {
                final ResultData resultData = transformer
                        .transform(responseData);
                if (resultData == null) {
                    logger.warn("No data for (" + responseData.getUrl() + ", "
                            + responseData.getMimeType() + ")");
                } else {
                    final UrlQueue<?> urlQueue = CrawlingParameterUtil
                            .getUrlQueue();
                    processResult(urlQueue, responseData, resultData);
                }
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug("Ignore a response(" + responseData.getStatus()
                    + "): " + responseData.getUrl());
        }
    }

    protected boolean isSuccessful(final ResponseData responseData) {
        if (successfulHttpCodes == null) {
            return true;
        }
        final int httpStatusCode = responseData.getHttpStatusCode();
        for (final int code : successfulHttpCodes) {
            if (code == httpStatusCode) {
                return true;
            }
        }
        return false;
    }

    protected boolean isNotModified(final ResponseData responseData) {
        if (notModifiedHttpCodes == null) {
            return false;
        }
        final int httpStatusCode = responseData.getHttpStatusCode();
        for (final int code : notModifiedHttpCodes) {
            if (code == httpStatusCode) {
                return true;
            }
        }
        return false;
    }

    protected void processResult(final UrlQueue<?> urlQueue,
            final ResponseData responseData, final ResultData resultData) {
        final AccessResult<?> accessResult = robotContainer
                .getComponent("accessResult");
        accessResult.init(responseData, resultData);

        final S2RobotContext robotContext = CrawlingParameterUtil
                .getRobotContext();
        final UrlQueueService<UrlQueue<?>> urlQueueService = CrawlingParameterUtil
                .getUrlQueueService();
        synchronized (robotContext.getAccessCountLock()) {
            if (!urlQueueService.visited(urlQueue)) {
                if (checkAccessCount(robotContext)) {
                    // store
                    CrawlingParameterUtil.getDataService().store(accessResult);

                    // add and filter urls
                    storeChildUrls(robotContext, resultData.getChildUrlSet(),
                            urlQueue.getUrl(), urlQueue.getDepth() == null ? 1
                                    : urlQueue.getDepth() + 1,
                            resultData.getEncoding());

                    // count up
                    if (robotContext.getMaxAccessCount() > 0) {
                        robotContext.setAccessCount(robotContext
                                .getAccessCount() + 1);
                    }
                } else if (robotContext.getMaxDepth() < 0
                        || urlQueue.getDepth() <= robotContext.getMaxDepth()) {
                    // cancel crawling
                    final List<UrlQueue<?>> newUrlQueueList = new ArrayList< >();
                    newUrlQueueList.add(urlQueue);
                    urlQueueService.offerAll(robotContext.getSessionId(),
                            newUrlQueueList);
                }
            }
        }

    }

    private boolean checkAccessCount(final S2RobotContext robotContext) {
        if (robotContext.getMaxAccessCount() > 0) {
            return robotContext.getAccessCount() < robotContext
                    .getMaxAccessCount();
        }
        return true;
    }

    private void storeChildUrls(final S2RobotContext robotContext,
            final Set<RequestData> childUrlList, final String url,
            final int depth, final String encoding) {
        if (robotContext.getMaxDepth() >= 0
                && depth > robotContext.getMaxDepth()) {
            return;
        }

        // add url and filter
        final List<UrlQueue<?>> childList = new ArrayList<>();
        for (final RequestData childUrl : childUrlList) {
            if (robotContext.getUrlFilter().match(childUrl.getUrl())) {
                final UrlQueue<?> uq = robotContainer.getComponent("urlQueue");
                uq.setCreateTime(SystemUtil.currentTimeMillis());
                uq.setDepth(depth);
                uq.setMethod(childUrl.getMethod().name());
                uq.setEncoding(encoding);
                uq.setParentUrl(url);
                uq.setSessionId(robotContext.getSessionId());
                uq.setUrl(childUrl.getUrl());
                uq.setMetaData(childUrl.getMetaData());
                childList.add(uq);
            }
        }
        if (!childList.isEmpty()) {
            CrawlingParameterUtil.getUrlQueueService().offerAll(
                    robotContext.getSessionId(), childList);
        }
    }

    public Transformer getTransformer() {
        return transformer;
    }

    public void setTransformer(final Transformer transformer) {
        this.transformer = transformer;
    }

    public int[] getSuccessfulHttpCodes() {
        return successfulHttpCodes;
    }

    public void setSuccessfulHttpCodes(final int[] successfulHttpCodes) {
        this.successfulHttpCodes = successfulHttpCodes;
    }

    public int[] getNotModifiedHttpCodes() {
        return notModifiedHttpCodes;
    }

    public void setNotModifiedHttpCodes(final int[] notModifiedHttpCodes) {
        this.notModifiedHttpCodes = notModifiedHttpCodes;
    }
}

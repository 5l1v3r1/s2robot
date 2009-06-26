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
package org.seasar.robot;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Resource;

import org.seasar.framework.container.S2Container;
import org.seasar.framework.util.StringUtil;
import org.seasar.robot.filter.UrlFilter;
import org.seasar.robot.rule.RuleManager;
import org.seasar.robot.service.DataService;
import org.seasar.robot.service.UrlQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shinsuke
 *
 */
public class S2Robot {

    private static final Logger logger = LoggerFactory.getLogger(S2Robot.class);

    @Resource
    protected UrlQueueService urlQueueService;

    @Resource
    protected DataService dataService;

    @Resource
    protected UrlFilter urlFilter;

    @Resource
    protected RuleManager ruleManager;

    @Resource
    protected S2RobotConfig robotConfig;

    @Resource
    protected S2Container container;

    private S2RobotContext robotContext;

    public S2Robot() {
        robotContext = new S2RobotContext();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        robotContext.sessionId = sdf.format(new Date());
    }

    public void addUrl(String url) {
        urlQueueService.add(robotContext.sessionId, url);
    }

    public String getSessionId() {
        return robotContext.sessionId;
    }

    public void setSessionId(String sessionId) {
        if (StringUtil.isNotBlank(sessionId)
                && !sessionId.equals(robotContext.sessionId)) {
            robotContext.sessionId = sessionId;
            urlQueueService.updateSessionId(robotContext.sessionId, sessionId);
        }
    }

    public String execute() {
        // context
        robotContext.urlFilter = urlFilter;
        robotContext.ruleManager = ruleManager;

        ThreadGroup threadGroup = new ThreadGroup("Robot-"
                + robotContext.sessionId);
        Thread[] threads = new Thread[robotConfig.getNumOfThread()];
        for (int i = 0; i < robotConfig.getNumOfThread(); i++) {
            S2RobotThread robotThread = (S2RobotThread) container
                    .getComponent("robotThread");
            robotThread.robotContext = robotContext;
            threads[i] = new Thread(threadGroup, robotThread, "Robot-"
                    + robotContext.sessionId + "-" + Integer.toString(i + 1));
        }

        // run
        robotContext.running = true;
        for (int i = 0; i < robotConfig.numOfThread; i++) {
            threads[i].start();
        }

        // join
        for (int i = 0; i < robotConfig.numOfThread; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                logger.warn("Could not join " + threads[i].getName());
            }
        }
        robotContext.running = false;

        urlQueueService.saveSession(robotContext.sessionId);

        return robotContext.sessionId;
    }

    public void cleanup(String sessionId) {
        // TODO transaction?
        urlQueueService.delete(sessionId);
        dataService.delete(sessionId);
    }

    public void addIncludeFilter(String regexp) {
        if (StringUtil.isNotBlank(regexp)) {
            urlFilter.addInclude(regexp);
        }
    }

    public void addExcludeFilter(String regexp) {
        if (StringUtil.isNotBlank(regexp)) {
            urlFilter.addExclude(regexp);
        }
    }

    public S2RobotConfig getRobotConfig() {
        return robotConfig;
    }

    public void stop() {
        robotContext.running = false;
    }

    public UrlFilter getUrlFilter() {
        return urlFilter;
    }

    public RuleManager getRuleManager() {
        return ruleManager;
    }

}
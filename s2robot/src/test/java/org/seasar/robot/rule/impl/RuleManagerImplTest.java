/*
 * Copyright 2004-2013 the Seasar Foundation and the Others.
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
package org.seasar.robot.rule.impl;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.seasar.extension.unit.S2TestCase;
import org.seasar.framework.util.ResourceUtil;
import org.seasar.robot.RobotCrawlAccessException;
import org.seasar.robot.entity.ResponseData;
import org.seasar.robot.rule.Rule;
import org.seasar.robot.rule.RuleManager;

/**
 * @author shinsuke
 *
 */
public class RuleManagerImplTest extends S2TestCase {
    public RuleManager ruleManager;

    @Override
    protected String getRootDicon() throws Throwable {
        return "app.dicon";
    }

    public void test_getRule() {
        final Rule rule = ruleManager.getRule(new ResponseData());
        assertNotNull(rule);
        assertEquals("fileRule", rule.getRuleId());
    }

    public void test_getRule_sitemaps1() {
        final ResponseData responseData = new ResponseData();
        responseData.setUrl("http://www.example.com/sitemap1.xml");
        InputStream is =
            ResourceUtil.getResourceAsStream("sitemaps/sitemap1.xml");
        responseData.setResponseBody(is);
        final Rule rule = ruleManager.getRule(responseData);
        assertNotNull(rule);
        assertEquals("sitemapsRule", rule.getRuleId());
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(responseData.getResponseBody());
    }

    public void test_getRule_sitemaps2() {
        final ResponseData responseData = new ResponseData();
        responseData.setUrl("http://www.example.com/sitemap1.xml.gz");
        InputStream is =
            ResourceUtil.getResourceAsStream("sitemaps/sitemap1.xml.gz");
        responseData.setResponseBody(is);
        final Rule rule = ruleManager.getRule(responseData);
        assertNotNull(rule);
        assertEquals("sitemapsRule", rule.getRuleId());
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(responseData.getResponseBody());
    }

    public void test_getRule_sitemaps3() {
        final ResponseData responseData = new ResponseData();
        responseData.setUrl("http://www.example.com/sitemap1.txt");
        InputStream is =
            ResourceUtil.getResourceAsStream("sitemaps/sitemap1.txt");
        responseData.setResponseBody(is);
        final Rule rule = ruleManager.getRule(responseData);
        assertNotNull(rule);
        assertEquals("sitemapsRule", rule.getRuleId());
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(responseData.getResponseBody());
    }

    public void test_getRule_sitemaps4() {
        final ResponseData responseData = new ResponseData();
        responseData.setUrl("http://www.example.com/sitemap1.txt.gz");
        InputStream is =
            ResourceUtil.getResourceAsStream("sitemaps/sitemap1.xml.gz");
        responseData.setResponseBody(is);
        final Rule rule = ruleManager.getRule(responseData);
        assertNotNull(rule);
        assertEquals("sitemapsRule", rule.getRuleId());
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(responseData.getResponseBody());
    }

    public void test_getRule_sitemaps5() {
        final ResponseData responseData = new ResponseData();
        responseData.setUrl("http://www.example.com/sitemap/");
        InputStream is =
            ResourceUtil.getResourceAsStream("sitemaps/sitemap1.xml");
        responseData.setResponseBody(is);
        final Rule rule = ruleManager.getRule(responseData);
        assertNotNull(rule);
        assertEquals("sitemapsRule", rule.getRuleId());
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(responseData.getResponseBody());
    }

    public void test_getRule_sitemaps1_nocontent() {
        final ResponseData responseData = new ResponseData();
        responseData.setUrl("http://www.example.com/sitemap1.xml");
        try {
            ruleManager.getRule(responseData);
            fail();
        } catch (RobotCrawlAccessException e) {
            // ok
        }
    }

    public void test_checkRule() {
        final Rule rule = ruleManager.getRule(new ResponseData());
        assertNotNull(rule);
        assertEquals("fileRule", rule.getRuleId());

        final RegexRule rule2 = new RegexRule();
        rule2.setAllRequired(true);
        rule2.addRule("url", "http:.*");

        final RegexRule rule3 = new RegexRule();
        rule3.addRule("url", "http:.*");

        assertFalse(ruleManager.hasRule(rule2));
        assertFalse(ruleManager.hasRule(rule3));

        ruleManager.addRule(rule2);

        assertTrue(ruleManager.hasRule(rule2));
        assertFalse(ruleManager.hasRule(rule3));

        ruleManager.addRule(rule3);

        assertTrue(ruleManager.hasRule(rule2));
        assertTrue(ruleManager.hasRule(rule3));

        ruleManager.removeRule(rule2);

        assertFalse(ruleManager.hasRule(rule2));
        assertTrue(ruleManager.hasRule(rule3));

        ruleManager.removeRule(rule3);

        assertFalse(ruleManager.hasRule(rule2));
        assertFalse(ruleManager.hasRule(rule3));
    }
}

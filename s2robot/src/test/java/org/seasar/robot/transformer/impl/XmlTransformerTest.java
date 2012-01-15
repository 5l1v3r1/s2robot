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
package org.seasar.robot.transformer.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.seasar.extension.unit.S2TestCase;
import org.seasar.framework.util.ResourceUtil;
import org.seasar.robot.Constants;
import org.seasar.robot.RobotSystemException;
import org.seasar.robot.entity.AccessResultDataImpl;
import org.seasar.robot.entity.ResponseData;
import org.seasar.robot.entity.ResultData;
import org.seasar.robot.entity.TestEntity;

/**
 * @author shinsuke
 * 
 */
public class XmlTransformerTest extends S2TestCase {
    public XmlTransformer xmlTransformer;

    public XmlTransformer xmlNsTransformer;

    public XmlTransformer xmlMapTransformer;

    public XmlTransformer xmlEntityTransformer;

    @Override
    protected String getRootDicon() throws Throwable {
        return "app.dicon";
    }

    public void test_transform() throws Exception {
        String result =
            "<?xml version=\"1.0\"?>\n"//
                + "<doc>\n"//
                + "<field name=\"name\"><list><item>鈴木太郎</item><item>佐藤二朗</item><item>田中花子</item></list></field>\n"//
                + "<field name=\"access\"><list><item></item><item>http://www.taro.com/</item><item>jiro@hoge.foo.bar</item><item>090-xxxx-xxxx</item></list></field>\n"//
                + "<field name=\"image\"><list><item>taro.png</item><item>jiro.png</item><item>hanako.png</item></list></field>\n"//
                + "<field name=\"email\"><list><item></item><item>jiro@hoge.foo.bar</item></list></field>\n"//
                + "<field name=\"url\">http://www.taro.com/</field>\n"//
                + "<field name=\"tel\">090-xxxx-xxxx</field>\n"//
                + "</doc>";

        ResponseData responseData = new ResponseData();
        responseData.setResponseBody(ResourceUtil
            .getResourceAsStream("extractor/test.xml"));
        responseData.setCharSet(Constants.UTF_8);
        ResultData resultData = xmlTransformer.transform(responseData);
        assertEquals(result, new String(resultData.getData(), Constants.UTF_8));
    }

    public void test_transformNs() throws Exception {
        String result =
            "<?xml version=\"1.0\"?>\n"//
                + "<doc>\n"//
                + "<field name=\"name\"><list><item>鈴木太郎</item><item>佐藤二朗</item><item>田中花子</item></list></field>\n"//
                + "<field name=\"access\"><list><item></item><item>http://www.taro.com/</item><item>jiro@hoge.foo.bar</item><item>090-xxxx-xxxx</item></list></field>\n"//
                + "<field name=\"image\"><list><item>taro.png</item><item>jiro.png</item><item>hanako.png</item></list></field>\n"//
                + "<field name=\"email\"><list><item></item><item>jiro@hoge.foo.bar</item></list></field>\n"//
                + "<field name=\"url\">http://www.taro.com/</field>\n"//
                + "<field name=\"tel\">090-xxxx-xxxx</field>\n"//
                + "</doc>";

        ResponseData responseData = new ResponseData();
        responseData.setResponseBody(ResourceUtil
            .getResourceAsStream("extractor/test_ns.xml"));
        responseData.setCharSet(Constants.UTF_8);
        ResultData resultData = xmlNsTransformer.transform(responseData);
        assertEquals(result, new String(resultData.getData(), Constants.UTF_8));
    }

    public void test_getData() throws Exception {
        String value =
            "<?xml version=\"1.0\"?>\n"//
                + "<doc>\n"//
                + "<field name=\"title\">タイトル</field>\n"//
                + "<field name=\"body\">第一章 第一節 ほげほげふがふが LINK 第2章 第2節</field>\n"//
                + "<field name=\"list\"><list><item>リスト1</item><item>リスト2</item><item>リスト3</item></list></field>\n"//
                + "</doc>";

        AccessResultDataImpl accessResultDataImpl = new AccessResultDataImpl();
        accessResultDataImpl.setData(value.getBytes(Constants.UTF_8));
        accessResultDataImpl.setEncoding(Constants.UTF_8);
        accessResultDataImpl.setTransformerName("xmlTransformer");

        Object obj = xmlTransformer.getData(accessResultDataImpl);
        assertEquals(value, obj);
    }

    public void test_getData_wrongName() throws Exception {
        String value = "<?xml version=\"1.0\"?>\n"//
            + "<doc>\n"//
            + "<field name=\"title\">タイトル</field>\n"//
            + "<field name=\"body\">第一章 第一節 ほげほげふがふが LINK 第2章 第2節</field>\n"//
            + "</doc>";

        AccessResultDataImpl accessResultDataImpl = new AccessResultDataImpl();
        accessResultDataImpl.setData(value.getBytes(Constants.UTF_8));
        accessResultDataImpl.setEncoding(Constants.UTF_8);
        accessResultDataImpl.setTransformerName("transformer");

        try {
            Object obj = xmlTransformer.getData(accessResultDataImpl);
            fail();
        } catch (RobotSystemException e) {
        }
    }

    public void test_getData_nullData() throws Exception {
        String value = "<?xml version=\"1.0\"?>\n"//
            + "<doc>\n"//
            + "<field name=\"title\">タイトル</field>\n"//
            + "<field name=\"body\">第一章 第一節 ほげほげふがふが LINK 第2章 第2節</field>\n"//
            + "</doc>";

        AccessResultDataImpl accessResultDataImpl = new AccessResultDataImpl();
        accessResultDataImpl.setData(null);
        accessResultDataImpl.setEncoding(Constants.UTF_8);
        accessResultDataImpl.setTransformerName("xmlTransformer");

        Object obj = xmlTransformer.getData(accessResultDataImpl);
        assertNull(obj);
    }

    public void test_dataClass() {
        assertNull(xmlTransformer.dataClass);
        assertEquals(Map.class, xmlMapTransformer.dataClass);
    }

    public void test_getData_dataMap() throws Exception {
        String value =
            "<?xml version=\"1.0\"?>\n"//
                + "<doc>\n"//
                + "<field name=\"title\">タイトル</field>\n"//
                + "<field name=\"body\">第一章 第一節 ほげほげふがふが LINK 第2章 第2節</field>\n"//
                + "<field name=\"list\"><list><item>リスト1</item><item>リスト2</item><item>リスト3</item></list></field>\n"//
                + "</doc>";

        AccessResultDataImpl accessResultDataImpl = new AccessResultDataImpl();
        accessResultDataImpl.setData(value.getBytes(Constants.UTF_8));
        accessResultDataImpl.setEncoding(Constants.UTF_8);
        accessResultDataImpl.setTransformerName("xmlMapTransformer");

        Object obj = xmlMapTransformer.getData(accessResultDataImpl);
        assertTrue(obj instanceof Map);
        Map<String, String> map = (Map) obj;
        assertEquals("タイトル", map.get("title"));
        assertEquals("第一章 第一節 ほげほげふがふが LINK 第2章 第2節", map.get("body"));
        List<String> list = new ArrayList<String>();
        list.add("リスト1");
        list.add("リスト2");
        list.add("リスト3");
        assertEquals(list, map.get("list"));
    }

    public void test_getData_dataMap_entity() throws Exception {
        String value =
            "<?xml version=\"1.0\"?>\n"//
                + "<doc>\n"//
                + "<field name=\"title\">タイトル</field>\n"//
                + "<field name=\"body\">第一章 第一節 ほげほげふがふが LINK 第2章 第2節</field>\n"//
                + "<field name=\"list\"><list><item>リスト1</item><item>リスト2</item><item>リスト3</item></list></field>\n"//
                + "</doc>";

        AccessResultDataImpl accessResultDataImpl = new AccessResultDataImpl();
        accessResultDataImpl.setData(value.getBytes(Constants.UTF_8));
        accessResultDataImpl.setEncoding(Constants.UTF_8);
        accessResultDataImpl.setTransformerName("xmlEntityTransformer");

        Object obj = xmlEntityTransformer.getData(accessResultDataImpl);
        assertTrue(obj instanceof TestEntity);
        TestEntity entity = (TestEntity) obj;
        assertEquals("タイトル", entity.getTitle());
        assertEquals("第一章 第一節 ほげほげふがふが LINK 第2章 第2節", entity.getBody());
        List<String> list = new ArrayList<String>();
        list.add("リスト1");
        list.add("リスト2");
        list.add("リスト3");
        assertEquals(list, entity.getList());
    }

    public void test_getData_dataMap_entity_emptyList() throws Exception {
        String value = "<?xml version=\"1.0\"?>\n"//
            + "<doc>\n"//
            + "<field name=\"title\">タイトル</field>\n"//
            + "<field name=\"body\">第一章 第一節 ほげほげふがふが LINK 第2章 第2節</field>\n"//
            + "<field name=\"list\"><list></list></field>\n"//
            + "</doc>";

        AccessResultDataImpl accessResultDataImpl = new AccessResultDataImpl();
        accessResultDataImpl.setData(value.getBytes(Constants.UTF_8));
        accessResultDataImpl.setEncoding(Constants.UTF_8);
        accessResultDataImpl.setTransformerName("xmlEntityTransformer");

        Object obj = xmlEntityTransformer.getData(accessResultDataImpl);
        assertTrue(obj instanceof TestEntity);
        TestEntity entity = (TestEntity) obj;
        assertEquals("タイトル", entity.getTitle());
        assertEquals("第一章 第一節 ほげほげふがふが LINK 第2章 第2節", entity.getBody());
        List<String> list = new ArrayList<String>();
        assertEquals(list, entity.getList());
    }
}

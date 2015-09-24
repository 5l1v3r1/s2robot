/*
 * Copyright 2004-2010 the Seasar Foundation and the Others.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.codelibs.robot.Constants;
import org.codelibs.robot.entity.ExtractData;
import org.codelibs.robot.extractor.ExtractException;
import org.codelibs.robot.extractor.Extractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets a text from .eml file.
 * 
 * @author shinsuke
 *
 */
public class EmlExtractor implements Extractor {
    private static final Logger logger =
        LoggerFactory.getLogger(EmlExtractor.class);

    protected Properties mailProperties = new Properties();

    /* (non-Javadoc)
     * @see org.codelibs.robot.extractor.Extractor#getText(java.io.InputStream, java.util.Map)
     */
    @Override
    public ExtractData getText(InputStream in, Map<String, String> params) {
        Properties props = new Properties(mailProperties);
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                props.put(entry.getKey(), entry.getValue());
            }
        }
        try {
            Session mailSession = Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(mailSession, in);
            Object content = message.getContent();
            ExtractData data = new ExtractData(
                content != null ? content.toString() : Constants.EMPTY_STRING);
            @SuppressWarnings("unchecked")
            Enumeration<Header> headers = message.getAllHeaders();
            while (headers.hasMoreElements()) {
                Header header = headers.nextElement();
                data.putValue(header.getName(), header.getValue());
            }
            try {
                putValue(data, "Content-ID", message.getContentID());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(
                    data,
                    "Content-Language",
                    message.getContentLanguage());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Content-MD5", message.getContentMD5());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Description", message.getDescription());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Disposition", message.getDisposition());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Encoding", message.getEncoding());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "File-Name", message.getFileName());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "From", message.getFrom());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Line-Count", message.getLineCount());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Message-ID", message.getMessageID());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Message-Number", message.getMessageNumber());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Received-Date", getReceivedDate(message));
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Reply-To", message.getReplyTo());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Sender", message.getSender());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Sent-Date", message.getSentDate());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Size", message.getSize());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Subject", message.getSubject());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(data, "Receipients", message.getAllRecipients());
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(
                    data,
                    "To",
                    message.getRecipients(Message.RecipientType.TO));
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(
                    data,
                    "Cc",
                    message.getRecipients(Message.RecipientType.CC));
            } catch (Exception e) {
                //ignore
            }
            try {
                putValue(
                    data,
                    "Bcc",
                    message.getRecipients(Message.RecipientType.BCC));
            } catch (Exception e) {
                //ignore
            }
            return data;
        } catch (MessagingException | IOException e) {
            throw new ExtractException(e);

        }
    }

    /**
     * @param data
     * @param string
     * @param contentID
     */
    private void putValue(ExtractData data, String key, Object value) {
        if (value instanceof String) {
            if ("Subject".equals(key)) {
                data.putValue(key, getDecodeText(value.toString()));
            } else {
                data.putValue(key, value.toString());
            }
        } else if (value instanceof String[]) {
            data.putValues(key, (String[]) value);
        } else if (value instanceof Integer) {
            data.putValue(key, ((Integer) value).toString());
        } else if (value instanceof Address[]) {
            int size = ((Address[]) value).length;
            String[] values = new String[size];
            for (int i = 0; i < size; i++) {
                Address address = ((Address[]) value)[i];
                values[i] = getDecodeText(address.toString());
            }
            data.putValues(key, values);
        } else if (value instanceof Date) {
            data.putValue(
                key,
                new SimpleDateFormat(Constants.ISO_DATETIME_FORMAT)
                    .format(value));
        } else if (value != null) {
            data.putValue(key, value.toString());
        }
    }

    String getDecodeText(String value) {
        if (value == null) {
            return Constants.EMPTY_STRING;
        }
        try {
            return MimeUtility.decodeText(value);
        } catch (UnsupportedEncodingException e) {
            logger.warn("Invalid encoding.", e);
            return Constants.EMPTY_STRING;
        }
    }

    public Properties getMailProperties() {
        return mailProperties;
    }

    public void setMailProperties(Properties mailProperties) {
        this.mailProperties = mailProperties;
    }

    private static Date getReceivedDate(javax.mail.Message message)
            throws MessagingException {
        Date today = new Date();
        logger.info("message=" + message);
        final String[] received = message.getHeader("received");
        if (received != null) {
            for (final String v : received) {
                logger.info("received[]: " + v);
                String dateStr = null;
                try {
                    dateStr = getDateString(v);
                    logger.info("dateStr=" + dateStr);
                    final Date receivedDate =
                        new MailDateFormat().parse(dateStr);
                    if (!receivedDate.after(today)) {
                        return receivedDate;
                    }
                } catch (ParseException e) {
                    // ignore
                }
            }
        }
        return null;
    }

    private static String getDateString(String text) {
        String[] dayOfWeek =
            { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        for (final String dow : dayOfWeek) {
            final int i = text.lastIndexOf(dow);
            if (i != -1) {
                return text.substring(i);
            }
        }
        return null;
    }
}

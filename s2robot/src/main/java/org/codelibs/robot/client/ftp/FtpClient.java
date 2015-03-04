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
package org.codelibs.robot.client.ftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilters;
import org.codelibs.robot.Constants;
import org.codelibs.robot.MaxLengthExceededException;
import org.codelibs.robot.RobotCrawlAccessException;
import org.codelibs.robot.RobotLoginFailureException;
import org.codelibs.robot.builder.RequestDataBuilder;
import org.codelibs.robot.client.AbstractS2RobotClient;
import org.codelibs.robot.client.fs.ChildUrlsException;
import org.codelibs.robot.entity.RequestData;
import org.codelibs.robot.entity.ResponseData;
import org.codelibs.robot.helper.ContentLengthHelper;
import org.codelibs.robot.helper.MimeTypeHelper;
import org.codelibs.robot.util.TemporaryFileInputStream;
import org.seasar.framework.container.SingletonS2Container;
import org.seasar.framework.container.annotation.tiger.Binding;
import org.seasar.framework.container.annotation.tiger.BindingType;
import org.seasar.framework.container.annotation.tiger.DestroyMethod;
import org.seasar.framework.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shinsuke
 *
 */
public class FtpClient extends AbstractS2RobotClient {
    private static final Logger logger = LoggerFactory
        .getLogger(FtpClient.class);

    public static final String FTP_AUTHENTICATIONS_PROPERTY =
        "ftpAuthentications";

    protected String charset = Constants.UTF_8;

    @Binding(bindingType = BindingType.MAY)
    @Resource
    protected ContentLengthHelper contentLengthHelper;

    public volatile FtpAuthenticationHolder ftpAuthenticationHolder;

    private FTPClientConfig ftpClientConfig;

    public synchronized void init() {
        if (ftpAuthenticationHolder != null) {
            return;
        }

        // user agent
        final FtpAuthentication[] ftpAuthentications =
            getInitParameter(
                FTP_AUTHENTICATIONS_PROPERTY,
                new FtpAuthentication[0]);
        if (ftpAuthentications != null) {
            final FtpAuthenticationHolder holder =
                new FtpAuthenticationHolder();
            for (final FtpAuthentication ftpAuthentication : ftpAuthentications) {
                holder.add(ftpAuthentication);
            }
            ftpAuthenticationHolder = holder;
        }

        String systemKey =
            getInitParameter("ftpConfigSystemKey", FTPClientConfig.SYST_UNIX);
        ftpClientConfig = new FTPClientConfig(systemKey);
        String serverLanguageCode =
            getInitParameter("ftpConfigServerLanguageCode", "en");
        ftpClientConfig.setServerLanguageCode(serverLanguageCode);
        String serverTimeZoneId =
            getInitParameter("ftpConfigServerTimeZoneId", null);
        if (serverTimeZoneId != null) {
            ftpClientConfig.setServerTimeZoneId(serverTimeZoneId);
        }
    }

    @DestroyMethod
    public void destroy() {
        ftpAuthenticationHolder = null;
        for (FTPClient ftpClient : ftpClientQueue) {
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                logger.debug("Failed to disconnect FTPClient.", e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codelibs.robot.client.S2RobotClient#doGet(java.lang.String)
     */
    @Override
    public ResponseData doGet(final String uri) {
        return getResponseData(uri, true);
    }

    protected ResponseData getResponseData(final String uri,
            final boolean includeContent) {
        if (ftpAuthenticationHolder == null) {
            init();
        }

        final ResponseData responseData = new ResponseData();
        responseData.setMethod(Constants.GET_METHOD);

        FtpInfo ftpInfo = new FtpInfo(uri);
        responseData.setUrl(ftpInfo.toUrl());

        FTPClient client = null;
        try {
            client = getClient(ftpInfo);

            FTPFile file = null;
            client.changeWorkingDirectory(ftpInfo.getParent());
            validateRequest(client);

            if (ftpInfo.getName() == null) {
                // root directory
                final Set<RequestData> requestDataSet = new HashSet<>();
                if (includeContent) {
                    try {
                        FTPFile[] files =
                            client.listFiles(
                                ftpInfo.getParent(),
                                FTPFileFilters.NON_NULL);
                        validateRequest(client);
                        for (final FTPFile f : files) {
                            final String chileUri = ftpInfo.toUrl(f.getName());
                            requestDataSet.add(RequestDataBuilder
                                .newRequestData()
                                .get()
                                .url(chileUri)
                                .build());
                        }
                    } catch (IOException e) {
                        throw new RobotCrawlAccessException("Could not access "
                            + uri, e);
                    }
                }
                ftpClientQueue.offer(client);
                throw new ChildUrlsException(requestDataSet);
            }

            FTPFile[] files = client.listFiles(null, FTPFileFilters.NON_NULL);
            validateRequest(client);
            for (FTPFile f : files) {
                if (ftpInfo.getName().equals(f.getName())) {
                    file = f;
                    break;
                }
            }

            if (file == null) {
                responseData.setHttpStatusCode(Constants.NOT_FOUND_STATUS_CODE);
                responseData.setCharSet(charset);
                responseData.setContentLength(0);
            } else if (file.isFile()) {
                responseData.setHttpStatusCode(Constants.OK_STATUS_CODE);
                responseData.setCharSet(Constants.UTF_8);
                responseData.setLastModified(file.getTimestamp().getTime());

                // check file size
                responseData.setContentLength(file.getSize());
                if (contentLengthHelper != null) {
                    final long maxLength =
                        contentLengthHelper.getMaxLength(responseData
                            .getMimeType());
                    if (responseData.getContentLength() > maxLength) {
                        throw new MaxLengthExceededException(
                            "The content length ("
                                + responseData.getContentLength()
                                + " byte) is over " + maxLength
                                + " byte. The url is " + uri);
                    }
                }

                if (includeContent) {
                    File tempFile = null;
                    File outputFile = null;
                    try {
                        tempFile = File.createTempFile("ftp-", ".tmp");
                        try (OutputStream out =
                            new BufferedOutputStream(new FileOutputStream(
                                tempFile))) {
                            if (!client.retrieveFile(ftpInfo.getName(), out)) {
                                throw new RobotCrawlAccessException(
                                    "Failed to retrieve: " + ftpInfo.toUrl());
                            }
                        }

                        final MimeTypeHelper mimeTypeHelper =
                            SingletonS2Container.getComponent("mimeTypeHelper");
                        try (InputStream is = new FileInputStream(tempFile)) {
                            responseData.setMimeType(mimeTypeHelper
                                .getContentType(is, file.getName()));
                        } catch (final Exception e) {
                            responseData.setMimeType(mimeTypeHelper
                                .getContentType(null, file.getName()));
                        }

                        responseData.setCharSet(geCharSet(tempFile));

                        outputFile =
                            File.createTempFile(
                                "s2robot-FileSystemClient-",
                                ".out");
                        FileUtil.copy(tempFile, outputFile);
                        responseData
                            .setResponseBody(new TemporaryFileInputStream(
                                outputFile));
                    } catch (final Exception e) {
                        logger.warn("I/O Exception.", e);
                        responseData
                            .setHttpStatusCode(Constants.SERVER_ERROR_STATUS_CODE);
                    } finally {
                        if (tempFile != null && !tempFile.delete()) {
                            logger.warn("Could not delete "
                                + tempFile.getAbsolutePath());
                        }
                        if (outputFile != null && !outputFile.delete()) {
                            logger.warn("Could not delete "
                                + outputFile.getAbsolutePath());
                        }
                    }
                }
            } else if (file.isDirectory()) {
                final Set<RequestData> requestDataSet = new HashSet<>();
                if (includeContent) {
                    try {
                        FTPFile[] ftpFiles =
                            client.listFiles(
                                ftpInfo.getName(),
                                FTPFileFilters.NON_NULL);
                        validateRequest(client);
                        for (final FTPFile f : ftpFiles) {
                            final String chileUri = ftpInfo.toUrl(f.getName());
                            requestDataSet.add(RequestDataBuilder
                                .newRequestData()
                                .get()
                                .url(chileUri)
                                .build());
                        }
                    } catch (IOException e) {
                        throw new RobotCrawlAccessException("Could not access "
                            + uri, e);
                    }
                }
                ftpClientQueue.offer(client);
                throw new ChildUrlsException(requestDataSet);
            } else {
                responseData.setHttpStatusCode(Constants.NOT_FOUND_STATUS_CODE);
                responseData.setCharSet(charset);
                responseData.setContentLength(0);
            }
            ftpClientQueue.offer(client);
        } catch (final ChildUrlsException e) {
            throw e;
        } catch (final Exception e) {
            throw new RobotCrawlAccessException("Could not access " + uri, e);
        }

        return responseData;
    }

    /**
     * @param client
     */
    private void validateRequest(FTPClient client) {
        int replyCode = client.getReplyCode();
        if (replyCode >= 200 && replyCode < 300) {
            return;
        }
        throw new RobotCrawlAccessException("Failed FTP request: "
            + client.getReplyString().trim());
    }

    protected String geCharSet(final File file) {
        return charset;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(final String charset) {
        this.charset = charset;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codelibs.robot.client.S2RobotClient#doHead(java.lang.String)
     */
    @Override
    public ResponseData doHead(final String url) {
        try {
            final ResponseData responseData = getResponseData(url, false);
            responseData.setMethod(Constants.HEAD_METHOD);
            return responseData;
        } catch (final ChildUrlsException e) {
            return null;
        }
    }

    Queue<FTPClient> ftpClientQueue = new ConcurrentLinkedQueue<>();

    protected FTPClient getClient(FtpInfo info) throws IOException {
        FTPClient ftpClient = ftpClientQueue.poll();
        if (ftpClient != null) {
            if (ftpClient.isAvailable()) {
                return ftpClient;
            }
            try {
                ftpClient.disconnect();
            } catch (Exception e) {
                logger.debug("Failed to disconnect " + info.toUrl(), e);
            }
        }

        try {
            ftpClient = new FTPClient();
            ftpClient.configure(ftpClientConfig);

            ftpClient.connect(info.getHost(), info.getPort());
            validateRequest(ftpClient);

            FtpAuthentication auth = ftpAuthenticationHolder.get(info.toUrl());
            if (auth != null) {
                if (!ftpClient.login(auth.getUsername(), auth.getPassword())) {
                    throw new RobotLoginFailureException("Login Failure: "
                        + auth.getUsername() + " for " + info.toUrl());
                }
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            return ftpClient;
        } catch (IOException e) {
            if (ftpClient != null) {
                try {
                    ftpClient.disconnect();
                } catch (Exception e1) {
                    logger.debug("Failed to disconnect " + info.toUrl(), e);
                }
            }
            throw e;
        }
    }

    public static class FtpInfo {

        private static final int DEFAULT_FTP_PORT = 21;

        private URL uri;

        private String parent;

        private String name;

        public FtpInfo(String s) {
            try {
                uri = new URL(s);
            } catch (MalformedURLException e) {
                throw new RobotCrawlAccessException("Invalid URL: " + s, e);
            }

            String path = uri.getPath();
            if (path == null) {
                parent = "/";
                name = null;
            } else {
                String[] values =
                    path
                        .replaceAll("/+", "/")
                        .replaceFirst("/$", "")
                        .split("/");
                if (values.length == 1) {
                    parent = "/";
                    name = null;
                } else if (values.length == 2) {
                    parent = "/";
                    name = values[1];
                } else {
                    parent =
                        StringUtils.join(values, "/", 0, values.length - 1);
                    name = values[values.length - 1];
                }
            }
        }

        public String getCacheKey() {
            return getHost() + ":" + getPort();
        }

        public String getHost() {
            return uri.getHost();
        }

        public int getPort() {
            int port = uri.getPort();
            if (port == -1) {
                port = DEFAULT_FTP_PORT;
            }
            return port;
        }

        public String toUrl() {
            StringBuilder buf = new StringBuilder(100);
            buf.append("ftp://");
            buf.append(getHost());
            int port = getPort();
            if (port != DEFAULT_FTP_PORT) {
                buf.append(':').append(port);
            }
            buf.append(uri.getPath());
            if ("/".equals(uri.getPath())) {
                return buf.toString();
            }
            return buf.toString().replaceAll("/+$", "");
        }

        public String toUrl(String child) {
            String url = toUrl();
            if (url.endsWith("/")) {
                return toUrl() + child;
            }
            return toUrl() + "/" + child;
        }

        public String getParent() {
            return parent;
        }

        public String getName() {
            return name;
        }
    }
}

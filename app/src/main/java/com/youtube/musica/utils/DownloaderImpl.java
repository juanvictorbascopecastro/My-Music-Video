package com.youtube.musica.utils;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class DownloaderImpl extends Downloader {

    private static DownloaderImpl instance;
    private final OkHttpClient client;

    private DownloaderImpl() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.readTimeout(30, TimeUnit.SECONDS);
        builder.connectTimeout(30, TimeUnit.SECONDS);
        client = builder.build();
    }

    public static DownloaderImpl getInstance() {
        if (instance == null) {
            instance = new DownloaderImpl();
        }
        return instance;
    }

    @Override
    public Response execute(Request request) throws IOException, ReCaptchaException {
        String httpMethod = request.httpMethod();
        String url = request.url();
        Map<String, List<String>> headers = request.headers();
        byte[] dataToSend = request.dataToSend();

        RequestBody body = null;
        if (dataToSend != null) {
            body = RequestBody.create(dataToSend);
        } else if ("POST".equalsIgnoreCase(httpMethod) || "PUT".equalsIgnoreCase(httpMethod)) {
            body = RequestBody.create(new byte[0]);
        }

        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                .url(url)
                .method(httpMethod, body);

        if (headers != null) {
            for (Map.Entry<String, List<String>> pair : headers.entrySet()) {
                String headerName = pair.getKey();
                for (String headerValue : pair.getValue()) {
                    requestBuilder.addHeader(headerName, headerValue);
                }
            }
        }

        okhttp3.Response response = client.newCall(requestBuilder.build()).execute();

        int responseCode = response.code();
        String responseMessage = response.message();
        Map<String, List<String>> responseHeaders = response.headers().toMultimap();
        
        ResponseBody body_ = response.body();
        String responseBody = body_ != null ? body_.string() : "";

        return new Response(responseCode, responseMessage, responseHeaders, responseBody, request.url());
    }
}

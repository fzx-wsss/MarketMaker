package com.wsss.market.maker.model.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpUtils {
    private static OkHttpClient httpClient = new OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory(), x509TrustManager())
            .retryOnConnectionFailure(false)
            .connectionPool(pool())
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build();;

    public static String doGetJson(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(url);

        if (params != null && params.size() > 0) {
            sb.append("?");
            params.forEach((k, v) -> {
                sb.append(k).append("=").append(v).append("&");
            });
            sb.setLength(sb.length()-1);
        }

        Request.Builder reqBuilder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            headers.forEach(reqBuilder::addHeader);
        }
        Request request = reqBuilder.url(sb.toString()).build();
        Response response = httpClient.newCall(request).execute();
        return response.body().string();
    }


    private static ConnectionPool pool() {
        return new ConnectionPool(100, 5, TimeUnit.MINUTES);
    }

    private static X509TrustManager x509TrustManager() {
        return new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

        };
    }

    private static SSLSocketFactory sslSocketFactory() {
        try {
            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(null, new TrustManager[] {x509TrustManager()}, new SecureRandom());
            return sslCtx.getSocketFactory();
        }
        catch(NoSuchAlgorithmException ex) {
            log.error(ex.getMessage(), ex);
        }
        catch(KeyManagementException ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }
}

// /model/NerClient.java

package com.example.eta.model;

import android.content.Context;
import android.util.Log;

import com.example.eta.R;
import com.example.eta.service.NerApi;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NerClient {
    private static Retrofit retrofit = null;
    // 실제 통신에 사용할 IP 주소 또는 도메인으로 변경하세요.
    // 예: "https://<YOUR_EC2_PUBLIC_IP>:7070"
    private static final String BASE_URL = "https://172.31.57.147:7070";

    public static NerApi getClient(Context context) {
        if (retrofit == null) {
            OkHttpClient okHttpClient = getUnsafeOkHttpClient(context);
            if (okHttpClient != null) {
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(okHttpClient) // 자체 서명 인증서를 신뢰하는 클라이언트 설정
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
        }
        return retrofit.create(NerApi.class);
    }

    // 자체 서명 인증서를 신뢰하는 OkHttpClient 생성
    private static OkHttpClient getUnsafeOkHttpClient(Context context) {
        try {
            // 1. res/raw/cert.pem 파일 로드
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = context.getResources().openRawResource(R.raw.cert);
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
            } finally {
                caInput.close();
            }

            // 2. 인증서를 포함하는 KeyStore 생성
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // 3. KeyStore를 사용하는 TrustManager 생성
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // 4. SSLContext 생성 및 TrustManager로 초기화
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // 5. OkHttpClient 빌드
            X509TrustManager trustManager = (X509TrustManager) tmf.getTrustManagers()[0];
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .hostnameVerifier((hostname, session) -> true) // 호스트 이름 검증 비활성화 (개발용)
                    .build();

        } catch (Exception e) {
            Log.e("NerClient", "OkHttpClient 생성 실패", e);
            return null;
        }
    }
}
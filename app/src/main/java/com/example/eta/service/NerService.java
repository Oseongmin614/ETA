// /service/NerService.java

package com.example.eta.service;

import android.content.Context;
import android.util.Log;

import com.example.eta.model.NerCallback;
import com.example.eta.model.NerClient;
import com.example.eta.model.NerRequest;
import com.example.eta.model.NerResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NerService {
    private static final String TAG = "NER_APP";
    private NerApi nerApi;

    // Context를 받는 생성자 추가
    public NerService(Context context) {
        this.nerApi = NerClient.getClient(context);
        testConnection();
    }

    private void testConnection() {
        if (nerApi == null) {
            Log.e(TAG, "NerApi is null. Check NerClient setup.");
            return;
        }
        nerApi.testConnection().enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "서버 연결 성공: " + response.body());
                } else {
                    Log.e(TAG, "서버 연결 응답 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e(TAG, "서버 연결 실패", t);
            }
        });
    }

    public void requestNer(String text, NerCallback callback) {
        if (nerApi == null) {
            callback.onFailure(new IllegalStateException("NerApi not initialized"));
            return;
        }
        NerRequest req = new NerRequest(text);
        // 서버의 응답은 문자열 리스트(List<String>)로 가정합니다.
        nerApi.getNerResult(req).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 리스트가 비어있지 않다면, 첫 번째 요소를 사용하거나 전체 리스트를 문자열로 변환
                    if (!response.body().isEmpty()) {
                        callback.onSuccess(String.join(", ", response.body()));
                    } else {
                        callback.onSuccess("분석된 장소 없음");
                    }
                } else {
                    callback.onFailure(new Exception("응답 실패 또는 내용 없음: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                callback.onFailure(t);
            }
        });
    }
}
package com.example.eta.service;

import android.util.Log;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.poi_item.TMapPOIItem;

import java.util.ArrayList;

/**
 * TMap API를 사용하여 장소를 검색하고 첫 번째 결과의 좌표를 추출하는 클래스.
 */
public class NerPointExtractor {

    private static final String TAG = "NerPointExtractor";

    /**
     * 비동기 검색 결과를 처리하기 위한 콜백 인터페이스.
     */
    public interface TMapSearchCallback {
        /**
         * 검색에 성공했을 때 호출됩니다.
         * @param coordinates "위도,경도" 형식의 좌표 문자열
         */
        void onSuccess(String coordinates);

        /**
         * 검색에 실패했거나 결과가 없을 때 호출됩니다.
         * @param errorMessage 오류 메시지
         */
        void onFailure(String errorMessage);
    }

    /**
     * 주어진 검색어로 장소를 검색하고 첫 번째 결과의 좌표를 콜백으로 반환합니다.
     * @param searchText 검색할 장소 이름
     * @param callback 결과를 전달받을 콜백 객체
     */
    public void getFirstPOICoordinates(String searchText, final TMapSearchCallback callback) {
        if (searchText == null || searchText.trim().isEmpty()) {
            callback.onFailure("검색어가 비어있습니다.");
            return;
        }

        TMapData tMapData = new TMapData();
        tMapData.findAllPOI(searchText, new TMapData.FindAllPOIListenerCallback() {
            @Override
            public void onFindAllPOI(ArrayList<TMapPOIItem> poiItems) {
                if (poiItems != null && !poiItems.isEmpty()) {
                    // 첫 번째 검색 결과 가져오기
                    TMapPOIItem firstItem = poiItems.get(0);
                    TMapPoint point = firstItem.getPOIPoint();

                    if (point != null) {
                        // 위도와 경도를 문자열로 조합
                        String coordinates = point.getLatitude() + "," + point.getLongitude();
                        Log.d(TAG, "검색 성공: " + firstItem.getPOIName() + ", 좌표: " + coordinates);
                        callback.onSuccess(coordinates);
                    } else {
                        Log.e(TAG, "오류: 첫 번째 검색 결과에 좌표 정보가 없습니다.");
                        callback.onFailure("좌표 정보를 찾을 수 없습니다.");
                    }
                } else {
                    Log.d(TAG, "검색 결과가 없습니다.");
                    callback.onFailure("검색 결과가 없습니다.");
                }
            }
        });
    }
}
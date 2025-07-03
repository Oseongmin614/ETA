package com.example.eta.model;

public interface NerCallback {

        void onSuccess(String result);
        void onFailure(Throwable t);

}

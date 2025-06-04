package com.example.eta.util;

import android.location.Location;

public interface LocationResultListener {
    void onLocationSuccess(Location location);
    void onLocationFailure(String errorMessage);
    void onPermissionNeeded();
}

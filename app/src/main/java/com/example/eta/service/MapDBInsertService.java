package com.example.eta.service;

import android.widget.Toast;

import com.example.eta.activity.MapActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MapDBInsertService {
    private final DatabaseReference mDatabase;
    private static final String TAG = "MapDBInsertService";

    public MapDBInsertService(){
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void insert(String chatRoomId, String colum, String value){
        String messageId = mDatabase.child("chatRooms").child(chatRoomId).child("Coordinates").child(colum).push().getKey();
        if (messageId != null) {
            mDatabase.child("chatRooms").child(chatRoomId).child("Coordinates").child(colum).setValue(value);
        }
    }

    public void gps(String chatRoomId, String colum, String value){
        String messageId = mDatabase.child("chatRooms").child(chatRoomId).child("gps").child(colum).push().getKey();
        if (messageId != null) {
            mDatabase.child("chatRooms").child(chatRoomId).child("gps").child(colum).setValue(value);
        }
    }
}

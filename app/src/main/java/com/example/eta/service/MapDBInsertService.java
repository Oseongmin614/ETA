package com.example.eta.service;

import android.widget.Toast;

import com.example.eta.activity.ChatActivity;
import com.example.eta.activity.MapActivity;
import com.example.eta.model.ChatMessage;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MapDBInsertService {
    private final DatabaseReference mDatabase;
    private static final String TAG = "MapDBInsertService";

    public MapDBInsertService(){
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void insert(String chatRoomId, String userId, String value){
            mDatabase.child("chatRooms").child(chatRoomId).child("maps").child("Coordinates").child(userId).setValue(value);
    }

    public void insertGPS(String chatRoomId, String userId, String value){
            mDatabase.child("chatRooms").child(chatRoomId).child("maps").child("gps").child(userId).setValue(value);

    }

    public void insertTime(String chatRoomId, String userId, String value){
            mDatabase.child("chatRooms").child(chatRoomId).child("maps").child("requiredtime").child(userId).setValue(value);
    }
    public void insertEnd(String chatRoomId, String userId){
        mDatabase.child("chatRooms").child(chatRoomId).child("maps").child("ifStart").child(userId).setValue("end");
    }

}

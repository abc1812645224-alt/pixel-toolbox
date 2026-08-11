package com.example.pixeltoolbox;

interface ILogCallback {
    void onLogEvent(String level, String tag, String message, String throwableStackTrace);
}
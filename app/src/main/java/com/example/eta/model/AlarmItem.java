package com.example.eta.model;

public class AlarmItem {
    private String timeText;
    private int requestCode;
    private boolean isEnabled;
    private long timeInMillis;

    // 기본 생성자
    public AlarmItem() {
        this.isEnabled = true;
    }

    // 생성자
    public AlarmItem(String timeText, int requestCode) {
        this.timeText = timeText;
        this.requestCode = requestCode;
        this.isEnabled = true;
        this.timeInMillis = System.currentTimeMillis();
    }

    public AlarmItem(String timeText, int requestCode, boolean isEnabled, long timeInMillis) {
        this.timeText = timeText;
        this.requestCode = requestCode;
        this.isEnabled = isEnabled;
        this.timeInMillis = timeInMillis;
    }

    // Getter
    public String getTimeText() {
        return timeText;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    // Setter
    public void setTimeText(String timeText) {
        this.timeText = timeText;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    @Override
    public String toString() {
        return "AlarmItem{" +
                "timeText='" + timeText + '\'' +
                ", requestCode=" + requestCode +
                ", isEnabled=" + isEnabled +
                ", timeInMillis=" + timeInMillis +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AlarmItem alarmItem = (AlarmItem) obj;
        return requestCode == alarmItem.requestCode;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(requestCode);
    }
}
public class MKVVideoTrack {
    private int id;
    private String codecId;
    private long defaultDuration;
    private String displayDimensions;
    private String language;

    // Constructor
    public MKVVideoTrack(int id, String codecId, long defaultDuration, String displayDimensions, String language) {
        this.id = id;
        this.codecId = codecId;
        this.defaultDuration = defaultDuration;
        this.displayDimensions = displayDimensions;
        this.language = language;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getCodecId() {
        return codecId;
    }

    public long getDefaultDuration() {
        return defaultDuration;
    }

    public float getFrameRate() {
        return (float) (1000 * 1000 * 1000) / (float) defaultDuration;
    }

    public String getDisplayDimensions() {
        return displayDimensions;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "MKVVideoTrack {" +
                "id=" + id +
                ", codecId='" + codecId + '\'' +
                ", defaultDuration=" + defaultDuration +
                ", frameRate=" + getFrameRate() + '\'' +
                ", displayDimensions='" + displayDimensions + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}


public class MKVSubtitleTrack {
    private int id;
    private String language;
    private boolean defaultTrack;
    private boolean forcedTrack;

    // Constructor
    public MKVSubtitleTrack(int id, String language, boolean defaultTrack, boolean forcedTrack) {
        this.id = id;
        this.language = language;
        this.defaultTrack = defaultTrack;
        this.forcedTrack = forcedTrack;
    }
    // Getters
    public int getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }   

    public boolean isDefaultTrack() {
        return defaultTrack;
    }

    public boolean isForcedTrack() {
        return forcedTrack;
    }
    @Override
    public String toString() {
        return "MKVSubtitleTrack {" +
                "id=" + id +
                ", language='" + language + '\'' +
                ", defaultTrack=" + defaultTrack +
                ", forcedTrack=" + forcedTrack +
                '}';
    }
}

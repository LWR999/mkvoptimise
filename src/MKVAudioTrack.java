public class MKVAudioTrack {
    private int id;
    private String codecId;
    private boolean defaultTrack;
    private int audioChannels;
    private int audioSamplingFrequency;
    private String language;

    // Constructor
    public MKVAudioTrack(int id, String codecId, boolean defaultTrack, int audioChannels, int audioSamplingFrequency, String language) {
        this.id = id;
        this.codecId = codecId;
        this.defaultTrack = defaultTrack;
        this.audioChannels = audioChannels;
        this.audioSamplingFrequency = audioSamplingFrequency;
        this.language = language;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getCodecId() {
        return codecId;
    }

    public boolean isDefaultTrack() {
        return defaultTrack;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public int getAudioSamplingFrequency() {
        return audioSamplingFrequency;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "MKVAudioTrack {" +
                "id=" + id +
                ", codecId='" + codecId + '\'' +
                ", defaultTrack=" + defaultTrack +
                ", audioChannels=" + audioChannels +
                ", audioSamplingFrequency=" + audioSamplingFrequency +
                ", language='" + language + '\'' +
                '}';
    }
}

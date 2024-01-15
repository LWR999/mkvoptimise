import java.io.*;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.json.*;

public class MKVInfoParser {
    // Collections to hold track objects
    private static List<MKVVideoTrack> videoTracks = new ArrayList<>();
    private static List<MKVAudioTrack> audioTracks = new ArrayList<>();
    private static List<MKVSubtitleTrack> subtitleTracks = new ArrayList<>();
    private static MKVVideoTrack bestVideoTrack;
    private static List<MKVAudioTrack> bestAudioTracks;
    private static List<MKVSubtitleTrack> bestSubtitleTracks;
    private static String fileName; 
    private static String originalFileName;
    private static final String inputFileName = "input.mkv";

    public static void main(String[] args) throws IOException, JSONException {
        if (args.length < 2) {
            System.out.println("Usage: java MKVInfoParser <mode> <filename>");
            System.out.println("<mode> can be -i (for info) or -o (for optimizing)");
            return;
            //args = new String[2];
            //args[0] = "-o";
            //args[1] = "/Volumes/_Torrents/completed/4K/FOE (2023)/FOE (2023).mkv";
            //System.out.println("Using test Superbad test args in info mode");
        }

        originalFileName = args[1]; // Filename from arguments
        if (fileExists(originalFileName)) {
            // Build JSON output from mkvmerge command
            buildMKVJSONData(originalFileName);

            // Do further processing in info mode or optimize mode
            String mode = args[0];
            doInfoMode(); // Always do info mode
            if ((mode.equals("-o")) && (isProcessingWorthIt())) doOptimiseMode(originalFileName);
        }
        else {
            System.out.println("File " + originalFileName + " does not exist");
        }
    }

    // buildMKVJSONData - build our collections (audiotracks, videotracks, subtitletracks) from the JSON file created by MKVMerge
    private static void buildMKVJSONData(String fileName) {
        // Build collection data for mkvfile
        String jsonOutput = null;
        JSONObject jsonObject = null;
        try { 
            jsonOutput = executeCommand("mkvmerge -J " + escapeUnixShellCharacters(fileName));
            jsonObject = new JSONObject(jsonOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Extracting file name
        String extractedFileName = jsonObject.getString("file_name");
        System.out.println("File Name: " + extractedFileName);
  
        // Parse the JSON data
        JSONArray tracks = jsonObject.getJSONArray("tracks");

        // Process each track
        for (int i = 0; i < tracks.length(); i++) {
            JSONObject track = tracks.getJSONObject(i);
            String type = track.getString("type");
            if (type.equals("video")) {
                processVideoTrack(track);
            }
            else if (type.equals("audio")) {
                processAudioTrack(track);
            }
            else if (type.equals("subtitles")) {
                processSubtitleTrack(track);
            }
        }
    }

    // doInfoMode - print relavant video, audio and subtitle track info
    private static void doInfoMode() {
        for (MKVVideoTrack track : videoTracks) {
            System.out.println("Video Track ID     : " + track.getId() + " / CodecID: " + track.getCodecId() + " / Frame Rate: " + track.getFrameRate() + " / Display Dimensions: " + track.getDisplayDimensions() + " / Language: " + track.getLanguage());
        }
        for (MKVAudioTrack track : audioTracks) {
            System.out.println("Audio Track ID     : " + track.getId() + " / CodecID: " + track.getCodecId() + " / Audio Channels: " + track.getAudioChannels() + " / Audio Sampling Frequency: " + track.getAudioSamplingFrequency() + " / Language: " + track.getLanguage()); 
        }
        for (MKVSubtitleTrack track : subtitleTracks) {
            System.out.println("Subtitle Track ID  : " + track.getId() + " / Language: " + track.getLanguage() + " / Default Track: " + track.isDefaultTrack() + " / Forced Track: " + track.isForcedTrack());
        }

        // Build a list of the best video/audio/subtitle tracks
        bestAudioTracks = MKVInfoParser.getBestAudioTracks();
        bestSubtitleTracks = MKVInfoParser.getBestSubtitleTracks();
        bestVideoTrack = MKVInfoParser.getBestVideoTrack();
        
        if (isForeignLanguageFilm()) {
            System.out.println("WARNING : Probably a foreign language movie");
            System.out.println("Audio Tracks       : 1 " + bestAudioTracks.get(0).getLanguage().toUpperCase() + " Audio Track as main audio");
        }
        else {
            System.out.println("Audio Tracks       : " + bestAudioTracks.size() + " English Audio Track(s)    / " + (audioTracks.size() - bestAudioTracks.size()) + " Non-English Audio Track(s)");
        }
        System.out.println("Subtitle Tracks    : " + bestSubtitleTracks.size() + " English Subtitle Track(s) / " + (subtitleTracks.size() - bestSubtitleTracks.size()) + " Non-English Subtitle Track(s)");

        if (isProcessingWorthIt()) {
            if (videoTracks.size() > 1) {
                System.out.println("WARNING : Multiple Video Tracks - recommend using Video Track ID: " + bestVideoTrack.getId());
            }
            System.out.println("Recommended params : " + createMKVMergeParams());
        }
        else {
            System.out.println("Info Mode          : No work worth doing in MKVMERGE - exiting");
        }
    }

    // doOptimiseMode - ask MKVMerge to strip out the unwanted tracks 
    private static void doOptimiseMode(String fileName) {
        int exitCode = 0;

        // Rename the file to use for input
        String inputFilePath = renameForInput(fileName);

        // Execute mkvmerge
        String command = "mkvmerge -o " + escapeUnixShellCharacters(fileName) + " " + createMKVMergeParams() +" " + escapeUnixShellCharacters(inputFilePath);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);
        try {
            Process process = processBuilder.start();

            // Read the standard output
            try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = stdInput.readLine()) != null) {
                    System.out.println(line);
                    if (line.startsWith("Progress")) {
                        System.out.print("\033[A");
                    }
                }
            }

            exitCode = process.waitFor();
            if (exitCode == 0)
                deleteInputFile(inputFilePath);
            else {
                renameForLibrary(fileName);
                System.out.println("\nExited with error code : " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
                e.printStackTrace();
        }
    }
    
    private static boolean isProcessingWorthIt() {
        boolean processingWorthIt = false;  // Assume we aren't going to process
        if (videoTracks.size() > 1) {
            processingWorthIt = true; // Always worth doing if more than 1 video track
        }
        else {
            if (audioTracks.size() > bestAudioTracks.size()) {
                processingWorthIt = true; // If more audio tracks than recommended, worth doing   }
            }
        }
        return processingWorthIt;
    }
    
    private static String getAudioTracks() {
        StringJoiner audioTrackIDsJoiner = new StringJoiner(",", "", "");  
        for (MKVAudioTrack track : bestAudioTracks) {
            audioTrackIDsJoiner.add(String.valueOf(track.getId()));
        }
        return audioTrackIDsJoiner.toString();
    }

    private static String getSubtitleTracks() {
        StringJoiner subtitleTrackIDsJoiner = new StringJoiner(",", "", "");
        for (MKVSubtitleTrack track : bestSubtitleTracks) {
            subtitleTrackIDsJoiner.add(String.valueOf(track.getId()));
        }
        return subtitleTrackIDsJoiner.toString();
    }   

    private static String createMKVMergeParams() {
        // Build recommended mkvmerge parameter
        String audioTrackIDs = "--audio-tracks " + getAudioTracks();
        String subtitleTrackIDs = "--subtitle-tracks " + getSubtitleTracks();
        String videoTracksID = "--video-tracks " + bestVideoTrack.getId();
        return videoTracksID + " " + audioTrackIDs + " " + subtitleTrackIDs; // Return mkvmerge parameters as a string      
    }
    
    private static void processVideoTrack(JSONObject track) {
        int id = track.getInt("id");
        String codecId = track.getJSONObject("properties").getString("codec_id");
        long defaultDuration = track.getJSONObject("properties").getLong("default_duration");
        String displayDimensions = track.getJSONObject("properties").getString("display_dimensions");
        String language = track.getJSONObject("properties").getString("language");

        MKVVideoTrack videoTrack = new MKVVideoTrack(id, codecId, defaultDuration, displayDimensions, language);
        videoTracks.add(videoTrack);
    }

    private static void processAudioTrack(JSONObject track) {
        int id = track.getInt("id");
        JSONObject properties = track.getJSONObject("properties");
        String codecId = properties.getString("codec_id");
        boolean defaultTrack = properties.getBoolean("default_track");
        int audioChannels = properties.getInt("audio_channels");
        int audioSamplingFrequency = properties.getInt("audio_sampling_frequency");
        String language = properties.getString("language");

        MKVAudioTrack audioTrack = new MKVAudioTrack(id, codecId, defaultTrack, audioChannels, audioSamplingFrequency, language);
        audioTracks.add(audioTrack);
    }

    private static void processSubtitleTrack(JSONObject track) {
        int id = track.getInt("id");
        JSONObject properties = track.getJSONObject("properties");
        String language = properties.getString("language");
        boolean defaultTrack = properties.getBoolean("default_track");
        boolean forcedTrack = properties.getBoolean("forced_track");
       
        MKVSubtitleTrack subtitleTrack = new MKVSubtitleTrack(id, language, defaultTrack, forcedTrack);
        subtitleTracks.add(subtitleTrack);
    }

    private static String executeCommand(String command) throws IOException {
        StringBuilder output = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) System.out.println("\nExited with error code : " + exitCode);
        } catch (IOException | InterruptedException e) {
                e.printStackTrace();
        }
        return output.toString();
    }

    private static String renameForInput(String fileName) {
        Path filePath = Paths.get(fileName);
        Path inputFile = filePath.getParent().resolve(inputFileName);
        String inputFilePath = inputFile.toString();
        try {
            Files.move(filePath, inputFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println(filePath.toString() + " renamed for input to " + inputFile.toString());
        } catch (IOException e) {
            System.out.println("Error occurred while renaming the file.");
            e.printStackTrace();
        }
        return inputFilePath;
    }
    
    private static void renameForLibrary(String fileNmame) {
        Path filePath = Paths.get(fileName);
        Path inputFile = filePath.getParent().resolve(inputFileName);        
        // rename files back
        try {
            Files.move(inputFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println(inputFile.toString() + "renamed for library to " + filePath.toString());
        } catch (IOException e) {
            System.out.println("Error occurred while renaming the file.");
            e.printStackTrace();
        }
    }

    private static void deleteInputFile(String fileName) {
        Path filePath = Paths.get(fileName);
        try {
            Files.delete(filePath);
            System.out.println("Input file " + filePath.toString() + " deleted successfully.");
        } catch (IOException e) {
            System.out.println("Error occurred while deleting input file" + filePath.toString());
            e.printStackTrace();
        }
    }

    private static boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists() && !file.isDirectory();
    }
    private static String escapeUnixShellCharacters(String input) {
        // Replace characters that need escaping
        return input.replaceAll("([\\s&()])", "\\\\$1");
    }

    private static boolean isForeignLanguageFilm() {
        boolean isForeign = false;
        String foreignLanguage = audioTracks.get(0).getLanguage();
        if ((audioTracks.size() == 1) && (!foreignLanguage.equals("eng"))) {
            isForeign = true;
        }
        return isForeign;
    }

    public static MKVVideoTrack getBestVideoTrack() {
        MKVVideoTrack bestTrack = null;
        
        // Return the single track if there is only one in the list
        if (videoTracks.size() == 1) {
            bestTrack = videoTracks.get(0);
        }
        else {
            int highestResolution = 0;
            for (MKVVideoTrack track : videoTracks) {
                String[] dimensions = track.getDisplayDimensions().split("x");
                if (dimensions.length == 2) {
                    try {
                        int yResolution = Integer.parseInt(dimensions[1]);
                        if (yResolution > highestResolution) {
                            highestResolution = yResolution;
                            bestTrack = track;
                        }
                    } catch (NumberFormatException e) {
                        // Handle the case where the dimension is not a valid integer
                        System.err.println("Invalid dimension format in MKVVideoTrack: " + track.getDisplayDimensions());
                    }
                }
            }
        }
        return bestTrack;
    }

    public static List<MKVAudioTrack> getBestAudioTracks() {
        List<MKVAudioTrack> bestAudioTracks = new ArrayList<>();
        if (audioTracks.size() == 1) {
            bestAudioTracks.add(audioTracks.get(0));
        }
        else {
            for (MKVAudioTrack track : audioTracks) {
                String language = track.getLanguage();
                if (language == null || language.isEmpty() || language.equals("eng")) {
                    bestAudioTracks.add(track);
                }
            }
        }
        return bestAudioTracks;
    }
    
    public static List<MKVSubtitleTrack> getBestSubtitleTracks() {
        List<MKVSubtitleTrack> bestSubtitleTracks = new ArrayList<>();

        for (MKVSubtitleTrack track : subtitleTracks) {
            String language = track.getLanguage();
            if (language == null || language.isEmpty() || language.equals("eng") || track.isForcedTrack()) {
                bestSubtitleTracks.add(track);
            }
        }
        return bestSubtitleTracks;
    }
}
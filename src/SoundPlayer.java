import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/** * Utility class for playing sound clips.
 * Only includes methods for one-shot SFX.
 */
public class SoundPlayer {

    /**
     * Plays a sound clip from the given file path.
     * @param soundFilePath The path to the sound file (e.g., "assets/sounds/hit_regular.wav").
     */
    public static void playSound(String soundFilePath) {
        // Run sound playback on a new thread
        new Thread(() -> {
            try {
                File soundFile = new File(soundFilePath);
                if (!soundFile.exists()) {
                    System.err.println("Sound file not found: " + soundFilePath);
                    return;
                }

                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();

                // --- FIX: Use LineListener to close clip after STOP event ---
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        try {
                            audioIn.close(); // Also close the stream
                        } catch (IOException e) {
                            System.err.println("Error closing audio stream: " + e.getMessage());
                        }
                    }
                });
                // --- END FIX ---

                clip.open(audioIn);
                clip.start();

            } catch (UnsupportedAudioFileException | IOException e) {
                System.err.println("Error loading sound: " + e.getMessage());
            } catch (LineUnavailableException e) {
                // This handles the 'Audio line unavailable' error gracefully
                System.err.println("Warning: Audio line unavailable. Skipping sound: " + soundFilePath);
            }
        }).start();
    }
}
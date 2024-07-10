package nlScript.screenplay;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * ffmpeg command:
 *
 * Download and install https://github.com/rdp/screen-capture-recorder-to-video-windows-free/releases (0.13.3)
 *
 * ./ffmpeg.exe -f gdigrab -framerate 30 -offset_x 0 -offset_y 0 -video_size 3840x2160 -i desktop -f dshow -i audio="virtual-audio-capturer" -c:v libx264 -vf scale=1920x1080 output.mkv -y -preset ultrafast
 */
public class FFMPEG {
	final static String PATH_TO_FFMPEG = "ffmpeg";

	private Process process;

	private Thread thread;

	private String outputPath;

	public void startRecording(Rectangle r, Dimension tgtSize, String outPath) {
		if(isRecording())
			throw new RuntimeException("Already recording");
		try {
			outputPath = outPath != null ? outPath : File.createTempFile("ffmpeg", ".mkv").getAbsolutePath();
		} catch (IOException e) {
			throw new RuntimeException("Cannot create output file", e);
		}
		String[] cmd = makeCmd(r, tgtSize, outputPath);
		run(cmd);
	}

	public void startRecording(String windowTitle, Dimension tgtSize, String outPath) {
		if(isRecording())
			throw new RuntimeException("Already recording");
		try {
			outputPath = outPath != null ? outPath : File.createTempFile("ffmpeg", ".mkv").getAbsolutePath();
		} catch (IOException e) {
			throw new RuntimeException("Cannot create output file", e);
		}
		String[] cmd = makeCmd(windowTitle, tgtSize, outputPath);
		run(cmd);
	}

	private String[] makeCmd(Rectangle r, Dimension tgtSize, String outPath) {
		return new String[] {
				PATH_TO_FFMPEG,
				"-f", "gdigrab",
				"-framerate", "30",
				"-offset_x", Integer.toString(r.x),
				"-offset_y", Integer.toString(r.y),
				"-video_size", r.width + "x" + r.height,
				"-i", "desktop",
				"-f", "dshow",
				"-i", "audio=\"virtual-audio-capturer\"",
				"-c:v", "libx264",
				"-vf", "scale=" + tgtSize.width + "x" + tgtSize.height,
				"-pix_fmt", "yuv420p",
				outPath,
				"-y",
				"-preset", "ultrafast"
		};
	}

	private String[] makeCmd(String windowTitle, Dimension tgtSize, String outPath) {
		return new String[] {
				PATH_TO_FFMPEG,
				"-f", "gdigrab",
				"-framerate", "30",
				"-i", "title=" + windowTitle,
				"-f", "dshow",
				"-i", "audio=\"virtual-audio-capturer\"",
				"-c:v", "libx264",
				"-vf", "scale=" + tgtSize.width + "x" + tgtSize.height,
				"-pix_fmt", "yuv420p",
				outPath,
				"-y",
				"-preset", "ultrafast"
		};
	}

	private void run(String[] cmd) {
		try {
			this.process = Runtime.getRuntime().exec(cmd);

			thread = new Thread(() -> {
				String line;
				System.out.println("Standard Error:");
				BufferedReader stderr = new BufferedReader(new InputStreamReader(
						this.process.getErrorStream()));
				try {
					while ((line = stderr.readLine()) != null)
						System.out.println(line);
					stderr.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			thread.start();
		} catch (IOException e) {
			throw new RuntimeException("Cannot create process for 'record screen' command.", e);
		}
	}

	public void stopRecording() {
		if(process == null)
			return;
		try {
			OutputStream os = process.getOutputStream();
			os.write('q');
			os.flush();
			os.close();
			try {
				thread.join(5000);
				if(thread.isAlive()) {
					thread.interrupt();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			process = null;
		} catch (IOException e) {
			throw new RuntimeException("Cannot create process for 'record screen' command.", e);
		}
	}

	public String getLastRecordedFile() {
		return outputPath;
	}

	public boolean isRecording() {
		return process != null;
	}

	public static void main(String[] args) throws InterruptedException {
		FFMPEG ffmpeg = new FFMPEG();
		ffmpeg.startRecording(new Rectangle(0, 0, 3840, 2160), new Dimension(1280, 800), "D:/output.mkv");
		Thread.sleep(3000);
		ffmpeg.stopRecording();
	}
}

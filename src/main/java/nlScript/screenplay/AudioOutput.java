package nlScript.screenplay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AudioOutput {
	public static void main(String[] args) {
		speak("The positions we've set up in ZEN were automatically defined at the start of the script.");
	}

	public static void speak(String text) {
		text = text.replaceAll("'", "''");
		try {
			Process proc = Runtime.getRuntime().exec(new String[] {"PowerShell",
					"-Command",
					"Add-Type -AssemblyName System.Speech;" +
					"$tts = New-Object System.Speech.Synthesis.SpeechSynthesizer;" +
					"$voices = $tts.GetInstalledVoices();" +
					"$tts.selectvoice('Microsoft Zira Desktop');" +
					"foreach ($voice in $voices) {" +
						"Write-Output($voice.VoiceInfo);" +
					"}" +
					"$tts.speak('" + text + "');"});
			proc.getOutputStream().close();
			String line;
			System.out.println("Standard Output:");
			BufferedReader stdout = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));
			while ((line = stdout.readLine()) != null) {
				System.out.println(line);
			}
			stdout.close();
			System.out.println("Standard Error:");
			BufferedReader stderr = new BufferedReader(new InputStreamReader(
					proc.getErrorStream()));
			while ((line = stderr.readLine()) != null) {
				System.out.println(line);
			}
			stderr.close();
			System.out.println("Done");
		} catch (IOException e) {
			throw new RuntimeException("Cannot create process for 'speak' command.", e);
		}
	}
}

# Screenplay

## Motivation
Screen casts are a popular way to demonstrate and teach new software. However, creating them can be tedious. Typically one needs to practice a couple of times before obtaining the desired result. Even then, it might happen that you do a wrong click, you forget some steps, or an OS message pops up, and you need to start over again.

Then with every software update that changes the software's user interface, screencasts should be be re-created, so that they are in sync with the current software version, to not confuse users, and to not feel outdated.

This is where Screenplay trys to help:

- It creates screen captures automatically based on instructions given in natural English language

- It features plenty of UI automation functions, like moving the mouse pointer, clicking, entering text, activating windows, sizing windows, etc (see below).

- Instructions don't need to be remembered. Intuitive user input is supported by auto-completion (e.g., click coordinates can be picked visually with the mouse pointer).

- It has a so-called validation mode, to update mouse positions to create a cast on a changed/different screen configuration.

- **At the moment, Screenplay is only implemented for Windows**


## Example: Open a website
``` text
Start recording the primary screen.
Wait for 2 second(s).

// Click on the Firefox icon in the taskbar
Mouse click left at (512, 1062) on the Firefox icon.

// Wait until Firefox is open
Wait for window 'Mozilla Firefox'.

// Visit a website
Enter 'https://youtu.be/PKXRzivIRfM', <ENTER>.

Wait for 2 second(s).
Stop recording.
```

## More information
https://nlScript.github.io/screenplay


## License

This project is licensed under the MIT License - see the [LICENSE.txt](LICENSE.txt) file for details.

Screenplay depends on JNA. JNA is selectively licensed under [LGPL-3.0 License](https://www.gnu.org/licenses/lgpl-3.0.html) or [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0). 

Users are entitled to modify and replace the LGPL-licensed JNA library. For more details, please refer to the LGPL-3.0 license text.

If you need to install or modify JNA, it can be obtained from:
[JNA](https://github.com/java-native-access/jna)



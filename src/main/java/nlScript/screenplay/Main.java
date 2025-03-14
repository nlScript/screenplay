package nlScript.screenplay;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import nlScript.Autocompleter;
import nlScript.Parser;
import nlScript.core.Autocompletion;
import nlScript.core.DefaultParsedNode;
import nlScript.core.Matcher;
import nlScript.ui.ACEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


import static java.awt.event.MouseEvent.*;
import static nlScript.screenplay.Main.Mode.RUN;

public class Main implements AutoCloseable {

    private static final Icon recordingIcon = getScaledIcon("/nlScript/screenplay/recording.png", 51, 15);
    private static final Icon recordingPausedIcon = getScaledIcon("/nlScript/screenplay/recording-paused.png", 51, 15);

    private static final Icon playIcon = getScaledIcon("/nlScript/screenplay/play-button-2.png", 300, 300);

    private static Icon getScaledIcon(String resource, int w, int h) {
        ImageIcon icon = new ImageIcon(Main.class.getResource(resource));
        return new ImageIcon(icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH));
    }

    public enum Mode {
        RUN,
        VALIDATE
    }

    static class Replacement {
        final Matcher matcher;
        final String replacement;

        public Replacement(Matcher matcher, String replacement) {
            this.matcher = matcher;
            this.replacement = replacement;
        }
    }

    static class EmergencyExit implements nlScript.screenplay.KeyboardHook.GlobalKeyListener {
        private long firstPressed  = -1;
        private long secondPressed = -1;

        private final Runnable trigger;

        private static final int MAX_TIME_INTERVAL = 500;

        public EmergencyExit(Runnable trigger) {
            this.trigger = trigger;
        }

        @Override
        public void keyPressed(nlScript.screenplay.KeyboardHook.GlobalKeyEvent e) {
            if(e.keycode == KeyEvent.VK_ESCAPE) {
                firstPressed = secondPressed;
                secondPressed = System.currentTimeMillis();
                if (secondPressed - firstPressed < MAX_TIME_INTERVAL)
                    trigger.run();
            }
        }

        @Override
        public void keyReleased(nlScript.screenplay.KeyboardHook.GlobalKeyEvent e) {
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }

    private Mode mode = RUN;

    private int autoDelay = 1500;

    private boolean selectedLinesOnly = false;

    private final nlScript.screenplay.KeyboardHook keyboardHook;

    private final MouseHook mouseHook;

    final nlScript.screenplay.FFMPEG ffmpeg = new FFMPEG();

    private final ACEditor editor;

    private final JLabel recordingLabel = new JLabel(recordingIcon);

    private final nlScript.screenplay.LabelScreenMessage runNotification = new nlScript.screenplay.LabelScreenMessage();

    private final nlScript.screenplay.LabelScreenMessage messages = new nlScript.screenplay.LabelScreenMessage();

    private final nlScript.screenplay.KeyStrokeScreenMessage keystrokeMessages = new KeyStrokeScreenMessage();

    private final nlScript.screenplay.BubbleScreenMessage bubbleMessages = new BubbleScreenMessage();

    private final nlScript.screenplay.LabelScreenMessage validationInstructions = new nlScript.screenplay.LabelScreenMessage();

    private final nlScript.screenplay.LabelScreenMessage autocompletionInstructions = new LabelScreenMessage();

    private final MyRobot robot;

    @Override
    public void close() {
        keyboardHook.close();
    }

    public void cancel() {
        Thread runThread = editor.getRunThread();
        if(runThread == null || !runThread.isAlive())
            return;
        runThread.interrupt();
        try {
            runThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        hideAutocompletionInstruction();
        hideValidationInstruction();
        robot.mbv.getMessage().hide();
        runNotification.hide();
        bubbleMessages.hide();
        keystrokeMessages.hide();
        ffmpeg.stopRecording();
    }

    private void checkInterrupted() {
        boolean interr = Thread.currentThread().isInterrupted();
        if(interr)
            throw new RuntimeException("Interrupted");
    }

    private String getButtonString(int button) {
        return button == BUTTON1 ? "left" :
                (button == BUTTON2 ? "middle" : "right");
    }

    public Main() {
        // TODO enable all hooks only as long as validating

        keyboardHook = new KeyboardHook();
        keyboardHook.setKeyboardHook();

        mouseHook = new MouseHook();
        mouseHook.setMouseHook();

        keyboardHook.addGlobalKeyListener(new EmergencyExit(() -> {
            System.out.println("emergency exit");
            cancel();

        }));

        runNotification.showMessage(playIcon);
        runNotification.setBackground(new Color(0, 0, 0, 0));
        runNotification.setAlignAt(ScreenMessage4.AlignAt.SCREEN);
        runNotification.setAlignment(ScreenMessage4.Alignment.TOP_LEFT);
        runNotification.setPadding(30, 30);
        runNotification.hide();

        messages.setPadding(20);
        messages.setBackground(new Color(0, 0, 0, 200));
        messages.setSize(ScreenMessage4.Size.MAXIMIZED);

        validationInstructions.setPadding(10, 40, 10, 40);
        validationInstructions.setMargin(20);
        validationInstructions.setFontSize(24);
        validationInstructions.setBackground(new Color(50, 104, 189, 150));

        autocompletionInstructions.setPadding(10, 40, 10, 40);
        autocompletionInstructions.setMargin(20);
        autocompletionInstructions.setFontSize(24);
        autocompletionInstructions.setBackground(new Color(0, 0, 0, 150));

        robot = new MyRobot();

        final List<Replacement> replacements = new ArrayList<>();

        Parser parser = new Parser();
        editor = new Editor(parser);


//        parser.defineType("goal", "{ }{purpose:[^.]:+}.", DefaultParsedNode::getParsedString, true);
        parser.defineType("goal", "{ }{purpose:[^.]:+}.", pn -> pn.getParsedString("purpose"), true);
        parser.defineType("goal", ".", e -> null);

        parser.defineType("point", "{point:tuple<int,x,y>}", e -> {
            checkInterrupted();

            int x = (Integer) e.evaluate("point", "x");
            int y = (Integer) e.evaluate("point", "y");
            switch(mode) {
                case RUN:
                    return new Point(x, y);
                case VALIDATE:
                    try {
//                        Point ret = new Point(x, y);
//                        MouseDragOverlay mdo = new MouseDragOverlay(MouseDragOverlay.Mode.SINGLE_POINT);
//                        ArrayList<Point> pList = new ArrayList<>();
//                        pList.add(ret);
//                        mdo.setControls(pList);
//                        mdo.show(true);
//                        pList = mdo.getControls();
//                        if(!pList.isEmpty())
//                            ret = pList.get(0);
//                        String pStr = "(" + ret.x + "," + ret.y + ")";
//                        replacements.add(new Replacement(e.getMatcher(), pStr));
//                        return ret;
                        robot.robot.mouseMove(x, y);
                        keyboardHook.waitForKey(KeyEvent.VK_F2);
                        Point p = MouseInfo.getPointerInfo().getLocation();
                        String pStr = "(" + p.x + ", " + p.y + ")";
                        replacements.add(new Replacement(e.getMatcher(), pStr));
                        return p;
                    } catch(Exception ex) {
                        throw new RuntimeException(ex);
                    }
                default:
                    return null;
            }

        }, (e, justCheck) -> {
            if(!e.getParsedString().isEmpty())
                return Autocompletion.veto(e);
            if(justCheck)
                return Autocompletion.doesAutocomplete(e);
            showAutocompletionInstruction("Position the cursor at the target coordinate, then press <F2>");
            keyboardHook.waitForKey(KeyEvent.VK_F2);
            hideAutocompletionInstruction();
            Point p = MouseInfo.getPointerInfo().getLocation();
            return Autocompletion.literal(e, "(" + p.x + ", " + p.y + ")");
        });

        parser.defineType("mouse-button", "left",   e -> BUTTON1);
        parser.defineType("mouse-button", "middle", e -> BUTTON2);
        parser.defineType("mouse-button", "right",  e -> BUTTON3);

        parser.defineSentence("Mouse click {button:mouse-button} at {point:point}{goal:goal}", e -> {
            checkInterrupted();
            String goal = (String) e.evaluate("goal");
            if (mode == Mode.VALIDATE) {
                String gs = goal == null ? "" : " " + goal;
                showValidationInstruction("Position the cursor" + gs + ", then press <F2>");
            }

            Point p = (Point) e.evaluate("point");
            int button = (Integer) e.evaluate("button");

            if(goal != null)
                showBubbleMessage("Click " + getButtonString(button) + " " + goal, p);


            if (mode == Mode.VALIDATE)
                hideValidationInstruction();
            switch (mode) {
                case RUN:      robot.clickAt(p.x, p.y, button, 1, true);  break;
                case VALIDATE: robot.clickAt(p.x, p.y, button, 1, false); break;
            }
            robot.delay(autoDelay);
            return null;
        });

        parser.defineSentence("Mouse double-click {button:mouse-button} at {point:point}{goal:goal}", e -> {
            checkInterrupted();
            String goal = (String) e.evaluate("goal");
            if(mode == Mode.VALIDATE) {
                String gs = goal == null ? "" : " " + goal;
                showValidationInstruction("Position the cursor" + gs + ", then press <F2>");
            }
            Point p = (Point) e.evaluate("point");
            int button = (Integer) e.evaluate("button");

            if(goal != null)
                showBubbleMessage("Double-click " + getButtonString(button) + " " + goal, p);

            if(mode == Mode.VALIDATE)
                hideValidationInstruction();
            switch (mode) {
                case RUN:      robot.clickAt(p.x, p.y, button, 2, true); break;
                case VALIDATE: robot.clickAt(p.x, p.y, button, 2, false); break;
            }
            robot.delay(autoDelay);
            return null;
        });

        parser.defineSentence("Mouse press {button:mouse-button} at {point:point}{goal:goal}", e -> {
            checkInterrupted();
            String goal = (String) e.evaluate("goal");
            if(mode == Mode.VALIDATE) {
                String gs = goal == null ? "" : " " + goal;
                showValidationInstruction("Position the cursor" + gs + ", then press <F2>");
            }
            Point p = (Point) e.evaluate("point");
            int button = (Integer) e.evaluate("button");

            if(goal != null)
                showBubbleMessage("Press " + getButtonString(button) + " " + goal, p);

            if(mode == Mode.VALIDATE)
                hideValidationInstruction();
            switch (mode) {
                case RUN:      robot.pressAt(p.x, p.y, button, true); break;
                case VALIDATE: robot.pressAt(p.x, p.y, button, false); break;
            }
            robot.delay(autoDelay);
            return null;
        });

        parser.defineSentence("Mouse release {button:mouse-button} at {point:point}{goal:goal}", e -> {
            String goal = (String) e.evaluate("goal");
            checkInterrupted();
            if (mode == Mode.VALIDATE) {
                String gs = goal == null ? "" : " " + goal;
                showValidationInstruction("Position the cursor" + gs + ", then press <F2>");
            }
            Point p = (Point) e.evaluate("point");
            int button = (Integer) e.evaluate("button");

            if(goal != null)
                showBubbleMessage("Release " + getButtonString(button) + " " + goal, p);

            if (mode == Mode.VALIDATE)
                hideValidationInstruction();
            switch (mode) {
                case RUN:      robot.releaseAt(p.x, p.y, button, true); break;
                case VALIDATE: robot.releaseAt(p.x, p.y, button, false); break;
            }
            robot.delay(autoDelay);
            return null;
        });

        parser.defineSentence("Mouse move to {point:point}{goal:goal}", e -> {
            checkInterrupted();
            String goal = (String) e.evaluate("goal");
            if(mode == Mode.VALIDATE) {
                String gs = goal == null ? "" : " " + goal;
                showValidationInstruction("Position the cursor" + gs + ", then press <F2>");
            }
            Point p = (Point) e.evaluate("point");

            if(goal != null)
                showBubbleMessage("Move here " + goal, p);

            if(mode == Mode.VALIDATE)
                hideValidationInstruction();
            switch (mode) {
                case RUN:      robot.moveTo(p.x, p.y, true); break;
                case VALIDATE: robot.moveTo(p.x, p.y, false); break;
            }
            robot.delay(autoDelay);
            return null;
        });

        parser.defineType("pointlist", "{pointlist:list<point>:+}",
                e -> {
                    if(mode == RUN)
                        return e.evaluate("pointlist");

                    mode = RUN;
                    Object[] ret = (Object[]) e.evaluate("pointlist");
                    mode = Mode.VALIDATE;

                    MouseDragOverlay mdo = new MouseDragOverlay(MouseDragOverlay.Mode.CURVE);
                    mdo.setControls(Arrays.stream(ret).map(o -> (Point) o).collect(Collectors.toList()));
                    mdo.show(true);
                    ArrayList<Point> splineControls = mdo.getControls();
                    String pStr = splineControls.stream().map(p -> "(" + p.x + "," + p.y + ")").collect(Collectors.joining(","));
                    replacements.add(new Replacement(e.getMatcher(), pStr));
                    return splineControls.toArray();
                },
                (e, justCheck) -> {
                    if (justCheck)
                        return Autocompletion.doesAutocomplete(e);
                    if (!e.getParsedString().isEmpty())
                        return Autocompletion.literal(e, "");
                    MouseDragOverlay mdo = new MouseDragOverlay(MouseDragOverlay.Mode.CURVE);
                    mdo.showWithClickThrough(mouseHook, keyboardHook);
                    ArrayList<Point> splineControls = mdo.getControls();
                    return Autocompletion.literal(e, splineControls.stream().map(p -> "(" + p.x + "," + p.y + ")").collect(Collectors.joining(",")));
                });

        parser.defineSentence("Mouse move along coordinates {pointlist:pointlist}.", e -> {
            Object[] pointsAsObjects = (Object[]) e.evaluate("pointlist");
            Point[] path = Arrays.stream((pointsAsObjects)).map(o -> (Point) o).toArray(Point[]::new);
            int button = -1;
            robot.move(path, button, mode == RUN);
            robot.delay(autoDelay);
            return null;
        });

        parser.defineSentence("Mouse drag {button:mouse-button} along coordinates {pointlist:pointlist}.", e -> {
            Object[] pointsAsObjects = (Object[]) e.evaluate("pointlist");
            Point[] path = Arrays.stream((pointsAsObjects)).map(o -> (Point) o).toArray(Point[]::new);
            int button = (Integer) e.evaluate("button");
            robot.move(path, button, mode == RUN);
            robot.delay(autoDelay);
            return null;
        });

        final ScrollCounter scrollCounter = new ScrollCounter(mouseHook);

        parser.defineType("scroll-ticks", "{ticks:int} tick(s)", e -> {
            checkInterrupted();
            switch(mode) {
                case RUN:
                    return e.evaluate("ticks");
                case VALIDATE:
                    showValidationInstruction("Scroll events are now recorded. Press <F2> when finished");
                    scrollCounter.start();
                    keyboardHook.waitForKey(KeyEvent.VK_F2);
                    hideValidationInstruction();
                    int ticks = scrollCounter.stop();
                    replacements.add(new Replacement(e.getMatcher(), ticks + " tick(s)"));
                    return ticks;
                default:
                    return null;
            }
        }, (e, justCheck) -> {
            if(justCheck)
                return Autocompletion.doesAutocomplete(e);
            showAutocompletionInstruction("Scroll events are now recorded. Press <F2> when finished");
            scrollCounter.start();
            keyboardHook.waitForKey(KeyEvent.VK_F2);
            hideAutocompletionInstruction();
            int ticks = scrollCounter.stop();
            return Autocompletion.literal(e, ticks + " tick(s)");
        });

        parser.defineSentence("Mouse scroll by {ticks:scroll-ticks}.", e -> {
            checkInterrupted();
            int ticks = (Integer) e.evaluate("ticks");
            switch(mode) {
                case RUN:      robot.scroll(ticks, true); break;
                case VALIDATE: break; // no need to scroll because it was done interactively
            }
            robot.delay(autoDelay);
            return null;
        });

        parser.defineType("keystroke", "{key:[^>]:+}",
                e -> KeyStroke.getKeyStroke(e.getParsedString()),
                (e, justCheck) -> {
                    if(justCheck)
                        return Autocompletion.doesAutocomplete(e);
                    return Autocompletion.literal(e, KeystrokeDialog.getKeyStrokeString(editor.getTextArea()));
                });

        parser.defineType("entry", "'{text:[^']:+}'",
                e -> e.getParsedString("text"),
                true);

        parser.defineType("another-entry", ", '{text:[^']:+}'",
                e -> e.getParsedString("text"),
                true);

        parser.defineType("entry", "<{key:keystroke}>",
                e -> e.evaluate("key"),
                new Autocompleter.EntireSequenceCompleter(parser.getTargetGrammar(), new HashMap<>()) {
                    @Override
                    public Autocompletion[] getAutocompletion(DefaultParsedNode pn, boolean justCheck) {
                        if(pn.getParsedString().isEmpty()) {
                            Autocompletion.EntireSequence es = new Autocompletion.EntireSequence(pn);
                            es.addLiteral(pn.getChild(0).getSymbol(), null, "<");
                            Autocompletion.Parameterized pp = new Autocompletion.Parameterized(pn.getChild(1).getSymbol(), "key", "key");
                            pp.setAutocompleteOnActivation(true);
                            es.add(pp);
                            es.addLiteral(pn.getChild(2).getSymbol(), null, ">");
                            return es.asArray();
                        }
                        return null;
                    }
                });

        parser.defineType("another-entry", ", <{key:keystroke}>",
                e -> e.evaluate("key"),
                new Autocompleter.EntireSequenceCompleter(parser.getTargetGrammar(), new HashMap<>()) {
                    @Override
                    public Autocompletion[] getAutocompletion(DefaultParsedNode pn, boolean justCheck) {
                        if(pn.getParsedString().length() < 3) {
                            Autocompletion.EntireSequence es = new Autocompletion.EntireSequence(pn);
                            es.addLiteral(pn.getChild(0).getSymbol(), null, ", <");
                            Autocompletion.Parameterized pp = new Autocompletion.Parameterized(pn.getChild(1).getSymbol(), "key", "key");
                            pp.setAutocompleteOnActivation(true);
                            es.add(pp);
                            es.addLiteral(pn.getChild(2).getSymbol(), null, ">");
                            return es.asArray();
                        }
                        return null;
                    }
                });


        parser.defineSentence("Enter {entry:entry}{remaining:another-entry:*}.", e -> {
            checkInterrupted();
            Object first = e.evaluate("entry");
            Object[] remaining = (Object[]) e.evaluate("remaining");

            Object[] all = new Object[remaining.length + 1];
            all[0] = first;
            System.arraycopy(remaining, 0, all, 1, remaining.length);

            for(int i = 0; i < all.length; i++) {
                Object entry = all[i];
                if(entry instanceof String) {
                    robot.enter((String) entry);
                    robot.delay(300);
                }
                else { // KeyStroke
                    // check ahead how many keystrokes follow:
                    int nKeystrokes = 1;
                    for(int j = i + 1; j < all.length; j++) {
                        if(! (all[j] instanceof KeyStroke))
                            break;
                        nKeystrokes++;
                    }
                    // visualize keystrokes
                    KeyStroke[] ks = new KeyStroke[nKeystrokes];
                    for(int j = 0; j < nKeystrokes; j++) {
                        ks[j] = (KeyStroke) all[i + j];
                    }
                    showKeystrokeMessage(ks);
                    for(int j = 0; j < nKeystrokes; j++) {
                        switch (mode) {
                            case RUN:      robot.enter((KeyStroke) all[i + j], true);  break;
                            case VALIDATE: robot.enter((KeyStroke) all[i + j], false); break;
                        }
                        robot.delay(300);
                    }
                    i += nKeystrokes - 1; // -1 because i is anyway incremented by 1
                }
            }
            robot.delay(autoDelay);
            return null;
        });

        parser.defineSentence("Press key {key:keystroke}.", e -> {
            checkInterrupted();
            KeyStroke ks = (KeyStroke) e.evaluate("key");
            showKeystrokeMessage(ks);
            switch (mode) {
                case RUN:      robot.pressKey(ks, true);  break;
                case VALIDATE: robot.pressKey(ks, false); break;
            }
            robot.delay(autoDelay);
            return null;
        });

        parser.defineSentence("Release key {key:keystroke}.", e -> {
            checkInterrupted();
            KeyStroke ks = (KeyStroke) e.evaluate("key");
            robot.releaseKey(ks);
            robot.delay(autoDelay);
            return null;
        });

        parser.defineSentence("Set the automatic delay between instructions to {delay:float} second(s).", e -> {
            double delay = (Double) e.evaluate("delay");
            this.autoDelay = (int) Math.round(1000 * delay);
            return null;
        });

        parser.defineSentence("//{comment:[^\n]:*}{\n}",
                e -> null,
                new Autocompleter.EntireSequenceCompleter(parser.getTargetGrammar(), new HashMap<>()) {
                    @Override
                    public Autocompletion[] getAutocompletion(DefaultParsedNode pn, boolean justCheck) {
                        if(pn.getParsedString().length() < 2) {
                            Autocompletion.EntireSequence es = new Autocompletion.EntireSequence(pn);
                            es.addLiteral(pn.getChild(0).getSymbol(), null, "// ");
                            es.addParameterized(pn.getChild(1).getSymbol(), "comment", "comment");
                            return es.asArray();
                        }
                        return Autocompletion.veto(pn);
                    }
                });

        parser.defineSentence("[{message:[^\\]]:+}]", e -> {
            String msg = e.getParsedString("message");
            // if it starts with HTML, will just hand it over to the label to be treated as HTML.
            // if it doesn't, and it doesn't contain newlines, will just leave it as it is.
            // if it doesn't, but it does contain newlines, we convert the text to HTML
            if(!msg.startsWith("<html>") && msg.contains("\n"))
                msg = "<html>" + textToHTML(msg) + "</html>";
            showMessage(msg);
            return null;
        }, true);


        parser.defineSentence("Pause.", e -> {
            robot.delay(1000);
            return null;
        });

        parser.defineType("time-to-wait", "{time:float} second(s)", e -> e.evaluate("time"), true);

        parser.defineSentence("{Wait for} {time:time-to-wait}.", e -> {
            checkInterrupted();
            int ms = (int) Math.round(1000 * (Double) e.evaluate("time"));
            robot.delay(ms);
            return null;
        });

        parser.defineType("window", "in front", e -> WindowUtils.getForegroundWindow());

        final AtomicReference<String> lastParsedWindow = new AtomicReference<>();

        parser.defineType("window", "'{window:[^']:+}'",
                e -> e.getParsedString("window"),
                (e, justCheck) -> {
                    if(justCheck) return Autocompletion.doesAutocomplete(e);
                    List<String> windowTitles = WindowUtils.listWindowTitles();
                    return Autocompletion.literal(e, windowTitles, "'", "'");
                }).onSuccessfulParsed(e -> lastParsedWindow.set(e.getParsedString("window")));

        parser.defineSentence("{Wait for} window {window:window}.", e -> {
            String title = (String) e.evaluate("window");
            while(!WindowUtils.listWindowTitles().contains(title)) {
                checkInterrupted();
                System.out.println("Waiting for window " + title);
                robot.delay(500);
            }
            WindowUtils.activateWindow(title);
            return null;
        });

        parser.defineType("window-placement", "{w:int} x {h:int} at ({x:int}, {y:int})", e -> {
            int w = (Integer) e.evaluate("w");
            int h = (Integer) e.evaluate("h");
            int x = (Integer) e.evaluate("x");
            int y = (Integer) e.evaluate("y");
            return new Rectangle(x, y, w, h);
        }, new Autocompleter() {
            @Override
            public Autocompletion[] getAutocompletion(DefaultParsedNode pn, boolean justCheck) {
                if(!pn.getParsedString().isEmpty())
                    return Autocompletion.veto(pn);
                Rectangle r = WindowUtils.getWindowPosition(lastParsedWindow.get());
                return Autocompletion.literal(pn, r.width + " x " + r.height + " at (" + r.x + ", " + r.y + ")");
            }
        });
        parser.defineSentence("Position the window {window:window} {window-placement:window-placement}.", e -> {
            checkInterrupted();
            String window = (String) e.evaluate("window");
            Rectangle r = (Rectangle) e.evaluate("window-placement");
            WindowUtils.setWindowPosition(window, r.x, r.y, r.width, r.height);
            robot.delay(autoDelay);
            return null;
        });

        parser.defineSentence("Activate the window {window:window}.", e -> {
            checkInterrupted();
            String window = (String) e.evaluate("window");
            WindowUtils.activateWindow(window);
            robot.delay(autoDelay);
            return null;
        });

        parser.defineType("window-state", "normal",    e -> WindowUtils.WindowState.NORMAL);
        parser.defineType("window-state", "maximized", e -> WindowUtils.WindowState.MAXIMIZED);
        parser.defineType("window-state", "minimized", e -> WindowUtils.WindowState.MINIMIZED);

        parser.defineSentence("Set the state of the window {window:window} {state:window-state}.", e -> {
            checkInterrupted();
            String window = (String) e.evaluate("window");
            WindowUtils.WindowState state = (WindowUtils.WindowState) e.evaluate("state");
            WindowUtils.setWindowState(window, state);
            robot.delay(autoDelay);
            return null;
        });

        parser.defineType("text-to-speak", "\"{text-to-speak:[^\"]:+}\"", e -> e.getParsedString("text-to-speak"), true);

        parser.defineSentence("Speak {text-to-speak:text-to-speak}.", e -> {
            checkInterrupted();
            if(mode == RUN)
                AudioOutput.speak(e.getParsedString("text-to-speak"));
            return null;
        });

        parser.defineType("screen", "{screen:int}", e -> e.evaluate("screen"), (e, justCheck) -> {
            if(!e.getParsedString().isEmpty())
                return Autocompletion.veto(e);
            if(justCheck)
                return Autocompletion.doesAutocomplete(e);
            showAutocompletionInstruction("Click into the screen to be recorded");
            GraphicsDevice dev = SingleMouseClickHook.getClickedPoint().getDevice();
            hideAutocompletionInstruction();

            GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            for(int i = 0; i < devices.length; i++)
                if(devices[i] == dev)
                    return Autocompletion.literal(e, Integer.toString(i));
            return Autocompletion.literal(e, "");
        });

        parser.defineType("desktop-area", "{w:int} x {h:int} at ({x:int}, {y:int})", e -> {
            int w = (Integer) e.evaluate("w");
            int h = (Integer) e.evaluate("h");
            int x = (Integer) e.evaluate("x");
            int y = (Integer) e.evaluate("y");
            return new Rectangle(x, y, w, h);
        }, new Autocompleter() {
            @Override
            public Autocompletion[] getAutocompletion(DefaultParsedNode pn, boolean justCheck) {
                if(!pn.getParsedString().isEmpty())
                    return Autocompletion.veto(pn);
                if(justCheck)
                    return Autocompletion.doesAutocomplete(pn);
                Rectangle r = FullscreenOverlay.getDraggedRectangle();
                return Autocompletion.literal(pn, r.width + " x " + r.height + " at (" + r.x + ", " + r.y + ")");
            }
        });

        parser.defineSentence("{Start recording} screen {screen:screen}.", e -> {
            checkInterrupted();
            if(mode == Mode.VALIDATE)
                return null;
            int screenIdx = (Integer) e.evaluate("screen");
            Screen screen = Screen.getScreen(screenIdx);
            ffmpeg.startRecording(screen.unscaledBounds, screen.scaledSize, null);
            System.out.println("Started recording to " + ffmpeg.getLastRecordedFile());
            return null;
        });

        parser.defineSentence("{Start recording} the primary screen.", e -> {
            checkInterrupted();
            if(mode == Mode.VALIDATE)
                return null;
            Screen screen = Screen.getPrimaryScreen();
            ffmpeg.startRecording(screen.unscaledBounds, screen.scaledSize, null);
            System.out.println("Started recording to " + ffmpeg.getLastRecordedFile());
            return null;
        });

        parser.defineSentence("{Start recording} area {area:desktop-area}.", e -> {
            checkInterrupted();
            if(mode == Mode.VALIDATE)
                return null;

            Rectangle r = (Rectangle) e.evaluate("area");

            Point upperLeftScaled  = new Point(r.x, r.y);
            Point lowerRightScaled = new Point(r.x + r.width, r.y + r.height);

            Point upperLeft = Screen.getUnscaled(upperLeftScaled);
            Point lowerRight = Screen.getUnscaled(lowerRightScaled);

            Rectangle unscaledBounds = new Rectangle(
                    upperLeft.x,
                    upperLeft.y,
                    lowerRight.x - upperLeft.x,
                    lowerRight.y - upperLeft.y);

            double maxScaling = Math.max(
                Screen.getScreenForPoint(upperLeftScaled).scaling,
                Screen.getScreenForPoint(lowerRightScaled).scaling);

            Dimension scaledSize = new Dimension(
                    (int) Math.round(unscaledBounds.getWidth() / maxScaling),
                    (int) Math.round(unscaledBounds.getHeight() / maxScaling));

            scaledSize.width = scaledSize.width / 2 * 2;
            scaledSize.height = scaledSize.height / 2 * 2;

            ffmpeg.startRecording(unscaledBounds, scaledSize, null);
            return null;
        });

        parser.defineSentence("{Start recording} window {window:window}.", e -> {
            checkInterrupted();
            if(mode == Mode.VALIDATE)
                return null;
            String window = (String) e.evaluate("window");
            Rectangle r = WindowUtils.getWindowPosition(window);
            Dimension d = new Dimension(r.width / 2 * 2, r.height / 2 * 2);
            ffmpeg.startRecording(window, d, null);
            return null;
        });

        parser.defineSentence("Stop recording.", e -> {
            if(mode == Mode.VALIDATE)
                return null;
            ffmpeg.stopRecording();
            String lastRecording = ffmpeg.getLastRecordedFile();
            System.out.println("Recorded " + lastRecording);
            try {
                Desktop.getDesktop().open(new File(lastRecording));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            return null;
        });


        editor.getFrame().setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        ImageIcon appIcon = new ImageIcon(this.getClass().getResource("/nlScript/screenplay/screenplay.png"));
        editor.getFrame().setIconImage(appIcon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH));

        editor.setOnRun(e -> {
            selectedLinesOnly = false;
            mode = RUN;
            editor.run();
        });
        editor.setBeforeRun(() -> {
            if(mode == Mode.VALIDATE)
                replacements.clear();
            if(mode == RUN)
                runNotification.show();
        });
        editor.setAfterRun(() -> {
            if(mode == RUN)
                runNotification.hide();
            if(mode != Mode.VALIDATE)
                return;
            replacements.sort((o1, o2) -> Integer.compare(o2.matcher.pos, o1.matcher.pos));
            StringBuilder script = new StringBuilder(editor.getText());
            for (Replacement r : replacements) {
                int pos = r.matcher.pos;
                if(selectedLinesOnly)
                    pos += editor.getSelectedLinesStart();
                script.replace(pos, pos + r.matcher.parsed.length(), r.replacement);
            }
            SwingUtilities.invokeLater(() -> editor.getTextArea().setText(script.toString()));
        });

        JMenuBar menuBar = editor.getFrame().getJMenuBar();
        JMenu menu = new JMenu("Run");
        JMenuItem mi = new JMenuItem("Run");
        mi.addActionListener(l -> {
            selectedLinesOnly = false;
            mode = RUN;
            editor.run();
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, CTRL_DOWN_MASK));
        menu.add(mi);
        mi = new JMenuItem("Run selected lines");
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, SHIFT_DOWN_MASK | CTRL_DOWN_MASK));
        mi.addActionListener(l -> {
            selectedLinesOnly = true;
            mode = RUN;
            editor.run(true);
        });
        menu.add(mi);
        mi = new JMenuItem("Validate");
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, CTRL_DOWN_MASK));
        mi.addActionListener(l -> {
            selectedLinesOnly = false;
            mode = Mode.VALIDATE;
            editor.run();
        });
        menu.add(mi);
        mi = new JMenuItem("Validate selected lines");
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, SHIFT_DOWN_MASK | CTRL_DOWN_MASK));
        mi.addActionListener(l -> {
            selectedLinesOnly = true;
            mode = Mode.VALIDATE;
            editor.run(true);
        });
        menu.add(mi);

        menuBar.add(menu);

        menu = new JMenu("Messages");

        JCheckBoxMenuItem cmi = new JCheckBoxMenuItem("Show Script Messages", true);
        cmi.addItemListener(e -> messages.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        menu.add(cmi);

        cmi = new JCheckBoxMenuItem("Show Mouse Instructions", true);
        cmi.addItemListener(e -> bubbleMessages.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        menu.add(cmi);

        cmi = new JCheckBoxMenuItem("Show Mouse Buttons", true);
        cmi.addItemListener(e -> robot.mbv.getMessage().setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        menu.add(cmi);

        cmi = new JCheckBoxMenuItem("Show Keyboard Keys", true);
        cmi.addItemListener(e -> keystrokeMessages.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        menu.add(cmi);

        cmi = new JCheckBoxMenuItem("Show Play Indicator", true);
        cmi.addItemListener(e -> runNotification.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        menu.add(cmi);

        menu.addSeparator();

        mi = new JMenuItem("Configure Script Messages");
        mi.addActionListener(l -> messages.configure());
        menu.add(mi);

        menuBar.add(menu);

        Recorder recorder = new Recorder(mouseHook, keyboardHook, editor);

        menu = new JMenu("Record");
        menuBar.add(menu);
        mi = new JMenuItem("Start recording");
        mi.addActionListener(l -> {
            recorder.start();
            recordingLabel.setVisible(true);
            LabelScreenMessage msg = new LabelScreenMessage();
            msg.setAlignAt(ScreenMessage4.AlignAt.SCREEN);
            msg.setAlignment(ScreenMessage4.Alignment.TOP_CENTER);
            msg.setSize(ScreenMessage4.Size.PACKED);
            msg.setMargin(20);
            msg.setPadding(10);
            msg.setFontSize(18);
            msg.setBackground(new Color(0, 0, 0, 250));
            msg.showMessage("Recording is paused. Toggle with [F8].");
            msg.hideAfter(2000);
        });
        menu.add(mi);

        mi = new JMenuItem("Stop recording");
        mi.addActionListener(l -> {
            recorder.pause();
            recordingLabel.setVisible(false);
        });
        menu.add(mi);

        menuBar.add(Box.createHorizontalGlue());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        menuBar.add(recordingLabel, c);
        recordingLabel.setVisible(false);
        recordingLabel.setToolTipText("Toggle with [F8]");

        recorder.setOnToggleIgnoreEvents(() -> {
            recordingLabel.setIcon(recorder.getIgnoreEvents() ? recordingPausedIcon : recordingIcon);
        });

        menuBar.add(Box.createHorizontalStrut(10));

        editor.setMenuBar(menuBar);
        editor.setVisible(true);

//        editor.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
//            @Override public void insertUpdate(DocumentEvent e) {
//                if(WindowUtils.getForegroundWindowHandle().equals(WindowUtils.getHWnd(editor.getFrame()))) {
//                    System.out.println("editor already in focus");
//                    return;
//                }
                // WindowUtils.activateWindow(WindowUtils.getHWnd(editor.getFrame()));
//                JFrame frame = editor.getFrame();
//                int x = frame.getX() + frame.getWidth() / 2;
//                int y = frame.getY() + frame.getInsets().top / 2;
//                Point prev = MouseInfo.getPointerInfo().getLocation();
//                robot.robot.mouseMove(x, y);
//                robot.robot.mousePress(BUTTON1_DOWN_MASK);
//                robot.robot.delay(50);
//                robot.robot.mouseRelease(BUTTON1_DOWN_MASK);
//                robot.robot.mouseMove(prev.x, prev.y);
//            }

//            @Override public void removeUpdate(DocumentEvent e) {}
//
//            @Override public void changedUpdate(DocumentEvent e) {}
//        });
    }

    private void showBubbleMessage(String text, Point p) {
        if(mode == RUN) {
            bubbleMessages.setManualLocation(p);
            bubbleMessages.showMessage(text);
            // assume we comfortably read 13 characters per second
            robot.delay(Math.max(1000, text.length() * 75));
            bubbleMessages.hide();
        }
    }

    public void showValidationInstruction(String text) {
        validationInstructions.showMessage(text);
    }

    public void hideValidationInstruction() {
        validationInstructions.hide();
    }

    public void showAutocompletionInstruction(String text) {
        if(mode == RUN)
            autocompletionInstructions.showMessage(text);
    }

    public void hideAutocompletionInstruction() {
        autocompletionInstructions.hide();
    }

    public void showMessage(String text) {
        if(mode == RUN) {
            messages.showMessage(text);
            // assume we comfortably read 13 characters per second
            robot.delay(Math.max(1000, text.length() * 75));
            messages.hide();
        }
    }

    public void showKeystrokeMessage(KeyStroke... ks) {
        if(mode == RUN) {
            keystrokeMessages.showKeystrokes(ks);
            robot.delay(1000);
            keystrokeMessages.hide();
        }
    }

    // https://stackoverflow.com/questions/5134959/convert-plain-text-to-html-text-in-java
    private static String textToHTML(String text) {
        StringBuilder builder = new StringBuilder();
        boolean previousWasASpace = false;
        for (char c : text.toCharArray()) {
            if (c == ' ') {
                if (previousWasASpace) {
                    builder.append("&nbsp;");
                    previousWasASpace = false;
                    continue;
                }
                previousWasASpace = true;
            } else {
                previousWasASpace = false;
            }
            switch (c) {
                case '<':
                    builder.append("&lt;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '&':
                    builder.append("&amp;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                case '\n':
                    builder.append("<br>");
                    break;
                // We need Tab support here, because we print StackTraces as HTML
                case '\t':
                    builder.append("&nbsp; &nbsp; &nbsp;");
                    break;
                default:
                    if (c < 128) {
                        builder.append(c);
                    } else {
                        builder.append("&#").append((int) c).append(";");
                    }
            }
        }
        return builder.toString();
    }

    private class MyRobot {

        private final Robot robot;

        private final MouseButtonVisualization mbv;

        public MyRobot() {
            try {
                this.robot = new Robot();
            } catch(AWTException e) {
                throw new RuntimeException("Cannot create AWT Robot", e);
            }
            this.mbv = new MouseButtonVisualization(null);
        }

        public void clickAt(int x, int y, int button, int numClicks, boolean animate) {
            int buttonMask = InputEvent.getMaskForButton(button);
            moveTo(x, y, animate);
            if(animate)
                mbv.mouseClicked(x, y, button); // ScreenMessage.showMessage("", iconForButton(buttonMask), 500);
            delay(50);
            checkInterrupted();
            for(int i = 0; i < numClicks; i++) {
                robot.mousePress(buttonMask);
                delay(50);
                robot.mouseRelease(buttonMask);
            }
        }

        public void pressAt(int x, int y, int button, boolean animate) {
            moveTo(x, y, animate);
            delay(50);
            press(button, animate);
        }

        public void press(int button, boolean animate) {
            checkInterrupted();
            int buttonMask = InputEvent.getMaskForButton(button);
            robot.mousePress(buttonMask);
            if(animate) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                mbv.mousePressed(p.x, p.y, button); // ScreenMessage.showMessage("", leftClickIcon);
            }
        }

        public void releaseAt(int x, int y, int button, boolean animate) {
            moveTo(x, y, animate);
            delay(50);
            release(button, animate);
        }

        public void release(int button, boolean animate) {
            checkInterrupted();
            int buttonMask = InputEvent.getMaskForButton(button);
            robot.mouseRelease(buttonMask);
            if(animate) {
                mbv.mouseReleased(); // ScreenMessage.hide();
            }
        }

        /**
         * Move or drag the cursor; moves if <code>button</code> is negative, otherwise drags with the specified button.
         * @param pts points along the path
         * @param button if negative, moves the cursor, otherwise drags with the specified mouse button
         * @param animate if true, adds some periodic delay to mimic real mouse movement
         */
        public void moveOld(Point[] pts, int button, boolean animate) {
            checkInterrupted();

            double within = 2000; // ms // TODO make this a parameter
            int delay = 40;
            int nSegments = pts.length - 1;
            double timePerSegment = within / nSegments;
            int nPerSegment = (int) Math.round(timePerSegment / delay);

            GeneralPath path = new nlScript.screenplay.CurveEditor.Spline2D(
                    Arrays.stream(pts).map(p -> new java.awt.Point(p.x, p.y)).collect(Collectors.toList())).getPath(nPerSegment);
            PathIterator pi = path.getPathIterator(null);
            float[] points = new float[6];

            checkInterrupted();
            pi.currentSegment(points);
            moveTo((int) points[0], (int) points[1], animate);
            if(button >= 0)
                press(button, animate);
            pi.next();
            long prevTimePoint = System.currentTimeMillis();
            while(!pi.isDone()) {
                checkInterrupted();
                pi.currentSegment(points);
                int x = Math.round(points[0]);
                int y = Math.round(points[1]);
                robot.mouseMove(x, y);
                pi.next();
                long nextTimepoint = prevTimePoint + delay;
                long time = System.currentTimeMillis();
                delay((int)(nextTimepoint - time));
                prevTimePoint = nextTimepoint;
            }
            if(button > 0) {
                delay(30);
                release(button, animate);
            }
        }

        public void moveNotSoOld(Point[] pts, int button, boolean animate) {
            checkInterrupted();

            double within = 2000; // ms // TODO make this a parameter
            int delay = 20;
            int nSegments = pts.length - 1;
            double timePerSegment = within / nSegments;
            int nPerSegment = (int) Math.round(timePerSegment / delay);

            GeneralPath path = new CurveEditor.Spline2D(
                    Arrays.stream(pts).map(p -> new java.awt.Point(p.x, p.y)).collect(Collectors.toList())).getPath(nPerSegment);
            PathIterator pi = path.getPathIterator(null);
            float[] points = new float[6];

            checkInterrupted();
            pi.currentSegment(points);
            moveTo((int) points[0], (int) points[1], animate);
            if(button >= 0)
                press(button, animate);
            pi.next();
            long prevTimePoint = System.currentTimeMillis();
            while(!pi.isDone()) {
                checkInterrupted();
                pi.currentSegment(points);
                int x = Math.round(points[0]);
                int y = Math.round(points[1]);
                robot.mouseMove(x, y);
                mbv.mouseMoved(x, y);
                pi.next();
                long nextTimepoint = prevTimePoint + delay;
                long time = System.currentTimeMillis();
                delay((int)(Math.max(0, nextTimepoint - time)));
                prevTimePoint = nextTimepoint;
            }
            if(button > 0) {
                delay(30);
                release(button, animate);
            }
        }

        private double getMouseSpeed(double distance) {
            return (1500 - 1500 * Math.exp(-0.003 * distance));
        }

        public void move(Point[] pts, int button, boolean animate) {
            checkInterrupted();

            moveTo(pts[0].x, pts[0].y, animate);
            if(button >= 0)
                press(button, animate);

            float[] points = new float[6];

            long prevTimePoint = System.currentTimeMillis();

            for(int i = 1; i < pts.length; i++) {
                checkInterrupted();
                Point p0 = pts[i - 1];
                Point p1 = pts[i];
                int dx = p1.x - p0.x;
                int dy = p1.y - p0.y;
                double stepsPerSecond = 40;
                double s = Math.sqrt(dx * dx + dy * dy);
                double mouseSpeed = getMouseSpeed(s);
                double duration = s / mouseSpeed;
                int nSteps = (int) Math.round(duration * stepsPerSecond);
                stepsPerSecond = nSteps / duration;
                int timeToWait = (int)Math.round(1000.0 / stepsPerSecond); // in ms

                GeneralPath path = new CurveEditor.Spline2D(
                        Arrays.stream(pts).map(p -> new java.awt.Point(p.x, p.y)).collect(Collectors.toList())).getPath(nSteps);
                PathIterator pi = path.getPathIterator(null);
                // skip the first [(i-1) * nSteps + 1] entrys
                int toSkip = (i - 1) * nSteps + 1;
                for(int skip = 0; skip < toSkip; skip++)
                    pi.next();

                for(int p = 0; p < nSteps; p++) {
                    checkInterrupted();
                    pi.currentSegment(points);
                    int x = Math.round(points[0]);
                    int y = Math.round(points[1]);
                    robot.mouseMove(x, y);
                    mbv.mouseMoved(x, y);
                    pi.next();
                    long nextTimepoint = prevTimePoint + timeToWait;
                    long time = System.currentTimeMillis();
                    int delay = (int)(Math.max(0, nextTimepoint - time));
                    delay(delay);
                    prevTimePoint = nextTimepoint;
                }
            }

            if(button > 0) {
                delay(30);
                release(button, animate);
            }
        }

        public void scroll(int ticks, boolean animate) {
            if(animate) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                mbv.mouseWheel(p.x, p.y); // ScreenMessage.showMessage("", middleClickIcon, 500);
                for (int i = 0; i < Math.abs(ticks); i++) {
                    checkInterrupted();
                    robot.mouseWheel((int) Math.signum(ticks));
                    delay(40);
                }
            }
            else {
                robot.mouseWheel(ticks);
            }
        }

        public void pressKey(KeyStroke keyStroke, boolean animate) {
            checkInterrupted();
            int modifiers = keyStroke.getModifiers();
            int kc = keyStroke.getKeyCode();
            boolean isShift = (modifiers & SHIFT_DOWN_MASK)     != 0;
            boolean isCtrl  = (modifiers & CTRL_DOWN_MASK)      != 0;
            boolean isAlt   = (modifiers & ALT_DOWN_MASK)       != 0;
            boolean isAltGr = (modifiers & ALT_GRAPH_DOWN_MASK) != 0;
            boolean isMeta  = (modifiers & META_DOWN_MASK)      != 0;

//            if(animate) {
//                keystrokeMessages.showMessage("[" + keyStroke.toString().replace("pressed ", "") + "]");
//                delay(700);
//                keystrokeMessages.hide();
////                ScreenMessage.showMessage("[" + keyStroke.toString().replace("pressed ", "") + "]", null, 700, true);
//            }

            if(isShift) robot.keyPress(KeyEvent.VK_SHIFT);
            if(isCtrl)  robot.keyPress(KeyEvent.VK_CONTROL);
            if(isAlt)   robot.keyPress(KeyEvent.VK_ALT);
            if(isAltGr) robot.keyPress(KeyEvent.VK_ALT_GRAPH);
            if(isMeta)  robot.keyPress(KeyEvent.VK_META);

            robot.keyPress(kc);
        }

        public void releaseKey(KeyStroke keyStroke) {
            checkInterrupted();
            int modifiers = keyStroke.getModifiers();
            int kc = keyStroke.getKeyCode();
            boolean isShift = (modifiers & SHIFT_DOWN_MASK)     != 0;
            boolean isCtrl  = (modifiers & CTRL_DOWN_MASK)      != 0;
            boolean isAlt   = (modifiers & ALT_DOWN_MASK)       != 0;
            boolean isAltGr = (modifiers & ALT_GRAPH_DOWN_MASK) != 0;
            boolean isMeta  = (modifiers & META_DOWN_MASK)      != 0;

            robot.keyRelease(kc);

            if(isShift) robot.keyRelease(KeyEvent.VK_SHIFT);
            if(isCtrl)  robot.keyRelease(KeyEvent.VK_CONTROL);
            if(isAlt)   robot.keyRelease(KeyEvent.VK_ALT);
            if(isAltGr) robot.keyRelease(KeyEvent.VK_ALT_GRAPH);
            if(isMeta)  robot.keyRelease(KeyEvent.VK_META);
        }

        public void enter(KeyStroke keyStroke, boolean animate) {
            pressKey(keyStroke, animate);
            delay(40);
            releaseKey(keyStroke);
        }

        public void enter(String s) {
            for(int i = 0; i < s.length(); i++) {
                typeCharacter(s.charAt(i));
                delay(100);
            }
        }

        /*
         * TODO: This is a horrible hack, but unfortunately the solution which works best, so we'll keep it for now
         */
        public void typeCharacter(char c) {
            String text = Character.toString(c);
            StringSelection stringSelection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, stringSelection);

            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.delay(10);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
        }

        /*
         * https://stackoverflow.com/questions/28538234/sending-a-keyboard-input-with-java-jna-and-sendinput
         * https://stackoverflow.com/questions/21641725/how-to-simulate-string-keyboard-input-using-python
         * https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-sendinput?redirectedfrom=MSDN
         * https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-input
         * https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-keybdinput
         */
        /*
         * Unfortunately, this does not work with some programs, in particular it doesn't work with
         * AWT textfields.
         */
        public void typeCharacterOld(char c) {
            checkInterrupted();
            WinUser.INPUT input = new WinUser.INPUT();

            input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
            input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
            input.input.ki.wScan = new WinDef.WORD(c);
            input.input.ki.time = new WinDef.DWORD(0);
            input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

            // Press "a"
            input.input.ki.wVk = new WinDef.WORD(0);
            input.input.ki.dwFlags = new WinDef.DWORD(WinUser.KEYBDINPUT.KEYEVENTF_UNICODE);

            User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
        }

        public void typeCharacterNotSoOld(char c) {
            WinUser.INPUT input = new WinUser.INPUT();
            input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
            input.input.setType("ki");
            input.input.ki.wScan = new WinDef.WORD(0);
            input.input.ki.time = new WinDef.DWORD(0);
            input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

            // Get the keyboard layout for the current thread
            WinDef.HKL layout = User32.INSTANCE.GetKeyboardLayout(0);

            // Get the virtual-key code and shift state
            short keyData = User32.INSTANCE.VkKeyScanExA((byte)c, layout);
            byte vkCode = (byte) (keyData & 0xFF);
            byte shiftState = (byte) (keyData >> 8);

            // Handle modifier keys
            if ((shiftState & 1) != 0) { // Shift key
                sendModifierKey(User32.VK_SHIFT, true);
            }
            if ((shiftState & 2) != 0) { // Control key
                sendModifierKey(User32.VK_CONTROL, true);
            }
            if ((shiftState & 4) != 0) { // Alt key
                sendModifierKey(User32.VK_MENU, true);
            }

            // Press character key
            input.input.ki.wVk = new WinDef.WORD(vkCode);
            input.input.ki.dwFlags = new WinDef.DWORD(0);  // keydown
            User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

            // Release character key
            input.input.ki.dwFlags = new WinDef.DWORD(WinUser.KEYBDINPUT.KEYEVENTF_KEYUP);  // keyup
            User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

            // Release modifier keys
            if ((shiftState & 1) != 0) { // Shift key
                sendModifierKey(User32.VK_SHIFT, false);
            }
            if ((shiftState & 2) != 0) { // Control key
                sendModifierKey(User32.VK_CONTROL, false);
            }
            if ((shiftState & 4) != 0) { // Alt key
                sendModifierKey(User32.VK_MENU, false);
            }
        }

        private void sendModifierKey(int key, boolean press) {
            WinUser.INPUT input = new WinUser.INPUT();
            input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
            input.input.setType("ki");
            input.input.ki.wVk = new WinDef.WORD(key);
            input.input.ki.wScan = new WinDef.WORD(0);
            input.input.ki.time = new WinDef.DWORD(0);
            input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
            input.input.ki.dwFlags = new WinDef.DWORD(press ? 0 : WinUser.KEYBDINPUT.KEYEVENTF_KEYUP);
            User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
        }

        public void delay(int ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void moveTo(int x, int y, boolean animate) {
            checkInterrupted();
            if(!animate) {
                robot.mouseMove(x,y);
                return;
            }
            Point current = MouseInfo.getPointerInfo().getLocation();
            if(current.distance(new Point(x, y)) <= 3) {
                robot.mouseMove(x, y);
                return;
            }
            PointerInfo pi = MouseInfo.getPointerInfo();
            java.awt.Point p = pi.getLocation();
            int dx = x - p.x;
            int dy = y - p.y;
            double stepsPerSecond = 20;
            double s = Math.sqrt(dx * dx + dy * dy);
            double mouseSpeed = getMouseSpeed(s);
            // double duration = 1.0; // s / MOUSE_SPEED;
            double duration = s / mouseSpeed;
            int nSteps = (int) Math.round(duration * stepsPerSecond);
            stepsPerSecond = nSteps / duration;
            int timeToWait = (int)Math.round(1000.0 / stepsPerSecond); // in ms
            double dxPerStep = (double) dx / nSteps;
            double dyPerStep = (double) dy / nSteps;

            for(int step = 0; step < nSteps; step++) {
                checkInterrupted();
                int xTgt = (int) Math.round(p.x + step * dxPerStep);
                int yTgt = (int) Math.round(p.y + step * dyPerStep);
                robot.mouseMove(xTgt, yTgt);
                delay(timeToWait);
            }
            robot.mouseMove(x, y);
            delay(timeToWait);
        }
    }
}

/*
Right-click at (1369, 1423) on the Firefox icon in the task bar.
Left-click at (1319, 1321) to open Firefox.
Enter keystroke <ctrl pressed L> to focus on the address bar.
Enter literal 'https://euclid.oice.uni-erlangen.de'.
Enter keystroke <pressed ENTER>.

Left-click at (66, 299) to expand the project.
Left-click at (94, 315) to expand the dataset.
Right-click at (139, 337) on the first image.
Left-click at (205, 519) on Open With.
Left-click at (387, 593) to open 3Dscript.

Left-click at (1011, 1327) to render.

Speak "oh shit, I have to fix this.".
 */


/*
Mouse click left at (203, 206).
Enter <ctrl SPACE>, <DOWN>.
Enter <ENTER>.
Enter <DOWN>, <DOWN>, <ENTER>.
Mouse move to (1265, 1425).
Enter <F2>.
Mouse move to (317, 42).
Enter ' on Firefox.', <ENTER>.
Enter <DOWN>, <ENTER>, <ENTER>.
Mouse click right at (1270, 1425).
Mouse move to (1235, 1310).
Enter <F2>.
Mouse click left at (344, 78).
Enter ' on New Window.', <ENTER>.
Mouse click right at (1280, 1409).
Mouse click left at (1235, 1315).
Wait for window 'Mozilla Firefox'.
Position the window 'Mozilla Firefox' 1936 x 2108 at (1912, 0).
Mouse drag left along coordinates (389, 150),(385, 263).
Mouse double-click left at (74, 219) on Wait.
Mouse double-click left at (143, 137) on Window.
Mouse double-click left at (218, 148) on Firefox.


Mouse click left at (174, 247).
Enter <ENTER>.
Enter 'E'.
Enter <ENTER>.
Enter 'romulus.oice.uni-erlangen.de', <ENTER>.
Enter <DOWN>, <ENTER>, <ENTER>.
Enter <ENTER>, <DOWN>, <DOWN>, <ENTER>.
Enter <ENTER>.

Mouse drag left along coordinates (405, 116),(363, 108),(295, 103),(231, 103),(126, 108),(69, 111),(45, 109),(33, 101).
Enter <shift ctrl R>.






Mouse click right at (1265, 1417) on Firefox.
Mouse click left at (1234, 1310) on New Window.
Wait for window 'Mozilla Firefox'.
Position the window 'Mozilla Firefox' 1937 x 2100 at (1903, 0).
Enter 'https://euclid.oice.uni-erlangen.de', <ENTER>.
Mouse click left at (1336, 298) on 3Dscript.
Mouse click left at (1371, 319) on organoids.
Mouse click right at (1406, 333) on the first image.
Mouse click left at (1463, 513) on Open With.
Mouse click left at (1599, 579) on 3Dscript.
Mouse click left at (2097, 710) at the end of the script.
Enter <ENTER>.
Enter 'From frame 0 to frame 200 zoom by a factor of 1.4', <ENTER>.
Mouse click left at (1649, 1320) on Render.
Wait for 15 second(s).
Mouse click left at (1641, 635) on the Play button.
 */


/*
 * Create my own screencast:
 *
 * Mouse click left at (289, 148).
Enter 'M', <ENTER>, <DOWN>, <DOWN>, <ENTER>.
Mouse move to (1273, 1432).
Enter <F2>, ' on Firefox'.
Enter '.', <ENTER>.
Enter 'M', <ENTER>.
Enter <ENTER>.
Mouse click right at (1268, 1421).
Mouse move to (1264, 1319).
Enter <F2>.
Mouse click left at (321, 76).
Enter ' on New Window.', <ENTER>.
Mouse click right at (1271, 1419).
Mouse click left at (1252, 1311).
Wait for window 'Mozilla Firefox'.
Position the window 'Mozilla Firefox' 1937 x 2100 at (1804, 0).
Mouse drag left along coordinates (387, 187),(391, 219),(401, 274),(407, 300).
Mouse double-click left at (87, 222).
Mouse double-click left at (139, 131).
Mouse double-click left at (195, 129).
Mouse click left at (319, 100).
Enter <ENTER>, 'E'.
Enter <ENTER>, 'euclid.oice.uni-erlangen.de', <TAB>, <DOWN>, <ENTER>, <ENTER>, <ENTER>, '.'.
Mouse drag left along coordinates (395, 113),(227, 110),(142, 100),(89, 93),(39, 96),(15, 99).
Enter <shift ctrl R>.
Mouse click left at (128, 160).
Enter <ENTER>, <DOWN>.
Enter <ENTER>.
Enter <ENTER>.
Mouse move to (1272, 292).
Enter <F2>, ' on 3D'.
Enter 'script.'.
Enter <ENTER>.
Enter <shift SHIFT>, <DOWN>.
Enter <ENTER>.
Enter <ENTER>.
Mouse move to (1306, 318).
Enter <F2>.
Enter ' on organoids.'.
Enter <ENTER>.
Enter 'Mou', <ENTER>.
Enter <ENTER>.
Mouse click left at (1306, 318).
Mouse move to (1336, 336).
Enter <F2>.
Mouse click right at (417, 162).
Enter ' on the first image.'.
Enter <ENTER>.
Enter <DOWN>.
Enter <ENTER>, <ENTER>.
Mouse click right at (1337, 342).
Mouse move to (1401, 528).
Enter <F2>.
Mouse click left at (345, 187).
Enter ' on Open With.'.
Enter <ENTER>.
Enter 'M', <ENTER>, <ENTER>.
Mouse move to (1551, 599).
Enter <F2>.
Enter ' on 3Dscript.'.
Mouse click left at (1551, 599).
Enter <ENTER>.
Enter <DOWN>, <ENTER>, <ENTER>.
Mouse move to (2025, 711).
Enter <F2>.
Enter ' at the end of the '.
Enter 'script.'.
Enter <ENTER>.

Enter 'E'.
Enter <DOWN>, <ENTER>, <ENTER>.
Enter <TAB>, <ENTER>.
Enter 'From frame 0 to frame 200 zoom by a factor of 2.'.
Mouse click left at (2028, 703) in the script area.
Enter <ENTER>, <ENTER>.
Enter '0 ', <ENTER>.
Enter '200 z', <ENTER>, '2'.
Mouse click left at (195, 272).
Enter <ENTER>, 'M'.
Enter <ENTER>.
Enter <ENTER>.
Mouse move to (1573, 1324).
Enter <F2>.
Enter ' on Render'.
Enter '.', <ENTER>.
Mouse click left at (1573, 1324).
Mouse click left at (78, 271).
Enter 'W15 s'.
Enter 'e', <ctrl SPACE>, 'cond(s).', <BACK_SPACE>.
Enter <ENTER>.
Enter 'M', <ENTER>, <ENTER>.
Mouse move to (1575, 631).
Enter <F2>.
Enter ' on Play'.
Enter '.'.
Mouse click left at (1575, 631).
Mouse click left at (231, 518) on Run.


 */
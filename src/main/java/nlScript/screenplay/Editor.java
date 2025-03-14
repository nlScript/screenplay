package nlScript.screenplay;

import nlScript.Parser;
import nlScript.ui.ACEditor;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Editor extends ACEditor {

	private File currentFile;

	public Editor(Parser parser) {
		super(parser);

		JMenuBar menuBar = new JMenuBar();

		// Create the File menu with a mnemonic
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F); // Alt + F

		// Determine the platform-specific modifier key (Ctrl for Windows/Linux, Command for macOS)
		int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		// Create menu items with mnemonics and accelerators
		JMenuItem openItem = new JMenuItem("Open");
		openItem.setMnemonic(KeyEvent.VK_O); // Alt + O
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, modifierKey));

		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.setMnemonic(KeyEvent.VK_S); // Alt + S
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, modifierKey));

		JMenuItem saveAsItem = new JMenuItem("Save As");
		saveAsItem.setMnemonic(KeyEvent.VK_A); // Alt + A
		saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifierKey));

		JMenuItem closeItem = new JMenuItem("Close");
		closeItem.setMnemonic(KeyEvent.VK_C); // Alt + C
		closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, modifierKey));




		// Add menu items to the File menu
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.add(saveAsItem);
		fileMenu.add(closeItem);


		// Add the File menu to the menu bar
		menuBar.add(fileMenu);

		// Set the menu bar for the frame
		setMenuBar(menuBar);

		// Add action listeners for the menu items
		openItem.addActionListener(e -> openFile());

		saveItem.addActionListener(e -> saveFile());

		saveAsItem.addActionListener(e -> saveFileAs());

		closeItem.addActionListener(e -> System.exit(0));

		getTextArea().setDropTarget(new DropTarget() {
			@Override
			public synchronized void drop(DropTargetDropEvent dtde) {
				try {
					dtde.acceptDrop(dtde.getDropAction());
					List<File> droppedFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					if(!droppedFiles.isEmpty())
						open(droppedFiles.get(0));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}

	public void open(File file) {
		currentFile = file;
		try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
			getTextArea().read(reader, null);
			getFrame().setTitle("bdv-nlScript - " + currentFile.getName());
		} catch (IOException ex) {
			throw new RuntimeException("Cannot open file " + currentFile, ex);
		}
	}

	private void openFile() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(getFrame());
		if (result == JFileChooser.APPROVE_OPTION)
			open(fileChooser.getSelectedFile());
	}

	private void saveFile() {
		if (currentFile != null) {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
				getTextArea().write(writer);
				getFrame().setTitle("bdv-nlScript - " + currentFile.getName());
			} catch (IOException ex) {
				throw new RuntimeException("Cannot save file " + currentFile, ex);
			}
		} else {
			saveFileAs();
		}
	}

	private void saveFileAs() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showSaveDialog(getFrame());
		if (result == JFileChooser.APPROVE_OPTION) {
			currentFile = fileChooser.getSelectedFile();
			saveFile();
		}
	}
}

package nlScript.screenplay;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

public class ScreenMessage4 {
	protected final JDialog frame;
	protected final JComponent component;

	protected boolean enabled = true;

	private final GridBagLayout gridbag;

	private final GridBagConstraints c;

	private final Insets margin;

	private Alignment alignment = Alignment.TOP_CENTER;

	protected Alignment correctedAlignment = alignment;

	private AlignAt alignAt = AlignAt.SCREEN;

	private Size size = Size.PACKED;

	private final Point manualLocation = new Point();

	public enum Size {
		PACKED {
			public void apply(Window window) {
				window.pack();
			}
		},
		MAXIMIZED {
			public void apply(Window window) {
				Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
				window.setSize(d);
			}
		};

		public abstract void apply(Window window);
	}

	public enum AlignAt {
		SCREEN,
		CURSOR,
		CUSTOM;
	}

	public enum Alignment {
		TOP_LEFT {
			public Point calculateLocation(Window frame, Insets margin, Point ref) {
				return new Point(ref.x + margin.left, ref.y + margin.top);
			}

			public Point calculateLocation(Window frame, Insets margin, Rectangle ref) {
				return calculateLocation(frame, margin, ref.getLocation());
			}

			public void apply(GridBagLayout gridbag, Component comp) {
				GridBagConstraints c = gridbag.getConstraints(comp);
				c.anchor = GridBagConstraints.NORTHWEST;
				gridbag.setConstraints(comp, c);
			}
		},
		TOP_CENTER {
			public Point calculateLocation(Window frame, Insets margin, Point ref) {
				Dimension f = frame.getSize();
				return new Point(ref.x - f.width / 2, ref.y + margin.top);
			}

			public Point calculateLocation(Window frame, Insets margin, Rectangle ref) {
				return calculateLocation(frame, margin, new Point(ref.x + ref.width / 2, ref.y));
			}

			public void apply(GridBagLayout gridbag, Component comp) {
				GridBagConstraints c = gridbag.getConstraints(comp);
				c.anchor = GridBagConstraints.NORTH;
				gridbag.setConstraints(comp, c);
			}
		},
		TOP_RIGHT {
			public Point calculateLocation(Window frame, Insets margin, Point ref) {
				Dimension f = frame.getSize();
				return new Point(ref.x - f.width - margin.right, ref.y + margin.top);
			}

			public Point calculateLocation(Window frame, Insets margin, Rectangle ref) {
				return calculateLocation(frame, margin, new Point(ref.x + ref.width, ref.y));
			}

			public void apply(GridBagLayout gridbag, Component comp) {
				GridBagConstraints c = gridbag.getConstraints(comp);
				c.anchor = GridBagConstraints.NORTHEAST;
				gridbag.setConstraints(comp, c);
			}
		},
		CENTER_LEFT {
			public Point calculateLocation(Window frame, Insets margin, Point ref) {
				Dimension f = frame.getSize();
				return new Point(ref.x + margin.left, ref.y - f.height / 2);
			}

			public Point calculateLocation(Window frame, Insets margin, Rectangle ref) {
				return calculateLocation(frame, margin, new Point(ref.x, ref.y + ref.height / 2));
			}

			public void apply(GridBagLayout gridbag, Component comp) {
				GridBagConstraints c = gridbag.getConstraints(comp);
				c.anchor = GridBagConstraints.WEST;
				gridbag.setConstraints(comp, c);
			}
		},
		CENTER {
			public Point calculateLocation(Window frame, Insets margin, Point ref) {
				Dimension f = frame.getSize();
				return new Point(ref.x - f.width / 2, ref.y - f.height / 2);
			}

			public Point calculateLocation(Window frame, Insets margin, Rectangle ref) {
				return calculateLocation(frame, margin, new Point(ref.x + ref.width / 2, ref.y + ref.height / 2));
			}

			public void apply(GridBagLayout gridbag, Component comp) {
				GridBagConstraints c = gridbag.getConstraints(comp);
				c.anchor = GridBagConstraints.CENTER;
				gridbag.setConstraints(comp, c);
			}
		},
		CENTER_RIGHT {
			public Point calculateLocation(Window frame, Insets margin, Point ref) {
				Dimension f = frame.getSize();
				return new Point(ref.x - f.width - margin.right, ref.y - f.height / 2);
			}

			public Point calculateLocation(Window frame, Insets margin, Rectangle ref) {
				return calculateLocation(frame, margin, new Point(ref.x + ref.width, ref.y + ref.height / 2));
			}

			public void apply(GridBagLayout gridbag, Component comp) {
				GridBagConstraints c = gridbag.getConstraints(comp);
				c.anchor = GridBagConstraints.EAST;
				gridbag.setConstraints(comp, c);
			}
		},
		BOTTOM_LEFT {
			public Point calculateLocation(Window frame, Insets margin, Point ref) {
				Dimension f = frame.getSize();
				return new Point(ref.x + margin.left, ref.y - f.height - margin.bottom);
			}

			public Point calculateLocation(Window frame, Insets margin, Rectangle ref) {
				return calculateLocation(frame, margin, new Point(ref.x, ref.y + ref.height));
			}

			public void apply(GridBagLayout gridbag, Component comp) {
				GridBagConstraints c = gridbag.getConstraints(comp);
				c.anchor = GridBagConstraints.SOUTHWEST;
				gridbag.setConstraints(comp, c);
			}
		},
		BOTTOM_CENTER {
			public Point calculateLocation(Window frame, Insets margin, Point ref) {
				Dimension f = frame.getSize();
				return new Point(ref.x - f.width / 2, ref.y - f.height - margin.bottom);
			}

			public Point calculateLocation(Window frame, Insets margin, Rectangle ref) {
				return calculateLocation(frame, margin, new Point(ref.x + ref.width / 2, ref.y + ref.height));
			}

			public void apply(GridBagLayout gridbag, Component comp) {
				GridBagConstraints c = gridbag.getConstraints(comp);
				c.anchor = GridBagConstraints.SOUTH;
				gridbag.setConstraints(comp, c);
			}
		},
		BOTTOM_RIGHT {
			public Point calculateLocation(Window frame, Insets margin, Point ref) {
				Dimension f = frame.getSize();
				return new Point(ref.x - f.width - margin.right, ref.y - f.height - margin.bottom);
			}

			public Point calculateLocation(Window frame, Insets margin, Rectangle ref) {
				return calculateLocation(frame, margin, new Point(ref.x + ref.width, ref.y + ref.height));
			}

			public void apply(GridBagLayout gridbag, Component comp) {
				GridBagConstraints c = gridbag.getConstraints(comp);
				c.anchor = GridBagConstraints.SOUTHEAST;
				gridbag.setConstraints(comp, c);
			}
		};

		public abstract Point calculateLocation(Window frame, Insets margin, Point ref);

		public abstract Point calculateLocation(Window frame, Insets margin, Rectangle ref);

		public abstract void apply(GridBagLayout layout, Component comp);
	}

	public ScreenMessage4(JComponent component) {
		this.component = component;
		JFrame parent = new JFrame();
		frame = new JDialog(parent);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setUndecorated(true); // Remove window decorations (title bar, etc.)
		frame.setBackground(new Color(0, 0, 0, 150)); // Make the window transparent
		frame.setAlwaysOnTop(true); // Keep the window on top of other windows
		frame.setFocusable(false);
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 20, 20));
			}
		});

		margin = new Insets(0, 0, 0, 0);

		gridbag = new GridBagLayout();
		c = new GridBagConstraints();
		frame.getContentPane().setLayout(gridbag);

		c.gridx = 0; c.gridy = 0;
		c.insets = new Insets(0, 0, 0, 0);
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 1.0;
		c.weighty = 1.0;
		frame.getContentPane().add(component, c);
	}

	public void close() {
		hide();
		frame.dispose();
	}

	public ScreenMessage4 hide() {
		if(frame.isVisible())
			frame.setVisible(false);
		return this;
	}

	public ScreenMessage4 hideAfter(int ms) {
		Timer timer = new Timer(ms, e -> hide());
		timer.setRepeats(false);
		timer.start();
		return this;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Color getForeground() {
		return component.getForeground();
	}

	public ScreenMessage4 setForeground(Color color) {
		component.setForeground(color);
		return this;
	}

	public Color getBackground() {
		return frame.getBackground();
	}

	public ScreenMessage4 setBackground(Color color) {
		int a = color.getAlpha();
		if(a > 254)
			color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 254);
		frame.setBackground(color);
		return this;
	}

	public ScreenMessage4 setPadding(int top, int left, int bottom, int right) {
		c.insets = new Insets(top, left, bottom, right);
		gridbag.setConstraints(component, c);
		component.invalidate();
		frame.doLayout();
		return this;
	}

	public ScreenMessage4 setPadding(int vertical, int horizontal) {
		return setPadding(vertical, horizontal, vertical, horizontal);
	}

	public ScreenMessage4 setPadding(int insets) {
		return setPadding(insets, insets);
	}

	public ScreenMessage4 setMargin(int top, int left, int bottom, int right) {
		this.margin.set(top, left, bottom, right);
		return this;
	}

	public ScreenMessage4 setMargin(int vertical, int horizontal) {
		return setMargin(vertical, horizontal, vertical, horizontal);
	}

	public ScreenMessage4 setMargin(int margin) {
		return setMargin(margin, margin);
	}

	public ScreenMessage4 setFontSize(int size) {
		component.setFont(component.getFont().deriveFont((float) size));
		return this;
	}

	public ScreenMessage4 setAlignment(Alignment alignment) {
		this.alignment = alignment;
		return this;
	}

	public ScreenMessage4 setAlignAt(AlignAt alignAt) {
		this.alignAt = alignAt;
		return this;
	}

	public Alignment autoCorrectAlignment(Alignment preferred, Point location, Rectangle bounds) {
		int fx0 = location.x;
		int fy0 = location.y;
		int fx1 = fx0 + frame.getWidth();
		int fy1 = fy0 + frame.getHeight();

		int bx0 = bounds.x;
		int by0 = bounds.y;
		int bx1 = bx0 + bounds.width;
		int by1 = by0 + bounds.height;

		if(preferred == Alignment.TOP_LEFT) {
			if(fx1 <= bx1 && fy1 <= by1) return Alignment.TOP_LEFT;
			if(fx1 >  bx1 && fy1 <= by1) return Alignment.TOP_RIGHT;
			if(fx1 <= bx1 && fy1 >  by1) return Alignment.BOTTOM_LEFT;
			if(fx1 >  bx1 && fy1 >  by1) return Alignment.BOTTOM_RIGHT;
		}
		else if(preferred == Alignment.TOP_CENTER) {
			if(fy1 <= by1) return Alignment.TOP_CENTER;
			if(fy1 >  by1) return Alignment.BOTTOM_CENTER;
		}
		else if(preferred == Alignment.TOP_RIGHT) {
			if(fx0 >= bx0 && fy1 <= by1) return Alignment.TOP_RIGHT;
			if(fx0 <  bx0 && fy1 <= by1) return Alignment.TOP_LEFT;
			if(fx0 >= bx0 && fy1 >  by1) return Alignment.BOTTOM_RIGHT;
			if(fx0 <  bx0 && fy1 >  by1) return Alignment.BOTTOM_LEFT;
		}
		else if(preferred == Alignment.CENTER_LEFT) {
			if(fx1 <= bx1) return Alignment.CENTER_LEFT;
			if(fx1 >  bx1) return Alignment.CENTER_RIGHT;
		}
		else if(preferred == Alignment.CENTER) {
			return Alignment.CENTER;
		}
		else if(preferred == Alignment.CENTER_RIGHT) {
			if(fx0 >= bx0) return Alignment.CENTER_RIGHT;
			if(fx0 <  bx0) return Alignment.CENTER_LEFT;
		}
		else if(preferred == Alignment.BOTTOM_LEFT) {
			if(fx1 <= bx1 && fy0 >= by0) return Alignment.BOTTOM_LEFT;
			if(fx1 >  bx1 && fy0 >= by0) return Alignment.BOTTOM_RIGHT;
			if(fx1 <= bx1 && fy0 <  by0) return Alignment.TOP_LEFT;
			if(fx1 >  bx1 && fy0 <  by0) return Alignment.TOP_RIGHT;
		}
		else if(preferred == Alignment.BOTTOM_CENTER) {
			if(fy0 >= by0) return Alignment.BOTTOM_CENTER;
			if(fy0 <  by0) return Alignment.TOP_CENTER;
		}
		else if(preferred == Alignment.BOTTOM_RIGHT) {
			if(fx0 >= bx0 && fy0 >= by0) return Alignment.BOTTOM_RIGHT;
			if(fx0 <  bx0 && fy0 >= by0) return Alignment.BOTTOM_LEFT;
			if(fx0 >= bx0 && fy0 <  by0) return Alignment.TOP_RIGHT;
			if(fx0 <  bx0 && fy0 <  by0) return Alignment.TOP_LEFT;
		}
		return preferred;
	}

	public ScreenMessage4 setManualLocation(Point p) {
		manualLocation.setLocation(p);
		return this;
	}

	public ScreenMessage4 setSize(Size size) {
		this.size = size;
		return this;
	}

	public ScreenMessage4 show() {
		if(!enabled)
			return this;
		// hide();
		frame.setAlwaysOnTop(true); // Keep the window on top of other windows
		frame.setFocusable(false);
		size.apply(frame);
		alignment.apply(gridbag, component);

		updatePosition();

		frame.setFocusableWindowState(false);
		frame.setVisible(true);
		WindowUtils.makeWindowClickThrough(frame);
		frame.setAlwaysOnTop(true);
		return this;
	}

	public void updatePosition() {
		Point location = new Point(0, 0);
		if(size != Size.MAXIMIZED) {
			if(alignAt == AlignAt.SCREEN) {
				Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
				Rectangle ref = new Rectangle(0, 0, d.width, d.height);
				location = alignment.calculateLocation(frame, margin, ref);
				correctedAlignment = alignment;
			}

			else {
				Point ref = null;
				switch(alignAt) {
					case CURSOR: ref = MouseInfo.getPointerInfo().getLocation(); break;
					case CUSTOM: ref = manualLocation; break;
				}
				System.out.println("cursor at " + ref);
				nlScript.screenplay.Screen s = Screen.getScreenForPoint(ref);
				System.out.println("screen = " + s);
				location = alignment.calculateLocation(frame, margin, ref);
				System.out.println("calculated position = " + location);
				correctedAlignment = autoCorrectAlignment(alignment, location, s.scaledBounds);
				location = correctedAlignment.calculateLocation(frame, margin, ref);
			}
		}
		frame.setLocation(location);
	}

	public void configure() {
		show();

		boolean originalEnabled = enabled;
		Size originalSize = size;
		AlignAt originalAlignAt = alignAt;
		Alignment originalAlignment = alignment;
		Color originalForeground = getForeground();
		Color originalBackground = getBackground();
		Insets originalPadding = (Insets) gridbag.getConstraints(component).insets.clone();
		Insets originalMargin = (Insets) margin.clone();
		int originalFontsize = component.getFont().getSize();

		JCheckBox enabledCheckBox              = new JCheckBox();
		JComboBox<Size> sizeComboBox           = new JComboBox<>(Size.values());
		JComboBox<AlignAt> alignAtComboBox     = new JComboBox<>(AlignAt.values());
		JComboBox<Alignment> alignmentComboBox = new JComboBox<>(Alignment.values());
		JButton foregroundButton               = new JButton("Foreground");
		JButton backgroundButton               = new JButton("Background");
		JTextField paddingTextField            = new JTextField("0 0 0 0");
		JTextField marginTextField             = new JTextField("0 0 0 0");
		JSpinner fontsizeSpinner               = new JSpinner();

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(9, 2, 5, 5));

		panel.add(new JLabel("Enabled"));    panel.add(enabledCheckBox);
		panel.add(new JLabel("Size"));       panel.add(sizeComboBox);
		panel.add(new JLabel("Align at"));   panel.add(alignAtComboBox);
		panel.add(new JLabel("Anchor"));     panel.add(alignmentComboBox);
		panel.add(new JLabel("Foreground")); panel.add(foregroundButton);
		panel.add(new JLabel("Background")); panel.add(backgroundButton);
		panel.add(new JLabel("Padding"));    panel.add(paddingTextField);
		panel.add(new JLabel("Margin"));     panel.add(marginTextField);
		panel.add(new JLabel("Font size"));  panel.add(fontsizeSpinner);

		enabledCheckBox.setSelected(enabled);

		sizeComboBox.setSelectedItem(size);
		sizeComboBox.addItemListener(e -> {
			setSize((Size) sizeComboBox.getSelectedItem());
			show();
		});

		alignAtComboBox.setSelectedItem(alignAt);
		alignAtComboBox.addItemListener(e -> {
			setAlignAt((AlignAt) alignAtComboBox.getSelectedItem());
			show();
		});

		alignmentComboBox.setSelectedItem(alignment);
		alignmentComboBox.addItemListener(e -> {
			setAlignment((Alignment) alignmentComboBox.getSelectedItem());
			show();
		});

		foregroundButton.setBackground(new Color(getForeground().getRGB() | 0xff000000));
		foregroundButton.addActionListener(e -> {
			Color col = JColorChooser.showDialog(null, "Choose a color", ScreenMessage4.this.getForeground());
			if(col != null) {
				ScreenMessage4.this.setForeground(col);
				foregroundButton.setBackground(new Color(col.getRGB() | 0xff000000));
			}
		});

		backgroundButton.setBackground(new Color(getBackground().getRGB() | 0xff000000));
		backgroundButton.addActionListener(e -> {
			Color col = JColorChooser.showDialog(null, "Choose a color", ScreenMessage4.this.getBackground());
			if(col != null) {
				ScreenMessage4.this.setBackground(col);
				backgroundButton.setBackground(new Color(col.getRGB() | 0xff000000));
			}
		});

		Insets insets = gridbag.getConstraints(component).insets;
		paddingTextField.setText(insets.top + " " + insets.left + " " + insets.bottom + " " + insets.right);
		paddingTextField.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) { changed(); }
			@Override public void removeUpdate(DocumentEvent e) { changed(); }
			@Override public void changedUpdate(DocumentEvent e) { changed(); }
			public void changed() {
				String text = paddingTextField.getText();
				String[] toks = text.split(" ");
				try {
					switch (toks.length) {
						case 1: setPadding(Integer.parseInt(toks[0])); break;
						case 2: setPadding(Integer.parseInt(toks[0]), Integer.parseInt(toks[1])); break;
						case 4: setPadding(Integer.parseInt(toks[0]), Integer.parseInt(toks[1]), Integer.parseInt(toks[2]), Integer.parseInt(toks[3])); break;
					}
					show();
				} catch (NumberFormatException ignored) {}
			}
		});

		marginTextField.setText(margin.top + " " + margin.left + " " + margin.bottom + " " + margin.right);
		marginTextField.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) { changed(); }
			@Override public void removeUpdate(DocumentEvent e) { changed(); }
			@Override public void changedUpdate(DocumentEvent e) { changed(); }
			public void changed() {
				String text = marginTextField.getText();
				String[] toks = text.split(" ");
				try {
					switch (toks.length) {
						case 1: setMargin(Integer.parseInt(toks[0])); break;
						case 2: setMargin(Integer.parseInt(toks[0]), Integer.parseInt(toks[1])); break;
						case 4: setMargin(Integer.parseInt(toks[0]), Integer.parseInt(toks[1]), Integer.parseInt(toks[2]), Integer.parseInt(toks[3])); break;
					}
					show();
				} catch (NumberFormatException ignored) {}
			}
		});


		fontsizeSpinner.setValue(component.getFont().getSize());
		fontsizeSpinner.addChangeListener(e -> {
			setFontSize((Integer)fontsizeSpinner.getValue());
			show();
		});


		int option = JOptionPane.showConfirmDialog(null, panel, "Configure message", JOptionPane.OK_CANCEL_OPTION);
		if (option != JOptionPane.OK_OPTION) {
			enabled = originalEnabled;
			setSize(originalSize);
			setAlignAt(originalAlignAt);
			setAlignment(originalAlignment);
			setForeground(originalForeground);
			setBackground(originalBackground);
			setPadding(originalPadding.top, originalPadding.left, originalPadding.bottom, originalPadding.right);
			setMargin(originalMargin.top, originalMargin.left, originalMargin.bottom, originalMargin.right);
			setFontSize(originalFontsize);
		} else {
			enabled = enabledCheckBox.isEnabled();
		}
		hide();
	}

	public static void main(String[] args) throws InterruptedException {
		nlScript.screenplay.LabelScreenMessage lsm = new LabelScreenMessage();
		lsm.showMessage("bla");
		lsm.configure();
	}
}

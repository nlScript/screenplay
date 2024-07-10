package nlScript.screenplay;

public class ScrollCounter implements nlScript.screenplay.MouseHook.GlobalMouseListener {

	private final nlScript.screenplay.MouseHook hook;

	private int nTicks = 0;

	public ScrollCounter(nlScript.screenplay.MouseHook hook) {
		this.hook = hook;
	}

	public void start() {
		nTicks = 0;
		hook.addGlobalMouseListener(this);
	}

	public int stop() {
		hook.removeGlobalMouseListener(this);
		return nTicks;
	}

	@Override
	public void mousePressed(nlScript.screenplay.MouseHook.GlobalMouseEvent e) {
	}

	@Override
	public void mouseReleased(nlScript.screenplay.MouseHook.GlobalMouseEvent e) {
	}

	@Override
	public void mouseMoved(nlScript.screenplay.MouseHook.GlobalMouseEvent e) {
	}

	@Override
	public void mouseWheel(nlScript.screenplay.MouseHook.GlobalMouseEvent e) {
		System.out.println("mouseWheel");
		nTicks += e.scrollAmount;
	}

	public static void main(String[] args) throws InterruptedException {
		nlScript.screenplay.MouseHook mouseHook = new MouseHook();
		mouseHook.setMouseHook();
		ScrollCounter counter = new ScrollCounter(mouseHook);
		counter.start();
		Thread.sleep(5000);
		System.out.println("scrolled " + counter.stop() + " times");
	}
}

package gui;

import java.io.File;

import javax.swing.JFrame;

import emulator.Chip8;
import emulator.exception.EmulatorException;
import fileio.Loader;


public class Controller {

	//this needs to be 2^n, where n is a member of {0,1,2,3}
	public final static int DEFAULT_SCALE = 8;

	private Chip8 emulator;
	private DisplayPanel panel;
	private EmulatorInfoPanel infoPanel;
	private JFrame frame;
	private Thread currentThread;
	private boolean hasQuit;
	private byte[] program;

	public Controller(JFrame frame, Chip8 emulator, DisplayPanel panel, EmulatorInfoPanel infoPanel) {
		this.frame = frame;
		this.emulator = emulator;
		this.panel = panel;
		this.infoPanel = infoPanel;
	}

	/**
	 * Resizes and immediately repacks the graphical display for the game.
	 *
	 * @param scale the scaler 
	 */
	public void resizeDisplay(int scale) {
		panel.resizeDisplay(scale);
		frame.pack();
	}

	/**
	 * Restarts the emulator with the current game.
	 * If no game has been initially loaded, this method does nothing.
	 */
	public void reset() {
		//if we haven't loaded a game, reset does nothing
		if (currentThread == null) return;

		hasQuit = true;

		while (currentThread.isAlive())
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				//ignore
			}
		
		startNewGame(program);
	}

	public void startNewGame(File file) {
		if (currentThread != null && currentThread.isAlive()) {
			hasQuit = true;

			while (currentThread.isAlive())
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					//ignore
				}
		}
		startNewGame(Loader.load(file));
	}

	private void startNewGame(byte[] program) {
		this.program = program;
		
		hasQuit = false;
		panel.clear();
		emulator.loadProgram(program);
		
		currentThread = new Thread() {
			@Override
			public void run() {
				while (!hasQuit) {
					try {
						emulator.tick();
					} catch (EmulatorException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					infoPanel.update();
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						//ignore
					}
				}
			}
		};
		
		currentThread.start();
	}
}
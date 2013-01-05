package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

/**
 * Menubar for the emulator program. 
 * Has various ways to control the game, including load, restart, change screen size, etc.
 * @author Troy Shaw
 */
public class MenuBar extends JMenuBar implements ActionListener {

	private JMenu fileMenu, optionsMenu, helpMenu;
	
	private JMenu size;
	
	private List<JRadioButtonMenuItem> scaleButtons;
	
	private JMenuItem reset, load, exit;
	private JMenuItem mute, volume, controls;
	private JMenuItem help, about;
	
	private Controller controller;
	
	/**
	 * Instantiates a new <code>MenuBar</code> with the given controller.<br>
	 * The controller will have methods called on it asynchronously as the buttons are clicked. 
	 * 
	 * @param controller the controller that will be queried with the menubar actions
	 */
	public MenuBar(Controller controller) {
		if (controller == null) throw new NullPointerException("controller cannot be null");
		this.controller = controller;
		
		fileMenu = new JMenu("File");
		optionsMenu = new JMenu("Options");
		helpMenu = new JMenu("Help");
		
		reset = new JMenuItem("Reset");
		load = new JMenuItem("Load...");
		exit = new JMenuItem("Exit");
		
		size = new JMenu("Screen size");
		mute = new JMenuItem("Mute");
		volume = new JMenuItem("Volume");
		controls = new JMenuItem("Controls");
		
		help = new JMenuItem("Help");
		about = new JMenuItem("About");
		
		scaleButtons = new ArrayList<JRadioButtonMenuItem>();
		for (int i = 0; i < 4; i++) {
			int val = (int) Math.pow(2, i);
			//if the value is equal to the default scale, we 'tick' this radio button
			scaleButtons.add(new JRadioButtonMenuItem(val + "x", val == Controller.DEFAULT_SCALE));
		}
		
		ButtonGroup b = new ButtonGroup();
		for (JRadioButtonMenuItem button : scaleButtons) b.add(button);
		
		reset.addActionListener(this);
		load.addActionListener(this);
		exit.addActionListener(this);
		
		for (JRadioButtonMenuItem button : scaleButtons) button.addActionListener(this);
		
		mute.addActionListener(this);
		volume.addActionListener(this);
		controls.addActionListener(this);
		
		help.addActionListener(this);
		about.addActionListener(this);
		
		fileMenu.add(load);
		fileMenu.addSeparator();
		fileMenu.add(reset);
		fileMenu.addSeparator();
		fileMenu.add(exit);
		
		optionsMenu.add(size);
		optionsMenu.addSeparator();
		optionsMenu.add(mute);
		optionsMenu.add(volume);
		optionsMenu.addSeparator();
		optionsMenu.add(controls);
		
		for (JRadioButtonMenuItem button : scaleButtons) size.add(button);
		
		helpMenu.add(help);
		helpMenu.addSeparator();
		helpMenu.add(about);
		
		add(fileMenu);
		add(optionsMenu);
		add(helpMenu);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		
		if (o == reset) {
			controller.reset();
		} else if (o == load) {
			JFileChooser chooser = new JFileChooser("C:\\Users\\Troy Shaw\\Downloads\\chp8_220\\CHIP8\\GAMES");
			int result = chooser.showOpenDialog(null);
			
			if(result == JFileChooser.APPROVE_OPTION) {
				controller.startNewGame(chooser.getSelectedFile());
			}
		} else if (o == exit) {
			System.exit(0);
		} else if (o == help) {
			// TODO add
		} else if (o == about) {
			// TODO add
		} else if (o instanceof JRadioButtonMenuItem) {
			controller.resizeDisplay((int) Math.pow(2, scaleButtons.indexOf(o)));
		}
	}
}
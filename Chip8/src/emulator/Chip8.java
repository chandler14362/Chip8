package emulator;

import java.awt.Toolkit;
import java.util.Timer;
import java.util.TimerTask;

import emulator.exception.EmulatorException;
import emulator.exception.RegisterOutOfBoundsException;
import emulator.exception.UnknownOpcodeException;

/**
 * The Chip8 emulator.
 * <p>
 * It contains:
 *  <ul>
 * <li>PC - program counter</li>
 * <li>I - carry flag</li>
 * <li>4096 byte main memory</li>
 * <li>16 8-bit registers</li>
 * <li>16 level stack for subroutines</li>
 * <li>SP - stack pointer</li>
 * <li>64 * 32 boolean array of pixel data</li>
 * <li>draw flag indicating if we need to redraw this tick </li>
 * <li>16 length boolean array for held keys</li>
 * <li>delay timer, counting down to 0 at 60hz</li>
 * <li>sound timer, counting down to 0 at 60hz, making a beep each time it reaches 0</li>
 * </UL>
 *
 * @author Troy Shaw
 */
public class Chip8 implements KeyController {

	/** Width of default emulator in pixels */
	public static final int WIDTH = 64;

	/** Height of default emulator in pixels */
	public static final int HEIGHT = 32;

	private static final int MAX_REGISTERS = 16;
	private static final int MAX_MEMORY = 4096;
	private static final int MAX_STACK = 16;

	private boolean[][] pixels;
	private boolean drawFlag;

	private int[] memory;
	private int[] register;
	private int[] stack;
	private int PC, SP, I;
	private boolean[] keys;
	private volatile int delayTimer, soundTimer;

	/**
	 * Constructs a new Chip8 emulator. <br>
	 * A game can then be loaded with a call to <code>loadProgram()</code> to begin a game.
	 */
	public Chip8() {
		keys 		= new boolean[16];
		pixels 		= new boolean[WIDTH][HEIGHT];
		drawFlag 	= false;
		stack 		= new int[MAX_STACK];
		register 	= new int[MAX_REGISTERS];
		memory 		= new int[MAX_MEMORY];
		
		//make our timer which will constantly count down at 60hz, decrementing the two counters
		Timer timer = new Timer(true);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				//if sound is 1, it will next be decremented to 0, so we'll make a beep sound
				if (soundTimer == 1) Toolkit.getDefaultToolkit().beep();
				delayTimer = Math.max(delayTimer - 1, 0);
				soundTimer = Math.max(soundTimer - 1, 0);

				//this is an optimisation if needed
				//max(num, 0) = (num - 1) & ~((num - 1) >> 28);
				//delayTimer = (delayTimer - 1) & ~((delayTimer - 1) >> 28);
				//soundTimer = (soundTimer - 1) & ~((soundTimer - 1) >> 28);
			}
		};

		//clock counts down at 60 hz
		timer.schedule(task, 0, Math.round(1000.0 / 60.0));
	}

	/**
	 * Loads the program into memory and reinitialises variables. <br>
	 * The program cannot be null, and cannot have length greater than 3584 bytes (4096 - 512).
	 * @param program the program
	 */
	public void loadProgram(byte[] program) {
		if (program == null) throw new NullPointerException("program cannot be null");
		if (program.length > 3584) throw new IllegalArgumentException("program cannot be longer than 3584 bytes");

		//reinitialise our arrays to 0/false
		for (int i = 0; i < pixels.length; i++)
			for (int j = 0; j < pixels[i].length; j++) 
													pixels[i][j] = false;
		for (int i = 0; i < keys.length; i++) 		keys[i] = false;
		for (int i = 0; i < stack.length; i++) 		stack[i] = 0;
		for (int i = 0; i < register.length; i++) 	register[i] = 0;
		for (int i = 0; i < memory.length; i++) 	memory [i] = 0;
		
		//reinitialise our single variables
		drawFlag 	= false;
		PC 			= 512;
		SP 			= 0;
		I 			= 0;
		delayTimer 	= 0;
		soundTimer 	= 0;
		
		//load in our font-set (in case the last program overwrote it)
		for (int i = 0; i < font.length; i++) 		memory[i] 		= font[i] & 0xFF;
		//load in the main program
		for (int i = 0; i < program.length; i++) 	memory[i + 512] = program[i] & 0xFF;
	}

	/**
	 * Does a single 'tick' of the emulator. <br>
	 * An exception may be thrown for many reasons, including stack over/under flow, memory outofbounds, etc.
	 * @throws EmulatorException
	 */
	public void tick() throws EmulatorException {
		int hi = memory[PC] & 0xFF;
		int low = memory[PC + 1] & 0xFF;
		int opcode = (hi << 8) | low;
		PC += 2;

		System.out.println("Executing opcode: " + Integer.toString(opcode, 16).toUpperCase() + " at PC = " + (PC - 2));

		//reset the drawflag
		drawFlag = false;

		//we will extract the common elements of the opcode
		//0xy0 for x/ y
		//00nn for nn
		//0nnn for nnn
		int x = hi & 0xF;
		int y = (low & 0xF0) >> 4;
		int n = low & 0xF;
		int nn = low;
		int nnn = (x << 8) | low;

		switch (hi >> 4) {
		case 0x0:
			switch (low) {
			case 0xE0: clearScreen();		break;
			case 0xEE: PC = stack[--SP];	break;
			default: invalidOpcode(opcode);
			}
			break;
		case 0x1: PC = nnn;		 								break;
		case 0x2: stack[SP++] = PC; PC = nnn;					break;
		case 0x3: if (register[x] == nn) PC += 2;				break;
		case 0x4: if (register[x] != nn) PC += 2;				break;
		case 0x5: if (register[x] == register[y]) PC += 2;		break;
		case 0x6: register[x] = nn;								break;
		case 0x7: register[x] = (register[x] + nn) & 0xFF;		break;
		case 0x8: 
			switch(low & 0xF) {
			case 0x0: register[x]  = register[y];				break;
			case 0x1: register[x] |= register[y];				break;
			case 0x2: register[x] &= register[y];				break;
			case 0x3: register[x] = (register[x] ^ register[y]) & 0xFF;		break;
			case 0x4: 
				register[x] += register[y];
				register[15] = register[x] > 0xFF ? 1 : 0;
				register[x] &= 0xFF;
				break;
			case 0x5: 
				register[15] = register[y] > register[x] ? 0 : 1;
				register[x] -= register[y];
				register[x] &= 0xFF;
				break;
			case 0x6: 
				register[15] = register[x] & 0x1;
				register[x] = (register[x] >> 1) & 0xFF;
				break;
			case 0x7: 
				register[15] = register[x] > register[y] ? 0 : 1;
				register[x] = (register[y] - register[x]) & 0xFF;
				break;
			case 0xE: 
				register[15] = (register[x] >> 7);
				register[x] = (register[x] << 1) & 0xFF;
				break;
			default: invalidOpcode(opcode);
			}
			break;
		case 0x9: if (register[x] != register[y]) PC += 2;				break;
		case 0xA: I = nnn;												break;
		case 0xB: PC = (nnn + register[0]) & 0xFFFF;					break;
		case 0xC: register[x] = ((int) (Math.random() * 0xFF)) & nn; 	break;
		case 0xD: draw(register[x], register[y], n);					break;
		case 0xE: 
			switch (low) {
			case 0x9E: if (keys[register[x]]) PC += 2; 	break;
			case 0xA1: if (!keys[register[x]]) PC += 2; 	break;
			default: invalidOpcode(opcode);
			}
			break;
		case 0xF: 
			switch (low) {
			case 0x07: register[x] = delayTimer;		break;
			case 0x0A: awaitKeyPress(x);				break;
			case 0x15: delayTimer = register[x];		break;
			case 0x18: soundTimer = register[x];		break;
			case 0x1E:
				register[15] = I + register[x] > 0xFFF ? 1 : 0;
				I = (I + register[x]) & 0xFFF;	
				break;
			case 0x29: I = register[x] * 5;				break;
			case 0x33: 
				memory[I] = register[x] / 100;
				memory[I + 1] = (register[x] / 10) % 10;
				memory[I + 2] = (register[x] % 100) % 10;
				break;
			case 0x55: for (int i = 0; i <= x; i++) memory[I + i] = register[i]; I += x + 1;		break;
			case 0x65: for (int i = 0; i <= x; i++) register[i] = memory[I + i]; I += x + 1;		break;
			default: invalidOpcode(opcode);
			}
			break;
		default: invalidOpcode(opcode);
		}
	}

	/**
	 * Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels. <p>
	 * Each row of 8 pixels is read as bit-coded (with the most significant bit of each byte displayed on the left) 
	 * starting from memory location I; I value doesn't change after the execution of this instruction.<p> 
	 * VF is set to 1 if any screen pixels are flipped from set to unset when the sprite is drawn, 
	 * and to 0 if that doesn't happen.
	 * 
	 * @param x start x coordinate
	 * @param y start y coordinate
	 * @param height the height of drawing
	 */
	private void draw(int x, int y, int height) {
		//first set flag to off. It will later be set if a pixel is flipped from on to off
		register[15] = 0;
		
		for (int j = 0; j < height; j++) {
			int dat = memory[j + I];

			for (int i = 0; i < 8; i++) {
				//if the bit is 0, we aren't changing this value
				if ((dat & (0x80 >> i)) == 0) continue;
				
				// NOTE: not sure if meant to skip out-of-bounds pixels or modulo them
				// for the time being, I am ignoring them, as all games seem to function using this mechanism
				
				int rx = i + x;
				int ry = j + y;
				
				// ignore them. 
				if (rx >= WIDTH || ry >= HEIGHT) continue;
				
				// modulo version. Causes weird visual disturbances on the Blitz game
				//rx %= 64;
				//ry %= 32; 
				
				//if the pixel was on, it means we are now unsetting it, and we must set the carry flag
				if (pixels[rx][ry]) register[15] = 1;
				//flip the pixel
				pixels[rx][ry] ^= true;
			}
		}
		
		//set draw flag to show we need to redraw
		drawFlag = true;
	}

	/**
	 * Clears the screen. Sets all elements in the boolean pixel array to false.
	 */
	private void clearScreen() {
		//iterate over pixel array setting to false
		for (int i = 0; i < pixels.length; i++) {
			for (int j = 0; j < pixels[i].length; j++) {
				pixels[i][j] = false;
			}
		}
			
		//set draw flag to show we need to redraw
		drawFlag = true;
	}

	/**
	 * Awaits a key press. If any key is pressed, we have already 'awaited' and we can continue. <br>
	 * If no key is pressed, we decrease the program counter by 2 to retry the command, thus 'waiting'.
	 * 
	 * @param destReg destination register 
	 * @throws RegisterOutOfBoundsException if destReg is not between 0 and 15 inclusive
	 */
	private void awaitKeyPress(int destReg) throws RegisterOutOfBoundsException {
		if (destReg < 0 || destReg > 15) throw new RegisterOutOfBoundsException("Cannot await on register: " + destReg);
		
		for (int i = 0; i < keys.length; i++) {
			//if a key is pressed, the await succeeded, and we set it and return
			if (keys[i]) {
				register[destReg] = 1 << i;
				return;
			}
		}

		//if we had no key pressed, we decrement our pc which causes the instruction to repeat again
		PC -= 2;
	}

	/**
	 * Throws an <code>UnknownOpcodeException</code> with the given opcode.
	 * @param opcode the unknown opcode
	 * @throws UnknownOpcodeException the exception to be thrown
	 */
	private void invalidOpcode(int opcode) throws UnknownOpcodeException {
		throw new UnknownOpcodeException("Invalid opcode: " + Integer.toString(opcode, 16).toUpperCase() + " at PC = " + (PC - 2));
	}

	@Override
	public void keyInteracted(int i, boolean pressed) {
		keys[i] = pressed;
	}

	/**
	 * Gets the register file containing v0-v15. v15 is the carry flag.
	 * @return the register
	 */
	public int[] getRegister() {
		return register;
	}

	/**
	 * Gets the program counter.
	 * @return returns the PC
	 */
	public int getPC() {
		return PC;
	}

	/**
	 * Gets the stack pointer (will be between 0 and 15).
	 * @return the stack pointer
	 */
	public int getSP() {
		return SP;
	}

	/**
	 * Returns the stack, a 16 level address array.
	 * @return the stack
	 */
	public int[] getStack() {
		return stack;
	}

	/**
	 * Returns the chip8 memory. It is a 4096 byte array (stored as ints).
	 * @return the memory
	 */
	public int[] getMemory() {
		return memory;
	}

	/**
	 * Returns the I register.
	 * @return the I register
	 */
	public int getIRegister() {
		return I;
	}

	/**
	 * Returns the delay timer.
	 * @return the delay timer
	 */
	public int getDelayTimer() {
		return delayTimer;
	}

	/**
	 * Returns the sound timer.
	 * @return the sound timer
	 */
	public int getSoundTimer() {
		return soundTimer;
	}

	/**
	 * Returns the draw flag, which indicates if the emulator needs to be redrawn this cycle. <br>
	 * <b>Note:</b> this method call does not reset the draw flag.
	 * @return the draw flag
	 */
	public boolean getDrawFlag() {
		return drawFlag;
	}

	/**
	 * Returns the pixel data for the emulator.<br>
	 * It is a 64 * 32 boolean array. <br>
	 * A true indicates that pixel is set. False indicates it is not.
	 * @return the pixel data
	 */
	public boolean[][] getPixelData() {
		return pixels;
	}

	/**
	 * Returns the currently held keys. <br>
	 * The returned array is of length 16 (to represent the hex keypad).
	 * @return the keys
	 */
	public boolean[] getKeys() {
		return keys;
	}

	/** 
	 * This is the font-set for the emulator of characters 0-9 A-F (hex charas).
	 * Each 5 shorts is a different character, designated by the adjacent comment.
	 */
	private short[] font = { 
			0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
			0x20, 0x60, 0x20, 0x20, 0x70, // 1
			0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
			0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
			0x90, 0x90, 0xF0, 0x10, 0x10, // 4
			0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
			0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
			0xF0, 0x10, 0x20, 0x40, 0x40, // 7
			0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
			0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
			0xF0, 0x90, 0xF0, 0x90, 0x90, // A
			0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
			0xF0, 0x80, 0x80, 0x80, 0xF0, // C
			0xE0, 0x90, 0x90, 0x90, 0xE0, // D
			0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
			0xF0, 0x80, 0xF0, 0x80, 0x80  // F
	};
}
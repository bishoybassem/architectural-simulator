package engine.storage;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import engine.Helpers;
import engine.types.Addressable;
import engine.types.Instruction;

public class Memory implements Addressable {
	
	private final int size;
	private final int instructionsStartAddress;
	private final int dataStartAddress;
	private int instructionAccesses;
	private int dataAccesses;
	private int accessTime;
	private TreeMap<Integer, Byte> memory;
	private ArrayList<Instruction> instructions;
	
	public Memory(int size, int instructionsStartAddress, int dataStartAddress, int accessTime) {
		if (size < 128 || size > 4194304)
			throw new IllegalArgumentException("Memory size must be greater than 128B and less than 4MB");
		
		if (!Helpers.isPowerOf2(size))
			throw new IllegalArgumentException("Memory size (" + size + ") must be a power of 2");
		
		if (instructionsStartAddress < dataStartAddress && dataStartAddress < size 
				&& instructionsStartAddress % 2 == 0 && dataStartAddress % 2 == 0) {
			this.instructionsStartAddress = instructionsStartAddress;
			this.dataStartAddress = dataStartAddress;
			this.size = size;
			this.accessTime = accessTime;
		} else 
			throw new IllegalArgumentException("Invalid memory configuration");
		
		clear();
	}

	public void addInstruction(String operation, Object[] operands) {
		int address = instructionsStartAddress + instructions.size() * 2;
		if (address >= dataStartAddress)
			throw new IllegalArgumentException("No enough space for the instructions");
		
		instructions.add(new Instruction(address, operation, operands));
	}
	
	public Instruction[] getInstructions(int address, int number) {
		if (address % 2 != 0 || address < 0 || address + number * 2 >= size) 
			throw new IllegalArgumentException("Invalid instruction address (" + address + ")");
		
		instructionAccesses++;
		Instruction[] ins = new Instruction[number];
		int index;
		for (int i = 0; i < number; i++) {
			index = (address - instructionsStartAddress) / 2 + i;
			ins[i] = (index < 0 || index >= instructions.size())? null : instructions.get(index);
		}
		return ins;
	}
	
	public void setByte(int address, byte data) {
		if (address < 0 || address >= size)
			throw new IllegalArgumentException("Invalid address (" + address + ")");
			
		memory.put(address, data);
	}
	
	public byte getByte(int address) {
		if (address < 0 || address >= size)
			throw new IllegalArgumentException("Invalid address (" + address + ")");
		
		Byte data = memory.get(address);
		return (data == null)? 0 : data;
	}
	
	public void setWord(int address, short data) {
		if (address % 2 != 0) 
			throw new IllegalArgumentException("Invalid word address (" + address + ")");
		
		byte[] bytes = Helpers.toBytes(data);
		setByte(address, bytes[0]);
		setByte(address + 1, bytes[1]);
	}
	
	public short getWord(int address) {
		if (address % 2 != 0) 
			throw new IllegalArgumentException("Invalid word address (" + address + ")");
		
		return Helpers.toWord(new byte[]{getByte(address), getByte(address + 1)});
	}
	
	public byte[] getData(int address, int bytes) {
		dataAccesses++;
		byte[] data = new byte[bytes];
		for (int i = 0; i < data.length; i++) {
			data[i] = getByte(address + i);
		}
		return data;
	}
	
	public void setData(int address, byte[] data) {
		dataAccesses++;
		for (int i = 0; i < data.length; i++)
			setByte(address + i, data[i]);
	}
	
	public Object[] displayDataBytes(boolean hex) {
		int bits = (int)(Math.ceil(Helpers.log(size, (hex)? 16 : 10)));
		String[] headers = {"Address", "Byte"}; 
		
		int length = 0;
		for (Map.Entry<Integer, Byte> entry : memory.entrySet())
			if (entry.getValue() != 0)
				length++;
		
		String[][] data = new String[length][2];
		int i = 0;
		for (Map.Entry<Integer, Byte> entry : memory.entrySet()) {
			if (entry.getValue() == 0)
				continue;
			data[i][0] = String.format((hex)? "0x%0" + bits + "X" : "%d", entry.getKey());
			data[i][1] = String.format((hex)? "0x%02X" : "%d", entry.getValue());
			i++;
		}
		String accesses = String.format("%-20s : %d\n%-20s : %d", "Instruction accesses", instructionAccesses, "Data accesses", dataAccesses);
		return new Object[]{data, headers, accesses};
	}
	
	public Object[] displayDataWords(boolean hex) {
		int bits = (int)(Math.ceil(Helpers.log(size, (hex)? 16 : 10)));
		String[] headers = {"Address", "Word"}; 
		
		int length = 0;
		for (int a : memory.keySet())
			if (a % 2 == 0 && getWord(a) != 0)
				length++;
		
		String[][] data = new String[length][2];
		int i = 0;
		for (int address : memory.keySet()){
			if (address % 2 == 1 || getWord(address) == 0)
				continue;
			data[i][0] = String.format((hex)? "0x%0" + bits + "X" : "%d", address);
			data[i][1] = String.format((hex)? "0x%04X" : "%d", getWord(address));
			i++;
		}
		String accesses = String.format("%-20s : %d\n%-20s : %d", "Instruction accesses", instructionAccesses, "Data accesses", dataAccesses);
		return new Object[]{data, headers, accesses};
	}
	
	public void clear() {
		instructionAccesses = 0;
		dataAccesses = 0;
		memory = new TreeMap<Integer, Byte>();
		instructions = new ArrayList<Instruction>();
	}
	
	public int getDataAccesses() {
		return dataAccesses;
	}
	
	public int getAccessTime() {
		return accessTime;
	}
	
	public ArrayList<Instruction> getInstructions() {
		 return instructions;
	 }
	
	public int getSize() {
		return size;
	}
	
	public int getInstructionsStartAddress() {
		return instructionsStartAddress;
	}
	
	public int getLastInstructionAddress() {
		return instructionsStartAddress + instructions.size() * 2 - 2;
	}
	
	public int getDataStartAddress() {
		return dataStartAddress;
	}
	
	public boolean isInstructionAddress(int address) {
		return address % 2 == 0 && address >= instructionsStartAddress && address <= getLastInstructionAddress() + 2;
	}
	
	public boolean isWordAddress(int address) {
		return address % 2 == 0 && address >= dataStartAddress && address < size;
	}

}

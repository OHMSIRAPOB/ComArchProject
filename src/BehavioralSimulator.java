import java.io.*;
import java.util.*;

public class BehavioralSimulator {
    private static final int NUM_REGISTERS = 8;
    private static final int MEMORY_SIZE = 65536;

    private static int[] registers = new int[NUM_REGISTERS];
    private static int[] memory = new int[MEMORY_SIZE];
    private static int programCounter = 0;

    static class State {
        int pc;
        int[] mem;
        int[] reg;
        int numMemory;

        State(int memorySize, int numRegisters) {
            this.pc = 0;
            this.mem = new int[memorySize];
            this.reg = new int[numRegisters];
            this.numMemory = 0;
        }
    }

    public static void main(String[] args) {
        // Initialize registers and memory
        Arrays.fill(registers, 0);  // Set all registers to 0
        Arrays.fill(memory, 0);     // Set all memory to 0

        // Load machine code from file into memory
        loadMachineCode("src/machine_code.txt");

        // Print example run header
        printRunHeader();

        // Simulate the machine code until halt
        simulate();
    }

    // Load machine code from the file into memory
    private static void loadMachineCode(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int address = 0;

            while ((line = br.readLine()) != null) {
                memory[address++] = Integer.parseInt(line.trim());
            }

        } catch (IOException e) {
            System.err.println("Error loading machine code: " + e.getMessage());
            System.exit(1);
        }
    }

    // Method to print example run header
    private static void printRunHeader() {
        for (int i = 0; i < 10; i++) {
            System.out.printf("memory[%d]=%d\n", i, memory[i]);
        }
        System.out.println(); // Add a newline for better formatting
    }

    // Simulate the execution of machine code instructions
    private static void simulate() {
        boolean halted = false;

        while (!halted) {
            // Print the current state before executing the instruction
            printState();

            // Fetch the instruction at the program counter
            int instruction = memory[programCounter];

            // Decode the opcode (bits 24-22)
            int opcode = (instruction >> 22) & 0x7;

            // Execute the instruction based on the opcode
            switch (opcode) {
                case 0:  // add (R-type)
                    executeAdd(instruction);
                    break;
                case 1:  // nand (R-type)
                    executeNand(instruction);
                    break;
                case 2:  // lw (I-type)
                    executeLw(instruction);
                    break;
                case 3:  // sw (I-type)
                    executeSw(instruction);
                    break;
                case 4:  // beq (I-type)
                    executeBeq(instruction);
                    break;
                case 5:  // jalr (J-type)
                    executeJalr(instruction);
                    break;
                case 6:  // halt (O-type)
                    halted = true;
                    // Ensure the PC is incremented before halting
                    programCounter++;
                    break;
                case 7:  // noop (O-type)
                    // Do nothing
                    break;
                default:
                    System.err.println("Error: Invalid opcode: " + opcode);
                    System.exit(1);
            }

            // Increment the program counter unless halted
            if (!halted) {
                programCounter++;
            }
        }

        // Print the final state after halting
        printState();
        System.out.println("Machine halted.");
    }

    // R-type instruction: add
    private static void executeAdd(int instruction) {
        int regA = (instruction >> 19) & 0x7;
        int regB = (instruction >> 16) & 0x7;
        int destReg = instruction & 0x7;

        registers[destReg] = registers[regA] + registers[regB];
    }

    // R-type instruction: nand
    private static void executeNand(int instruction) {
        int regA = (instruction >> 19) & 0x7;
        int regB = (instruction >> 16) & 0x7;
        int destReg = instruction & 0x7;

        registers[destReg] = ~(registers[regA] & registers[regB]);
    }

    // I-type instruction: lw
    private static void executeLw(int instruction) {
        int regA = (instruction >> 19) & 0x7;
        int regB = (instruction >> 16) & 0x7;
        int offset = convertNum(instruction & 0xFFFF);

        registers[regB] = memory[registers[regA] + offset];
    }

    // I-type instruction: sw
    private static void executeSw(int instruction) {
        int regA = (instruction >> 19) & 0x7;
        int regB = (instruction >> 16) & 0x7;
        int offset = convertNum(instruction & 0xFFFF);

        memory[registers[regA] + offset] = registers[regB];
    }

    // I-type instruction: beq
    private static void executeBeq(int instruction) {
        int regA = (instruction >> 19) & 0x7;
        int regB = (instruction >> 16) & 0x7;
        int offset = convertNum(instruction & 0xFFFF);

        if (registers[regA] == registers[regB]) {
            programCounter += offset;
        }
    }

    // J-type instruction: jalr
    private static void executeJalr(int instruction) {
        int regA = (instruction >> 19) & 0x7;
        int regB = (instruction >> 16) & 0x7;

        registers[regB] = programCounter + 1;
        programCounter = registers[regA] - 1;  // -1 because PC will be incremented after this
    }

    // Convert a 16-bit number to a 32-bit 2's complement integer
    private static int convertNum(int num) {
        if ((num & (1 << 15)) != 0) {
            num -= (1 << 16);
        }
        return num;
    }

    private static void printState() {
        State state = new State(MEMORY_SIZE, NUM_REGISTERS);
        state.pc = programCounter;

        System.arraycopy(memory, 0, state.mem, 0, MEMORY_SIZE);
        System.arraycopy(registers, 0, state.reg, 0, NUM_REGISTERS);

        System.out.println("\n@@@");
        System.out.println("state:");
        System.out.printf("\tpc %d\n", state.pc);
        System.out.println("\tmemory:");

        // Print all memory contents
        for (int i = 0; i < 10; i++) {
            System.out.printf("\t\tmem[ %d ] %d\n", i, state.mem[i]);
        }

        System.out.println("\tregisters:");
        for (int i = 0; i < NUM_REGISTERS; i++) {
            System.out.printf("\t\treg[ %d ] %d\n", i, state.reg[i]);
        }
        System.out.println("end state");
    }
}

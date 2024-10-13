import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class BehavioralSimulator {
    private static final int NUMMEMORY = 65536; // maximum number ของ words ใน memory
    private static final int NUMREGS = 8; // จำนวน machine registers
    private static final int MAXLINELENGTH = 1000; // จำนวน machine registers

    public static class stateStruct {
        int pc; // program counter
        int[] mem = new int[NUMMEMORY];
        int[] reg = new int[NUMREGS];
        int numMemory; // นับจำนวน momory ที่ใช้อยู่
    }

    public static void printState(stateStruct state) {
        System.out.println("\n@@@\nstate:");
        System.out.println("\tpc " + state.pc);
        System.out.println("\tmemory:");
        for (int i = 0; i < state.numMemory; i++) {
            System.out.println("\t\tmem[ " + i + " ] " + state.mem[i]);
        }
        System.out.println("\tregisters:");
        for (int i = 0; i < NUMREGS; i++) {
            System.out.println("\t\treg[ " + i + " ] " + state.reg[i]);
        }
        System.out.println("end state");
    }

    private static void RType(int bit, int[] arg) {
        arg[0] = (bit & (7 << 19)) >> 19; // regA (Bits 19-21)
        arg[1] = (bit & (7 << 16)) >> 16; // regB (Bits 16-18)
        arg[2] = bit & 7; // destReg (Bits 0-2)
    }

    private static void IType(int bit, int[] arg) {
        arg[0] = (bit & (7 << 19)) >> 19; // regA (Bits 19-21)
        arg[1] = (bit & (7 << 16)) >> 16; // regB (Bits 16-18)
        arg[2] = bit & 0xFFFF; // Offset (Bits 0-15)
        arg[2] = convert(arg[2]); // Convert เป็น signed offset
    }

    private static void JType(int bit, int[] arg) {
        arg[0] = (bit & (7 << 19)) >> 19; // regA (Bits 19-21)
        arg[1] = (bit & (7 << 16)) >> 16; // regB (Bits 16-18)
        arg[2] = bit & 0xFFFF; // destReg
    }

    private static void OType(int bit, int[] arg) {
        arg[0] = bit & 0x3FFFFFF; // regA
    }

    public static int convert(int num) {
        if ((num & (1 << 15)) != 0) {
            num -= (1 << 16); // Converts เป็น signed number
        }
        return num;
    }

    public static void main(String[] args) {
        String fileName = "src/machine_code.txt"; // อ่าน machine_code.txt แล้ว store ใน memory array (mem[])
        stateStruct state = new stateStruct();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                state.mem[state.numMemory] = Integer.parseInt(line);
                System.out.println("memory[" + state.numMemory + "]=" + state.mem[state.numMemory]);
                state.numMemory++;
            }
        } catch (IOException e) {
            System.err.println("error: can't open file " + fileName);
            e.printStackTrace();
            System.exit(1);
        }

        state.pc = 0; // เริ่มที่ 0
        int regA, regB;
        int offset = 0;
        int[] arg = new int[3];
        int total = 0;


        for (int i = 1; i != 0; i++) {
            total++;
            printState(state);

            int instruction = state.mem[state.pc];
            int opcode = instruction >> 22; // แต่ละ instruction จะถูกแยกตามการเช็ค topmost bits

            switch (opcode) {
                case 0: // add
                    RType(state.mem[state.pc], arg); // R-Type
                    regA = state.reg[arg[0]];
                    regB = state.reg[arg[1]];
                    state.reg[arg[2]] = regA + regB; // add arg[0] กับ arg[1] เก็บไว้ที่ arg[2]
                    break;

                case 1: // nand
                    RType(state.mem[state.pc], arg); // R-Type
                    regA = state.reg[arg[0]];
                    regB = state.reg[arg[1]];
                    state.reg[arg[2]] = ~(regA & regB); // nand arg[0] กับ arg[1] เก็บไว้ที่ arg[2]
                    break;

                case 2: // lw
                    IType(state.mem[state.pc], arg); // I-Type
                    offset = arg[2] + state.reg[arg[0]];
                    state.reg[arg[1]] = state.mem[offset]; // load ค่าจาก memory ไป arg[1]
                    break;

                case 3: // sw
                    IType(state.mem[state.pc], arg); // I-Type
                    offset = arg[2] + state.reg[arg[0]];
                    state.mem[offset] = state.reg[arg[1]]; // load ค่าจาก register ไป memory
                    break;

                case 4: // beq
                    IType(state.mem[state.pc], arg); // I-Type
                    regA = state.reg[arg[0]];
                    regB = state.reg[arg[1]];
                    if (regA == regB) {
                        state.pc += arg[2];
                    }
                    break;

                case 5: // jalr
                    JType(state.mem[state.pc], arg); // J-Type
                    regA = state.reg[arg[0]];
                    regB = state.reg[arg[1]];
                    state.reg[arg[1]] = state.pc + 1; // stores current PC+1 ลงใน regB
                    state.pc = regA; // กระโดดไปยัง address ที่ถูกเก็บไว้ใน regA
                    state.pc--;
                    break;

                case 6: // bhalt
                    OType(state.mem[state.pc], arg); // O-Type
                    i = -1; // หยุด execution program, หยุด loop
                    break;

                case 7: // noop
                    OType(state.mem[state.pc], arg); // no operation
                    break;

                case 8: // div
                    RType(state.mem[state.pc], arg); // R-Type
                    regA = state.reg[arg[0]];
                    regB = state.reg[arg[1]];
                    if (regB != 0) {
                        state.reg[arg[2]] = regA / regB; // divide arg[0] by arg[1], store in arg[2]
                    } else {
                        System.err.println("Error: Division by zero");
                    }
                    break;

            }
            state.pc++;

            if (total > MAXLINELENGTH) {
                i = -1; // หยุด execution program
                System.out.println("Max instruction limit reached");
            }
        }
        System.out.println("machine halted\n" +
                "total of " + total + " instructions executed\n" +
                "final state of machine:");
        printState(state);
    }

}

import java.io.*;
import java.util.*;

    public class Assembler {
        // เก็บคู่ข้อมูล opcodes ที่เกี่ยวข้อง
        private static final Map<String, String> opcodes = new HashMap<>();
        // เก็บตำแหน่งของ labels ที่ถูกประกาศในโปรแกรม Assembly
        private static final Map<String, Integer> symbolTable = new HashMap<>();
        private static int currentAddress = 0;
    private static final String outputFileName = "src/machine_code.txt";

    static {
        opcodes.put("add", "000");
        opcodes.put("nand", "001");
        opcodes.put("lw", "010");
        opcodes.put("sw", "011");
        opcodes.put("beq", "100");
        opcodes.put("jalr", "101");
        opcodes.put("halt", "110");
        opcodes.put("noop", "111");
    }

    public static void main(String[] args) {
        // อ่านโค้ด Assembly จากไฟล์ และเก็บแต่ละบรรทัดในรูปของ List<String>
        List<String> assemblyCode = readAssemblyFile("src/combination.txt");

        // สร้าง symbol table ที่เก็บตำแหน่งของ labels
        first(assemblyCode);

        // แปลง Assembly เป็น machine code และเขียนลงไฟล์
        second(assemblyCode);

        // ออกจากโปรแกรมเมื่อเสร็จการทำงาน
        System.exit(0);
    }

    // ฟังก์ชันในการอ่านไฟล์
    private static List<String> readAssemblyFile(String filename) {
        List<String> lines = new ArrayList<>(); //สร้างลิสต์ lines ที่จะเก็บบรรทัดของโปรแกรม Assembly ที่ถูกอ่านจากไฟล์
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    // ลบ คอมเมนต์
                    int commentIndex = line.indexOf('#');
                    if (commentIndex != -1) { //ถ้าค่าคอมเมนค์ไม่เท่ากับ -1 แปลว่ามีคอมเมนต์
                        line = line.substring(0, commentIndex).trim();
                    }
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
        return lines;
    }

    // First: เพื่อสร้าง symbol table ที่เก็บตำแหน่ง (address) ของทุกๆ label ที่ถูกใช้ในโปรแกรม และเช็ค labels ที่ซ้ำ
    private static void first(List<String> assemblyCode) {
        currentAddress = 0;
        for (String line : assemblyCode) {
            String[] parts = line.split("\\s+"); //แยกบรรทัดออกเป็นส่วนๆใช้ช่องว่างแบ่ง

            // เช็คว่าเริ่มต้นด้วย label ไหม เช็คว่า เป็นคำสั่งassembly กับ .fillบ่
            if (!opcodes.containsKey(parts[0]) && !parts[0].equals(".fill")) {
                String label = parts[0];
                if (symbolTable.containsKey(label)) {
                    System.err.println("Error: Duplicate label found: " + label);
                    System.exit(1); //ดูว่าซ้ำบ่
                }
                symbolTable.put(label, currentAddress);  // Add label to symbol table
                parts = Arrays.copyOfRange(parts, 1, parts.length); //พบ label และบันทึกลงใน symbol table แล้ว โปรแกรมจะต้องลบ label ออกจากคำสั่ง เพื่อไม่ให้คำสั่งถัดไปได้รับผลกระทบ
            }

            // Skip empty lines after label removal
            if (parts.length == 0) continue;

            currentAddress++;  // Increment address for each instruction or label
        }
    }

    // Second: แปลงคำสั่ง Assembly เป็น Machine Code
    private static void second(List<String> assemblyCode) {
        currentAddress = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            for (String line : assemblyCode) {
                String[] parts = line.split("\\s+");

                // ข้ามถ้ามี label
                if (!opcodes.containsKey(parts[0]) && !parts[0].equals(".fill")) {
                    parts = Arrays.copyOfRange(parts, 1, parts.length);
                }

                // บรรทัดว่าง
                if (parts.length == 0) continue;

                String instruction = parts[0];

                // เช็ค opcode ถูกบ่
                if (!opcodes.containsKey(instruction) && !instruction.equals(".fill")) {
                    System.err.println("Error: Invalid opcode: " + instruction);
                    System.exit(1);
                }

                int machineCode = 0;

                try {
                    if (opcodes.containsKey(instruction)) {
                        int opcode = Integer.parseInt(opcodes.get(instruction), 2);
                        machineCode = opcode << 22;

                        switch (instruction) {
                            case "add":
                            case "nand":
                                int regA = Integer.parseInt(parts[1]);
                                int regB = Integer.parseInt(parts[2]);
                                int destReg = Integer.parseInt(parts[3]);
                                machineCode |= (regA << 19) | (regB << 16) | destReg;
                                break;

                            case "lw":
                            case "sw":
                                regA = Integer.parseInt(parts[1]);
                                regB = Integer.parseInt(parts[2]);
                                int offsetField = 0;

                                if (isNumeric(parts[3])) {
                                    offsetField = Integer.parseInt(parts[3]);
                                } else if (symbolTable.containsKey(parts[3])) {
                                    offsetField = symbolTable.get(parts[3]);
                                } else {
                                    System.err.println("Error: Undefined label: " + parts[3]);
                                    System.exit(1);
                                }

                                if (offsetField < -32768 || offsetField > 32767) {
                                    System.err.println("Error: Offset field out of range (-32768 to 32767): " + offsetField);
                                    System.exit(1);
                                }

                                machineCode |= (regA << 19) | (regB << 16) | (offsetField & 0xFFFF);
                                break;

                            case "beq":
                                regA = Integer.parseInt(parts[1]);
                                regB = Integer.parseInt(parts[2]);
                                int offset = 0;

                                if (isNumeric(parts[3])) {
                                    offset = Integer.parseInt(parts[3]);
                                } else if (symbolTable.containsKey(parts[3])) {
                                    offset = symbolTable.get(parts[3]) - (currentAddress + 1);
                                } else {
                                    System.err.println("Error: Undefined label: " + parts[3]);
                                    System.exit(1);
                                }

                                if (offset < -32768 || offset > 32767) {
                                    System.err.println("Error: Offset out of range (-32768 to 32767): " + offset);
                                    System.exit(1);
                                }

                                machineCode |= (regA << 19) | (regB << 16) | (offset & 0xFFFF);
                                break;

                            case "jalr":
                                regA = Integer.parseInt(parts[1]);
                                regB = Integer.parseInt(parts[2]);
                                machineCode |= (regA << 19) | (regB << 16);
                                break;

                            case "halt":
                            case "noop":
                                break;

                            default:
                                System.err.println("Error: Invalid opcode: " + instruction);
                                System.exit(1);
                        }
                    } else if (instruction.equals(".fill")) {
                        int machineCodeValue = 0;
                        if (isNumeric(parts[1])) {
                            machineCodeValue = Integer.parseInt(parts[1]);
                        } else if (symbolTable.containsKey(parts[1])) {
                            machineCodeValue = symbolTable.get(parts[1]);
                        } else {
                            System.err.println("Error: Undefined label in .fill: " + parts[1]);
                            System.exit(1);
                        }
                        machineCode = machineCodeValue;
                    }

                    writer.write(Integer.toString(machineCode));
                    writer.newLine();
                    currentAddress++;

                } catch (Exception e) {
                    System.err.println("Error at line " + currentAddress + ": " + e.getMessage());
                    System.exit(1);
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+");
    }
}

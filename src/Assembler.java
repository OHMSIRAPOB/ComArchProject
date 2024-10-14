import java.io.*;
import java.util.*;

public class Assembler {
    private static final Map<String, String> opcodes = new HashMap<>();
    private static final Map<String, Integer> symbolTable = new HashMap<>();
    private static int currentAddress = 0;
    private static final String outputFileName = "src/machine_code.txt";

    static {
        // Define the opcodes
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
        // Read the assembly code
        List<String> assemblyCode = readAssemblyFile("src/div.txt");

        // First pass to populate the symbol table
        firstPass(assemblyCode);

        // Second pass to generate machine code and write it to a file
        secondPass(assemblyCode);

        // Exit successfully if no errors
        System.exit(0);
    }

    // Function to read the assembly file
    private static List<String> readAssemblyFile(String filename) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    // Remove comments
                    int commentIndex = line.indexOf('#');
                    if (commentIndex != -1) {
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

    // First pass: Populate the symbol table and check for duplicate labels
    private static void firstPass(List<String> assemblyCode) {
        currentAddress = 0;
        for (String line : assemblyCode) {
            String[] parts = line.split("\\s+");

            // Check if the line starts with a label
            if (!opcodes.containsKey(parts[0]) && !parts[0].equals(".fill")) {
                String label = parts[0];
                if (symbolTable.containsKey(label)) {
                    System.err.println("Error: Duplicate label found: " + label);
                    System.exit(1);
                }
                symbolTable.put(label, currentAddress);  // Add label to symbol table
                parts = Arrays.copyOfRange(parts, 1, parts.length);
            }

            // Skip empty lines after label removal
            if (parts.length == 0) continue;

            currentAddress++;  // Increment address for each instruction or label
        }
    }

    // Second pass: Generate machine code and write to file
    private static void secondPass(List<String> assemblyCode) {
        currentAddress = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            for (String line : assemblyCode) {
                String[] parts = line.split("\\s+");

                // Skip label if present
                if (!opcodes.containsKey(parts[0]) && !parts[0].equals(".fill")) {
                    parts = Arrays.copyOfRange(parts, 1, parts.length);
                }

                // Skip empty lines
                if (parts.length == 0) continue;

                String instruction = parts[0];
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

                            case "beq": // จะใช้ค่า offset คำนวณเป็นการข้ามบรรทัด (relative jump)
                                regA = Integer.parseInt(parts[1]);
                                regB = Integer.parseInt(parts[2]);
                                int offset = 0; //คำนวณค่า offset โดยใช้ตำแหน่งปัจจุบัน (current address) ลบด้วยตำแหน่งของ label

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

                            case "jalr":  //จะใช้รีจิสเตอร์ regA และ regB โดยเก็บไว้ในบิตที่ 19-21 และ 16-18 ตามลำดับ
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
                    } else if (instruction.equals(".fill")) { // ใช้เพื่อกำหนดค่าโดยตรงลงในตำแหน่งหน่วยความจำ (ซึ่งอาจเป็นค่าตัวเลขหรือ label)
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
                    //หลังจากแปลงคำสั่งเป็น Machine Code แล้ว จะเขียนลงไฟล์และเพิ่มค่า currentAddress
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

    //ตรวจสอบว่าข้อความที่ได้รับเป็นตัวเลขหรือไม่
    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+");
    }
}

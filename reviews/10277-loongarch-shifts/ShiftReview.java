// Validates the three LoongArch immediate shifts changed in Ghidra PR #10277.
// @category Testing

import java.math.BigInteger;

import ghidra.app.emulator.EmulatorHelper;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Instruction;

public class ShiftReview extends GhidraScript {
    private static final String[] MNEMONICS = {"slli.d", "srli.d", "srai.d"};
    private static final String[] INPUTS = {
        "0123456789abcdef", "fedcba9876543211", "0000000000000001",
        "8000000000000000", "ffffffffffffffff", "0000000000000000"
    };
    private static final BigInteger MODULUS = BigInteger.ONE.shiftLeft(64);
    private static final BigInteger MASK = MODULUS.subtract(BigInteger.ONE);

    private BigInteger expected(String mnemonic, BigInteger input, int count) {
        switch (mnemonic) {
            case "slli.d":
                return input.shiftLeft(count).and(MASK);
            case "srli.d":
                return input.shiftRight(count);
            case "srai.d":
                BigInteger signed = input.testBit(63) ? input.subtract(MODULUS) : input;
                return signed.shiftRight(count).and(MASK);
            default:
                throw new IllegalArgumentException(mnemonic);
        }
    }

    @Override
    public void run() throws Exception {
        int mismatches = 0;
        int checks = 0;
        Address start = currentProgram.getMinAddress();
        EmulatorHelper emulator = new EmulatorHelper(currentProgram);
        try {
            for (int opcode = 0; opcode < MNEMONICS.length; opcode++) {
                String mnemonic = MNEMONICS[opcode];
                for (int count = 0; count < 64; count++) {
                    Address address = start.add(4L * (opcode * 64 + count));
                    disassemble(address);
                    Instruction instruction = getInstructionAt(address);
                    if (instruction == null || !mnemonic.equalsIgnoreCase(instruction.getMnemonicString())) {
                        throw new IllegalStateException("Unexpected decode at " + address + ": " + instruction);
                    }

                    for (String hex : INPUTS) {
                        BigInteger input = new BigInteger(hex, 16);
                        emulator.writeRegister("a0", BigInteger.ZERO);
                        emulator.writeRegister("a1", input);
                        emulator.writeRegister(emulator.getPCRegister(), address.getOffset());
                        if (!emulator.step(monitor)) {
                            throw new IllegalStateException("Emulator failed at " + address + ": " + emulator.getLastError());
                        }
                        BigInteger actual = emulator.readRegister("a0").and(MASK);
                        BigInteger reference = expected(mnemonic, input, count);
                        checks++;
                        if (!actual.equals(reference)) {
                            mismatches++;
                        }
                        if (count == 31 || count == 32 || count == 63) {
                            println(String.format("SHIFT_VECTOR %s %d %s %016x %016x",
                                mnemonic, count, hex, reference, actual));
                        }
                    }
                }
            }
            println("SHIFT_SUMMARY checks=" + checks + " mismatches=" + mismatches);
        }
        finally {
            emulator.dispose();
        }
    }
}

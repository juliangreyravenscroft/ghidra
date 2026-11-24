"""Generate little-endian LoongArch shift instructions using a0 and a1."""

import argparse
import struct
from pathlib import Path

OPCODES = (0x41, 0x45, 0x49)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    words = (
        struct.pack("<I", (opcode << 16) | (amount << 10) | (5 << 5) | 4)
        for opcode in OPCODES
        for amount in range(64)
    )
    args.output.write_bytes(b"".join(words))

if __name__ == "__main__":
    main()

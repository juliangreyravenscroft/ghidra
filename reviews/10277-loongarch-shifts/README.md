# Ghidra PR 10277 independent regression check

Ghidra 12.2 PUBLIC and Temurin 25 were used on Linux. Only the LoongArch SLEIGH definitions were rebuilt from each revision. The emulator and the rest of Ghidra came from the same installation for both runs.

| Revision | Checks | Mismatches |
| --- | ---: | ---: |
| c8d842450e18d132a4f154702878be27daa21a43 | 1152 | 355 |
| b0d0ce58824b8ba3e4c098d8e23676911d4b1a8c | 1152 | 0 |

The test executes slli.d, srli.d and srai.d for every immediate from 0 through 63 with six 64-bit inputs. The reference uses BigInteger calculations independent of Ghidra. The logs show individual results for counts 31, 32 and 63. The two source trees differ in one file within the LoongArch language directory.

## Run

Use a Ghidra 12.2 PUBLIC installation, JDK 25, Bash, Git, Python 3 and a Ghidra source checkout. Allow space for a temporary copy of the installation. The script leaves the original installation and source working tree untouched.

```bash
git -C /path/to/ghidra-source fetch https://github.com/NationalSecurityAgency/ghidra.git c8d842450e18d132a4f154702878be27daa21a43 b0d0ce58824b8ba3e4c098d8e23676911d4b1a8c
bash run.sh /path/to/ghidra_12.2_PUBLIC /path/to/ghidra-source
```

The runner generates the 768-byte fixture in its temporary directory. `results.csv` records the observed boundary results at counts 31, 32 and 63. Values are 64-bit hex. No LoongArch hardware was used.

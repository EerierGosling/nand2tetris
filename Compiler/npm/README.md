# jackc

compiler for jack! this package compiles between

- jack
- vm code
- assembly
- binary

for the nand2tetris hack computer.

compiler is written in java, and test GUI is from the [nand2tetris software suite](https://www.nand2tetris.org/software) (not built by me!).

## background

jack is a language very similar to java but much easier to compile, designed for the [nand2tetris course](https://www.nand2tetris.org)! it runs on the hack computer (also from the course).

the nand2tetris software suite contains many emylators to test scripts/chips, and two are bundled into this package! those are the vm emulator, cpu emulator, and hardware simulator (not useful for right now - but i may add features in the future that need it!).


## commands

how to use jackc:

jackc [-i <asm|vm|jack>] [-o <hack|asm|vm>] <file-or-folder>

  -i, --in <type>   input file type (inferred from extension if omitted)
  -o, --out <type>  output file type (default: hack)
  -h, --help        show this message

if <file-or-folder> is a folder, all matching files in it and its subfolders are compiled

emulators (official nand2tetris tools - need java installed):
  jackc vm-emulator [file.tst]         run VM programs (built-in OS, no 32K limit)
  jackc cpu-emulator [file.tst]        run compiled .hack binaries
  jackc hardware-simulator [file.tst]  simulate .hdl chip designs
with no .tst file they open interactively; with one they run it in batch mode
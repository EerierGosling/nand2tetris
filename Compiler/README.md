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

#### file extensions

jack -> .jack
vm code -> .vm
assembly -> .asm
binary -> .hack


## usage

if you want to compile a script and run it, you have two choices:

- compile to binary (.hack) and run in the cpu emulator (works for jack, vm, and assembly code)
- compile to vm and run in the vm emulator (work for jack code)

the vm emulator will allow you to run much larger scripts than the cpu emulator, which has a much smaller memory limit (the OS takes up a significant amount of the available memory).

you can use the snake script that i wrote in jack [here](https://github.com/EerierGosling/nand2tetris/tree/main/9/Snake) as a test! it's too big to compile to binary - it'll only run in the vm emulator.


## how it's built

the package has a lot of different parts.
- the compiler is written in java and is [here](https://github.com/EerierGosling/nand2tetris/tree/main/Compiler)
- the OS was written in jack (compiled to vm) and can be found [here](https://github.com/EerierGosling/nand2tetris/tree/main/12)
- the test snake script was written in jack and can be found [here](https://github.com/EerierGosling/nand2tetris/tree/main/9/Snake)
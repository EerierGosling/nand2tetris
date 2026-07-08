# jackc

compiler for jack! this package compiles between

- jack
- vm code
- assembly
- binary

for the nand2tetris hack computer.

compiler is written in java, and test GUI is from the [nand2tetris software suite](https://www.nand2tetris.org/software) (not built by me!).


how to use jackc:

jackc [-i <asm|vm|jack>] [-o <hack|asm|vm>] <file-or-folder>

  -i, --in <type>   input file type (inferred from extension if omitted)
  -o, --out <type>  output file type (default: hack)
  -h, --help        show this message

if <file-or-folder> is a folder, all matching files in it and its subfolders are compiled
# jackc

A complete compiler toolchain for the [nand2tetris](https://www.nand2tetris.org/) Hack platform, written from scratch. Takes Jack (a Java-like language) all the way down to Hack machine code — compiler, VM translator, and assembler in one tool, with the operating system built in.

Ships as a native binary (built with GraalVM) — no Java required.

## Install

```sh
npm install -g jackc
```

## Usage

```sh
jackc Main.jack              # compile one file to a .hack binary
jackc -o vm Main.jack        # stop at VM code
jackc -o asm Main.jack       # stop at assembly
jackc -i jack -o hack src/   # compile a whole program folder into one .hack binary
```

Compiling a folder links every class in it (plus whichever OS classes the program actually uses) into a single runnable `.hack` image with the bootstrap included — load it straight into the CPU Emulator.

| input     | output              |
| --------- | ------------------- |
| `.jack`   | `.vm`, `.asm`, `.hack` |
| `.vm`     | `.asm`, `.hack`     |
| `.asm`    | `.hack`             |

The input type is inferred from the file extension; `-i` is only needed for folders.

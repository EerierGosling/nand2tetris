// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/4/Fill.asm

// Runs an infinite loop that listens to the keyboard input. 
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel. When no key is pressed, 
// the screen should be cleared.

//// Replace this comment with your code.

@24500 // 16384 + 8160
D=A
@address
M=D

(WHITE)
    @address
    A=M
    M=0
    A=A-1
    D=A
    @address
    M=D
    
    @16383 // 16384 - 1
    D=D-A

    @SETEND
    D;JEQ

    @KBD
    D=M
    @BLACK
    D;JNE
    @WHITE
    0;JMP

(BLACK)
    @address
    A=M
    M=-1
    A=A+1
    D=A
    @address
    M=D
    @24544 // 16384 + 8160
    D=D-A

    @SETSTART
    D;JEQ

    @KBD
    D=M
    @BLACK
    D;JNE
    @WHITE
    0;JMP

(SETSTART)
    @SCREEN
    D=A
    @address
    M=D

    @BLACK
    D;JMP

(SETEND)
    @24544 // 16384 + 8160
    D=A
    @address
    M=D

    @WHITE
    D;JMP
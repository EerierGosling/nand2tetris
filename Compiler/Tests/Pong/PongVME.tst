// Runs Pong on the VM emulator - loads Ball.vm/Bat.vm/Main.vm/PongGame.vm directly,
// using the emulator's own built-in OS (no assembly step, no 32K ROM limit).

load,  // loads all the VM files from the current folder
output-file Pong.out,

set sp 256,

repeat 10000 {
  vmstep;
}

output-list RAM[0]%D1.6.1 RAM[16384]%D1.6.1;
output;

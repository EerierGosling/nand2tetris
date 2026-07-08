#!/usr/bin/env node
// thin shim: npm puts this on PATH as `jackc`.
// - `jackc vm-emulator` / `jackc cpu-emulator` / `jackc hardware-simulator`
//   (each takes an optional .tst file) launch the official nand2tetris tools
//   (need java on the system)
// - everything else goes straight to the native compiler binary
const { spawnSync } = require("child_process");
const path = require("path");
const fs = require("fs");

const args = process.argv.slice(2);

const emulators = {
  "vm-emulator": "VMEmulatorMain",
  "cpu-emulator": "CPUEmulatorMain",
  "hardware-simulator": "HardwareSimulatorMain",
};

if (emulators[args[0]]) {
  runEmulator(emulators[args[0]], args.slice(1));
} else {
  runCompiler(args);
}

function runCompiler(args) {
  const binary = path.join(
    __dirname,
    process.platform === "win32" ? "jackc-native.exe" : "jackc-native"
  );

  if (!fs.existsSync(binary)) {
    console.error("jackc: native binary not found - the install step may have been skipped.");
    console.error("try reinstalling without --ignore-scripts: npm install -g jack-compiler");
    process.exit(1);
  }

  const result = spawnSync(binary, args, { stdio: "inherit" });
  process.exit(result.status ?? 1);
}

function runEmulator(mainClass, args) {
  const toolsDir = path.join(__dirname, "..", "tools");
  if (!fs.existsSync(path.join(toolsDir, "bin", "classes"))) {
    console.error("jackc: emulator tools not found - try reinstalling: npm install -g jack-compiler");
    process.exit(1);
  }

  // emulators are GUI java apps - they need a real JVM, unlike the compiler
  const java = process.env.JAVA_HOME
    ? path.join(process.env.JAVA_HOME, "bin", "java")
    : "java";

  const classpath = [
    path.join(toolsDir, "bin", "classes"),
    ...["Hack", "HackGUI", "Simulators", "SimulatorsGUI", "Compilers"].map((jar) =>
      path.join(toolsDir, "bin", "lib", `${jar}.jar`)
    ),
  ].join(path.delimiter);

  // absolute path, since the emulator runs with cwd=toolsDir (it finds its
  // built-in OS and help files relative to there, like the official .sh scripts)
  const emulatorArgs = args.map((a) => (a.endsWith(".tst") ? path.resolve(a) : a));

  const result = spawnSync(java, ["-classpath", classpath, mainClass, ...emulatorArgs], {
    stdio: "inherit",
    cwd: toolsDir,
  });

  if (result.error && result.error.code === "ENOENT") {
    console.error("jackc: java not found - the emulators need a Java runtime (the compiler itself doesn't).");
    console.error("install one from https://adoptium.net or set JAVA_HOME");
    process.exit(1);
  }
  process.exit(result.status ?? 1);
}

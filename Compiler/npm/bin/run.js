#!/usr/bin/env node
// thin shim: npm puts this on PATH as `jackc`, it just execs the native binary
const { spawnSync } = require("child_process");
const path = require("path");
const fs = require("fs");

const binary = path.join(
  __dirname,
  process.platform === "win32" ? "jackc-native.exe" : "jackc-native"
);

if (!fs.existsSync(binary)) {
  console.error("jackc: native binary not found - the install step may have been skipped.");
  console.error("try reinstalling without --ignore-scripts: npm install -g jackc");
  process.exit(1);
}

const result = spawnSync(binary, process.argv.slice(2), { stdio: "inherit" });
process.exit(result.status ?? 1);

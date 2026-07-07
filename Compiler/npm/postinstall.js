// downloads the prebuilt jackc binary for this platform from GitHub Releases,
// plus the nand2tetris emulator tools (jars - run with system java via
// `jackc vm-emulator` / `jackc cpu-emulator`).
// runs once at `npm install` time - the package itself ships no binaries.
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const { version, repository } = require("./package.json");
const repo = repository.url.match(/github\.com\/(.+?)(\.git)?$/)[1];

const targets = {
  "darwin-arm64": "jackc-darwin-arm64",
  "darwin-x64": "jackc-darwin-x64",
  "linux-x64": "jackc-linux-x64",
  "win32-x64": "jackc-win32-x64.exe",
};

const key = `${process.platform}-${process.arch}`;
const asset = targets[key];

if (!asset) {
  console.error(`jackc: no prebuilt binary for ${key}`);
  console.error(`supported: ${Object.keys(targets).join(", ")}`);
  process.exit(1);
}

const binaryUrl = `https://github.com/${repo}/releases/download/v${version}/${asset}`;
const binaryDest = path.join(__dirname, "bin", asset.endsWith(".exe") ? "jackc-native.exe" : "jackc-native");

const toolsUrl = `https://raw.githubusercontent.com/${repo}/main/Compiler/npm-assets/nand2tetris-tools.tar.gz`;
const toolsDir = path.join(__dirname, "tools");

async function download(url, dest) {
  console.log(`jackc: downloading ${url}`);
  const res = await fetch(url); // follows GitHub's redirect to the actual asset host
  if (!res.ok) {
    throw new Error(`download failed: ${res.status} ${res.statusText}`);
  }
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.writeFileSync(dest, Buffer.from(await res.arrayBuffer()));
}

async function main() {
  await download(binaryUrl, binaryDest);
  fs.chmodSync(binaryDest, 0o755);
  console.log(`jackc: installed binary at ${binaryDest}`);

  // emulator tools are optional - a failure here shouldn't break the compiler install
  try {
    const tarball = path.join(__dirname, "tools.tar.gz");
    await download(toolsUrl, tarball);
    fs.mkdirSync(toolsDir, { recursive: true });
    const tar = spawnSync("tar", ["-xzf", tarball, "-C", toolsDir], { stdio: "inherit" });
    if (tar.status !== 0) throw new Error("tar extraction failed");
    fs.unlinkSync(tarball);
    console.log(`jackc: installed emulator tools at ${toolsDir}`);
  } catch (err) {
    console.warn(`jackc: emulator tools unavailable (${err.message}) - compiling still works, but 'jackc vm-emulator'/'jackc cpu-emulator' won't`);
  }
}

main().catch((err) => {
  console.error(`jackc: ${err.message}`);
  process.exit(1);
});

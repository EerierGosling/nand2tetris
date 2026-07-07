// downloads the prebuilt jackc binary for this platform from GitHub Releases.
// runs once at `npm install` time - the package itself ships no binaries.
const fs = require("fs");
const path = require("path");

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

const url = `https://github.com/${repo}/releases/download/v${version}/${asset}`;
const dest = path.join(__dirname, "bin", asset.endsWith(".exe") ? "jackc-native.exe" : "jackc-native");

async function main() {
  console.log(`jackc: downloading ${url}`);
  const res = await fetch(url); // follows GitHub's redirect to the actual asset host
  if (!res.ok) {
    throw new Error(`download failed: ${res.status} ${res.statusText}`);
  }

  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.writeFileSync(dest, Buffer.from(await res.arrayBuffer()));
  fs.chmodSync(dest, 0o755);
  console.log(`jackc: installed binary at ${dest}`);
}

main().catch((err) => {
  console.error(`jackc: ${err.message}`);
  process.exit(1);
});

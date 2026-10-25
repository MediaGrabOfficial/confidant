import { readFileSync } from "node:fs";
import vm from "node:vm";

const html = readFileSync("site/index.html", "utf8");
const checks = [
  ["PeerJS URL is valid HTTPS", html.includes('src="https://cdnjs.cloudflare.com/ajax/libs/peerjs/1.5.4/peerjs.min.js"')],
  ["QRCode.js URL is valid HTTPS", html.includes('src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"')],
  ["ECDH P-256 is present", html.includes('namedCurve:"P-256"')],
  ["HKDF SHA-256 is present", html.includes('hash:"SHA-256"')],
  ["AES-GCM is present", html.includes('name:"AES-GCM"')],
  ["IndexedDB local storage is present", html.includes('indexedDB.open(DB_NAME, DB_VERSION)')],
  ["No malformed escaped scheme", !html.includes('https\\://')]
];
for (const [name, ok] of checks) {
  if (!ok) {
    console.error("FAIL:", name);
    process.exitCode = 1;
  } else {
    console.log("PASS:", name);
  }
}
const inlineScripts = [...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/gi)]
  .map(match => match[1])
  .filter(script => script.trim());
if (!inlineScripts.length) {
  console.error("FAIL: inline application script found");
  process.exitCode = 1;
} else {
  for (const [index, script] of inlineScripts.entries()) {
    try {
      new vm.Script(script, { filename: "site/index.html:inline-" + (index + 1) + ".js" });
    } catch (error) {
      console.error("FAIL: JavaScript syntax:", error.message);
      process.exitCode = 1;
    }
  }
  if (!process.exitCode) console.log("PASS: syntax of " + inlineScripts.length + " inline script(s)");
}

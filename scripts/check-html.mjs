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
  ["Conversation list is wired", html.includes('id="conversationList"') && html.includes("function refreshConversationList()")],
  ["Invite screen can open local history", html.includes('id="btnViewHistory"') && html.includes('await loadHistory();')],
  ["Identity/key persistence is present", html.includes("privateKey:State.keyPair.privateKey") && html.includes("localPeerId:State.myId")],
  ["Text queue is present", html.includes('status:"queued"') && html.includes("async function flushQueuedMessages()")],
  ["Delivery ACK handler is present", html.includes("async function handleMessageAck(msg)") && html.includes('msg.t === "ack"')],
  ["ACK uses Web Crypto", html.includes('const aad=State.convId+"|ack|"') && html.includes("sendMessageAck(record.id,msg.sender)")],
  ["Control messages are encrypted", html.includes("async function handleEncryptedControl(msg)") && html.includes('msg.t === "control"')],
  ["No direct unencrypted delete-request route", !html.includes('msg.t === "delete-request"')],
  ["Reconnect attempts are bounded", html.includes("[1000,2000,4000,8000,15000][State.reconnectAttempts++]")],
  ["Integrated QR scan action exists", html.includes('id="btnScanInvite"') && html.includes("async function startQrScanner()")],
  ["QR scanner supports camera APIs", html.includes("BarcodeDetector") && html.includes("jsQR(frame.data")],
  ["QR intent has browser fallback", html.includes("buildQrInviteLink()") && html.includes("S.browser_fallback_url")],
  ["Invitations can be opened from query parameters", html.includes("new URLSearchParams(location.search).has("id")")],
  ["Android mobile input adjusts to keyboard", html.includes("visualViewport") && html.includes("--keyboard-bottom")],
  ["Reconnect ignores stale connection events", html.includes("State.conn !== conn) return;")],
  ["Offline media queue is present", html.includes("async function transmitFileRecord") && html.includes("requeueUnconfirmedFiles")],
  ["Saved chats reopen without another invitation", html.includes('showScreen("chatScreen");') && html.includes("async function resumeConversation")],
  ["Message notification hook exists", html.includes("async function notifyIncomingMessage") && html.includes("btnEnableNotifications")],
  ["No malformed escaped scheme", !html.includes("https\\://")]
];
let failed = false;
for (const [name, ok] of checks) {
  if (!ok) {
    console.error("FAIL:", name);
    failed = true;
  } else {
    console.log("PASS:", name);
  }
}
const inlineScripts = [...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/gi)]
  .map(match => match[1])
  .filter(script => script.trim());
if (!inlineScripts.length) {
  console.error("FAIL: inline application script found");
  failed = true;
} else {
  for (const [index, script] of inlineScripts.entries()) {
    try {
      new vm.Script(script, { filename: "site/index.html:inline-" + (index + 1) + ".js" });
    } catch (error) {
      console.error("FAIL: JavaScript syntax:", error.message);
      failed = true;
    }
  }
  if (!failed) console.log("PASS: syntax of " + inlineScripts.length + " inline script(s)");
}
if (failed) process.exitCode = 1;

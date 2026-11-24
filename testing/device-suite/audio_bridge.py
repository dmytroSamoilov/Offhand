"""Mac-side helper for the on-device Maestro suite.

Maestro runs on this Mac, so a flow reaches the bridge on 127.0.0.1 from
`runScript` (http.post). The bridge plays a fixture clip through the Mac's
speakers for the phone's microphone, and scores the note the flow exported to
the phone's Download folder against the fixture text. A summaryMustMention
entry passes when the summary contains any of its "|"-separated choices.

Only clip ids that have a fixture are accepted; nothing from a request reaches
a path or a shell command any other way.

    POST /play   {"clip": "<id>"}   -> starts playback, {"durationMs", "livePhrase"}
    POST /status {}                 -> {"playing": bool}
    POST /score  {"clip": "<id>", "processingMs": n, "mode": "acoustic"}
                                    -> {"pass": bool, "wer": float, ...}

Run by run-android.sh; standalone: python3 -I audio_bridge.py --serial <adb serial>

run-ios.sh has no bridge to call from the device, so it scores a note the test
handed back as JSON ({"summary", "transcript", "processingMs"}) in one shot:

    python3 -I audio_bridge.py --score-export <file> --clip <id> --device-name <name>

and asks for a clip's timing with --describe-clip <id>.
"""
import argparse
import json
import re
import subprocess
import threading
import time
import unicodedata
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

SUITE = Path(__file__).resolve().parent
FIXTURES = SUITE / "fixtures"
CLIPS = SUITE / "clips"
CLIP_ID = re.compile(r"^[a-z0-9-]{1,64}$")
DEVICE_DOWNLOADS = "/sdcard/Download"
CLOCK_SKEW_S = 120


class Player:
    def __init__(self) -> None:
        self._process: subprocess.Popen | None = None
        self._lock = threading.Lock()

    def play(self, clip: Path) -> None:
        with self._lock:
            if self._process and self._process.poll() is None:
                raise BridgeError(409, "a clip is already playing")
            self._process = subprocess.Popen(["afplay", str(clip)])

    def is_playing(self) -> bool:
        with self._lock:
            return self._process is not None and self._process.poll() is None


class BridgeError(Exception):
    def __init__(self, status: int, message: str) -> None:
        super().__init__(message)
        self.status = status


def load_fixture(clip_id: object) -> dict:
    if not isinstance(clip_id, str) or not CLIP_ID.match(clip_id):
        raise BridgeError(400, "invalid clip id")
    path = FIXTURES / f"{clip_id}.json"
    if not path.is_file():
        raise BridgeError(404, f"no fixture for {clip_id}")
    return json.loads(path.read_text())


def clip_duration_ms(clip: Path) -> int:
    info = subprocess.run(["afinfo", str(clip)], capture_output=True, text=True, check=True).stdout
    seconds = re.search(r"estimated duration: ([0-9.]+)", info)
    return int(float(seconds.group(1)) * 1000) if seconds else 0


def words(text: str) -> list[str]:
    folded = unicodedata.normalize("NFKC", text).lower().replace("’", "").replace("'", "")
    return re.findall(r"\w+", folded)


def word_error_rate(reference: str, hypothesis: str) -> float:
    ref, hyp = words(reference), words(hypothesis)
    previous = list(range(len(hyp) + 1))
    for i, ref_word in enumerate(ref, start=1):
        current = [i] + [0] * len(hyp)
        for j, hyp_word in enumerate(hyp, start=1):
            substitution = previous[j - 1] + (ref_word != hyp_word)
            current[j] = min(previous[j] + 1, current[j - 1] + 1, substitution)
        previous = current
    return previous[-1] / max(len(ref), 1)


def mentions(summary: str, term: str) -> bool:
    return any(choice.strip().lower() in summary.lower() for choice in term.split("|"))


def split_export(text: str, summary_label: str, transcript_label: str) -> tuple[str, str]:
    lines = text.splitlines()
    summary_at = lines.index(f"{summary_label}:")
    transcript_at = len(lines) - 1 - lines[::-1].index(f"{transcript_label}:")
    summary = "\n".join(lines[summary_at + 1:transcript_at]).strip()
    transcript = "\n".join(lines[transcript_at + 1:]).strip()
    return summary, transcript


def quoted(path: str) -> str:
    return "'" + path.replace("'", "'\\''") + "'"


class Device:
    def __init__(self, serial: str) -> None:
        self.serial = serial

    def adb(self, *args: str) -> str:
        command = ["adb", "-s", self.serial, *args]
        return subprocess.run(command, capture_output=True, text=True, encoding="utf-8", check=True).stdout

    def take_newest_export(self, not_before: float) -> str:
        listing = self.adb("shell", "ls", "-t", DEVICE_DOWNLOADS).splitlines()
        exports = [name.strip() for name in listing if name.strip().endswith(".txt")]
        if not exports:
            raise BridgeError(404, "no .txt export in Download")
        remote = quoted(f"{DEVICE_DOWNLOADS}/{exports[0]}")
        if float(self.adb("shell", f"stat -c %Y {remote}").strip()) < not_before - CLOCK_SKEW_S:
            raise BridgeError(404, f"newest export is older than this run: {exports[0]}")
        export = self.adb("shell", f"cat {remote}")
        self.adb("shell", f"rm -f {remote}")
        return export

    def model(self) -> str:
        return self.adb("shell", "getprop", "ro.product.model").strip()


class ScoredNote:
    def __init__(self, summary: str, transcript: str, processing_ms: object, device: str, mode: str) -> None:
        self.summary = summary
        self.transcript = transcript
        self.processing_ms = processing_ms
        self.device = device
        self.mode = mode


def score_note(fixture: dict, note: ScoredNote, results: Path) -> dict:
    wer = word_error_rate(fixture["text"], note.transcript)
    missing = [term for term in fixture["summaryMustMention"] if not mentions(note.summary, term)]
    threshold = fixture["maxWerImport" if note.mode == "import" else "maxWerAcoustic"]
    result = {
        "clip": fixture["id"],
        "mode": note.mode,
        "device": note.device,
        "wer": round(wer, 4),
        "maxWer": threshold,
        "summaryMissing": missing,
        "processingMs": note.processing_ms,
        "transcript": note.transcript,
        "summary": note.summary,
        "at": time.strftime("%Y-%m-%dT%H:%M:%S"),
    }
    result["pass"] = wer <= threshold and not missing
    with results.open("a", encoding="utf-8") as out:
        out.write(json.dumps(result, ensure_ascii=False) + "\n")
    print(f"[bridge] {fixture['id']} wer={wer:.3f} (max {threshold}) missing={missing} pass={result['pass']}")
    return result


def score_export(export: Path, clip_id: str, device_name: str, results: Path) -> bool:
    note = json.loads(export.read_text(encoding="utf-8"))
    scored = ScoredNote(
        str(note.get("summary", "")), str(note.get("transcript", "")), note.get("processingMs"), device_name, "acoustic"
    )
    return score_note(load_fixture(clip_id), scored, results)["pass"]


def describe_clip(clip_id: str) -> dict:
    fixture = load_fixture(clip_id)
    clip = CLIPS / f"{fixture['id']}.m4a"
    if not clip.is_file():
        raise BridgeError(404, f"clip not rendered: {clip.name}")
    return {"clip": str(clip), "durationMs": clip_duration_ms(clip), "livePhrase": fixture["livePhrase"]}


class Bridge:
    def __init__(self, device: Device, results: Path, labels: tuple[str, str]) -> None:
        self.device = device
        self.results = results
        self.labels = labels
        self.player = Player()
        self.played_at = 0.0

    def play(self, body: dict) -> dict:
        fixture = load_fixture(body.get("clip"))
        clip = CLIPS / f"{fixture['id']}.m4a"
        if not clip.is_file():
            raise BridgeError(404, f"clip not rendered: {clip.name}")
        self.player.play(clip)
        self.played_at = time.time()
        return {"durationMs": clip_duration_ms(clip), "livePhrase": fixture["livePhrase"]}

    def status(self, _: dict) -> dict:
        return {"playing": self.player.is_playing()}

    def score(self, body: dict) -> dict:
        fixture = load_fixture(body.get("clip"))
        mode = "import" if body.get("mode") == "import" else "acoustic"
        summary, transcript = split_export(self.device.take_newest_export(self.played_at), *self.labels)
        note = ScoredNote(summary, transcript, body.get("processingMs"), self.device.model(), mode)
        return score_note(fixture, note, self.results)

    def routes(self) -> dict:
        return {"/play": self.play, "/status": self.status, "/score": self.score}


def handler_for(bridge: Bridge) -> type[BaseHTTPRequestHandler]:
    routes = bridge.routes()

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self) -> None:
            route = routes.get(self.path)
            if route is None:
                return self.reply(404, {"error": "unknown route"})
            try:
                length = min(int(self.headers.get("Content-Length") or 0), 4096)
                body = json.loads(self.rfile.read(length) or b"{}")
                self.reply(200, route(body if isinstance(body, dict) else {}))
            except BridgeError as error:
                self.reply(error.status, {"error": str(error)})
            except (ValueError, subprocess.CalledProcessError) as error:
                self.reply(500, {"error": f"{type(error).__name__}: {error}"})

        def reply(self, status: int, payload: dict) -> None:
            data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

        def log_message(self, format: str, *args: object) -> None:
            print(f"[bridge] {self.command} {self.path} -> {args[1] if len(args) > 1 else ''}")

    return Handler


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial")
    parser.add_argument("--score-export", type=Path)
    parser.add_argument("--describe-clip")
    parser.add_argument("--clip")
    parser.add_argument("--device-name", default="iOS device")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--results", type=Path, default=SUITE / "results" / "device-results.jsonl")
    parser.add_argument("--summary-label", default="Summary")
    parser.add_argument("--transcript-label", default="Transcript")
    args = parser.parse_args()
    args.results.parent.mkdir(parents=True, exist_ok=True)
    if args.describe_clip:
        print(json.dumps(describe_clip(args.describe_clip)))
        return
    if args.score_export:
        raise SystemExit(0 if score_export(args.score_export, args.clip, args.device_name, args.results) else 1)
    if not args.serial:
        parser.error("--serial is required to run the bridge")
    bridge = Bridge(Device(args.serial), args.results, (args.summary_label, args.transcript_label))
    server = ThreadingHTTPServer(("127.0.0.1", args.port), handler_for(bridge))
    print(f"[bridge] listening on 127.0.0.1:{args.port}, results -> {args.results}")
    server.serve_forever()


if __name__ == "__main__":
    main()

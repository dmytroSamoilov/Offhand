// Calls the Mac-side audio bridge (testing/device-suite/audio_bridge.py).
// ACTION is play, status or score; CLIP is the fixture id. Results land in `output`.
function call(route, body) {
    const response = http.post('http://127.0.0.1:8765/' + route, {
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    })
    const payload = JSON.parse(response.body)
    if (response.status !== 200) throw new Error('bridge /' + route + ': ' + payload.error)
    return payload
}

if (typeof CLIP === 'undefined') {
    throw new Error('pass the fixture id: maestro test -e CLIP=<id>')
} else if (ACTION === 'play') {
    output.livePhrase = call('play', { clip: CLIP }).livePhrase
    output.playing = true
} else if (ACTION === 'status') {
    output.playing = call('status', {}).playing
} else if (ACTION === 'score') {
    const score = call('score', { clip: CLIP, mode: 'acoustic', processingMs: output.processingMs })
    output.pass = score.pass
    output.verdict = 'WER ' + score.wer + ' (max ' + score.maxWer + '), summary missing ' + JSON.stringify(score.summaryMissing)
} else {
    throw new Error('unknown ACTION ' + ACTION)
}

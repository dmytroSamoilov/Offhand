package com.dmytrosamoilov.offhand.core.audio

object PcmResampler {

    fun downmix(interleaved: ShortArray, channels: Int): ShortArray {
        if (channels <= 1) return interleaved
        val frames = interleaved.size / channels
        return ShortArray(frames) { frame ->
            var sum = 0
            for (channel in 0 until channels) sum += interleaved[frame * channels + channel]
            (sum / channels).toShort()
        }
    }

    fun resample(mono: ShortArray, inputRate: Int, outputRate: Int): ShortArray {
        require(inputRate > 0 && outputRate > 0) { "Sample rates must be positive" }
        if (inputRate == outputRate || mono.isEmpty()) return mono
        val smoothed = if (inputRate > outputRate) lowPass(mono, inputRate / outputRate) else mono
        val outputSize = (mono.size.toLong() * outputRate / inputRate).toInt()
        val step = inputRate.toDouble() / outputRate
        return ShortArray(outputSize) { index -> interpolate(smoothed, index * step) }
    }

    private fun interpolate(samples: ShortArray, position: Double): Short {
        val left = position.toInt().coerceIn(0, samples.lastIndex)
        val right = (left + 1).coerceAtMost(samples.lastIndex)
        val fraction = position - left
        return (samples[left] + (samples[right] - samples[left]) * fraction).toInt().toShort()
    }

    private fun lowPass(samples: ShortArray, window: Int): ShortArray {
        if (window <= 1) return samples
        val half = window / 2
        val prefix = LongArray(samples.size + 1)
        samples.forEachIndexed { index, sample -> prefix[index + 1] = prefix[index] + sample }
        return ShortArray(samples.size) { index ->
            val start = (index - half).coerceAtLeast(0)
            val end = (index + half + 1).coerceAtMost(samples.size)
            ((prefix[end] - prefix[start]) / (end - start)).toShort()
        }
    }
}

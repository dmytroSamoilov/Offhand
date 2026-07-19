package com.dmytrosamoilov.offhand.core.ai.api

/**
 * Text-path execution target for the on-device engine, picked per model via
 * models.json. Known constraints of the LiteRT-LM 0.10.x AAR:
 *  - GPU is broken on Pixel Tensor G3–G5 (the AAR lacks the GPU sampler
 *    libraries); it works on Snapdragon Adreno.
 *  - NPU requires NPU weights in the model bundle; the catalog bundles
 *    ship CPU weights only.
 */
enum class HardwareBackend {
    CPU,
    GPU,
    NPU,
}

"""Render the iOS app icon from the Android launcher vector art.

Geometry is taken verbatim from app/src/main/res/drawable/ic_launcher_*.xml.
Android masks its 108dp adaptive icon down to the centre 72dp, while iOS shows
the whole square, so we crop to that same 72dp safe zone. That keeps the mark at
the size Android users actually see instead of leaving it looking inset.
"""

from PIL import Image, ImageDraw

VIEWPORT = 108.0
SAFE_ORIGIN = 18.0        # (108 - 72) / 2
SAFE_SIZE = 72.0
OUT = 1024
SUPERSAMPLE = 4           # render big, downsample for clean edges

CIRCLE_CENTER = (54.0, 54.0)
CIRCLE_RADIUS = 20.19
CIRCLE_STROKE = 7.29

BAR_STROKE = 3.28
BARS = [
    (48.58, 51.64, 56.30),
    (54.00, 47.63, 60.31),
    (59.42, 51.64, 56.30),
]

FOREGROUND = (255, 255, 255)
FLAVORS = {
    "AppIcon": (0x0B, 0x57, 0xD0),       # brand blue, matches Android exactly
    "AppIcon-dev": (0x00, 0x6A, 0x60),   # brand teal, so dev is obvious on the home screen
}


def render(background):
    size = OUT * SUPERSAMPLE
    scale = size / SAFE_SIZE

    def px(value):
        return (value - SAFE_ORIGIN) * scale

    image = Image.new("RGB", (size, size), background)
    draw = ImageDraw.Draw(image)

    cx, cy = px(CIRCLE_CENTER[0]), px(CIRCLE_CENTER[1])

    # A stroked circle drawn as two filled discs, so the stroke stays centred on
    # the path rather than biting inwards the way PIL's outline width does.
    outer = (CIRCLE_RADIUS + CIRCLE_STROKE / 2) * scale
    inner = (CIRCLE_RADIUS - CIRCLE_STROKE / 2) * scale
    draw.ellipse([cx - outer, cy - outer, cx + outer, cy + outer], fill=FOREGROUND)
    draw.ellipse([cx - inner, cy - inner, cx + inner, cy + inner], fill=background)

    half = BAR_STROKE / 2 * scale
    for x, y_top, y_bottom in BARS:
        bx, top, bottom = px(x), px(y_top), px(y_bottom)
        draw.rounded_rectangle(
            [bx - half, top - half, bx + half, bottom + half],
            radius=half,
            fill=FOREGROUND,
        )

    return image.resize((OUT, OUT), Image.LANCZOS)


if __name__ == "__main__":
    import json
    import os
    import sys

    catalog = sys.argv[1]
    for name, background in FLAVORS.items():
        folder = os.path.join(catalog, f"{name}.appiconset")
        os.makedirs(folder, exist_ok=True)
        render(background).save(os.path.join(folder, "icon-1024.png"))
        contents = {
            "images": [
                {
                    "filename": "icon-1024.png",
                    "idiom": "universal",
                    "platform": "ios",
                    "size": "1024x1024",
                }
            ],
            "info": {"author": "xcode", "version": 1},
        }
        with open(os.path.join(folder, "Contents.json"), "w") as handle:
            json.dump(contents, handle, indent=2)
        print(f"wrote {folder}")

    with open(os.path.join(catalog, "Contents.json"), "w") as handle:
        json.dump({"info": {"author": "xcode", "version": 1}}, handle, indent=2)

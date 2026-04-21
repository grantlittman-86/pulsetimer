"""
Render the Google Play Store feature graphic for PulseTimer.

Spec (per Play Console):
    - 1024 x 500 px
    - 24-bit PNG (no alpha) or JPEG
    - Max 1 MB
    - Important content kept within the center safe zone so cropping on
      some Play surfaces does not clip the layout.

Design:
    - Background: brand dark (#0E1415), same as the adaptive icon background.
    - A faint horizontal pulse waveform in brand orange stretches the full
      width, echoing the heartbeat motif inside the app icon. It sits behind
      the main content and is intentionally low-contrast so it reads as
      texture, not a competing element.
    - The 512 px launcher icon is placed on the left, scaled down.
    - "PulseTimer" wordmark in brand cyan next to the icon.
    - Tagline in off-white below the wordmark.
"""

import os

from PIL import Image, ImageDraw, ImageFont

# --- Output config ---
OUT_W = 1024
OUT_H = 500
SUPERSAMPLE = 2  # render at 2x and downscale for cleaner edges
CANVAS_W = OUT_W * SUPERSAMPLE
CANVAS_H = OUT_H * SUPERSAMPLE

# --- Brand colors ---
BG = (0x0E, 0x14, 0x15)
CYAN = (0x4F, 0xC3, 0xF7)
ORANGE = (0xFF, 0xB7, 0x4D)
OFF_WHITE = (0xEC, 0xEF, 0xF1)

# --- Copy ---
WORDMARK = "PulseTimer"
TAGLINE = "Stay on pace, without watching the clock"

# --- Fonts (macOS system fonts, reliable on Grant's Mac mini) ---
FONT_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
FONT_REGULAR = "/System/Library/Fonts/Supplemental/Arial.ttf"

# --- Paths ---
HERE = os.path.dirname(os.path.abspath(__file__))
ICON_PATH = os.path.join(HERE, "play-store-icon-512.png")
OUT_PATH = os.path.join(HERE, "play-store-feature-graphic-1024x500.png")


def draw_background_waveform(draw: ImageDraw.ImageDraw) -> None:
    """Draw a faint heart-monitor-style waveform in the lower band of the
    canvas so it reads as decorative texture without colliding with the
    wordmark or tagline."""
    # Position the waveform in the lower third so it does not cut through text.
    baseline = int(CANVAS_H * 0.83)
    # Amplitude is modest, roughly 8% of canvas height, to keep the motif calm.
    amp = int(CANVAS_H * 0.08)

    # Points are (x_fraction_of_width, y_offset_fraction_of_amp) forming one
    # heart-monitor beat: flat, small dip, big spike up, deeper spike down,
    # spike up, small dip, flat. Mirrors the pulse motif inside the app icon.
    motif = [
        (0.00, 0.0), (0.04, 0.0), (0.05, -0.5), (0.06, 1.0),
        (0.07, -1.5), (0.08, 1.0), (0.09, -0.5), (0.10, 0.0), (0.14, 0.0),
    ]

    points = []
    x_cursor = 0.0
    beat_spacing = 0.22  # roughly one beat every 22% of canvas width
    while x_cursor < 1.05:
        for fx, fy in motif:
            px = int((x_cursor + fx) * CANVAS_W)
            py = baseline + int(fy * amp)
            points.append((px, py))
        x_cursor += beat_spacing
        points.append((int(x_cursor * CANVAS_W), baseline))

    # Clip overshoot on the right edge.
    points = [(min(x, CANVAS_W), y) for x, y in points]

    # Very low-contrast orange so the motif does not compete with text.
    def blend(a, b, t):
        return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))

    faint = blend(BG, ORANGE, 0.16)
    stroke_w = max(2, int(CANVAS_H * 0.007))
    draw.line(points, fill=faint, width=stroke_w, joint="curve")


def compose() -> Image.Image:
    img = Image.new("RGB", (CANVAS_W, CANVAS_H), BG)
    draw = ImageDraw.Draw(img)

    # 1) Background waveform first, so icon and text render on top of it.
    draw_background_waveform(draw)

    # 2) Icon on the left, vertically centered. Sized and positioned so both
    #    the icon and the text block stay inside Play's center safe zone.
    icon = Image.open(ICON_PATH).convert("RGBA")
    icon_target = int(CANVAS_H * 0.56)  # 56% of banner height, ~280 px output
    icon = icon.resize((icon_target, icon_target), Image.LANCZOS)
    icon_x = int(CANVAS_W * 0.095)      # ~97 px at output resolution
    icon_y = (CANVAS_H - icon_target) // 2
    img.paste(icon, (icon_x, icon_y), icon)

    # 3) Text block to the right of the icon. Tagline is split across two
    #    lines so it fits within the safe zone without shrinking below a
    #    comfortable reading size.
    text_x = icon_x + icon_target + int(CANVAS_W * 0.03)

    wordmark_size = int(CANVAS_H * 0.18)   # ~90 px at output resolution
    tagline_size = int(CANVAS_H * 0.068)   # ~34 px at output resolution

    wordmark_font = ImageFont.truetype(FONT_BOLD, wordmark_size)
    tagline_font = ImageFont.truetype(FONT_REGULAR, tagline_size)

    tagline_lines = ["Stay on pace,", "without watching the clock"]

    # Measure heights so we can vertically center the block.
    wm_bbox = draw.textbbox((0, 0), WORDMARK, font=wordmark_font)
    tg_bboxes = [draw.textbbox((0, 0), line, font=tagline_font) for line in tagline_lines]
    wm_h = wm_bbox[3] - wm_bbox[1]
    tg_line_h = max(b[3] - b[1] for b in tg_bboxes)
    tg_leading = int(tg_line_h * 1.25)
    gap = int(CANVAS_H * 0.035)
    block_h = wm_h + gap + tg_line_h + tg_leading * (len(tagline_lines) - 1)
    text_top = (CANVAS_H - block_h) // 2

    # Pillow's text origin is the bbox top-left; we subtract the bbox top
    # offset so glyph tops align visually.
    draw.text(
        (text_x, text_top - wm_bbox[1]),
        WORDMARK,
        font=wordmark_font,
        fill=CYAN,
    )
    tagline_top = text_top + wm_h + gap
    for i, line in enumerate(tagline_lines):
        bb = tg_bboxes[i]
        draw.text(
            (text_x, tagline_top + i * tg_leading - bb[1]),
            line,
            font=tagline_font,
            fill=OFF_WHITE,
        )

    # 4) Downscale for anti-aliased edges.
    return img.resize((OUT_W, OUT_H), Image.LANCZOS)


if __name__ == "__main__":
    out = compose()
    # Play requires 24-bit PNG (no alpha). RGB mode accomplishes this.
    out.save(OUT_PATH, "PNG", optimize=True)
    size = os.path.getsize(OUT_PATH)
    print(f"Wrote {OUT_PATH} ({size} bytes, {out.size[0]}x{out.size[1]})")

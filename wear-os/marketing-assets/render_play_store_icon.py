"""
Render the PulseTimer launcher icon as a 512x512 PNG for the Google Play Store listing.

The adaptive icon foreground lives at
    wear-os/app/src/main/res/drawable/ic_launcher_foreground.xml
(108x108 viewport) and is composed onto the background color defined by
    wear-os/app/src/main/res/drawable/ic_launcher_background.xml (#0E1415).

Adaptive icons reserve a central 72x72 dp "safe zone" from the 108x108 canvas.
The launcher masks the outer ring, so the on-device visible content corresponds
roughly to the safe zone. To make the Play Store icon match the visual scale of
the on-device icon, we crop to the 72-unit safe zone and render at 512x512.

Anti-aliasing is achieved by rendering at a 4x supersample and downscaling with
LANCZOS, since Pillow's native drawing does not anti-alias by default.
"""

from PIL import Image, ImageDraw

# --- Configuration ---
OUT_SIZE = 512            # Google Play requires 512x512
SUPERSAMPLE = 4           # render at 4x then downscale for AA
CANVAS_SIZE = OUT_SIZE * SUPERSAMPLE

VIEWPORT = 108            # adaptive icon logical size
SAFE_ZONE = 72            # central safe zone
SAFE_OFFSET = (VIEWPORT - SAFE_ZONE) / 2  # 18; safe zone spans [18, 90]

# Colors (from ic_launcher_background.xml and ic_launcher_foreground.xml)
BG = (0x0E, 0x14, 0x15, 255)
CYAN = (0x4F, 0xC3, 0xF7, 255)
ORANGE = (0xFF, 0xB7, 0x4D, 255)

SCALE = CANVAS_SIZE / SAFE_ZONE  # vector units -> pixels

def vx(x):
    """Transform a vector x/y coord (0..108) to a pixel coord on the supersampled canvas."""
    return (x - SAFE_OFFSET) * SCALE

def px_len(v):
    """Transform a vector length to a pixel length."""
    return v * SCALE


def render():
    img = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), BG)
    draw = ImageDraw.Draw(img)

    cx, cy = vx(54), vx(54)

    # --- Timer ring: cyan annulus, outer radius 28, inner radius 24 ---
    r_outer = px_len(28)
    r_inner = px_len(24)
    draw.ellipse([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], fill=CYAN)
    # Punch the inner disc back to the background color to form the ring.
    draw.ellipse([cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner], fill=BG)

    # --- Tick marks (rectangles) ---
    ticks = [
        (52.5, 28.0, 55.5, 34.0),   # 12 o'clock
        (74.0, 52.5, 80.0, 55.5),   # 3 o'clock
        (52.5, 74.0, 55.5, 80.0),   # 6 o'clock
        (28.0, 52.5, 34.0, 55.5),   # 9 o'clock
    ]
    for x1, y1, x2, y2 in ticks:
        draw.rectangle([vx(x1), vx(y1), vx(x2), vx(y2)], fill=CYAN)

    # --- Pulse / heartbeat polyline ---
    pulse_vec = [
        (34, 54), (44, 54), (47, 46), (50, 62), (53, 42),
        (56, 62), (59, 46), (62, 54), (74, 54),
    ]
    pulse_px = [(vx(x), vx(y)) for x, y in pulse_vec]
    stroke_px = int(round(px_len(2.5)))
    # Draw the polyline. joint='curve' smooths interior joints.
    draw.line(pulse_px, fill=ORANGE, width=stroke_px, joint="curve")
    # Add round end-caps (and reinforced joints) by stamping a disc at each vertex.
    cap_r = stroke_px / 2
    for x, y in pulse_px:
        draw.ellipse([x - cap_r, y - cap_r, x + cap_r, y + cap_r], fill=ORANGE)

    # --- Center dot ---
    dot_r = px_len(2.5)
    draw.ellipse([cx - dot_r, cy - dot_r, cx + dot_r, cy + dot_r], fill=ORANGE)

    # --- Downscale for anti-aliasing ---
    final = img.resize((OUT_SIZE, OUT_SIZE), Image.LANCZOS)
    return final


if __name__ == "__main__":
    import os
    out_path = os.path.join(os.path.dirname(__file__), "play-store-icon-512.png")
    render().save(out_path, "PNG", optimize=True)
    size = os.path.getsize(out_path)
    print(f"Wrote {out_path} ({size} bytes)")

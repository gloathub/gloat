#!/usr/bin/env python3

"""Generate the social-card.png for the GloatHub website.

Reads the subtitle from mkdocs.yaml's site_description so the card
stays in sync with the site metadata.  Run from the www/ directory
or pass the www/ path as the first argument.
"""

import math
import os
import sys

import yaml
import yaml.constructor
from PIL import Image, ImageDraw, ImageFont


# mkdocs.yaml uses !!python/name tags that safe_load rejects.
# Register a handler that returns None for those tags so we can
# still read the top-level keys we care about.
class _MkDocsLoader(yaml.SafeLoader):
    pass


_MkDocsLoader.add_multi_constructor(
    'tag:yaml.org,2002:python/',
    lambda loader, suffix, node: None,
)

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
www_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(
    os.path.dirname(os.path.abspath(__file__)))

mkdocs_path = os.path.join(www_dir, 'mkdocs.yaml')
goat_path = os.path.join(www_dir, 'docs', 'img', 'gloat.jpeg')
out_path = os.path.join(www_dir, 'docs', 'img', 'social-card.png')

# ---------------------------------------------------------------------------
# Read subtitle from mkdocs.yaml
# ---------------------------------------------------------------------------
with open(mkdocs_path) as f:
    mkdocs = yaml.load(f, Loader=_MkDocsLoader)

subtitle = mkdocs['site_description']

# ---------------------------------------------------------------------------
# Canvas and teal gradient background
# ---------------------------------------------------------------------------
WIDTH, HEIGHT = 1200, 630
img = Image.new('RGB', (WIDTH, HEIGHT), '#004D40')
draw = ImageDraw.Draw(img)

for y in range(HEIGHT):
    r = 0
    g = int(50 + (137 - 50) * y / HEIGHT)
    b = int(40 + (123 - 40) * y / HEIGHT)
    draw.line([(0, y), (WIDTH, y)], fill=(r, g, b))

# Subtle radial glow in center
cx, cy = WIDTH // 2, 280
for y in range(HEIGHT):
    for x in range(0, WIDTH, 2):
        dist = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
        if dist < 350:
            factor = (1.0 - dist / 350) * 0.12
            px = img.getpixel((x, y))
            nr = min(255, int(px[0] + 80 * factor))
            ng = min(255, int(px[1] + 180 * factor))
            nb = min(255, int(px[2] + 160 * factor))
            img.putpixel((x, y), (nr, ng, nb))
            if x + 1 < WIDTH:
                img.putpixel((x + 1, y), (nr, ng, nb))

draw = ImageDraw.Draw(img)

# ---------------------------------------------------------------------------
# Goat mascot (circular portrait with teal glow ring)
# ---------------------------------------------------------------------------
goat = Image.open(goat_path)
goat_size = 340
goat = goat.resize((goat_size, goat_size), Image.LANCZOS)

mask = Image.new('L', (goat_size, goat_size), 0)
mask_draw = ImageDraw.Draw(mask)
mask_draw.ellipse([0, 0, goat_size - 1, goat_size - 1], fill=255)

goat_x = (WIDTH - goat_size) // 2
goat_y = 60
ring_padding = 10

# Teal/cyan glow rings
for i in range(25, 0, -1):
    c = (0, min(255, 180 + i * 3), min(255, 200 + i * 2))
    draw.ellipse([
        goat_x - ring_padding - i, goat_y - ring_padding - i,
        goat_x + goat_size + ring_padding + i,
        goat_y + goat_size + ring_padding + i,
    ], outline=c, width=1)

# White circle background
draw.ellipse([
    goat_x - ring_padding, goat_y - ring_padding,
    goat_x + goat_size + ring_padding,
    goat_y + goat_size + ring_padding,
], fill=(255, 255, 255))

img.paste(goat, (goat_x, goat_y), mask)

# ---------------------------------------------------------------------------
# Text
# ---------------------------------------------------------------------------
bold_font = ImageFont.truetype(
    '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 60)
regular_font = ImageFont.truetype(
    '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 26)

# Title
title = 'GloatHub'
title_bbox = draw.textbbox((0, 0), title, font=bold_font)
title_w = title_bbox[2] - title_bbox[0]
title_x = (WIDTH - title_w) // 2
title_y = 430
draw.text((title_x + 2, title_y + 2), title, fill=(0, 30, 25),
          font=bold_font)
draw.text((title_x, title_y), title, fill=(255, 255, 255), font=bold_font)

# Subtitle (from mkdocs.yaml)
sub_bbox = draw.textbbox((0, 0), subtitle, font=regular_font)
sub_w = sub_bbox[2] - sub_bbox[0]
sub_x = (WIDTH - sub_w) // 2
sub_y = 510
draw.text((sub_x + 1, sub_y + 1), subtitle, fill=(0, 30, 25),
          font=regular_font)
draw.text((sub_x, sub_y), subtitle, fill=(178, 223, 219),
          font=regular_font)

# Bottom accent line
line_y = 590
line_w = 200
draw.line([(WIDTH // 2 - line_w, line_y), (WIDTH // 2 + line_w, line_y)],
          fill=(0, 188, 212), width=3)

# ---------------------------------------------------------------------------
# Save
# ---------------------------------------------------------------------------
img.save(out_path, 'PNG')
print(f'Generated {out_path}')

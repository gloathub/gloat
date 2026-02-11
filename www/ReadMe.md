# GloatHub Website

This directory contains the source for [gloathub.org](https://gloathub.org), the
official website for the Gloat project.

## Built With

- **MkDocs** - Static site generator
- **Material for MkDocs** - Theme with modern design
- **Custom CSS** - Green/Go-inspired color palette

## Local Development

### Setup and Serve

```bash
cd www
make serve
```

This will:
1. Auto-install Python via Makes (if needed)
2. Create a Python virtual environment
3. Install dependencies (MkDocs and Material theme)
4. Start the development server at http://localhost:8000

No prerequisites needed - Makes handles all dependencies automatically!

Changes to markdown or configuration files will automatically reload.

## Building

To build the static site:

```bash
make site
```

Output will be in the `site/` directory.

## Make Shell

To access the installed tools (mkdocs, typos, etc.) directly:

```bash
make shell
```

This starts a subshell with all Makes-installed dependencies in your PATH.
Press Ctrl-D or type `exit` to return to your normal shell.

## Publishing

To publish to GitHub Pages:

```bash
make publish
```

This builds the site and pushes to the `gh-pages` branch.

## Site Structure

```
www/
├── docs/               # Content pages
│   ├── index.md        # Homepage
│   ├── demo.md         # Live demo page
│   ├── getting-started.md  # Installation guide
│   ├── examples.md     # Example programs showcase
│   ├── CNAME           # Custom domain (gloathub.org)
│   └── css/
│       └── extra.css   # Custom styling
├── theme/              # Theme customizations
│   └── partials/
│       ├── header.html # Clickable site title
│       └── logo.html   # Custom logo
├── mkdocs.yaml         # MkDocs configuration
├── requirements.txt    # Python dependencies
└── Makefile            # Build automation
```

## Theme

The site uses a custom green/Go-inspired color palette:

- Primary: Teal (#00897B)
- Accent: Cyan (#00BCD4)
- Supports light/dark mode toggle

## License

Copyright 2026 - Ingy dot Net

MIT License

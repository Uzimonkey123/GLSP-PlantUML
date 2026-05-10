# PlantUML Diagram Editor

Edit PlantUML class and sequence diagrams visually — right inside VS Code. 

> Open a .puml file → Click "Open PlantUML Preview" → Start editing

This extension gives you a split-view workflow: write PlantUML notation in the text editor on one side, and see a live, interactive diagram on the other. Changes flow both ways, edit the text, save, and the diagram updates, edit the diagram, save, and the text updates.

Built on the [Graphical Language Server Platform (GLSP)](https://www.eclipse.org/glsp/).

---

## Features

**Visual diagram editing** — Select, move, rename, and delete elements directly on the rendered diagram. Double-click any label to edit it inline (use `Shift+Enter` for multi-line editing where supported).

**Bidirectional synchronization** — The `.puml` source file and the diagram stay in sync. Save the file to push text changes into the diagram; edits made on the diagram are written back to the source.

**Real-time diagnostics** — An integrated LSP server highlights syntax errors as you type and offers code completions for PlantUML keywords, arrow types, and existing entity names.

**SVG export** — Export your diagram to `.svg` from the tool palette.

**Plugin architecture** — Drop JAR files into the `plugins/` directory to add custom validation rules or entirely new diagram modules.

### Supported diagram types

- **Class diagrams** — entities, attributes, methods, relationships, labels, layout, notes
- **Sequence diagrams** — participants, messages, labels, groups, notes

---

## Requirements

| Dependency         | Version                                 |
|--------------------|-----------------------------------------|
| **Java JDK**       | 21 or later                             |
| **GraphViz (dot)** | 2.43 (must be available in system PATH) |

---

## Getting started

1. Install the extension from the Marketplace.
2. Open any `.puml` file.
3. Click **"Open PlantUML Preview"** in the editor toolbar.
4. The diagram panel opens to the right — start editing.

---

## Usage tips

- **Rename elements** — double-click a label on the diagram and type a new name.
- **Delete elements** — select an element and use the delete tool in the tool palette.
- **Multi-line labels** — press `Shift+Enter` while editing a label to insert a line break.

---

## Extending with plugins

### Custom validation rules

Implement the `ValidationRule` interface, package as a JAR, and place it in the `plugins/` directory. The rule will be picked up on the next server startup.

### Custom diagram modules

Support for new diagram types requires both extension-side parsing and client-side rendering implementation.
Extend `DiagramModule` from GLSP, and drop the JAR into your `<user>/.glsp-plantuml/plugins` directory.

---

## Known limitations

- Only class and sequence diagrams are supported out of the box.
- The diagram preview requires Java 21+ to be available on `PATH` or configured via `java.home`.
- Switching between diagram types requires reopening the editor session to reload the appropriate server module.
- PlantUML skinparam and CSS styling is not supported.

---

## Base troubleshooting

### Graphviz not found (dot command fails)

**Cause:**  
Graphviz is not installed or `dot` is not available in system PATH.

**Fix:**
- Install Graphviz: https://graphviz.org/download/
- Verify installation:
```bash
dot -V
```

### Incompatible Java version
The tool supports from Java 21+, while if you are running the tests, only up to Java 22, due to Mockito.

### GLSP server does not start / empty preview
Ensure port 5007 is free to use.

---

## License

GPL-2.0-only WITH Classpath-exception-2.0
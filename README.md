# PlantUML GLSP Diagram Editor

A VS Code extension for visually editing PlantUML class and sequence diagrams using the 
[Graphical Language Server Platform (GLSP)](https://www.eclipse.org/glsp/). 
The extension provides a split-view workflow:edit PlantUML notation in the text editor on the left, 
and see/manipulate the rendered diagram on the right.

The project consists of a **Java GLSP backend** that parses PlantUML files and serves the graphical model,
and a **TypeScript frontend** built on Sprotty that renders and interacts with diagrams inside VS Code.
An integrated **LSP server** provides real-time syntax diagnostics and code completions in the text editor.

---

## Features

- Visual editing and deletion of **class diagrams** entities, labels and their positions
- Visual editing and deletion of **sequence diagrams** labels, messages
- Real-time **syntax validation** with error highlighting in the text editor
- **Code completions** for PlantUML keywords, arrows, and existing entity names
- **Bidirectional sync** — save the text file to update the diagram
- **SVG export** of diagrams
- **Plugin architecture** — extend with custom diagram types and validation rules via JAR plugins

---

## Prerequisites

| Tool | Version |
|------|---------|
| **Java JDK** | 21+     |
| **Maven** | 3.8+    |
| **Node.js** | 22.15   |
| **npm** | 9+      |
| **VS Code** | 1.101   |

---

## Getting Started

### Installing as VSIX package

Download the `.vsix` file from the latest release. 
In VS Code, open the Command Palette (Ctrl+Shift+P), run `Extensions: Install from VSIX`, 
and select the downloaded file.

### Cloning the repository and VS Code debug mode usage

### 1. Clone the repository

```bash
git clone https://github.com/Uzimonkey123/GLSP-PlantUML
cd GLSPPlantUML
```

### 2. Install extension dependencies

```bash
cd plantuml-client
npm install
```

### 3. Build the Java server

This compiles the GLSP server, LSP server, parsers, and validators into a single fat JAR and copies it into the extension's `server/` directory.

(Needs to be in GLSPPlantUML directory)

**Windows:**
```cmd
./build.bat -p
```

**Linux/macOS:**
```bash
chmod +x build.sh
./build.sh -p
```

### 5. Run in VS Code

1. Open the **root `plantuml-client/` folder** in VS Code
2. Press **F5** to launch the Extension Development Host
3. In the new VS Code window, open any `.puml` file
4. Click on "Open PlantUML Preview" button to open diagram editor

---

## Usage

### Editing workflow

1. Write PlantUML notation in the text editor
2. Press "Open PlantUML Preview" to open the diagram editor on the right as a panel
3. Use the **tool palette** in the diagram editor to remove elements or export diagram as `.svg` file
4. To edit labels, names of elements, double-click on the text. Adding more lines into the text is possible by pressing `SHIFT+ENTER` in the text editing area. Some elements do not support multi-line editing.
5. Changes made in the diagram editor are written back to the `.puml` file after saving

## Plugin System

The extension supports a plugin architecture for adding custom **diagram modules** and **validation rules**.

### Adding a validation rule

1. Implement the `ValidationRule` interface:

```java
public class MyRule implements ValidationRule {
    @Override
    public String getDiagramType() { return "class-diagram"; }
 
    @Override
    public ErrorRecord validate(String fileText) {
        // Your validation logic
        return new ErrorRecord(false, null, -1, -1, -1);
    }
}
```

2. Register it via `META-INF/services/com.GLSPPlantUML.validators.ValidationRule`
3. Place the JAR in the `plugins/` directory

### Adding a diagram module

1. Extend `DiagramModule` from GLSP
2. Register via `META-INF/services/org.eclipse.glsp.server.di.DiagramModule`
3. Place the JAR in the `plugins/` directory

Plugins are loaded automatically on server startup via `ServiceLoader`.
 
---

## License

This project is licensed under the GNU General Public License v2.0 with the Classpath Exception.
See the [LICENSE](LICENSE) file for details.

SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
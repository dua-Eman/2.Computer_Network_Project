# 2.Computer_Network_Project
This project simulates basic network routing algorithms using Java in IntelliJ. It allows users to visualize network nodes, paths, and the routing process between different nodes. The goal is to understand how routing is done using routing algorithms such as BFS algorithm.


# BFS Visualizer – Graph simulator in Java (Swing)

**BFS Simulator** a interactive Java GUI application that simulate BFS (Breadth-First Search) algorithm graphically using `Swing` and `AWT`. From this toolyou can create, visualize, aur manipulate directed/undirected graphs.

---

## Features

- Add/Delete **nodes** by clicking
- Add/Delete **edges** with weights
- Supports **directed** and **undirected** graphs
- Visual **BFS traversal** from source to destination
- Display **visited nodes**, **paths**, and **messages**
- Toggle between **Paper View** and **World Map View**
- **Undo delete** operation
- Scrollable **message panel** showing traversal logs


---

## Folder Structure

Structure the project as follows:

```
BfsVisualizerX/
│
├── src/
│   ├── BFS_GUI.java         // Main GUI file
│   ├── GraphCanvas.java          // Graph drawing and logic
│   ├── Node.java                 // Node class
│   ├── Edge.java                 // Edge class
│   └── Messageconsole.java        // Log display panel
│
├── resources/
│   ├── WORLD.jpg                 // Background image (World map)
│   └── paper_view.jpg           // Background image (Paper look)
│
└── README.md                     // You're reading it!
```

---

## Add Background Images (IMPORTANT)

Make sure to **place the images** correctly:

1. Put `WORLD.jpg` and `paper_view.jpg` in a `resources/` folder.
2. These images **must be accessible via classpath**, or `getResource("/WORLD.jpg")` will return `null`.

If running from IDE (IntelliJ), ensure:

- `resources` folder is **marked as Resources Root** or is on **build path**.
- Alternatively, **copy images into `src/`** for ease.

---

## External Dependencies

None! This app only uses standard Java libraries:
- `javax.swing.*`
- `java.awt.*`
- `java.util.*`
- `java.util.concurrent.CopyOnWriteArrayList`

---

## How to Compile and Run

### Step 1: Compile all files

If you are using **command line**:

 compile in your IDE (select all files and Run).

### Step 2: Run the app

in your IDE, right-click `BFS_GUI.java` → `Run`.

---

## How to Use the App

### Adding Nodes:
- **Click** anywhere on canvas to add a node.
- Nodes are auto-named: A, B, C...

### Connecting Edges:
- **Click two nodes** one after another.
- Enter the **edge weight** (must be positive).
- Self-loops are **not allowed**.

### Directed vs Undirected:
- Check/Uncheck **"Directed Graph"** checkbox.

### BFS Simulation:
- Enter **source** and **destination** node names (e.g., A, B).
- Click **"Start BFS"** to begin traversal.
- Watch animation + logs in the right panel.

### Deleting:
- Click **"Delete Node"** → enter node name.
- Click **"Delete Edge"** → enter edge like `A to B`.

### Undo:
- Click **"Undo"** to restore last deleted node or edge.

### BackgrounD Change:
- Click **"Paper View"** to switch between themes.

---

## Message Panel Details

The right-side panel logs:
- Visited nodes
- Enqueued nodes
- Final BFS path

You canScroll it. Messages are color-coded and stylized for clarity.

---

## Made by Dua Eman Rabia ejaz, Anila khan


# MultiplayerTemplate

# Multiplayer Game Template (LibGDX + KryoNet)

A reusable **multiplayer game template** built with [LibGDX](https://libgdx.com/) and [KryoNet](https://github.com/EsotericSoftware/kryonet).  
This project provides a simple but expandable **client-server networking system** for real-time games.

---

## ✨ Features
- **Server-Client Architecture** with TCP (reliable) and UDP (fast, real-time) channels.
- **Player Management System**:
  - Register player names
  - Track joins and disconnects
  - Maintain synchronized world state
- **Reusable `ClientManager`** for handling connections, events, and player data.
- **Cross-platform** (Desktop via LWJGL3, Android, iOS, HTML5).
- Easy to **expand with new packet types** (chat, inventory, actions, etc.).

---

## 📂 Project Structure
net.alex.game/\
├── network/\
│ └── Network.java # Defines all packets & registers them\
│\
├── ServerMain.java # Dedicated game server\
├── ClientManager.java # Handles client networking logic\
├── FirstScreen.java # Simple join screen (UI example)\
├── MainGame.java # Main LibGDX game class\
│\
└── lwjgl3/\
└── Lwjgl3Launcher.java # Desktop launcher\

yaml
Copy code

---

⚡ Requirements
Java 8+

Gradle

LibGDX

KryoNet

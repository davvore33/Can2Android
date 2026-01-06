# CAN Bus Plotter — Proof of Concept

## Overview

The **CAN Bus Plotter** is an Android application designed to monitor, decode, and visualize CAN bus signals in real-time. This repository contains a Proof of Concept (PoC) implementation that validates the technical feasibility and architectural approach.

**Purpose:** Validate that CAN signal monitoring is achievable on Android with a modular, future-proof architecture that supports multiple data sources (files, USB adapters, remote streams).

**Status:** 🚧 Proof of Concept

---

## PoC Objectives

The PoC aims to validate:

✅ **Technical Feasibility** - CAN signal decoding and visualization on Android  
✅ **Architecture** - Modular design supporting multiple data sources  
✅ **Real-Time Performance** - Signal processing and visualization without lag  
✅ **Extensibility** - Core logic independent of data source type  
✅ **Hardware Viability** - Assessment of live CAN acquisition capabilities  

**PoC Success Criteria:** See [requirements.md](docs/requirements.md)

---

## Features (PoC Scope)

### Implemented ✅
- **ASC File Ingestion** - Stream-based processing of CAN data files
- **DBC-Based Decoding** - Dynamic signal extraction from raw CAN frames
- **Real-Time Visualization** - Signal list view and basic line charts
- **Modular Architecture** - Clean separation between data sources, decoding, and visualization
- **Lifecycle Management** - Proper startup/shutdown and error handling

### Planned for Production 🔮
- USB CAN adapter support (live data acquisition)
- Remote gRPC streams with pre-decoded signals
- Advanced visualizations (gauges, dashboards)
- Data export and persistence
- Performance optimizations

---

## Architecture

The system uses a **layered, stream-based architecture** with clear separation of concerns:

```
┌─────────────────┐
│  Data Sources   │  ASC Files (PoC) | USB CAN (Future) | gRPC (Future)
└────────┬────────┘
         │
         v
┌─────────────────┐
│ RawFrameProvider│  Interface for raw CAN data acquisition
└────────┬────────┘
         │
         v
┌─────────────────┐
│ SignalDecoder   │  Optional: DBC-based signal decoding
└────────┬────────┘  (Bypassed for pre-decoded sources)
         │
         v
┌─────────────────┐
│ SignalProvider  │  Interface for signal samples
└────────┬────────┘
         │
         v
┌─────────────────┐
│SignalStreamCore │  Signal buffering and time-windowing
└────────┬────────┘
         │
         v
┌─────────────────┐
│  Visualization  │  UI: Signal lists and charts
└─────────────────┘
```

**Key Principles:**
- **Decoder Optionality** - Decoding layer can be bypassed for pre-decoded data
- **Interface-Driven** - Core logic depends on abstractions, not implementations
- **Stream-Based** - Flow-based processing with backpressure support
- **Lifecycle Aware** - Clean startup/shutdown and error isolation

---

## Documentation

### Core Documentation
- **[requirements.md](docs/requirements.md)** - PoC requirements and success criteria
- **[architecture.md](docs/architecture.md)** - Complete architecture definition
- **[architecture-validation.md](docs/architecture-validation.md)** - Validation against 10 future scenarios
- **[lifecycle-execution-flow.md](docs/lifecycle-execution-flow.md)** - Component lifecycle and execution flow
- **[decoder-optionality.md](docs/decoder-optionality.md)** - How the decoder layer can be bypassed
- **[hardware-feasibility.md](docs/hardware-feasibility.md)** - Analysis of USB CAN adapter support on Android

### Quick Links
- [PoC Success Criteria](docs/requirements.md#3-success-criteria-poc-completion)
- [Architecture Validation Summary](docs/architecture-validation.md#8-conclusion)
- [Lifecycle Management](docs/lifecycle-execution-flow.md#7-lifecycle-coordination)
- [Future Scenarios](docs/architecture-validation.md#2-future-scenarios)

---

## Project Structure

```
Can2Android/
├── app/
│   └── src/
│       └── main/
│           └── java/
│               └── com/
│                   └── can2android/
│                       └── domain/          # Core domain layer
│                           ├── RawCanFrame.kt
│                           ├── RawFrameProvider.kt
│                           ├── SignalSample.kt
│                           ├── SignalDecoder.kt
│                           ├── SignalProvider.kt
│                           ├── DecodingSignalProvider.kt
│                           ├── DirectSignalProvider.kt
│                           ├── SignalBuffer.kt
│                           ├── SignalStreamCore.kt
│                           ├── SignalUpdate.kt
│                           ├── SystemLifecycleManager.kt
│                           ├── DataSourceState.kt
│                           ├── SignalProviderState.kt
│                           ├── SignalStreamCoreState.kt
│                           └── DataSourceException.kt
└── docs/                                    # Architecture documentation
    ├── requirements.md
    ├── architecture.md
    ├── architecture-validation.md
    ├── lifecycle-execution-flow.md
    ├── decoder-optionality.md
    └── hardware-feasibility.md
```

---

## Key Components

### Domain Layer

**Data Flow:**
- `RawCanFrame` - Raw CAN message with timestamp, ID, DLC, payload
- `SignalSample` - Decoded signal with name, physical value, unit, timestamp
- `SignalUpdate` - Signal update event with latest sample and time-series

**Providers:**
- `RawFrameProvider` - Interface for raw CAN data sources (ASC files, USB, etc.)
- `SignalProvider` - Interface for signal samples (decoded or pre-decoded)
- `DecodingSignalProvider` - Combines RawFrameProvider + SignalDecoder
- `DirectSignalProvider` - For pre-decoded signal sources (gRPC)

**Core Processing:**
- `SignalDecoder` - Interface for CAN frame → signal transformation
- `SignalBuffer` - Time-windowed signal history
- `SignalStreamCore` - Central signal processing and distribution hub

**Lifecycle:**
- `SystemLifecycleManager` - Coordinates startup/shutdown of all components
- State tracking for all major components (IDLE → STARTING → ACTIVE → STOPPING → STOPPED)

---

## Technology Stack

- **Language:** Kotlin
- **Platform:** Android (API level TBD)
- **Concurrency:** Kotlin Coroutines + Flow
- **Architecture:** Clean Architecture with domain-driven design
- **Testing:** JUnit, Mockito (planned)

---

## Getting Started

### Prerequisites
- Android Studio
- Kotlin 1.9+
- Android SDK

### Build and Run
```bash
# Clone the repository
git clone https://github.com/davvore33/Can2Android.git
cd Can2Android

# Open in Android Studio
# Build and run on device/emulator
```

### Test with Sample Data
1. Prepare an ASC file with CAN data
2. Prepare a DBC file with signal definitions
3. Load files in the application
4. View real-time signal decoding and visualization

---

## Architecture Highlights

### ✅ Validated Against 10 Future Scenarios

The architecture has been validated against:
1. USB CAN Adapter (live data)
2. Remote gRPC Stream (pre-decoded)
3. Multiple Simultaneous Sources
4. Signal Recording and Replay
5. Multiple DBC File Support
6. Cloud-Based Processing
7. Signal Filtering and Transformation
8. Persistent Signal History
9. Multiple Visualization Types
10. CAN Frame Injection for Testing

**Result:** 100% of scenarios supported without core modifications. See [architecture-validation.md](docs/architecture-validation.md).

### ✅ SOLID Principles Maintained

- **Single Responsibility** - Each component has one well-defined purpose
- **Open/Closed** - Open for extension, closed for modification
- **Liskov Substitution** - All implementations are substitutable
- **Interface Segregation** - Minimal, focused interfaces
- **Dependency Inversion** - High-level modules depend on abstractions

### ✅ Clean Lifecycle Management

- Bottom-up startup: RawFrameProvider → SignalProvider → SignalStreamCore
- Top-down shutdown: SignalStreamCore → SignalProvider → RawFrameProvider
- Error isolation: Component failures don't cascade
- Idempotent operations: Safe to call start/stop multiple times

---

## PoC Deliverables

### Completed ✅
- ✅ Working domain layer with all core components
- ✅ Stream-based data processing architecture
- ✅ Interface-driven design for extensibility
- ✅ Lifecycle management with state tracking
- ✅ Comprehensive architecture documentation
- ✅ Validation against future scenarios
- ✅ Hardware feasibility analysis

### In Progress 🚧
- 🚧 ASC file reader implementation
- 🚧 DBC parser and signal decoder
- 🚧 Android UI (signal list and charts)
- 🚧 Integration and end-to-end testing

---

## Contributing

This is a Proof of Concept project. Contributions should focus on:
- Validating architectural decisions
- Identifying edge cases or limitations
- Improving documentation clarity
- Testing the extensibility claims

---

## License

[License TBD - Consider GPLv3 or LGPLv3]

**Third-Party Library Licenses:**
- **usb-serial-for-android:** MIT License ✅ GPL-compatible
- **Kotlin Coroutines:** Apache 2.0 ✅ GPLv3-compatible (but incompatible with GPLv2)
- **Android SDK:** Apache 2.0 ✅ GPLv3-compatible

**License Compatibility Analysis:**

✅ **MIT License (usb-serial-for-android):**
- Fully compatible with GPLv2, GPLv3, and LGPLv3
- Most permissive license - no compatibility issues

✅ **Apache 2.0 (Kotlin, Android SDK):**
- Compatible with GPLv3 and LGPLv3
- NOT compatible with GPLv2 (due to patent clause)
- If using GPLv3/LGPLv3: No issues

**Recommendation:**
If this project adopts GPL licensing, use **GPLv3 or LGPLv3** (not GPLv2) to ensure compatibility with all dependencies. The combined work would be distributed under your chosen GPL license, while the third-party libraries retain their original licenses (MIT, Apache 2.0).

---

## Acknowledgments

This PoC validates that Android can effectively monitor and visualize CAN bus signals with a clean, extensible architecture that supports multiple data sources without requiring core redesign.

---

## Contact

Repository: [davvore33/Can2Android](https://github.com/davvore33/Can2Android)

---

**Last Updated:** January 6, 2026  
**PoC Status:** Core architecture complete, UI implementation in progress

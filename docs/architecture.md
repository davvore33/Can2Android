# CAN Bus Plotter — Architecture Definition

## 1. Overview

This document defines the high-level architecture of the CAN Bus Plotter, establishing clear boundaries, responsibilities, and data flow between components. The architecture is designed to be modular and extensible, supporting both the current PoC scope and future enhancements.

---

## 2. Core Architectural Blocks

### 2.1 Data Sources
**Responsibility:** Provide raw CAN data from various sources

**PoC Implementation:**
- ASC File Reader (✓ PoC)

**Future Sources:**
- CAN-to-USB Live Acquisition (⏳ Future)
- Remote gRPC Stream (⏳ Future)

**Interface:** `RawDataSource`
```
Output: Stream<RawCanFrame>
where RawCanFrame = { timestamp, canId, dlc, payload }
```

**Constraints:**
- MUST NOT parse or decode data
- MUST NOT interpret signal semantics
- MUST provide only raw, timestamped CAN frames
- MUST operate as a stream (not batch)

---

### 2.2 Raw Frame Provider
**Responsibility:** Abstract interface for raw CAN data acquisition

**PoC Implementation:**
- Interface definition (✓ PoC)
- ASC File implementation (✓ PoC)

**Interface:**
```
interface RawFrameProvider {
    fun start()
    fun stop()
    fun observeFrames(): Flow<RawCanFrame>
}
```

**Constraints:**
- MUST be the only entry point for raw CAN data
- MUST emit frames in chronological order
- MUST support backpressure

**Dependencies:**
- None (this is the system entry point)

---

### 2.3 Parser / Decoder
**Responsibility:** Transform raw CAN frames into physical signals using DBC definitions

**PoC Implementation:**
- DBC file parser (✓ PoC)
- Signal decoder (✓ PoC)
- Frame-to-signal mapping (✓ PoC)

**Future Enhancements:**
- Advanced DBC features (⏳ Future)
- Multiple DBC support (⏳ Future)

**Interface:**
```
Input: RawCanFrame
DBC: Signal definitions (name, offset, length, scale, offset, unit)
Output: List<SignalSample>
where SignalSample = { signalName, physicalValue, unit, timestamp }
```

**Constraints:**
- MUST decode signals at runtime (not pre-process)
- MUST preserve timestamps
- MUST handle unknown CAN IDs gracefully
- MUST support stream-based operation

**Dependencies:**
- Depends on: Raw Frame Provider
- Optional: Can be bypassed for pre-decoded data sources (e.g., gRPC)

**Bypassability:**
- This block is **optional** when the data source provides already-decoded signals
- Future gRPC sources may emit SignalSample directly

> **See also:** [Decoder Abstraction and Optionality](decoder-optionality.md) for detailed implementation patterns and examples.

---

### 2.4 Signal Stream Core
**Responsibility:** Manage signal state, buffering, and time-windowing

**PoC Implementation:**
- In-memory signal buffer (✓ PoC)
- Basic time-window support (✓ PoC)
- Multi-signal state management (✓ PoC)

**Future Enhancements:**
- Advanced time-window strategies (⏳ Future)
- Persistence (⏳ Future)
- Aggregation/downsampling (⏳ Future)

**Interface:**
```
Input: SignalSample
State: Map<SignalName, TimeSeriesBuffer>
Output: Flow<SignalUpdate>
```

**Constraints:**
- MUST buffer decoded signals in memory
- MUST support real-time updates
- MUST support multiple signals simultaneously
- MUST operate on physical values only (not raw frames)

**Dependencies:**
- Depends on: Parser/Decoder OR direct decoded signal source
- Input must be SignalSample (physical values with timestamps)

---

### 2.5 Visualization
**Responsibility:** Display signal data to the user

**PoC Implementation:**
- Signal list view (✓ PoC)
- Real-time value display (✓ PoC)
- Basic line chart (✓ PoC)

**Future Enhancements:**
- Advanced charts (⏳ Future)
- Multi-signal plotting (⏳ Future)
- Custom dashboards (⏳ Future)

**Interface:**
```
Input: Flow<SignalUpdate> from Signal Stream Core
Output: UI rendering
```

**Constraints:**
- MUST update in real time
- MUST NOT block signal processing
- MUST handle rapid updates efficiently

**Dependencies:**
- Depends on: Signal Stream Core
- Must receive physical values (SignalSample), never raw frames

---

## 3. Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        DATA SOURCES                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  ASC File    │  │ USB CAN (F)  │  │  gRPC (F)    │       │
│  │    (PoC)     │  │              │  │              │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                 │               │
│         └─────────────────┴─────────────────┘               │
│                           │                                 │
└───────────────────────────┼─────────────────────────────────┘
                            │
                            ▼
            ┌───────────────────────────────┐
            │   Raw Frame Provider          │
            │   (Interface)                 │
            │                               │
            │   Output: RawCanFrame         │
            │   { timestamp, id, dlc,       │
            │     payload }                 │
            └───────────────┬───────────────┘
                            │
                            ▼
            ┌───────────────────────────────┐
            │   Parser / Decoder            │◄─── DBC Files
            │   (OPTIONAL - Bypassable)     │
            │                               │
            │   Input:  RawCanFrame         │
            │   Output: SignalSample        │
            └───────────────┬───────────────┘
                            │
                  ┌─────────┴─────────┐
                  │                   │
                  │  gRPC source      │
                  │  bypasses this    │
                  │  (already         │
                  │   decoded)        │
                  │                   │
                  └─────────┬─────────┘
                            │
                            ▼
            ┌───────────────────────────────┐
            │   Signal Stream Core          │
            │   (MANDATORY)                 │
            │                               │
            │   Input:  SignalSample        │
            │   State:  Time-series buffer  │
            │   Output: SignalUpdate        │
            └───────────────┬───────────────┘
                            │
                            ▼
            ┌───────────────────────────────┐
            │   Visualization               │
            │   (UI Layer)                  │
            │                               │
            │   Input:  SignalUpdate        │
            │   Output: Charts, lists       │
            └───────────────────────────────┘
```

**Legend:**
- **(PoC)** = Implemented in Proof of Concept
- **(F)** = Future implementation
- **OPTIONAL** = Can be bypassed depending on data source
- **MANDATORY** = Always present in the system

---

## 4. Dependency Rules

### 4.1 Allowed Dependencies

| Block                | Can Depend On            |
|----------------------|--------------------------|
| Data Sources         | None                     |
| Raw Frame Provider   | Data Sources             |
| Parser/Decoder       | Raw Frame Provider       |
| Signal Stream Core   | Parser/Decoder OR direct decoded source |
| Visualization        | Signal Stream Core       |

### 4.2 Forbidden Dependencies

- ❌ Data Sources MUST NOT depend on Parser/Decoder
- ❌ Raw Frame Provider MUST NOT perform parsing/decoding
- ❌ Parser/Decoder MUST NOT depend on Signal Stream Core
- ❌ Signal Stream Core MUST NOT depend on raw data format (ASC, USB, etc.)
- ❌ Visualization MUST NOT access raw frames directly

---

## 5. Optional vs. Mandatory Components

### 5.1 Mandatory Components

These components are **always** present in the system:

1. **Raw Frame Provider** (or equivalent decoded signal source)
2. **Signal Stream Core**
3. **Visualization**

### 5.2 Optional / Bypassable Components

These components can be bypassed depending on the data source:

1. **Parser / Decoder**
   - Required when: Data source provides raw CAN frames (ASC, USB)
   - Bypassed when: Data source provides already-decoded signals (gRPC)

---

## 6. PoC vs. Future Components

### 6.1 PoC Scope (Current Implementation)

| Component            | PoC Feature                      |
|----------------------|----------------------------------|
| Data Sources         | ASC File Reader                  |
| Raw Frame Provider   | ASC-based implementation         |
| Parser/Decoder       | DBC parsing + signal decoding    |
| Signal Stream Core   | In-memory buffer, basic windowing|
| Visualization        | List view + basic line chart     |

### 6.2 Future Extensions

| Component            | Future Feature                   |
|----------------------|----------------------------------|
| Data Sources         | USB CAN adapter                  |
| Data Sources         | Remote gRPC stream               |
| Parser/Decoder       | Advanced DBC features            |
| Signal Stream Core   | Persistence, aggregation         |
| Visualization        | Multi-signal plots, dashboards   |

---

## 7. Key Architectural Principles

### 7.1 Separation of Concerns

- **Raw data acquisition** is isolated from **signal interpretation**
- **Signal state management** is isolated from **data source type**
- **Visualization** depends only on processed signals, never on raw data

### 7.2 Extensibility

- New data sources can be added by implementing `RawFrameProvider`
- Pre-decoded sources (gRPC) can bypass the Parser/Decoder layer
- Visualization layer is agnostic to data source type

### 7.3 Stream-Based Processing

- All components operate on data streams, not batches
- Supports real-time processing and visualization
- Enables low-latency signal updates

### 7.4 Layer Bypass Support

The architecture supports **two data ingestion paths**:

**Path 1: Raw CAN Frames (PoC)**
```
Data Source → Raw Frame Provider → Parser/Decoder → Signal Stream Core → Visualization
```

**Path 2: Pre-Decoded Signals (Future)**
```
gRPC Source → Signal Stream Core → Visualization
(Parser/Decoder bypassed)
```

> **Implementation Guide:** See [Decoder Abstraction and Optionality](decoder-optionality.md) for detailed implementation patterns, including the `SignalProvider` abstraction that enables decoder optionality.

---

## 8. Interface Contracts

### 8.1 RawCanFrame
```kotlin
data class RawCanFrame(
    val timestamp: Long,        // Microseconds since epoch
    val canId: Int,             // CAN identifier
    val dlc: Int,               // Data length code
    val payload: ByteArray      // Raw data bytes
)
```

### 8.2 SignalSample
```kotlin
data class SignalSample(
    val signalName: String,     // Signal identifier
    val physicalValue: Double,  // Decoded value
    val unit: String?,          // Physical unit (e.g., "km/h")
    val timestamp: Long         // Microseconds since epoch
)
```

### 8.3 RawFrameProvider
```kotlin
interface RawFrameProvider {
    fun start()
    fun stop()
    fun observeFrames(): Flow<RawCanFrame>
}
```

### 8.4 SignalDecoder
```kotlin
interface SignalDecoder {
    fun decode(frame: RawCanFrame): List<SignalSample>
}
```

---

## 9. Lifecycle and Execution Flow

The system follows a well-defined lifecycle with proper startup, runtime, and shutdown phases. This ensures clean resource management, proper error handling, and the ability to start and stop components without resource leaks or cascading failures.

**Key Documentation:**
See [Lifecycle and Execution Flow](lifecycle-execution-flow.md) for comprehensive details on:
- Component lifecycle states and transitions
- Startup sequence (bottom-up initialization)
- Shutdown sequence (top-down cleanup)
- Error propagation and isolation strategies
- Testing and validation guidelines

**Summary:**
- **Startup Order**: RawFrameProvider → SignalProvider → SignalStreamCore
- **Shutdown Order**: SignalStreamCore → SignalProvider → RawFrameProvider (reverse)
- **Error Handling**: Component failures are isolated and logged; partial operation continues when possible
- **State Management**: All major components track their lifecycle state (IDLE, STARTING, ACTIVE, STOPPING, STOPPED, ERROR)

**Components:**
- `SystemLifecycleManager`: Coordinates startup and shutdown of all components
- `SignalStreamCoreState`: Tracks lifecycle state of the core component
- `SignalProviderState`: Tracks lifecycle state of signal providers
- `DataSourceState`: Tracks lifecycle state of data sources

---

## 10. Architectural Validation

The architecture has been validated against 10 future scenarios to ensure it can accommodate planned enhancements without requiring fundamental redesign.

**Key Documentation:**
See [Architecture Validation Against Future Scenarios](architecture-validation.md) for comprehensive validation including:
- 10 concrete future scenarios tested (USB CAN, gRPC, cloud processing, etc.)
- Impact assessment for each scenario
- SOLID principles validation
- Risk assessment and recommendations
- Core stability metrics

**Validation Summary:**
- ✅ All 10 scenarios supported without core component modifications
- ✅ 100% core stability (zero changes to SignalStreamCore, interfaces, data types)
- ✅ Average 130 lines of new code per scenario
- ✅ All SOLID principles maintained

**Validation Checklist:**

The architecture is considered valid if:

- ✅ Core logic does NOT depend on file formats (ASC, DBC)
- ✅ Core logic does NOT depend on hardware interfaces (USB)
- ✅ Parser/Decoder can be bypassed for pre-decoded sources
- ✅ Adding a new data source does NOT require refactoring Signal Stream Core
- ✅ Signal Stream Core operates exclusively on `SignalSample`, never `RawCanFrame`
- ✅ Visualization is decoupled from data acquisition method
- ✅ Components can be started and stopped cleanly
- ✅ Failures in one block do not crash unrelated blocks
- ✅ System supports restart after clean shutdown

**All criteria validated:** ✅ See detailed validation in [architecture-validation.md](architecture-validation.md)

---

## 11. Conclusion

This architecture ensures:

1. **Modularity**: Each block has a single, well-defined responsibility
2. **Extensibility**: New data sources can be added without core refactoring
3. **Flexibility**: Parser/Decoder layer can be bypassed when not needed
4. **Maintainability**: Clear boundaries prevent tight coupling
5. **Testability**: Each block can be tested independently
6. **Robustness**: Lifecycle management ensures clean startup/shutdown and error isolation

The architecture supports both the current PoC scope (ASC files + DBC decoding) and future enhancements (USB CAN, gRPC streams) without requiring fundamental redesign.

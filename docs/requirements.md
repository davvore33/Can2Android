# CAN Bus Plotter — Proof of Concept Requirements

## 1. Purpose of the Proof of Concept

The goal of this Proof of Concept (PoC) is **not** to build a production-ready Android application, but to:

- Validate the technical feasibility of CAN signal monitoring on Android
- Validate a modular, future-proof architecture
- Prove that real-time signal decoding and visualization is achievable
- Evaluate hardware feasibility for live CAN acquisition
- Establish a clear GO / NO-GO decision for full implementation

---

## 2. Scope of the PoC

### In scope
- ASC file ingestion as CAN data source
- Dynamic CAN frame parsing
- DBC-based signal decoding
- Real-time signal visualization (minimal UI)
- Hardware feasibility analysis (non-implementation)
- Architecture extensibility validation

### Out of scope
- Production-grade UI/UX
- CAN-to-USB live acquisition implementation
- Advanced visualization types
- Data export
- Performance optimization
- Persistence and user profiles

---

## 3. Success Criteria (PoC Completion)

The PoC is considered **successful** if:

- CAN signals can be decoded dynamically from an ASC file
- Signals are visualized in real time during parsing
- The architecture supports replacing the data source without refactoring core logic
- Hardware feasibility is clearly assessed and documented
- The system core operates on decoded, time-based signals

---

## 4. Functional Requirements

### 4.1 Data Source — ASC File

- The system MUST support reading CAN data from ASC files
- ASC files MUST be processed as a stream (not fully loaded into memory)
- Each CAN frame MUST include:
  - Timestamp
  - CAN ID
  - DLC
  - Payload

---

### 4.2 Raw Data Acquisition Layer

- The acquisition layer MUST provide raw CAN frames only
- The acquisition layer MUST NOT perform:
  - Parsing
  - Decoding
  - Signal interpretation
- A common interface MUST be defined for all raw data sources

---

### 4.3 CAN Parsing & Decoding

- The system MUST parse CAN frames dynamically
- The system MUST support DBC files
- Signal decoding MUST:
  - Happen at runtime
  - Produce physical values
  - Preserve timestamps
- Decoding MUST be stream-based, not batch-based

---

### 4.4 Signal Model

- A `SignalSample` MUST represent:
  - Signal name
  - Physical value
  - Unit (if available)
  - Timestamp
- Signals MUST be handled as time-series data
- A configurable time window MUST be supported (basic implementation)

---

### 4.5 State Management

- The system MUST buffer decoded signals in memory
- The buffer MUST support real-time updates
- The buffer MUST support multiple signals simultaneously

---

### 4.6 Visualization (PoC UI)

- The UI MUST display a list of available signals
- The UI MUST show:
  - Last value of a signal
  - Line chart of recent signal history
- Visualization MUST update in real time during parsing

---

## 5. Non-Functional Requirements

### 5.1 Architecture

- The architecture MUST be modular
- Core logic MUST NOT depend on:
  - File formats
  - Hardware interfaces
- Parsing and decoding MUST be optional components

---

### 5.2 Extensibility

The architecture MUST support future data sources, including:
- CAN-to-USB live acquisition
- Remote gRPC streams with already-decoded signals

For gRPC sources:
- Parsing and decoding layers MUST be bypassable
- The system MUST ingest already physical, time-based signals

> **Architecture Details:** See [Decoder Abstraction and Optionality](decoder-optionality.md) for the complete design of how decoder bypass is achieved through the `SignalProvider` abstraction.

---

## 6. Hardware Feasibility Analysis (PoC Deliverable)

The PoC MUST include a **technical feasibility analysis document** covering:

- Android USB Host support
- Common CAN-to-USB adapters
- Driver and kernel constraints
- Feasibility of live CAN acquisition
- Risks and limitations

> No hardware integration code is required in the PoC.

---

## 7. Deliverables

The PoC MUST deliver:

- A working Android application (PoC level)
- ASC → decoded signal → visualization pipeline
- Modular architecture implementation
- Hardware feasibility analysis document
- Clear GO / NO-GO recommendation

---

## 8. Explicit PoC Limitations

The following are intentionally excluded:

- CAN live acquisition implementation
- Data export (CSV, JSON)
- Advanced UI customization
- Persistent storage
- Performance tuning

---

## 9. Transition to Implementation

If the PoC is successful, the following areas will be addressed next:

- Live CAN acquisition via USB
- Advanced visualization
- User customization
- Data export
- Performance optimization

---

## 10. PoC Mindset

This PoC prioritizes:
- Architectural correctness over completeness
- Learning and validation over polish
- Long-term maintainability over short-term features

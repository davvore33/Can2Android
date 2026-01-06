# Documentation Guide

## Overview

This guide helps you navigate the CAN Bus Plotter PoC documentation. Documentation is organized by topic and audience.

---

## Quick Start

**New to the project?** Start here:
1. [README.md](../README.md) - Project overview and quick introduction
2. [requirements.md](requirements.md) - PoC scope and success criteria
3. [architecture.md](architecture.md) - High-level architecture overview

**Want to understand the architecture?** Read in this order:
1. [architecture.md](architecture.md) - Core architectural blocks and data flow
2. [decoder-optionality.md](decoder-optionality.md) - How decoder bypass works
3. [lifecycle-execution-flow.md](lifecycle-execution-flow.md) - Component lifecycle management

**Validating future scenarios?**
- [architecture-validation.md](architecture-validation.md) - 10 future scenarios validated

**Planning hardware integration?**
- [hardware-feasibility.md](hardware-feasibility.md) - USB CAN adapter analysis

---

## Documentation Index

### 1. Project Overview

#### [README.md](../README.md)
- **Purpose:** Project introduction and overview
- **Audience:** Everyone
- **Key Content:**
  - PoC objectives and status
  - Architecture diagram
  - Quick links to key documentation
  - Project structure
  - Technology stack

**Start here if you're new to the project.**

---

### 2. Requirements and Scope

#### [requirements.md](requirements.md)
- **Purpose:** Define PoC scope, requirements, and success criteria
- **Audience:** Product owners, architects, developers
- **Key Content:**
  - PoC objectives (what we're validating)
  - In-scope vs. out-of-scope features
  - Success criteria
  - Functional requirements (data sources, decoding, visualization)
  - Non-functional requirements (architecture, extensibility)
  - Deliverables and limitations

**Read this to understand what the PoC aims to achieve.**

**Key Sections:**
- [Purpose of the PoC](requirements.md#1-purpose-of-the-proof-of-concept)
- [Success Criteria](requirements.md#3-success-criteria-poc-completion)
- [Functional Requirements](requirements.md#4-functional-requirements)
- [Extensibility Requirements](requirements.md#52-extensibility)

---

### 3. Architecture Documentation

#### [architecture.md](architecture.md)
- **Purpose:** Complete architectural definition
- **Audience:** Architects, senior developers
- **Key Content:**
  - Core architectural blocks (Data Sources, Providers, Decoder, Core, Visualization)
  - Component responsibilities and constraints
  - Data flow diagrams
  - Dependency rules
  - Interface contracts
  - PoC vs. future components

**Read this to understand the system architecture.**

**Key Sections:**
- [Core Architectural Blocks](architecture.md#2-core-architectural-blocks)
- [Data Flow Diagram](architecture.md#3-data-flow-diagram)
- [Dependency Rules](architecture.md#4-dependency-rules)
- [Optional vs. Mandatory Components](architecture.md#5-optional-vs-mandatory-components)
- [Interface Contracts](architecture.md#8-interface-contracts)

**Related Documents:**
- [decoder-optionality.md](decoder-optionality.md) - Detailed decoder bypass design
- [lifecycle-execution-flow.md](lifecycle-execution-flow.md) - Component lifecycle

---

#### [decoder-optionality.md](decoder-optionality.md)
- **Purpose:** Explain how the decoder layer can be bypassed
- **Audience:** Architects, developers implementing signal providers
- **Key Content:**
  - Why decoder bypass is important (gRPC pre-decoded signals)
  - SignalProvider abstraction
  - Implementation patterns (with/without local decoding)
  - Code examples for both patterns
  - Testing strategies

**Read this to understand decoder flexibility.**

**Key Sections:**
- [Key Principle](decoder-optionality.md#key-principle)
- [Implementation Pattern 1: With Local Decoding](decoder-optionality.md#pattern-1-with-local-decoding-poc)
- [Implementation Pattern 2: Pre-Decoded Remote Source](decoder-optionality.md#pattern-2-pre-decoded-remote-source-future)
- [Testing Approach](decoder-optionality.md#testing-the-abstraction)

---

#### [lifecycle-execution-flow.md](lifecycle-execution-flow.md)
- **Purpose:** Define component lifecycle and execution flow
- **Audience:** Developers implementing components
- **Key Content:**
  - Component lifecycle states (IDLE → STARTING → ACTIVE → STOPPING → STOPPED → ERROR)
  - Startup sequence (bottom-up initialization)
  - Shutdown sequence (top-down cleanup)
  - Error propagation strategy (4 error categories)
  - Lifecycle coordination (SystemLifecycleManager)
  - Testing recommendations

**Read this to understand how components start, run, and stop.**

**Key Sections:**
- [Component Lifecycle States](lifecycle-execution-flow.md#2-component-lifecycle-states)
- [Startup Sequence](lifecycle-execution-flow.md#3-startup-sequence)
- [Shutdown Sequence](lifecycle-execution-flow.md#5-shutdown-sequence)
- [Error Propagation Strategy](lifecycle-execution-flow.md#6-error-propagation-strategy)
- [Lifecycle Coordination](lifecycle-execution-flow.md#7-lifecycle-coordination)

---

#### [architecture-validation.md](architecture-validation.md)
- **Purpose:** Validate architecture against future scenarios
- **Audience:** Architects, product owners, stakeholders
- **Key Content:**
  - 10 concrete future scenarios tested
  - Architecture mapping for each scenario
  - Impact assessment (changes required)
  - SOLID principles validation
  - Risk assessment
  - Recommendations

**Read this to see how the architecture handles future requirements.**

**Validated Scenarios:**
1. USB CAN Adapter (live data)
2. Remote gRPC Stream (pre-decoded signals)
3. Multiple Simultaneous Sources
4. Signal Recording and Replay
5. Multiple DBC File Support
6. Cloud-Based Signal Processing
7. Signal Filtering and Transformation
8. Persistent Signal History
9. Multiple Visualization Types
10. CAN Frame Injection for Testing

**Key Sections:**
- [Future Scenarios](architecture-validation.md#2-future-scenarios)
- [Architectural Principles Validation](architecture-validation.md#4-architectural-principles-validation)
- [Future-Proofing Assessment](architecture-validation.md#5-future-proofing-assessment)
- [Conclusion](architecture-validation.md#8-conclusion)

**Key Findings:**
- ✅ 100% of scenarios supported without core modifications
- ✅ 100% core stability (zero changes to interfaces)
- ✅ All SOLID principles maintained

---

### 4. Hardware Feasibility

#### [hardware-feasibility.md](hardware-feasibility.md)
- **Purpose:** Analyze feasibility of USB CAN adapters on Android
- **Audience:** Hardware engineers, architects, product owners
- **Key Content:**
  - Android USB Host API capabilities
  - Common USB CAN adapter evaluation
  - Recommended adapters for Android (CANable, CANUSB, ELM327)
  - Technical implementation details (slcan protocol)
  - Challenges and limitations
  - Alternative solutions (Bluetooth, WiFi)
  - Implementation roadmap
  - Cost analysis
  - Risk assessment

**Read this to understand hardware integration possibilities.**

**Key Sections:**
- [Executive Summary](hardware-feasibility.md#1-executive-summary)
- [Android USB Host API](hardware-feasibility.md#2-android-usb-host-api)
- [Common USB CAN Adapters](hardware-feasibility.md#3-common-usb-can-adapters)
- [Technical Implementation](hardware-feasibility.md#4-technical-implementation)
- [Challenges and Limitations](hardware-feasibility.md#5-challenges-and-limitations)
- [Recommended PoC Approach](hardware-feasibility.md#7-recommended-poc-approach)
- [Conclusion](hardware-feasibility.md#11-conclusion)

**Conclusion:** ✅ Feasible - CANable with slcan protocol recommended

---

## Reading Paths

### For Developers

**Getting Started:**
1. [README.md](../README.md) - Overview
2. [architecture.md](architecture.md) - System design
3. [lifecycle-execution-flow.md](lifecycle-execution-flow.md) - Component lifecycle
4. Domain layer code (`app/src/main/java/com/can2android/domain/`)

**Implementing a New Data Source:**
1. [architecture.md - RawFrameProvider](architecture.md#22-raw-frame-provider)
2. [decoder-optionality.md](decoder-optionality.md)
3. [lifecycle-execution-flow.md - Startup Sequence](lifecycle-execution-flow.md#3-startup-sequence)
4. Example: `DecodingSignalProvider.kt`

**Implementing USB CAN Support:**
1. [hardware-feasibility.md](hardware-feasibility.md)
2. [hardware-feasibility.md - Technical Implementation](hardware-feasibility.md#4-technical-implementation)
3. Reference: Existing `RawFrameProvider` implementations

### For Architects

**Architecture Review:**
1. [requirements.md](requirements.md)
2. [architecture.md](architecture.md)
3. [architecture-validation.md](architecture-validation.md)
4. [decoder-optionality.md](decoder-optionality.md)
5. [lifecycle-execution-flow.md](lifecycle-execution-flow.md)

**Future Planning:**
1. [architecture-validation.md](architecture-validation.md) - See what's possible
2. [hardware-feasibility.md](hardware-feasibility.md) - Hardware options
3. [requirements.md - Extensibility](requirements.md#52-extensibility)

### For Product Owners

**PoC Understanding:**
1. [README.md](../README.md) - Quick overview
2. [requirements.md](requirements.md) - What we're building
3. [requirements.md - Success Criteria](requirements.md#3-success-criteria-poc-completion)
4. [architecture-validation.md - Conclusion](architecture-validation.md#8-conclusion)

**Future Capabilities:**
1. [architecture-validation.md](architecture-validation.md) - Future scenarios
2. [hardware-feasibility.md](hardware-feasibility.md) - Hardware options
3. [requirements.md - Transition to Implementation](requirements.md#9-transition-to-implementation)

### For QA/Testers

**Testing Approach:**
1. [requirements.md - Success Criteria](requirements.md#3-success-criteria-poc-completion)
2. [lifecycle-execution-flow.md - Testing Recommendations](lifecycle-execution-flow.md#9-testing-recommendations)
3. [architecture-validation.md - Scenarios](architecture-validation.md#2-future-scenarios) (future test cases)

---

## Document Status

| Document | Status | Last Updated | Completeness |
|----------|--------|--------------|--------------|
| README.md | ✅ Complete | 2026-01-06 | 100% |
| requirements.md | ✅ Complete | 2025-XX-XX | 100% |
| architecture.md | ✅ Complete | 2026-01-06 | 100% |
| decoder-optionality.md | ✅ Complete | 2025-XX-XX | 100% |
| lifecycle-execution-flow.md | ✅ Complete | 2026-01-06 | 100% |
| architecture-validation.md | ✅ Complete | 2026-01-06 | 100% |
| hardware-feasibility.md | ✅ Complete | 2026-01-06 | 100% |

---

## Documentation Maintenance

### When to Update

**requirements.md:**
- Scope changes
- New success criteria
- Requirement clarifications

**architecture.md:**
- New architectural blocks
- Interface changes
- Dependency rule modifications

**decoder-optionality.md:**
- New SignalProvider implementations
- Protocol changes

**lifecycle-execution-flow.md:**
- New lifecycle states
- Error handling strategy changes
- New components with lifecycle

**architecture-validation.md:**
- New future scenarios to validate
- Architecture changes that affect validation
- Risk assessment updates

**hardware-feasibility.md:**
- New hardware options
- Protocol discoveries
- Cost updates

---

## Related Resources

### Code Locations

**Domain Layer:**
```
app/src/main/java/com/can2android/domain/
├── RawCanFrame.kt              # Raw CAN message data class
├── RawFrameProvider.kt         # Interface for raw data sources
├── SignalSample.kt             # Decoded signal data class
├── SignalDecoder.kt            # Interface for CAN decoding
├── SignalProvider.kt           # Interface for signal sources
├── DecodingSignalProvider.kt   # Combines provider + decoder
├── DirectSignalProvider.kt     # Pre-decoded signal source
├── SignalBuffer.kt             # Time-windowed signal buffer
├── SignalStreamCore.kt         # Central signal processing
├── SignalUpdate.kt             # Signal update event
├── SystemLifecycleManager.kt   # Lifecycle coordinator
├── DataSourceState.kt          # Data source lifecycle states
├── SignalProviderState.kt      # Signal provider states
├── SignalStreamCoreState.kt    # Core component states
└── DataSourceException.kt      # Exception hierarchy
```

### External References

- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)
- [Android USB Host](https://developer.android.com/guide/topics/connectivity/usb/host)
- [CAN Bus Protocol](https://en.wikipedia.org/wiki/CAN_bus)
- [DBC File Format](https://www.csselectronics.com/pages/can-dbc-file-database-intro)

---

## Glossary

- **ASC File** - ASCII CAN trace file format for recording CAN bus data
- **CAN Bus** - Controller Area Network, vehicle communication protocol
- **DBC File** - Database CAN, text file defining signal layouts in CAN messages
- **PoC** - Proof of Concept
- **Signal Sample** - Decoded CAN signal with name, physical value, unit, timestamp
- **Raw CAN Frame** - Uninterpreted CAN message (ID, DLC, payload bytes)
- **slcan** - Serial Line CAN protocol for USB CAN adapters
- **USB Host** - Android mode where device acts as USB master

---

## Getting Help

**Found an issue or have questions?**
- Check the appropriate documentation section above
- Review code examples in domain layer
- Create an issue on GitHub

**Contributing to Documentation:**
- Follow existing structure and style
- Update this index when adding new documents
- Keep code examples synchronized with implementation

---

**Last Updated:** January 6, 2026  
**Maintained By:** CAN2Android PoC Team

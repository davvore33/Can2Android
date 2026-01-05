# Decoder Abstraction and Optionality

## Overview

This document explains how the decoder layer integrates into the data flow and how it can be bypassed for pre-decoded data sources.

## Key Principle

**The Signal Stream Core depends only on `SignalProvider`, not on how signals are obtained.**

This enables two distinct data paths:
1. **Local Decoding**: Raw frames → Decoder → Signals
2. **Pre-Decoded**: Remote source → Signals (decoder bypassed)

---

## Architecture Components

### Core Interfaces

```kotlin
// Entry point for Signal Stream Core
interface SignalProvider {
    fun start()
    fun stop()
    fun observeSignals(): Flow<SignalSample>
}

// Source of raw CAN frames (ASC, USB, etc.)
interface RawFrameProvider {
    fun start()
    fun stop()
    fun observeFrames(): Flow<RawCanFrame>
}

// Decoder: raw frames → signals
interface SignalDecoder {
    fun decode(frame: RawCanFrame): List<SignalSample>
}
```

### Data Types

```kotlin
// Input to decoder (raw data)
data class RawCanFrame(
    val timestamp: Long,
    val canId: Int,
    val dlc: Int,
    val payload: ByteArray
)

// Output of decoder (processed data)
data class SignalSample(
    val signalName: String,
    val physicalValue: Double,
    val unit: String?,
    val timestamp: Long
)
```

---

## Implementation Patterns

### Pattern 1: With Local Decoding (PoC)

Used for data sources that provide raw CAN frames.

**Data Flow:**
```
ASC File → RawFrameProvider → SignalDecoder → SignalProvider → Signal Stream Core
```

**Implementation:**
```kotlin
// Create the components
val rawProvider: RawFrameProvider = AscFileProvider(file)
val decoder: SignalDecoder = DbcSignalDecoder(dbcFile)

// Combine them using DecodingSignalProvider
val signalProvider: SignalProvider = DecodingSignalProvider(
    rawFrameProvider = rawProvider,
    signalDecoder = decoder
)

// Signal Stream Core only sees SignalProvider
signalStreamCore.attach(signalProvider)
```

**Characteristics:**
- Decoder runs at runtime
- Timestamps preserved through decoding pipeline
- Decoder can be replaced without changing Signal Stream Core
- Multiple signals extracted from each frame

---

### Pattern 2: Pre-Decoded Remote Source (Future)

Used for data sources that already provide decoded signals.

**Data Flow:**
```
gRPC Server → DirectSignalProvider → Signal Stream Core
(Decoder bypassed)
```

**Implementation:**
```kotlin
// Remote source provides Flow<SignalSample> directly
val grpcClient = GrpcSignalClient(host, port)

// Create DirectSignalProvider - no decoder needed
val signalProvider: SignalProvider = DirectSignalProvider(
    signalSource = { grpcClient.streamSignals() },
    onStart = { grpcClient.connect() },
    onStop = { grpcClient.disconnect() }
)

// Signal Stream Core works identically
signalStreamCore.attach(signalProvider)
```

**Characteristics:**
- No local decoding
- No DBC files needed
- Signals arrive pre-processed
- Signal Stream Core operates identically to Pattern 1

---

## Key Benefits

### 1. Decoder Not Tightly Coupled to Data Sources

The decoder is a separate component that operates on `RawCanFrame` objects. It doesn't know or care where frames come from:

```kotlin
class DbcSignalDecoder(dbcFile: File) : SignalDecoder {
    override fun decode(frame: RawCanFrame): List<SignalSample> {
        // Works with frames from ANY RawFrameProvider
        // - ASC files
        // - USB CAN adapters
        // - Mock test data
        // - Replay buffers
    }
}
```

### 2. Decoder Can Be Replaced or Disabled

Multiple decoder implementations can coexist:

```kotlin
// DBC-based decoder
val dbcDecoder: SignalDecoder = DbcSignalDecoder(dbcFile)

// Custom decoder (e.g., proprietary format)
val customDecoder: SignalDecoder = CustomSignalDecoder(config)

// No decoder at all
val directProvider = DirectSignalProvider(...)
```

Switching decoders requires only changing the `SignalProvider` construction:

```kotlin
// Before: DBC decoder
val provider = DecodingSignalProvider(rawProvider, dbcDecoder)

// After: Custom decoder
val provider = DecodingSignalProvider(rawProvider, customDecoder)

// Signal Stream Core unchanged
```

### 3. Signal Stream Core Agnostic to Decoding

The Signal Stream Core depends only on `SignalProvider`:

```kotlin
class SignalStreamCore(
    private val signalProvider: SignalProvider
) {
    fun start() {
        signalProvider.start()
        
        signalProvider.observeSignals()
            .collect { sample ->
                // Process signal - doesn't know if it was:
                // - Decoded locally from ASC file
                // - Decoded locally from USB CAN
                // - Received pre-decoded from gRPC
                updateBuffer(sample)
            }
    }
}
```

This isolation means:
- Adding new data sources doesn't require refactoring Signal Stream Core
- Testing is easier (mock SignalProvider instead of entire pipeline)
- Pre-decoded sources integrate seamlessly

---

## Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│                   SIGNAL STREAM CORE                        │
│                  (Application Core)                         │
└───────────────────────┬─────────────────────────────────────┘
                        │ depends on
                        ▼
                ┌───────────────┐
                │SignalProvider │ ◄── Interface abstraction
                │  (interface)  │
                └───────┬───────┘
                        │ implemented by
           ┌────────────┴────────────┐
           ▼                         ▼
┌──────────────────────┐   ┌─────────────────────┐
│DecodingSignalProvider│   │DirectSignalProvider │
│  (with decoder)      │   │  (bypass decoder)   │
└──────┬───────────────┘   └─────────────────────┘
       │ uses
       ├──────────┬──────────┐
       ▼          ▼          ▼
┌─────────┐ ┌──────────┐    (no decoder needed)
│RawFrame │ │  Signal  │
│Provider │ │  Decoder │
└─────────┘ └──────────┘
```

### Dependency Rules

✅ **Allowed:**
- Signal Stream Core → SignalProvider (interface)
- DecodingSignalProvider → RawFrameProvider + SignalDecoder
- DirectSignalProvider → (external signal source)

❌ **Forbidden:**
- Signal Stream Core → SignalDecoder (direct dependency)
- Signal Stream Core → RawFrameProvider (direct dependency)
- SignalDecoder → SignalProvider (circular dependency)

---

## Testing Strategy

### Unit Testing Each Component

```kotlin
// Test decoder in isolation
class DbcSignalDecoderTest {
    @Test
    fun `decode speed signal from CAN frame`() {
        val decoder = DbcSignalDecoder(testDbcFile)
        val frame = RawCanFrame(
            timestamp = 1000000,
            canId = 0x100,
            dlc = 8,
            payload = byteArrayOf(0x10, 0x20, ...)
        )
        
        val signals = decoder.decode(frame)
        
        assertEquals("VehicleSpeed", signals[0].signalName)
        assertEquals(65.5, signals[0].physicalValue, 0.1)
    }
}

// Test SignalProvider with mock decoder
class DecodingSignalProviderTest {
    @Test
    fun `forwards signals from decoder`() = runTest {
        val mockRawProvider = mockk<RawFrameProvider>()
        val mockDecoder = mockk<SignalDecoder>()
        
        val provider = DecodingSignalProvider(mockRawProvider, mockDecoder)
        
        // Test without knowing decoder implementation
        val signals = provider.observeSignals().first()
        // ...
    }
}
```

### Integration Testing

```kotlin
// Test Signal Stream Core with different providers
class SignalStreamCoreTest {
    @Test
    fun `processes signals from decoded source`() {
        val provider = DecodingSignalProvider(ascProvider, dbcDecoder)
        val core = SignalStreamCore(provider)
        
        core.start()
        // Verify signal processing
    }
    
    @Test
    fun `processes signals from direct source`() {
        val provider = DirectSignalProvider { flowOf(...) }
        val core = SignalStreamCore(provider)
        
        core.start()
        // Same verification - core doesn't know the difference
    }
}
```

---

## Migration Path for Future Sources

### Adding gRPC Pre-Decoded Source

1. **Implement gRPC client** (no decoder involved):
```kotlin
class GrpcSignalStreamClient(host: String, port: Int) {
    fun connect() { ... }
    fun disconnect() { ... }
    
    fun streamSignals(): Flow<SignalSample> {
        // Receives already-decoded signals from server
        return grpcStub.streamSignals()
            .asFlow()
            .map { proto -> proto.toSignalSample() }
    }
}
```

2. **Wrap in DirectSignalProvider**:
```kotlin
val client = GrpcSignalStreamClient(host, port)
val provider = DirectSignalProvider(
    signalSource = { client.streamSignals() },
    onStart = { client.connect() },
    onStop = { client.disconnect() }
)
```

3. **Use with Signal Stream Core** (unchanged):
```kotlin
val core = SignalStreamCore(provider)
core.start()
```

### Adding USB CAN Adapter

1. **Implement RawFrameProvider**:
```kotlin
class UsbCanAdapter(device: String) : RawFrameProvider {
    override fun observeFrames(): Flow<RawCanFrame> {
        // Read raw frames from USB device
    }
}
```

2. **Use existing decoder**:
```kotlin
val rawProvider = UsbCanAdapter("/dev/ttyUSB0")
val decoder = DbcSignalDecoder(dbcFile)
val provider = DecodingSignalProvider(rawProvider, decoder)
```

3. **Use with Signal Stream Core** (unchanged):
```kotlin
val core = SignalStreamCore(provider)
core.start()
```

---

## Acceptance Criteria Verification

### ✅ Decoder is not tightly coupled to data sources
- `SignalDecoder` interface operates on `RawCanFrame` only
- Decoder doesn't know about ASC files, USB, or any specific source
- Same decoder works with any `RawFrameProvider` implementation

### ✅ Decoder can be replaced or disabled
- Replaced: Swap decoder in `DecodingSignalProvider` constructor
- Disabled: Use `DirectSignalProvider` instead

### ✅ Signal Stream Core does not know if decoding happened locally
- Depends only on `SignalProvider` interface
- Receives `SignalSample` objects without knowing their origin
- Works identically with local decoding or pre-decoded sources

---

## Conclusion

The decoder abstraction achieves complete optionality through the `SignalProvider` interface:

1. **For raw frame sources**: `DecodingSignalProvider` applies decoder
2. **For pre-decoded sources**: `DirectSignalProvider` bypasses decoder
3. **Signal Stream Core**: Agnostic to which path is used

This design satisfies all architectural requirements and provides a clean, testable, and extensible foundation for both current and future data sources.

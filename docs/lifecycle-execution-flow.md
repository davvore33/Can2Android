# Lifecycle and Execution Flow

## 1. Overview

This document defines how the CAN Bus Plotter system starts, runs, and stops during the PoC. It establishes the lifecycle management strategy, execution flow, and error propagation mechanisms that ensure components can be started and stopped cleanly while preventing failures from cascading across unrelated blocks.

---

## 2. Component Lifecycle States

All major components (RawFrameProvider, SignalProvider, SignalStreamCore) follow a standardized lifecycle:

```
┌─────┐  start()   ┌──────────┐           ┌────────┐
│IDLE │ ─────────> │ STARTING │ ───────> │ ACTIVE │
└─────┘            └──────────┘           └────────┘
   ^                    │                      │
   │                    │ error                │ stop()
   │                    v                      v
   │                ┌───────┐            ┌──────────┐
   └────────────────│ ERROR │            │ STOPPING │
                    └───────┘            └──────────┘
                                              │
                                              v
                                         ┌─────────┐
                                         │ STOPPED │
                                         └─────────┘
```

### State Definitions

- **IDLE**: Component created but not started. No resources allocated.
- **STARTING**: Transitional state during initialization. Resources being allocated.
- **ACTIVE**: Component is running and processing data.
- **STOPPING**: Transitional state during shutdown. Resources being released.
- **STOPPED**: Component has stopped cleanly. Resources released.
- **ERROR**: Component encountered a failure. May require manual intervention.

---

## 3. Startup Sequence

The system follows a strict bottom-up initialization order to ensure dependencies are satisfied:

### 3.1 Phase 1: Data Source Initialization
```
1. Create RawFrameProvider instance
2. Call RawFrameProvider.start()
   - Open file/connection
   - Validate data source
   - Allocate buffers
   - Transition IDLE → STARTING → ACTIVE
```

**Error Handling:**
- Failures throw `DataSourceInitializationException`
- State transitions to ERROR
- No downstream components are affected (not yet created)

### 3.2 Phase 2: Signal Provider Initialization
```
1. Create SignalProvider instance (with RawFrameProvider dependency)
2. Call SignalProvider.start()
   - Initialize decoder (if needed)
   - Load DBC definitions (if applicable)
   - Begin observing RawFrameProvider
   - Transition IDLE → STARTING → ACTIVE
```

**Error Handling:**
- Failures during DBC loading or decoder initialization
- SignalProvider transitions to ERROR state
- RawFrameProvider continues running but data is not consumed
- System can attempt recovery by restarting SignalProvider

### 3.3 Phase 3: Signal Stream Core Initialization
```
1. Create SignalStreamCore instance (with SignalProvider dependency)
2. Call SignalStreamCore.start()
   - Begin observing SignalProvider
   - Initialize signal buffers
   - Start emitting SignalUpdates
   - Transition IDLE → STARTING → ACTIVE
```

**Error Handling:**
- Failures are rare (mostly memory-related)
- Core transitions to ERROR state
- SignalProvider continues running but samples are not consumed
- System can attempt recovery by restarting Core

### 3.4 Complete Startup Flow
```
┌──────────────────┐
│ RawFrameProvider │
└────────┬─────────┘
         │ observeFrames()
         v
┌──────────────────┐
│ SignalProvider   │ (optional decoding)
└────────┬─────────┘
         │ observeSignals()
         v
┌──────────────────┐
│SignalStreamCore  │
└────────┬─────────┘
         │ signalUpdates
         v
┌──────────────────┐
│   UI/Observers   │
└──────────────────┘
```

**Initialization Code Example:**
```kotlin
// 1. Create and start raw frame provider
val rawFrameProvider: RawFrameProvider = AscFileFrameProvider(file)
rawFrameProvider.start()

// 2. Create and start signal provider with decoder
val signalProvider: SignalProvider = DecodingSignalProvider(
    rawFrameProvider = rawFrameProvider,
    decoder = signalDecoder
)
signalProvider.start()

// 3. Create and start signal stream core
val signalStreamCore = SignalStreamCore(
    signalProvider = signalProvider,
    timeWindowMicros = 60_000_000L,
    scope = coroutineScope
)
signalStreamCore.start()

// 4. Observe signal updates in UI
coroutineScope.launch {
    signalStreamCore.signalUpdates.collect { update ->
        // Update UI with signal data
    }
}
```

---

## 4. Runtime Execution Flow

Once all components are started, data flows through the pipeline:

### 4.1 Data Flow During Execution

```
1. RawFrameProvider emits RawCanFrame
   ↓
2. SignalProvider receives frame
   ↓
3. SignalDecoder processes frame (if applicable)
   ↓
4. SignalProvider emits SignalSample(s)
   ↓
5. SignalStreamCore receives sample
   ↓
6. SignalStreamCore updates buffer
   ↓
7. SignalStreamCore emits SignalUpdate
   ↓
8. UI/Observers receive update and refresh
```

### 4.2 Backpressure Handling

All data flows use Kotlin `Flow`, which provides built-in backpressure:

- If SignalStreamCore cannot keep up, SignalProvider will slow down
- If SignalProvider cannot keep up, RawFrameProvider will slow down
- No data is dropped unless explicitly configured (not in PoC)

### 4.3 Threading Model

- **RawFrameProvider**: Typically runs on I/O dispatcher (file reading, USB I/O)
- **SignalProvider/Decoder**: Runs on Default dispatcher (CPU-bound decoding)
- **SignalStreamCore**: Runs on Default dispatcher (buffering, state management)
- **UI Updates**: Runs on Main dispatcher

Coroutine scopes handle thread switching automatically via Flow collectors.

---

## 5. Shutdown Sequence

The system follows a strict top-down shutdown order to ensure clean resource release:

### 5.1 Phase 1: Stop Signal Stream Core
```
1. Call SignalStreamCore.stop()
   - Cancel signal observation coroutines
   - Clear all signal buffers
   - Stop emitting SignalUpdates
   - Transition ACTIVE → STOPPING → STOPPED
```

**Side Effects:**
- SignalProvider no longer has active consumers
- Backpressure may cause SignalProvider to buffer samples (temporarily)

### 5.2 Phase 2: Stop Signal Provider
```
1. Call SignalProvider.stop()
   - Cancel frame observation coroutines
   - Release decoder resources (if applicable)
   - Clear internal buffers
   - Transition ACTIVE → STOPPING → STOPPED
```

**Side Effects:**
- RawFrameProvider no longer has active consumers
- Frames may accumulate in RawFrameProvider buffers

### 5.3 Phase 3: Stop Raw Frame Provider
```
1. Call RawFrameProvider.stop()
   - Close file handles/connections
   - Release I/O buffers
   - Cancel reading coroutines
   - Transition ACTIVE → STOPPING → STOPPED
```

**Complete Shutdown:**
- All resources released
- All coroutines cancelled
- System can be restarted cleanly

### 5.4 Shutdown Code Example
```kotlin
// Stop in reverse order of startup
try {
    signalStreamCore.stop()
} catch (e: Exception) {
    log.error("Failed to stop SignalStreamCore", e)
}

try {
    signalProvider.stop()
} catch (e: Exception) {
    log.error("Failed to stop SignalProvider", e)
}

try {
    rawFrameProvider.stop()
} catch (e: Exception) {
    log.error("Failed to stop RawFrameProvider", e)
}
```

**Key Principle:** Continue shutdown even if individual components fail.

---

## 6. Error Propagation Strategy

### 6.1 Error Isolation Principles

1. **Component Independence**: Failure in one component should not crash unrelated components
2. **Graceful Degradation**: System should continue partial operation when possible
3. **Error Transparency**: Errors should be logged and observable, not silently swallowed
4. **Recovery Support**: Components should support restart after error

### 6.2 Error Categories and Handling

#### Category 1: Initialization Errors
**Examples:**
- File not found
- Invalid DBC file
- USB device not connected

**Handling:**
- Throw exception during `start()`
- Component transitions to ERROR state
- Do not initialize downstream components
- User is notified to fix the issue

#### Category 2: Runtime Data Errors
**Examples:**
- Corrupted CAN frame
- Invalid signal value
- Malformed ASC line

**Handling:**
- Log error with context
- Skip the problematic data item
- Continue processing subsequent items
- Emit error metric/event for monitoring
- Do NOT transition to ERROR state

**Example:**
```kotlin
try {
    val frame = parseAscLine(line)
    emit(frame)
} catch (e: DataSourceFormatException) {
    log.warn("Skipping invalid line: $line", e)
    // Continue reading next line
}
```

#### Category 3: Resource Errors
**Examples:**
- Out of memory
- I/O error during read
- Network timeout

**Handling:**
- Transition component to ERROR state
- Stop emitting data
- Downstream components continue with last known state
- System may attempt automatic recovery (retry with backoff)

#### Category 4: Coroutine Cancellation
**Examples:**
- User stops the system
- Parent scope cancelled
- Component explicitly stopped

**Handling:**
- Gracefully clean up resources
- Do NOT log as error (this is normal operation)
- Transition to STOPPED state

**Example:**
```kotlin
scope.launch {
    try {
        observeFrames().collect { frame ->
            // Process frame
        }
    } catch (e: CancellationException) {
        // Normal shutdown - don't log as error
        throw e // Re-throw to propagate cancellation
    } catch (e: Exception) {
        log.error("Unexpected error in frame processing", e)
        // Transition to ERROR state
    }
}
```

### 6.3 Error Propagation Flow

```
┌──────────────────┐
│ RawFrameProvider │
│   (ERROR state)  │
└────────┬─────────┘
         │ Flow completes/fails
         v
┌──────────────────┐
│ SignalProvider   │ ───> Logs error, transitions to ERROR
└────────┬─────────┘      (optional: attempt reconnection)
         │ Flow completes
         v
┌──────────────────┐
│SignalStreamCore  │ ───> Maintains last known state
└────────┬─────────┘      UI shows "disconnected"
         │ No new updates
         v
┌──────────────────┐
│   UI/Observers   │ ───> Display error state to user
└──────────────────┘      Offer "retry" action
```

### 6.4 PoC Error Handling Guidelines

For the PoC, keep error handling simple but structured:

1. **Do**: Log all errors with context
2. **Do**: Use structured exception types (DataSourceException hierarchy)
3. **Do**: Allow partial failures (skip bad data, continue processing)
4. **Do**: Transition to ERROR state for fatal errors
5. **Don't**: Implement complex retry logic (out of PoC scope)
6. **Don't**: Silently swallow exceptions
7. **Don't**: Allow one component failure to crash the entire app

---

## 7. Lifecycle Coordination

For the PoC, lifecycle can be managed manually in the UI layer. A future enhancement would be a `LifecycleManager`:

### 7.1 Manual Lifecycle Management (PoC)
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var rawFrameProvider: RawFrameProvider
    private lateinit var signalProvider: SignalProvider
    private lateinit var signalStreamCore: SignalStreamCore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create components but don't start
    }
    
    fun startDataFlow(file: File) {
        try {
            rawFrameProvider.start()
            signalProvider.start()
            signalStreamCore.start()
        } catch (e: Exception) {
            // Handle initialization failure
            stopDataFlow()
            showError(e)
        }
    }
    
    fun stopDataFlow() {
        signalStreamCore.stop()
        signalProvider.stop()
        rawFrameProvider.stop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopDataFlow()
    }
}
```

### 7.2 Future Enhancement: LifecycleManager
```kotlin
class SystemLifecycleManager(
    private val rawFrameProvider: RawFrameProvider,
    private val signalProvider: SignalProvider,
    private val signalStreamCore: SignalStreamCore
) {
    suspend fun startAll() {
        rawFrameProvider.start()
        signalProvider.start()
        signalStreamCore.start()
    }
    
    suspend fun stopAll() {
        withContext(NonCancellable) {
            tryStopComponent { signalStreamCore.stop() }
            tryStopComponent { signalProvider.stop() }
            tryStopComponent { rawFrameProvider.stop() }
        }
    }
    
    private suspend fun tryStopComponent(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            log.error("Error stopping component", e)
            // Continue with other components
        }
    }
}
```

---

## 8. PoC Validation Checklist

### Execution Flow
- [ ] Components start in correct order (bottom-up)
- [ ] Data flows from source to UI without loss
- [ ] Components stop in correct order (top-down)
- [ ] System can be restarted after clean shutdown

### Error Handling
- [ ] Initialization errors prevent system startup cleanly
- [ ] Runtime data errors are logged but processing continues
- [ ] Component failures don't crash the app
- [ ] Error states are observable in logs

### Resource Management
- [ ] File handles are closed on shutdown
- [ ] Coroutines are cancelled on shutdown
- [ ] Memory buffers are cleared on shutdown
- [ ] No resource leaks after multiple start/stop cycles

### Clean Shutdown
- [ ] SignalStreamCore stops cleanly
- [ ] SignalProvider releases decoder resources
- [ ] RawFrameProvider closes files/connections
- [ ] No exceptions during normal shutdown

---

## 9. Testing Recommendations

### Unit Tests
```kotlin
@Test
fun `test component lifecycle transitions`() {
    val provider = AscFileFrameProvider(file)
    
    // Initial state
    assertEquals(IDLE, provider.state)
    
    // Start
    provider.start()
    assertEquals(ACTIVE, provider.state)
    
    // Stop
    provider.stop()
    assertEquals(STOPPED, provider.state)
}

@Test
fun `test shutdown continues despite component failure`() {
    val failingProvider = mock<SignalProvider> {
        on { stop() } doThrow RuntimeException("Stop failed")
    }
    
    // Should not throw
    assertDoesNotThrow {
        signalStreamCore.stop()
        failingProvider.stop()
        rawFrameProvider.stop()
    }
}
```

### Integration Tests
```kotlin
@Test
fun `test complete startup and shutdown sequence`() = runTest {
    val manager = SystemLifecycleManager(...)
    
    // Start all components
    manager.startAll()
    
    // Verify data flow
    val update = signalStreamCore.signalUpdates.first()
    assertNotNull(update)
    
    // Stop all components
    manager.stopAll()
    
    // Verify clean shutdown
    assertFalse(signalStreamCore.isActive())
}
```

---

## 10. Summary

This lifecycle and execution flow design ensures:

✅ **Clean Startup**: Components initialize in dependency order
✅ **Robust Execution**: Data flows reliably through the pipeline
✅ **Clean Shutdown**: Resources are released in reverse order
✅ **Error Isolation**: Failures don't cascade to unrelated components
✅ **Observability**: Errors are logged and state transitions are trackable
✅ **Restartability**: System can be stopped and restarted cleanly

The architecture supports the PoC goal of validating technical feasibility while establishing patterns for production-grade lifecycle management in future iterations.

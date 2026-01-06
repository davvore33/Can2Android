# Hardware Feasibility Analysis — USB CAN Adapters on Android

## 1. Executive Summary

This document analyzes the technical feasibility of using USB CAN adapters with Android devices to acquire live CAN bus data.

**Conclusion:** ✅ **Feasible with Constraints**

Live CAN data acquisition on Android is technically feasible using USB CAN adapters, but requires:
- Android USB Host API support (Android 3.1+)
- Compatible CAN adapter hardware
- Custom driver implementation or compatible protocol
- Appropriate device permissions and hardware capabilities

**Recommendation:** Proceed with PoC using file-based data; validate live acquisition in next phase with specific hardware.

---

## 2. Android USB Host API

### 2.1 Overview

**Android USB Host API** (android.hardware.usb) enables Android devices to act as USB hosts and communicate with USB peripherals.

**Key Features:**
- Available since Android 3.1 (API level 12)
- Supports USB communication modes: Control, Bulk, Interrupt, Isochronous
- Provides device enumeration and permission management
- Low-level USB data transfer APIs

**Documentation:** https://developer.android.com/guide/topics/connectivity/usb/host

### 2.2 Requirements

**Device Requirements:**
- USB Host capability (not all Android devices support this)
- USB OTG (On-The-Go) support
- Sufficient power supply (some devices can't power USB peripherals)

**Software Requirements:**
- Android 3.1+ (API level 12+)
- USB Host feature declared in manifest:
  ```xml
  <uses-feature android:name="android.hardware.usb.host" />
  ```

**Runtime Requirements:**
- User permission grant for USB device access
- Device must be connected before app starts or handle hotplug events

### 2.3 Communication Flow

```
Android App
    ↓
USB Host API (Java/Kotlin)
    ↓
Android USB Subsystem
    ↓
USB Hardware Controller
    ↓
USB Cable
    ↓
USB CAN Adapter
    ↓
CAN Bus
```

---

## 3. Common USB CAN Adapters

### 3.1 Adapter Types

| Adapter Type | Protocol | Android Compatibility | Notes |
|--------------|----------|----------------------|-------|
| PCAN-USB | Proprietary | ⚠️ Requires reverse engineering | Widely used, no official Android support |
| CANUSB (Lawicel) | ASCII protocol | ✅ Good | Simple text-based protocol |
| Kvaser USBcan | Proprietary | ⚠️ Requires custom driver | Professional grade |
| CANable | slcan protocol | ✅ Good | Open-source firmware, slcan protocol |
| PEAK CAN | Proprietary | ⚠️ Limited | Requires custom implementation |
| ELM327 | AT commands | ✅ Excellent | OBD-II focused, limited CAN support |

### 3.2 Recommended Adapters for Android

#### Option 1: CANable (Best for PoC) ✅
- **Protocol:** slcan (Serial Line CAN)
- **Interface:** USB CDC (Virtual COM port)
- **Android Support:** ✅ Excellent - Uses standard USB serial
- **Firmware:** Open-source (https://github.com/normaldotcom/canable-fw)
- **Cost:** ~$40-60
- **Pros:**
  - Simple text-based protocol
  - USB CDC class (appears as serial port)
  - Android USB serial libraries available
  - Open-source, well-documented
- **Cons:**
  - Lower performance than binary protocols
  - Limited to 1 Mbps CAN bus speed

**Android Implementation:**
```kotlin
// Uses USB serial library
implementation("com.github.mik3y:usb-serial-for-android:3.5.1")

val driver = UsbSerialProber.getDefaultProber().probeDevice(usbDevice)
val port = driver.ports[0]
port.open(connection)
port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

// Send slcan commands
port.write("O\r".toByteArray()) // Open CAN
port.write("S6\r".toByteArray()) // Set 500 kbps

// Read CAN frames
val buffer = ByteArray(128)
val len = port.read(buffer, 1000)
val frame = parseSlcanFrame(String(buffer, 0, len))
```

#### Option 2: CANUSB (Lawicel) ✅
- **Protocol:** ASCII protocol (similar to slcan)
- **Interface:** USB CDC
- **Android Support:** ✅ Good - USB serial compatible
- **Cost:** ~$150-200
- **Pros:**
  - Established protocol
  - Good documentation
  - Industrial grade
- **Cons:**
  - More expensive than CANable
  - ASCII protocol has overhead

#### Option 3: ELM327 (OBD-II) ⚠️
- **Protocol:** AT commands
- **Interface:** USB serial or Bluetooth
- **Android Support:** ✅ Excellent - Many libraries available
- **Cost:** ~$20-50
- **Pros:**
  - Very well supported on Android
  - Cheap and widely available
  - Bluetooth variants available
- **Cons:**
  - ❌ OBD-II focused (limited to vehicle protocols)
  - ❌ Can't access arbitrary CAN messages
  - ❌ Filtered/interpreted data only
  - ❌ Not suitable for general CAN monitoring

**Verdict:** ❌ Not recommended for general CAN bus monitoring

---

## 4. Technical Implementation

### 4.1 USB Serial Communication

**Recommended Library:**
- **usb-serial-for-android** (https://github.com/mik3y/usb-serial-for-android)
- Supports CDC, FTDI, CP210x, CH34x, PL2303 drivers
- **MIT License** (GPL-compatible)
- Active maintenance
- Well-documented

**License Compatibility Analysis:**

The usb-serial-for-android library uses the **MIT License**, which is highly permissive and compatible with GPL licensing:

✅ **MIT + GPLv3:** Fully compatible
- MIT-licensed code can be included in GPLv3 projects
- The combined work must be distributed under GPLv3
- The MIT library itself remains under MIT license

✅ **MIT + LGPLv3:** Fully compatible  
- MIT-licensed code can be included in LGPLv3 projects
- The combined work follows LGPLv3 terms
- The MIT library itself remains under MIT license

✅ **MIT + GPLv2:** Also compatible
- Unlike Apache 2.0, MIT is compatible with GPLv2

**Conclusion:** ✅ If this project adopts GPLv3 or LGPLv3 licensing, the usb-serial-for-android library (MIT) can be used without any licensing conflicts. The MIT license is one of the most permissive and GPL-friendly licenses available.

**Integration:**
```kotlin
// Add dependency
dependencies {
    implementation("com.github.mik3y:usb-serial-for-android:3.5.1")
}

// Manifest permissions
<uses-permission android:name="android.permission.USB_PERMISSION" />
<uses-feature android:name="android.hardware.usb.host" />
```

### 4.2 Protocol Implementation (slcan example)

**slcan Protocol:**
- Text-based protocol for CAN communication
- Commands sent as ASCII strings
- CAN frames encoded as hex strings

**Frame Format:**
```
Standard Frame: tiiildd...
Extended Frame: Tiiiiiiiildd...

t/T = standard/extended frame
iii/iiiiiiii = CAN ID (hex)
l = data length (0-8)
dd... = data bytes (hex)
```

**Example:**
```
t1230412345678  → CAN ID 0x123, DLC 4, Data: 0x12 0x34 0x56 0x78
T000001FF812345678DEADBEEF → Extended ID 0x1FF, DLC 8, Data: ...
```

**Implementation:**
```kotlin
class SlcanFrameProvider(
    private val usbPort: UsbSerialPort
) : RawFrameProvider {
    
    override fun observeFrames(): Flow<RawCanFrame> = flow {
        val buffer = ByteArray(256)
        
        while (isActive) {
            val len = usbPort.read(buffer, 100)
            if (len > 0) {
                val message = String(buffer, 0, len)
                val frames = parseSlcanMessages(message)
                frames.forEach { emit(it) }
            }
        }
    }
    
    private fun parseSlcanMessages(data: String): List<RawCanFrame> {
        return data.split('\r')
            .filter { it.startsWith('t') || it.startsWith('T') }
            .mapNotNull { parseSlcanFrame(it) }
    }
    
    private fun parseSlcanFrame(slcan: String): RawCanFrame? {
        if (slcan.isEmpty()) return null
        
        val isExtended = slcan[0] == 'T'
        val idLen = if (isExtended) 8 else 3
        
        val canId = slcan.substring(1, 1 + idLen).toInt(16)
        val dlc = slcan[1 + idLen].digitToInt()
        val dataStart = 2 + idLen
        
        val payload = ByteArray(dlc) { i ->
            slcan.substring(dataStart + i * 2, dataStart + i * 2 + 2)
                .toInt(16).toByte()
        }
        
        return RawCanFrame(
            timestamp = System.currentTimeMillis() * 1000, // Convert to micros
            canId = canId,
            dlc = dlc,
            payload = payload
        )
    }
}
```

### 4.3 Initialization Sequence

```kotlin
class UsbCanManager(private val context: Context) {
    
    suspend fun initialize(device: UsbDevice): UsbCanFrameProvider {
        // 1. Get USB manager
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        // 2. Request permission
        if (!usbManager.hasPermission(device)) {
            requestPermission(device)
            // Wait for permission grant
        }
        
        // 3. Open connection
        val connection = usbManager.openDevice(device)
            ?: throw IOException("Failed to open USB device")
        
        // 4. Find serial driver
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            ?: throw IOException("No compatible driver found")
        
        // 5. Open port
        val port = driver.ports[0]
        port.open(connection)
        port.setParameters(
            baudRate = 115200,
            dataBits = 8,
            stopBits = UsbSerialPort.STOPBITS_1,
            parity = UsbSerialPort.PARITY_NONE
        )
        
        // 6. Configure CAN adapter (slcan example)
        port.write("C\r".toByteArray()) // Close CAN (if open)
        delay(100)
        port.write("S6\r".toByteArray()) // 500 kbps
        delay(100)
        port.write("O\r".toByteArray()) // Open CAN
        delay(100)
        
        return UsbCanFrameProvider(port)
    }
}
```

---

## 5. Challenges and Limitations

### 5.1 Device Compatibility

**Challenge:** Not all Android devices support USB Host
- Tablets: Usually support USB Host
- Phones: Mixed support, especially budget devices
- Check: `context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)`

**Mitigation:**
- Declare USB Host feature as required in manifest
- Test on target devices before deployment
- Consider Bluetooth CAN adapters as alternative

### 5.2 Power Supply

**Challenge:** USB CAN adapters require power, but phones may not provide sufficient current
- Most phones provide 100-500 mA
- Some adapters need 500 mA or more
- CAN bus itself may draw power

**Mitigation:**
- Use powered USB hub
- Choose low-power adapters
- Use devices with OTG power support

### 5.3 Driver Availability

**Challenge:** Proprietary protocols require custom drivers
- PCAN, Kvaser use binary protocols
- No official Android SDKs available
- Reverse engineering required

**Mitigation:**
- Use adapters with open protocols (slcan, ASCII)
- Choose USB CDC class adapters (appear as serial ports)
- Leverage existing Android USB serial libraries

### 5.4 Real-Time Performance

**Challenge:** Android is not a real-time OS
- Garbage collection can cause delays
- Thread scheduling is non-deterministic
- USB polling has latency

**Mitigation:**
- Use Flow with appropriate buffering
- Accept that some frames may be missed
- For PoC, file replay is sufficient for validation

### 5.5 Permission Management

**Challenge:** Users must grant USB device permission
- Permission dialog shown on connection
- Permission must be granted each time (unless remembered)
- App can't access device without permission

**Mitigation:**
- Handle permission requests gracefully
- Provide clear user instructions
- Use PendingIntent for permission requests

---

## 6. Alternative Solutions

### 6.1 Bluetooth CAN Adapters

**Pros:**
- No USB cable required
- Better device compatibility
- Easier power management

**Cons:**
- Limited bandwidth (Classic Bluetooth: ~1 Mbps, BLE: ~1 Mbps)
- Additional pairing complexity
- Potential latency issues

**Recommendation:** Consider for production if USB proves problematic

### 6.2 WiFi CAN Gateways

**Pros:**
- Network-based, very flexible
- Can support multiple clients
- No Android-specific hardware dependencies

**Cons:**
- Requires WiFi infrastructure
- More complex setup
- Higher cost

**Recommendation:** Good for fixed installations, not mobile use

### 6.3 Raspberry Pi Bridge

**Pros:**
- Full Linux environment
- Excellent CAN support (SocketCAN)
- Can pre-process data

**Cons:**
- Additional hardware complexity
- Requires separate device
- Power and size considerations

**Recommendation:** Good for development/testing, overkill for production

---

## 7. Recommended PoC Approach

### Phase 1: PoC (Current) ✅
**Data Source:** ASC files (file replay)
- **Pros:** Zero hardware dependencies, perfect for architecture validation
- **Implementation:** Completed in PoC
- **Status:** ✅ Sufficient for PoC success criteria

### Phase 2: Hardware Validation (Next)
**Data Source:** USB CAN adapter (CANable with slcan)
- **Adapter:** CANable ($40-60)
- **Protocol:** slcan (simple, well-documented)
- **Library:** usb-serial-for-android
- **Implementation Effort:** ~2-3 days
- **Risk:** Low - well-established technology stack

### Phase 3: Production (Future)
**Enhancements:**
- Support multiple adapter types
- Bluetooth CAN option
- Automatic adapter detection
- Advanced error handling and recovery

---

## 8. Implementation Roadmap

### Immediate (PoC)
- ✅ Architecture supports RawFrameProvider abstraction
- ✅ No hardware dependencies in core logic
- ✅ File-based testing validates architecture

### Short-Term (Post-PoC)
1. Acquire CANable USB CAN adapter
2. Implement `UsbCanFrameProvider` with slcan protocol
3. Test on 2-3 Android devices with USB Host support
4. Validate frame rates and latency
5. Document device compatibility matrix

### Long-Term (Production)
1. Support multiple adapter protocols
2. Add Bluetooth CAN adapter support
3. Implement automatic protocol detection
4. Create adapter configuration UI
5. Add adapter firmware update capability

---

## 9. Cost Analysis

### Development Costs
- **CANable Adapter:** $50
- **Test CAN Bus Setup:** $100-200 (cables, terminators, power)
- **Android Test Devices:** $0 (use existing devices)
- **Development Time:** ~3-5 days

### Production Costs (per unit)
- **CAN Adapter:** $40-150 (depending on model)
- **USB OTG Cable:** $5-10
- **Bluetooth Adapter Alternative:** $60-120

---

## 10. Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| Device lacks USB Host | Medium | High | Test on target devices, consider Bluetooth |
| Insufficient power | Medium | Medium | Use powered hub or low-power adapter |
| Protocol compatibility | Low | Medium | Use standard slcan/CDC adapters |
| Real-time performance | Low | Low | Accept best-effort, not hard real-time |
| Driver availability | Low | High | Use CDC class adapters with existing libs |

**Overall Risk:** ⚠️ **Low to Medium** - Mitigable with proper hardware selection

---

## 11. Conclusion

### Feasibility Assessment: ✅ FEASIBLE

**Summary:**
- USB CAN adapters CAN work on Android via USB Host API
- CANable with slcan protocol is the recommended starting point
- File-based PoC correctly validates the architecture
- Live acquisition is feasible but requires hardware investment

**Recommendations:**

1. **For PoC:** ✅ Continue with ASC file approach
   - Sufficient for architecture validation
   - Zero hardware risk
   - Fast iteration

2. **For Next Phase:** Implement USB CAN support
   - Hardware: CANable ($50)
   - Protocol: slcan
   - Library: usb-serial-for-android
   - Effort: 3-5 days

3. **For Production:** Evaluate Bluetooth option
   - Better device compatibility
   - No cable required
   - Good user experience

**GO/NO-GO Decision:** ✅ **GO** - Live CAN acquisition on Android is viable with appropriate hardware selection.

---

## 12. References

### Android Documentation
- [USB Host API Guide](https://developer.android.com/guide/topics/connectivity/usb/host)
- [USB Host API Reference](https://developer.android.com/reference/android/hardware/usb/package-summary)

### Libraries
- [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)

### Hardware
- [CANable](https://canable.io/)
- [slcan Protocol Specification](https://www.can232.com/docs/canusb_manual.pdf)

### CAN Bus Resources
- [SocketCAN Documentation](https://www.kernel.org/doc/Documentation/networking/can.txt)
- [CAN Bus Explained](https://www.csselectronics.com/pages/can-bus-simple-intro-tutorial)

---

**Document Version:** 1.0  
**Last Updated:** January 6, 2026  
**Author:** CAN2Android PoC Team

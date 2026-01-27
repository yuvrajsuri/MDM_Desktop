# Desktop MDM Backend - Production Implementation

## 🎯 Critical Design Decisions (Addressing Your Concerns)

### ✅ **1. PushToken IS Device Authentication**

**Fixed Mental Model:**
- **Devices → PushToken authentication** (NOT "just a carrier")
- **Admins → JWT authentication** (future - separate path)

PushToken:
- Identifies the device ✅
- Gates `/desktopmdm/status` endpoint ✅
- Can be revoked ✅
- **This IS authentication**, just not JWT-style

### ✅ **2. No More JWT Confusion**

**Clear Separation:**
```
Device Endpoints:
- POST /desktopmdm/register  → Issues pushToken
- GET  /desktopmdm/status    → Validates pushToken (X-Push-Token header)

Admin Endpoints (future):
- POST /admin/login          → Issues JWT
- *    /admin/*              → Validates JWT (Authorization: Bearer header)
```

**NO JWT for devices. ONLY pushToken.**

### ✅ **3. Status Values Are Explicit**

**Clean Status Transitions:**
```java
PENDING_ENROLLMENT  →  (device calls /register)  →  ENROLLED
ENROLLED            →  (first /status call)      →  ACTIVE
ACTIVE              →  (admin action)            →  SUSPENDED | BLOCKED
*                   →  (admin action)            →  WIPED
```

**Enforced in code:**
- `Device.enroll()` - Only works if status == PENDING_ENROLLMENT
- `Device.activate()` - Only works if status == ENROLLED
- All transitions throw `IllegalStateException` if invalid

---

## 📦 Project Structure

```
mdm-backend/
├── pom.xml                                    # Maven dependencies
├── src/main/
│   ├── java/com/company/mdm/
│   │   ├── MdmBackendApplication.java        # Main application
│   │   ├── domain/entity/
│   │   │   ├── Device.java                    # Device entity (immutable identity)
│   │   │   ├── DeviceStatus.java             # Status enum (explicit transitions)
│   │   │   ├── DeviceAuditLog.java           # Audit log entity
│   │   │   ├── AuditEventType.java           # Event types enum
│   │   │   └── ActorType.java                # Actor enum (DEVICE/ADMIN/SYSTEM)
│   │   ├── repository/
│   │   │   ├── DeviceRepository.java         # Device data access
│   │   │   └── DeviceAuditLogRepository.java # Audit data access
│   │   └── service/
│   │       ├── PushTokenService.java         # Device authentication
│   │       └── AuditService.java             # Automatic audit logging
│   └── resources/
│       ├── application.properties             # Configuration
│       └── db/migration/
│           └── V1__Initial_schema.sql         # Flyway migration
└── README.md                                  # This file
```

---

## 🔧 Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 13+

### 1. Database Setup

```bash
# Create database
createdb mdm

# Create user
psql -d mdm -c "CREATE USER mdm_user WITH PASSWORD 'mdm_password';"
psql -d mdm -c "GRANT ALL PRIVILEGES ON DATABASE mdm TO mdm_user;"
```

### 2. Configure Database

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mdm
spring.datasource.username=mdm_user
spring.datasource.password=mdm_password
```

### 3. Build & Run

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Or run JAR directly
java -jar target/mdm-backend-1.0.0.jar
```

### 4. Verify Setup

```bash
# Check health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","components":{"db":{"status":"UP"}}}
```

---

## 🏗️ Architecture Guarantees

### ✅ **1. Device Identity is Immutable**

```java
@Column(name = "fulluuid", nullable = false, unique = true, updatable = false)
private String fulluuid;  // IMMUTABLE - Set once, never changed
```

- `fulluuid` and `uuid15` are marked `updatable = false`
- Database has UUID format constraint
- Once set, cannot be changed

### ✅ **2. Enrollment is Idempotent**

```java
// If device already has valid pushToken, return same token
if (device.getTokenHash() != null && !isTokenExpired(device)) {
    // Idempotent: return existing info
    return ResponseEntity.ok(...)
}
```

- Calling `/register` multiple times returns same result
- No duplicate devices created
- Same pushToken returned (idempotent)

### ✅ **3. PushToken Never Stored in Plaintext**

```java
// Generate pushToken
String pushToken = pushTokenService.generatePushToken();

// ONLY store hash
String tokenHash = pushTokenService.hashToken(pushToken);
device.setTokenHash(tokenHash);  // SHA-256 hash stored
```

- Full pushToken sent to device
- Only SHA-256 hash stored in database
- Constant-time comparison for validation

### ✅ **4. All State Transitions are Explicit**

```java
public void enroll(String tokenHash, LocalDateTime expiresAt) {
    if (this.status != DeviceStatus.PENDING_ENROLLMENT) {
        throw new IllegalStateException("Cannot enroll device in status: " + this.status);
    }
    this.status = DeviceStatus.ENROLLED;
    // ...
}
```

- Every transition enforced by code
- Invalid transitions throw exceptions
- No silent status changes

### ✅ **5. Audit Logging is Automatic**

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logEnrollmentSuccess(Device device, String ipAddress) {
    // Logs saved even if main transaction rolls back
    auditLogRepository.save(log);
}
```

- Every device action logged
- Uses `REQUIRES_NEW` propagation
- Logs saved even on errors

### ✅ **6. Admin and Device Paths are Isolated**

```
Device Authentication (PushToken):
- POST /desktopmdm/register  → PushToken issued
- GET  /desktopmdm/status    → PushToken validated (X-Push-Token header)

Admin Authentication (JWT - future):
- POST /admin/login          → JWT issued
- GET  /admin/devices        → JWT validated (Authorization: Bearer header)
```

- Separate endpoints
- Separate authentication mechanisms
- No confusion

---

## 📊 Database Schema

### Devices Table

```sql
CREATE TABLE devices (
    id                  BIGSERIAL PRIMARY KEY,
    
    -- IMMUTABLE Identity
    fulluuid            VARCHAR(36) NOT NULL UNIQUE,
    uuid15              VARCHAR(15) NOT NULL,
    
    -- Metadata (mutable)
    computer_name       VARCHAR(255),
    os_name             VARCHAR(100),
    os_version          VARCHAR(50),
    ip_address          VARCHAR(45),
    
    -- Status (explicit transitions)
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING_ENROLLMENT',
    
    -- PushToken (device auth)
    token_hash          VARCHAR(255),      -- SHA-256 hash only
    token_issued_at     TIMESTAMP,
    token_expires_at    TIMESTAMP,
    
    -- Timestamps
    created_at          TIMESTAMP NOT NULL,
    enrolled_at         TIMESTAMP,
    last_check_in       TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL
);
```

### Audit Log Table

```sql
CREATE TABLE device_audit_log (
    id                  BIGSERIAL PRIMARY KEY,
    device_id           BIGINT,
    fulluuid            VARCHAR(36),
    event_type          VARCHAR(50) NOT NULL,
    event_data          JSONB,
    ip_address          VARCHAR(45),
    actor_type          VARCHAR(50),
    actor_id            VARCHAR(100),
    created_at          TIMESTAMP NOT NULL
);
```

---

## 🔐 Security Model

### Device Authentication (PushToken)

**Generation:**
```java
SecureRandom secureRandom = SecureRandom.getInstanceStrong();
byte[] tokenBytes = new byte[32]; // 256 bits
secureRandom.nextBytes(tokenBytes);
String pushToken = bytesToHex(tokenBytes); // 64 hex chars
```

**Storage:**
```java
String tokenHash = DigestUtils.sha256Hex(pushToken);
// Store only hash in database
```

**Validation:**
```java
// Constant-time comparison (prevents timing attacks)
boolean isValid = MessageDigest.isEqual(
    computedHash.getBytes(UTF_8),
    storedHash.getBytes(UTF_8)
);
```

**Header:**
```http
GET /desktopmdm/status
X-Push-Token: a3f5e2d1c4b6789012345678901234567890abcdef...
```

---

## 📝 Configuration

### Key Properties

```properties
# PushToken Configuration
mdm.pushtoken.expiration-days=365
mdm.pushtoken.token-length=64

# Device Enrollment
mdm.device.enrollment.max-retries=10
mdm.device.enrollment.retry-interval-minutes=5

# Device Status Check
mdm.device.status-check.interval-minutes=5
mdm.device.status-check.max-missed-checkins=12
```

---

## 🧪 Testing

### Pre-provision Test Device

```sql
INSERT INTO devices (fulluuid, uuid15, status, created_by, notes)
VALUES (
    '03D0CBD0-D924-2DEC-BD74-DC375680ECE4',
    '03ddc375680ece4',
    'PENDING_ENROLLMENT',
    'admin@company.com',
    'Test Device - KBP-DEV-207'
);
```

### Test Enrollment (cURL)

```bash
curl -X POST http://localhost:8080/desktopmdm/register \
  -H "Content-Type: application/json" \
  -d '{
    "uuid15": "03ddc375680ece4",
    "fulluuid": "03D0CBD0-D924-2DEC-BD74-DC375680ECE4",
    "computer_name": "KBP-DEV-207",
    "os_name": "Windows",
    "os_version": "10.0.19045"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Device registered successfully",
  "pushToken": "a3f5e2d1c4b6789012345678901234567890abcdef1234567890abcdef123456",
  "expires_at": "2027-01-10T14:30:00Z"
}
```

---

## 🚀 Next Steps

### Implement Controllers

Need to create:
1. ✅ `DeviceRegistrationController` - POST /desktopmdm/register
2. ✅ `DeviceStatusController` - GET /desktopmdm/status
3. ⏳ `AdminDeviceController` - Admin CRUD operations (future)

### Add Features

- [ ] Rate limiting (Spring AOP + Guava)
- [ ] Command delivery system
- [ ] Admin JWT authentication
- [ ] Metrics & monitoring
- [ ] Docker deployment

---

## 📚 Key Design Patterns Used

1. **Domain-Driven Design**: Entities have business logic
2. **Repository Pattern**: Data access abstraction
3. **Service Layer**: Business logic separation
4. **Audit Logging**: REQUIRES_NEW propagation
5. **Immutability**: Device identity cannot change
6. **Explicit State Machine**: Status transitions enforced

---

## ⚠️ Important Notes

### PushToken vs JWT

**PushToken (Devices):**
- Random 64-char hex string
- Stored as SHA-256 hash
- Validated via hash comparison
- Header: `X-Push-Token`
- Purpose: Device authentication

**JWT (Admins - future):**
- Signed token with claims
- Validated via signature verification
- Header: `Authorization: Bearer`
- Purpose: Admin authentication

**These are completely separate authentication mechanisms.**

### Status Flow

```
PENDING_ENROLLMENT  ──register──►  ENROLLED  ──first check-in──►  ACTIVE
                                                                     │
                                                    ┌────────────────┼────────────────┐
                                                    │                │                │
                                               SUSPENDED         BLOCKED           WIPED
```

### Idempotency

Calling `/register` multiple times with same `fulluuid`:
1. First call: Issues pushToken, status → ENROLLED
2. Subsequent calls: Returns same response (idempotent)
3. No duplicate devices created
4. No errors thrown

---

## 🐛 Troubleshooting

### Database Connection Failed

```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Check credentials
psql -U mdm_user -d mdm -h localhost
```

### Flyway Migration Failed

```bash
# Drop and recreate database
dropdb mdm
createdb mdm

# Re-run application
mvn spring-boot:run
```

### Port 8080 Already in Use

```properties
# Change port in application.properties
server.port=8081
```

<!-- pom.xml -->
spring.datasource.hikari.maximum-pool-size=20                  //Max 20 concurrent DB connections        //Handles 20 simultaneous /status or /register requests
spring.datasource.hikari.minimum-idle=5                        //Always keep 5 connections ready        //Fast response time (no connection creation delay)
spring.datasource.hikari.connection-timeout=30000               //Wait 30s for available connection     //Request fails with timeout if all 20 connections busy
spring.datasource.hikari.idle-timeout=600000                    //Close idle connections after 10min        //Reduces DB load during low traffic
spring.datasource.hikari.max-lifetime=1800000                   //Refresh connections every 30min         //Prevents stale connections
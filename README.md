# GhostSim - Concurrent Banking Simulation

GhostSim is a multi-threaded banking simulation written in Java 21 that demonstrates object-oriented design, deterministic deadlock prevention, thread-safe concurrent transfers, worker thread lifecycle management, and a responsive Java Swing user interface.

## Architecture and Project Structure

GhostSim follows a clean separation of concerns across domain models, business services, simulation controllers, and UI presentation layers:

- **`com.banking.model`**
  - `Account`: Encapsulates account identity and balance with atomic deposit/withdraw operations and validation.
  - `Bank`: Manages an in-memory collection of accounts indexed by account ID.
  - `Transaction`: Immutable record representing a financial transfer between source and destination accounts.

- **`com.banking.service`**
  - `BankService`: Domain service providing high-level banking operations (account creation, lookup, deposit, withdrawal, single-threaded transfer).
  - `TransactionService`: Service recording transfer history and managing transaction objects.
  - `ConcurrentTransferService`: Thread-safe transfer service preventing deadlocks through deterministic lock ordering (lexicographical ID comparison with identity hashcode fallback).

- **`com.banking.simulation`**
  - `SimulationController`: Coordinates background worker threads (`TransferWorker-X`) across lifecycle states (`STOPPED`, `RUNNING`, `PAUSED`) using monitor synchronization (`wait`/`notifyAll`).

- **`com.banking.ui`**
  - `SimulationFrame`: Java Swing GUI titled `"GhostSim - Concurrent Banking Simulation"`. Dispatches background work off the Event Dispatch Thread (EDT) and updates UI components safely via `SwingUtilities.invokeLater`.

---

## Concurrency & Deadlock Prevention

### Locking Strategy in `ConcurrentTransferService`
When multiple threads concurrently transfer funds between arbitrary accounts (e.g., Thread 1 transfers A -> B while Thread 2 transfers B -> A), circular wait deadlocks can occur if locks are acquired arbitrarily.

GhostSim prevents deadlocks by enforcing a **deterministic global lock order**:
1. Account IDs are compared lexicographically (`source.getId().compareTo(destination.getId())`).
2. The account with the lower ID is always locked first, followed by the account with the higher ID:
   ```java
   synchronized (firstLock) {
       synchronized (secondLock) {
           performTransfer(source, destination, amount);
       }
   }
   ```
3. If account IDs are identical in value (different instances sharing ID or collisions), `System.identityHashCode(account)` is used as a secondary deterministic tie-breaker. If identity hash codes also collide, a static tie-breaking class lock is acquired.

### Simulation Lifecycle Coordination in `SimulationController`
- **State Management**: Governed by `SimulationState` (`STOPPED`, `RUNNING`, `PAUSED`) and guarded by `stateLock`.
- **START**: Instantiates worker threads and transitions state to `RUNNING`.
- **PAUSE**: Sets state to `PAUSED`. Worker threads encounter `stateLock.wait()` in their execution loop and freeze without terminating.
- **RESUME**: Sets state to `RUNNING` and executes `stateLock.notifyAll()`, waking workers to resume transfers.
- **STOP**: Sets state to `STOPPED`, interrupts worker threads to break any sleeping/waiting states, calls `thread.join()` to ensure clean termination, and releases all thread references.

---

## Swing User Interface

The GUI is encapsulated in `SimulationFrame` and provides:
- **START Button**: Starts the simulation worker threads. Enabled only when `STOPPED`.
- **PAUSE Button**: Pauses transfer operations. Enabled only when `RUNNING`.
- **RESUME Button**: Resumes transfer operations. Enabled only when `PAUSED`.
- **STOP Button**: Halts workers and joins threads on a background worker thread (preventing EDT UI freeze). Enabled when `RUNNING` or `PAUSED`.
- **Status Label**: Displays the current status (`STOPPED`, `RUNNING`, `PAUSED`).
- **Activity Log Area**: Displays real-time transfer logs and system balance conservation stats.

---

## Building and Running

### Prerequisites
- Java Development Kit (JDK) 21+
- Apache Maven 3.8+

### Build and Run Tests
```powershell
mvn clean test
```

### Launch the GUI Application
```powershell
mvn compile exec:java -Dexec.mainClass="com.banking.ui.SimulationFrame"
```
or run `com.banking.ui.SimulationFrame` directly from your IDE.

---

## Verification

The project includes an automated JUnit 5 test suite verifying all services, concurrency invariants, controller states, and UI interactions:
- **`BankServiceTest`** (5 tests): Account creation, lookup, deposit, withdrawal, validation.
- **`TransactionServiceTest`** (5 tests): Transfer processing, transaction logging, input validation.
- **`ConcurrentTransferServiceTest`** (6 tests): Thread safety, conservation of total money under concurrent transfer loads, deadlock freedom, validation.
- **`SimulationControllerTest`** (11 tests): State transitions, worker coordination, pause/resume synchronization, clean thread stop/join, restartability.
- **`SimulationFrameTest`** (6 tests): Frame layout, button state management, EDT dispatching, clean window closing.

**Test Results**: 33 tests run, 33 passed, 0 failed, 0 errors, Maven BUILD SUCCESS.

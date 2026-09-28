# CMSC 335 Course Alignment & Architectural Specification

This document details the object-oriented design, architectural patterns, concurrency mechanisms, graphical user interface implementation, and test verification within the **GhostSim** banking simulation.

---

## 1. Object-Oriented Programming (OOP) Principles

### Java Classes and Objects
GhostSim models real-world banking domain entities and simulation components as discrete, cohesive Java classes:
- **`Account`** (`com.banking.model.Account`): Represents an individual bank account identified by a unique ID and holding a monetary balance.
- **`Bank`** (`com.banking.model.Bank`): Represents the banking institution maintaining the collection of accounts.
- **`Transaction`** (`com.banking.model.Transaction`): Represents an immutable historical record of a monetary transfer between two accounts with a timestamp.
- **`BankService`**, **`TransactionService`**, **`ConcurrentTransferService`** (`com.banking.service`): Provide operations for managing accounts, recording transactions, and executing thread-safe transfers.
- **`SimulationController`** (`com.banking.simulation`): Coordinates worker thread lifecycles and simulation execution states.
- **`SimulationFrame`** (`com.banking.ui`): Encapsulates the desktop Swing GUI interface.

### Encapsulation
- **Data Hiding**: All member variables across domain models and controllers are declared `private` or `private final` (e.g., `Account.balance`, `Account.id`, `Bank.accounts`, `SimulationController.stateLock`, `SimulationController.state`).
- **Controlled Access**: Balance changes in `Account` cannot be made directly from external classes; they must go through validated `deposit(double amount)` and `withdraw(double amount)` methods.
- **Defensive Copying & Immutability**:
  - `Bank.getAccounts()` returns an unmodifiable view via `Collections.unmodifiableMap(accounts)`.
  - `TransactionService.getTransactionHistory()` returns an unmodifiable view via `Collections.unmodifiableList(transactionHistory)`.
  - `Transaction` fields are entirely `final`, guaranteeing immutability once instantiated.

### Abstraction
- **Business Logic Abstraction**: `BankService` exposes high-level banking operations (`depositMoney`, `withdrawMoney`, `transferMoney`) while abstracting internal validation, account retrieval, and balance adjustment.
- **Concurrency & Lifecycle Abstraction**: `SimulationController` provides clean lifecycle methods (`start()`, `pause()`, `resume()`, `stop()`) hiding complex worker thread spawning, thread interruption, join synchronization, and monitor wait/notify mechanisms from the UI layer.

### Inheritance and Classification
- **GUI Hierarchy**: `SimulationFrame extends JFrame`, inheriting window management, layout containers, and rendering behavior from the Swing/AWT hierarchy (`Component` -> `Container` -> `Window` -> `Frame` -> `JFrame`).
- **Adapter Inheritance**: `SimulationFrame` instantiates an anonymous inner subclass of `WindowAdapter` (`addWindowListener(new WindowAdapter() { ... })`) to cleanly intercept `windowClosing` events.
- **Enum Inheritance**: `SimulationController.SimulationState` implicitly extends `java.lang.Enum<SimulationState>`.
- **Root Inheritance**: All domain and service classes inherit from `java.lang.Object`, overriding fundamental methods.

### Polymorphism
- **Method Overriding (Dynamic Polymorphism)**:
  - `Account` and `Transaction` override `equals(Object o)`, `hashCode()`, and `toString()` from `java.lang.Object`, allowing polymorphic equality checks in Java collections.
  - `WindowAdapter.windowClosing(WindowEvent e)` is overridden in `SimulationFrame` to handle window lifecycle termination polymorphically.
- **Interface-Based Polymorphism**:
  - Passing lambda expressions implementing `java.lang.Runnable` to `Thread` constructors, `SimulationController`, and `SwingUtilities.invokeLater()`.
  - Passing `java.awt.event.ActionListener` lambdas to Swing `JButton.addActionListener(...)`.

### Composition
- **`Bank` has-a `Map<String, Account>`**: Aggregates `Account` objects within an internal hash map.
- **`BankService` has-a `Bank`**: Composes a `Bank` instance to perform domain operations.
- **`Transaction` has-a `Account` (Source and Destination)**: References source and target `Account` objects.
- **`SimulationFrame` has-a `SimulationController` and UI components (`JButton`, `JLabel`, `JTextArea`)**: Composes UI controls and simulation engine.

### Interfaces
- **`java.lang.Runnable`**: Implemented by transfer tasks executed by simulation worker threads and UI dispatch tasks.
- **`java.awt.event.ActionListener`**: Event listener interface implemented via lambdas on button clicks.
- **`java.awt.event.WindowListener`**: Intercepts window closing events.
- **`java.util.List` & `java.util.Map`**: Standard collection interfaces decoupling concrete implementations (`ArrayList`, `HashMap`).

### Constructors and Constructor Overloading
- **`Account`**:
  - `Account(String id, double balance)`: Main constructor with validation.
  - `Account(String id)`: Overloaded constructor chaining to `this(id, 0.0)`.
- **`Bank`**:
  - `Bank()`: Defaults bank name to `"Default Bank"`.
  - `Bank(String name)`: Configures custom bank name.
- **`Transaction`**:
  - `Transaction(String id, Account source, Account destination, double amount, LocalDateTime timestamp)`: Explicit constructor.
  - `Transaction(Account source, Account destination, double amount)`: Overloaded constructor generating a random UUID and timestamp via `this(...)`.
- **`BankService`**:
  - `BankService()`: Defaults to new `Bank()`.
  - `BankService(Bank bank)`: Injects an existing `Bank` instance.
- **`SimulationController`**:
  - `SimulationController()`: Default 5 workers.
  - `SimulationController(int workerCount)`
  - `SimulationController(int workerCount, Runnable defaultTask)`
  - `SimulationController(int workerCount, Runnable defaultTask, long delayMillis)`
  - `SimulationController(List<Runnable> tasks)`
  - `SimulationController(List<Runnable> tasks, long delayMillis)`
- **`SimulationFrame`**:
  - `SimulationFrame()`: Constructs frame with default simulation controller.
  - `SimulationFrame(SimulationController controller)`: Constructs frame with specified controller.

### Method Overriding
- **`Account.java`**:
  - `@Override public boolean equals(Object o)`: Compares account instances based on unique `id`.
  - `@Override public int hashCode()`: Computes hash based on `id`.
  - `@Override public String toString()`: Returns string representation of ID and balance.
- **`Transaction.java`**:
  - `@Override public boolean equals(Object o)`: Compares transactions by unique `id`.
  - `@Override public int hashCode()`: Computes hash based on `id`.
  - `@Override public String toString()`: Returns formatted transaction summary.
- **`SimulationFrame.java`**:
  - `@Override public void windowClosing(WindowEvent e)`: Cleans up running worker threads upon frame closing.

### Collections and Generics
- **`Map<String, Account>`**: In `Bank.java`, parameterized map storing accounts keyed by `String` IDs.
- **`List<Transaction>`**: In `TransactionService.java`, parameterized list storing transfer history.
- **`List<Thread>` & `List<Runnable>`**: In `SimulationController.java`, generic lists managing worker threads and tasks.
- **Immutability Wrappers**: Using `Collections.unmodifiableMap(...)` and `Collections.unmodifiableList(...)` to prevent direct mutation of internal collections.

### Exception Handling
- **Input & State Validation**: Throws `IllegalArgumentException` on invalid inputs (e.g., `null` account IDs, negative balances, negative or zero transfer amounts, self-transfers, or non-existent accounts).
- **Business Rule Enforcement**: Throws `IllegalArgumentException("Insufficient funds")` in `Account.withdraw` and `BankService.transfer` when funds are lacking.
- **Concurrency Interruption Handling**: Handles `InterruptedException` in `SimulationController.runWorkerLoop()` and `SimulationController.stop()`, properly restoring the thread interrupted flag via `Thread.currentThread().interrupt()`.

### Service/Repository Architecture
- **Repository/Store Layer (`Bank`)**: Encapsulates in-memory account storage and basic CRUD lookups.
- **Service Layer (`BankService`, `TransactionService`, `ConcurrentTransferService`)**: Implements business transactions, concurrency control, and domain invariant enforcement independently of storage representation.

---

## 2. Java Concurrency & Multi-Threading

### Runnable and Thread Management
- Simulation workers are created as named threads (`"TransferWorker-1"`, `"TransferWorker-2"`, etc.) running a worker loop (`runWorkerLoop`).
- Tasks are defined as `Runnable` instances representing money transfer operations between random bank accounts.

### Synchronization and Locking Strategy
In multi-threaded banking systems, concurrent transfers between shared accounts introduce race conditions and deadlocks. GhostSim employs a robust, deterministic locking strategy in `ConcurrentTransferService`:

1. **Deterministic Lock Ordering**:
   To prevent circular-wait deadlocks (e.g., Thread 1 transferring A $\rightarrow$ B while Thread 2 transfers B $\rightarrow$ A), account locks are acquired in a consistent lexicographical order:
   ```java
   int comparison = source.getId().compareTo(destination.getId());
   if (comparison < 0) {
       firstLock = source;
       secondLock = destination;
   } else if (comparison > 0) {
       firstLock = destination;
       secondLock = source;
   }
   ```
2. **Tie-Breaking Fallback**:
   If account IDs are identical (or in synthetic collision scenarios), the service falls back to `System.identityHashCode(account)`:
   ```java
   int hash1 = System.identityHashCode(source);
   int hash2 = System.identityHashCode(destination);
   if (hash1 < hash2) {
       firstLock = source;
       secondLock = destination;
   } else if (hash1 > hash2) {
       firstLock = destination;
       secondLock = source;
   } else {
       // Class-level lock tie breaker for extreme hash collision
       synchronized (ConcurrentTransferService.class) {
           synchronized (source) {
               synchronized (destination) {
                   performTransfer(source, destination, amount);
                   return;
               }
           }
       }
   }
   ```
3. **Atomic Transfer Execution**:
   Once both locks are acquired in order, the funds are withdrawn from the source and deposited into the destination atomically:
   ```java
   synchronized (firstLock) {
       synchronized (secondLock) {
           performTransfer(source, destination, amount);
       }
   }
   ```

### Race-Condition Prevention & Invariant Conservation
- Dual-account locking prevents any concurrent thread from observing or altering intermediate balance states.
- The total amount of money in the banking system is strictly conserved ($\sum \text{balances} = C$) across thousands of concurrent operations, verified by automated unit tests.

### Thread Lifecycle and State Coordination
The simulation lifecycle is managed in `SimulationController` using an explicit state machine (`SimulationState.STOPPED`, `SimulationState.RUNNING`, `SimulationState.PAUSED`) and monitor synchronization on `stateLock`:

- **START**:
  - Validates current state is `STOPPED`.
  - Changes state to `RUNNING`.
  - Spawns and starts worker threads (`workerThread.start()`).
- **PAUSE**:
  - Sets state to `PAUSED`.
  - Worker threads check state in `runWorkerLoop` and enter `stateLock.wait()` until notified.
  - Worker threads remain alive and preserve execution context.
- **RESUME**:
  - Sets state to `RUNNING`.
  - Wakes waiting worker threads via `stateLock.notifyAll()`.
- **STOP**:
  - Sets state to `STOPPED`.
  - Wakes any waiting threads via `stateLock.notifyAll()`.
  - Calls `thread.interrupt()` on all worker threads.
  - Calls `thread.join()` for each worker thread to guarantee complete, clean termination before returning.
  - Clears worker thread collections, allowing subsequent `start()` calls.

---

## 3. Java Swing Graphical User Interface (GUI)

### GUI Architecture and Components (`SimulationFrame`)
- **Title**: `"GhostSim - Concurrent Banking Simulation"`
- **Controls**:
  - **START Button**: Transitions simulation from `STOPPED` to `RUNNING`. Enabled only when stopped.
  - **PAUSE Button**: Pauses active transfers. Enabled only when running.
  - **RESUME Button**: Resumes paused transfers. Enabled only when paused.
  - **STOP Button**: Halts and terminates worker threads. Enabled when running or paused.
- **Status Indicator**: `JLabel` displaying `"Status: STOPPED"`, `"Status: RUNNING"`, or `"Status: PAUSED"`.
- **Activity Log Area**: `JTextArea` wrapped in `JScrollPane` displaying real-time transfer activity, account balances, and total bank balance invariants.

### Event-Driven Programming
- Button clicks trigger `ActionListener` callbacks bound to controller methods (`onStart`, `onPause`, `onResume`, `onStop`).
- Window closing event triggers graceful background controller shutdown.

### Swing Event Dispatch Thread (EDT) Safety
- **GUI Launch**: Launched on the EDT via `SwingUtilities.invokeLater(() -> new SimulationFrame().setVisible(true))`.
- **UI State Updates**: All label changes, button enablement updates, and log appends are marshaled onto the EDT via `SwingUtilities.invokeLater` (or executed directly if already on EDT).
- **Non-Blocking Controller Operations**: The `STOP` action initiates worker thread joins on a separate background thread to ensure the Swing UI remains responsive and does not freeze the EDT.

---

## 4. Build System & Test Automation

### Maven Configuration (`pom.xml`)
- Java Version: Source and target configured to Java 21 (`<maven.compiler.source>21</maven.compiler.source>`).
- Dependencies: JUnit Jupiter 5.10.2 (`org.junit.jupiter:junit-jupiter`).
- Plugins: Apache Maven Surefire Plugin 3.2.5 (`maven-surefire-plugin`).

---

## 5. Verification

The test suite thoroughly verifies domain logic, multi-threaded balance integrity, deadlock freedom, state machine transitions, and GUI component behavior.

### Test Suites
1. **`BankServiceTest`** (5 tests)
   - Account creation and ID lookups
   - Deposit and withdrawal logic
   - Transfer logic
   - Validation for negative amounts and null/blank IDs
   - Insufficient funds and non-existent account handling

2. **`TransactionServiceTest`** (5 tests)
   - Successful transfer execution and balance deduction
   - Transaction object instantiation, UUID generation, and timestamping
   - Zero and negative transfer amount rejection
   - Insufficient funds rejection
   - Null source/destination account rejection

3. **`ConcurrentTransferServiceTest`** (6 tests)
   - Normal single-threaded transfer
   - Bidirectional concurrent transfers between two accounts (deadlock prevention verification)
   - Highly concurrent multi-account transfer stress test (verifying strict conservation of total bank balance)
   - Invalid and negative transfer amounts
   - Self-transfer rejection
   - Insufficient funds handling

4. **`SimulationControllerTest`** (11 tests)
   - Initial `STOPPED` state verification
   - `start()` transition to `RUNNING`
   - `pause()` transition to `PAUSED`
   - `resume()` transition back to `RUNNING`
   - `stop()` transition to `STOPPED`
   - Worker execution validation during running state
   - Worker execution freeze validation during paused state
   - Clean worker thread termination upon `stop()`
   - Clean restartability (`start()` $\rightarrow$ `stop()` $\rightarrow$ `start()`)
   - Idempotent handling of repeated lifecycle calls
   - Custom worker tasks execution

5. **`SimulationFrameTest`** (6 tests)
   - Frame title and UI component initialization
   - Initial button enablement states (`START` enabled, `PAUSE`/`RESUME`/`STOP` disabled)
   - Start button click action and state transition
   - Pause and resume button click actions and state transitions
   - Stop button click action and background worker termination
   - Default constructor and log area rendering

### Maven Build & Test Results
- **Command Executed**: `mvn clean test`
- **Tests Run**: 33
- **Passed**: 33
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Result**: `BUILD SUCCESS`

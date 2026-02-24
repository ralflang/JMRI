package apps;

import jmri.jmrix.SerialPort;
import jmri.jmrix.jserialcomm.JSerialPort;
import jmri.jmrix.AbstractSerialPortController;
import jmri.jmrix.marklin.MarklinMessageFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standalone CLI program that bridges MCAN (Märklin CAN) serial adapters to TCP/IP.
 * <p>
 * This bridge creates a TCP multiplexer for MCAN protocol devices, allowing multiple JMRI
 * instances and other software to connect simultaneously to serial MCAN adapters
 * (such as CC-Schnitte by CanDigitalBahn) over TCP/IP using the CS2 Ethernet protocol.
 * <p>
 * Supports:
 * - Single or multiple serial devices simultaneously
 * - Custom TCP port per device
 * - Auto-increment TCP ports for multiple devices
 * - Message queuing to prevent command collisions from multiple clients
 * - Specific IP binding for security
 * <p>
 * Usage:
 * <pre>
 * # Single device
 * java -jar mcan-tcp-bridge.jar --port COM3
 * java -jar mcan-tcp-bridge.jar --port COM3:50000
 *
 * # Multiple devices
 * java -jar mcan-tcp-bridge.jar --port COM2,COM3:49999,COM4:50000
 * # COM2 gets default port 15731
 * # COM3 gets explicit port 49999
 * # COM4 gets explicit port 50000
 * </pre>
 *
 * @author Ralf Lang (ralf.lang@ralf-lang.de) Copyright (C) 2026
 * @see <a href="https://www.maerklin.de/fileadmin/media/produkte/CS2_can-protokoll_1-0.pdf">CS2 CAN Protocol</a>
 */
public class McanTcpBridge {

    private static final Logger log = LoggerFactory.getLogger(McanTcpBridge.class);

    // Default configuration
    private static final int DEFAULT_TCP_PORT = 15731; // Standard Märklin CS2 TCP port
    private static final int DEFAULT_BAUD_RATE = 500000; // CC-Schnitte (third-party) uses 500k baud
    private static final Pattern PORT_PATTERN = Pattern.compile("^([^:]+)(?::([^:]+))?(?::([0-9]+))?$");

    private static final List<BridgeInstance> bridges = new ArrayList<>();
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static boolean monitorMode = false; // Message logging to stdout

    /**
     * Device specification: serial port name + optional bind address + TCP port.
     */
    static class DeviceSpec {
        final String serialPort;
        final String bindAddress; // null means bind to all interfaces (0.0.0.0)
        final int tcpPort;

        DeviceSpec(String serialPort, String bindAddress, int tcpPort) {
            this.serialPort = serialPort;
            this.bindAddress = bindAddress;
            this.tcpPort = tcpPort;
        }

        @Override
        public String toString() {
            if (bindAddress != null) {
                return serialPort + " → " + bindAddress + ":" + tcpPort;
            }
            return serialPort + ":" + tcpPort;
        }
    }

    /**
     * Parse device specifications from command line.
     * Supports:
     * - COM3 (default port on all interfaces)
     * - COM3:50000 (custom port on all interfaces)
     * - COM3:127.0.0.1:50000 (specific IP and port)
     * - /dev/ttyUSB0:192.168.1.100:49999 (Linux with specific IP and port)
     */
    static List<DeviceSpec> parseDeviceSpecs(String portArg) {
        List<DeviceSpec> specs = new ArrayList<>();
        String[] ports = portArg.split(",");
        int autoPort = DEFAULT_TCP_PORT;

        for (String portSpec : ports) {
            portSpec = portSpec.trim();
            Matcher matcher = PORT_PATTERN.matcher(portSpec);

            if (matcher.matches()) {
                String serialPort = matcher.group(1);
                String bindAddressOrPort = matcher.group(2);
                String tcpPortStr = matcher.group(3);

                String bindAddress = null;
                int tcpPort;

                // Parse the middle component - could be bind address or port number
                if (tcpPortStr != null) {
                    // Format: serial:bindAddress:port
                    bindAddress = bindAddressOrPort;
                    tcpPort = Integer.parseInt(tcpPortStr);
                } else if (bindAddressOrPort != null) {
                    // Format: serial:X where X could be port or bind address
                    try {
                        tcpPort = Integer.parseInt(bindAddressOrPort);
                        // It's a port number, bind address stays null (all interfaces)
                    } catch (NumberFormatException e) {
                        // It's a bind address, use auto-increment port
                        bindAddress = bindAddressOrPort;
                        tcpPort = autoPort++;
                    }
                } else {
                    // Format: serial (no bind address, auto port)
                    tcpPort = autoPort++;
                }

                specs.add(new DeviceSpec(serialPort, bindAddress, tcpPort));
            } else {
                throw new IllegalArgumentException("Invalid port specification: " + portSpec);
            }
        }

        return specs;
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        // Configure logging to console
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss");

        System.out.println("MCAN Protocol Serial To TCP Multiplexer Bridge");
        System.out.println("==============================================");
        System.out.println("Bridges MCAN serial adapters to CS2 TCP protocol");
        System.out.println();

        // Parse command line arguments
        String portArg = null;
        int baudRate = DEFAULT_BAUD_RATE;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                case "-p":
                    if (i + 1 < args.length) {
                        portArg = args[++i];
                    }
                    break;
                case "--baud":
                case "-b":
                    if (i + 1 < args.length) {
                        try {
                            baudRate = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid baud rate: " + args[i]);
                            System.exit(1);
                        }
                    }
                    break;
                case "--monitor":
                case "-m":
                    monitorMode = true;
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;
                case "--list":
                case "-l":
                    listSerialPorts();
                    System.exit(0);
                    break;
                default:
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }

        // Validate required arguments
        if (portArg == null) {
            System.err.println("Error: Serial port is required");
            System.out.println();
            printUsage();
            System.exit(1);
        }

        // Parse device specifications
        List<DeviceSpec> devices;
        try {
            devices = parseDeviceSpecs(portArg);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return;
        }

        if (devices.isEmpty()) {
            System.err.println("Error: No devices specified");
            System.exit(1);
        }

        // Print configuration
        System.out.println("Configuration:");
        System.out.println("  Baud rate: " + baudRate);
        System.out.println("  Devices: " + devices.size());
        for (DeviceSpec spec : devices) {
            if (spec.bindAddress != null) {
                System.out.println("    - " + spec.serialPort + " → " + spec.bindAddress + ":" + spec.tcpPort);
            } else {
                System.out.println("    - " + spec.serialPort + " → TCP port " + spec.tcpPort + " (all interfaces)");
            }
        }
        System.out.println();

        // Create and start bridges
        try {
            for (DeviceSpec spec : devices) {
                BridgeInstance bridge = new BridgeInstance(spec.serialPort, spec.bindAddress, spec.tcpPort, baudRate);
                bridge.start();
                bridges.add(bridge);
                log.info("Bridge {} started successfully", spec);
            }

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(McanTcpBridge::shutdown));

            System.out.println();
            System.out.println("All bridges running. Press Ctrl+C to stop.");
            System.out.println();

            // Keep main thread alive
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted: " + e.getMessage());
            log.error("Bridge interrupted", e);
            shutdown();
            System.exit(1);
        } catch (java.io.IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
            log.error("Failed to start bridge", e);
            shutdown();
            System.exit(1);
        }
    }

    /**
     * Shutdown all bridges gracefully.
     */
    static void shutdown() {
        log.info("Shutting down all bridges...");
        running.set(false);

        for (BridgeInstance bridge : bridges) {
            bridge.shutdown();
        }

        log.info("All bridges stopped");
    }

    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.out.println("Usage: java -jar mcan-tcp-bridge.jar --port <port_spec> [options]");
        System.out.println();
        System.out.println("Port Specification:");
        System.out.println("  Single device:    --port COM3");
        System.out.println("  Custom TCP port:  --port COM3:50000");
        System.out.println("  Bind to specific IP: --port COM3:127.0.0.1:50000");
        System.out.println("  Multiple devices: --port COM2,COM3:49999,COM4:192.168.1.100:50000");
        System.out.println();
        System.out.println("  Format: <serial>[:<bind_addr>][:<tcp_port>][,<serial>[:<bind_addr>][:<tcp_port>],...]");
        System.out.println("    - If TCP port omitted, uses default (15731) for first device");
        System.out.println("    - Subsequent devices without port get auto-incremented (15732, 15733, ...)");
        System.out.println("    - If bind address omitted, binds to all interfaces (0.0.0.0)");
        System.out.println("    - Bind address can be IPv4 (192.168.1.100) or IPv6 (::1) or hostname (localhost)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  --port COM2,COM3,COM4");
        System.out.println("    COM2 → TCP 15731 (all interfaces)");
        System.out.println("    COM3 → TCP 15732 (all interfaces)");
        System.out.println("    COM4 → TCP 15733 (all interfaces)");
        System.out.println();
        System.out.println("  --port COM2,COM3:49999,COM4:50000");
        System.out.println("    COM2 → TCP 15731 (default, all interfaces)");
        System.out.println("    COM3 → TCP 49999 (explicit, all interfaces)");
        System.out.println("    COM4 → TCP 50000 (explicit, all interfaces)");
        System.out.println();
        System.out.println("  --port COM3:127.0.0.1:15731");
        System.out.println("    COM3 → localhost:15731 (localhost only)");
        System.out.println();
        System.out.println("  --port COM3:192.168.1.100:15731,COM4:192.168.1.100:15732");
        System.out.println("    COM3 → 192.168.1.100:15731 (specific interface)");
        System.out.println("    COM4 → 192.168.1.100:15732 (specific interface)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --port, -p <spec>    Port specification (required, see above)");
        System.out.println("  --baud, -b <rate>    Serial baud rate for all devices (default: 500000)");
        System.out.println("  --monitor, -m        Enable message monitoring to stdout");
        System.out.println("  --list, -l           List available serial ports and exit");
        System.out.println("  --help, -h           Show this help message");
        System.out.println();
        System.out.println("Advanced: Virtual COM Port for Serial Software");
        System.out.println("  To use serial-only software (e.g., programmer tools) alongside JMRI,");
        System.out.println("  you can create a virtual COM port that connects to the bridge's TCP server.");
        System.out.println();
        System.out.println("  Linux/macOS: ./scripts/create-mcan-virtual-port.sh /dev/ttyVCOM0 localhost 15731");
        System.out.println("  Windows:     .\\scripts\\create-mcan-virtual-port.ps1 -VirtualPort COM99 -TcpHost localhost -TcpPort 15731");
        System.out.println();
        System.out.println("  See: help/en/html/hardware/marklin/McanTcpBridge.shtml#virtual-ports");
        System.out.println();
    }

    /**
     * List available serial ports.
     */
    private static void listSerialPorts() {
        System.out.println("Available serial ports:");
        System.out.println();

        java.util.Vector<String> portNames = JSerialPort.getActualPortNames();
        if (portNames.isEmpty()) {
            System.out.println("  (none found)");
        } else {
            for (String portName : portNames) {
                JSerialPort port = JSerialPort.getPort(portName);
                System.out.println("  " + portName);
                System.out.println("    Description: " + port.getPortDescription());
                System.out.println("    Location: " + port.getPortLocation());
                System.out.println();
            }
        }
    }

    /**
     * Single bridge instance for one serial port.
     */
    static class BridgeInstance {
        private static final int BUFFER_SIZE = 1024;
        private static final int RECONNECT_DELAY_MS = 5000; // 5 seconds
        private static final int CAN_MESSAGE_SIZE = 13; // Märklin CAN messages are 13 bytes

        private final String serialPortName;
        private final String bindAddress; // null means bind to all interfaces
        private final int tcpPort;
        private final int baudRate;
        private volatile SerialPort serialPort;  // jmri.jmrix.SerialPort interface
        private volatile boolean serialPortConnected = false;
        private ServerSocket serverSocket;
        private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
        private final AtomicBoolean reconnecting = new AtomicBoolean(false);

        // Message queue for TCP → Serial (prevents command interleaving)
        private final BlockingQueue<byte[]> outgoingQueue = new LinkedBlockingQueue<>();

        // Statistics
        private long serialBytesRead = 0;
        private long serialBytesWritten = 0;
        private long tcpBytesRead = 0;
        private long tcpBytesWritten = 0;

        BridgeInstance(String serialPortName, String bindAddress, int tcpPort, int baudRate) {
            this.serialPortName = serialPortName;
            this.bindAddress = bindAddress;
            this.tcpPort = tcpPort;
            this.baudRate = baudRate;
        }

        void start() throws IOException {
            log.info("[{}:{}] Starting bridge", serialPortName, tcpPort);

            // Try to open serial port (non-fatal if it fails)
            tryOpenSerialPort();

            // Start TCP server
            startTcpServer();

            // Start serial reader thread
            startSerialReader();

            // Start serial writer thread (processes outgoing queue)
            startSerialWriter();

            // Start reconnection monitor thread
            startReconnectionMonitor();

            log.info("[{}:{}] Bridge started (serial port {})",
                     serialPortName, tcpPort, serialPortConnected ? "connected" : "not available");
        }

        private void tryOpenSerialPort() {
            try {
                log.info("[{}:{}] Attempting to open serial port", serialPortName, tcpPort);

                // Open port via JSerialPort wrapper (pass null for systemPrefix)
                serialPort = JSerialPort.activatePort(
                    null,                        // systemPrefix (null for standalone apps)
                    serialPortName,              // port name
                    log,                         // logger
                    1,                           // stop bits
                    SerialPort.Parity.NONE       // parity
                );

                if (serialPort == null) {
                    log.warn("[{}:{}] Serial port not available: {}", serialPortName, tcpPort, serialPortName);
                    serialPortConnected = false;
                    return;
                }

                // Configure baud rate (activatePort doesn't set it)
                serialPort.setBaudRate(baudRate);

                // Configure flow control using JMRI enum
                serialPort.setFlowControl(AbstractSerialPortController.FlowControl.NONE);

                // Set control signals
                serialPort.setRTS();
                serialPort.setDTR();

                serialPortConnected = true;
                log.info("[{}:{}] Serial port opened: {}", serialPortName, tcpPort, serialPort.getDescriptivePortName());
            } catch (Exception e) {
                log.warn("[{}:{}] Failed to open serial port: {}", serialPortName, tcpPort, e.getMessage());
                serialPortConnected = false;
                serialPort = null;
            }
        }

        private void closeSerialPort() {
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                log.info("[{}:{}] Serial port closed", serialPortName, tcpPort);
            }
            serialPort = null;
            serialPortConnected = false;
        }

        private void startReconnectionMonitor() {
            Thread monitorThread = new Thread(() -> {
                log.info("[{}:{}] Reconnection monitor started", serialPortName, tcpPort);

                while (running.get()) {
                    try {
                        Thread.sleep(RECONNECT_DELAY_MS);

                        // Check if serial port is connected
                        if (!serialPortConnected || serialPort == null || !serialPort.isOpen()) {
                            if (reconnecting.compareAndSet(false, true)) {
                                log.info("[{}:{}] Serial port disconnected, attempting reconnection...",
                                         serialPortName, tcpPort);

                                closeSerialPort();
                                tryOpenSerialPort();

                                if (serialPortConnected) {
                                    log.info("[{}:{}] Serial port reconnected successfully",
                                             serialPortName, tcpPort);
                                }
                                reconnecting.set(false);
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        log.error("[{}:{}] Error in reconnection monitor", serialPortName, tcpPort, e);
                        reconnecting.set(false);
                    }
                }

                log.info("[{}:{}] Reconnection monitor stopped", serialPortName, tcpPort);
            }, "Reconnect-Monitor-" + tcpPort);

            monitorThread.setDaemon(true);
            monitorThread.start();
        }

        private void startTcpServer() throws IOException {
            if (bindAddress != null) {
                // Bind to specific address
                java.net.InetAddress addr = java.net.InetAddress.getByName(bindAddress);
                serverSocket = new ServerSocket(tcpPort, 50, addr);
                log.info("[{}:{}] TCP server listening on {}:{}", serialPortName, tcpPort, bindAddress, tcpPort);
            } else {
                // Bind to all interfaces
                serverSocket = new ServerSocket(tcpPort);
                log.info("[{}:{}] TCP server listening on all interfaces", serialPortName, tcpPort);
            }

            Thread acceptorThread = new Thread(this::acceptClients, "TCP-Acceptor-" + tcpPort);
            acceptorThread.setDaemon(true);
            acceptorThread.start();
        }

        private void acceptClients() {
            while (running.get() && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("[{}:{}] Client connected: {}", serialPortName, tcpPort,
                             clientSocket.getRemoteSocketAddress());

                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    clients.add(handler);

                    Thread clientThread = new Thread(handler,
                        "TCP-Client-" + tcpPort + "-" + clientSocket.getRemoteSocketAddress());
                    clientThread.setDaemon(true);
                    clientThread.start();

                } catch (IOException e) {
                    if (running.get()) {
                        log.error("[{}:{}] Error accepting client", serialPortName, tcpPort, e);
                    }
                }
            }
        }

        private void startSerialReader() {
            Thread readerThread = new Thread(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];

                log.info("[{}:{}] Serial reader started", serialPortName, tcpPort);

                while (running.get()) {
                    try {
                        // Wait for serial port to be available
                        if (!serialPortConnected || serialPort == null) {
                            Thread.sleep(1000);
                            continue;
                        }

                        InputStream serialIn = serialPort.getInputStream();
                        if (serialIn == null) {
                            log.warn("[{}:{}] Serial input stream unavailable", serialPortName, tcpPort);
                            serialPortConnected = false;
                            Thread.sleep(1000);
                            continue;
                        }

                        int bytesRead = serialIn.read(buffer);
                        if (bytesRead > 0) {
                            serialBytesRead += bytesRead;

                            if (log.isDebugEnabled()) {
                                StringBuilder hex = new StringBuilder();
                                for (int i = 0; i < Math.min(bytesRead, 13); i++) {
                                    hex.append(String.format("%02X ", buffer[i] & 0xFF));
                                }
                                log.debug("[{}:{}] Serial RX ({} bytes): {}",
                                         serialPortName, tcpPort, bytesRead, hex);
                            }

                            broadcastToClients(buffer, bytesRead);
                        } else if (bytesRead < 0) {
                            // End of stream - port disconnected
                            log.warn("[{}:{}] Serial port disconnected (EOF)", serialPortName, tcpPort);
                            serialPortConnected = false;
                        }
                    } catch (IOException e) {
                        if (running.get()) {
                            log.warn("[{}:{}] Serial read error (device may be unplugged): {}",
                                    serialPortName, tcpPort, e.getMessage());
                            serialPortConnected = false;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                log.info("[{}:{}] Serial reader stopped", serialPortName, tcpPort);
            }, "Serial-Reader-" + tcpPort);

            readerThread.setDaemon(true);
            readerThread.start();
        }

        private void startSerialWriter() {
            Thread writerThread = new Thread(() -> {
                byte[] messageBuffer = new byte[CAN_MESSAGE_SIZE];
                int bufferPos = 0;

                log.info("[{}:{}] Serial writer started", serialPortName, tcpPort);

                while (running.get()) {
                    try {
                        // Wait for serial port to be available
                        if (!serialPortConnected || serialPort == null) {
                            Thread.sleep(1000);
                            continue;
                        }

                        // Take a message from the queue (blocking)
                        byte[] queuedData = outgoingQueue.take();

                        // Accumulate bytes into 13-byte CAN messages
                        for (int i = 0; i < queuedData.length; i++) {
                            messageBuffer[bufferPos++] = queuedData[i];

                            // When we have a complete 13-byte message, send it
                            if (bufferPos == CAN_MESSAGE_SIZE) {
                                sendMessageToSerial(messageBuffer, CAN_MESSAGE_SIZE);
                                bufferPos = 0; // Reset buffer for next message
                            }
                        }

                        // Note: Partial messages stay in buffer until completed
                        // This ensures we only send complete 13-byte CAN packets

                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        log.error("[{}:{}] Error in serial writer", serialPortName, tcpPort, e);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }

                log.info("[{}:{}] Serial writer stopped", serialPortName, tcpPort);
            }, "Serial-Writer-" + tcpPort);

            writerThread.setDaemon(true);
            writerThread.start();
        }

        private void sendMessageToSerial(byte[] data, int length) {
            if (!serialPortConnected || serialPort == null) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}:{}] Serial port not connected, discarding {} bytes",
                             serialPortName, tcpPort, length);
                }
                return;
            }

            // Monitor mode: log TCP → Serial with device identification
            if (monitorMode && length >= 13) {
                int[] rawData = new int[13];
                for (int i = 0; i < 13; i++) {
                    rawData[i] = data[i] & 0xFF;
                }
                String message = MarklinMessageFormatter.formatRaw(rawData, null, true);
                log.info("[{}] [TCP→Serial] {}", serialPortName, message);
            }

            try {
                OutputStream serialOut = serialPort.getOutputStream();
                if (serialOut == null) {
                    log.warn("[{}:{}] Serial output stream unavailable", serialPortName, tcpPort);
                    serialPortConnected = false;
                    return;
                }

                serialOut.write(data, 0, length);
                serialOut.flush();
                serialBytesWritten += length;

                if (log.isDebugEnabled()) {
                    StringBuilder hex = new StringBuilder();
                    for (int i = 0; i < Math.min(length, 13); i++) {
                        hex.append(String.format("%02X ", data[i] & 0xFF));
                    }
                    log.debug("[{}:{}] Serial TX ({} bytes): {}",
                             serialPortName, tcpPort, length, hex);
                }
            } catch (IOException e) {
                log.warn("[{}:{}] Error writing to serial (device may be unplugged): {}",
                        serialPortName, tcpPort, e.getMessage());
                serialPortConnected = false;
            }
        }

        void broadcastToClients(byte[] data, int length) {
            // Monitor mode: log Serial → TCP with device identification
            if (monitorMode && length >= 13) {
                int[] rawData = new int[13];
                for (int i = 0; i < 13; i++) {
                    rawData[i] = data[i] & 0xFF;
                }
                String message = MarklinMessageFormatter.formatRaw(rawData, null, true);
                log.info("[{}] [Serial→TCP] {}", serialPortName, message);
            }

            for (ClientHandler client : clients) {
                try {
                    client.send(data, length);
                    tcpBytesWritten += length;
                } catch (IOException e) {
                    log.warn("[{}:{}] Failed to send to client", serialPortName, tcpPort, e);
                    removeClient(client);
                }
            }
        }

        void sendToSerial(byte[] data, int length) {
            if (!serialPortConnected || serialPort == null) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}:{}] Serial port not connected, discarding {} bytes",
                             serialPortName, tcpPort, length);
                }
                return;
            }

            // Queue the data for the serial writer thread
            // The writer will accumulate bytes into complete 13-byte CAN messages
            byte[] copy = new byte[length];
            System.arraycopy(data, 0, copy, 0, length);

            try {
                outgoingQueue.put(copy);
            } catch (InterruptedException e) {
                log.warn("[{}:{}] Interrupted while queueing message", serialPortName, tcpPort);
                Thread.currentThread().interrupt();
            }
        }

        void removeClient(ClientHandler client) {
            clients.remove(client);
            client.close();
        }

        void onTcpBytesRead(int bytes) {
            tcpBytesRead += bytes;
        }

        void printStatistics() {
            log.info("[{}:{}] === Statistics ===", serialPortName, tcpPort);
            log.info("[{}:{}] Serial port status: {}", serialPortName, tcpPort,
                     serialPortConnected ? "connected" : "disconnected");
            log.info("[{}:{}] Serial: {} bytes read, {} bytes written",
                     serialPortName, tcpPort, serialBytesRead, serialBytesWritten);
            log.info("[{}:{}] TCP: {} bytes read, {} bytes written",
                     serialPortName, tcpPort, tcpBytesRead, tcpBytesWritten);
            log.info("[{}:{}] Connected clients: {}", serialPortName, tcpPort, clients.size());
        }

        void shutdown() {
            log.info("[{}:{}] Shutting down bridge", serialPortName, tcpPort);

            printStatistics();

            // Close all clients
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();

            // Close server socket
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                log.error("[{}:{}] Error closing server socket", serialPortName, tcpPort, e);
            }

            // Close serial port
            closeSerialPort();

            log.info("[{}:{}] Bridge shutdown complete", serialPortName, tcpPort);
        }
    }

    /**
     * Handler for a single TCP client connection.
     */
    static class ClientHandler implements Runnable {
        private static final int BUFFER_SIZE = 1024;

        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        private final String remoteAddress;
        private final BridgeInstance bridge;

        ClientHandler(Socket socket, BridgeInstance bridge) throws IOException {
            this.socket = socket;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            this.remoteAddress = socket.getRemoteSocketAddress().toString();
            this.bridge = bridge;

            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
        }

        @Override
        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];

            try {
                while (running.get() && !socket.isClosed()) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead < 0) {
                        break;
                    }

                    if (bytesRead > 0) {
                        bridge.onTcpBytesRead(bytesRead);

                        if (log.isDebugEnabled()) {
                            StringBuilder hex = new StringBuilder();
                            for (int i = 0; i < Math.min(bytesRead, 13); i++) {
                                hex.append(String.format("%02X ", buffer[i] & 0xFF));
                            }
                            log.debug("[{}] TCP RX from {} ({} bytes): {}",
                                     bridge.tcpPort, remoteAddress, bytesRead, hex);
                        }

                        bridge.sendToSerial(buffer, bytesRead);
                    }
                }
            } catch (SocketException e) {
                if (running.get()) {
                    log.info("[{}] Client disconnected: {}", bridge.tcpPort, remoteAddress);
                }
            } catch (IOException e) {
                log.error("[{}] Error reading from client: {}", bridge.tcpPort, remoteAddress, e);
            } finally {
                close();
                bridge.removeClient(this);
            }
        }

        void send(byte[] data, int length) throws IOException {
            synchronized (out) {
                out.write(data, 0, length);
                out.flush();
            }
        }

        void close() {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                log.error("Error closing client socket", e);
            }
        }
    }
}

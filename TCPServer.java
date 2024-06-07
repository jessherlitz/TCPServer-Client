import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

public class TCPServer {

    public static void main(String[] args) throws Exception {

        if (args.length != 1)
            throw new IllegalArgumentException("Parameter(s): <Port>");

        int port = Integer.parseInt(args[0]);
        System.out.println("\nServer running on port: " + port);

        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            try (Socket clientSocket = serverSocket.accept();
                 DataInputStream src = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

                System.out.println("\nClient connected: " + clientSocket.getInetAddress().getHostAddress());

                Request request = readRequest(src);
                Response response = processRequest(request);

                sendResponse(out, response);

                logResponse(response);

            } catch (IOException e) {
                System.err.println("I/O error occurred: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error occurred: " + e.getMessage());
            }
        }
    }

    private static Request readRequest(DataInputStream src) throws IOException {
        byte tml = src.readByte();
        byte opCode = src.readByte();
        int operandOne = src.readInt();
        int operandTwo = src.readInt();
        short requestId = src.readShort();
        byte opNameLength = src.readByte();
        byte[] opNameBytes = new byte[opNameLength];
        src.readFully(opNameBytes);
        String opName = new String(opNameBytes, StandardCharsets.UTF_16);

        System.out.println("\nRequest ID: " + requestId);
        System.out.println("Operation: " + opName);
        System.out.println("Operand 1: " + operandOne);
        System.out.println("Operand 2: " + operandTwo);

        return new Request(tml, opCode, operandOne, operandTwo, requestId, opNameLength, opName);
    }

    private static Response processRequest(Request request) {
        int result = 0;
        boolean error = false;

        switch (request.getOpCode()) {
            case 0: result = request.getOperandOne() + request.getOperandTwo(); break;
            case 1: result = request.getOperandOne() - request.getOperandTwo(); break;
            case 2: result = request.getOperandOne() | request.getOperandTwo(); break;
            case 3: result = request.getOperandOne() & request.getOperandTwo(); break;
            case 4:
                if (request.getOperandTwo() == 0) {
                    error = true;
                } else {
                    result = request.getOperandOne() / request.getOperandTwo();
                }
                break;
            case 5: result = request.getOperandOne() * request.getOperandTwo(); break;
            default: error = true; break;
        }

        if (request.getTml() != 13 + request.getOpNameLength()) {
            error = true;
        }

        byte errorCode = (byte) (error ? 127 : 0);

        return new Response((byte) 8, result, errorCode, request.getRequestId());
    }

    private static void sendResponse(DataOutputStream out, Response response) throws IOException {
        out.writeByte(response.getTml());
        out.writeInt(response.getResult());
        out.writeByte(response.getErrorCode());
        out.writeShort(response.getRequestId());
        out.flush();
    }

    private static void logResponse(Response response) {
        System.out.println("\nResponse ID: " + response.getRequestId());
        System.out.println("Result: " + response.getResult());
        System.out.println("Error Code: " + (response.getErrorCode() == 0 ? "OK" : "Error"));
    }
}

class Request {
    private byte tml;
    private byte opCode;
    private int operandOne;
    private int operandTwo;
    private short requestId;
    private byte opNameLength;
    private String opName;

    public Request(byte tml, byte opCode, int operandOne, int operandTwo, short requestId, byte opNameLength, String opName) {
        this.tml = tml;
        this.opCode = opCode;
        this.operandOne = operandOne;
        this.operandTwo = operandTwo;
        this.requestId = requestId;
        this.opNameLength = opNameLength;
        this.opName = opName;
    }

    public byte getTml() {
        return tml;
    }

    public byte getOpCode() {
        return opCode;
    }

    public int getOperandOne() {
        return operandOne;
    }

    public int getOperandTwo() {
        return operandTwo;
    }

    public short getRequestId() {
        return requestId;
    }

    public byte getOpNameLength() {
        return opNameLength;
    }

    public String getOpName() {
        return opName;
    }
}

class Response {
    private byte tml;
    private int result;
    private byte errorCode;
    private short requestId;

    public Response(byte tml, int result, byte errorCode, short requestId) {
        this.tml = tml;
        this.result = result;
        this.errorCode = errorCode;
        this.requestId = requestId;
    }

    public byte getTml() {
        return tml;
    }

    public int getResult() {
        return result;
    }

    public byte getErrorCode() {
        return errorCode;
    }

    public short getRequestId() {
        return requestId;
    }
}

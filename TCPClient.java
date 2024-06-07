import java.net.*;
import java.io.*;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;

public class TCPClient {

    private static Scanner scanner = new Scanner(System.in);
    private static short requestId = 1;
    private static ArrayList<Long> dataCollection = new ArrayList<>();

    public static void main(String args[]) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Parameter(s): <Server> <Port>");
        }

        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);

        while (true) {
            try (Socket socket = new Socket(serverAddress, serverPort);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                byte opCode = getOpCode();
                int operand1 = getOperand("Enter Operand 1: ");
                int operand2 = getOperand("Enter Operand 2: ");

                byte[] requestBytes = buildRequest(opCode, operand1, operand2);
                long startTime = System.currentTimeMillis();

                out.write(requestBytes);
                out.flush();

                System.out.printf("\nRequest: ");
                printBytes(requestBytes);

                byte[] responseBytes = readResponse(in);
                long endTime = System.currentTimeMillis();
                long rtt = endTime - startTime;

                dataCollection.add(rtt);

                printResponse(responseBytes);
                printRTTStatistics(rtt);

                requestId++;

                if (!askToContinue()) {
                    break;
                }

            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter valid numeric values.");
                scanner.nextLine(); 
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
                break;
            }
        }

        scanner.close();
    }

    private static void exitApp() {
        if (!scanner.hasNext()) {
            System.exit(0);
        }
    }
    private static byte getOpCode() {
        System.out.print("\nEnter OpCode (0 for +, 1 for -, 2 for |, 3 for &, 4 for /, 5 for *): ");

        exitApp();

        return scanner.nextByte();
    }

    private static int getOperand(String prompt) {
        System.out.print(prompt);
        exitApp();

        return scanner.nextInt();
    }

    private static byte[] buildRequest(byte opCode, int operand1, int operand2) throws IOException {
        String opName = getOpName(opCode);
        byte[] opNameBytes = opName.getBytes(StandardCharsets.UTF_16);
        byte opNameLength = (byte) opNameBytes.length;
        byte tml = (byte) (13 + opNameLength);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream bufferOut = new DataOutputStream(buf);

        bufferOut.writeByte(tml);
        bufferOut.writeByte(opCode);
        bufferOut.writeInt(operand1);
        bufferOut.writeInt(operand2);
        bufferOut.writeShort(requestId);
        bufferOut.writeByte(opNameLength);
        bufferOut.write(opNameBytes);
        bufferOut.flush();

        return buf.toByteArray();
    }

    private static String getOpName(byte opCode) {
        switch (opCode) {
            case 0: return "addition";
            case 1: return "subtraction";
            case 2: return "or";
            case 3: return "and";
            case 4: return "division";
            case 5: return "multiplication";
            default: throw new IllegalArgumentException("Invalid OpCode");
        }
    }

    private static byte[] readResponse(DataInputStream in) throws IOException {
        byte responseTML = in.readByte();
        int result = in.readInt();
        byte errorCode = in.readByte();
        short responseRequestId = in.readShort();

        ByteBuffer responseBuf = ByteBuffer.allocate(8);
        responseBuf.put(responseTML);
        responseBuf.putInt(result);
        responseBuf.put(errorCode);
        responseBuf.putShort(responseRequestId);

        return responseBuf.array();
    }

    private static void printBytes(byte[] bytes) {
        for (byte b : bytes) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }

    private static void printResponse(byte[] responseBytes) {
        ByteBuffer responseBuf = ByteBuffer.wrap(responseBytes);
        byte responseTML = responseBuf.get();
        int result = responseBuf.getInt();
        byte errorCode = responseBuf.get();
        short responseRequestId = responseBuf.getShort();

        System.out.printf("\nResponse: ");
        printBytes(responseBytes);

        System.out.println("\nResponse ID: " + responseRequestId);
        System.out.println("Result: " + result);
        System.out.println("Error Code: " + (errorCode == 0 ? "OK" : "Error " + errorCode));
    }

    private static void printRTTStatistics(long rtt) {
        System.out.println("\nRound trip time: " + rtt + " ms");

        long minRTT = dataCollection.stream().min(Long::compare).orElse(0L);
        long maxRTT = dataCollection.stream().max(Long::compare).orElse(0L);
        double averageRTT = dataCollection.stream().mapToLong(Long::longValue).average().orElse(0.0);

        System.out.println("\nMin RTT: " + minRTT + " ms");
        System.out.println("Max RTT: " + maxRTT + " ms");
        System.out.println("Average RTT: " + averageRTT + " ms");
    }

    private static boolean askToContinue() {
        System.out.print("\nDo you want to send another request? (yes/no): ");      
        exitApp();

        String answer = scanner.next();
        scanner.nextLine(); 
        return answer.equalsIgnoreCase("yes");
    }
}













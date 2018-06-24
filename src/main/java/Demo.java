import cn.edu.ustc.Guerrillas.BigBlock.client.UserClient;
import cn.edu.ustc.Guerrillas.BigBlock.core.Block;

import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;

public class Demo {
    static void createMode(UserClient userClient, String repo, Scanner scanner) {
        System.out.println("Please enter the file's path");
        String filename = scanner.next();
        Block root = userClient.createRepo(filename, repo);
        System.out.println("Repo " + repo + " has been created!");
        System.out.println("File hash:");
        String fileHash = Base64.getEncoder().encodeToString(root.getFileHashCode());
        System.out.println(fileHash);
        System.out.println("Block id:");
        System.out.println(root.getUuid());
    }

    static void startTrade(UserClient userClient, Scanner scanner) {
        System.out.println("Enter the block's id to start a trade");
        String id = scanner.next();
        long timeStamp = System.currentTimeMillis();
        Block block = userClient.getBlock(UUID.fromString(id));
        String tradeHash = Base64.getEncoder().encodeToString(userClient.sign(block, timeStamp));
        System.out.println("TradeHash is: " + tradeHash);
        System.out.println("in time: " + timeStamp);
    }

    static void addBlock(UserClient userClient, Scanner scanner) {
        System.out.println("Please enter the block's id");
        String prevId = scanner.next();
        System.out.println("Please enter the tradeHash");
        byte[] tradeHash = Base64.getDecoder().decode(scanner.next());
        System.out.println("Please enter the timestamp");
        long timeStamp = scanner.nextLong();
        System.out.println("Please enter the filename");
        String filename = scanner.next();
        try {
            Block block = userClient.addBlock(UUID.fromString(prevId), tradeHash, timeStamp, filename);
            String fileHash = Base64.getEncoder().encodeToString(block.getFileHashCode());
            System.out.println(fileHash);
            System.out.println("Trade complete!");
            System.out.println("Block id:");
            System.out.println(block.getUuid());
        } catch (Exception e) {
            System.out.println("Trade failed");
        }
    }

    public static void main(String[] args) {
        System.out.println("Copyright [2018] [Guerrillas-ustc]");
        String logo =
                " _______   __                  _______   __                      __       \n" +
                        "/       \\ /  |                /       \\ /  |                    /  |      \n" +
                        "$$$$$$$  |$$/   ______        $$$$$$$  |$$ |  ______    _______ $$ |   __ \n" +
                        "$$ |__$$ |/  | /      \\       $$ |__$$ |$$ | /      \\  /       |$$ |  /  |\n" +
                        "$$    $$< $$ |/$$$$$$  |      $$    $$< $$ |/$$$$$$  |/$$$$$$$/ $$ |_/$$/ \n" +
                        "$$$$$$$  |$$ |$$ |  $$ |      $$$$$$$  |$$ |$$ |  $$ |$$ |      $$   $$<  \n" +
                        "$$ |__$$ |$$ |$$ \\__$$ |      $$ |__$$ |$$ |$$ \\__$$ |$$ \\_____ $$$$$$  \\ \n" +
                        "$$    $$/ $$ |$$    $$ |      $$    $$/ $$ |$$    $$/ $$       |$$ | $$  |\n" +
                        "$$$$$$$/  $$/  $$$$$$$ |      $$$$$$$/  $$/  $$$$$$/   $$$$$$$/ $$/   $$/ \n" +
                        "              /  \\__$$ |                                                  \n" +
                        "              $$    $$/                                                   \n" +
                        "               $$$$$$/                                                    \n";
        System.out.println(logo);
        String licence = "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                "you may not use this file except in compliance with the License.\n" +
                "You may obtain a copy of the License at\n" +
                "\n" +
                "    http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "Unless required by applicable law or agreed to in writing, software\n" +
                "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                "See the License for the specific language governing permissions and\n" +
                "limitations under the License.";
        System.out.println(licence);
        String serverAddress = "localhost";
        int port = 9000;
        System.out.println("Please enter the username");
        Scanner scanner = new Scanner(System.in);
        String username = scanner.next();
        UserClient userClient = new UserClient(username, serverAddress, port);
        System.out.println("Please enter the repo's name");
        String repo = scanner.next();
        while (true) {
            System.out.println("Please choose the mode:");
            System.out.println("A: Add a block, C: Create repo, S: Start a trade, E: Exit");
            String mode = scanner.next().toLowerCase();
            if (mode.contains("a")) {
                addBlock(userClient, scanner);
            } else if (mode.contains("c")) {
                createMode(userClient, repo, scanner);
            } else if (mode.contains("s")) {
                startTrade(userClient, scanner);
            } else {
                return;
            }
        }
    }
}

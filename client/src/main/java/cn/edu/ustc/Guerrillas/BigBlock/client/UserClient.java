package cn.edu.ustc.Guerrillas.BigBlock.client;

import cn.edu.ustc.Guerrillas.BigBlock.core.Block;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.UUID;
import java.util.logging.Logger;

public class UserClient {
    private static Logger logger = Logger.getLogger(UserClient.class.getName());
    private HttpClient httpClient;
    private String committer;
    private PrivateKey privateKey;
    private RocksDB db;

    static {
        RocksDB.loadLibrary();
    }

    public UserClient(String committer, String serverAddress, int port) {
        httpClient = new HttpClient(serverAddress, port);
        Options options = new Options().setCreateIfMissing(true);
        try {
            File file = new File("data/" + committer);
            file.mkdirs();
            db = RocksDB.open(options, file.getPath());
            this.committer = committer;
            byte[] keyBytes = db.get(committer.getBytes());
            if (keyBytes == null) {
                KeyPair keyPair = KeyPairGenerator.getInstance("RSA").genKeyPair();
                this.privateKey = keyPair.getPrivate();
                if (httpClient.register(committer, keyPair.getPublic().getEncoded())) {
                    db.put(committer.getBytes(), keyPair.getPrivate().getEncoded());
                }
            } else {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                this.privateKey = keyFactory.generatePrivate(keySpec);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Block createRepo(String filename, String repo) {
        File file = new File(filename);
        Block block = Block.createRootBlock(committer, privateKey, file, repo);
        if (httpClient.createRepo(block)) {
            putBlockToDB(block);
            return block;
        }
        return null;
    }

    public Block addBlock(UUID prevId, byte[] tradeHash, long prevTimeStamp, String filename) {
        File file = new File(filename);
        Block prev = getBlock(prevId);
        try {
            String repo = prev.getRepo();
            Block block = new Block(tradeHash, prevId, committer, privateKey, file, repo, prevTimeStamp);
            if (httpClient.putBlock(block)) {
                putBlockToDB(block);
                return block;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] sign(Block block, long timeStamp) {
        return Block.sign(block.getFileHashCode(), privateKey, timeStamp);
    }

    private boolean updateRepo(UUID uuid) {
        try {
            while (true) {
                byte[] blockBytes = db.get(uuid.toString().getBytes());
                if (blockBytes != null) {
                    return true;
                }
                Block block = httpClient.getBlock(uuid);
                if (block == null) {
                    return false;
                }
                putBlockToDB(block);
                uuid = block.getUuid();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Block getBlock(UUID uuid) {
        try {
            byte[] blockBytes = db.get(uuid.toString().getBytes());
            if (blockBytes == null && !updateRepo(uuid)) {
                return null;
            }
            blockBytes = db.get(uuid.toString().getBytes());
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(blockBytes, Block.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void fetchRepo(String repo) {
        httpClient.fetchRepo(repo, this::putBlockToDB);
    }

    private void putBlockToDB(Block block) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            db.put(block.getUuid().toString().getBytes(), objectMapper.writeValueAsBytes(block));
        } catch (RocksDBException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}

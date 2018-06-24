package cn.edu.ustc.Guerrillas.BigBlock.server;

import cn.edu.ustc.Guerrillas.BigBlock.core.Block;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;

import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;

public class CassandraDao {
    private static Logger logger = Logger.getLogger(CassandraDao.class.getName());
    private Cluster cluster;
    private Session session;
    private Mapper<BlockObject> mapper;

    public CassandraDao() {
        cluster = Cluster.builder().addContactPoint("localhost").build();
        session = cluster.connect();
        session.execute("CREATE KEYSPACE IF NOT EXISTS BigBlock " +
                "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
        session.execute("USE BigBlock");
        session.execute("CREATE TABLE IF NOT EXISTS public_key (id text PRIMARY KEY, pubKey blob)");
        session.execute("CREATE TABLE IF NOT EXISTS blocks(" +
                "repo text, prevId UUID, prevTimeStamp bigint, tradeHash blob, uuid UUID, " +
                "signedHashCode blob, fileHashCode blob, committer text, timeStamp bigint, " +
                "PRIMARY KEY (uuid))");
        session.execute("CREATE INDEX IF NOT EXISTS ON blocks (repo)");
        session.execute("CREATE TABLE IF NOT EXISTS hashes (hashCode blob PRIMARY KEY)");
        MappingManager manager = new MappingManager(session);
        mapper = manager.mapper(BlockObject.class);
    }

    public byte[] getPublicKey(String id) {
        ResultSet resultSet = session.execute("SELECT pubKey FROM public_key WHERE id = ?", id);
        Row res = resultSet.one();
        if (res != null) {
            return res.getBytes("pubKey").array();
        }
        return null;
    }

    public boolean putPublicKey(String id, byte[] publicKey) {
        ByteBuffer pubKey = ByteBuffer.wrap(publicKey);
        try {
            ResultSet resultSet = session.execute("SELECT * FROM public_key WHERE id = ?", id);
            if (!resultSet.iterator().hasNext()) {
                session.execute("INSERT INTO public_key (id, pubKey) VALUES (?,?)", id, pubKey);
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
            throw e;
        }
        return true;
    }

    public Block getBlock(UUID uuid) {
        return mapper.get(uuid).toBlock();
    }

    public Iterator<Block> getRepo(String repo) {
        ResultSet resultSet = session.execute("SELECT * FROM blocks WHERE repo = ?", repo);
        Iterator<BlockObject> results = mapper.map(resultSet).iterator();
        return new Iterator<Block>() {
            @Override
            public boolean hasNext() {
                return results.hasNext();
            }

            @Override
            public Block next() {
                return results.next().toBlock();
            }
        };
    }

    public boolean createRepo(Block block) {
        String repo = block.getRepo();
        ResultSet resultSet = session.execute("SELECT * FROM blocks WHERE repo = ?", repo);
        if (resultSet.iterator().hasNext()) {
            return false;
        }
        if (!checkBlock(block)) {
            return false;
        }
        BlockObject blockObject = new BlockObject(block);
        mapper.save(blockObject);
        return true;
    }

    public boolean checkBlock(Block block) {
        if (!Block.isRoot(block)) {
            ByteBuffer signedHashCode = ByteBuffer.wrap(block.getTradeHash());
            ResultSet resultSet = session.execute("SELECT * FROM hashes WHERE hashCode = ?", signedHashCode);
            if (resultSet.iterator().hasNext()) {
                return false;
            }
        } else {
            ByteBuffer tradeHash = ByteBuffer.wrap(block.getTradeHash());
            ResultSet resultSet = session.execute("SELECT * FROM hashes WHERE hashCode = ?", tradeHash);
            if (resultSet.iterator().hasNext()) {
                return false;
            }
        }
        String committer = block.getCommitter();
        byte[] keyBytes = getPublicKey(committer);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = null;
        PublicKey publicKey = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
        if (Block.isRoot(block)) {
            return block.checkBody(publicKey);
        } else {
            Block prevBlock = getBlock(block.getPrevId());
            byte[] prevKeyBytes = getPublicKey(prevBlock.getCommitter());
            keySpec = new X509EncodedKeySpec(prevKeyBytes);
            try {
                PublicKey prevPublicKey = keyFactory.generatePublic(keySpec);
                return block.check(prevPublicKey, prevBlock.getFileHashCode(), publicKey);
            } catch (Exception e) {
                logger.severe(e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean putBlock(Block block) {
        if (!checkBlock(block)) {
            return false;
        }
        mapper.save(new BlockObject(block));
        ByteBuffer buffer = ByteBuffer.wrap(block.getSignedHashCode());
        session.execute("INSERT INTO hashes (hashCode) VALUES (?)", buffer);
        if (!Block.isRoot(block)) {
            buffer = ByteBuffer.wrap(block.getTradeHash());
            session.execute("INSERT INTO hashes (hashCode) VALUES (?)", buffer);
        }
        return true;
    }

    public void stop() {
        cluster.close();
        session.close();
    }
}

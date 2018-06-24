package cn.edu.ustc.Guerrillas.BigBlock.server;

import cn.edu.ustc.Guerrillas.BigBlock.core.Block;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.nio.ByteBuffer;
import java.util.UUID;

@Table(keyspace = "BigBlock", name = "blocks",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM")
class BlockObject {
    String repo;
    UUID prevId;
    long prevTimeStamp;
    ByteBuffer tradeHash;
    @PartitionKey(0)
    UUID uuid;
    ByteBuffer signedHashCode;
    ByteBuffer fileHashCode;
    String committer;
    long timeStamp;

    BlockObject(Block block) {
        repo = block.getRepo();
        prevId = block.getPrevId();
        prevTimeStamp = block.getPrevTimeStamp();
        tradeHash = ByteBuffer.wrap(block.getTradeHash());
        uuid = block.getUuid();
        signedHashCode = ByteBuffer.wrap(block.getSignedHashCode());
        fileHashCode = ByteBuffer.wrap(block.getFileHashCode());
        committer = block.getCommitter();
        timeStamp = block.getTimeStamp();
    }

    BlockObject() {
    }

    Block toBlock() {
        return new Block(repo, prevId, prevTimeStamp, tradeHash.array(), uuid, signedHashCode.array(),
                fileHashCode.array(), committer, timeStamp);
    }
}

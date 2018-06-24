package cn.edu.ustc.Guerrillas.BigBlock.core;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.UUID;

public class Block {
    private String repo;
    private UUID prevId;
    private long prevTimeStamp;
    private byte[] tradeHash;
    private UUID uuid;
    private byte[] signedHashCode;
    private byte[] fileHashCode;
    private String committer;
    private long timeStamp;

    private Block() {

    }

    public Block(byte[] tradeHash, UUID prevId, String committer, PrivateKey privateKey, File file, String repo,
                 long prevTimeStamp) {
        this.uuid = UUID.randomUUID();
        this.prevId = prevId;
        try {
            this.fileHashCode = Files.asByteSource(file).hash(Hashing.sha256()).asBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.timeStamp = System.currentTimeMillis();
        this.signedHashCode = sign(fileHashCode, privateKey, timeStamp);
        this.tradeHash = tradeHash;
        this.committer = committer;
        this.repo = repo;
        this.prevTimeStamp = prevTimeStamp;
    }

    public Block(String repo, UUID prevId, long prevTimeStamp, byte[] tradeHash, UUID uuid, byte[] signedHashCode,
                 byte[] fileHashCode, String committer, long timeStamp) {
        this.repo = repo;
        this.prevId = prevId;
        this.prevTimeStamp = prevTimeStamp;
        this.tradeHash = tradeHash;
        this.uuid = uuid;
        this.signedHashCode = signedHashCode;
        this.fileHashCode = fileHashCode;
        this.committer = committer;
        this.timeStamp = timeStamp;
    }

    static public byte[] sign(byte[] hashBytes, PrivateKey privateKey, long timeStamp) {
        HashFunction hf = Hashing.sha256();
        hashBytes = hf.newHasher().putBytes(hashBytes).putLong(timeStamp).hash().asBytes();
        try {
            Signature signature = Signature.getInstance("NONEwithRSA");
            signature.initSign(privateKey);
            signature.update(hashBytes);
            return signature.sign();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static public Block createRootBlock(String committer, PrivateKey privateKey, File file, String repo) {
        return new Block(new byte[256], null, committer, privateKey, file, repo, 0);
    }

    static public boolean isRoot(Block block) {
        return block.prevId == null;
    }

    static public boolean verify(PublicKey publicKey, byte[] sigBytes, byte[] origin) {
        try {
            Signature signature = Signature.getInstance("NONEwithRSA");
            signature.initVerify(publicKey);
            signature.update(origin);
            return signature.verify(sigBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean check(PublicKey prevPublicKey, byte[] prevFileHash, PublicKey publicKey) {
        if (!checkBody(publicKey)) {
            return false;
        }
        if (isRoot(this)) {
            return true;
        }
        return checkPrev(prevPublicKey, prevFileHash);
    }

    public boolean checkBody(PublicKey publicKey) {
        HashFunction hf = Hashing.sha256();
        byte[] hashBytes = hf.newHasher().putBytes(fileHashCode).putLong(timeStamp).hash().asBytes();
        return verify(publicKey, signedHashCode, hashBytes);
    }

    public boolean checkPrev(PublicKey prevPublicKey, byte[] prevFileHash) {
        HashFunction hf = Hashing.sha256();
        byte[] hashBytes = hf.newHasher().putBytes(prevFileHash).putLong(prevTimeStamp).hash().asBytes();
        return verify(prevPublicKey, tradeHash, hashBytes);
    }

    public byte[] getSignedHashCode() {
        return signedHashCode.clone();
    }

    public byte[] getTradeHash() {
        return tradeHash.clone();
    }

    public String getRepo() {
        return repo;
    }

    public String getCommitter() {
        return committer;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public byte[] getFileHashCode() {
        return fileHashCode;
    }

    public long getPrevTimeStamp() {
        return prevTimeStamp;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getPrevId() {
        return prevId;
    }

}

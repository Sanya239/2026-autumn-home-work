package company.vk.edu.distrib.compute.sanya239.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.util.NoSuchElementException;

public class ClusterDao implements Dao<String> {

    Dao<String>[] shards;

    private int selectShard(String key) {
        var shardNumber = 0;
        var maxHash = Integer.MIN_VALUE;

        for (int i = 0; i < shards.length; i++) {
            var hash = String.format("%d : %s", i, key).hashCode();
            if (hash > maxHash) {
                maxHash = hash;
                shardNumber = i;
            }
        }
        return shardNumber;
    }

    public ClusterDao(int... ports) throws IOException {
        var factory = new RemoteDaoFactory();
        shards = new Dao[ports.length];
        for (int pos = 0; pos < ports.length; pos++) {
            shards[pos] = factory.create(ports[pos]);
        }
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        var shard = shards[selectShard(key)];
        return shard.get(key);
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        var shard = shards[selectShard(key)];
        shard.upsert(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        var shard = shards[selectShard(key)];
        shard.delete(key);
    }

    @Override
    public void close() throws IOException {
        for (var shard : shards) {
            shard.close();
        }
    }
}
